#include <sys/stat.h>
#include <stdint.h>
#include <stdbool.h>

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
