# HTTP client

This example shows how to communicate with libcurl, HTTP/HTTPS/FTP/etc client library and how to
depend on an artifact published in a maven repository. The sample depends on a library
built by [libcurl sample](../libcurl) so you need to run it first.
 
To build use `../gradlew build`.

To run use `../gradlew run`.

To change run arguments, change property runArgs in gradle.propeties file 
or pass `-PrunArgs="https://www.jetbrains.com"` to gradle run. 

Alternatively you can run artifact directly 

    ./build/konan/bin/<platform>/Curl.kexe https://www.jetbrains.com

It will perform HTTP get and print out the data obtained.
