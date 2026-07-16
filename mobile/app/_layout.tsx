import { createContext, useContext, useEffect, useState } from "react";
import { Slot, useRouter, useSegments } from "expo-router";

// ==========================================
// 1. AuthContext の作成（アプリ全体で認証状態を共有する器）
// ==========================================
const AuthContext = createContext({
  isAuthenticated: false,
  signIn: () => {},
  signOut: () => {},
});

// 他の画面から useAuth() で簡単に状態や関数を呼び出せるようにする
export const useAuth = () => useContext(AuthContext);

// ==========================================
// 2. 大元のレイアウトコンポーネント
// ==========================================
export default function RootLayout() {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);

  // ダミーのサインイン・サインアウト処理
  const signIn = () => setIsAuthenticated(true);
  const signOut = () => setIsAuthenticated(false);

  return (
    <AuthContext.Provider value={{ isAuthenticated, signIn, signOut }}>
      <AuthGuard />
    </AuthContext.Provider>
  );
}

// ==========================================
// 3. ルーティングの関所（ガード処理）
// ==========================================
function AuthGuard() {
  const { isAuthenticated } = useAuth();
  const segments = useSegments();
  const router = useRouter();
  const [isLoaded, setIsLoaded] = useState<boolean>(false);

  useEffect(() => {
    setIsLoaded(true);
  }, []);

  useEffect(() => {
    if (!isLoaded) return;

    const inPrivateGroup = segments[0] === "(private)";

    if (!isAuthenticated && inPrivateGroup) {
      // 未ログインで private を開こうとしたら signin に弾く
      router.replace("/(public)/signin");
    } else if (isAuthenticated && !inPrivateGroup) {
      // ログイン済み状態になれば、自動的に private に飛ばす
      router.replace("/(private)");
    }
  }, [isAuthenticated, isLoaded, segments]);

  // 子要素を描画
  return <Slot />;
}
