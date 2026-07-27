package com.familyschedule.api.schedule.usecase.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 予定作成ユースケースへの入力。 */
public record CreateEventCommand(
        UUID spaceId,
        String title,
        OffsetDateTime startAt,
        OffsetDateTime endAt) {
}
