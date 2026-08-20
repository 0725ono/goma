# 🏛 バックエンドアーキテクチャ

Java API のレイヤ構成・パッケージ規約・依存の向き・検証とエラー処理の規則。
エラー応答の契約（クライアントとの約束）は [error-contract.md](./error-contract.md)、
システム全体の方針は [../../architecture.md](../../architecture.md) の「6. バックエンド設計思想」、
ドメイン用語は [../../ubiquitous-language.md](../../ubiquitous-language.md) を参照。

使用しているフレームワークとそのバージョンは [api/pom.xml](../../../api/pom.xml) が正。

---

## 1. 基本方針

クリーンアーキテクチャ + DDD を採用するが、**過度な複雑化は避ける**。
CQRS やイベントソーシングは採用せず、UseCase を中心とした素直なレイヤードアーキテクチャとする。

守りたいのは 1 点だけである。

> **ビジネスルールを、フレームワークとデータベースから独立させる。**

Spring も JPA も PostgreSQL も、いずれ差し替わりうる「外側の都合」である。
「終了日時は開始日時より後」のようなルールは、それらが変わっても変わらない。
だからルールを最内層に置き、外側から内側へだけ依存させる。

---

## 2. パッケージ構成

**まず機能で切り、その中を層で切る**（縦スライス）。層で切ってから機能で分けると、
1 つの機能を追う際にパッケージツリー全体を横断することになるため採らない。

```
com.familyschedule.api
├── schedule/                  ← 機能のまとまり（予定・タスクなど時間の管理）
│   ├── domain/event/          ← ビジネスルール（最内層）
│   ├── application/event/     ← ユースケース
│   ├── infrastructure/event/  ← 永続化の実装
│   └── interfaces/event/      ← HTTP エンドポイント
└── shared/                    ← 機能をまたいで使うもの
    ├── domain/                    例外の基底クラス、ErrorCode など
    └── interfaces/                例外ハンドラ、CORS 設定など
```

`shared` は「複数の機能から実際に使われているもの」だけを置く。
「将来使いそう」で先に置かない。

---

## 3. 依存の向き

```
interfaces  ──→  application  ──→  domain  ←──  infrastructure
 (HTTP)          (ユースケース)     (ルール)      (DB)
```

**矢印は必ず内側（domain）を向く。** domain から外側へ向かう依存は作らない。

infrastructure だけ向きが逆に見えるが、これは**依存性逆転**である。
domain が「こう保存してほしい」というインターフェース（[EventRepository](../../../api/src/main/java/com/familyschedule/api/schedule/domain/event/EventRepository.java)）を定義し、
infrastructure がそれを実装する（[EventRepositoryAdapter](../../../api/src/main/java/com/familyschedule/api/schedule/infrastructure/event/EventRepositoryAdapter.java)）。
これにより domain は JPA を知らないまま、永続化を利用できる。

| 層 | 責務 | 置いてはいけないもの |
|---|---|---|
| **domain** | エンティティ、ビジネスルール、リポジトリのインターフェース | Spring / JPA / HTTP に関するもの一切（import が 1 つでもあれば設計ミス） |
| **application** | ユースケースの進行、ID の採番、トランザクション境界 | ビジネスルールそのもの（domain に置く）、HTTP の知識 |
| **infrastructure** | JPA エンティティ、リポジトリ実装、外部サービス呼び出し | ビジネスルール |
| **interfaces** | リクエスト/レスポンス DTO、Controller、例外ハンドラ | ビジネスルール、永続化の知識 |

Controller は「usecase を呼び、DTO に詰め替えて返すだけ」の薄い層に保つ。
Controller が厚くなり始めたら、それは application 層に移すべきロジックが漏れている合図である。

---

## 4. データの入れ物 — 同じ「予定」に 4 つの形がある

「予定を作る」データは、層を通過するたびに入れ物を替える。
冗長ではなく、**各層が自分の都合を自分の入れ物に閉じ込める**ための設計である。

```
HTTP JSON
   │  Spring が変換し、@Valid で検査      ← 形式バリデーションはここ（§5）
   ▼
CreateEventRequest   … interfaces。HTTP 契約の形（DTO）
   │  Controller が詰め替え
   ▼
CreateEventCommand   … application。ユースケースへの入力
   │  UseCase が id を採番して生成
   ▼
Event                … domain。不変条件を持つ本体
   │  Adapter が変換
   ▼
EventEntity          … infrastructure。テーブルの形
```

