package com.qify.identity.application;

import java.util.UUID;

import com.qify.identity.domain.ActorRole;

public record CurrentActor(UUID id, String alias, ActorRole role) {
}
