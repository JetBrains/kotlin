// std::thread declarations -*- C++ -*-

// Copyright (C) 2008-2022 Free Software Foundation, Inc.
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

/** @file bits/std_thread.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{thread}
 */

#ifndef _GLIBCXX_THREAD_H
#define _GLIBCXX_THREAD_H 1

#pragma GCC system_header

#if __cplusplus >= 201103L
#include <bits/c++config.h>

#include <iosfwd>		// std::basic_ostream
#include <tuple>		// std::tuple
#include <bits/functional_hash.h> // std::hash
#include <bits/invoke.h>	// std::__invoke
#include <bits/refwrap.h>       // not required, but helpful to users
#include <bits/unique_ptr.h>	// std::unique_ptr

#ifdef _GLIBCXX_HAS_GTHREADS
# include <bits/gthr.h>
#else
# include <errno.h>
# include <bits/functexcept.h>
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  /** @addtogroup threads
   *  @{
   */

  /// thread
  class thread
  {
  public:
#ifdef _GLIBCXX_HAS_GTHREADS
    // Abstract base class for types that wrap arbitrary functors to be
    // invoked in the new thread of execution.
    struct _State
    {
      virtual ~_State();
      virtual void _M_run() = 0;
    };
    using _State_ptr = unique_ptr<_State>;

    using native_handle_type = __gthread_t;
#else
    using native_handle_type = int;
#endif

    /// thread::id
    class id
    {
      native_handle_type	_M_thread;

    public:
      id() noexcept : _M_thread() { }

      explicit
      id(native_handle_type __id) : _M_thread(__id) { }

    private:
      friend class thread;
      friend struct hash<id>;

      friend bool
      operator==(id __x, id __y) noexcept;

#if __cpp_lib_three_way_comparison
      friend strong_ordering
      operator<=>(id __x, id __y) noexcept;
#else
      friend bool
      operator<(id __x, id __y) noexcept;
#endif

      template<class _CharT, class _Traits>
	friend basic_ostream<_CharT, _Traits>&
	operator<<(basic_ostream<_CharT, _Traits>& __out, id __id);
    };

  private:
    id				_M_id;

    // _GLIBCXX_RESOLVE_LIB_DEFECTS
    // 2097.  packaged_task constructors should be constrained
    // 3039. Unnecessary decay in thread and packaged_task
    template<typename _Tp>
      using __not_same = __not_<is_same<__remove_cvref_t<_Tp>, thread>>;

  public:
    thread() noexcept = default;

#ifdef _GLIBCXX_HAS_GTHREADS
    template<typename _Callable, typename... _Args,
	     typename = _Require<__not_same<_Callable>>>
      explicit
      thread(_Callable&& __f, _Args&&... __args)
      {
	static_assert( __is_invocable<typename decay<_Callable>::type,
				      typename decay<_Args>::type...>::value,
	  "std::thread arguments must be invocable after conversion to rvalues"
	  );

#ifdef GTHR_ACTIVE_PROXY
	// Create a reference to pthread_create, not just the gthr weak symbol.
	auto __depend = reinterpret_cast<void(*)()>(&pthread_create);
#else
	auto __depend = nullptr;
#endif
	using _Wrapper = _Call_wrapper<_Callable, _Args...>;
	// Create a call wrapper with DECAY_COPY(__f) as its target object
	// and DECAY_COPY(__args)... as its bound argument entities.
	_M_start_thread(_State_ptr(new _State_impl<_Wrapper>(
	      std::forward<_Callable>(__f), std::forward<_Args>(__args)...)),
	    __depend);
      }
#endif // _GLIBCXX_HAS_GTHREADS

    ~thread()
    {
      if (joinable())
	std::__terminate();
    }

    thread(const thread&) = delete;

    thread(thread&& __t) noexcept
    { swap(__t); }

    thread& operator=(const thread&) = delete;

    thread& operator=(thread&& __t) noexcept
    {
      if (joinable())
	std::__terminate();
      swap(__t);
      return *this;
    }

    void
    swap(thread& __t) noexcept
    { std::swap(_M_id, __t._M_id); }

    bool
    joinable() const noexcept
    { return !(_M_id == id()); }

    void
    join();

    void
    detach();

    id
    get_id() const noexcept
    { return _M_id; }

    /** @pre thread is joinable
     */
    native_handle_type
    native_handle()
    { return _M_id._M_thread; }

    // Returns a value that hints at the number of hardware thread contexts.
    static unsigned int
    hardware_concurrency() noexcept;

#ifdef _GLIBCXX_HAS_GTHREADS
  private:
    template<typename _Callable>
      struct _State_impl : public _State
      {
	_Callable		_M_func;

	template<typename... _Args>
	  _State_impl(_Args&&... __args)
	  : _M_func{{std::forward<_Args>(__args)...}}
	  { }

	void
	_M_run() { _M_func(); }
      };

    void
    _M_start_thread(_State_ptr, void (*)());

#if _GLIBCXX_THREAD_ABI_COMPAT
  public:
    struct _Impl_base;
    typedef shared_ptr<_Impl_base>	__shared_base_type;
    struct _Impl_base
    {
      __shared_base_type	_M_this_ptr;
      virtual ~_Impl_base() = default;
      virtual void _M_run() = 0;
    };

  private:
    void
    _M_start_thread(__shared_base_type, void (*)());

    void
    _M_start_thread(__shared_base_type);
#endif

  private:
    // A call wrapper that does INVOKE(forwarded tuple elements...)
    template<typename _Tuple>
      struct _Invoker
      {
	_Tuple _M_t;

	template<typename>
	  struct __result;
	template<typename _Fn, typename... _Args>
	  struct __result<tuple<_Fn, _Args...>>
	  : __invoke_result<_Fn, _Args...>
	  { };

	template<size_t... _Ind>
	  typename __result<_Tuple>::type
	  _M_invoke(_Index_tuple<_Ind...>)
	  { return std::__invoke(std::get<_Ind>(std::move(_M_t))...); }

	typename __result<_Tuple>::type
	operator()()
	{
	  using _Indices
	    = typename _Build_index_tuple<tuple_size<_Tuple>::value>::__type;
	  return _M_invoke(_Indices());
	}
      };

  public:
    template<typename... _Tp>
      using _Call_wrapper = _Invoker<tuple<typename decay<_Tp>::type...>>;
#endif // _GLIBCXX_HAS_GTHREADS
  };

