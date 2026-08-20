import { Platform } from "react-native";

import { getToken } from "@/lib/session";
import { ApiError, NetworkError, type ProblemDetail } from "./errors";
import { toErrorCode } from "./errorCodes";

/**
 * API のベース URL。優先順位:
 * 1. 環境変数 EXPO_PUBLIC_API_URL（実機で使用。mobile/.env に mac の LAN IP を設定する）
 * 2. Platform 既定値
 *    - iOS シミュレータ: mac の localhost にそのまま届く
 *    - Android エミュレータ: 10.0.2.2 がホスト(mac)を指す特別なアドレス
 */
const BASE_URL =
  process.env.EXPO_PUBLIC_API_URL ??
  Platform.select({
    android: "http://10.0.2.2:18080",
    default: "http://localhost:18080",
  });

/**
 * Bearer トークンの付与・エラー契約の解釈を 1 箇所に集約する fetch ラッパ。
 * サーバー状態を扱う React Query の queryFn は、必ずこの client を通す。
 *
 * - 非 2xx: ProblemDetail をパースして ApiError を throw
 * - 接続不能: NetworkError を throw
 * - 2xx: ボディの JSON を T として返す
 */
export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const token = await getToken();
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let res: Response;
  try {
    res = await fetch(`${BASE_URL}${path}`, { ...init, headers });
  } catch (cause) {
    throw new NetworkError(cause);
  }

  if (!res.ok) {
    // エラーは ProblemDetail 契約でパース（万一パース不能でも status だけで ApiError を組む）
    const problem: ProblemDetail | null = await res.json().catch(() => null);
    throw new ApiError(
      res.status,
      toErrorCode(problem?.code),
      problem?.detail,
      problem?.errors,
    );
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
