package com.qify.catalog.persistence;

import java.util.List;
import java.util.UUID;

import com.qify.catalog.domain.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    @Query("""
            select offering
            from ServiceOffering offering
            join fetch offering.venue venue
            where offering.delegationApproved = true
              and offering.active = true
            order by venue.name asc, offering.category asc, offering.id asc
            """)
    List<ServiceOffering> findActiveApprovedOfferings();
}
