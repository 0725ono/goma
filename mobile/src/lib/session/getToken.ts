import { useAuthStore } from "./store";

/**
 * 認証の唯一の継ぎ目。API 呼び出し時にトークンを取得するために使う。
 *
 * 第一段階（スタブ）: ストア内のダミートークンを返す。
 * 将来: firebase の currentUser.getIdToken() を返す形に差し替える。
 */
export async function getToken(): Promise<string | null> {
  return useAuthStore.getState().token;
}
