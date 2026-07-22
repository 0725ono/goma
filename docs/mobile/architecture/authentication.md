# 🔐 認証方針（Firebase Authentication）

モバイルアプリの認証・トークンの流れ・トークン保管の設計。フロント全体の構成は
[frontend-architecture.md](./frontend-architecture.md)、システム全体の方針は
[../../architecture.md](../../architecture.md) を参照。

---

## 1. 全体像：Firebase を「外部の認証サーバー」として使う

Firebase Authentication を **IdP（本人確認と JWT 発行）専用の外部サービス**として使い、
アプリのビジネスデータは自前の PostgreSQL で持つ（[../../architecture.md](../../architecture.md) の方針）。

- Firebase は **API を公開しているマネージドサービス**。認証サーバーを自分で立てずに済む。
- 認証（メール/パスワード・Google 等の SNS ログイン）は**無料枠が大きく**、開発・練習・家族利用の規模なら実質無料（SMS 認証のみ有料）。※正確な料金は実装前に料金ページで確認。

> これは「認証を独立したサーバーに分離する」構成を、サーバーを自前運用せずに達成する形。identity を完全に外出しするため、アプリ DB に認証テーブルを同居させずに済む。

---

## 2. 登場人物と責務

**ユーザー情報は 2 箇所に別々に存在し、Firebase UID で連結する。**

| | 持ち主 | 中身 | 照合する主体 |
|---|---|---|---|
| **Firebase ユーザーストア** | Google（外部） | メール/パスワードのハッシュ、SNS 連携、UID | **ログイン時に Firebase 自身** |
| **PostgreSQL** | 自前 | UID をキーにしたプロフィール・所属スペース等（**パスワードは保存しない**） | **API アクセス時に Java** |

- **Firebase は自前 DB を一切見ない。** 両者をつなぐ唯一の糸が **Firebase UID**。
- Java 側は初回ログイン時に「UID → users 行」を upsert して紐付ける。

---

## 3. トークンの種類と流れ

Firebase は 2 種類のトークンを扱う。

| トークン | 寿命 | 役割 | 保管 |
|---|---|---|---|
| **ID トークン (JWT)** | 短命（約 1 時間） | API に `Authorization: Bearer` で送る本体 | メモリ（都度 `getIdToken()`） |
| **refresh token** | 長寿命（数ヶ月） | ID トークンを再発行し続ける鍵。**最も守るべき“宝”** | OS セキュアストレージ（§6） |

### フロー

```
[ログイン] （← ここだけ Firebase と通信する）
  アプリ SDK → Firebase「この認証情報は正しい?」
  Firebase が “自分の” ユーザーストアで照合
  → OK なら 署名済み JWT(ID トークン,1h) + refresh token を返す

[初回] アプリ → Java(JWT) → Java が UID を Postgres に upsert

[以降・毎回] アプリ → Java へ  Authorization: Bearer <JWT>
  Java: ① 署名検証（キャッシュした公開鍵でローカル）＝ 認証(AuthN)
        ② UID で Postgres 照合（所属スペース等）  ＝ 認可(AuthZ)
        → OK ならスケジュール登録などを実行
  ※この毎回のフローで Firebase/Google へは通信しない
```

---

## 4. Java 側の検証（署名検証・毎回 Firebase に行かない）

Java がやるのは **「この JWT は本当に Firebase が発行したもので、改ざんされておらず、期限内か」の暗号的検証**。「ユーザーが実在するか」を問い合わせるのではない。

- Firebase は JWT を **Google の秘密鍵で署名**して発行する。
- Java は**対応する公開鍵で署名・`exp`・`aud`・`iss` を検証** → 合致すれば「確かに Firebase 製・未改ざん」と確定し、中の UID を信頼して取り出す。
- **公開鍵は取得してキャッシュ**する（鍵は約 1 日で更新）。個々の API 検証はローカル計算で、Google へは行かない。鍵の再取得（約 1 日に 1 回程度）のときだけ Google へアクセスする。
- これらは **Firebase Admin SDK が自動で面倒を見る**（鍵の取得・キャッシュ・更新）。

> 「スケジュール登録の API を叩くたびに Java が Firebase へ問い合わせる」ことは無い。検証はローカル、Firebase への通信はログイン時（クライアント）と鍵更新時（サーバー・稀）だけ。

---

## 5. 認証(AuthN) と 認可(AuthZ) の分離

| | 何を | 誰が | Firebase/Google と通信 |
|---|---|---|---|
| **認証(AuthN)** | 本人か | ログイン時：Firebase が照合。API 毎：Java が署名検証 | ログイン時のみ（API 毎は無し） |
| **認可(AuthZ)** | このスペース/家族を見て良いか | Java が Postgres で `space_members` 等を照合 | 一切無し |

- API 毎に JWT を検証するのは、**サーバーにセッションを持たない（ステートレス）ため**にリクエストごとに身元を確かめ直すもの。テナント分離“そのもの”ではない。
- **多テナントのデータ保護（家族/グループの権限）は Postgres 側で範囲を絞ることで実現**する（例：`WHERE space_id IN (所属スペース)`）。

---

## 6. トークンの保管とセキュリティ

### 守るべきは refresh token

- **短命 ID トークン** → 盗まれても 1 時間で失効。**メモリ（RAM）に持つ**。自分で永続化しない。
- **refresh token** → 「これさえあれば数ヶ月なりすませる」＝ Web の localStorage/Cookie に相当する**宝**。ここを安全に置くのが焦点。

### ネイティブでの置き場所（安全な順）

