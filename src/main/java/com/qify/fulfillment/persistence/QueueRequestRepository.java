package com.qify.fulfillment.persistence;

import java.util.UUID;

import com.qify.fulfillment.domain.QueueRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueRequestRepository extends JpaRepository<QueueRequest, UUID> {
}
