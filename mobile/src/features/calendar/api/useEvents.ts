import { useQuery } from "@tanstack/react-query";

import { apiFetch } from "@/lib/api/client";
import type { Event } from "@/features/calendar/model/event";

/**
 * 予定一覧のサーバー状態。キャッシュ・再取得・ローディングは React Query が担う。
 * （spaceId は認証未実装のためサーバー側のスタブ空間に委ねる）
 */
export function useEvents() {
  return useQuery({
    queryKey: ["events"],
    queryFn: () => apiFetch<Event[]>("/events"),
  });
}
