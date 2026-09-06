package com.qify.catalog.web;

import java.util.UUID;

import com.qify.catalog.domain.ServiceCategory;

public record ServiceOfferingResponse(UUID id, UUID venueId, String venueName, ServiceCategory category) {
}
