Build native:

    g++ -std=c++14 -o src/native/features.o -c src/native/features.cpp

c++ interop:

    ../../dist/bin/cinterop -def features.def  -compiler-options "-I." -o features

