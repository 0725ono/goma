# 家族スケジュールアプリ 開発タスク
#
# - api-*    : devcontainer 内で実行する（Java はコンテナ内で動かす方針）
# - mobile-* : macOS ホストで実行する（RN はホストで動かす方針。コンテナ内では動かない）
#
# `make` だけ打つとタスク一覧を表示する。

.DEFAULT_GOAL := help
.PHONY: help api-run api-stop api-build api-test api-jar mobile-install mobile-start mobile-clean

help: ## タスク一覧を表示
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  make %-16s %s\n", $$1, $$2}'

# ---- API (Java / devcontainer 内) ----

api-run: ## dev サーバー起動（フォアグラウンド。Ctrl+C で停止）
	cd api && ./mvnw spring-boot:run

api-stop: ## 起動しっぱなしの dev サーバーを停止
	-pkill -f "spring-boot:run"
	-pkill -f "com.familyschedule.api.ApiApplication"

api-build: ## コンパイルのみ（動作確認せず素早くエラー検出）
	cd api && ./mvnw -B -DskipTests compile

api-test: ## テスト実行（要: db コンテナ稼働）
	cd api && ./mvnw -B test

api-jar: ## 本番形態の実行可能 jar を作成
	cd api && ./mvnw -B -DskipTests package

# ---- Mobile (React Native / macOS ホスト) ----

mobile-install: ## 依存インストール（ホスト専用）
	@command -v npm >/dev/null || { echo "ERROR: mobile-* は macOS ホストで実行してください（コンテナに npm はありません）"; exit 1; }
	cd mobile && npm install

mobile-start: ## Expo dev サーバー起動（ホスト専用。i=iOS / a=Android）
	@command -v npm >/dev/null || { echo "ERROR: mobile-* は macOS ホストで実行してください（コンテナに npm はありません）"; exit 1; }
	cd mobile && npx expo start

mobile-clean: ## Metro キャッシュをクリアして起動（ホスト専用）
	@command -v npm >/dev/null || { echo "ERROR: mobile-* は macOS ホストで実行してください（コンテナに npm はありません）"; exit 1; }
	cd mobile && npx expo start -c
