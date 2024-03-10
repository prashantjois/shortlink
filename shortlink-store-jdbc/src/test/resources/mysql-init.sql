CREATE TABLE shortlinks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id INT NOT NULL,
  code VARCHAR(10)  NOT NULL,
  owner VARCHAR(255) NOT NULL,
  creator VARCHAR(255) NOT NULL,
  url TEXT NOT NULL,
  created_at BIGINT NOT NULL,
  expires_at BIGINT,

  UNIQUE INDEX group_code_unique (group_id, code)
);

CREATE TABLE shortlink_groups (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE
);
