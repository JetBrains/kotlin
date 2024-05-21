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

/** @file bits/atomic_timed_wait.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{atomic}
 */

#ifndef _GLIBCXX_ATOMIC_TIMED_WAIT_H
#define _GLIBCXX_ATOMIC_TIMED_WAIT_H 1

#pragma GCC system_header

#include <bits/atomic_wait.h>

#if __cpp_lib_atomic_wait
#include <bits/functional_hash.h>
#include <bits/this_thread_sleep.h>
#include <bits/chrono.h>

#ifdef _GLIBCXX_HAVE_LINUX_FUTEX
#include <sys/time.h>
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  namespace __detail
  {
    using __wait_clock_t = chrono::steady_clock;

    template<typename _Clock, typename _Dur>
      __wait_clock_t::time_point
      __to_wait_clock(const chrono::time_point<_Clock, _Dur>& __atime) noexcept
      {
	const typename _Clock::time_point __c_entry = _Clock::now();
	const __wait_clock_t::time_point __w_entry = __wait_clock_t::now();
	const auto __delta = __atime - __c_entry;
	using __w_dur = typename __wait_clock_t::duration;
	return __w_entry + chrono::ceil<__w_dur>(__delta);
      }

    template<typename _Dur>
      __wait_clock_t::time_point
      __to_wait_clock(const chrono::time_point<__wait_clock_t,
					       _Dur>& __atime) noexcept
      {
	using __w_dur = typename __wait_clock_t::duration;
	return chrono::ceil<__w_dur>(__atime);
      }

#ifdef _GLIBCXX_HAVE_LINUX_FUTEX
#define _GLIBCXX_HAVE_PLATFORM_TIMED_WAIT
    // returns true if wait ended before timeout
    template<typename _Dur>
      bool
      __platform_wait_until_impl(const __platform_wait_t* __addr,
				 __platform_wait_t __old,
				 const chrono::time_point<__wait_clock_t, _Dur>&
				      __atime) noexcept
      {
	auto __s = chrono::time_point_cast<chrono::seconds>(__atime);
	auto __ns = chrono::duration_cast<chrono::nanoseconds>(__atime - __s);

	struct timespec __rt =
	{
	  static_cast<std::time_t>(__s.time_since_epoch().count()),
	  static_cast<long>(__ns.count())
	};

	auto __e = syscall (SYS_futex, __addr,
			    static_cast<int>(__futex_wait_flags::
						__wait_bitset_private),
			    __old, &__rt, nullptr,
			    static_cast<int>(__futex_wait_flags::
						__bitset_match_any));

	if (__e)
	  {
	    if (errno == ETIMEDOUT)
	      return false;
	    if (errno != EINTR && errno != EAGAIN)
	      __throw_system_error(errno);
	  }
	return true;
      }

    // returns true if wait ended before timeout
    template<typename _Clock, typename _Dur>
      bool
      __platform_wait_until(const __platform_wait_t* __addr, __platform_wait_t __old,
			    const chrono::time_point<_Clock, _Dur>& __atime)
      {
	if constexpr (is_same_v<__wait_clock_t, _Clock>)
	  {
	    return __platform_wait_until_impl(__addr, __old, __atime);
	  }
	else
	  {
	    if (!__platform_wait_until_impl(__addr, __old,
					    __to_wait_clock(__atime)))
	      {
		// We got a timeout when measured against __clock_t but
		// we need to check against the caller-supplied clock
		// to tell whether we should return a timeout.
		if (_Clock::now() < __atime)
		  return true;
	      }
	    return false;
	  }
      }
#else
// define _GLIBCXX_HAVE_PLATFORM_TIMED_WAIT and implement __platform_wait_until()
// if there is a more efficient primitive supported by the platform
// (e.g. __ulock_wait())which is better than pthread_cond_clockwait
#endif // ! PLATFORM_TIMED_WAIT

#ifdef _GLIBCXX_HAS_GTHREADS
    // Returns true if wait ended before timeout.
    // _Clock must be either steady_clock or system_clock.
    template<typename _Clock, typename _Dur>
      bool
      __cond_wait_until_impl(__condvar& __cv, mutex& __mx,
			     const chrono::time_point<_Clock, _Dur>& __atime)
      {
	static_assert(std::__is_one_of<_Clock, chrono::steady_clock,
					       chrono::system_clock>::value);

	auto __s = chrono::time_point_cast<chrono::seconds>(__atime);
	auto __ns = chrono::duration_cast<chrono::nanoseconds>(__atime - __s);

	__gthread_time_t __ts =
	  {
	    static_cast<std::time_t>(__s.time_since_epoch().count()),
	    static_cast<long>(__ns.count())
	  };

#ifdef _GLIBCXX_USE_PTHREAD_COND_CLOCKWAIT
	if constexpr (is_same_v<chrono::steady_clock, _Clock>)
	  __cv.wait_until(__mx, CLOCK_MONOTONIC, __ts);
	else
#endif
	  __cv.wait_until(__mx, __ts);
	return _Clock::now() < __atime;
      }

    // returns true if wait ended before timeout
    template<typename _Clock, typename _Dur>
      bool
      __cond_wait_until(__condvar& __cv, mutex& __mx,
	  const chrono::time_point<_Clock, _Dur>& __atime)
      {
#ifdef _GLIBCXX_USE_PTHREAD_COND_CLOCKWAIT
	if constexpr (is_same_v<_Clock, chrono::steady_clock>)
	  return __detail::__cond_wait_until_impl(__cv, __mx, __atime);
	else
#endif
	if constexpr (is_same_v<_Clock, chrono::system_clock>)
	  return __detail::__cond_wait_until_impl(__cv, __mx, __atime);
	else
	  {
	    if (__cond_wait_until_impl(__cv, __mx,
				       __to_wait_clock(__atime)))
	      {
		// We got a timeout when measured against __clock_t but
		// we need to check against the caller-supplied clock
		// to tell whether we should return a timeout.
		if (_Clock::now() < __atime)
		  return true;
	      }
	    return false;
	  }
      }
#endif // _GLIBCXX_HAS_GTHREADS

    struct __timed_waiter_pool : __waiter_pool_base
    {
      // returns true if wait ended before timeout
      template<typename _Clock, typename _Dur>
	bool
	_M_do_wait_until(__platform_wait_t* __addr, __platform_wait_t __old,
			 const chrono::time_point<_Clock, _Dur>& __atime)
	{
#ifdef _GLIBCXX_HAVE_PLATFORM_TIMED_WAIT
	  return __platform_wait_until(__addr, __old, __atime);
#else
	  __platform_wait_t __val;
	  __atomic_load(__addr, &__val, __ATOMIC_RELAXED);
	  if (__val == __old)
	    {
	      lock_guard<mutex> __l(_M_mtx);
	      return __cond_wait_until(_M_cv, _M_mtx, __atime);
	    }
	  else
	    return true;
#endif // _GLIBCXX_HAVE_PLATFORM_TIMED_WAIT
	}
    };

    struct __timed_backoff_spin_policy
    {
      __wait_clock_t::time_point _M_deadline;
      __wait_clock_t::time_point _M_t0;

      template<typename _Clock, typename _Dur>
	__timed_backoff_spin_policy(chrono::time_point<_Clock, _Dur>
				      __deadline = _Clock::time_point::max(),
				    chrono::time_point<_Clock, _Dur>
				      __t0 = _Clock::now()) noexcept
	  : _M_deadline(__to_wait_clock(__deadline))
	  , _M_t0(__to_wait_clock(__t0))
	{ }

      bool
      operator()() const noexcept
      {
	using namespace literals::chrono_literals;
	auto __now = __wait_clock_t::now();
	if (_M_deadline <= __now)
	  return false;

	// FIXME: this_thread::sleep_for not available #ifdef _GLIBCXX_NO_SLEEP

	auto __elapsed = __now - _M_t0;
	if (__elapsed > 128ms)
	  {
	    this_thread::sleep_for(64ms);
	  }
	else if (__elapsed > 64us)
	  {
	    this_thread::sleep_for(__elapsed / 2);
	  }
	else if (__elapsed > 4us)
	  {
	    __thread_yield();
	  }
	else
	  return false;
	return true;
      }
    };

    template<typename _EntersWait>
      struct __timed_waiter : __waiter_base<__timed_waiter_pool>
      {
	using __base_type = __waiter_base<__timed_waiter_pool>;

	template<typename _Tp>
	  __timed_waiter(const _Tp* __addr) noexcept
	  : __base_type(__addr)
	{
	  if constexpr (_EntersWait::value)
	    _M_w._M_enter_wait();
	}

	~__timed_waiter()
	{
	  if constexpr (_EntersWait::value)
	    _M_w._M_leave_wait();
	}

	// returns true if wait ended before timeout
	template<typename _Tp, typename _ValFn,
		 typename _Clock, typename _Dur>
	  bool
	  _M_do_wait_until_v(_Tp __old, _ValFn __vfn,
			     const chrono::time_point<_Clock, _Dur>&
								__atime) noexcept
	  {
	    __platform_wait_t __val;
	    if (_M_do_spin(__old, std::move(__vfn), __val,
			   __timed_backoff_spin_policy(__atime)))
	      return true;
	    return __base_type::_M_w._M_do_wait_until(__base_type::_M_addr, __val, __atime);
	  }

	// returns true if wait ended before timeout
	template<typename _Pred,
		 typename _Clock, typename _Dur>
	  bool
	  _M_do_wait_until(_Pred __pred, __platform_wait_t __val,
			  const chrono::time_point<_Clock, _Dur>&
							      __atime) noexcept
	  {
	    for (auto __now = _Clock::now(); __now < __atime;
		  __now = _Clock::now())
	      {
		if (__base_type::_M_w._M_do_wait_until(
		      __base_type::_M_addr, __val, __atime)
		    && __pred())
		  return true;

		if (__base_type::_M_do_spin(__pred, __val,
			       __timed_backoff_spin_policy(__atime, __now)))
		  return true;
	      }
	    return false;
	  }

	// returns true if wait ended before timeout
	template<typename _Pred,
		 typename _Clock, typename _Dur>
	  bool
	  _M_do_wait_until(_Pred __pred,
			   const chrono::time_point<_Clock, _Dur>&
								__atime) noexcept
	  {
	    __platform_wait_t __val;
	    if (__base_type::_M_do_spin(__pred, __val,
					__timed_backoff_spin_policy(__atime)))
	      return true;
	    return _M_do_wait_until(__pred, __val, __atime);
	  }

	template<typename _Tp, typename _ValFn,
		 typename _Rep, typename _Period>
	  bool
	  _M_do_wait_for_v(_Tp __old, _ValFn __vfn,
			   const chrono::duration<_Rep, _Period>&
								__rtime) noexcept
	  {
	    __platform_wait_t __val;
	    if (_M_do_spin_v(__old, std::move(__vfn), __val))
	      return true;

	    if (!__rtime.count())
	      return false; // no rtime supplied, and spin did not acquire

	    auto __reltime = chrono::ceil<__wait_clock_t::duration>(__rtime);

	    return __base_type::_M_w._M_do_wait_until(
					  __base_type::_M_addr,
					  __val,
					  chrono::steady_clock::now() + __reltime);
	  }

	template<typename _Pred,
		 typename _Rep, typename _Period>
	  bool
	  _M_do_wait_for(_Pred __pred,
			 const chrono::duration<_Rep, _Period>& __rtime) noexcept
	  {
	    __platform_wait_t __val;
	    if (__base_type::_M_do_spin(__pred, __val))
	      return true;

	    if (!__rtime.count())
	      return false; // no rtime supplied, and spin did not acquire

	    auto __reltime = chrono::ceil<__wait_clock_t::duration>(__rtime);

	    return _M_do_wait_until(__pred, __val,
				    chrono::steady_clock::now() + __reltime);
	  }
      };

    using __enters_timed_wait = __timed_waiter<std::true_type>;
    using __bare_timed_wait = __timed_waiter<std::false_type>;
  } // namespace __detail

  // returns true if wait ended before timeout
  template<typename _Tp, typename _ValFn,
	   typename _Clock, typename _Dur>
    bool
    __atomic_wait_address_until_v(const _Tp* __addr, _Tp&& __old, _ValFn&& __vfn,
			const chrono::time_point<_Clock, _Dur>&
			    __atime) noexcept
    {
      __detail::__enters_timed_wait __w{__addr};
      return __w._M_do_wait_until_v(__old, __vfn, __atime);
    }

  template<typename _Tp, typename _Pred,
	   typename _Clock, typename _Dur>
    bool
    __atomic_wait_address_until(const _Tp* __addr, _Pred __pred,
				const chrono::time_point<_Clock, _Dur>&
							      __atime) noexcept
    {
      __detail::__enters_timed_wait __w{__addr};
      return __w._M_do_wait_until(__pred, __atime);
    }

  template<typename _Pred,
	   typename _Clock, typename _Dur>
    bool
    __atomic_wait_address_until_bare(const __detail::__platform_wait_t* __addr,
				_Pred __pred,
				const chrono::time_point<_Clock, _Dur>&
							      __atime) noexcept
    {
      __detail::__bare_timed_wait __w{__addr};
      return __w._M_do_wait_until(__pred, __atime);
    }

  template<typename _Tp, typename _ValFn,
	   typename _Rep, typename _Period>
    bool
    __atomic_wait_address_for_v(const _Tp* __addr, _Tp&& __old, _ValFn&& __vfn,
		      const chrono::duration<_Rep, _Period>& __rtime) noexcept
    {
      __detail::__enters_timed_wait __w{__addr};
      return __w._M_do_wait_for_v(__old, __vfn, __rtime);
    }

  template<typename _Tp, typename _Pred,
	   typename _Rep, typename _Period>
    bool
    __atomic_wait_address_for(const _Tp* __addr, _Pred __pred,
		      const chrono::duration<_Rep, _Period>& __rtime) noexcept
    {

      __detail::__enters_timed_wait __w{__addr};
      return __w._M_do_wait_for(__pred, __rtime);
    }

  template<typename _Pred,
	   typename _Rep, typename _Period>
    bool
    __atomic_wait_address_for_bare(const __detail::__platform_wait_t* __addr,
			_Pred __pred,
			const chrono::duration<_Rep, _Period>& __rtime) noexcept
    {
      __detail::__bare_timed_wait __w{__addr};
      return __w._M_do_wait_for(__pred, __rtime);
    }
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif // __cpp_lib_atomic_wait
#endif // _GLIBCXX_ATOMIC_TIMED_WAIT_H
