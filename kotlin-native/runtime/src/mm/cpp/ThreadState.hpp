/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_STATE_H
#define RUNTIME_MM_THREAD_STATE_H

#include <Common.h>
#include <Utils.hpp>

#include "ThreadData.hpp"
#include "ThreadSuspension.hpp"

namespace kotlin {

namespace internal {

ALWAYS_INLINE inline bool isStateSwitchAllowed(ThreadState oldState, ThreadState newState, bool reentrant) noexcept  {
    return oldState != newState || reentrant;
}

std::string statesToString(std::initializer_list<ThreadState> states) noexcept;

} // namespace internal

const char* ThreadStateName(ThreadState state) noexcept;

// Switches the state of the given thread to `newState` and returns the previous thread state.
ALWAYS_INLINE inline ThreadState SwitchThreadState(mm::ThreadData* threadData, ThreadState newState, bool reentrant = false) noexcept {
    RuntimeAssert(threadData != nullptr, "threadData must not be nullptr");
    auto oldState = threadData->setState(newState);
    // TODO(perf): Mesaure the impact of this assert in debug and opt modes.
    RuntimeAssert(internal::isStateSwitchAllowed(oldState, newState, reentrant),
                  "Illegal thread state switch. Old state: %s. New state: %s.",
                  ThreadStateName(oldState), ThreadStateName(newState));
    return oldState;
}

// Asserts that the given thread is in the given state.
ALWAYS_INLINE inline void AssertThreadState(mm::ThreadData* threadData, ThreadState expected) noexcept {
    // The read of the thread state is atomic, thus the compiler cannot eliminate it
    // even if its result is unused due to disabled runtime asserts.
    // So we explicitly avoid the read if asserts are disabled.
    if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
        RuntimeAssert(threadData != nullptr, "threadData must not be nullptr");
        auto actual = threadData->state();
        RuntimeAssert(
                actual == expected, "Unexpected thread state. Expected: %s. Actual: %s.", ThreadStateName(expected),
                ThreadStateName(actual));
    }
}

ALWAYS_INLINE inline void AssertThreadState(mm::ThreadData* threadData, std::initializer_list<ThreadState> expected) noexcept {
    // The read of the thread state is atomic, thus the compiler cannot eliminate it
    // even if its result is unused due to disabled runtime asserts.
    // So we explicitly avoid the read if asserts are disabled.
    if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
        RuntimeAssert(threadData != nullptr, "threadData must not be nullptr");
        auto actual = threadData->state();
        RuntimeAssert(
                std::any_of(expected.begin(), expected.end(), [actual](ThreadState expected) { return expected == actual; }),
                "Unexpected thread state. Expected one of: %s. Actual: %s", internal::statesToString(expected).c_str(),
                ThreadStateName(actual));
    }
}

} // namespace kotlin

#endif // RUNTIME_MM_THREAD_STATE_H
