Start with building compiler by using `dist` and `cross_dist` for cross-targets.
To build Tetris application for your host platform use
./build
note that SDL2 must be installed on the host.
For cross-compilation to iOS use
TARGET=iphone ./build

During build process compilation script creates interoperability bindings to SDL2, using SDL C headers,
and then compiles an application with the produced bindings.

To deploy executable to iPhone device take Info.plist, then use XCode and your own private signing identity.
