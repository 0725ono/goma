package com.familyschedule.api.schedule.domain.event;

import java.util.List;
import java.util.UUID;

/**
 * 予定の永続化ポート（インターフェース）。
 *
 * ドメイン側が「必要な操作」を定義し、実装（JPA など）は infrastructure 層が担う。
 * これにより usecase / domain は永続化技術に依存しない（依存の向きを内側へ保つ）。
 */
public interface EventRepository {

    Event save(Event event);

    List<Event> findBySpaceId(UUID spaceId);
}
