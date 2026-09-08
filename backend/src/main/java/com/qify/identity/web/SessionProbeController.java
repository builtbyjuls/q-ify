package com.qify.identity.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/probe/session")
class SessionProbeController {

    private static final String VISITS_ATTRIBUTE = "qify.probe.visits";

    @GetMapping
    SessionProbe session(HttpSession session) {
        Integer previousVisits = (Integer) session.getAttribute(VISITS_ATTRIBUTE);
        int visits = previousVisits == null ? 1 : previousVisits + 1;
        session.setAttribute(VISITS_ATTRIBUTE, visits);
        return new SessionProbe(visits);
    }

    record SessionProbe(int visits) {
    }
}
