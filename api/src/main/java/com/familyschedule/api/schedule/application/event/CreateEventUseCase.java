package com.familyschedule.api.schedule.application.event;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.familyschedule.api.schedule.domain.event.Event;
import com.familyschedule.api.schedule.domain.event.EventRepository;

/**
 * 予定を作成するユースケース。
 * id を採番し、ドメインモデル生成時にルール（終了>開始）を検証させてから永続化する。
 */
@Service
public class CreateEventUseCase {

    private final EventRepository repository;

    public CreateEventUseCase(EventRepository repository) {
        this.repository = repository;
    }

    public Event execute(CreateEventCommand command) {
        Event event = new Event(
                UUID.randomUUID(),
                command.spaceId(),
                command.title(),
                command.startAt(),
                command.endAt());
        return repository.save(event);
    }
}
