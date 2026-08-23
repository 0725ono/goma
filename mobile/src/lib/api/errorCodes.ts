/**
 * API の ErrorCode enum の写し（TypeScript 側）。
 *
 * 真実は api 側の ErrorCode.java にある。ここはその鏡であり、
 * 両者の一致は api 側の同期テスト（ErrorCodeContractTest）が検証する。
 * enum に定数を足したら、このファイルにも必ず足す（忘れると make test が落ちる）。
 */
export const ERROR_CODES = [
  // ---- 形式・入力 ----
  /** Bean Validation 違反。ApiError.fieldErrors にフィールド別内訳が入る */
  "VALIDATION_ERROR",
  /** リクエストが解釈できない（壊れた JSON・型変換不能なパラメータなど） */
  "MALFORMED_REQUEST",
  // ---- ルーティング ----
  /** エンドポイントが存在しない */
  "RESOURCE_NOT_FOUND",
  /** エンドポイントは存在するが、その HTTP メソッドは未対応 */
  "METHOD_NOT_ALLOWED",
  /** 個別の案内を持たない 4xx の受け皿 */
  "REQUEST_REJECTED",
  // ---- ドメインルール ----
  /** 終了日時が開始日時より後になっていない */
  "EVENT_TIME_INVALID",
  // ---- 想定外 ----
  /** サーバ内部の想定外の失敗 */
  "INTERNAL_ERROR",
] as const;

export type ErrorCode = (typeof ERROR_CODES)[number];

/**
 * ワイヤ上の値（ただの文字列）を ErrorCode に絞り込む。
 * 未知の値は null（= code 無し）として扱う。写しの更新漏れは同期テストが検知するため、
 * 実運用で未知の code が届くことは想定していない。
 */
export function toErrorCode(value: unknown): ErrorCode | null {
  return typeof value === "string" && (ERROR_CODES as readonly string[]).includes(value)
    ? (value as ErrorCode)
    : null;
}
