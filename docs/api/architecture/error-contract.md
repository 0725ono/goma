# ⚠️ エラー応答の契約

API が返すエラーの形式と、クライアントがそれをどう扱うかの取り決め。
エラー処理の内部規則（catch の分類・翻訳の境界）は [backend-architecture.md](./backend-architecture.md) §6、
モバイル側の受け取り方は [mobile/src/lib/api/errors.ts](../../../mobile/src/lib/api/errors.ts) を参照。

---

## 1. 形式: RFC 9457 (Problem Details)

すべてのエラー応答は `application/problem+json` で返す。独自形式は作らない。
翻訳は [GlobalExceptionHandler](../../../api/src/main/java/com/familyschedule/api/shared/interfaces/GlobalExceptionHandler.java) が一手に引き受ける。

標準フィールドに加え、2 つの**拡張メンバー**を定義している。

| フィールド | 出典 | 用途 |
|---|---|---|
| `status` `title` `detail` `instance` | RFC 9457 標準 | HTTP ステータス、概要、詳細、発生したパス |
| `code` | 拡張 | **機械可読なエラー識別子。クライアントはこれで分岐する** |
| `errors` | 拡張 | フィールド別の検証エラー一覧（`VALIDATION_ERROR` 時のみ） |

**`code` はすべてのエラー応答に必ず付く。** フレームワークが処理するエラー
（壊れた JSON・未知のパス・未対応メソッド等）にもハンドラが一括付与する。

---

## 2. エラーコードの一覧と真実の置き場

**一覧の真実は [ErrorCode enum](../../../api/src/main/java/com/familyschedule/api/shared/domain/ErrorCode.java) にある。**
このドキュメントには一覧を書かない（手書きの一覧は必ず現実と乖離するため。
実際、初版の一覧には HTTP 経由では到達不能なコードが 2 つ載っていた）。

定義は 3 箇所で同期する。

| 場所 | 役割 | 同期の担保 |
|---|---|---|
| [ErrorCode.java](../../../api/src/main/java/com/familyschedule/api/shared/domain/ErrorCode.java) | **真実**。code と HTTP ステータスの対応を持つ | — |
| [errorCodes.ts](../../../mobile/src/lib/api/errorCodes.ts) | TypeScript 側の写し。feature 層の分岐をタイポ安全にする | [ErrorCodeContractTest](../../../api/src/test/java/com/familyschedule/api/shared/domain/ErrorCodeContractTest.java) が一致を検証（`make test`） |
| この文書 | 「なぜ」と規則だけを書く | 人間（コードに現れないことのみ書くため乖離しにくい） |

言語が別である以上、定義の二重化は避けられない。大事なのは無くすことではなく
**「どちらが真実か」を決め、黙って乖離しない仕組みを置く**ことである。

---

## 3. クライアントは `code` で分岐する

**`detail` の文言や `status` の値で分岐してはいけない。**

- `detail` は人間向けの説明であり、文言はいつでも変わりうる
- `status` は粒度が粗い。400 だけでも「検証違反」「壊れた JSON」「ドメインルール違反」があり、
  どの画面要素にエラーを出すべきかは status からは決められない

`code` は API 契約の一部として**後方互換性を維持する**。
一度公開した `code` の意味は変えない。意味が変わるなら新しい `code` を作る。

```ts
// mobile 側の分岐イメージ（ErrorCode union 型によりタイポはコンパイルエラーになる）
if (e instanceof ApiError && e.code === "EVENT_TIME_INVALID") {
  setError("endAt", "終了時刻は開始時刻より後にしてください");
}
```

### 使い分けの目安（受け取り側）

| code | クライアントの扱い |
|---|---|
| `VALIDATION_ERROR` | `errors[]` を使ってフォームの項目別にエラー表示 |
| ドメインルール系（`EVENT_TIME_INVALID` 等） | ルールに対応した個別の案内 |
| `MALFORMED_REQUEST` / `RESOURCE_NOT_FOUND` / `METHOD_NOT_ALLOWED` | クライアント実装のバグの疑い。汎用エラー表示 + 開発時に気づくためのログ |
| `REQUEST_REJECTED` | 個別の案内を持たない 4xx の受け皿。汎用エラー表示 |
| `INTERNAL_ERROR` | 「時間をおいて再試行」の汎用表示。ユーザーにできることはない |

