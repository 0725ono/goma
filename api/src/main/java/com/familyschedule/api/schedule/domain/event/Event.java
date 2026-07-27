package com.familyschedule.api.schedule.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.familyschedule.api.shared.domain.DomainRuleViolationException;

/**
 * 予定（Event）のドメインモデル。
 *
 * 永続化（JPA）や HTTP の都合を一切持ち込まない純粋なオブジェクト。
 * 生成時に不変条件を必ず検証する。このとき投げる例外の型で意味を区別する:
 *
 * - IllegalArgumentException      … プログラミングエラー（呼び出し側のバグ）。
 *                                   id/spaceId はアプリ側が必ず採番・解決して渡すため、
 *                                   null はユーザー入力では起こり得ない → 500 が正しい。
 * - DomainRuleViolationException … ユーザー入力が破り得るドメインルール → 400 が正しい。
 */
public record Event(
        UUID id,
        UUID spaceId,
        String title,
        OffsetDateTime startAt,
        OffsetDateTime endAt) {

    public Event {
        // プログラミングエラー（内部不変条件）: 破られたら実装バグ
        if (id == null) throw new IllegalArgumentException("id is required");
        if (spaceId == null) throw new IllegalArgumentException("spaceId is required");

        // ドメインルール（ユーザー入力が破り得る）: code は API 契約の一部
        if (title == null || title.isBlank()) {
            throw new DomainRuleViolationException("EVENT_TITLE_REQUIRED", "title is required");
        }
        if (startAt == null || endAt == null) {
            throw new DomainRuleViolationException("EVENT_TIME_REQUIRED", "startAt and endAt are required");
        }
        if (!endAt.isAfter(startAt)) {
            throw new DomainRuleViolationException("EVENT_TIME_INVALID", "endAt must be after startAt");
        }
    }
}
