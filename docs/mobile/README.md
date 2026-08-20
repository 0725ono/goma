# 📱 Mobile ドキュメント

React Native (Expo SDK 56 / TypeScript) 製モバイルアプリの設計ドキュメント。
システム全体の設計は [../architecture.md](../architecture.md)、バックエンドの設計は [../api/](../api/)、開発環境の方針は
[../development-environment.md](../development-environment.md) を参照。

## 構成

| 区分 | 場所 | 内容 |
|---|---|---|
| **全体設計** | [architecture/](./architecture/) | アプリ全体に効く横断的な設計方針（アーキテクチャ・状態管理・認証など） |
| **機能設計** | [features/](./features/) | 個々の機能ごとの設計（機能単位で追加していく） |

### 全体設計 (architecture/)

- [frontend-architecture.md](./architecture/frontend-architecture.md) — feature-based アーキテクチャ、ディレクトリ構成、状態管理、第一段階スコープ
- [authentication.md](./architecture/authentication.md) — Firebase を用いた認証方針、トークンの流れと保管

### 機能設計 (features/)

機能ごとの設計はここに置く（例：カレンダー、スペース切り替え、通知 など）。詳細は
[features/README.md](./features/README.md) を参照。
