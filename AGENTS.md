# リポジトリ全体の作業ルール

モノレポ全体に効く、エージェント（Claude Code 等）向けの作業ルール。
mobile 固有の指示（Expo など）は [mobile/AGENTS.md](./mobile/AGENTS.md) を参照。

## 命名（ドメイン用語）

ドメイン概念の命名（日本語・英語とも）は `docs/ubiquitous-language.md` に従うこと。
新しい概念を導入するときは、先にこの用語集へ行を足す。

## コミットメッセージ規約

コミットする前に `docs/agents/commit-convention.md` を読み、その規約（Conventional Commits + スコープに作業対象フォルダを含める）に従うこと。

> 詳細を別ファイルに置き、ここからはパス参照（`@` 無し）にしている。これにより本文は常時コンテキストに載らず、コミット時にオンデマンドで読み込まれる。
