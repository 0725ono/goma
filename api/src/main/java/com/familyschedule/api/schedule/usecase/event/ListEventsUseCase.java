package com.familyschedule.api.schedule.usecase.event;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.familyschedule.api.schedule.domain.event.Event;
import com.familyschedule.api.schedule.domain.event.EventRepository;

/** 指定スペースの予定一覧を取得するユースケース。 */
@Service
public class ListEventsUseCase {

    private final EventRepository repository;

    public ListEventsUseCase(EventRepository repository) {
        this.repository = repository;
    }

    public List<Event> execute(UUID spaceId) {
        return repository.findBySpaceId(spaceId);
    }
}