| 入れ物 | 層 | 変わる理由 |
|---|---|---|
| [CreateEventRequest](../../../api/src/main/java/com/familyschedule/api/schedule/interfaces/event/CreateEventRequest.java) | interfaces | API 契約が変わったとき |
| [CreateEventCommand](../../../api/src/main/java/com/familyschedule/api/schedule/application/event/CreateEventCommand.java) | application | ユースケースの要件が変わったとき |
| [Event](../../../api/src/main/java/com/familyschedule/api/schedule/domain/event/Event.java) | domain | ビジネスルールが変わったとき |
| [EventEntity](../../../api/src/main/java/com/familyschedule/api/schedule/infrastructure/event/EventEntity.java) | infrastructure | スキーマが変わったとき |

Request と Command は現状フィールドがほぼ同じだが、統合しない。
統合すると application が interfaces を import することになり依存が逆流する。
またいずれ必ずズレる（例: 認証導入後、spaceId は Request から消えてトークン由来になるが Command には残る）。

**ドメインモデルと JPA エンティティを分ける理由**: JPA エンティティは引数なしコンストラクタと
可変フィールドを要求する。1 つのクラスで兼ねると、ドメインモデルが「未検証の状態で生成でき、
あとから書き換えられる」ものになり、「生成された時点で必ず正しい」という保証が失われる。

---

## 5. 検証の置き場所 — 機械的な判定規則で決める

検証がどこにあるかを記憶で管理しない。次の規則で置き場所が一意に決まる。

> **その検査は、フィールド 1 個だけを見て判定できるか？**
> - できる（null・空・長さ・型・形式）→ **DTO** のアノテーション（`@NotBlank` `@Size` 等）
> - できない（複数フィールドの関係・DB の状態が要る）→ **domain**（または usecase）

探すときも同じ規則で逆引きする。「タイトルの長さ制限どこ？」→ 単一フィールドだから DTO。
「終了 > 開始のチェックどこ？」→ 複数フィールドだから domain。

同じルールが複数の層に現れるが、それは重複ではなく**各層が自分の目的のために持っている**（多重防御）。

| 層 | 例 | 目的 | 破られたら |
|---|---|---|---|
| mobile のフォーム | 入力欄の maxLength | UX。往復なしの即時フィードバック | —（そもそも送らない） |
| DTO | `@Size(max = 255)` | 契約。クライアントを信用しない | 400 `VALIDATION_ERROR` + `errors[]` |
| domain | コンストラクタのガード | 不変条件。どの経路から来ても Event は常に正しい | 500（= 実装バグの検知） |
| DB | `VARCHAR(255)`, `CHECK` 制約 | アプリを経由しない書き込みへの最後の防波堤 | SQL エラー |

**注意**: DTO が先に弾く検査（null・空など）は、domain に届いた時点で実装バグである。
そのため domain 側のガードは `IllegalArgumentException`（→ 500）とし、
**API 契約用のエラーコードを与えない**。エラーコードを持つのは
「HTTP まで実際に届きうる」ルール違反（`DomainRuleViolationException`）だけである。
これを守らないと、カタログに「到達不能なエラーコード」が並び、契約が現実と乖離していく。

---

## 6. エラー処理 — 「行き先の保証」であって「catch すること」ではない

エラーハンドリングとは **すべてのエラーに最終的な行き先を保証すること**であり、
catch はその手段の 1 つにすぎない。「伝播させる」のも立派なハンドリング戦略である。

### 6.1 エラーコードは境界にだけ現れる

> **エラーコードは「境界」にだけ現れる。内側は例外の型で語り、外側への翻訳は 1 箇所で行う。**

```
domain      : throw new DomainRuleViolationException(ErrorCode.X, "...")  ← code を宣言する唯一の場所
application : ErrorCode を一切触らない。catch もしない
interfaces  : GlobalExceptionHandler が例外 → ProblemDetail へ翻訳       ← HTTP に変換する唯一の場所
```

code と HTTP ステータスの対応は [ErrorCode](../../../api/src/main/java/com/familyschedule/api/shared/domain/ErrorCode.java) 自身が持つ。
新しいエラーを追加するとき触るのは **enum と throw 箇所の 2 箇所だけ**になる
（+ TS の写しとドキュメント。[error-contract.md](./error-contract.md) を参照）。

### 6.2 catch の分類 — 既定は「catch しない」

