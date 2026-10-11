CREATE TABLE app_user (
    id            uuid         PRIMARY KEY,
    username      varchar(50)  NOT NULL,
    password_hash varchar(100) NOT NULL,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    version       bigint       NOT NULL DEFAULT 0,
    deleted_at    timestamptz
);

CREATE UNIQUE INDEX uq_app_user_username ON app_user (lower(username));

-- Opaque refresh tokens are stored hashed (SHA-256), rotated on every use.
CREATE TABLE refresh_token (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES app_user (id),
    token_hash varchar(64) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz
);

CREATE INDEX ix_refresh_token_user ON refresh_token (user_id);

-- Rows that predate accounts belong to a placeholder user that cannot log in
-- ('!' is not a valid BCrypt hash, so no password ever matches).
INSERT INTO app_user (id, username, password_hash, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'local', '!', now(), now());

ALTER TABLE category ADD COLUMN owner_id uuid;
ALTER TABLE task ADD COLUMN owner_id uuid;
UPDATE category SET owner_id = '00000000-0000-0000-0000-000000000001';
UPDATE task SET owner_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE category ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE task ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE category ADD CONSTRAINT fk_category_owner FOREIGN KEY (owner_id) REFERENCES app_user (id);
ALTER TABLE task ADD CONSTRAINT fk_task_owner FOREIGN KEY (owner_id) REFERENCES app_user (id);

-- category names are now unique per owner among live categories
DROP INDEX uq_category_name_live;
CREATE UNIQUE INDEX uq_category_owner_name_live ON category (owner_id, lower(name)) WHERE deleted_at IS NULL;
CREATE INDEX ix_category_owner_updated_at ON category (owner_id, updated_at);
CREATE INDEX ix_task_owner_updated_at ON task (owner_id, updated_at);
