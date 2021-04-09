/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemoryPrivate.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

const char* kotlin::internal::stateToString(ThreadState state) noexcept {
    switch (state) {
        case ThreadState::kRunnable:
            return "RUNNABLE";
        case ThreadState::kNative:
            return "NATIVE";
    }
}

ALWAYS_INLINE ThreadState kotlin::SwitchThreadState(MemoryState* thread, ThreadState newState, bool reentrant) noexcept {
    return SwitchThreadState(thread->GetThreadData(), newState, reentrant);
}

ALWAYS_INLINE void kotlin::AssertThreadState(MemoryState* thread, ThreadState expected) noexcept {
    AssertThreadState(thread->GetThreadData(), expected);
}

ThreadState kotlin::GetThreadState(MemoryState* thread) noexcept {
    return thread->GetThreadData()->state();
}