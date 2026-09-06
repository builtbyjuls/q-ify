package com.qify.identity.web;

import java.util.List;

import com.qify.identity.persistence.ActorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoActorController {

    private final ActorRepository actorRepository;

    public DemoActorController(ActorRepository actorRepository) {
        this.actorRepository = actorRepository;
    }

    @GetMapping("/api/v1/demo/actors")
    public List<DemoActorResponse> actors() {
        return actorRepository.findAllByOrderByAliasAsc().stream()
                .map(actor -> new DemoActorResponse(actor.getId(), actor.getAlias(), actor.getRole()))
                .toList();
    }
}
