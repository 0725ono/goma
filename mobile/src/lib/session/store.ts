import { create } from "zustand";

export type AuthStatus = "authenticated" | "unauthenticated";

type AuthState = {
  status: AuthStatus;
  /**
   * セッショントークン。
   * 第一段階ではダミー値。将来 Firebase の ID トークンに置き換える。
   */
  token: string | null;
  setAuthenticated: (token: string) => void;
  clear: () => void;
};

/**
 * 認証セッションのクライアント状態（Zustand）。
 * 画面からは useSession() 経由で参照し、Context のバケツリレーは行わない。
 */
export const useAuthStore = create<AuthState>((set) => ({
  status: "unauthenticated",
  token: null,
  setAuthenticated: (token) => set({ status: "authenticated", token }),
  clear: () => set({ status: "unauthenticated", token: null }),
}));
