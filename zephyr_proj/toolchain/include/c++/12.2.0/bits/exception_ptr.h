// Exception Handling support header (exception_ptr class) for -*- C++ -*-

// Copyright (C) 2008-2022 Free Software Foundation, Inc.
//
// This file is part of GCC.
//
// GCC is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 3, or (at your option)
// any later version.
// 
// GCC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

/** @file bits/exception_ptr.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{exception}
 */

#ifndef _EXCEPTION_PTR_H
#define _EXCEPTION_PTR_H

#pragma GCC visibility push(default)

#include <bits/c++config.h>
#include <bits/exception_defines.h>
#include <bits/cxxabi_init_exception.h>
#include <typeinfo>
#include <new>

#if __cplusplus >= 201103L
# include <bits/move.h>
#endif

#ifdef _GLIBCXX_EH_PTR_RELOPS_COMPAT
# define _GLIBCXX_EH_PTR_USED __attribute__((__used__))
#else
# define _GLIBCXX_EH_PTR_USED
#endif

extern "C++" {

namespace std 
{
  class type_info;

  /**
   * @addtogroup exceptions
   * @{
   */

  namespace __exception_ptr
  {
    class exception_ptr;
  }

  using __exception_ptr::exception_ptr;

  /** Obtain an exception_ptr to the currently handled exception. If there
   *  is none, or the currently handled exception is foreign, return the null
   *  value.
   */
  exception_ptr current_exception() _GLIBCXX_USE_NOEXCEPT;

  template<typename _Ex>
  exception_ptr make_exception_ptr(_Ex) _GLIBCXX_USE_NOEXCEPT;

  /// Throw the object pointed to by the exception_ptr.
  void rethrow_exception(exception_ptr) __attribute__ ((__noreturn__));

  namespace __exception_ptr
  {
    using std::rethrow_exception;

    /**
     *  @brief An opaque pointer to an arbitrary exception.
     *  @ingroup exceptions
     */
    class exception_ptr
    {
      void* _M_exception_object;

      explicit exception_ptr(void* __e) _GLIBCXX_USE_NOEXCEPT;

      void _M_addref() _GLIBCXX_USE_NOEXCEPT;
      void _M_release() _GLIBCXX_USE_NOEXCEPT;

      void *_M_get() const _GLIBCXX_NOEXCEPT __attribute__ ((__pure__));

      friend exception_ptr std::current_exception() _GLIBCXX_USE_NOEXCEPT;
      friend void std::rethrow_exception(exception_ptr);
      template<typename _Ex>
      friend exception_ptr std::make_exception_ptr(_Ex) _GLIBCXX_USE_NOEXCEPT;

    public:
      exception_ptr() _GLIBCXX_USE_NOEXCEPT;

      exception_ptr(const exception_ptr&) _GLIBCXX_USE_NOEXCEPT;

#if __cplusplus >= 201103L
      exception_ptr(nullptr_t) noexcept
      : _M_exception_object(nullptr)
      { }

      exception_ptr(exception_ptr&& __o) noexcept
      : _M_exception_object(__o._M_exception_object)
      { __o._M_exception_object = nullptr; }
#endif

#if (__cplusplus < 201103L) || defined (_GLIBCXX_EH_PTR_COMPAT)
      typedef void (exception_ptr::*__safe_bool)();

      // For construction from nullptr or 0.
      exception_ptr(__safe_bool) _GLIBCXX_USE_NOEXCEPT;
#endif

      exception_ptr& 
      operator=(const exception_ptr&) _GLIBCXX_USE_NOEXCEPT;

#if __cplusplus >= 201103L
      exception_ptr& 
      operator=(exception_ptr&& __o) noexcept
      {
        exception_ptr(static_cast<exception_ptr&&>(__o)).swap(*this);
        return *this;
      }
#endif

      ~exception_ptr() _GLIBCXX_USE_NOEXCEPT;

      void 
      swap(exception_ptr&) _GLIBCXX_USE_NOEXCEPT;

#ifdef _GLIBCXX_EH_PTR_COMPAT
      // Retained for compatibility with CXXABI_1.3.
      void _M_safe_bool_dummy() _GLIBCXX_USE_NOEXCEPT
	__attribute__ ((__const__));
      bool operator!() const _GLIBCXX_USE_NOEXCEPT
	__attribute__ ((__pure__));
      operator __safe_bool() const _GLIBCXX_USE_NOEXCEPT;
#endif

#if __cplusplus >= 201103L
      explicit operator bool() const noexcept
      { return _M_exception_object; }
#endif

#if __cpp_impl_three_way_comparison >= 201907L \
      && ! defined _GLIBCXX_EH_PTR_RELOPS_COMPAT
      friend bool
      operator==(const exception_ptr&, const exception_ptr&) noexcept = default;
#else
      friend _GLIBCXX_EH_PTR_USED bool
      operator==(const exception_ptr& __x, const exception_ptr& __y)
      _GLIBCXX_USE_NOEXCEPT
      { return __x._M_exception_object == __y._M_exception_object; }

      friend _GLIBCXX_EH_PTR_USED bool
      operator!=(const exception_ptr& __x, const exception_ptr& __y)
      _GLIBCXX_USE_NOEXCEPT
      { return __x._M_exception_object != __y._M_exception_object; }
#endif

      const class std::type_info*
      __cxa_exception_type() const _GLIBCXX_USE_NOEXCEPT
	__attribute__ ((__pure__));
    };

    _GLIBCXX_EH_PTR_USED
    inline
    exception_ptr::exception_ptr() _GLIBCXX_USE_NOEXCEPT
    : _M_exception_object(0)
    { }

    _GLIBCXX_EH_PTR_USED
    inline
    exception_ptr::exception_ptr(const exception_ptr& __other)
    _GLIBCXX_USE_NOEXCEPT
    : _M_exception_object(__other._M_exception_object)
    {
      if (_M_exception_object)
	_M_addref();
    }

    _GLIBCXX_EH_PTR_USED
    inline
    exception_ptr::~exception_ptr() _GLIBCXX_USE_NOEXCEPT
    {
      if (_M_exception_object)
	_M_release();
    }

    _GLIBCXX_EH_PTR_USED
    inline exception_ptr&
    exception_ptr::operator=(const exception_ptr& __other) _GLIBCXX_USE_NOEXCEPT
    {
      exception_ptr(__other).swap(*this);
      return *this;
    }

    _GLIBCXX_EH_PTR_USED
    inline void
    exception_ptr::swap(exception_ptr &__other) _GLIBCXX_USE_NOEXCEPT
    {
      void *__tmp = _M_exception_object;
      _M_exception_object = __other._M_exception_object;
      __other._M_exception_object = __tmp;
    }

    /// @relates exception_ptr
    inline void
    swap(exception_ptr& __lhs, exception_ptr& __rhs)
    { __lhs.swap(__rhs); }

    /// @cond undocumented
    template<typename _Ex>
      _GLIBCXX_CDTOR_CALLABI
      inline void
      __dest_thunk(void* __x)
      { static_cast<_Ex*>(__x)->~_Ex(); }
    /// @endcond

  } // namespace __exception_ptr

  /// Obtain an exception_ptr pointing to a copy of the supplied object.
#if (__cplusplus >= 201103L && __cpp_rtti) || __cpp_exceptions
  template<typename _Ex>
    exception_ptr
    make_exception_ptr(_Ex __ex) _GLIBCXX_USE_NOEXCEPT
    {
#if __cplusplus >= 201103L && __cpp_rtti
      using _Ex2 = typename decay<_Ex>::type;
      void* __e = __cxxabiv1::__cxa_allocate_exception(sizeof(_Ex));
      (void) __cxxabiv1::__cxa_init_primary_exception(
	  __e, const_cast<std::type_info*>(&typeid(_Ex)),
	  __exception_ptr::__dest_thunk<_Ex2>);
      __try
	{
	  ::new (__e) _Ex2(__ex);
	  return exception_ptr(__e);
	}
      __catch(...)
	{
	  __cxxabiv1::__cxa_free_exception(__e);
	  return current_exception();
	}
#else
      try
	{
          throw __ex;
	}
      catch(...)
	{
	  return current_exception();
	}
#endif
    }
#else // no RTTI and no exceptions
  // This is always_inline so the linker will never use this useless definition
  // instead of a working one compiled with RTTI and/or exceptions enabled.
  template<typename _Ex>
    __attribute__ ((__always_inline__))
    exception_ptr
    make_exception_ptr(_Ex) _GLIBCXX_USE_NOEXCEPT
    { return exception_ptr(); }
#endif

#undef _GLIBCXX_EH_PTR_USED

  /// @} group exceptions
} // namespace std

} // extern "C++"

#pragma GCC visibility pop

#endif
