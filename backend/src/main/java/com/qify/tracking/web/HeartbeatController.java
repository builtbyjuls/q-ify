package com.qify.tracking.web;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
class HeartbeatController {

    private static final long EMITTER_TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    @GetMapping(path = "/heartbeat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter heartbeat() {
        return register(new SseEmitter(EMITTER_TIMEOUT_MILLIS));
    }

    SseEmitter register(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(exception -> emitters.remove(emitter));
        sendHeartbeat(emitter);
        return emitter;
    }

    @Scheduled(fixedDelay = 15_000)
    void sendHeartbeats() {
        emitters.forEach(this::sendHeartbeat);
    }

    private void sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(new Heartbeat("alive"), MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            emitters.remove(emitter);
        } catch (IllegalStateException exception) {
            emitters.remove(emitter);
        }
    }

    record Heartbeat(String status) {
    }
}
