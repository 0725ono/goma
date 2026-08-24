import type { ErrorCode } from "./errorCodes";
import { ApiError, NetworkError } from "./errors";

/**
 * エラー → ユーザー向け日本語文言への変換（唯一の変換点）。
 *
 * 画面は error.message（サーバーの detail 等、英語の内部文言）を直接表示せず、
 * 必ずこの関数を通す。「feature 側は code で分岐し、文言に依存しない」という
 * エラー契約（docs/api/architecture/error-contract.md）の消費側の実装。
 *
 * Record<ErrorCode, string> で全コード分の文言を強制しているため、
 * errorCodes.ts に code を足すとここもコンパイルエラーで追加を促される。
 */
const MESSAGES: Record<ErrorCode, string> = {
  // 入力起因: ユーザーが直せる
  VALIDATION_ERROR: "入力内容に誤りがあります",
  EVENT_TIME_INVALID: "終了日時は開始日時より後にしてください",
  // クライアント実装のバグの疑い: ユーザーに直せることはないので汎用文言
  MALFORMED_REQUEST: "アプリの通信でエラーが発生しました",
  RESOURCE_NOT_FOUND: "アプリの通信でエラーが発生しました",
  METHOD_NOT_ALLOWED: "アプリの通信でエラーが発生しました",
  REQUEST_REJECTED: "アプリの通信でエラーが発生しました",
  // サーバー起因
  INTERNAL_ERROR: "サーバーで問題が発生しました。時間をおいてお試しください",
};

export function getErrorMessage(error: unknown): string {
  if (error instanceof NetworkError) {
    return "サーバーに接続できません。通信環境を確認してください";
  }
  if (error instanceof ApiError) {
    if (__DEV__ && error.code !== null && MESSAGES[error.code].startsWith("アプリの通信")) {
      // 契約上「実装バグの疑い」の code。開発中に気づけるようログを残す
      console.warn(`[api] client-bug-suspected error: ${error.code} (status ${error.status})`);
    }
    return error.code !== null
      ? MESSAGES[error.code]
      : "エラーが発生しました。時間をおいてお試しください";
  }
  return "予期しないエラーが発生しました";
}
