CREATE INDEX idx_incidents_created_at ON incidents (created_at);
CREATE INDEX idx_incidents_resolved_at ON incidents (resolved_at) WHERE resolved_at IS NOT NULL;
