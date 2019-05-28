# Tetris game

This example shows implementation of simple Tetris game using SDL
(Simple DirectMedia Layer) library for rendering. SDL allows easy development
of cross-platform game and multimedia applications.

Install SDL2 development files (see https://www.libsdl.org/download-2.0.php). For Mac -
copy `SDL2.framework` to `$HOME/Library/Frameworks`. For Debian-like Linux -
use `apt-get install libsdl2-dev`.
For Windows - `pacman -S mingw-w64-x86_64-SDL2` in MinGW64 console
or `pacman -S mingw-w64-i686-SDL2` in MinGW32 console. If you do not have MSYS2-MinGW64 (MSYS2-MinGW32)
installed - install it first as described in http://www.msys2.org

To build Tetris application for your host platform use `../gradlew assemble`.

Note that SDL2 must be installed on the host.

Now you can run the game using `../gradlew runReleaseExecutableTetris` or directly with

     ./build/bin/tetris/main/release/executable/tetris.kexe

During build process compilation script creates interoperability bindings to SDL2, using SDL C headers,
and then compiles an application with the produced bindings.

To deploy executable to iPhone device take Info.plist, then use XCode and your own private signing identity.

To run on Raspberry Pi one need to install SDL package with `apt-get install libsdl2-2.0.0` on the Pi. 
Also GLES2 renderer is recommended (use `SDL_RENDER_DRIVER=opengles2 ./Tetris.kexe`).

For Windows `set SDL_RENDER_DRIVER=software` may be needed on some machines.

Note: There is a known issue with SDL2 library on Mac OS X 10.14 Mojave. Window may render black until
it is dragged. See https://bugzilla.libsdl.org/show_bug.cgi?id=4272
