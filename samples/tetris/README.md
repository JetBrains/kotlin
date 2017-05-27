# Tetris game

This example shows implementation of simple Tetris game using SDL
(Simple DirectMedia Layer) library for rendering. SDL allows easy development
of cross-platform game and multimedia applications.

Start with building compiler by using `dist` and `cross_dist` for cross-targets (unless
using binary distribution).

Install SDL2 development files (see https://www.libsdl.org/download-2.0.php). For Mac -
copy `SDL2.framework` to `$HOME/Library/Frameworks`. For Debian-like Linux -
use `apt-get install libsdl2-dev`.

To build Tetris application for your host platform use 
    
    `../gradlew build` 
    
This task builds the sample for your host platform. Note that SDL2 must be installed on the host.

To run it on the host use `../gradlew run`

Alternatively you can run artifact directly 

     ./build/konan/bin/Tetris${target}/Tetris${target}.kexe

For cross-compilation to iOS (on Mac host) use gradle property

	konan.build.targets=all
    
For cross-compilation to Raspberry Pi (on Linux host) use

	konan.build.targets=all

During build process compilation script creates interoperability bindings to SDL2, using SDL C headers,
and then compiles an application with the produced bindings.

To deploy executable to iPhone device take Info.plist, then use XCode and your own private signing identity.

To run on Raspberry Pi one need to install SDL package with `apt-get install libsdl2-2.0.0` on the Pi. 
Also GLES2 renderer is recommended (use `SDL_RENDER_DRIVER=opengles2 ./Tetris.kexe`).

