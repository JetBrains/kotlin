/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_STATE_H
#define RUNTIME_MM_THREAD_STATE_H

#include <Common.h>
#include <Utils.hpp>

namespace kotlin {
namespace mm {

enum class ThreadState {
    kRunnable, kNative
};

// Switches the state of the current thread to `newState` and returns the previous thread state.
ALWAYS_INLINE ThreadState SwitchThreadState(ThreadData* threadData, ThreadState newState) noexcept;

ALWAYS_INLINE void AssertThreadState(ThreadData* threadData, ThreadState expected) noexcept;

class ThreadStateGuard final : private Pinned {
public:
    explicit ThreadStateGuard(ThreadData* threadData, ThreadState state) noexcept;
    ~ThreadStateGuard() noexcept;
private:
    ThreadData* threadData_;
    ThreadState oldState_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_STATE_H