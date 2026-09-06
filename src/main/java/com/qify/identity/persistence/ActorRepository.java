package com.qify.identity.persistence;

import java.util.List;
import java.util.UUID;

import com.qify.identity.domain.Actor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActorRepository extends JpaRepository<Actor, UUID> {

    List<Actor> findAllByOrderByAliasAsc();
}
