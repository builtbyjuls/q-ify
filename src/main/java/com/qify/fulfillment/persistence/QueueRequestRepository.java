package com.qify.fulfillment.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.qify.fulfillment.domain.QueueRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueRequestRepository extends JpaRepository<QueueRequest, UUID> {

    @Query("""
            select request
            from QueueRequest request
            join fetch request.serviceOffering offering
            join fetch offering.venue
            where request.customer.id = :customerId
            order by request.createdAt desc, request.id asc
            """)
    List<QueueRequest> findAllOwnedWithOfferingAndVenue(@Param("customerId") UUID customerId);

    @Query("""
            select request
            from QueueRequest request
            join fetch request.serviceOffering offering
            join fetch offering.venue
            where request.id = :requestId
              and request.customer.id = :customerId
            """)
    Optional<QueueRequest> findOwnedWithOfferingAndVenue(@Param("requestId") UUID requestId,
            @Param("customerId") UUID customerId);
}
