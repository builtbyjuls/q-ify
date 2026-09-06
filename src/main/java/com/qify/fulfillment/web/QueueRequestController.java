package com.qify.fulfillment.web;

import java.net.URI;
import java.util.UUID;

import com.qify.fulfillment.application.QueueRequestCreationService;
import com.qify.fulfillment.domain.QueueRequestStatus;
import com.qify.fulfillment.persistence.QueueRequestRepository;
import com.qify.identity.application.CurrentActor;
import com.qify.identity.domain.ActorRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/queue-requests")
public class QueueRequestController {

    private final QueueRequestCreationService queueRequestCreationService;
    private final QueueRequestRepository queueRequestRepository;

    public QueueRequestController(QueueRequestCreationService queueRequestCreationService,
            QueueRequestRepository queueRequestRepository) {
        this.queueRequestCreationService = queueRequestCreationService;
        this.queueRequestRepository = queueRequestRepository;
    }

    @PostMapping
    public ResponseEntity<CreateQueueRequestResponse> create(CurrentActor currentActor,
            @Valid @RequestBody CreateQueueRequestRequest request) {
        requireCustomer(currentActor, "Only customers may create queue requests.");

        var id = queueRequestCreationService.create(currentActor.id(), request.serviceOfferingId(),
                request.scheduledFor().toInstant(), request.expectedQueueMinutes(), request.arrivalNoticeMinutes());
        var response = new CreateQueueRequestResponse(id, QueueRequestStatus.REQUESTED);
        return ResponseEntity.created(URI.create("/api/v1/queue-requests/" + id)).body(response);
    }

    @GetMapping("/{id}")
    public QueueRequestResponse get(@PathVariable UUID id,
            CurrentActor currentActor) {
        requireCustomer(currentActor, "Only customers may view queue requests.");
        var request = queueRequestRepository.findOwnedWithOfferingAndVenue(id, currentActor.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Queue request was not found."));
        var offering = request.getServiceOffering();
        var venue = offering.getVenue();
        return new QueueRequestResponse(request.getId(), offering.getId(), venue.getId(), venue.getName(),
                offering.getCategory(), request.getStatus(), request.getScheduledFor(),
                request.getExpectedQueueMinutes(), request.getArrivalNoticeMinutes(), request.getCreatedAt());
    }

    private void requireCustomer(CurrentActor currentActor, String detail) {
        if (currentActor.role() != ActorRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, detail);
        }
    }
}
