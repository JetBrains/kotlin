# Simple video player

This example shows how one could implement a video player in Kotlin.
Almost any video file supported by ffmpeg could be played with it.
ffmpeg and SDL2 is needed for that to work, i.e.

     port install ffmpeg-devel
     brew install ffmpeg sdl2
     apt install libavcodec-dev libavformat-dev libavutil-dev libswscale-dev libswresample-dev
     apt install libsdl2-dev
     pacman -S mingw-w64-x86_64-SDL2 mingw-w64-x86_64-ffmpeg

To build use `../gradlew assemble`.

To run use `./build/bin/videoPlayer/main/release/executable/videoplayer.kexe <file>.mp4`.
