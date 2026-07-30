# 🛠 開発環境 全体方針（Development Environment Policy）

家族スケジュール＆タスク共有アプリの **開発環境・確認・配布・同期の方針と手順**。
アプリの機能設計は [architecture.md](./architecture.md) を参照。

- ホスト: **macOS**（Docker Desktop）
- 実機: Android スマホ / iPhone

---

## 1. 基本方針

| 軸 | 方針 |
|---|---|
| バックエンド (Java + PostgreSQL) | **Docker 内**で開発・稼働 |
| モバイル (React Native / Expo) | **macOS ホスト側**で実行（コンテナ外） |
| 確認 | **Android 実機 + iOS シミュレータ**（費用ゼロ） |
| 配布 | ストア公開しない。**直接配布 / 内部配布**で家族に届ける |
| データ同期 | クラウドの **自前 API + DB** に各端末が read/write |

---

## 2. 開発構成と起動手順

```
macOS ホスト                          Docker（ホスト公開ポート → コンテナ内部ポート）
├─ mobile/   ← Expo をここで実行        ├─ Java API (Spring Boot)  :18080 → :8080
│   npx expo start                    └─ PostgreSQL              :15432 → :5432
```

> **ホスト側ポートについて**: 別プロジェクトがホストの 8080/5432 を使用するため、本プロジェクトはホスト側を **18080 / 15432** にずらしている。コンテナ内部は 8080/5432 のまま（Java・Flyway の設定変更は不要）。ホスト（mobile や DB クライアント）からアクセスする際は 18080/15432 を使う。

モバイルは macOS ホストのターミナルから起動する。

```bash
cd mobile
npm start                 # = expo start（開発サーバ Metro 起動）
#   i  → iOS シミュレータ
#   a  → Android
#   w  → Web
```

npm スクリプト（[mobile/package.json](../mobile/package.json)）※すべてホストで実行:

| コマンド | 内容 | 用途 |
|---|---|---|
| `npm run dev` | `expo start --web --port 8081` | web での素早い確認 |
| `npm start` | `expo start`（対話メニュー起動） | 実機/シミュレータ |
| `npm run android` | `expo run:android` | Android ネイティブビルド |
| `npm run ios` | `expo run:ios` | iOS シミュレータ |

> **ポートの注意**: モバイルは **8081**（Metro 既定）を使う。`docker-compose.yml` で `8081:8081` を公開していると **Docker Desktop がホストの 8081 を占有**し、ホストの Expo と衝突する。そのため **8081 はコンテナで公開しない**（`docker-compose.yml` / `devcontainer.json` から除外済み）。コンテナがホストに公開するのは `18080`(Java API) と `15432`(PostgreSQL) のみ（内部はそれぞれ 8080/5432）。

---

## 3. 確認体制（費用ゼロ）

すべて macOS ホストで実行する。

| 手段 | コマンド | 費用 | Apple Developer |
|---|---|---|---|
| **iOS シミュレータ** | `npx expo run:ios` | 無料 | 不要 |
| iPhone 実機（ケーブル） | `npx expo run:ios --device` | 無料（署名は7日で失効→入れ直し） | 不要 |
| **Android 実機** | `npx expo start --dev-client` | 無料 | 不要 |
| Web（簡易確認） | `npm run dev` | 無料 | 不要 |

日常は **Android 実機 + iOS シミュレータ**の2本立て。

---

## 4. Development Build（dev client）

Expo Go は使わず、プロジェクト専用の Development Build を使う。一度実機に入れれば、以降は JS の変更が Metro 経由で即反映される（ネイティブモジュールを追加したときだけ作り直す）。

作成は下記「EAS」を参照。

---

## 5. EAS（Expo Application Services）

クラウドでアプリをビルド・配信するサービス。ローカルに Xcode / Android Studio を入れずに済む。

| サービス | 役割 |
|---|---|
| EAS Build | アプリ本体（Android=APK, iOS=IPA）をクラウドでビルド |
| EAS Update | JS だけを OTA 配信（再ビルド不要） |
| EAS Submit | ストア提出（当面不要） |

無料枠: ビルド Android 15 + iOS 15 回/月、OTA 1,000 MAU/月。

### セットアップ
```bash
npm i -g eas-cli
eas login                 # 無料の Expo アカウント
eas build:configure       # 初回のみ（eas.json 生成）
```

### ビルドプロファイル
| プロファイル | 用途 |
|---|---|
| `development` | Metro に繋いでホットリロードする開発用 dev client |
| `preview` / `production` | 単体で動く配布用（デプロイした API を見に行く設定） |

### Android への配布
```bash
eas build -p android --profile preview
# → DL リンク + QR → Android で開いて APK をインストール
```

### iPhone への配布
```bash
eas device:create                        # 配布先 iPhone の UDID を登録
eas build -p ios --profile preview       # 内部配布ビルド
```
iOS の内部配布・TestFlight・App Store はいずれも **Apple Developer Program（$99/年）** が必要。

---

## 6. 配布方針（ストア公開しない）

### Android
`eas build -p android --profile preview` の APK リンクを家族に渡すだけ。無料・恒久利用可。

### iPhone
| 方法 | 公開範囲 | 費用 |
|---|---|---|
| EAS 内部配布 (Ad Hoc) | 非公開（UDID 登録端末のみ・年100台） | $99/年 |
| TestFlight | 招待した内部テスター100人まで（審査なし） | $99/年 |
| App Store | 世界公開 | $99/年＋審査 |

家族配布は **EAS 内部配布 or TestFlight**。自分の開発 iPhone で試すだけならケーブル接続（無料・7日）で足りる。

---

## 7. データ同期

「スケジュール（データ）の同期」と「アプリのコード更新」は別物。

- **データ同期**: 各端末がクラウドの **Java API + PostgreSQL** に read/write する。→ API + DB を外部クラウド（Render / Fly.io / Railway / VPS 等）にデプロイする。
- **コード更新**: EAS Update（JS の OTA）/ EAS Build（ネイティブ変更時）。

### 同期方式は段階化
- **フェーズ1（MVP）**: 端末はオンラインで **クラウド API に直接 read/write**。
- **フェーズ2**: ローカル DB（SQLite / WatermelonDB 等）を導入し、オフライン対応＆差分同期を追加。

---

## 8. コード更新フロー（CI）

- GitHub Actions から `eas update`（JS の OTA）/ `eas build`（ネイティブ変更時）を実行（公式 `expo-github-action`）。
- JS の変更 → OTA で即配信。ネイティブの変更 → 再ビルド＆再配布。

---

## 9. TODO

- [ ] モバイルを macOS ホストで実行する構成へ移行（`npx expo start` で iOS シミュレータ / Android 実機の疎通確認）
- [ ] `docker-compose.yml` / `.devcontainer/.env` / `devcontainer.json` のポート設定を整理（Expo をホストに出す前提に統一）
- [ ] `eas.json` を用意し Development Build のプロファイルを作成 → Android 実機で初回インストール
- [ ] 実機からホストの Java API (18080) へ届くベースURL の持たせ方を設計（`.env` / `expo-constants`）
- [ ] バックエンド（Java API + PostgreSQL）のクラウドデプロイ方針を決める
- [ ] （必要時）iOS 配布のため Apple Developer Program に加入

---

## 参考リンク

- [Development builds の概要](https://docs.expo.dev/develop/development-builds/introduction/)
- [EAS 内部配布](https://docs.expo.dev/build/internal-distribution/)
- [Expo / EAS 料金](https://expo.dev/pricing)
- [Expo SDK 56 versioned docs](https://docs.expo.dev/versions/v56.0.0/)
