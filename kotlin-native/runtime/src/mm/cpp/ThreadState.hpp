/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_STATE_H
#define RUNTIME_MM_THREAD_STATE_H

#include <Common.h>
#include <Utils.hpp>

namespace kotlin {

// Switches the state of the given thread to `newState` and returns the previous thread state.
ALWAYS_INLINE ThreadState SwitchThreadState(mm::ThreadData* threadData, ThreadState newState) noexcept;

ALWAYS_INLINE void AssertThreadState(mm::ThreadData* threadData, ThreadState expected) noexcept;

} // namespace kotlin

#endif // RUNTIME_MM_THREAD_STATE_H