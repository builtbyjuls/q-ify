package com.qify.identity.web;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class IdentityWebMvcConfiguration implements WebMvcConfigurer {

    private final CurrentActorArgumentResolver currentActorArgumentResolver;

    public IdentityWebMvcConfiguration(CurrentActorArgumentResolver currentActorArgumentResolver) {
        this.currentActorArgumentResolver = currentActorArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(currentActorArgumentResolver);
    }
}
