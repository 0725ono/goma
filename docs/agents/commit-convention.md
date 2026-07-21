# コミットメッセージ規約

[Conventional Commits](https://www.conventionalcommits.org/) に従い、**スコープに作業対象フォルダを必ず含める**。

```
<type>(<scope>): <説明>
```

## type

| type | 用途 |
|---|---|
| `feat` | 機能追加 |
| `fix` | バグ修正 |
| `docs` | ドキュメントのみの変更 |
| `refactor` | 挙動を変えないコード変更 |
| `test` | テストの追加・修正 |
| `chore` | ビルド・設定・雑務 |
| `ci` | CI 設定 |

## scope（作業対象フォルダ）

| scope | 対象 |
|---|---|
| `mobile` | `mobile/`（React Native / Expo） |
| `api` | `api/`（Java / Spring Boot） |
| `docs` | `docs/` |
| `infra` | `docker-compose.yml`, `.devcontainer/`, ルート直下の設定 |

- 複数フォルダにまたがる場合は主たる scope を使う。リポジトリ全体に関わる変更（scope が定まらないもの）は scope を省略してよい。

## 例

```
feat(mobile): ログイン画面のフォームを追加
fix(api): JWT 検証の aud チェック漏れを修正
docs(mobile): 認証方針ドキュメントを追加
chore(infra): devcontainer に Java 17 feature を追加
```
