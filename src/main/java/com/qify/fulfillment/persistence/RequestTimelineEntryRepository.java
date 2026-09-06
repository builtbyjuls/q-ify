package com.qify.fulfillment.persistence;

import com.qify.fulfillment.domain.RequestTimelineEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestTimelineEntryRepository extends JpaRepository<RequestTimelineEntry, Long> {
}
