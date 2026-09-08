package com.qify.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/probe")
class ProbeController {

    @GetMapping
    ProbeStatus probe() {
        return new ProbeStatus("ready");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void acceptMutation() {
    }

    record ProbeStatus(String status) {
    }
}
