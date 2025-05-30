# Configuration guide

## Configuration file options

available-threads-limit [server] sets a limit on how many cpu threads can be utilized
to process incoming requests.

bind-address [client/server] for the client this option specifies the destination 
address of the server; for the server this option specifies the ip address the server
is bound to.

buffer-size [client] is the maximum size of the player's buffer in seconds.

certificate-location [server] sets the location of an X.509 certificate that will be 
used to establish a secure connection between 2 hosts.

connection-protocol [client/server] specifies the transport layer protocol; this 
option must be the same between the server and all the client that connect to 
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
minimum-batch-size, the client requests any desirable amount.

open-connections-limit [server] sets a limit of how many connection the server can 
keep open at a time.

secure-connection-enabled [client/server] specifies if a secure connection may be 
established between 2 hosts. If the server wants to enable secure connections it 
must also specify certificate-location and private-key-location.

secure-connection-handshake-disabled [clint/server] specifies that the host doesn't
need to use a handshake to determine if a secure connection can be established. In
order to work this option needs to be set on both hosts.

secure-connection-handshake-timeout [client/server] sets the timeout of a handshake
that determines if a secure connection can be established between 2 hosts and if it
can it's being established during that handshake.

secure-connection-required [client/server] lets the host know if an unsecure 
connection may be established between 2 hosts.

private-key-location [server] sets the location of an unencrypted PKCS8 private 
key that is used together with the certificate specified in certificate-location.

## Populating Server with media

Every media is stored in a separate directory. The video and audio streams of a media
are separated and split so every clip duration is ~1 second and every clip of a stream
has the same properties. For example every video clip of a video stream has the same 
resolution, frame-rate, codec, container, type, pixel format, etc. The convention 
regarding properties is necessary to speed up the decoding process on the client side.

The next step is to put these clips into the directory using the following naming 
scheme:
 - an audio clip has a name `a*.container_format` where `*` is a sequential number of 
the clip starting with 0 and `container_format` is a container extension
 - a video clip has a name `v*.container_format` where `*` is a sequential number of 
the clip starting with 0 and `container_fomrat` is a container extension 

Lastly, populate the media table accordingly where
 - id is a unique id of a media. It is assumed the value is a result of a hash 
function where the input is a concatenation of the title of a media and the current 
timestamp, or any other input value that is likely to be unique.
 - title is a UTF8 encoded media title
 - videoWidth, videoHeight, duration are self-explanatory
 - videoEncoding, audioEncoding are encoding formats video and audio clips 
respectively ( not used since 1.0.0 )
 - videoContainer, audioContainer are UTF8 encoded container extensions of video 
and audio clips respectively
 - contentPath is a UTF8 encoded location of a directory containing media clips 
