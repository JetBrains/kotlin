/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"LICENSE" at the root of this distribution.
-----------------------------------------------------------------------------*/
#pragma once
#ifndef MIMALLOC_ATOMIC_H
#define MIMALLOC_ATOMIC_H

// ------------------------------------------------------
// Atomics 
// We need to be portable between C, C++, and MSVC.
// ------------------------------------------------------

#if defined(_MSC_VER)
#define _Atomic(tp)         tp
#define ATOMIC_VAR_INIT(x)  x
#elif defined(__cplusplus)
#include <atomic>
#define  _Atomic(tp)        std::atomic<tp>
#else
#include <stdatomic.h>
#endif

#define mi_atomic_cast(tp,x)  (volatile _Atomic(tp)*)(x)

// ------------------------------------------------------
// Atomic operations specialized for mimalloc
// ------------------------------------------------------

// Atomically add a 64-bit value; returns the previous value. 
// Note: not using _Atomic(int64_t) as it is only used for statistics.
static inline void mi_atomic_add64(volatile int64_t* p, int64_t add);

// Atomically add a value; returns the previous value. Memory ordering is relaxed.
static inline intptr_t mi_atomic_add(volatile _Atomic(intptr_t)* p, intptr_t add);

// Atomically compare and exchange a value; returns `true` if successful. 
// May fail spuriously. Memory ordering as release on success, and relaxed on failure.
// (Note: expected and desired are in opposite order from atomic_compare_exchange)
static inline bool mi_atomic_cas_weak(volatile _Atomic(uintptr_t)* p, uintptr_t desired, uintptr_t expected);

// Atomically compare and exchange a value; returns `true` if successful.
// Memory ordering is acquire-release
// (Note: expected and desired are in opposite order from atomic_compare_exchange)
static inline bool mi_atomic_cas_strong(volatile _Atomic(uintptr_t)* p, uintptr_t desired, uintptr_t expected);

// Atomically exchange a value. Memory ordering is acquire-release.
static inline uintptr_t mi_atomic_exchange(volatile _Atomic(uintptr_t)* p, uintptr_t exchange);

// Atomically read a value. Memory ordering is relaxed.
static inline uintptr_t mi_atomic_read_relaxed(const volatile _Atomic(uintptr_t)* p);

// Atomically read a value. Memory ordering is acquire.
static inline uintptr_t mi_atomic_read(const volatile _Atomic(uintptr_t)* p);

// Atomically write a value. Memory ordering is release.
static inline void mi_atomic_write(volatile _Atomic(uintptr_t)* p, uintptr_t x);

// Yield
static inline void mi_atomic_yield(void);



// Atomically add a value; returns the previous value.
static inline uintptr_t mi_atomic_addu(volatile _Atomic(uintptr_t)* p, uintptr_t add) {
  return (uintptr_t)mi_atomic_add((volatile _Atomic(intptr_t)*)p, (intptr_t)add);
}
// Atomically subtract a value; returns the previous value.
static inline uintptr_t mi_atomic_subu(volatile _Atomic(uintptr_t)* p, uintptr_t sub) {
  return (uintptr_t)mi_atomic_add((volatile _Atomic(intptr_t)*)p, -((intptr_t)sub));
}

// Atomically increment a value; returns the incremented result.
static inline uintptr_t mi_atomic_increment(volatile _Atomic(uintptr_t)* p) {
  return mi_atomic_addu(p, 1);
}

// Atomically decrement a value; returns the decremented result.
static inline uintptr_t mi_atomic_decrement(volatile _Atomic(uintptr_t)* p) {
  return mi_atomic_subu(p, 1);
}

// Atomically read a pointer; Memory order is relaxed.
static inline void* mi_atomic_read_ptr_relaxed(volatile _Atomic(void*) const * p) {
  return (void*)mi_atomic_read_relaxed((const volatile _Atomic(uintptr_t)*)p);
}

// Atomically read a pointer; Memory order is acquire.
static inline void* mi_atomic_read_ptr(volatile _Atomic(void*) const * p) {
  return (void*)mi_atomic_read((const volatile _Atomic(uintptr_t)*)p);
}

// Atomically write a pointer
static inline void mi_atomic_write_ptr(volatile _Atomic(void*)* p, void* x) {
  mi_atomic_write((volatile _Atomic(uintptr_t)*)p, (uintptr_t)x );
}

// Atomically compare and exchange a pointer; returns `true` if successful. May fail spuriously.
// (Note: expected and desired are in opposite order from atomic_compare_exchange)
static inline bool mi_atomic_cas_ptr_weak(volatile _Atomic(void*)* p, void* desired, void* expected) {
  return mi_atomic_cas_weak((volatile _Atomic(uintptr_t)*)p, (uintptr_t)desired, (uintptr_t)expected);
}

// Atomically compare and exchange a pointer; returns `true` if successful.
// (Note: expected and desired are in opposite order from atomic_compare_exchange)
static inline bool mi_atomic_cas_ptr_strong(volatile _Atomic(void*)* p, void* desired, void* expected) {
  return mi_atomic_cas_strong((volatile _Atomic(uintptr_t)*)p, (uintptr_t)desired, (uintptr_t)expected);
}

// Atomically exchange a pointer value.
static inline void* mi_atomic_exchange_ptr(volatile _Atomic(void*)* p, void* exchange) {
  return (void*)mi_atomic_exchange((volatile _Atomic(uintptr_t)*)p, (uintptr_t)exchange);
}


