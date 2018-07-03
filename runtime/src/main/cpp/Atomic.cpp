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
#include "Exceptions.h"
#include "Memory.h"
#include "Types.h"

namespace {

struct AtomicReferenceLayout {
  KRef value_;
  KInt lock_;
};

template <typename T> T addAndGetImpl(KRef thiz, T delta) {
  volatile T* location = reinterpret_cast<volatile T*>(thiz + 1);
  return atomicAdd(location, delta);
}

template <typename T> T compareAndSwapImpl(KRef thiz, T expectedValue, T newValue) {
    volatile T* location = reinterpret_cast<volatile T*>(thiz + 1);
    return compareAndSwap(location, expectedValue, newValue);
}

inline AtomicReferenceLayout* asAtomicReference(KRef thiz) {
    return reinterpret_cast<AtomicReferenceLayout*>(thiz + 1);
}

}  // namespace

extern "C" {

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
#ifdef __mips
    // Potentially huge performance penalty, but correct.
    // TODO: reconsider, once target MIPS can do proper 64-bit CAS.
    static int lock = 0;
    while (compareAndSwap(&lock, 0, 1) != 0);
    KLong* address = reinterpret_cast<KLong*>(thiz + 1);
    KLong old = *address;
    if (old == expectedValue) {
      *address = newValue;
    }
    compareAndSwap(&lock, 1, 0);
    return old;
#else
    return compareAndSwapImpl(thiz, expectedValue, newValue);
#endif
}

KNativePtr Kotlin_AtomicNativePtr_compareAndSwap(KRef thiz, KNativePtr expectedValue, KNativePtr newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

void Kotlin_AtomicReference_checkIfFrozen(KRef value) {
    if (value != nullptr && !value->container()->permanentOrFrozen()) {
        ThrowInvalidMutabilityException(value);
    }
}

OBJ_GETTER(Kotlin_AtomicReference_compareAndSwap, KRef thiz, KRef expectedValue, KRef newValue) {
    Kotlin_AtomicReference_checkIfFrozen(newValue);
    // See Kotlin_AtomicReference_get() for explanations, why locking is needed.
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    RETURN_RESULT_OF(SwapRefLocked, &ref->value_, expectedValue, newValue, &ref->lock_);
}

OBJ_GETTER(Kotlin_AtomicReference_get, KRef thiz) {
    // Here we must take a lock to prevent race when value, while taken here, is CASed and immediately
    // destroyed by an another thread. AtomicReference no longer holds such an object, so if we got
    // rescheduled unluckily, between the moment value is read from the field and RC is incremented,
    // object may go away.
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    RETURN_RESULT_OF(ReadRefLocked, &ref->value_, &ref->lock_);
}

}  // extern "C"
