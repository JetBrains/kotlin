/* Copyright (C) 1997-2023 Free Software Foundation, Inc.

This file is part of GCC.

GCC is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 3, or (at your option) any later
version.

GCC is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

Under Section 7 of GPL version 3, you are granted additional
permissions described in the GCC Runtime Library Exception, version
3.1, as published by the Free Software Foundation.

You should have received a copy of the GNU General Public License and
a copy of the GCC Runtime Library Exception along with this program;
see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
<http://www.gnu.org/licenses/>.  */

/* gthr.h API implementation in terms of ISO C11 threads.h */

#ifndef _GLIBCXX_GCC_GTHR_C11_H
#define _GLIBCXX_GCC_GTHR_C11_H

#define __GTHREADS 1
#define __GTHREADS_CXX0X 1
#define __GTHREAD_ONCE_INIT ONCE_FLAG_INIT

#define _GTHREAD_USE_MUTEX_TIMEDLOCK 1

#undef __GTHREAD_MUTEX_INIT
#define _GTHREAD_USE_MUTEX_INIT_FUNC 1
#define __GTHREAD_MUTEX_INIT_FUNCTION __gthread_mutex_init_function

#undef __GTHREAD_RECURSIVE_MUTEX_INIT
#define _GTHREAD_USE_RECURSIVE_MUTEX_INIT_FUNC 1
#define __GTHREAD_RECURSIVE_MUTEX_INIT_FUNCTION __gthread_recursive_mutex_init_function

#define __GTHREAD_HAS_COND	1
#undef __GTHREAD_COND_INIT
#define _GTHREAD_USE_COND_INIT_FUNC 1
#define __GTHREAD_COND_INIT_FUNCTION __gthread_cond_init_function

#define _GLIBCXX_THREAD_ABI_COMPAT 1
#define _GLIBCXX_HAS_GTHREADS 1
#define _GLIBCXX_USE_THRD_SLEEP 1

#include <threads.h>
#include <time.h>

typedef thrd_t __gthread_t;
typedef tss_t __gthread_key_t;
typedef once_flag __gthread_once_t;
typedef mtx_t __gthread_mutex_t;
typedef mtx_t __gthread_recursive_mutex_t;
typedef cnd_t __gthread_cond_t;
typedef struct timespec __gthread_time_t;

static inline int __gthread_active_p(void)
{
  return 1;
}

static inline int
__gthread_create (__gthread_t *__threadid, void *(*__func) (void*),
		  void *__args)
{
  return thrd_create(__threadid, (thrd_start_t)__func, __args) != thrd_success;
}

static inline int
__gthread_join (__gthread_t __threadid, void **__value_ptr)
{
  return thrd_join(__threadid, (int *)__value_ptr) != thrd_success;
}

static inline int
__gthread_detach (__gthread_t __threadid)
{
  return thrd_detach(__threadid) != thrd_success;
}

static inline int
__gthread_equal (__gthread_t __t1, __gthread_t __t2)
{
  return thrd_equal(__t1, __t2);
}

static inline __gthread_t
__gthread_self (void)
{
  return thrd_current();
}

static inline int
__gthread_yield (void)
{
  (void)thrd_yield();
  return 0;
}

static inline int
__gthread_once (__gthread_once_t *__once, void (*__func) (void))
{
  call_once(__once, __func);
  return 0;
}

static inline int
__gthread_key_create (__gthread_key_t *__key, void (*__dtor) (void *))
{
  return tss_create(__key, __dtor) != thrd_success;
}

static inline int
__gthread_key_delete (__gthread_key_t __key)
{
  tss_delete(__key);
  return 0;
}

static inline void *
__gthread_getspecific (__gthread_key_t __key)
{
  return tss_get(__key);
}

static inline int
__gthread_setspecific (__gthread_key_t __key, const void *__ptr)
{
  return tss_set(__key, (void *)__ptr) != thrd_success;
}

static inline void
__gthread_mutex_init_function (__gthread_mutex_t *__mutex)
{
    (void)mtx_init(__mutex, mtx_plain);
}

static inline int
__gthread_mutex_destroy (__gthread_mutex_t *__mutex)
{
    mtx_destroy(__mutex);
    return 0;
}

static inline int
__gthread_mutex_lock (__gthread_mutex_t *__mutex)
{
    int mtx_lock_res = mtx_lock(__mutex);
    int compare_to = thrd_success;

    return mtx_lock_res != compare_to;
}

static inline int
__gthread_mutex_trylock (__gthread_mutex_t *__mutex)
{
    return mtx_trylock(__mutex) != thrd_success;
}

static inline int
__gthread_mutex_timedlock (__gthread_mutex_t *__mutex,
			   const __gthread_time_t *__abs_timeout)
{
    return mtx_timedlock(__mutex, __abs_timeout) != thrd_success;
}

static inline int
__gthread_mutex_unlock (__gthread_mutex_t *__mutex)
{
    return mtx_unlock(__mutex) != thrd_success;
}

static inline int
__gthread_recursive_mutex_init_function (__gthread_recursive_mutex_t *__mutex)
{
    return mtx_init(__mutex, mtx_recursive) != thrd_success;
}

static inline int
__gthread_recursive_mutex_lock (__gthread_recursive_mutex_t *__mutex)
{
  int res = mtx_lock(__mutex);
  int cmp = thrd_success;
  return res != thrd_success;
}

static inline int
__gthread_recursive_mutex_trylock (__gthread_recursive_mutex_t *__mutex)
{
  return mtx_trylock(__mutex) != thrd_success;
}

static inline int
__gthread_recursive_mutex_timedlock (__gthread_recursive_mutex_t *__mutex,
				     const __gthread_time_t *__abs_timeout)
{
  return __gthread_mutex_timedlock (__mutex, __abs_timeout);
}

static inline int
__gthread_recursive_mutex_unlock (__gthread_recursive_mutex_t *__mutex)
{
  return __gthread_mutex_unlock (__mutex);
}

static inline int
__gthread_recursive_mutex_destroy (__gthread_recursive_mutex_t *__mutex)
{
  return __gthread_mutex_destroy (__mutex);
}

static inline void
__gthread_cond_init_function (__gthread_cond_t *__cond)
{
     (void)cnd_init(__cond);
}

static inline int
__gthread_cond_broadcast (__gthread_cond_t *__cond)
{
  return cnd_broadcast(__cond) != thrd_success;
}

static inline int
__gthread_cond_signal (__gthread_cond_t *__cond)
{
  return cnd_signal(__cond) != thrd_success;
}

static inline int
__gthread_cond_wait (__gthread_cond_t *__cond, __gthread_mutex_t *__mutex)
{
  return cnd_wait(__cond, __mutex) != thrd_success;
}

static inline int
__gthread_cond_timedwait (__gthread_cond_t *__cond, __gthread_mutex_t *__mutex,
			  const __gthread_time_t *__abs_timeout)
{
  return cnd_timedwait(__cond, __mutex, __abs_timeout) != thrd_success;
}

static inline int
__gthread_cond_wait_recursive (__gthread_cond_t *__cond,
			       __gthread_recursive_mutex_t *__mutex)
{
  return __gthread_cond_wait (__cond, __mutex);
}

static inline int
__gthread_cond_destroy (__gthread_cond_t* __cond)
{
  cnd_destroy(__cond);
  return 0;
}

#endif /* ! _GLIBCXX_GCC_GTHR_C11_H */
