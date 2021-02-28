/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemoryPrivate.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

namespace {

ALWAYS_INLINE bool isStateSwitchAllowed(ThreadState oldState, ThreadState newState) noexcept {
    return oldState != newState;
}

const char* stateToString(ThreadState state) noexcept {
    switch (state) {
        case ThreadState::kRunnable:
            return "RUNNABLE";
        case ThreadState::kNative:
            return "NATIVE";
    }
}

} // namespace

// Switches the state of the current thread to `newState` and returns the previous state.
ALWAYS_INLINE ThreadState kotlin::SwitchThreadState(mm::ThreadData* threadData, ThreadState newState) noexcept {
    auto oldState = threadData->setState(newState);
    // TODO(perf): Mesaure the impact of this assert in debug and opt modes.
    RuntimeAssert(isStateSwitchAllowed(oldState, newState),
                  "Illegal thread state switch. Old state: %s. New state: %s.",
                  stateToString(oldState), stateToString(newState));
    return oldState;
}

ALWAYS_INLINE ThreadState kotlin::SwitchThreadState(MemoryState* thread, ThreadState newState) noexcept {
    return SwitchThreadState(thread->GetThreadData(), newState);
}

ALWAYS_INLINE void kotlin::AssertThreadState(mm::ThreadData* threadData, ThreadState expected) noexcept {
    auto actual = threadData->state();
    RuntimeAssert(actual == expected,
                  "Unexpected thread state. Expected: %s. Actual: %s.",
                  stateToString(expected), stateToString(actual));
}

ALWAYS_INLINE void kotlin::AssertThreadState(MemoryState* thread, ThreadState expected) noexcept {
    AssertThreadState(thread->GetThreadData(), expected);
}
