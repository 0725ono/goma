package com.familyschedule.api.schedule.infrastructure.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * events テーブルに対応する JPA エンティティ（永続化の都合を持つオブジェクト）。
 * ドメインの {@link com.familyschedule.api.schedule.domain.event.Event} とは別物で、
 * 変換は {@link EventRepositoryAdapter} が担当する。
 */
@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    // created_at は DB 側の default now() が入れるため、挿入・更新の対象外にする
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** JPA 用のデフォルトコンストラクタ。 */
    protected EventEntity() {
    }

    public EventEntity(UUID id, UUID spaceId, String title, OffsetDateTime startAt, OffsetDateTime endAt) {
        this.id = id;
        this.spaceId = spaceId;
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSpaceId() {
        return spaceId;
    }

    public String getTitle() {
        return title;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }
}
