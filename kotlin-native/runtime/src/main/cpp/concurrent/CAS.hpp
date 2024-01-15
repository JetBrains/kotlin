/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "std_support/AtomicRef.hpp"

template<typename T, typename Atomic>
ALWAYS_INLINE T compareAndSwap(Atomic&& atomic, T expectedValue, T newValue) {
    atomic.compare_exchange_strong(expectedValue, newValue);
    return expectedValue;
}

template<typename T, typename Atomic>
ALWAYS_INLINE bool compareAndSet(Atomic&& atomic, T expectedValue, T newValue) {
    return atomic.compare_exchange_strong(expectedValue, newValue);
}
