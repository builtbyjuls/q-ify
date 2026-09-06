package com.qify.identity.web;

import java.util.UUID;

import com.qify.identity.domain.ActorRole;

public record DemoActorResponse(UUID id, String alias, ActorRole role) {
}
