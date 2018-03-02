# HTTP client

 This example shows how to communicate with libcurl, HTTP/HTTPS/FTP/etc client library.
 Debian-like distros may need to `apt-get install libcurl4-openssl-dev`.

To build use `../gradlew build` or `./build.sh`.

To run use `../gradlew run`

To change run arguments, change property runArgs in gradle.propeties file 
or pass `-PrunArgs="https://www.jetbrains.com"` to gradle run. 

Alternatively you can run artifact directly 

    ./build/konan/bin/<platform>/Curl.kexe https://www.jetbrains.com

It will perform HTTP get and print out the data obtained.