```java
// ① 握りつぶし —— 禁止。エラーが消滅する
catch (E e) { }

// ② ログして続行 —— ほぼ禁止。処理していないのに成功したフリになる
catch (E e) { log.error(...); }

// ③ ログして投げ直し —— 避ける。ログが重複する
catch (E e) { log.error(...); throw e; }

// ④ 翻訳 —— 境界でのみ許可。意味を変えて投げ直す。原因(e)を必ず繋ぐ
catch (E e) { throw new DomainRuleViolationException(CODE, "...", e); }

// ⑤ catch しない —— 既定。境界のハンドラが 1 回だけログ + 応答
```

未捕捉の例外は消えない。伝播して GlobalExceptionHandler の最後の砦に到達し、
そこで **スタックトレース付きのログが必ず 1 回**出る。各層で catch してログを出すと
同じエラーが何度も記録され、ログが読めなくなる（log-and-rethrow アンチパターン）。

> **ログは、そのエラーを最終処理する場所で 1 回だけ。**

4xx はクライアント入力の問題なので控えめに、5xx は開発者が直すものなので
スタックトレース必須。この使い分けはハンドラが既に行っている。

### 6.3 infra 層の規則 — 握りつぶさない・意味があるときだけ翻訳

> **infra は握りつぶさない。DB の例外が「ドメインの意味」を持つときだけ、翻訳して投げ直す。それ以外は素通し。**

- **接続断・タイムアウト・SQL エラー** — ドメインの意味なし。catch しない。
  unchecked のまま伝播して 500 になり、詳細はサーバログに残る。
- **DB エラーがビジネス上の意味を運ぶ場合** — 唯一の例外。
  例えば UNIQUE 制約違反が「重複」というドメインの事象を意味するなら、
  adapter が技術例外をドメイン例外へ翻訳する（④）。呼び出し側は JPA の存在を知らずに済む。

現時点の実装に翻訳が必要なケースは存在しないため、該当コードはまだ無い。
最初の該当ケース（おそらくスペース実装時の UNIQUE 制約）で初めて書く。先回りで catch を書かない。

### 6.4 やってはいけない形

- **中間層の catch & rethrow** — application 層が catch して詰め替えると、経路が追えなくなり翻訳箇所が増殖する
- **Controller の try-catch** — 翻訳は GlobalExceptionHandler の独占業務。Controller に try-catch が現れたら設計が崩れ始めた合図
- **code とステータスの対応を呼び出し側に書く** — 対応は ErrorCode 自身が持つ

---

## 7. スキーマ管理

**スキーマの管理は Flyway が単独で担う**。JPA は `ddl-auto=validate` とし、
テーブルを勝手に作り変えさせない。スキーマ変更は必ずマイグレーションファイルで行う。

ドメインルールのうち DB で表現できるもの（`CHECK` 制約・`NOT NULL` 等）は
DB 側にも置く（§5 の多重防御）。

---

## 8. 新しい機能を追加する手順

Task を追加する場合を例にすると、以下の順に作る。**内側から外側へ**進めるのが原則である。

1. [ubiquitous-language.md](../../ubiquitous-language.md) に用語を足す（未登録の概念の場合）
2. `db/migration/V*__create_tasks.sql` — テーブルとマイグレーション
3. `schedule/domain/task/Task.java` — ルールを持つドメインモデル
4. `schedule/domain/task/TaskRepository.java` — 永続化のインターフェース
5. `schedule/infrastructure/task/` — JPA エンティティ・実装・変換
6. `schedule/application/task/` — ユースケース
7. `schedule/interfaces/task/` — DTO と Controller

新しいエラーコードは、ErrorCode enum・TS の写し・error-contract.md の 3 点を更新する
（enum と TS の一致は同期テストが検証する。詳細は [error-contract.md](./error-contract.md)）。

---

## 9. まだ決めていないこと

以下は実装時に決める。先回りして書かない。

- **テスト方針** — 現在は起動確認と ErrorCode 同期テストのみ。どの層をどの粒度で検証するか、
  DB を伴うテストをどう扱うかは未定
- **認証とスペース解決** — Firebase JWT の検証をどこで行うか（Filter か Interceptor か）、
  UID からスペースをどう解決するか。方針は [../../mobile/architecture/authentication.md](../../mobile/architecture/authentication.md) にある。
  なお Spring Security のフィルタ層で発生する 401/403 は DispatcherServlet に到達せず
  GlobalExceptionHandler を通らないため、導入時に AuthenticationEntryPoint 側での
  code 付与を設計する必要がある
- **トランザクション境界** — 現在は単一集約の操作しかないため明示していない
