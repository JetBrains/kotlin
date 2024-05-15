Currently working on getting llvm's clang++ working with the toolchain provided by zephyr.

We can run following command from `/Users/txie/kn_exp/kotlin-native/runtime/src`
```
/Users/txie/.konan/dependencies/apple-llvm-20200714-macos-aarch64-1/bin/clang++ \
    -I/Users/txie/kn_exp/kotlin-native/runtime/src/alloc/common/cpp \
    -I/Users/txie/kn_exp/kotlin-native/runtime/src/gcScheduler/common/cpp \
    -I/Users/txie/kn_exp/kotlin-native/runtime/src/gc/common/cpp \
    -I/Users/txie/kn_exp/kotlin-native/runtime/src/mm/cpp \
    -I/Users/txie/kn_exp/kotlin-native/runtime/src/externalCallsChecker/common/cpp \
    -I/Users/txie/kn_exp/kotlin-native/runtime/src/objcExport/cpp \
    -I/Users/txie/kn_exp/kotlin-native/runtime/src/main/cpp \
    -I"$ZEPHYR_SDK_ROOT"/arm-zephyr-eabi/include/c++/12.2.0 \
    -I"$ZEPHYR_SDK_ROOT"/arm-zephyr-eabi/include/c++/12.2.0/arm-zephyr-eabi/thumb/v8-m.main+fp/hard \
    -I"$ZEPHYR_SDK_ROOT"/arm-zephyr-eabi/include \
    -std=c++17 \
    -fno-stack-protector \
    -c \
    -emit-llvm \
    -o /Users/txie/kn_exp/kotlin-native/runtime/build/bitcode/main/zephyr_m55/common_alloc/Allocator.bc \
    -target thumb \
    -mfloat-abi=hard \
    alloc/common/cpp/Allocator.cpp

```

To enable features required by KN runtime, we need to add following lines to `gthr.h`

```
#define _UNIX98_THREAD_MUTEX_ATTRIBUTES 1
#define PTHREAD_MUTEX_RECURSIVE 1
#define _POSIX_TIMEOUTS 1
#define _POSIX_THREADS 1
#include "gthr-posix.h"
```

We also need to enable `gThread`

