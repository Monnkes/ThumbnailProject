\c postgres

CREATE TABLE images (
    id SERIAL PRIMARY KEY,
    data BYTEA NOT NULL,
    image_order INT UNIQUE
);

CREATE TABLE thumbnails (
    id SERIAL PRIMARY KEY,
    data BYTEA NOT NULL,
    type VARCHAR(10) NOT NULL,
    image_id BIGINT,
    thumbnail_order INT,
    CONSTRAINT fk_image_id FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
);
