# Installation Guide

## Requirements
 
- Both Client and Server require JRE 21 or newer
- For Server
    - Postgres 17 or newer
- For Client
    - Ffmpeg

## Setting up Server

- First you need to get the jar file. You can download it from 
[the release page](https://github.com/796176/Rubus/releases) or 
[build it yourself](https://github.com/796176/Rubus/wiki/Building).

- Next you have to set up Postgres and create a database. Then create a table named 
`media` with the following column names and their types: id text 
( primary key ), duration int, media_content_uri text, title_tsvector tsvector 
(generated column). You have to do so with the following command:

      CREATE TABLE media (
          id text PRIMARY KEY DEFAULT gen_random_uuid(),
          title text NOT NULL,
          duration integer NOT NULL,
          media_content_uri text NOT NULL,
          title_tsvector tsvector GENERATED ALWAYS AS (to_tsvector('english', title)) STORED
      );

  More details on the table columns
[here](https://github.com/796176/Rubus/wiki/Configuration).

- Now you have to create a directory and place the server configuration file named 
`rubus.conf` under it. An example of the server configuration file can be found
[here](https://github.com/796176/Rubus/blob/master/examples/configuration/server/rubus.conf).

- Now you can launch Server using the following command:

  `java -Drubus.workingDir="/path/to/server/directory" -Drubus.db.user="postgresName" -Drubus.db.password="postgresPassword" -jar /path/to/file.jar`


## Setting up Client

- First you need to get the jar file. You can download it from
  [the release page](https://github.com/796176/Rubus/releases) or
  [build it yourself](https://github.com/796176/Rubus/wiki/Building).

- In addition to the jar file, you have to download a binary file from
  [the release page](https://github.com/796176/Rubus/releases) or
  [build it yourself](https://github.com/796176/Rubus/wiki/Building).

- Then create a directory and place the client configuration file with named 
`rubus.conf` under it. An example of the client configuration file can be found 
[here](https://github.com/796176/Rubus/blob/master/examples/configuration/client/rubus.conf).

- Now you can launch Client using the following command:

  `java -Drubus.workingDir="/path/to/client/directory" -Djava.library.path="/path/to/directory/containing/binary" -jar /path/to/file.jar`