CREATE TABLE files (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    content_type TEXT NOT NULL,
    data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_files_created ON files (created_at DESC, id);
