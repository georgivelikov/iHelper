-- Category order: a per-owner position, set by drag-and-drop. Existing categories keep the
-- alphabetical order they were shown in until now.
ALTER TABLE category ADD COLUMN position integer NOT NULL DEFAULT 0;

UPDATE category c
   SET position = r.rn - 1
  FROM (SELECT id, row_number() OVER (PARTITION BY owner_id ORDER BY lower(name), id) AS rn
          FROM category
         WHERE deleted_at IS NULL) r
 WHERE c.id = r.id;

-- Bin: a soft-deleted task (deleted_at set) is in the bin until the bin is emptied.
-- Emptying wipes the content and stamps purged_at; the row stays as a tombstone so /sync can
-- still tell other devices that the task is gone.
ALTER TABLE task ADD COLUMN purged_at timestamptz;

CREATE INDEX ix_task_owner_bin ON task (owner_id, deleted_at) WHERE deleted_at IS NOT NULL AND purged_at IS NULL;
