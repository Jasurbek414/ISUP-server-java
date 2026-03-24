-- V4: Device enhancements and event_logs index improvements

-- Add extended device metadata columns
ALTER TABLE devices ADD COLUMN IF NOT EXISTS serial_no VARCHAR(100);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS mac_address VARCHAR(20);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS firmware_version VARCHAR(100);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_port INTEGER DEFAULT 80;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS use_https BOOLEAN DEFAULT FALSE;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_channels INTEGER DEFAULT 1;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS notes TEXT;

-- Add event_type index for faster filtering
CREATE INDEX IF NOT EXISTS idx_events_event_type ON event_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_events_employee_no ON event_logs(employee_no);
CREATE INDEX IF NOT EXISTS idx_events_event_time ON event_logs(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_events_device_id ON event_logs(device_id);
