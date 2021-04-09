/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_STATE_H
#define RUNTIME_MM_THREAD_STATE_H

#include <Common.h>
#include <Utils.hpp>

#include "ThreadData.hpp"

namespace kotlin {

namespace internal {

ALWAYS_INLINE inline bool isStateSwitchAllowed(ThreadState oldState, ThreadState newState, bool reentrant) noexcept  {
    return oldState != newState || reentrant;
}

const char* stateToString(ThreadState state) noexcept;

} // namespace internal

// Switches the state of the given thread to `newState` and returns the previous thread state.
ALWAYS_INLINE inline ThreadState SwitchThreadState(mm::ThreadData* threadData, ThreadState newState, bool reentrant = false) noexcept {
    auto oldState = threadData->setState(newState);
    // TODO(perf): Mesaure the impact of this assert in debug and opt modes.
    RuntimeAssert(internal::isStateSwitchAllowed(oldState, newState, reentrant),
                  "Illegal thread state switch. Old state: %s. New state: %s.",
                  internal::stateToString(oldState), internal::stateToString(newState));
    return oldState;
}

// Asserts that the given thread is in the given state.
ALWAYS_INLINE inline void AssertThreadState(mm::ThreadData* threadData, ThreadState expected) noexcept {
    auto actual = threadData->state();
    RuntimeAssert(actual == expected,
                  "Unexpected thread state. Expected: %s. Actual: %s.",
                  internal::stateToString(expected), internal::stateToString(actual));
}

} // namespace kotlin

#endif // RUNTIME_MM_THREAD_STATE_H