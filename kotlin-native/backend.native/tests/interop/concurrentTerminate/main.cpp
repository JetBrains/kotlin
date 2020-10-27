#include "testlib_api.h"

#include <iostream>
#include <thread>
#include <future>
#include <chrono>
#include <vector>
#include <csignal>  // signal.h

using namespace std;

static
int runConcurrent() {

    std::vector<std::future<void>> futures;

    for (size_t i = 0; i < 100; ++i) {
        futures.emplace_back(std::async(std::launch::async,
                [](auto delay) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(delay));
                    testlib_symbols()->kotlin.root.testTerminate();
                },
                100));
    }

    for (auto &future : futures) future.get();
    return 0;
}

int main() {
    signal(SIGABRT, *[](int){ exit(99); }); // Windows does not have sigaction

    set_terminate([](){
        cout << "This is the original terminate handler\n" << flush;
        std::abort();
    });

    try {
        runConcurrent();
    } catch(...) {
        std::cerr << "Unknown exception caught\n" << std::flush;
    }
    return 0;
}