#include "workerSignals.h"

#include <cassert>
#include <pthread.h>
#include <signal.h>

namespace {

int pendingValue = 0;
thread_local int value = 0;

void signalHandler(int signal) {
    value = pendingValue;
}

} // namespace

extern "C" void setupSignalHandler(void) {
    signal(SIGUSR1, &signalHandler);
}

extern "C" void signalThread(uint64_t thread, int value) {
    pendingValue = value;
    auto t = reinterpret_cast<pthread_t>(thread);
    auto result = pthread_kill(t, SIGUSR1);
    assert(result == 0);
}

extern "C" int getValue(void) {
    return value;
}
