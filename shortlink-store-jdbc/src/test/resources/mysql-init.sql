CREATE TABLE shortlinks (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) UNIQUE,
    url TEXT,
    created_at BIGINT NOT NULL,
    expires_at BIGINT,
    UNIQUE(code)
);
