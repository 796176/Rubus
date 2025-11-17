# Building guide

## Building jar files

- Install maven
- Under the project's directory execute:  
  - `mvn clean package -P server` to build a Rubus server jar
  - `mvn clean package -P client` to build a Rubus client jar

## Building binaries

### Linux
- Install gcc, libavcodec-dev, libavformat-dev, libavutils-dev, libswscale-dev
- Assign the Java home directory to the JAVA_HOME environment variable and assign 
the parent directory of the directories that contain the ffmpeg declaration files to
the FFMPEG_HEADERS environment variable
- Execute:  
  `gcc -c -fPIC -D_REENTRANT -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -I${FFMPEG_HEADERS}/libavcodec -I${FFMPEG_HEADERS}/libavformat -I${FFMPEG_HEADERS}/libavutil -I${FFMPEG_HEADERS}/libswscale frontend_decoders_FfmpegJniVideoDecoder.c frontend_decoders_FfmpegJniVideoDecoder.o`  
  `gcc -shared -fPIC -o librubus.so frontend_decoders_FfmpegJniVideoDecoder.o -lc -lpthread -lavcodec -lavformat -lavutil -lswscale`

## Building the Docker image

> #### Note
>
> The built image includes AOT cache to reduce the cold startup time. Its generation 
  requires running the application during the building stage. That, in turn,
  necessitates the rest of the infrastructure to be up and running.

- Install Docker
- Configure PostgreSQL according to `rubus.conf.aot` ( the file contains the default
PostgreSQL configuration with PostgreSQL running locally ), or modify `rubus.conf.aot` 
to reflect the PostgreSQL configuration

  `Dockerfile` declares the `DB_USER` and `DB_PASSWORD` arguments with `postgres` as
their default value, which can be overridden if necessary
- Under the project's directory execute:

  `docker build --network=host --build-arg=VERSION=$(cat src/main/resources/version) .`

  > #### Note
  > 
  > If PostgreSQL isn't run locally the `--network=host` flag is redundant.