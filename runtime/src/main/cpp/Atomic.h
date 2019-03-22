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

static ALWAYS_INLINE inline void synchronize() {
#ifndef KONAN_NO_THREADS
  __sync_synchronize();
#endif
}

#endif // RUNTIME_ATOMIC_H