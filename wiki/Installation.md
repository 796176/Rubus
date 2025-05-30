# Installation Guide

## Dependencies
 
- Both Client and Server require JRE 21 or newer
- For Server
    - Postgres 17 or newer
- For Client
    - Ffmpeg

## Setting up Server

- First you need to get a jar file. You can download it from 
[the release page](https://github.com/796176/Rubus/releases) or 
[build it yourself](https://github.com/796176/Rubus/wiki/Building).

- Next you have to set up Postgres and create a database. Then create a table with
the name `media` with the following column names and their types: id bytea, 
videoWidth int, videoHeight int, duration int, videoEncoding bytea, audioEncoding bytea, 
videoContainer bytea, contentPath bytea; where the primary key id. You can do so with 
the following command:

    `create table media (id bytea, title bytea, videoWidth int, videoHeight int, duration int, videoEncoding bytea, audioEncoding bytea, videoContainer bytea, audioContainer bytea, contentPath bytea, primary key(id));`

    Further details on table population and media hierarchy can be found 
[here](https://github.com/796176/Rubus/wiki/Configuration)

- Now you have to create a directory and place the server configuration file with 
the name `rubus.conf` under it. An example of a server configuration file can be found
[here](https://github.com/796176/Rubus/blob/master/examples/configuration/server/rubus.conf).

- Now you can launch Server with the following command:

  `java -Drubus.workingDir="/path/to/server/directory" -Drubus.db.user="postgresName" -Drubus.db.password="postgresPassword" -jar /path/to/file.jar`


## Setting up Client

- First you need to get a jar file. You can download it from
  [the release page](https://github.com/796176/Rubus/releases) or
  [build it yourself](https://github.com/796176/Rubus/wiki/Building).

- In addition to the jar file, you have to download a binary file from
  [the release page](https://github.com/796176/Rubus/releases) or
  [build it yourself](https://github.com/796176/Rubus/wiki/Building).

- Then create a directory and place the client configuration file with the name 
`rubus.conf` under it. An example of a client configuration file can be found 
[here](https://github.com/796176/Rubus/blob/master/examples/configuration/client/rubus.conf).

- Now you can launch Client with the following command:

  `java -Drubus.workingDir="/path/to/client/directory" -Djava.library.path="/path/to/directory/containing/binary" -jar /path/to/file.jar`