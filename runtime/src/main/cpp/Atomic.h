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


#endif // RUNTIME_ATOMIC_H