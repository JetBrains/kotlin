# OpenGL application

 This example shows interaction with OpenGL library, to render classical 3D test model. Linux build requires `apt-get install freeglut3-dev` or similar,
MacOS shall work as is.

To build use `./build.sh` script without arguments (or specify `TARGET` variable if cross-compiling).
You also may use Gradle to build this sample: `../gradlew build`.

To run use

    ./OpenGlTeapot.kexe

It will render 3D model of teapot. Feel free to experiment with it, the whole power of OpenGL
is at your hands.
