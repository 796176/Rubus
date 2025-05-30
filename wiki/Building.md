# Building guide

## Building jar files

- Install maven
- Under the project's directory execute:  
  `mvn clean package`
- Using an archive tool modify the `META-INF/MANIFEST.MF` file of the generated 
archive by adding:
  - `Main-Class: backend.spring.RubusConfiguration` line for Server
  - `Main-Class: frontend.spring.RubusConfiguration` line for Client

## Building binaries
### Linux
- Install gcc, libavcodec-dev, libavformat-dev, libavutils-dev, libswscale-dev
- Assign the java home directory to the JAVA_HOME environment variable and assign 
the parent directory of the directories that contain the ffmpeg declaration files to
the FFMPEG_HEADERS environment variable.
- Execute:  
  `gcc -c -fPIC -D_REENTRANT -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -I${FFMPEG_HEADERS}/libavcodec -I${FFMPEG_HEADERS}/libavformat -I${FFMPEG_HEADERS}/libavutil -I${FFMPEG_HEADERS}/libswscale frontend_decoders_FfmpegJniVideoDecoder.c frontend_decoders_FfmpegJniVideoDecoder.o`  
  `gcc -shared -fPIC -o librubus.so frontend_decoders_FfmpegJniVideoDecoder.o -lc -lpthread -lavcodec -lavformat -lavutil -lswscale`
