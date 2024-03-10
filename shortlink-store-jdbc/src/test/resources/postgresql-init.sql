CREATE TABLE shortlinks (
  id SERIAL PRIMARY KEY,
  group_id INTEGER NOT NULL,
  code VARCHAR NOT NULL,
  owner VARCHAR NOT NULL,
  creator VARCHAR NOT NULL,
  url VARCHAR NOT NULL,
  created_at BIGINT NOT NULL,
  expires_at BIGINT,

  CONSTRAINT group_code_unique UNIQUE (group_id, code)
);

CREATE TABLE shortlink_groups (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE
);
