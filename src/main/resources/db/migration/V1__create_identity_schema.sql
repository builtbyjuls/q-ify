CREATE TABLE actors (
    id UUID PRIMARY KEY,
    alias VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT actors_alias_unique UNIQUE (alias),
    CONSTRAINT actors_alias_nonblank CHECK (length(btrim(alias)) > 0),
    CONSTRAINT actors_role_valid CHECK (role IN ('CUSTOMER', 'RUNNER', 'ADMIN'))
);

CREATE TABLE runner_profiles (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    availability VARCHAR(20) NOT NULL,
    CONSTRAINT runner_profiles_actor_fk FOREIGN KEY (actor_id) REFERENCES actors (id),
    CONSTRAINT runner_profiles_actor_unique UNIQUE (actor_id),
    CONSTRAINT runner_profiles_availability_valid CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE'))
);
