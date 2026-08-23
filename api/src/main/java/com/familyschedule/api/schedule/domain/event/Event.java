package com.familyschedule.api.schedule.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.familyschedule.api.shared.domain.DomainRuleViolationException;
import com.familyschedule.api.shared.domain.ErrorCode;

/**
 * 予定（Event）のドメインモデル。
 *
 * 永続化（JPA）や HTTP の都合を一切持ち込まない純粋なオブジェクト。
 * 生成時に不変条件を必ず検証する。このとき投げる例外の型で意味を区別する:
 *
 * - IllegalArgumentException      … プログラミングエラー（呼び出し側のバグ）。
 *                                   null・空の検査は DTO（CreateEventRequest）が先に弾くため、
 *                                   ここに届くのは domain を直接呼ぶ経路の実装ミスだけ → 500 が正しい。
 *                                   API 契約ではないため、専用のエラーコードは持たない
 *                                   （DB の CHECK 制約と同じ「多重防御」の位置づけ）。
 * - DomainRuleViolationException … 単一フィールドでは判定できないビジネスルール違反。
 *                                   DTO では表現できず、ここが唯一の検査点 → 400 が正しい。
 */
public record Event(
        UUID id,
        UUID spaceId,
        String title,
        OffsetDateTime startAt,
        OffsetDateTime endAt) {

    public Event {
        // 多重防御（内部不変条件）: HTTP 経由では DTO が先に弾く。破られたら実装バグ
        if (id == null) throw new IllegalArgumentException("id is required");
        if (spaceId == null) throw new IllegalArgumentException("spaceId is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
        if (startAt == null || endAt == null) throw new IllegalArgumentException("startAt and endAt are required");

        // ドメインルール（複数フィールドにまたがる検査）: ここが唯一の検査点。code は API 契約の一部
        if (!endAt.isAfter(startAt)) {
            throw new DomainRuleViolationException(ErrorCode.EVENT_TIME_INVALID, "endAt must be after startAt");
        }
    }
}
