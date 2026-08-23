# 🎓 エラーハンドリング読解ノート（学習用）

> ⚠️ **このドキュメントは人間の学習用メモである。**
> 設計・実装の真実は [api/architecture/backend-architecture.md](../api/architecture/backend-architecture.md)、
> [api/architecture/error-contract.md](../api/architecture/error-contract.md) とコード本体にある。
> **AI エージェント（Claude 等）はコード生成・設計判断の参照にこのファイルを使わないこと。**
> ここに書かれた行番号やコード断片は執筆時点（Spring Framework 7.0.8）のスナップショットであり、更新の義務を負わない。

Spring のエラー処理が「例外の発生からクライアントへの JSON 応答まで」
どう流れるかを、実コードを追いながら理解したときの記録。

---

## 1. 登場人物

| 名前 | 正体 | 役割 |
|---|---|---|
| `DispatcherServlet` | Spring の中枢 | 全リクエストの入口。例外を必ず catch する |
| `ResponseEntityExceptionHandler` | Spring 備え付けの**基底クラス** | フレームワーク例外の処理係が約20個入っている |
| `GlobalExceptionHandler` | **自作の唯一のクラス**。上を継承 | ドメイン例外の翻訳・code の付与・最後の砦 |
| `ProblemDetail` | Spring 備え付けのデータクラス | エラー応答 JSON（`{"status":400,...}`）になる**中身の入れ物** |
| `ResponseEntity` | Spring 備え付けのデータクラス | ステータス + ヘッダ + ボディを詰めた**HTTP応答一式の箱** |
| `handleExceptionInternal` | 基底クラスの**メソッド**（クラスではない） | 全経路が最後に通る「梱包係」 |

---

## 2. `return handleExceptionInternal(...)` の読み方

メソッドを返しているのではない。**メソッドを呼び、戻ってきた値を返している。**

```java
// この1行は…
return handleExceptionInternal(ex, pd, headers, status, request);

// …この2行と同じ意味
ResponseEntity<Object> box = handleExceptionInternal(ex, pd, headers, status, request);
// ① メソッドを「呼ぶ」。処理がそちらへ飛び、終わると戻り値（値！）を持って帰ってくる
return box;
// ② その値を、自分の呼び出し元へ返す
```

`return add(1, 2)` が「add メソッドを返す」のではなく「3 を返す」のと同じ。

---

## 3. アノテーション = 起動時に作られる「振り分け表」

`@ExceptionHandler(...)` 自体は何もしない、ただのラベル。
Spring が**起動時に一度だけ** `@RestControllerAdvice` の付いたクラスをスキャンし、
「例外の型 → 呼ぶメソッド」の対応表を作る。

| 例外の型 | 呼ばれるメソッド | 登録の由来 |
|---|---|---|
| `DomainRuleViolationException` | `handleDomainRuleViolation` | 自作の `@ExceptionHandler(...)` |
| 404 / 405 / 壊れた JSON など約20種 | 基底クラスの `handleException` | **基底クラスに書いてある** `@ExceptionHandler({...20種}) ` |
| `Exception`（その他全部） | `handleUnexpected`（最後の砦） | 自作の `@ExceptionHandler(Exception.class)` |

実行時に例外が起きるたび、中枢がこの表を引く。ルールは「**一番具体的な型が勝つ**」。
具体的な登録がある限り `Exception.class` は選ばれない。だから最後の砦には
「どの行にも合致しなかった例外」だけが落ちてくる。

**基底クラスを継承するだけで、2行目の約20種ぶんの登録が自動的に表に載る。**
これが「継承するだけでフレームワーク例外の処理がついてくる」ことの正体。

---

## 4. オーバーライドは「Spring のコード内部からの呼び出し」にも効く

```java
class Base {
    void greet() { System.out.println("hello"); }
    void run()   { greet(); }   // ← Base 自身のコードが greet() を呼ぶ
}
class Mine extends Base {
    @Override
    void greet() { System.out.println("こんにちは"); }
}
new Mine().run();   // → 「こんにちは」
```

