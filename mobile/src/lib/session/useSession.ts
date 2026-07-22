import { useAuthStore } from "./store";

/**
 * 画面・ルートガードがセッション状態を参照するためのフック。
 */
export function useSession() {
  const status = useAuthStore((s) => s.status);
  return {
    status,
    isAuthenticated: status === "authenticated",
  };
}
