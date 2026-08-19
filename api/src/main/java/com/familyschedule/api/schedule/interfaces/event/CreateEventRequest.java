package com.familyschedule.api.schedule.interfaces.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 予定作成リクエスト（HTTP 入力の形）。
 * spaceId は省略可（認証未実装のため、省略時は固定のスタブ空間を使う）。
 * title の上限 255 は DB の VARCHAR(255) と対で、最も外側のゲートで先に弾く。
 */
public record CreateEventRequest(
        UUID spaceId,
        @NotBlank @Size(max = 255) String title,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt) {
}
