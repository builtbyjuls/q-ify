package com.qify.tracking.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class HeartbeatControllerTest {

    private final HeartbeatController controller = new HeartbeatController();

    @Test
    void failedWriteRemovesEmitterWithoutCompletingContainerOwnedError() throws Exception {
        SseEmitter failingEmitter = mock(SseEmitter.class);
        doThrow(new IOException("write failed"))
                .when(failingEmitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        controller.register(failingEmitter);
        controller.sendHeartbeats();

        verify(failingEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(failingEmitter, never()).complete();
        verify(failingEmitter, never()).completeWithError(any(Throwable.class));
    }

    @Test
    void oneClosedEmitterDoesNotPreventAnotherHeartbeat() throws Exception {
        SseEmitter closedEmitter = mock(SseEmitter.class);
        SseEmitter healthyEmitter = mock(SseEmitter.class);
        doNothing()
                .doThrow(new IllegalStateException("emitter closed"))
                .when(closedEmitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        controller.register(closedEmitter);
        controller.register(healthyEmitter);
        controller.sendHeartbeats();
        controller.sendHeartbeats();

        verify(closedEmitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(healthyEmitter, times(3)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
