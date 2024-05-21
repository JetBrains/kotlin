// -*- C++ -*- header.

// Copyright (C) 2020-2022 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the
// Free Software Foundation; either version 3, or (at your option)
// any later version.

// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

/** @file bits/semaphore_base.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{semaphore}
 */

#ifndef _GLIBCXX_SEMAPHORE_BASE_H
#define _GLIBCXX_SEMAPHORE_BASE_H 1

#pragma GCC system_header

#include <bits/atomic_base.h>
#include <bits/chrono.h>
#if __cpp_lib_atomic_wait
#include <bits/atomic_timed_wait.h>
#include <ext/numeric_traits.h>
#endif // __cpp_lib_atomic_wait

#ifdef _GLIBCXX_HAVE_POSIX_SEMAPHORE
# include <cerrno>	// errno, EINTR, EAGAIN etc.
# include <limits.h>	// SEM_VALUE_MAX
# include <semaphore.h>	// sem_t, sem_init, sem_wait, sem_post etc.
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#ifdef _GLIBCXX_HAVE_POSIX_SEMAPHORE
  struct __platform_semaphore
  {
    using __clock_t = chrono::system_clock;
#ifdef SEM_VALUE_MAX
    static constexpr ptrdiff_t _S_max = SEM_VALUE_MAX;
#else
    static constexpr ptrdiff_t _S_max = _POSIX_SEM_VALUE_MAX;
#endif

    explicit __platform_semaphore(ptrdiff_t __count) noexcept
    {
      sem_init(&_M_semaphore, 0, __count);
    }

    __platform_semaphore(const __platform_semaphore&) = delete;
    __platform_semaphore& operator=(const __platform_semaphore&) = delete;

    ~__platform_semaphore()
    { sem_destroy(&_M_semaphore); }

    _GLIBCXX_ALWAYS_INLINE void
    _M_acquire() noexcept
    {
      for (;;)
	{
	  auto __err = sem_wait(&_M_semaphore);
	  if (__err && (errno == EINTR))
	    continue;
	  else if (__err)
	    std::__terminate();
	  else
	    break;
	}
    }

    _GLIBCXX_ALWAYS_INLINE bool
    _M_try_acquire() noexcept
    {
      for (;;)
	{
	  auto __err = sem_trywait(&_M_semaphore);
	  if (__err && (errno == EINTR))
	    continue;
	  else if (__err && (errno == EAGAIN))
	    return false;
	  else if (__err)
	    std::__terminate();
	  else
	    break;
	}
      return true;
    }

    _GLIBCXX_ALWAYS_INLINE void
    _M_release(std::ptrdiff_t __update) noexcept
    {
      for(; __update != 0; --__update)
	{
	   auto __err = sem_post(&_M_semaphore);
	   if (__err)
	     std::__terminate();
	}
    }

    bool
    _M_try_acquire_until_impl(const chrono::time_point<__clock_t>& __atime)
      noexcept
    {

      auto __s = chrono::time_point_cast<chrono::seconds>(__atime);
      auto __ns = chrono::duration_cast<chrono::nanoseconds>(__atime - __s);

      struct timespec __ts =
      {
	static_cast<std::time_t>(__s.time_since_epoch().count()),
	static_cast<long>(__ns.count())
      };

      for (;;)
	{
	  if (auto __err = sem_timedwait(&_M_semaphore, &__ts))
	    {
	      if (errno == EINTR)
		continue;
	      else if (errno == ETIMEDOUT || errno == EINVAL)
		return false;
	      else
		std::__terminate();
	    }
	  else
	    break;
	}
      return true;
    }

    template<typename _Clock, typename _Duration>
      bool
      _M_try_acquire_until(const chrono::time_point<_Clock,
			   _Duration>& __atime) noexcept
      {
	if constexpr (std::is_same_v<__clock_t, _Clock>)
	  {
	    return _M_try_acquire_until_impl(__atime);
	  }
	else
	  {
	    const typename _Clock::time_point __c_entry = _Clock::now();
	    const auto __s_entry = __clock_t::now();
	    const auto __delta = __atime - __c_entry;
	    const auto __s_atime = __s_entry + __delta;
	    if (_M_try_acquire_until_impl(__s_atime))
	      return true;

	    // We got a timeout when measured against __clock_t but
	    // we need to check against the caller-supplied clock
	    // to tell whether we should return a timeout.
	    return (_Clock::now() < __atime);
	  }
      }

    template<typename _Rep, typename _Period>
      _GLIBCXX_ALWAYS_INLINE bool
      _M_try_acquire_for(const chrono::duration<_Rep, _Period>& __rtime)
	noexcept
      { return _M_try_acquire_until(__clock_t::now() + __rtime); }

  private:
    sem_t _M_semaphore;
  };
