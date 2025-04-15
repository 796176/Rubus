create table media (
    id blob,
    title blob,
    videoWidth int,
    videoHeight int,
    duration int,
    videoEncoding blob,
    audioEncoding blob,
    videoContainer blob,
    audioContainer blob,
    contentPath blob
);
insert into media (
    id,
    title,
    videoWidth,
    videoHeight,
    duration,
    videoEncoding,
    audioEncoding,
    videoContainer,
    audioContainer,
    contentPath
) values (
    X'ab',
    X'5469746c6531',
    854,
    480,
    1,
    X'6e756c6c31',
    X'6e756c6c32',
    X'6e756c6c33',
    X'6e756c6c34',
    null
);
insert into media (
    id,
    title,
    videoWidth,
    videoHeight,
    duration,
    videoEncoding,
    audioEncoding,
    videoContainer,
    audioContainer,
    contentPath
) values (
    X'cd',
    X'5469746c6532',
    1280,
    720,
    2,
    X'6e756c6c31',
    X'6e756c6c32',
    X'6e756c6c33',
    X'6e756c6c34',
    null
);
