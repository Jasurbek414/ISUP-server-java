-- Add ISAPI fields to devices
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_ip      VARCHAR(50);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_username VARCHAR(50)  DEFAULT 'admin';
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_password VARCHAR(255);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS capabilities   TEXT;

-- Update existing rows: copy ip_address to device_ip if null
UPDATE devices SET device_ip = ip_address WHERE device_ip IS NULL AND ip_address IS NOT NULL;

-- access_rules table
CREATE TABLE IF NOT EXISTS access_rules (
    id           BIGSERIAL PRIMARY KEY,
    employee_no  VARCHAR(100) NOT NULL,
    rule_type    VARCHAR(20)  NOT NULL CHECK (rule_type IN ('blacklist','whitelist')),
    device_ids   TEXT,
    valid_from   TIMESTAMPTZ,
    valid_to     TIMESTAMPTZ,
    reason       VARCHAR(500),
    created_by   VARCHAR(100),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_access_rules_emp ON access_rules (employee_no);
CREATE INDEX IF NOT EXISTS idx_access_rules_type ON access_rules (rule_type);
