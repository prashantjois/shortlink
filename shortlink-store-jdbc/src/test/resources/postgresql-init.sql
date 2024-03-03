CREATE TABLE shortlinks (
  id SERIAL PRIMARY KEY,
  grp VARCHAR NOT NULL,
  code VARCHAR NOT NULL,
  owner VARCHAR NOT NULL,
  creator VARCHAR NOT NULL,
  url VARCHAR NOT NULL,
  created_at BIGINT NOT NULL,
  expires_at BIGINT,

  CONSTRAINT grp_code_unique UNIQUE (grp, code)
);
