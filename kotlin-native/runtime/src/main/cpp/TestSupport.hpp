/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <functional>
#include <ostream>

#include "Memory.h"
#include "ObjectTestSupport.hpp"
#include "ThreadData.hpp"
#include "concurrent/ScopedThread.hpp"

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

inline mm::ThreadData& InitMemoryForTests() { return InitMemory(); }
void DeinitMemoryForTests(mm::ThreadData& threadData);

// Scopely initializes the memory subsystem of the current thread for tests.
// TODO(KT-72132): consider dropping this class.
class ScopedMemoryInit : private kotlin::Pinned {
public:
    ScopedMemoryInit() : threadData_(&InitMemoryForTests()) {
        kotlin::SwitchThreadState(threadData(), ThreadState::kRunnable);
    }
    ~ScopedMemoryInit() {
        // ClearForTests must not be done concurrently with GC
        SwitchThreadState(threadData(), ThreadState::kRunnable, /* reentrant = */ true);
        ClearMemoryForTests(threadData());
        // Ensure that memory deinit is performed in the native state.
        SwitchThreadState(threadData(), ThreadState::kNative);
        DeinitMemoryForTests(threadData());
    }

    mm::ThreadData& threadData() { return *static_cast<mm::ThreadData*>(threadData_); }

private:
    raw_ptr<mm::ThreadData> threadData_;
};

// Runs the given function in a separate thread with minimally initialized runtime.
inline void RunInNewThread(std::function<void(mm::ThreadData&)> f) {
    ScopedThread([&f]() {
        ScopedMemoryInit init;
        f(init.threadData());
    });
}

// Runs the given function in a separate thread with minimally initialized runtime.
inline void RunInNewThread(std::function<void()> f) {
    RunInNewThread([&f](mm::ThreadData&) {
        f();
    });
}

// Overload the << operator for ThreadState to allow the GTest runner
// to pretty print ThreadState constants.
std::ostream& operator<<(std::ostream& stream, ThreadState state);

namespace test_support {

RegularWeakReferenceImpl& InstallWeakReference(mm::ThreadData& threadData, ObjHeader* objHeader, ObjHeader** location);

} // namespace test_support


} // namespace kotlin
