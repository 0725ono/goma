import { getToken } from "@/lib/session";

/**
 * API のベース URL。
 * 第一段階では API 未着手のため未使用。将来 expo-constants / 環境変数から注入する。
 */
const BASE_URL = "";

/**
 * Bearer トークンの付与を 1 箇所に集約する fetch ラッパ。
 * サーバー状態を扱う React Query の queryFn などは、必ずこの client を通す。
 *
 * 第一段階では実際の呼び出しには使わないが、「トークンを載せる場所」を確定させておく。
 */
export async function apiFetch(
  path: string,
  init: RequestInit = {},
): Promise<Response> {
  const token = await getToken();
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  return fetch(`${BASE_URL}${path}`, { ...init, headers });
}
