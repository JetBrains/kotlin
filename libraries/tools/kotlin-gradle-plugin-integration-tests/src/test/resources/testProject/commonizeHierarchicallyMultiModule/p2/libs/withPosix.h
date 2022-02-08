#include <sys/stat.h>
#include <stdint.h>
#include <stdbool.h>

/*
Special function that will differentiate this header file from p1/../withPosix.h and p3/../withPosix.h
*/
void p2();

struct stat getStructFromPosix();

struct stat* getStructPointerFromPosix();

struct MyStruct getMyStruct();

struct MyStruct* getMyStructPointer();

struct MyStruct {
    struct stat posixProperty;

    #if _WIN32
        long long longProperty;
    #else
        long longProperty;
    #endif

    double doubleProperty;

    int32_t int32tProperty;

    int64_t int64tProperty;

    #if __linux__
        bool linuxOnlyProperty;
    #endif

    #if __APPLE__
        bool appleOnlyProperty;
        #include "TargetConditionals.h"
            #if TARGET_OS_IPHONE
                bool iosOnlyProperty;
            #endif
    #endif

    #if _WIN32
        bool windowsOnlyProperty;
    #endif
};
