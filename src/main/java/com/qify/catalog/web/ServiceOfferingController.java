package com.qify.catalog.web;

import java.util.List;

import com.qify.catalog.persistence.ServiceOfferingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/service-offerings")
public class ServiceOfferingController {

    private final ServiceOfferingRepository serviceOfferingRepository;

    public ServiceOfferingController(ServiceOfferingRepository serviceOfferingRepository) {
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    @GetMapping
    public List<ServiceOfferingResponse> getServiceOfferings() {
        return serviceOfferingRepository.findActiveApprovedOfferings().stream()
                .map(offering -> new ServiceOfferingResponse(
                        offering.getId(),
                        offering.getVenue().getId(),
                        offering.getVenue().getName(),
                        offering.getCategory()))
                .toList();
    }
}
