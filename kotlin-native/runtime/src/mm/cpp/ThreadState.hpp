/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_STATE_H
#define RUNTIME_MM_THREAD_STATE_H

#include "Common.h"
#include "Memory.h"

namespace kotlin {

namespace internal {

ALWAYS_INLINE inline bool isStateSwitchAllowed(ThreadState oldState, ThreadState newState, bool reentrant) noexcept  {
    return oldState != newState || reentrant;
}

std::string statesToString(std::initializer_list<ThreadState> states) noexcept;

} // namespace internal

const char* ThreadStateName(ThreadState state) noexcept;

} // namespace kotlin

#endif // RUNTIME_MM_THREAD_STATE_H