`run()` は Base に書かれたコードで、Base の作者は Mine を知らない。
それでも中の `greet()` は Mine 版に差し替わる。Java は「実際のオブジェクトが何者か」で
メソッドを選ぶから（動的ディスパッチ）。

これを当てはめると: Spring が昔書いた基底クラスの中の `handleExceptionInternal(...)` という
呼び出しが、実行時には自作の上書き版に化ける。**Spring のコードを1行も書き換えずに、
Spring の処理の真ん中に割り込める**のはこの仕組みのおかげ。

---

## 5. リクエストボディの3つの関門

JSON が DTO になるまでに3つの関門があり、どこで落ちたかで例外が変わる。

```
クライアントが送った文字列
  │
  ├─ 関門1: そもそも JSON として読めるか？（構文）
  │    `{broken` → 落ちると HttpMessageNotReadableException
  │
  ├─ 関門2: JSON は正しいが、DTO の型に変換できるか？
  │    {"startAt": "あした"} → これも HttpMessageNotReadableException
  │
  └─ 関門3: DTO はできたが、@NotBlank / @Size を満たすか？
       {"title": ""} → MethodArgumentNotValidException（VALIDATION_ERROR の経路）
```

区別の線は「**DTO という Java オブジェクトを作れたか**」。
作れなかった（関門1・2）→ 読めなかった系。作れたが中身が不正（関門3）→ 検証違反系。

---

## 6. 壊れた JSON の一部始終（Step 0〜5）

`POST /events` に `{broken` を送ったときの完全な流れ。

### Step 0: Controller は一度も動かない

JSON → `CreateEventRequest` の変換は、Controller に引数を渡すための**準備段階**で行われる。
そこで失敗するので `EventController.create` の本体には一度も入らない。

```
中枢: 「POST /events → EventController.create に渡す準備をしよう」
  → Jackson（JSON変換器）に {broken を渡す → 「読めません」
  → HttpMessageNotReadableException が投げられる
```

### Step 1: 中枢が catch して、表を引く

```
探している型: HttpMessageNotReadableException
  候補1: 基底クラスの handleException（この型が明記されている）→ 合致
  候補2: 自作の handleUnexpected（Exception.class）→ 親として合致
  → より具体的な候補1の勝ち
```

### Step 2: 振り分け係 `handleException`（基底クラス、Spring 作）

```java
else if (ex instanceof HttpMessageNotReadableException theEx) {
    return handleHttpMessageNotReadable(theEx, headers, HttpStatus.BAD_REQUEST, request);
}
```

instanceof の連鎖で種類を特定し、専門係を呼ぶ。400 はここで決まる。

### Step 3: 専門係 `handleHttpMessageNotReadable`（基底クラス、Spring 作）

```java
ProblemDetail body = createProblemDetail(ex, status, "Failed to read request", null, null, request);
return handleExceptionInternal(ex, body, headers, status, request);
```

この例外は自分で応答の材料を持たない（§7 参照）ので、専門係がここで**荷造り**する。

### Step 4: 梱包係 `handleExceptionInternal`

body（荷物）は入っているので、ProblemDetail + ヘッダ + 400 を `ResponseEntity` の箱に詰めて返す。

### Step 5: 箱が呼び出しの連鎖を逆向きに浮上し、送信される

行きは「呼び出し」で潜り、帰りは **return された箱が同じ道を浮上してくる**。

```
中枢
 └─▶ handleException（振り分け）
       └─▶ handleHttpMessageNotReadable（荷造り）
             └─▶ handleExceptionInternal（梱包）
             ◀── ResponseEntity の箱
       ◀── その箱をそのまま return
 ◀── その箱をそのまま return
中枢: 箱を開けて JSON に直列化し、クライアントへ送信
```

各層の `return handleXxx(...)` は「下請けの成果物を、加工せずそのまま上に渡す」の意味。

```json
HTTP/1.1 400
Content-Type: application/problem+json

{"detail":"Failed to read request","instance":"/events","status":400,"title":"Bad Request"}
```

**この流れで自作コードは1行も通っていない**ことに注目。継承した基底クラスの登録と処理だけで
ProblemDetail 形式の 400 応答が完成している。

---

## 7. null 派と荷物持参派 — Spring に2つの流儀がある理由

基底クラス内で梱包係を呼ぶ行は **19箇所**あり、渡し方が2派に分かれる（Spring 7.0.8 時点）。

```java
// 荷物持参派（5箇所）: ProblemDetail を作ってから渡す
ProblemDetail body = createProblemDetail(ex, status, "Failed to read request", ...);
return handleExceptionInternal(ex, body, headers, status, request);

// null 派（14箇所）: 手ぶらで渡す（404, 405, 415 など）
return handleExceptionInternal(ex, null, headers, status, request);
```

null 派の荷物は、梱包係の内部で例外自身から取り出される:

```java
// handleExceptionInternal の内部（Spring 作）
if (body == null && ex instanceof ErrorResponse errorResponse) {
    body = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
}
```

分かれている理由は例外の「身分」の違い。

- **null 派** = 生まれつき Web サーバのエラーである例外（404 の `NoResourceFoundException` など）。
  `ErrorResponse` インターフェースを実装し、**自分で応答の材料を持っている**。
- **荷物持参派** = Web 専用ではない汎用例外。JSON 変換器はクライアント側の通信でも使われるため、
  `HttpMessageNotReadableException` に「私は HTTP エラー応答です」と名乗らせるのは筋違い。
  「サーバのエラー応答である」という解釈は、Web 層のハンドラ側が後付けで荷造りする。

どちらも正当であり、**統一は出口（梱包係）で行われている**。

---

## 8. code 一括付与はどこに割り込むか — そしてハマった罠

ErrorCode enum 版では `handleExceptionInternal` を上書きし、Step 4 の入口で code を押してから
super（基底の梱包係）に渡す。19箇所の呼び出しすべてがここを通るため、1箇所の上書きで全経路に効く。

ただし null 派（404/405/415）は**上書き版が動く時点でまだ荷物が無い**。
荷解き（§7 の body 取り出し）は super の内部で、code 付与のあとに走るため、
そのままでは押す対象が存在しない。実測で 404/405/415 だけ code が付かなかったのはこのため。

対策は「super がやるはずだった荷解きを、上書き版の冒頭で**前倒し**する」:

```java
@Override
protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

    if (body == null && ex instanceof ErrorResponse errorResponse) {           // super 内部と同じ条件
        body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    }
    if (body instanceof ProblemDetail pd && !hasCode(pd)) {
        pd.setProperty("code", frameworkCodeFor(ex, statusCode).name());       // 今度は対象がいる
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
}
```

これが安全なのは、super 側の荷解きが `if (body == null && ...)` でガードされているから。
先に実体化して渡せば super は二重生成せず、渡した body をそのまま使う
（荷物持参派と同じ、Spring が公式にサポートしている渡し方に乗せ替えただけ）。

---

## 9. 教訓

1. **オーバーライドで割り込むときは、フックが呼ばれる時点の引数の状態を、
   基底クラスの呼び出し側まで遡って確認する。** シグネチャ（`Object body`）は
   嘘をつかないが、全部も語らない。呼び出し元を grep したら null を渡す一群がいた。
2. **境界部分は机上でなく実測で確認する。** 全エラー経路を curl で叩いたから
   404/405/415 の穴が見つかった。コードレビューだけでは気づけなかった。
3. **「統一は出口で」は Spring 自身の設計でもある。** 入口（例外の発生源）は
   多様なまま受け入れ、全員が通る一点で形式を揃える。code の一括付与も同じ手。
