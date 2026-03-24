CREATE TABLE IF NOT EXISTS projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    webhook_url VARCHAR(500),
    secret_key  VARCHAR(100) NOT NULL,
    retry_count INT          NOT NULL DEFAULT 3,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS devices (
    id            BIGSERIAL PRIMARY KEY,
    device_id     VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name          VARCHAR(200),
    location      VARCHAR(300),
    model         VARCHAR(100),
    device_type   VARCHAR(50)  NOT NULL DEFAULT 'face_terminal',
    project_id    BIGINT REFERENCES projects (id) ON DELETE SET NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'offline',
    last_seen     TIMESTAMPTZ,
    ip_address    VARCHAR(50),
    firmware      VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_devices_project ON devices (project_id);
CREATE INDEX IF NOT EXISTS idx_devices_status ON devices (status);

CREATE TABLE IF NOT EXISTS event_logs (
    id               BIGSERIAL PRIMARY KEY,
    device_id        VARCHAR(64)  NOT NULL,
    project_id       BIGINT REFERENCES projects (id) ON DELETE SET NULL,
    event_type       VARCHAR(50)  NOT NULL,
    employee_no      VARCHAR(100),
    employee_name    VARCHAR(200),
    card_no          VARCHAR(100),
    direction        VARCHAR(20),
    door_no          INT,
    verify_mode      VARCHAR(50),
    event_time       TIMESTAMPTZ,
    photo_base64     TEXT,
    raw_payload      TEXT,
    webhook_status   VARCHAR(20)  NOT NULL DEFAULT 'pending',
    webhook_attempts INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_events_device ON event_logs (device_id);
CREATE INDEX IF NOT EXISTS idx_events_project ON event_logs (project_id);
CREATE INDEX IF NOT EXISTS idx_events_time ON event_logs (event_time DESC);
CREATE INDEX IF NOT EXISTS idx_events_webhook ON event_logs (webhook_status) WHERE webhook_status != 'delivered';

CREATE TABLE IF NOT EXISTS discovered_devices (
    id          BIGSERIAL PRIMARY KEY,
    ip          VARCHAR(50)  NOT NULL,
    mac         VARCHAR(20)  UNIQUE,
    model       VARCHAR(100),
    device_type VARCHAR(50),
    serial_no   VARCHAR(100),
    firmware    VARCHAR(100),
    activated   BOOLEAN      NOT NULL DEFAULT FALSE,
    first_seen  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_seen   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    claimed     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_discovered_ip ON discovered_devices (ip);

-- Default project
INSERT INTO projects (name, webhook_url, secret_key, is_active)
VALUES ('Default Project', NULL, 'change-me-secret', TRUE)
ON CONFLICT DO NOTHING;
