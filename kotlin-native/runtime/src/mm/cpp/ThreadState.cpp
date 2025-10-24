/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadData.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

const char* kotlin::ThreadStateName(ThreadState state) noexcept {
    switch (state) {
        case ThreadState::kRunnable:
            return "RUNNABLE";
        case ThreadState::kNative:
            return "NATIVE";
    }
}

std::string kotlin::internal::statesToString(std::initializer_list<ThreadState> states) noexcept {
    std::string result = "{ ";
    for (size_t i = 0; i < states.size(); i++) {
        if (i != 0) {
            result += ", ";
        }
        result += ThreadStateName(data(states)[i]);
    }
    result += " }";
    return result;
}

// Switches the state of the given thread to `newState` and returns the previous thread state.
PERFORMANCE_INLINE ThreadState kotlin::SwitchThreadState(mm::ThreadData& threadData, ThreadState newState, bool reentrant) noexcept {
    auto oldState = threadData.setState(newState);
    // TODO(perf): Mesaure the impact of this assert in debug and opt modes.
    RuntimeAssert(internal::isStateSwitchAllowed(oldState, newState, reentrant),
                  "Illegal thread state switch. Old state: %s. New state: %s.",
                  ThreadStateName(oldState), ThreadStateName(newState));
    return oldState;
}

// Asserts that the given thread is in the given state.
ALWAYS_INLINE void kotlin::AssertThreadState(mm::ThreadData& threadData, ThreadState expected) noexcept {
    // The read of the thread state is atomic, thus the compiler cannot eliminate it
    // even if its result is unused due to disabled runtime asserts.
    // So we explicitly avoid the read if asserts are disabled.
    if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
        auto actual = threadData.state();
        RuntimeAssert(
                actual == expected, "Unexpected thread state. Expected: %s. Actual: %s.", ThreadStateName(expected),
                ThreadStateName(actual));
    }
}

ALWAYS_INLINE void kotlin::AssertThreadState(mm::ThreadData& threadData, std::initializer_list<ThreadState> expected) noexcept {
    // The read of the thread state is atomic, thus the compiler cannot eliminate it
    // even if its result is unused due to disabled runtime asserts.
    // So we explicitly avoid the read if asserts are disabled.
    if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
        auto actual = threadData.state();
        RuntimeAssert(
                std::any_of(expected.begin(), expected.end(), [actual](ThreadState expected) { return expected == actual; }),
                "Unexpected thread state. Expected one of: %s. Actual: %s", internal::statesToString(expected).c_str(),
                ThreadStateName(actual));
    }
}

kotlin::ThreadState kotlin::GetThreadState(mm::ThreadData& threadData) noexcept {
    return threadData.state();
}
