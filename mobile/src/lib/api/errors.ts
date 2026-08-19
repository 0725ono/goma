/**
 * API エラーの型定義。
 *
 * バックエンドは全エラーを RFC 9457 (Problem Details) + 拡張メンバー
 * （code: 機械可読な識別子, errors: フィールド別検証エラー）で返す契約。
 * その契約を HTTP の世界から TypeScript の型の世界へ翻訳するのがここ。
 * feature 側は ApiError.code で分岐し、HTTP や ProblemDetail の形は知らない。
 */

/** バックエンドの ProblemDetail の形（拡張メンバー込み） */
export type ProblemDetail = {
  status: number;
  title?: string;
  detail?: string;
  code?: string;
  errors?: FieldError[];
};

export type FieldError = {
  field: string;
  message: string;
};

/** サーバーがエラー応答を返した場合（4xx/5xx） */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    /** バックエンドの code 拡張メンバー（例: "EVENT_TIME_INVALID"）。分岐はこれで行う */
    readonly code: string | null,
    detail?: string,
    /** VALIDATION_ERROR 時のフィールド別エラー（フォームの setError に使う） */
    readonly fieldErrors?: FieldError[],
  ) {
    super(detail ?? `API error (status ${status})`);
    this.name = "ApiError";
  }
}

/** サーバーに到達すらできなかった場合（オフライン・接続不可） */
export class NetworkError extends Error {
  constructor(cause?: unknown) {
    super("network request failed");
    this.name = "NetworkError";
    this.cause = cause;
  }
}
