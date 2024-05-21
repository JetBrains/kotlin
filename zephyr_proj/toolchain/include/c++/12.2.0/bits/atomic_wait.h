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

/** @file bits/atomic_wait.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{atomic}
 */

#ifndef _GLIBCXX_ATOMIC_WAIT_H
#define _GLIBCXX_ATOMIC_WAIT_H 1

#pragma GCC system_header

#include <bits/c++config.h>
#if defined _GLIBCXX_HAS_GTHREADS || defined _GLIBCXX_HAVE_LINUX_FUTEX
#include <bits/functional_hash.h>
#include <bits/gthr.h>
#include <ext/numeric_traits.h>

#ifdef _GLIBCXX_HAVE_LINUX_FUTEX
# include <cerrno>
# include <climits>
# include <unistd.h>
# include <syscall.h>
# include <bits/functexcept.h>
#endif

# include <bits/std_mutex.h>  // std::mutex, std::__condvar

#define __cpp_lib_atomic_wait 201907L

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
  namespace __detail
  {
#ifdef _GLIBCXX_HAVE_LINUX_FUTEX
#define _GLIBCXX_HAVE_PLATFORM_WAIT 1
    using __platform_wait_t = int;
    static constexpr size_t __platform_wait_alignment = 4;
#else
// define _GLIBCX_HAVE_PLATFORM_WAIT and implement __platform_wait()
// and __platform_notify() if there is a more efficient primitive supported
// by the platform (e.g. __ulock_wait()/__ulock_wake()) which is better than
// a mutex/condvar based wait.
    using __platform_wait_t = uint64_t;
    static constexpr size_t __platform_wait_alignment
      = __alignof__(__platform_wait_t);
#endif
  } // namespace __detail

  template<typename _Tp>
    inline constexpr bool __platform_wait_uses_type
#ifdef _GLIBCXX_HAVE_PLATFORM_WAIT
      = is_scalar_v<_Tp>
	&& ((sizeof(_Tp) == sizeof(__detail::__platform_wait_t))
	&& (alignof(_Tp*) >= __detail::__platform_wait_alignment));
#else
      = false;
