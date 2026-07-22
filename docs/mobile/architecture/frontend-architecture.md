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

**ルーティング（`src/app/`）と実装（`src/features/` ほか）を分離する。** ルートディレクトリは Expo Router の `src/app` を使う（SDK 55+ の既定形。root の `app` より優先される）。

```
mobile/
  src/
    app/                       # ルーティングのみ。URL/ルート木を写すだけ（薄く保つ）
      (public)/signin.tsx      #   → features/auth の画面部品を呼ぶだけ
      (private)/index.tsx      #   → 各機能の画面部品を呼ぶだけ
      _layout.tsx              #   ルートガード（session に応じて出し分け）
      +not-found.tsx           #   → components の NotFoundPage を呼ぶだけ
    features/                  # 機能（ドメイン）単位。画面・部品・型・ルールを同居
      auth/                    #   サインインの画面・フロー（セッション状態は持たない）
      calendar/
    components/                # アプリ横断で使い回す汎用 UI（AppHeader, SignOutButton 等）
    hooks/                     # アプリ横断で使い回す汎用フック
    lib/                       # アプリ横断の基盤（非UIロジック）。誰でも依存してよい
      api/                     #   api クライアント
      session/                 #   セッション：Zustand ストア・signIn/signOut・getToken・useSession
    constants/                 # 定数・テーマ
  app.json / tsconfig.json / metro.config.js / package.json   # 設定ファイルは root に据え置き
```

- 設定ファイル（`app.json` / `package.json` / `metro.config.js` / `tsconfig.json`）と `public/` は **root に残す**。
- tsconfig の path エイリアスは **`@/*` → `./src/*`** に設定する（`@/features/auth/...` の形で参照）。

### ルートファイルは薄く保つ（原則）

`src/app/` の各ファイルは**ルート木を写すだけ**にし、中身は features / components の部品を呼ぶだけにする。ロジック・スタイル・フォーム定義をルートファイルに直書きしない。

```tsx
// src/app/(public)/signin.tsx — 例（薄いルートファイル）
export { default } from "@/features/auth/screens/SignInScreen";
```

`+not-found.tsx` が `NotFoundPage` を呼ぶ形が、このルールの基準例。

### feature の内部構成（例：auth）

feature は「画面」ではなく「ドメイン」で切る（`signin` ではなく `auth`。sign-in / sign-up などの画面・フローを内包する）。**セッション状態そのもの（ログイン状態・トークン・signIn/out）は feature ではなく基盤（`lib/session`）に置き**、auth はそれを利用する（下記「session は基盤」を参照）。

```
features/auth/
  screens/
    SignInScreen/
      SignInScreen.tsx       # 画面の組み立て
      index.ts               # export { default } from "./SignInScreen";
  components/
    SignInForm/
      SignInForm.tsx         # その機能の UI 部品（StyleSheet は末尾に同居でよい）
      index.ts               # export { SignInForm } from "./SignInForm";
  hooks/useSignInForm.ts     # フォームのロジック（RHF setup + onSubmit。lib/session の signIn を呼ぶ）
  model/schema.ts            # zod スキーマ + 型
```

### session は「機能」ではなく「基盤」

ログイン状態・トークン・`signIn`/`signOut`/`getToken`/`useSession` は、api クライアント・ルートガード・ヘッダなど**アプリ全体が依存する土台**なので、feature ではなく `lib/session`（基盤）に置く。これを feature に閉じ込めると「他が使う＝feature への越境」を招く。基盤に置けば、誰が依存しても向きは `→ lib/session` で正しい。

- `signOut` などの副作用（トークン消去等）は `lib/session` の中に集約し、UI はその関数を呼ぶだけにする（UI にストレージ消去などを直書きしない）。
- サインアウトボタンのような汎用 UI（`components/SignOutButton`）は基盤 `lib/session` に依存してよい。純粋な `components/AppHeader` に対しては、`right` スロットへ app 層（`(private)/_layout`）が `SignOutButton` を注入して合成する。

**コンポーネント/画面は「フォルダ + `index.ts` バレル」で 1 単位にする。** 各フォルダの `index.ts` から re-export し、import はフォルダパスで行う（例：`@/features/auth/components/SignInForm`）。実体ファイル名の変更や内部ファイル追加が、外部の import に波及しない。`src/components/errors/NotFoundPage/` も同じ形。

---

## 3. 依存の向きと境界ルール

本プロジェクトで守るべき原則は以下。

1. **Colocation（近くに置く）。** ある機能に属す画面・部品・状態・型・ルールは、その feature フォルダにまとめて置く。「components に全部品、hooks に全 hook」という**種類別ダンプは feature 内では作らない**。
2. **feature 同士は直接依存しない。** 複数 feature で共有したくなったものは、性質に応じて top-level へ降ろす（汎用 UI → `components`、汎用フック → `hooks`、非UIの基盤 → `lib`）。feature A が feature B の内部を import しない。**「複数 feature が依存する土台」は feature ではなく基盤（`lib`）に置く**（例：セッションは `lib/session`）。境界を誤って基盤を feature に閉じ込めると越境が生じる。
3. **top-level の `components` / `hooks` / `lib` は「アプリ横断のものだけ」に限定する。** 機能固有の部品・フックは必ず feature 内へ。ここを緩めると種類別ダンプに逆戻りする。1 箇所でしか使わないものは feature 内に留める。
4. **依存の向きは一方向とする。**

