/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemoryPrivate.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

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

ALWAYS_INLINE ThreadState kotlin::SwitchThreadState(MemoryState* thread, ThreadState newState, bool reentrant) noexcept {
    RuntimeAssert(thread != nullptr, "thread must not be nullptr");
    return SwitchThreadState(thread->GetThreadData(), newState, reentrant);
}

ALWAYS_INLINE void kotlin::AssertThreadState(MemoryState* thread, ThreadState expected) noexcept {
    // Avoid redundant read in GetThreadData if runtime asserts are disabled.
    if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
        RuntimeAssert(thread != nullptr, "thread must not be nullptr");
        AssertThreadState(thread->GetThreadData(), expected);
    }
}

ALWAYS_INLINE void kotlin::AssertThreadState(MemoryState* thread, std::initializer_list<ThreadState> expected) noexcept {
    // Avoid redundant read in GetThreadData if runtime asserts are disabled.
    if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
        RuntimeAssert(thread != nullptr, "thread must not be nullptr");
        AssertThreadState(thread->GetThreadData(), expected);
    }
}

ThreadState kotlin::GetThreadState(MemoryState* thread) noexcept {
    return thread->GetThreadData()->state();
}
