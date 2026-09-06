package com.qify.fulfillment.web;

import java.net.URI;

import com.qify.fulfillment.application.QueueRequestCreationService;
import com.qify.fulfillment.domain.QueueRequestStatus;
import com.qify.identity.application.CurrentActor;
import com.qify.identity.domain.ActorRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/queue-requests")
public class QueueRequestController {

    private final QueueRequestCreationService queueRequestCreationService;

    public QueueRequestController(QueueRequestCreationService queueRequestCreationService) {
        this.queueRequestCreationService = queueRequestCreationService;
    }

    @PostMapping
    public ResponseEntity<CreateQueueRequestResponse> create(CurrentActor currentActor,
            @Valid @RequestBody CreateQueueRequestRequest request) {
        if (currentActor.role() != ActorRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only customers may create queue requests.");
        }

        var id = queueRequestCreationService.create(currentActor.id(), request.serviceOfferingId(),
                request.scheduledFor().toInstant(), request.expectedQueueMinutes(), request.arrivalNoticeMinutes());
        var response = new CreateQueueRequestResponse(id, QueueRequestStatus.REQUESTED);
        return ResponseEntity.created(URI.create("/api/v1/queue-requests/" + id)).body(response);
    }
}