#ifndef _GLIBCXX_HAS_GTHREADS
  inline void thread::join() { std::__throw_system_error(EINVAL); }
  inline void thread::detach() { std::__throw_system_error(EINVAL); }
  inline unsigned int thread::hardware_concurrency() noexcept { return 0; }
#endif

  inline void
  swap(thread& __x, thread& __y) noexcept
  { __x.swap(__y); }

  inline bool
  operator==(thread::id __x, thread::id __y) noexcept
  {
    // pthread_equal is undefined if either thread ID is not valid, so we
    // can't safely use __gthread_equal on default-constructed values (nor
    // the non-zero value returned by this_thread::get_id() for
    // single-threaded programs using GNU libc). Assume EqualityComparable.
    return __x._M_thread == __y._M_thread;
  }

  // N.B. other comparison operators are defined in <thread>

  // DR 889.
  /// std::hash specialization for thread::id.
  template<>
    struct hash<thread::id>
    : public __hash_base<size_t, thread::id>
    {
      size_t
      operator()(const thread::id& __id) const noexcept
      { return std::_Hash_impl::hash(__id._M_thread); }
    };

  namespace this_thread
  {
    /// this_thread::get_id
    inline thread::id
    get_id() noexcept
    {
#ifndef _GLIBCXX_HAS_GTHREADS
      return thread::id(1);
#elif defined _GLIBCXX_NATIVE_THREAD_ID
      return thread::id(_GLIBCXX_NATIVE_THREAD_ID);
#else
      return thread::id(__gthread_self());
#endif
    }

    /// this_thread::yield
    inline void
    yield() noexcept
    {
#if defined _GLIBCXX_HAS_GTHREADS && defined _GLIBCXX_USE_SCHED_YIELD
      __gthread_yield();
#endif
    }

  } // namespace this_thread

  /// @}

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace
#endif // C++11

#endif // _GLIBCXX_THREAD_H
