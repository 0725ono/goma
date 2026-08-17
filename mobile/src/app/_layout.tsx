import { QueryClientProvider } from "@tanstack/react-query";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";

import { queryClient } from "@/lib/api/queryClient";
import { useSession } from "@/lib/session";

/**
 * ルートレイアウト兼ルートガード。
 *
 * v56 推奨の Stack.Protected を使い、セッション状態（Zustand）に応じて
 * (private) / (public) の到達可否を制御する。guard が false のグループには
 * 自動でリダイレクトされるため、手動の router.replace は不要。
 */
export default function RootLayout() {
  const { isAuthenticated } = useSession();

  return (
    <QueryClientProvider client={queryClient}>
      <StatusBar style="auto" />
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Protected guard={isAuthenticated}>
          <Stack.Screen name="(private)" />
        </Stack.Protected>

        <Stack.Protected guard={!isAuthenticated}>
          <Stack.Screen name="(public)" />
        </Stack.Protected>
      </Stack>
    </QueryClientProvider>
  );
}
