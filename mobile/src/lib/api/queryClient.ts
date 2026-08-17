import { QueryClient } from "@tanstack/react-query";

import { ApiError } from "./errors";

/**
 * React Query のグローバル既定。
 * - 4xx（クライアント起因）はリトライしない: 何度送っても結果は同じ
 * - 5xx / ネットワークエラーは 2 回まで再試行
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof ApiError && error.status < 500) return false;
        return failureCount < 2;
      },
    },
  },
});