| 置き場所 | 実体 | 暗号化 | Expo での名前 | 用途 |
|---|---|---|---|---|
| **セキュアストレージ** | iOS **Keychain** / Android **Keystore(EncryptedSharedPreferences)** | ◎ ハードウェア裏付け | **`expo-secure-store`** | **refresh token 等の機微情報** |
| AsyncStorage | アプリ専用領域の**平文**ファイル | ✕ | `@react-native-async-storage/async-storage` | 機微でない設定 |
| メモリ | RAM | —（揮発） | — | 短命 ID トークン |

### Web との脅威モデルの違い

| | Web の主脅威 | ネイティブの主脅威 |
|---|---|---|
| 代表 | **XSS**（任意 JS がトークンを読む）・CSRF | 端末の紛失・root化/脱獄での抽出・OS バックアップ流出 |
| 対策 | httpOnly Cookie・CSRF トークン | **OS のセキュア保存**（Keychain/Keystore） |

- ネイティブは **OS がアプリ単位でサンドボックス隔離**するため、他アプリはこのアプリの保存領域を読めない。**ベースラインが Web より安全**（同一オリジンの任意 JS が共有する localStorage のような世界ではない）。
- 残る脅威は物理奪取・root化・バックアップ流出で、これを **Keychain/Keystore のハードウェア暗号化が塞ぐ**。よって Web の XSS/CSRF 対策の重装備は不要で、要点は**「refresh token を OS セキュアストレージに置く」の一手**。

### やってはいけないこと

- トークンを**平文ログに出す**。
- Zustand 等の状態を**そのまま AsyncStorage に永続化して機微トークンを載せる**（＝平文保存と同じ）。
- 自作の平文ファイルや暗号化なしの設定に置く。

### SDK 選定との関係（誰が保管するか）

保管の実装は選ぶ SDK で変わる（§8 の要検証事項）。

- **Firebase JS SDK**：既定で AsyncStorage（平文）に永続化。手軽だが宝が平文。
- **React Native Firebase（ネイティブ）**：iOS Keychain 等でより安全に保管。dev client 前提。
- **自前管理**：refresh token を自分で `expo-secure-store` に入れる（最も堅いが手間）。

いずれにせよ、この保管の選択は **§7 の `getToken()` の継ぎ目の裏に隠れる**ため、第一段階では決め切らなくてよい。

---

## 7. feature-based での実装の継ぎ目

Context のバケツリレーは使わず、**セッションは Zustand の 1 ストア**で全画面が直接参照する。
**セッションは feature ではなく基盤（`lib/session`）に置く**（api クライアント・ガード・ヘッダなどアプリ全体が依存するため）。
**Bearer 付与は api クライアントの 1 箇所**に集約し、呼び出しごとにトークン処理を散らさない。

```
src/
  lib/session/        # 基盤（誰でも依存してよい）。バレル index.ts から公開
    store.ts          # Zustand: session状態(status, token) → prop-drilling 無し
    signIn.ts         # signIn()/signOut()。中身は後で Firebase 呼び出しに差し替え
    getToken.ts       # ← 認証の唯一の継ぎ目。getToken(): Promise<string|null>（hookにしない）
    useSession.ts     # 画面/ガードが使うフック
  lib/api/
    client.ts         # fetch ラッパ。1 箇所で getToken() を呼び Bearer 付与（インターセプタ）
  features/auth/       # auth 機能（サインイン画面・フォーム）。lib/session を利用
  components/
    SignOutButton/    # lib/session の signOut を呼ぶ（基盤依存＝OK）
    AppHeader/        # 純粋 UI。right スロットに SignOutButton を注入して合成
  app/
    _layout.tsx       # session状態で (public)/(private) を出し分け（ルートガード）
    (private)/_layout.tsx  # AppHeader に SignOutButton を注入（app 層で合成）
```

- **セッション** = `lib/session` の Zustand ストア（基盤）。どの層も直接読む。
- **Bearer 付与** = `lib/api` の 1 箇所。React Query の queryFn はこの client を通すだけ。
- **ルートガード** = `src/app/_layout` が session を見て出し分け（v56 の `Stack.Protected`）。feature は状態更新のみ、遷移は router が担う。
- **副作用の集約** = `signOut` のトークン消去などは `lib/session` 内に閉じ込め、UI（SignOutButton）は関数を呼ぶだけにする。

---

## 8. 第一段階でのスタブ

第一段階（[frontend-architecture.md](./frontend-architecture.md) の §7）では**形だけ作り、中身は後で差し替える**。

| 継ぎ目 | 第一段階（スタブ） | 将来（本実装） |
|---|---|---|
| `signIn()` | Firebase を呼ばず `store.status = 'authenticated'` にするだけ | `signInWithEmailAndPassword` 等を呼ぶ |
| `getToken()` | ダミー文字列を返す | `currentUser.getIdToken()` を返す |
| ルートガード | session 状態を見て遷移（変更不要） | 変更不要 |
| api client | Bearer 付与の口だけ用意 | 変更不要 |

> 継ぎ目（store / signIn / getToken / api client / route guard）を今きちんと切っておけば、本物の認証は**中身の差し替えだけ**で入り、ルートガード・API クライアント・画面は変更不要になる。

---

## 9. 未確定 / 要検証・将来対応

- **Firebase SDK の選定**（JS SDK / React Native Firebase / 自前管理）。**Expo SDK 56 / RN 0.85 / React 19 互換を、実装前に https://docs.expo.dev/versions/v56.0.0/ で要検証**（[../../../mobile/AGENTS.md](../../../mobile/AGENTS.md) の規約）。
- **SSO（Google / Apple 等の SNS ログイン）** は Firebase 標準機能で実現可能。第一段階以降に追加。
- **トークン交換方式**（Java が自前 JWT を再発行）は当面採らない。custom claims や失効管理が必要になった時に検討。
- **Custom Claims**（スペース所属などをトークンに載せる）は後付け可能。まずは Postgres 側の認可で足りる。
