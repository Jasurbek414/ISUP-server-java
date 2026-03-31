-- Add photo_path to event_logs table
ALTER TABLE event_logs ADD COLUMN photo_path VARCHAR(512);

-- Update description
COMMENT ON COLUMN event_logs.photo_path IS 'Path to the JPG file in the local storage';
