/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadData.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

namespace {

ALWAYS_INLINE bool isStateSwitchAllowed(mm::ThreadState oldState, mm::ThreadState newState) noexcept {
    return oldState != newState;
}

const char* stateToString(mm::ThreadState state) noexcept {
    switch (state) {
        case mm::ThreadState::kRunnable:
            return "RUNNABLE";
        case mm::ThreadState::kNative:
            return "NATIVE";
    }
}

std::string unexpectedStateMessage(mm::ThreadState expected, mm::ThreadState actual) noexcept {
    return std::string("Unexpected thread state. Expected: ") + stateToString(expected)
        + ". Actual: " + stateToString(actual);
}

std::string illegalStateSwitchMessage(mm::ThreadState oldState, mm::ThreadState newState) noexcept {
    return std::string("Illegal thread state switch. Old state: ") + stateToString(oldState)
        + ". New state: " + stateToString(newState);
}

} // namespace

// Switches the state of the current thread to `newState` and returns the previous state.
ALWAYS_INLINE mm::ThreadState mm::SwitchThreadState(ThreadData* threadData, ThreadState newState) noexcept {
    auto oldState = threadData->setState(newState);
    // TODO(perf): Mesaure the impact of this assert in debug and opt modes.
    RuntimeAssert(isStateSwitchAllowed(oldState, newState),
                  illegalStateSwitchMessage(oldState, newState).c_str());
    return oldState;
}

ALWAYS_INLINE void mm::AssertThreadState(ThreadData* threadData, ThreadState expected) noexcept {
    auto actual = threadData->state();
    RuntimeAssert(actual == expected, unexpectedStateMessage(expected, actual).c_str());
}

mm::ThreadStateGuard::ThreadStateGuard(ThreadData* threadData, ThreadState state) noexcept : threadData_(threadData) {
    oldState_ = SwitchThreadState(threadData, state);
}

mm::ThreadStateGuard::~ThreadStateGuard() noexcept {
    SwitchThreadState(threadData_, oldState_);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateNative() {
    mm::SwitchThreadState(mm::ThreadRegistry::Instance().CurrentThreadData(), mm::ThreadState::kNative);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateRunnable() {
    mm::SwitchThreadState(mm::ThreadRegistry::Instance().CurrentThreadData(), mm::ThreadState::kRunnable);
}

