 CREATE TABLE media (
    id clob,
    title clob,
    duration int,
    media_content_uri clob
);
INSERT INTO media (
    id,
    title,
    duration,
    media_content_uri
) VALUES (
    '00000000-0000-4000-b000-000000000000',
    'Title1',
    1,
    NULL
);
INSERT INTO media (
    id,
    title,
    duration,
    media_content_uri
) VALUES (
    '11111111-1111-4111-b111-111111111111',
    'Title2',
    2,
    NULL
);
