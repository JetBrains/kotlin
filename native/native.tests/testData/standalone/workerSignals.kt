// Cross-thread signalling does not work on Windows
// DISABLE_NATIVE: targetFamily=MINGW
// MODULE: cinterop
// FILE: workerSignals.def
---
#include <stdint.h>
void setupSignalHandler(void);
void signalThread(uint64_t thread, int value);
int getValue(void);

// FILE: workerSignals.cpp
#include <cassert>
#include <cstdint>
#include <cstring>
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
    pthread_t t = {};
    memcpy(&t, &thread, sizeof(pthread_t));
    auto result = pthread_kill(t, SIGUSR1);
    assert(result == 0);
}

extern "C" int getValue(void) {
    return value;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class, ObsoleteWorkersApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.concurrent.*
import kotlin.test.*
import workerSignals.*

const val defaultValue = 0
const val newValue = 42

fun main() {
    setupSignalHandler()

    withWorker {
        val before = execute(TransferMode.SAFE, {}) {
            getValue()
        }.result
        assertEquals(defaultValue, getValue())
        assertEquals(defaultValue, before)

        signalThread(platformThreadId, newValue)
        val after = execute(TransferMode.SAFE, {}) {
            getValue()
        }.result
        assertEquals(defaultValue, getValue())
        assertEquals(newValue, after)
    }
}