```
src/app/ (ルーティング)  →  features/  →  components / hooks  →  lib（基盤：api, session）
```

`lib`（基盤）は最も依存される層で、内部（features 等）には依存しない。逆流（`lib` や `components` が feature を知る、`feature` が `app` を知る）を作らない。依存の向きを制御するという考え方はクリーンアーキから取り入れる（層は設けないが、向きは規定する）。

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

### hook にするもの / しないもの

- **React の状態・再描画に反応して使うもの → hook**（例：`useSession`、Zustand セレクタ）。
- **命令的に呼ぶ／React の外から呼ぶもの → ただの関数**（例：`signIn`、`getToken`）。`getToken` は `lib/api` の fetch ラッパ（React ではない）から呼ぶため、関数のままにする。

### 導入タイミング（段階化）

採用は確定。ただし各ライブラリは最初に必要になった箇所で導入する。

| ライブラリ | 最初の利用箇所 | 第一段階で入れるか |
|---|---|---|
| Zustand | 認証セッション（トークン保持） | 入れる |
| react-hook-form + zod | ログイン画面のフォーム | 入れる |
| React Query | API 由来データ（Event/Task 一覧） | 入れない（API 未着手のため）。置き場所（各 feature の `api/`）のみ定義し、API 着手時に導入 |

---

## 6. 反パターン（避ける構成）

- ルートファイル（`src/app/`）にロジック・スタイル・フォーム定義を直書きする
- top-level の巨大な `components/` / `hooks/`（機能固有まで入れた種類別ダンプ）
- feature 同士の相互 import
- 1 箇所でしか使わないものの top-level への早期移動
- サーバーデータを Zustand に抱え込む
- feature を「画面」単位で切る（`signin`）。「ドメイン」単位（`auth`）で切る

---

## 7. 第一段階スコープ（walking skeleton）

最初に作るのは個別機能ではなく、構成を薄く 1 本貫くスケルトンとする。第一段階の目的は、動作そのものよりも feature-based の置き場所を確定させることに置く。

### ゴール（UI のみ）

- `src/app/(public)/signin` をデザイン込みで整える（中身は feature の部品）。
- ログインボタンは認証せず、セッションを立てて `(private)` へ遷移させる（[authentication.md](./authentication.md) のスタブを利用）。
- `(private)` の遷移後画面が表示される。この画面はカレンダーである必要はない（カレンダー描画は次段階の確認テーマ）。

### 第一段階では対象外

- 実際の認証（Firebase 本体の配線）
- データの永続化 / API 呼び出し / React Query
- カレンダー本実装

### この段階で用意する置き場所・継ぎ目

後から本実装を差し込めるよう、以下をスタブで用意する（詳細は [authentication.md](./authentication.md)）。

- `src/lib/session/`（基盤：`store` = Zustand セッション、`signIn`/`signOut`、`getToken`、`useSession`。バレル `index.ts` から公開）
- `src/features/auth/screens/SignInScreen/` ＋ `components/SignInForm/` ＋ `hooks/useSignInForm.ts` ＋ `model/schema.ts`（auth 機能。`lib/session` を利用）
- `src/lib/api/client.ts`（Bearer 付与を 1 箇所に集約。`lib/session` の getToken を使う）
- `src/components/AppHeader/`（純粋 UI）＋ `src/components/SignOutButton/`（`lib/session` の signOut を呼ぶ）
- `src/app/_layout.tsx` のルートガード（`lib/session` の useSession を参照）、`(private)/_layout.tsx` で AppHeader に SignOutButton を注入、各ルートは薄く画面部品を呼ぶだけ

### 完了条件

「ログイン画面 → ボタン押下 → private 遷移 → 遷移後画面が見える」が動作し、上記の置き場所・継ぎ目が本ドキュメントの原則（薄いルート・feature 内 colocation・一方向依存）に沿って配置されていること。

---

## 8. 未確定 / 要検証

- **カレンダー描画ライブラリの選定**（次段階）。候補：flash-calendar（FlashList ベース・カスタマイズ前提・New Architecture 対応）／react-native-calendars（普及・`dayComponent` で自作可）。いずれも自前 `<MonthCalendar>` で包み、差し替え可能にする。Expo SDK 56 / RN 0.85 / React 19（New Architecture）互換は実装前に v56 versioned docs で要検証。
- 状態管理各ライブラリの Expo SDK 56 互換（実装前に versioned docs で確認）。

> 本プロジェクトの規約（[../../../mobile/AGENTS.md](../../../mobile/AGENTS.md)）に従い、コードを書く前に https://docs.expo.dev/versions/v56.0.0/ を確認する。