#endif // _GLIBCXX_HAVE_POSIX_SEMAPHORE

#if __cpp_lib_atomic_wait
  struct __atomic_semaphore
  {
    static constexpr ptrdiff_t _S_max = __gnu_cxx::__int_traits<int>::__max;
    explicit __atomic_semaphore(__detail::__platform_wait_t __count) noexcept
      : _M_counter(__count)
    {
      __glibcxx_assert(__count >= 0 && __count <= _S_max);
    }

    __atomic_semaphore(const __atomic_semaphore&) = delete;
    __atomic_semaphore& operator=(const __atomic_semaphore&) = delete;

    static _GLIBCXX_ALWAYS_INLINE bool
    _S_do_try_acquire(__detail::__platform_wait_t* __counter) noexcept
    {
      auto __old = __atomic_impl::load(__counter, memory_order::acquire);
      if (__old == 0)
	return false;

      return __atomic_impl::compare_exchange_strong(__counter,
						    __old, __old - 1,
						    memory_order::acquire,
						    memory_order::relaxed);
    }

    _GLIBCXX_ALWAYS_INLINE void
    _M_acquire() noexcept
    {
      auto const __pred =
	[this] { return _S_do_try_acquire(&this->_M_counter); };
      std::__atomic_wait_address_bare(&_M_counter, __pred);
    }

    bool
    _M_try_acquire() noexcept
    {
      auto const __pred =
	[this] { return _S_do_try_acquire(&this->_M_counter); };
      return std::__detail::__atomic_spin(__pred);
    }

    template<typename _Clock, typename _Duration>
      _GLIBCXX_ALWAYS_INLINE bool
      _M_try_acquire_until(const chrono::time_point<_Clock,
			   _Duration>& __atime) noexcept
      {
	auto const __pred =
	  [this] { return _S_do_try_acquire(&this->_M_counter); };

	return __atomic_wait_address_until_bare(&_M_counter, __pred, __atime);
      }

    template<typename _Rep, typename _Period>
      _GLIBCXX_ALWAYS_INLINE bool
      _M_try_acquire_for(const chrono::duration<_Rep, _Period>& __rtime)
	noexcept
      {
	auto const __pred =
	  [this] { return _S_do_try_acquire(&this->_M_counter); };

	return __atomic_wait_address_for_bare(&_M_counter, __pred, __rtime);
      }

    _GLIBCXX_ALWAYS_INLINE void
    _M_release(ptrdiff_t __update) noexcept
    {
      if (0 < __atomic_impl::fetch_add(&_M_counter, __update, memory_order_release))
	return;
      if (__update > 1)
	__atomic_notify_address_bare(&_M_counter, true);
      else
	__atomic_notify_address_bare(&_M_counter, true);
// FIXME - Figure out why this does not wake a waiting thread
//	__atomic_notify_address_bare(&_M_counter, false);
    }

  private:
    alignas(__detail::__platform_wait_alignment)
    __detail::__platform_wait_t _M_counter;
  };
#endif // __cpp_lib_atomic_wait

// Note: the _GLIBCXX_USE_POSIX_SEMAPHORE macro can be used to force the
// use of Posix semaphores (sem_t). Doing so however, alters the ABI.
#if defined __cpp_lib_atomic_wait && !_GLIBCXX_USE_POSIX_SEMAPHORE
  using __semaphore_impl = __atomic_semaphore;
#elif _GLIBCXX_HAVE_POSIX_SEMAPHORE
  using __semaphore_impl = __platform_semaphore;
#endif

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif // _GLIBCXX_SEMAPHORE_BASE_H
