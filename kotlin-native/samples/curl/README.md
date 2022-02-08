# HTTP client

This example shows how to communicate with libcurl, HTTP/HTTPS/FTP/etc client library and how to
depend on an artifact published in a maven repository. The sample depends on a library
built by [libcurl sample](../libcurl) so you need to run it first.

To build use `../gradlew assemble`.

To run use `../gradlew runReleaseExecutableCurl` or execute the program directly:

    ./build/bin/curl/main/release/executable/curl.kexe 'https://www.jetbrains.com/'

It will perform HTTP get and print out the data obtained.
