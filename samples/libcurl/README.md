# HTTP client

 This example shows how to communicate with libcurl, HTTP/HTTPS/FTP/etc client library.
 Debian-like distros may need to `apt-get install libcurl4-openssl-dev`.
To build use `./build.sh` script without arguments (or specify `TARGET` variable if cross-compiling).
You also may use Gradle to build this sample: `../gradlew build`.

To run use

    ./Curl.kexe https://www.jetbrains.com

It will perform HTTP get and print out the data obtained.
