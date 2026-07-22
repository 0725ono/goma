import { useAuthStore } from "./store";

type Credentials = { email: string; password: string };

/**
 * サインイン。
 *
 * 第一段階（スタブ）: 認証を行わず、ダミートークンでセッションを立てるだけ。
 * 将来: Firebase の signInWithEmailAndPassword 等を呼び、取得した ID トークンで
 *       setAuthenticated する形に差し替える（ルートガード・API クライアント・画面は変更不要）。
 */
export function signIn(_credentials?: Credentials): void {
  useAuthStore.getState().setAuthenticated("dummy-token");
}

export function signOut(): void {
  useAuthStore.getState().clear();
}
