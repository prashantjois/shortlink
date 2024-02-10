CREATE TABLE shortlinks (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    owner VARCHAR(255),
    creator VARCHAR(255),
    code VARCHAR(10) UNIQUE,
    url TEXT,
    created_at BIGINT NOT NULL,
    expires_at BIGINT,
    UNIQUE(code),
    INDEX idx_owner (owner)
);

