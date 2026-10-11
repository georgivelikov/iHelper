CREATE TABLE category (
    id          uuid         PRIMARY KEY,
    name        varchar(100) NOT NULL,
    color       varchar(7),
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    version     bigint       NOT NULL DEFAULT 0,
    deleted_at  timestamptz
);

-- names are unique among live categories only, so a deleted name can be reused
CREATE UNIQUE INDEX uq_category_name_live ON category (lower(name)) WHERE deleted_at IS NULL;
CREATE INDEX ix_category_updated_at ON category (updated_at);

CREATE TABLE task (
    id           uuid          PRIMARY KEY,
    title        varchar(200)  NOT NULL,
    description  varchar(5000),
    category_id  uuid          REFERENCES category (id),
    -- ordinal of the Priority enum: 0 LOW, 1 MEDIUM, 2 HIGH, 3 URGENT (so it sorts correctly)
    priority     smallint      NOT NULL DEFAULT 1 CHECK (priority BETWEEN 0 AND 3),
    status       varchar(20)   NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    due_date     date,
    completed_at timestamptz,
    created_at   timestamptz   NOT NULL,
    updated_at   timestamptz   NOT NULL,
    version      bigint        NOT NULL DEFAULT 0,
    deleted_at   timestamptz
);

CREATE INDEX ix_task_category ON task (category_id);
CREATE INDEX ix_task_updated_at ON task (updated_at);

INSERT INTO category (id, name, color, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Personal', '#6366f1', now(), now()),
    (gen_random_uuid(), 'Work',     '#0ea5e9', now(), now()),
    (gen_random_uuid(), 'Home',     '#22c55e', now(), now()),
    (gen_random_uuid(), 'Shopping', '#f59e0b', now(), now()),
    (gen_random_uuid(), 'Ideas',    '#ec4899', now(), now());