#ifdef _MSC_VER
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <intrin.h>
#ifdef _WIN64
typedef LONG64   msc_intptr_t;
#define RC64(f)  f##64
#else
typedef LONG     msc_intptr_t;
#define RC64(f)  f
#endif
static inline intptr_t mi_atomic_add(volatile _Atomic(intptr_t)* p, intptr_t add) {
  return (intptr_t)RC64(_InterlockedExchangeAdd)((volatile msc_intptr_t*)p, (msc_intptr_t)add);
}
static inline bool mi_atomic_cas_strong(volatile _Atomic(uintptr_t)* p, uintptr_t desired, uintptr_t expected) {
  return (expected == (uintptr_t)RC64(_InterlockedCompareExchange)((volatile msc_intptr_t*)p, (msc_intptr_t)desired, (msc_intptr_t)expected));
}
static inline bool mi_atomic_cas_weak(volatile _Atomic(uintptr_t)* p, uintptr_t desired, uintptr_t expected) {
  return mi_atomic_cas_strong(p,desired,expected);
}
static inline uintptr_t mi_atomic_exchange(volatile _Atomic(uintptr_t)* p, uintptr_t exchange) {
  return (uintptr_t)RC64(_InterlockedExchange)((volatile msc_intptr_t*)p, (msc_intptr_t)exchange);
}
static inline uintptr_t mi_atomic_read(volatile _Atomic(uintptr_t) const* p) {
  return *p;
}
static inline uintptr_t mi_atomic_read_relaxed(volatile _Atomic(uintptr_t) const* p) {
  return mi_atomic_read(p);
}
static inline void mi_atomic_write(volatile _Atomic(uintptr_t)* p, uintptr_t x) {
  mi_atomic_exchange(p,x);
}
static inline void mi_atomic_yield(void) {
  YieldProcessor();
}
static inline void mi_atomic_add64(volatile _Atomic(int64_t)* p, int64_t add) {
  #ifdef _WIN64
  mi_atomic_add(p,add);
  #else
  int64_t current;
  int64_t sum;
  do {
    current = *p;
    sum = current + add;
  } while (_InterlockedCompareExchange64(p, sum, current) != current);
  #endif
}

#else
#ifdef __cplusplus
#define  MI_USING_STD   using namespace std;
#else
#define  MI_USING_STD
#endif
static inline void mi_atomic_add64(volatile int64_t* p, int64_t add) {
  MI_USING_STD
  atomic_fetch_add_explicit((volatile _Atomic(int64_t)*)p, add, memory_order_relaxed);
}
static inline intptr_t mi_atomic_add(volatile _Atomic(intptr_t)* p, intptr_t add) {
  MI_USING_STD
  return atomic_fetch_add_explicit(p, add, memory_order_relaxed);
}
static inline bool mi_atomic_cas_weak(volatile _Atomic(uintptr_t)* p, uintptr_t desired, uintptr_t expected) {
  MI_USING_STD
  return atomic_compare_exchange_weak_explicit(p, &expected, desired, memory_order_release, memory_order_relaxed);
}
static inline bool mi_atomic_cas_strong(volatile _Atomic(uintptr_t)* p, uintptr_t desired, uintptr_t expected) {
  MI_USING_STD
  return atomic_compare_exchange_strong_explicit(p, &expected, desired, memory_order_acq_rel, memory_order_relaxed);
}
static inline uintptr_t mi_atomic_exchange(volatile _Atomic(uintptr_t)* p, uintptr_t exchange) {
  MI_USING_STD
  return atomic_exchange_explicit(p, exchange, memory_order_acq_rel);
}
static inline uintptr_t mi_atomic_read_relaxed(const volatile _Atomic(uintptr_t)* p) {
  MI_USING_STD
  return atomic_load_explicit((volatile _Atomic(uintptr_t)*) p, memory_order_relaxed);
}
static inline uintptr_t mi_atomic_read(const volatile _Atomic(uintptr_t)* p) {
  MI_USING_STD
  return atomic_load_explicit((volatile _Atomic(uintptr_t)*) p, memory_order_acquire);
}
static inline void mi_atomic_write(volatile _Atomic(uintptr_t)* p, uintptr_t x) {
  MI_USING_STD
  return atomic_store_explicit(p, x, memory_order_release);
}

#if defined(__cplusplus)
  #include <thread>
  static inline void mi_atomic_yield(void) {
    std::this_thread::yield();
  }
#elif (defined(__GNUC__) || defined(__clang__)) && \
      (defined(__x86_64__) || defined(__i386__) || defined(__arm__) || defined(__aarch64__))
#if defined(__x86_64__) || defined(__i386__)
  static inline void mi_atomic_yield(void) {
    asm volatile ("pause" ::: "memory");
  }
#elif defined(__arm__) || defined(__aarch64__)
  static inline void mi_atomic_yield(void) {
    asm volatile("yield");
  }
#endif
#elif defined(__wasi__)
  #include <sched.h>
  static inline void mi_atomic_yield(void) {
    sched_yield();
  }
#else
  #include <unistd.h>
  static inline void mi_atomic_yield(void) {
    sleep(0);
  }
#endif

#endif

#endif // __MIMALLOC_ATOMIC_H
