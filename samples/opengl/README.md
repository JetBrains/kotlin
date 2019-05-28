# OpenGL application

This example shows interaction with OpenGL library, to render classical 3D test model. Linux build requires `apt-get install freeglut3-dev` or similar,
MacOS shall work as is.

To build use `../gradlew assemble`.

To run use `../gradlew runReleaseExecutableOpengl` or execute the program directly:

    ./build/bin/opengl/main/release/executable/opengl.kexe

It will render 3D model of teapot. Feel free to experiment with it, the whole power of OpenGL
is at your hands.
