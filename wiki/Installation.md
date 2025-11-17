# Installation Guide

## Requirements
 
- Both Client and Server require JRE version 25 or greater
- For Server
    - Postgres version 17 or greater
- For Client
    - Ffmpeg

## Setting up Server

### Docker

 - Install Docker Compose on your machine
 - Create a file named `compose.yaml` with the following content:


    services:
      postgresql:
        image: javarubus/db
        volumes:
          - type: bind
            source: <persistence_directory>
            target: /var/lib/postgresql/data
        environment:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: rubus
      rubus:
        image: javarubus/rubus
        volumes:
          - type: bind
            source: <working_directory>
            target: /var/opt/rubus
          - type: bind
            source: <media_directory>
            target: /var/lib/rubus
        environment:
          DB_USER: postgres
          DB_PASSWORD: postgres
        ports:
          - 0.0.0.0:54300:54300
        depends_on:
          - postgresql


 - Replace the placeholders with appropriate values
   - \<persistence_directory\> is the directory where the database content is stored
   - \<working_directory\> is the Rubus working directory
   - \<media_directory\> is the directory to store media locally
 - Open a terminal under the same directory where you created `compose.yaml` and 
type in:

    `docker compose up -d`

---

If the configuration file isn't present at the working directory, a default one is 
created. Changing the default database configuration values ( user, password, database 
name, hostname, port ), Rubus Server configuration values ( hostname, port ) in 
`compose.yaml` requires changing the configuration file as well.

The Rubus Server container features the following environment variables:
 - `DB_USER` ( required ) the database username
 - `DB_PASSWORD` ( required ) the database password
 - `JVM_OPTIONS` ( optional ) extra JVM options using the same syntax as when typing 
`java [options] -jar file.jar`

For customization of the database container refer to <https://hub.docker.com/_/postgres>.

### Traditional approach

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