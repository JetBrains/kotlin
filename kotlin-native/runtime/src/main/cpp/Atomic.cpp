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
  ObjHeader header;
  KRef value_;
  KInt lock_;
  KInt cookie_;
};

template<typename T> struct AtomicPrimitive {
  ObjHeader header;
  volatile T value_;
};

template <typename T> inline volatile T* getValueLocation(KRef thiz) {
  AtomicPrimitive<T>* atomic = reinterpret_cast<AtomicPrimitive<T>*>(thiz);
  return &atomic->value_;
}

template <typename T> void setImpl(KRef thiz, T value) {
  volatile T* location = getValueLocation<T>(thiz);
  atomicSet(location, value);
}

template <typename T> T getImpl(KRef thiz) {
  volatile T* location = getValueLocation<T>(thiz);
  return atomicGet(location);
}

template <typename T> T addAndGetImpl(KRef thiz, T delta) {
  volatile T* location = getValueLocation<T>(thiz);
  return atomicAdd(location, delta);
}

template <typename T> T compareAndSwapImpl(KRef thiz, T expectedValue, T newValue) {
  volatile T* location = getValueLocation<T>(thiz);
  return compareAndSwap(location, expectedValue, newValue);
}

template <typename T> KBoolean compareAndSetImpl(KRef thiz, T expectedValue, T newValue) {
    volatile T* location = getValueLocation<T>(thiz);
    return compareAndSet(location, expectedValue, newValue);
}

inline AtomicReferenceLayout* asAtomicReference(KRef thiz) {
    return reinterpret_cast<AtomicReferenceLayout*>(thiz);
}

}  // namespace

extern "C" {

KInt Kotlin_AtomicInt_addAndGet(KRef thiz, KInt delta) {
    return addAndGetImpl(thiz, delta);
}

KInt Kotlin_AtomicInt_compareAndSwap(KRef thiz, KInt expectedValue, KInt newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

KBoolean Kotlin_AtomicInt_compareAndSet(KRef thiz, KInt expectedValue, KInt newValue) {
    return compareAndSetImpl(thiz, expectedValue, newValue);
}

void Kotlin_AtomicInt_set(KRef thiz, KInt newValue) {
    setImpl(thiz, newValue);
}

KInt Kotlin_AtomicInt_get(KRef thiz) {
    return getImpl<KInt>(thiz);
}

KLong Kotlin_AtomicLong_addAndGet(KRef thiz, KLong delta) {
    return addAndGetImpl(thiz, delta);
}

#if KONAN_NO_64BIT_ATOMIC
static int lock64 = 0;
#endif

KLong Kotlin_AtomicLong_compareAndSwap(KRef thiz, KLong expectedValue, KLong newValue) {
#if KONAN_NO_64BIT_ATOMIC
    // Potentially huge performance penalty, but correct.
    while (compareAndSwap(&lock64, 0, 1) != 0);
    volatile KLong* address = getValueLocation<KLong>(thiz);
    KLong old = *address;
    if (old == expectedValue) {
      *address = newValue;
    }
    compareAndSwap(&lock64, 1, 0);
    return old;
#else
    return compareAndSwapImpl(thiz, expectedValue, newValue);
#endif
}

KBoolean Kotlin_AtomicLong_compareAndSet(KRef thiz, KLong expectedValue, KLong newValue) {
#if KONAN_NO_64BIT_ATOMIC
    // Potentially huge performance penalty, but correct.
    KBoolean result = false;
    while (compareAndSwap(&lock64, 0, 1) != 0);
    volatile KLong* address = getValueLocation<KLong>(thiz);
    KLong old = *address;
    if (old == expectedValue) {
      result = true;
      *address = newValue;
    }
    compareAndSwap(&lock64, 1, 0);
    return result;
#else
    return compareAndSetImpl(thiz, expectedValue, newValue);
#endif
}

void Kotlin_AtomicLong_set(KRef thiz, KLong newValue) {
#if KONAN_NO_64BIT_ATOMIC
    // Potentially huge performance penalty, but correct.
    while (compareAndSwap(&lock64, 0, 1) != 0);
    volatile KLong* address = getValueLocation<KLong>(thiz);
    *address = newValue;
    compareAndSwap(&lock64, 1, 0);
#else
    setImpl(thiz, newValue);
#endif
}

KLong Kotlin_AtomicLong_get(KRef thiz) {
#if KONAN_NO_64BIT_ATOMIC
    // Potentially huge performance penalty, but correct.
    while (compareAndSwap(&lock64, 0, 1) != 0);
    volatile KLong* address = getValueLocation<KLong>(thiz);
    KLong value = *address;
    compareAndSwap(&lock64, 1, 0);
    return value;
#else
    return getImpl<KLong>(thiz);
#endif
}

KNativePtr Kotlin_AtomicNativePtr_compareAndSwap(KRef thiz, KNativePtr expectedValue, KNativePtr newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

KBoolean Kotlin_AtomicNativePtr_compareAndSet(KRef thiz, KNativePtr expectedValue, KNativePtr newValue) {
    return compareAndSetImpl(thiz, expectedValue, newValue);
}

void Kotlin_AtomicNativePtr_set(KRef thiz, KNativePtr newValue) {
    setImpl(thiz, newValue);
}

KNativePtr Kotlin_AtomicNativePtr_get(KRef thiz) {
    return getImpl<KNativePtr>(thiz);
}

void Kotlin_AtomicReference_checkIfFrozen(KRef value) {
    if (value != nullptr && !isPermanentOrFrozen(value)) {
        ThrowInvalidMutabilityException(value);
    }
}

OBJ_GETTER(Kotlin_AtomicReference_compareAndSwap, KRef thiz, KRef expectedValue, KRef newValue) {
    Kotlin_AtomicReference_checkIfFrozen(newValue);
    // See Kotlin_AtomicReference_get() for explanations, why locking is needed.
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    RETURN_RESULT_OF(SwapHeapRefLocked, &ref->value_, expectedValue, newValue,
        &ref->lock_, &ref->cookie_);
}

KBoolean Kotlin_AtomicReference_compareAndSet(KRef thiz, KRef expectedValue, KRef newValue) {
    Kotlin_AtomicReference_checkIfFrozen(newValue);
    // See Kotlin_AtomicReference_get() for explanations, why locking is needed.
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    ObjHolder holder;
    auto old = SwapHeapRefLocked(&ref->value_, expectedValue, newValue,
        &ref->lock_, &ref->cookie_, holder.slot());
    return old == expectedValue;
}

void Kotlin_AtomicReference_set(KRef thiz, KRef newValue) {
    Kotlin_AtomicReference_checkIfFrozen(newValue);
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    SetHeapRefLocked(&ref->value_, newValue, &ref->lock_, &ref->cookie_);
}

OBJ_GETTER(Kotlin_AtomicReference_get, KRef thiz) {
    // Here we must take a lock to prevent race when value, while taken here, is CASed and immediately
    // destroyed by an another thread. AtomicReference no longer holds such an object, so if we got
    // rescheduled unluckily, between the moment value is read from the field and RC is incremented,
    // object may go away.
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    RETURN_RESULT_OF(ReadHeapRefLocked, &ref->value_, &ref->lock_, &ref->cookie_);
}

}  // extern "C"
