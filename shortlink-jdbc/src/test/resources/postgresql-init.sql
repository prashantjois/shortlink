CREATE TABLE shortlinks (
    ID SERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE,
    url TEXT,
    created_at BIGINT NOT NULL,
    expires_at BIGINT
);
