# GTK application

 This example shows how one may use _Kotlin/Native_ to build GUI
 applications with the GTK toolkit.

To build use `./build.sh` script without arguments (or specify `TARGET` variable if cross-compiling).
You also may use gradle to build the sample: `../gradlew build`.

Do not forget to install GTK3.

On Mac use `port install gtk3`, on Debian flavours of Linux - `apt-get install libgtk-3-dev`.
To run on Mac also install XQuartz X server (https://www.xquartz.org/), and then

    ./Gtk3Demo.kexe

Dialog box with the button will be shown, and application will print message
and terminate on button click.
