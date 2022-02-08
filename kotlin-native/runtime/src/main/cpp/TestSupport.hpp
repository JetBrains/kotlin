/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <functional>

#include "Memory.h"
#include "Runtime.h"
#include "ScopedThread.hpp"

namespace kotlin {

#if KONAN_WINDOWS
// TODO: Figure out why creating many threads on windows is so slow.
constexpr int kDefaultThreadCount = 10;
#elif __has_feature(thread_sanitizer)
// TSAN has a huge overhead.
constexpr int kDefaultThreadCount = 10;
#else
constexpr int kDefaultThreadCount = 100;
#endif

inline MemoryState* InitMemoryForTests() { return InitMemory(false); }
void DeinitMemoryForTests(MemoryState* memoryState);

// Scopely initializes the memory subsystem of the current thread for tests.
class ScopedMemoryInit : private kotlin::Pinned {
public:
    ScopedMemoryInit() : memoryState_(InitMemoryForTests()) {
        kotlin::SwitchThreadState(memoryState(), ThreadState::kRunnable);
    }
    ~ScopedMemoryInit() {
        // ClearForTests must not be done concurrently with GC
        SwitchThreadState(memoryState(), ThreadState::kRunnable, /* reentrant = */ true);
        ClearMemoryForTests(memoryState());
        // Ensure that memory deinit is performed in the native state.
        SwitchThreadState(memoryState(), ThreadState::kNative);
        DeinitMemoryForTests(memoryState());
    }

    MemoryState* memoryState() { return memoryState_; }
private:
    MemoryState* memoryState_;
};

// Runs the given function in a separate thread with minimally initialized runtime.
inline void RunInNewThread(std::function<void(MemoryState*)> f) {
    ScopedThread([&f]() {
        ScopedMemoryInit init;
        f(init.memoryState());
    });
}

// Runs the given function in a separate thread with minimally initialized runtime.
inline void RunInNewThread(std::function<void()> f) {
    RunInNewThread([&f](MemoryState* unused) {
        f();
    });
}

} // namespace kotlin
