# 🏛 フロントエンド アーキテクチャ方針

React Native (Expo SDK 56 / TypeScript) 製モバイルアプリの**全体設計**。
認証まわりは [authentication.md](./authentication.md) に分離。システム全体は
[../../architecture.md](../../architecture.md) を参照。

---

## 1. 採用するアーキテクチャ：feature-based

本プロジェクトのフロントエンドは **feature-based（機能単位）アーキテクチャ**を採用する。
バックエンド（Java）で採用する DDD / クリーンアーキテクチャはフロントには適用しない。

### 判断の背景（トレードオフ）

DDD / クリーンアーキとの間で検討し、複雑さの出どころの違いから feature-based を採用した。

| | 複雑さの出どころ | 適した構造 |
|---|---|---|
| バックエンド | ビジネスルール・不変条件・永続化・整合性 | ルールを中心に据え、外側（DB/Web）を差し替え可能にする DDD / クリーンアーキ |
| フロントエンド | UI 状態・ユーザー操作・画面遷移・非同期データ・描画 | 機能単位でまとめる feature-based |

フロントは「API を叩いて画面に出す」クライアントであり、Repository / UseCase / DI の層を設けても、主要な複雑さ（状態と描画）には寄与せず、層を跨ぐ変換とユーティリティが増える。これは本プロジェクトの方針（無駄なユーティリティを増やさない）に反する。以上より feature-based を採用する。

---

## 2. ディレクトリ構成

`app/` は expo-router のルーティング配線に限定し、ロジックは `src/` 側に置く。

```
mobile/
  app/                     # expo-router のルート（配線のみ）
    (public)/signin.tsx    #   未ログイン領域
    (private)/index.tsx    #   ログイン後領域
    _layout.tsx            #   セッション状態で (public)/(private) を出し分け（ルートガード）
  src/
    features/              # 機能単位。画面・部品・状態・型・ルールを機能ごとに同居
      auth/                #   認証（セッション・ログイン・トークン継ぎ目）
      calendar/            #   カレンダー（Event/Task モデル + 表示）
    shared/                # 横断的に再利用するものだけを置く
      ui/                  #   汎用 UI 部品
      lib/                 #   api クライアント等の横断ロジック
      config/              #   環境設定・定数
```

各 feature の内部構成の目安：

```
features/<feature>/
  components/   # その機能の UI 部品
  hooks/        # その機能のロジック（状態・副作用）
  model/        # ドメイン型 + 純粋関数（§4 参照）
  api/          # その機能の API 呼び出し（React Query の queryFn 等）
  store.ts      # その機能のクライアント状態（Zustand。必要な機能のみ）
```

---

## 3. 依存の向きと境界ルール

本プロジェクトで守るべき原則は以下。

1. **Colocation（近くに置く）。** ある機能に属す画面・部品・状態・型・ルールは、その feature フォルダにまとめて置く。「components に全部品、hooks に全 hook、utils に全関数」という種類別フォルダは採らない。
2. **feature 同士は直接依存しない。** 共有したいものは `shared/` に降ろす。feature A が feature B の内部を import しない。
3. **`shared/` は薄く保つ。** 「2 つ以上の feature が実際に使っている」ものだけを降ろす。1 箇所でしか使わないものは feature 内に留める。
4. **依存の向きは一方向とする。**

```
app/ (ルーティング)  →  features/  →  shared/
```

逆流（`shared` が feature を知る、`feature` が `app` を知る）を作らない。依存の向きを制御するという考え方はクリーンアーキから取り入れる（層は設けないが、向きは規定する）。

---

## 4. ドメインモデル：model + 純粋関数

層（Repository / UseCase / DI）は設けないが、ドメインモデルとビジネスルールはコードとして持つ。
`Event` / `Task` を型だけで終わらせず、型 + 純粋関数として feature 内に同居させ、ルールが UI コンポーネントに散らないようにする。

```
features/calendar/model/
  event.ts   # 型 Event + isValid(開始 < 終了) などの純粋関数
  task.ts    # 型 Task  + isOverdue(now) などの純粋関数
```

