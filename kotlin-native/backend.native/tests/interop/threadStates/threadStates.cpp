#include <future>
#include <thread>
#include <stdint.h>
#include <stdlib.h>

// Implemented in the runtime for test purposes.
extern "C" bool Kotlin_Debugging_isThreadStateNative();

extern "C" void assertNativeThreadState() {
    if (!Kotlin_Debugging_isThreadStateNative()) {
        printf("Incorrect thread state. Expected native thread state.");
        abort();
    }
}

extern "C" void runInNewThread(void(*callback)(void)) {
    std::thread t([callback]() {
        callback();
    });
    t.join();
}

extern "C" void runInForeignThread(void(*callback)(void)) {
    std::thread t([callback]() {
        // This thread is not attached to the Kotlin runtime.
        auto future = std::async(std::launch::async, callback);

        // The machinery of the direct interop doesn't filter out a Kotlin exception thrown by the callback.
        // The get() call will re-throw this exception.
        future.get();
    });
    t.join();
}