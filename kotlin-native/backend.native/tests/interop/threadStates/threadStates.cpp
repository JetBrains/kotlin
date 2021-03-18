#include <thread>
#include <stdint.h>

// Implemented in the runtime for test purposes.
extern "C" bool Kotlin_Debugging_isThreadStateNative();

extern "C" void assertNativeThreadState() {
    if (!Kotlin_Debugging_isThreadStateNative()) {
        printf("Incorrect thread state. Expected native thread state.");
        exit(1);
    }
}

extern "C" void runInNewThread(int32_t(*callback)()) {
    std::thread t([callback]() {
        callback();
    });
    t.join();
}