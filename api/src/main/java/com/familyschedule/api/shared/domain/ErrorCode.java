package com.familyschedule.api.shared.domain;

/**
 * API がクライアントに返すエラーコードのカタログ（API 契約の一部）。
 *
 * - クライアントはエラー応答の code 拡張メンバー（= この enum の name()）で分岐する。
 *   一度公開した code の意味は変えない。意味が変わるなら新しい定数を追加する。
 * - status は「この code を自前で応答にするときの HTTP ステータス」。
 *   Spring の型（HttpStatus）ではなく int で持つ。domain 層はフレームワークに
 *   依存できないためで、ステータス番号自体は RFC の数字であり Spring の概念ではない。
 * - フレームワーク例外に code を割り当てる経路（GlobalExceptionHandler）では、
 *   実際のステータスは Spring が決めたものを維持し、code だけをここから付与する。
 *
 * 定数を追加・変更したら必ず以下も更新する（同期テスト ErrorCodeContractTest が見張る）:
 * - mobile/src/lib/api/errorCodes.ts（TypeScript 側の写し）
 * - docs/api/architecture/error-contract.md（「なぜ」の記述）
 */
public enum ErrorCode {

    // ---- 形式・入力（interfaces 層で検出） ----

    /** Bean Validation 違反。errors 拡張メンバー（フィールド別内訳）を伴う。 */
    VALIDATION_ERROR(400),

    /** リクエストが解釈できない（壊れた JSON・型変換不能なパラメータなど）。 */
    MALFORMED_REQUEST(400),

    // ---- ルーティング ----

    /** エンドポイントが存在しない。 */
    RESOURCE_NOT_FOUND(404),

    /** エンドポイントは存在するが、その HTTP メソッドは未対応。 */
    METHOD_NOT_ALLOWED(405),

    /**
     * 上記いずれにも分類しない 4xx の受け皿（例: 415, 406）。
     * 「個別の案内を用意するほどの価値がない」と明示的に判断したエラーがここに落ちる。
     * このとき実際のステータスは Spring が決めた値を維持する（400 は名目値）。
     */
    REQUEST_REJECTED(400),

    // ---- ドメインルール（domain 層で検出） ----

    /** 終了日時が開始日時より後になっていない。 */
    EVENT_TIME_INVALID(400),

    // ---- 想定外 ----

    /** サーバ内部の想定外の失敗。詳細はサーバログにのみ残す。 */
    INTERNAL_ERROR(500);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }

    /** この code を自前で応答にするときの HTTP ステータス。 */
    public int status() {
        return status;
    }
}
