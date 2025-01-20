\c postgres

CREATE TABLE images (
    id SERIAL PRIMARY KEY,
    data BYTEA NOT NULL,
    image_order INT,
    folder_id INT NOT NULL
);

CREATE TABLE thumbnails (
    id SERIAL PRIMARY KEY,
    data BYTEA NOT NULL,
    type VARCHAR(10) NOT NULL,
    image_id BIGINT,
    thumbnail_order INT,
    CONSTRAINT fk_image_id FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
);

CREATE TABLE folders (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT NOT NULL
);
