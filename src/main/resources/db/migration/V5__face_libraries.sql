CREATE TABLE IF NOT EXISTS face_libraries (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    project_id  BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    type        VARCHAR(20) NOT NULL DEFAULT 'whitelist'
                CHECK (type IN ('whitelist','blacklist','vip','staff','visitor')),
    device_ids  TEXT,
    face_count  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS face_records (
    id            BIGSERIAL PRIMARY KEY,
    library_id    BIGINT NOT NULL REFERENCES face_libraries(id) ON DELETE CASCADE,
    employee_no   VARCHAR(100) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    gender        VARCHAR(10) DEFAULT 'male',
    phone         VARCHAR(30),
    department    VARCHAR(100),
    position      VARCHAR(100),
    photo_base64  TEXT,
    photo_url     VARCHAR(500),
    valid_from    TIMESTAMPTZ,
    valid_to      TIMESTAMPTZ,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    upload_status VARCHAR(20) DEFAULT 'pending',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_face_records_library ON face_records(library_id);
CREATE INDEX IF NOT EXISTS idx_face_records_employee ON face_records(employee_no);
