-- Additional indexes for performance
CREATE INDEX IF NOT EXISTS idx_events_employee_no ON event_logs (employee_no) WHERE employee_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_events_device_time ON event_logs (device_id, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_events_created ON event_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices (device_id);
CREATE INDEX IF NOT EXISTS idx_devices_status_online ON devices (status) WHERE status = 'online';

-- Cleanup function for old events
CREATE OR REPLACE FUNCTION cleanup_old_events(retention_days INTEGER)
RETURNS INTEGER AS $$
DECLARE
  deleted_count INTEGER;
BEGIN
  DELETE FROM event_logs WHERE created_at < NOW() - (retention_days || ' days')::INTERVAL;
  GET DIAGNOSTICS deleted_count = ROW_COUNT;
  RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;