---

## 4. 命名と設計の規則

- **命名は `<対象>_<事象>`**、大文字スネークケース（例: `EVENT_TIME_INVALID`）。
  対象には [ユビキタス言語](../../ubiquitous-language.md) の英語名を使う。
  対象を持たない横断的なものは対象を省く（例: `VALIDATION_ERROR`）
- **code を持てるのは「HTTP まで実際に届きうる」エラーだけ**。
  DTO が先に弾くため到達不能な domain 側のガードには code を与えない
  （[backend-architecture.md](./backend-architecture.md) §5 の規則）
- **HTTP ステータスとの対応は enum 自身が持つ**（`ErrorCode.status()`）。
  ただしフレームワーク例外に code を付ける経路では、実際のステータスは Spring が決めた値を維持する
  （例: 415 は `REQUEST_REJECTED` を付けても 415 のまま）
- **意図的に分類しない受け皿がある**: 個別の案内を用意する価値がないフレームワーク例外は
  `REQUEST_REJECTED` に落ちる。「分類しない」という判断を明示するためのコードであり、
  status の言い換えコード（`BAD_REQUEST` のような情報量ゼロのもの）は作らない

## 5. HTTP ステータスの使い分け

判断基準は「**誰が直すべきエラーか**」である。

| ステータス | 意味 | 誰が直すか |
|---|---|---|
| **400 番台** | 入力・リクエストが不正、またはビジネスルール違反 | クライアント側（ユーザーの入力修正、または実装バグ修正） |
| **500** | 想定外の失敗 | 開発者（コードを直す） |

500 のとき、応答には一般的な文言しか載せない。例外の詳細とスタックトレースは
サーバログにのみ出力する（内部実装を外部に漏らさない）。
4xx の `detail` はデバッグに有益なため隠さない。

---

## 6. 追加・変更の手順

1. [ErrorCode.java](../../../api/src/main/java/com/familyschedule/api/shared/domain/ErrorCode.java) に定数を追加（javadoc で発生条件を書く）
2. [errorCodes.ts](../../../mobile/src/lib/api/errorCodes.ts) に同じ名前を追加
3. throw 箇所を書く（domain のルールなら `DomainRuleViolationException`、
   フレームワーク例外の分類なら `GlobalExceptionHandler` の `FRAMEWORK_CODES`）
4. `make test` — 同期テストが 1 と 2 の一致を検証する
5. この文書の §3 の表に受け取り方を足す（クライアントの扱いが変わる場合）

---

## 7. 未定義のもの

以下はまだ存在しない。実装時に追加する。

- **401 / 403** — 認証・認可が未実装のため。`/events` は現在だれでも呼べる。
  Spring Security のフィルタ層は GlobalExceptionHandler を通らないため、
  導入時に AuthenticationEntryPoint 側での code 付与を設計する（backend-architecture.md §9）
- **リソース単体の 404** — `GET /events/{id}` のようなエンドポイントがまだ無い。
  追加時はルーティングの `RESOURCE_NOT_FOUND` と区別するか検討する
- **409（競合）** — 競合を扱うユースケースがまだ無い

---

## 8. OpenAPI を導入する判断基準

現在この契約は「enum + 手書きの規則」で管理している。エンドポイントが 2 つしかなく、
自動生成では表現できない「なぜ」にこそ価値があるためである。

以下のいずれかに当てはまったら springdoc-openapi（コードから仕様を自動生成）の導入を検討する。

- モバイル以外のクライアントが増える
- エンドポイントが増え、一覧を手で保守するのが現実的でなくなる
- API を実装する人と使う人が分かれる
- **api と mobile のリポジトリ / CI が分かれ、相対パス参照の同期テストが成立しなくなる**

導入した場合も、この文書は残す。生成される仕様に「なぜ」は書けないためである。
