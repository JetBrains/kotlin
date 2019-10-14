#ifndef RUNTIME_ATOMIC_H
#define RUNTIME_ATOMIC_H

#include "Common.h"

template <typename T>
ALWAYS_INLINE inline T atomicAdd(volatile T* where, T what) {
#ifndef KONAN_NO_THREADS
  return __sync_add_and_fetch(where, what);
#else
  return *where += what;
#endif
}

template <typename T>
ALWAYS_INLINE inline T compareAndSwap(volatile T* where, T expectedValue, T newValue) {
#ifndef KONAN_NO_THREADS
  return __sync_val_compare_and_swap(where, expectedValue, newValue);
#else
   T oldValue = *where;
   if (oldValue == expectedValue) {
        *where = newValue;
   }
   return oldValue;
#endif
}

template <typename T>
ALWAYS_INLINE inline bool compareAndSet(volatile T* where, T expectedValue, T newValue) {
#ifndef KONAN_NO_THREADS
  return __sync_bool_compare_and_swap(where, expectedValue, newValue);
#else
   T oldValue = *where;
   if (oldValue == expectedValue) {
        *where = newValue;
        return true;
   }
   return false;
#endif
}

#pragma clang diagnostic push

#if KONAN_ANDROID && (KONAN_ARM32 || KONAN_X86)
// On 32-bit Android clang generates library calls for "large" atomic operations
// and warns about "significant performance penalty". See more details here:
// https://github.com/llvm/llvm-project/blob/ce56e1a1cc5714f4af5675dd963cfebed766d9e1/clang/lib/CodeGen/CGAtomic.cpp#L775
// Ignore these warnings:
#pragma clang diagnostic ignored "-Watomic-alignment"
#endif

template <typename T>
ALWAYS_INLINE inline void atomicSet(volatile T* where, T what) {
#ifndef KONAN_NO_THREADS
  __atomic_store(where, &what, __ATOMIC_SEQ_CST);
#else
  *where = what;
#endif
}

template <typename T>
ALWAYS_INLINE inline T atomicGet(volatile T* where) {
#ifndef KONAN_NO_THREADS
  T what;
  __atomic_load(where, &what, __ATOMIC_SEQ_CST);
  return what;
#else
  return *where;
#endif
}

#pragma clang diagnostic pop

static ALWAYS_INLINE inline void synchronize() {
#ifndef KONAN_NO_THREADS
  __sync_synchronize();
#endif
}

#endif // RUNTIME_ATOMIC_H