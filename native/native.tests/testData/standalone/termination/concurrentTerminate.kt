// TARGET_BACKEND: NATIVE
// EXIT_CODE: 99
// OUTPUT_REGEX: .*Reporting error![\r\n]+

// MODULE: cinterop
// FILE: concurrentTerminate.def
package async
---
int test_ConcurrentTerminate();

// FILE: async.h
#ifdef __cplusplus
extern "C" {
#endif

int test_ConcurrentTerminate();

#ifdef __cplusplus
}
#endif

// FILE: async.cpp
#include <thread>
#include <future>
#include <chrono>
#include <vector>
#include <csignal>  // signal.h

#include "async.h"

int test_ConcurrentTerminate() {
    signal(SIGABRT, *[](int){ exit(99); }); // Windows does not have sigaction

    std::vector<std::future<void>> futures;
#ifdef __linux__
    // TODO: invalid terminate handler called from bridge on non-main thread on Linux X64
    throw std::runtime_error("Reporting error!");
#endif

    for (size_t i = 0; i < 100; ++i) {
        futures.emplace_back(std::async(std::launch::async,
                [](size_t param) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(param));
                    throw std::runtime_error("Reporting error!");
                },
                200 - i));
    }

    for (auto &future : futures) future.get();
    return 0;
}


// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import async.*
import kotlinx.cinterop.*

fun main() {
    test_ConcurrentTerminate()
    println("This is not expected.")
}