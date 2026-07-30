package com.familyschedule.api.schedule.interfaces.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.familyschedule.api.schedule.domain.event.Event;

/** 予定のレスポンス（HTTP 出力の形）。ドメインモデルから変換して返す。 */
public record EventResponse(
        UUID id,
        UUID spaceId,
        String title,
        OffsetDateTime startAt,
        OffsetDateTime endAt) {

    public static EventResponse from(Event e) {
        return new EventResponse(e.id(), e.spaceId(), e.title(), e.startAt(), e.endAt());
    }
}