- Repository / UseCase / DI は用いない。型と純粋関数のみ。
- バリデーションは **zod** で書き、フォーム検証（react-hook-form）とドメイン検証でスキーマを共有する。

---

## 5. 状態管理

状態を種類で分けて扱う。すべてを 1 つのストアに入れない。

| 状態の種類 | 例 | 使うもの |
|---|---|---|
| **クライアント状態**（自分が持つ UI/セッション状態） | ログインセッション、選択中の月、表示中のスペース | **Zustand** |
| **サーバー状態**（API 由来のデータ） | Event 一覧、Task 一覧、ユーザー情報 | **TanStack Query (React Query)** |
| **フォーム状態** | ログイン、予定/タスクの入力 | **react-hook-form + zod** |

いずれも RN で動作する（Zustand は DOM 非依存。react-hook-form は RN の `TextInput` を `<Controller>` でラップして使う）。

サーバーデータは Zustand に持たせない。キャッシュ・再取得・ローディング・エラーの管理は React Query が担う。

### 導入タイミング（段階化）

採用は確定。ただし各ライブラリは最初に必要になった箇所で導入する。

| ライブラリ | 最初の利用箇所 | 第一段階で入れるか |
|---|---|---|
| Zustand | 認証セッション（トークン保持） | 入れる |
| react-hook-form + zod | ログイン画面のフォーム | 入れる |
| React Query | API 由来データ（Event/Task 一覧） | 入れない（API 未着手のため）。置き場所（各 feature の `api/`）のみ定義し、API 着手時に導入 |

---

## 6. 反パターン（避ける構成）

- 巨大な共通 `components/` / `utils/`（種類別フォルダ）
- feature 間の相互 import
- 1 箇所でしか使わないものの `shared/` への早期移動
- サーバーデータを Zustand に抱え込む
- ルーティング（`app/`）にビジネスロジックを書く

---

## 7. 第一段階スコープ（walking skeleton）

最初に作るのは個別機能ではなく、構成を薄く 1 本貫くスケルトンとする。第一段階の目的は、動作そのものよりも feature-based の置き場所を確定させることに置く。

### ゴール（UI のみ）

- `(public)/signin` をデザイン込みで整える。
- ログインボタンは認証せず、セッションを立てて `(private)` へ遷移させる（[authentication.md](./authentication.md) のスタブを利用）。
- `(private)` の遷移後画面が表示される。この画面はカレンダーである必要はない（カレンダー描画は次段階の確認テーマ）。

### 第一段階では対象外

- 実際の認証（Firebase 本体の配線）
- データの永続化 / API 呼び出し / React Query
- カレンダー本実装

### この段階で用意する継ぎ目

後から本実装を差し込めるよう、以下の継ぎ目をスタブで用意する（詳細は [authentication.md](./authentication.md)）。

- `features/auth` のセッションストア（Zustand）
- `signIn()` / `signOut()`（中身はダミー）
- `getToken()`（ダミー値を返す。将来 Firebase の `getIdToken()` に差し替え）
- `shared/lib/api` の API クライアント（Bearer 付与は 1 箇所に集約）
- `app/_layout` のルートガード

### 完了条件

「ログイン画面 → ボタン押下 → private 遷移 → 遷移後画面が見える」が動作し、上記の置き場所・継ぎ目が本ドキュメントの原則に沿って配置されていること。

---

## 8. 未確定 / 要検証

- **カレンダー描画ライブラリの選定**（次段階）。候補：flash-calendar（FlashList ベース・カスタマイズ前提・New Architecture 対応）／react-native-calendars（普及・`dayComponent` で自作可）。いずれも自前 `<MonthCalendar>` で包み、差し替え可能にする。Expo SDK 56 / RN 0.85 / React 19（New Architecture）互換は実装前に v56 versioned docs で要検証。
- 状態管理各ライブラリの Expo SDK 56 互換（実装前に versioned docs で確認）。

> 本プロジェクトの規約（[../../../mobile/AGENTS.md](../../../mobile/AGENTS.md)）に従い、コードを書く前に https://docs.expo.dev/versions/v56.0.0/ を確認する。
