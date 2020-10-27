# GTK application

This example shows how one may use _Kotlin/Native_ to build GUI
applications with the GTK toolkit.

To build use `../gradlew assemble`.

Do not forget to install GTK3. See bellow.

To run on Mac also install XQuartz X server (https://www.xquartz.org/), and then `../gradlew runReleaseExecutableGtk` or execute the program directly:

    ./build/bin/gtk/main/release/executable/gtk.kexe

Dialog box with the button will be shown, and application will print message
and terminate on button click.


#### GTK3 Install

on Mac use

    brew install gtk+3

or

    port install gtk3

on Debian flavours of Linux

    sudo apt-get install libgtk-3-dev

on Fedora

    sudo dnf install gtk3-devel

on Windows in MinGW64 console, if you do
not have MSYS2-MinGW64 installed - install it first as described in http://www.msys2.org

    pacman -S mingw-w64-x86_64-gtk3
