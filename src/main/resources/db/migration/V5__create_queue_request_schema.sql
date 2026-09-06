CREATE TABLE queue_requests (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    service_offering_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    scheduled_for TIMESTAMPTZ NOT NULL,
    expected_queue_minutes INTEGER NOT NULL,
    arrival_notice_minutes INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT queue_requests_customer_fk FOREIGN KEY (customer_id) REFERENCES actors (id),
    CONSTRAINT queue_requests_service_offering_fk FOREIGN KEY (service_offering_id) REFERENCES service_offerings (id),
    CONSTRAINT queue_requests_status_valid CHECK (status IN ('REQUESTED', 'ASSIGNED', 'ARRIVED', 'IN_QUEUE', 'HANDOFF_READY', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT queue_requests_expected_queue_minutes_valid CHECK (expected_queue_minutes BETWEEN 1 AND 720),
    CONSTRAINT queue_requests_arrival_notice_minutes_valid CHECK (arrival_notice_minutes BETWEEN 0 AND 120)
);

CREATE TABLE request_timeline (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    performed_by_actor_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT request_timeline_request_fk FOREIGN KEY (request_id) REFERENCES queue_requests (id),
    CONSTRAINT request_timeline_performed_by_actor_fk FOREIGN KEY (performed_by_actor_id) REFERENCES actors (id),
    CONSTRAINT request_timeline_status_valid CHECK (status IN ('REQUESTED', 'ASSIGNED', 'ARRIVED', 'IN_QUEUE', 'HANDOFF_READY', 'COMPLETED', 'CANCELLED'))
);
