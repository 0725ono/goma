package com.familyschedule.api.schedule.interfaces.event;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.familyschedule.api.schedule.domain.event.Event;
import com.familyschedule.api.schedule.application.event.CreateEventCommand;
import com.familyschedule.api.schedule.application.event.CreateEventUseCase;
import com.familyschedule.api.schedule.application.event.ListEventsUseCase;

import jakarta.validation.Valid;

/** 予定の API エンドポイント。usecase を呼び、DTO に詰め替えて返すだけの薄い層。 */
@RestController
@RequestMapping("/events")
public class EventController {

    // 認証未実装のため、当面は固定のスタブ空間を使う（将来はトークンの UID から解決する）。
    private static final UUID DEFAULT_SPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final CreateEventUseCase createEvent;
    private final ListEventsUseCase listEvents;

    public EventController(CreateEventUseCase createEvent, ListEventsUseCase listEvents) {
        this.createEvent = createEvent;
        this.listEvents = listEvents;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody CreateEventRequest request) {
        UUID spaceId = request.spaceId() != null ? request.spaceId() : DEFAULT_SPACE_ID;
        Event event = createEvent.execute(
                new CreateEventCommand(spaceId, request.title(), request.startAt(), request.endAt()));
        return EventResponse.from(event);
    }

    @GetMapping
    public List<EventResponse> list(@RequestParam(required = false) UUID spaceId) {
        UUID target = spaceId != null ? spaceId : DEFAULT_SPACE_ID;
        return listEvents.execute(target).stream().map(EventResponse::from).toList();
    }
}
