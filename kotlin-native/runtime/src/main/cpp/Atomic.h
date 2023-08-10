#ifndef RUNTIME_ATOMIC_H
#define RUNTIME_ATOMIC_H

#include "Common.h"

template <typename T>
ALWAYS_INLINE inline T atomicAdd(volatile T* where, T what) {
  return __sync_add_and_fetch(where, what);
}

#pragma clang diagnostic push

#if (KONAN_ANDROID || KONAN_IOS || KONAN_WATCHOS || KONAN_LINUX) && (KONAN_ARM32 || KONAN_X86)
// On 32-bit Android clang generates library calls for "large" atomic operations
// and warns about "significant performance penalty". See more details here:
// https://github.com/llvm/llvm-project/blob/ce56e1a1cc5714f4af5675dd963cfebed766d9e1/clang/lib/CodeGen/CGAtomic.cpp#L775
// Ignore these warnings:
#pragma clang diagnostic ignored "-Watomic-alignment"
#endif

// as if (std::atomic<T> where).compare_exchange_strong(expectedValue, newValue)
template <typename T>
ALWAYS_INLINE inline bool compareExchange(volatile T& where, T &expectedValue, T newValue) {
#ifdef KONAN_NO_64BIT_ATOMIC
    static_assert(sizeof(T) <= 4);
#endif
    return __atomic_compare_exchange_n(&where, &expectedValue, newValue, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
}

template <typename T>
ALWAYS_INLINE inline T compareAndSwap(volatile T* where, T expectedValue, T newValue) {
    compareExchange(*where, expectedValue, newValue);
    return expectedValue;
}

template <typename T>
ALWAYS_INLINE inline bool compareAndSet(volatile T* where, T expectedValue, T newValue) {
    return compareExchange(*where, expectedValue, newValue);
}


template <int model = __ATOMIC_SEQ_CST, typename T>
ALWAYS_INLINE inline void atomicSet(volatile T* where, T what) {
#ifdef KONAN_NO_64BIT_ATOMIC
  static_assert(sizeof(T) <= 4);
#endif
  __atomic_store(where, &what, model);
}

template <typename T>
ALWAYS_INLINE inline void atomicSetRelease(volatile T* where, T what) {
    return atomicSet<__ATOMIC_RELEASE>(where, what);
}


template <int model = __ATOMIC_SEQ_CST, typename T>
ALWAYS_INLINE inline T atomicGet(volatile const T* where) {
#ifdef KONAN_NO_64BIT_ATOMIC
  static_assert(sizeof(T) <= 4);
#endif
  T what;
  __atomic_load(where, &what, model);
  return what;
}

template <typename T>
ALWAYS_INLINE inline T atomicGetAcquire(volatile const T* where) {
    return atomicGet<__ATOMIC_ACQUIRE>(where);
}

template <typename T>
ALWAYS_INLINE inline T atomicGetRelaxed(volatile const T* where) {
    return atomicGet<__ATOMIC_RELAXED>(where);
}

#pragma clang diagnostic pop

static ALWAYS_INLINE inline void synchronize() {
  __sync_synchronize();
}

#endif // RUNTIME_ATOMIC_H
