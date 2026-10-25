CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL
                  CHECK (role IN ('ADMIN', 'AGENT', 'USER')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE incidents (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                CHECK (status IN ('OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'RESOLVED', 'CLOSED')),
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM'
                CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    category    VARCHAR(50),
    reporter_id BIGINT       NOT NULL REFERENCES users (id),
    assignee_id BIGINT       REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_incidents_status   ON incidents (status);
CREATE INDEX idx_incidents_assignee ON incidents (assignee_id);
CREATE INDEX idx_incidents_reporter ON incidents (reporter_id);

CREATE TABLE comments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    incident_id BIGINT      NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    author_id   BIGINT      NOT NULL REFERENCES users (id),
    body        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_incident ON comments (incident_id);

CREATE TABLE incident_history (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    incident_id BIGINT       NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    actor_id    BIGINT       NOT NULL REFERENCES users (id),
    field       VARCHAR(50)  NOT NULL,
    old_value   VARCHAR(255),
    new_value   VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_history_incident ON incident_history (incident_id);
