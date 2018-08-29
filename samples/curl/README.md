# HTTP client

This example shows how to communicate with libcurl, HTTP/HTTPS/FTP/etc client library and how to
depend on an artifact published in a maven repository. The sample depends on a library
built by [libcurl sample](../libcurl) so you need to run it first.
 
To build use `../gradlew assemble`.

Now you can run the client directly 

    ./build/exe/main/release/<platform>/curl.kexe https://www.jetbrains.com

It will perform HTTP get and print out the data obtained.
