package com.familyschedule.api.schedule.infrastructure.event;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.familyschedule.api.schedule.domain.event.Event;
import com.familyschedule.api.schedule.domain.event.EventRepository;

/**
 * ドメインのポート {@link EventRepository} を JPA で実装するアダプタ。
 * ドメインモデル ⇔ JPA エンティティの変換をここに閉じ込める。
 */
@Repository
public class EventRepositoryAdapter implements EventRepository {

    private final EventJpaRepository jpa;

    public EventRepositoryAdapter(EventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Event save(Event event) {
        EventEntity saved = jpa.save(toEntity(event));
        return toDomain(saved);
    }

    @Override
    public List<Event> findBySpaceId(UUID spaceId) {
        return jpa.findBySpaceId(spaceId).stream().map(this::toDomain).toList();
    }

    private EventEntity toEntity(Event e) {
        return new EventEntity(e.id(), e.spaceId(), e.title(), e.startAt(), e.endAt());
    }

    private Event toDomain(EventEntity e) {
        return new Event(e.getId(), e.getSpaceId(), e.getTitle(), e.getStartAt(), e.getEndAt());
    }
}
