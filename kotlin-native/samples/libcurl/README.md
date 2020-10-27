# Curl interop library

This example shows how to build and publish an interop library to communicate with the libcurl,
HTTP/HTTPS/FTP/etc client library.

Install libcurl development files. For Mac - `brew install curl`. For Debian-like Linux - use `apt-get install libcurl4-openssl-dev` or `apt-get install libcurl4-gnutls-dev`.
For Windows - `pacman -S mingw-w64-x86_64-curl` in MinGW64 console, if you do
not have MSYS2-MinGW64 installed - install it first as described in http://www.msys2.org

To build use `../gradlew assemble`.

To publish the library into a local repo use `../gradlew publish`.

