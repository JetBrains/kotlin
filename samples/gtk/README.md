# GTK application

 This example shows how one may use _Kotlin/Native_ to build GUI
 applications with the GTK toolkit.

To build use `../gradlew build` or `./build.sh [-I=/include/path]`.

Do not forget to install GTK3. See bellow.

On Mac use `port install gtk3`, on Debian flavours of Linux - `apt-get install libgtk-3-dev`.
To run on Mac also install XQuartz X server (https://www.xquartz.org/), and then

    ../gradlew run

Alternatively you can run artifact directly

    ./build/konan/bin/<platform>/Gtk3Demo.kexe

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
