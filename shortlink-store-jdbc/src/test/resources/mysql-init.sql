CREATE TABLE shortlinks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    grp VARCHAR(255) NOT NULL,
    code VARCHAR(10)  NOT NULL,
    owner VARCHAR(255) NOT NULL,
    creator VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT,

    UNIQUE INDEX grp_code_unique (grp, code)
);
