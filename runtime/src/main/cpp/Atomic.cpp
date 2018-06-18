/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "Atomic.h"
#include "Common.h"
#include "Types.h"

namespace {

template <typename T> T addAndGetImpl(KRef thiz, T delta) {
  volatile T* location = reinterpret_cast<volatile T*>(thiz + 1);
  return atomicAdd(location, delta);
}

template <typename T> T compareAndSwapImpl(KRef thiz, T expectedValue, T newValue) {
    volatile T* location = reinterpret_cast<volatile T*>(thiz + 1);
    return compareAndSwap(location, expectedValue, newValue);
}

}  // namespace

extern "C" {

RUNTIME_NORETURN void ThrowInvalidMutabilityException();

KInt Kotlin_AtomicInt_addAndGet(KRef thiz, KInt delta) {
    return addAndGetImpl(thiz, delta);
}

KInt Kotlin_AtomicInt_compareAndSwap(KRef thiz, KInt expectedValue, KInt newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

KLong Kotlin_AtomicLong_addAndGet(KRef thiz, KLong delta) {
    return addAndGetImpl(thiz, delta);
}

KLong Kotlin_AtomicLong_compareAndSwap(KRef thiz, KLong expectedValue, KLong newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

KNativePtr Kotlin_AtomicNativePtr_compareAndSwap(KRef thiz, KNativePtr expectedValue, KNativePtr newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

void Kotlin_AtomicReference_checkIfFrozen(KRef value) {
    if (value != nullptr && !value->container()->permanentOrFrozen()) {
        ThrowInvalidMutabilityException();
    }
}

KRef Kotlin_AtomicReference_compareAndSwap(KRef thiz, KRef expectedValue, KRef newValue) {
    Kotlin_AtomicReference_checkIfFrozen(newValue);
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

}  // extern "C"