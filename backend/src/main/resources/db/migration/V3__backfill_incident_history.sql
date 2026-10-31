-- Incidents created before the history existed get their "created" entry
INSERT INTO incident_history (incident_id, actor_id, field, old_value, new_value, created_at)
SELECT i.id, i.reporter_id, 'CREATED', NULL, NULL, i.created_at
FROM incidents i
WHERE NOT EXISTS (SELECT 1 FROM incident_history h WHERE h.incident_id = i.id);
