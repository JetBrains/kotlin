#include "leakMemory.h"

#include <atomic>
#include <thread>

extern "C" void test_RunInNewThread(void (*f)()) {
    std::atomic<bool> haveRun(false);
    std::thread t([f, &haveRun]() {
        f();
        haveRun = true;
        while (true) {}
    });
    t.detach();
    while (!haveRun) {}
}