#endif

  namespace __detail
  {
#ifdef _GLIBCXX_HAVE_LINUX_FUTEX
    enum class __futex_wait_flags : int
    {
#ifdef _GLIBCXX_HAVE_LINUX_FUTEX_PRIVATE
      __private_flag = 128,
#else
      __private_flag = 0,
#endif
      __wait = 0,
      __wake = 1,
      __wait_bitset = 9,
      __wake_bitset = 10,
      __wait_private = __wait | __private_flag,
      __wake_private = __wake | __private_flag,
      __wait_bitset_private = __wait_bitset | __private_flag,
      __wake_bitset_private = __wake_bitset | __private_flag,
      __bitset_match_any = -1
    };

    template<typename _Tp>
      void
      __platform_wait(const _Tp* __addr, __platform_wait_t __val) noexcept
      {
	auto __e = syscall (SYS_futex, static_cast<const void*>(__addr),
			    static_cast<int>(__futex_wait_flags::__wait_private),
			    __val, nullptr);
	if (!__e || errno == EAGAIN)
	  return;
	if (errno != EINTR)
	  __throw_system_error(errno);
      }

    template<typename _Tp>
      void
      __platform_notify(const _Tp* __addr, bool __all) noexcept
      {
	syscall (SYS_futex, static_cast<const void*>(__addr),
		 static_cast<int>(__futex_wait_flags::__wake_private),
		 __all ? INT_MAX : 1);
      }
#endif

    inline void
    __thread_yield() noexcept
    {
#if defined _GLIBCXX_HAS_GTHREADS && defined _GLIBCXX_USE_SCHED_YIELD
     __gthread_yield();
#endif
    }

    inline void
    __thread_relax() noexcept
    {
#if defined __i386__ || defined __x86_64__
      __builtin_ia32_pause();
#else
      __thread_yield();
#endif
    }

    constexpr auto __atomic_spin_count_relax = 12;
    constexpr auto __atomic_spin_count = 16;

    struct __default_spin_policy
    {
      bool
      operator()() const noexcept
      { return false; }
    };

    template<typename _Pred,
	     typename _Spin = __default_spin_policy>
      bool
      __atomic_spin(_Pred& __pred, _Spin __spin = _Spin{ }) noexcept
      {
	for (auto __i = 0; __i < __atomic_spin_count; ++__i)
	  {
	    if (__pred())
	      return true;

	    if (__i < __atomic_spin_count_relax)
	      __detail::__thread_relax();
	    else
	      __detail::__thread_yield();
	  }

	while (__spin())
	  {
	    if (__pred())
	      return true;
	  }

	return false;
      }

    // return true if equal
    template<typename _Tp>
      bool __atomic_compare(const _Tp& __a, const _Tp& __b)
      {
	// TODO make this do the correct padding bit ignoring comparison
	return __builtin_memcmp(&__a, &__b, sizeof(_Tp)) == 0;
      }

    struct __waiter_pool_base
    {
      // Don't use std::hardware_destructive_interference_size here because we
      // don't want the layout of library types to depend on compiler options.
      static constexpr auto _S_align = 64;

      alignas(_S_align) __platform_wait_t _M_wait = 0;

#ifndef _GLIBCXX_HAVE_PLATFORM_WAIT
      mutex _M_mtx;
#endif

      alignas(_S_align) __platform_wait_t _M_ver = 0;

#ifndef _GLIBCXX_HAVE_PLATFORM_WAIT
      __condvar _M_cv;
#endif
      __waiter_pool_base() = default;

      void
      _M_enter_wait() noexcept
      { __atomic_fetch_add(&_M_wait, 1, __ATOMIC_SEQ_CST); }

      void
      _M_leave_wait() noexcept
      { __atomic_fetch_sub(&_M_wait, 1, __ATOMIC_RELEASE); }

      bool
      _M_waiting() const noexcept
      {
	__platform_wait_t __res;
	__atomic_load(&_M_wait, &__res, __ATOMIC_SEQ_CST);
	return __res != 0;
      }

      void
      _M_notify(const __platform_wait_t* __addr, bool __all, bool __bare) noexcept
      {
	if (!(__bare || _M_waiting()))
	  return;

#ifdef _GLIBCXX_HAVE_PLATFORM_WAIT
	__platform_notify(__addr, __all);
#else
	if (__all)
	  _M_cv.notify_all();
	else
	  _M_cv.notify_one();
#endif
      }

      static __waiter_pool_base&
      _S_for(const void* __addr) noexcept
      {
	constexpr uintptr_t __ct = 16;
	static __waiter_pool_base __w[__ct];
	auto __key = (uintptr_t(__addr) >> 2) % __ct;
	return __w[__key];
      }
    };

    struct __waiter_pool : __waiter_pool_base
    {
      void
      _M_do_wait(const __platform_wait_t* __addr, __platform_wait_t __old) noexcept
      {
#ifdef _GLIBCXX_HAVE_PLATFORM_WAIT
	__platform_wait(__addr, __old);
#else
	__platform_wait_t __val;
	__atomic_load(__addr, &__val, __ATOMIC_SEQ_CST);
	if (__val == __old)
	  {
	    lock_guard<mutex> __l(_M_mtx);
	    _M_cv.wait(_M_mtx);
	  }
#endif // __GLIBCXX_HAVE_PLATFORM_WAIT
      }
    };

    template<typename _Tp>
      struct __waiter_base
      {
	using __waiter_type = _Tp;

	__waiter_type& _M_w;
	__platform_wait_t* _M_addr;

	template<typename _Up>
	  static __platform_wait_t*
	  _S_wait_addr(const _Up* __a, __platform_wait_t* __b)
	  {
	    if constexpr (__platform_wait_uses_type<_Up>)
	      return reinterpret_cast<__platform_wait_t*>(const_cast<_Up*>(__a));
	    else
	      return __b;
	  }

	static __waiter_type&
	_S_for(const void* __addr) noexcept
	{
	  static_assert(sizeof(__waiter_type) == sizeof(__waiter_pool_base));
	  auto& res = __waiter_pool_base::_S_for(__addr);
	  return reinterpret_cast<__waiter_type&>(res);
	}

	template<typename _Up>
	  explicit __waiter_base(const _Up* __addr) noexcept
	    : _M_w(_S_for(__addr))
	    , _M_addr(_S_wait_addr(__addr, &_M_w._M_ver))
	  { }

	bool
	_M_laundered() const
	{ return _M_addr == &_M_w._M_ver; }

	void
	_M_notify(bool __all, bool __bare = false)
	{
	  if (_M_laundered())
	    {
	      __atomic_fetch_add(_M_addr, 1, __ATOMIC_SEQ_CST);
	      __all = true;
	    }
	  _M_w._M_notify(_M_addr, __all, __bare);
	}

	template<typename _Up, typename _ValFn,
		 typename _Spin = __default_spin_policy>
	  static bool
	  _S_do_spin_v(__platform_wait_t* __addr,
		       const _Up& __old, _ValFn __vfn,
		       __platform_wait_t& __val,
		       _Spin __spin = _Spin{ })
	  {
	    auto const __pred = [=]
	      { return !__detail::__atomic_compare(__old, __vfn()); };

	    if constexpr (__platform_wait_uses_type<_Up>)
	      {
		__builtin_memcpy(&__val, &__old, sizeof(__val));
	      }
	    else
	      {
		__atomic_load(__addr, &__val, __ATOMIC_ACQUIRE);
	      }
	    return __atomic_spin(__pred, __spin);
	  }

	template<typename _Up, typename _ValFn,
		 typename _Spin = __default_spin_policy>
	  bool
	  _M_do_spin_v(const _Up& __old, _ValFn __vfn,
		       __platform_wait_t& __val,
		       _Spin __spin = _Spin{ })
	  { return _S_do_spin_v(_M_addr, __old, __vfn, __val, __spin); }

	template<typename _Pred,
		 typename _Spin = __default_spin_policy>
	  static bool
	  _S_do_spin(const __platform_wait_t* __addr,
		     _Pred __pred,
		     __platform_wait_t& __val,
		     _Spin __spin = _Spin{ })
	  {
	    __atomic_load(__addr, &__val, __ATOMIC_ACQUIRE);
	    return __atomic_spin(__pred, __spin);
	  }

	template<typename _Pred,
		 typename _Spin = __default_spin_policy>
	  bool
	  _M_do_spin(_Pred __pred, __platform_wait_t& __val,
		     _Spin __spin = _Spin{ })
	  { return _S_do_spin(_M_addr, __pred, __val, __spin); }
      };

    template<typename _EntersWait>
      struct __waiter : __waiter_base<__waiter_pool>
      {
	using __base_type = __waiter_base<__waiter_pool>;

	template<typename _Tp>
	  explicit __waiter(const _Tp* __addr) noexcept
	    : __base_type(__addr)
	  {
	    if constexpr (_EntersWait::value)
	      _M_w._M_enter_wait();
	  }

	~__waiter()
	{
	  if constexpr (_EntersWait::value)
	    _M_w._M_leave_wait();
	}

	template<typename _Tp, typename _ValFn>
	  void
	  _M_do_wait_v(_Tp __old, _ValFn __vfn)
	  {
	    do
	      {
		__platform_wait_t __val;
		if (__base_type::_M_do_spin_v(__old, __vfn, __val))
		  return;
		__base_type::_M_w._M_do_wait(__base_type::_M_addr, __val);
	      }
	    while (__detail::__atomic_compare(__old, __vfn()));
	  }

	template<typename _Pred>
	  void
	  _M_do_wait(_Pred __pred) noexcept
	  {
	    do
	      {
		__platform_wait_t __val;
		if (__base_type::_M_do_spin(__pred, __val))
		  return;
		__base_type::_M_w._M_do_wait(__base_type::_M_addr, __val);
	      }
	    while (!__pred());
	  }
      };

    using __enters_wait = __waiter<std::true_type>;
    using __bare_wait = __waiter<std::false_type>;
  } // namespace __detail

  template<typename _Tp, typename _ValFn>
    void
    __atomic_wait_address_v(const _Tp* __addr, _Tp __old,
			    _ValFn __vfn) noexcept
    {
      __detail::__enters_wait __w(__addr);
      __w._M_do_wait_v(__old, __vfn);
    }

  template<typename _Tp, typename _Pred>
    void
    __atomic_wait_address(const _Tp* __addr, _Pred __pred) noexcept
    {
      __detail::__enters_wait __w(__addr);
      __w._M_do_wait(__pred);
    }

  // This call is to be used by atomic types which track contention externally
  template<typename _Pred>
    void
    __atomic_wait_address_bare(const __detail::__platform_wait_t* __addr,
			       _Pred __pred) noexcept
    {
#ifdef _GLIBCXX_HAVE_PLATFORM_WAIT
      do
	{
	  __detail::__platform_wait_t __val;
	  if (__detail::__bare_wait::_S_do_spin(__addr, __pred, __val))
	    return;
	  __detail::__platform_wait(__addr, __val);
	}
      while (!__pred());
#else // !_GLIBCXX_HAVE_PLATFORM_WAIT
      __detail::__bare_wait __w(__addr);
      __w._M_do_wait(__pred);
#endif
    }

  template<typename _Tp>
    void
    __atomic_notify_address(const _Tp* __addr, bool __all) noexcept
    {
      __detail::__bare_wait __w(__addr);
      __w._M_notify(__all);
    }

  // This call is to be used by atomic types which track contention externally
  inline void
  __atomic_notify_address_bare(const __detail::__platform_wait_t* __addr,
			       bool __all) noexcept
  {
#ifdef _GLIBCXX_HAVE_PLATFORM_WAIT
    __detail::__platform_notify(__addr, __all);
#else
    __detail::__bare_wait __w(__addr);
    __w._M_notify(__all, true);
#endif
  }
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif // GTHREADS || LINUX_FUTEX
#endif // _GLIBCXX_ATOMIC_WAIT_H
