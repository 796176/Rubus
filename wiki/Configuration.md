# Configuration guide

## Configuration file options

available-threads-limit [server] sets a limit on how many cpu threads can be utilized
to process incoming requests.

bind-address [client/server] for the client this option specifies the destination 
address of the server; for the server this option specifies the ip address the server
is bound to.

buffer-size [client] is the maximum size of the player's buffer in seconds.

certificate-location [server] sets the location of an X.509 certificate that will be 
used to establish secure connections between the server and the clients.

connection-protocol [client/server] specifies the transport layer protocol; this 
option must be the same between the server and all the clients that connect to 
that server.  
The list of supported transport layer protocols:
 - tcp

database-address [server] specifies the internet address of the Postgres dbms server.

database-name [server] specifies the name of the database containing the `media` table.

database-port [server] specifies the port number of the Postgres dbms server.

interface-language [client] specifies the interface language of the clint's user 
interface.

listening-port [client/server] for the client this option specifies the destination 
port of the server; for the server this option species the port the server occupies.

look-and-feel [client] is a fully qualified name of the class that represents 
a specific look and feel.

main-frame-height [client] is the height of the main window.

main-frame-width [client] is the width of the main window.

main-frame-x [client] specifies the x coordinate of the upper-left corner of 
the main window.

main-frame-y [client] specifies the y coordinate of the upper-left corner of 
the main window.

minimum-batch-size [client] specifies the minimum amount of media clips the client
requests from the server; if the amount of available media clips is less than
minimum-batch-size, the client requests less than that.

open-connections-limit [server] sets a limit on how many connections the server can 
keep open at a time.

private-key-location [server] sets the location of the unencrypted PKCS8 private
key that is used together with the certificate specified in certificate-location to
establish secure connections between the server and the clients.

secure-connection-enabled [client/server] specifies if a secure connection may be 
established between 2 hosts. If the server wants to enable secure connections it 
must also specify certificate-location and private-key-location.

secure-connection-handshake-disabled [clint/server] specifies that the host doesn't
need to perform a handshake to determine if a secure connection can be established. In
order to work this option needs to be set true for both hosts.

secure-connection-handshake-timeout [client/server] sets the timeout of a handshake
that determines if a secure connection can be established between 2 hosts and if it
can it's being established during that handshake.

secure-connection-required [client/server] lets the host know if an unsecure 
connection may be established between 2 hosts.

transaction-failure-retry-attempts [server] specifies how many times a transaction
that failed due to a serialization failure can be retried.

transaction-timeout [server] specifies the timeout of a transaction in seconds.

## Populating Server with media

Single media consists of associated resources and a record in the table that stores 
a URI to the location where the resources are located at alongside other meta-information. 
The URI can point to a directory, a remote storage, etc.; as of the most recent version 
the URI is always a local directory.

The resources associated with the media are audio clips and video clips. Every clip is
1 second long and stores the same amount of frames. The audio clips and the video clips
must have the same encoding configuration e.g. all the video clips of
the media must have the same resolution, container type, codec type, etc.

The naming scheme of resources when the URI is a local directory is as follows:
 - an audio clip has a name `a*` where `*` is a sequential number of the clip 
starting with 0
 - a video clip has a name `v*` where `*` is a sequential number of the clip 
starting with 0

The description of the columns of the `media` table:
 - id is an GUID v4 value associated with the media. Every id must be unique
 - title is the media title
 - duration is the media duration
 - media_content_uri is a URI that points to the location where the resources are 
stored
 - title_tsvector is a generated column that calculates tsvector based on `title`

## Further optimization

This section is optional, but it gives some tips on improving the Server performance.

### Indexation
Indexation allows for a faster querying.

`CREATE INDEX media_id_index ON media (id);` to create an index on `id`

`CREATE INDEX media_id_title_search_index ON media USING GIN (title_tsvector);` to
create an index on `title_tsvector`

### VACUUM ANALYZE

When the content of the table has changed significantly it's recommended to run 
`VACUUM ANALYZE media;`. Consider creating a script that runs this command every 
day/week/month.