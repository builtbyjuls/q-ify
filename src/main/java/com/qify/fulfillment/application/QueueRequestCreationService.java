package com.qify.fulfillment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.qify.catalog.domain.ServiceOffering;
import com.qify.catalog.persistence.ServiceOfferingRepository;
import com.qify.fulfillment.domain.QueueRequest;
import com.qify.fulfillment.domain.RequestTimelineEntry;
import com.qify.fulfillment.persistence.QueueRequestRepository;
import com.qify.fulfillment.persistence.RequestTimelineEntryRepository;
import com.qify.identity.domain.Actor;
import com.qify.identity.persistence.ActorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueRequestCreationService {

    private final ActorRepository actorRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final QueueRequestRepository queueRequestRepository;
    private final RequestTimelineEntryRepository requestTimelineEntryRepository;
    private final Clock clock;

    public QueueRequestCreationService(ActorRepository actorRepository,
            ServiceOfferingRepository serviceOfferingRepository,
            QueueRequestRepository queueRequestRepository,
            RequestTimelineEntryRepository requestTimelineEntryRepository,
            Clock clock) {
        this.actorRepository = actorRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.queueRequestRepository = queueRequestRepository;
        this.requestTimelineEntryRepository = requestTimelineEntryRepository;
        this.clock = clock;
    }

    @Transactional
    public UUID create(UUID customerId, UUID serviceOfferingId, Instant scheduledFor,
            int expectedQueueMinutes, int arrivalNoticeMinutes) {
        Actor customer = actorRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        ServiceOffering serviceOffering = serviceOfferingRepository.findById(serviceOfferingId)
                .orElseThrow(() -> new IllegalArgumentException("Service offering not found: " + serviceOfferingId));
        Instant now = clock.instant();

        QueueRequest request = QueueRequest.create(UUID.randomUUID(), customer, serviceOffering,
                scheduledFor, expectedQueueMinutes, arrivalNoticeMinutes, now);
        RequestTimelineEntry timelineEntry = RequestTimelineEntry.initialFor(request, now);

        queueRequestRepository.save(request);
        requestTimelineEntryRepository.save(timelineEntry);
        return request.getId();
    }
}
