package com.qify.identity.web;

import java.util.UUID;

import com.qify.identity.application.CurrentActor;
import com.qify.identity.domain.Actor;
import com.qify.identity.persistence.ActorRepository;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentActorArgumentResolver implements HandlerMethodArgumentResolver {

    static final String ACTOR_ID_HEADER = "X-Actor-Id";

    private final ActorRepository actorRepository;

    public CurrentActorArgumentResolver(ActorRepository actorRepository) {
        this.actorRepository = actorRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentActor.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String actorIdHeader = webRequest.getHeader(ACTOR_ID_HEADER);
        if (actorIdHeader == null || actorIdHeader.isBlank()) {
            throw ActorAuthenticationException.missingIdentity();
        }

        UUID actorId;
        try {
            actorId = UUID.fromString(actorIdHeader);
        } catch (IllegalArgumentException exception) {
            throw ActorAuthenticationException.invalidIdentity();
        }
        if (!actorId.toString().equalsIgnoreCase(actorIdHeader)) {
            throw ActorAuthenticationException.invalidIdentity();
        }

        Actor actor = actorRepository.findById(actorId)
                .orElseThrow(ActorAuthenticationException::invalidIdentity);
        return new CurrentActor(actor.getId(), actor.getAlias(), actor.getRole());
    }
}
