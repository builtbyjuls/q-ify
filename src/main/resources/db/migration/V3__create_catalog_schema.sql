CREATE TABLE venues (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    CONSTRAINT venues_name_nonblank CHECK (length(btrim(name)) > 0)
);

CREATE TABLE service_offerings (
    id UUID PRIMARY KEY,
    venue_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL,
    delegation_approved BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT service_offerings_venue_fk FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT service_offerings_category_valid CHECK (category IN ('DINING', 'RETAIL', 'CUSTOMER_SERVICE')),
    CONSTRAINT service_offerings_category_unique UNIQUE (venue_id, category)
);
