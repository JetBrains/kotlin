// Implementation of std::move_only_function -*- C++ -*-

// Copyright The GNU Toolchain Authors.
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

/** @file include/bits/move_only_function.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{functional}
 */

#ifndef _GLIBCXX_MOVE_ONLY_FUNCTION_H
#define _GLIBCXX_MOVE_ONLY_FUNCTION_H 1

#pragma GCC system_header

#if __cplusplus > 202002L

#include <bits/invoke.h>
#include <bits/utility.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#define __cpp_lib_move_only_function 202110L

  template<typename... _Signature>
    class move_only_function; // not defined

  /// @cond undocumented
  class _Mofunc_base
  {
  protected:
    _Mofunc_base() noexcept
    : _M_manage(_S_empty)
    { }

    _Mofunc_base(_Mofunc_base&& __x) noexcept
    {
      _M_manage = std::__exchange(__x._M_manage, _S_empty);
      _M_manage(_M_storage, &__x._M_storage);
    }

    template<typename _Tp, typename... _Args>
      static constexpr bool
      _S_nothrow_init() noexcept
      {
	if constexpr (__stored_locally<_Tp>)
	  return is_nothrow_constructible_v<_Tp, _Args...>;
	return false;
      }

    template<typename _Tp, typename... _Args>
      void
      _M_init(_Args&&... __args) noexcept(_S_nothrow_init<_Tp, _Args...>())
      {
	if constexpr (__stored_locally<_Tp>)
	  ::new (_M_storage._M_addr()) _Tp(std::forward<_Args>(__args)...);
	else
	  _M_storage._M_p = new _Tp(std::forward<_Args>(__args)...);

	_M_manage = &_S_manage<_Tp>;
      }

    _Mofunc_base&
    operator=(_Mofunc_base&& __x) noexcept
    {
      _M_manage(_M_storage, nullptr);
      _M_manage = std::__exchange(__x._M_manage, _S_empty);
      _M_manage(_M_storage, &__x._M_storage);
      return *this;
    }

    _Mofunc_base&
    operator=(nullptr_t) noexcept
    {
      _M_manage(_M_storage, nullptr);
      _M_manage = _S_empty;
      return *this;
    }

    ~_Mofunc_base() { _M_manage(_M_storage, nullptr); }

    void
    swap(_Mofunc_base& __x) noexcept
    {
      // Order of operations here is more efficient if __x is empty.
      _Storage __s;
      __x._M_manage(__s, &__x._M_storage);
      _M_manage(__x._M_storage, &_M_storage);
      __x._M_manage(_M_storage, &__s);
      std::swap(_M_manage, __x._M_manage);
    }

    template<typename _Tp, typename _Self>
      static _Tp*
      _S_access(_Self* __self) noexcept
      {
	if constexpr (__stored_locally<remove_const_t<_Tp>>)
	  return static_cast<_Tp*>(__self->_M_storage._M_addr());
	else
	  return static_cast<_Tp*>(__self->_M_storage._M_p);
      }

  private:
    struct _Storage
    {
      void*       _M_addr() noexcept       { return &_M_bytes[0]; }
      const void* _M_addr() const noexcept { return &_M_bytes[0]; }

      // We want to have enough space to store a simple delegate type.
      struct _Delegate { void (_Storage::*__pfm)(); _Storage* __obj; };
      union {
	void* _M_p;
	alignas(_Delegate) alignas(void(*)())
	  unsigned char _M_bytes[sizeof(_Delegate)];
      };
    };

    template<typename _Tp>
      static constexpr bool __stored_locally
	= sizeof(_Tp) <= sizeof(_Storage) && alignof(_Tp) <= alignof(_Storage)
	    && is_nothrow_move_constructible_v<_Tp>;

    // A function that either destroys the target object stored in __target,
    // or moves the target object from *__src to __target.
    using _Manager = void (*)(_Storage& __target, _Storage* __src) noexcept;

    // The no-op manager function for objects with no target.
    static void _S_empty(_Storage&, _Storage*) noexcept { }

    // The real manager function for a target object of type _Tp.
    template<typename _Tp>
      static void
      _S_manage(_Storage& __target, _Storage* __src) noexcept
      {
	if constexpr (__stored_locally<_Tp>)
	  {
	    if (__src)
	      {
		_Tp* __rval = static_cast<_Tp*>(__src->_M_addr());
		::new (__target._M_addr()) _Tp(std::move(*__rval));
		__rval->~_Tp();
	      }
	    else
	      static_cast<_Tp*>(__target._M_addr())->~_Tp();
	  }
	else
	  {
	    if (__src)
	      __target._M_p = __src->_M_p;
	    else
	      delete static_cast<_Tp*>(__target._M_p);
	  }
      }

    _Storage _M_storage;
    _Manager _M_manage;
  };

  template<typename _Tp>
    inline constexpr bool __is_move_only_function_v = false;
  template<typename _Tp>
    constexpr bool __is_move_only_function_v<move_only_function<_Tp>> = true;
  /// @endcond

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#include "mofunc_impl.h"
#define _GLIBCXX_MOF_CV const
#include "mofunc_impl.h"
#define _GLIBCXX_MOF_REF &
#include "mofunc_impl.h"
#define _GLIBCXX_MOF_REF &&
#include "mofunc_impl.h"
#define _GLIBCXX_MOF_CV const
#define _GLIBCXX_MOF_REF &
#include "mofunc_impl.h"
#define _GLIBCXX_MOF_CV const
#define _GLIBCXX_MOF_REF &&
#include "mofunc_impl.h"

#endif // C++23
#endif // _GLIBCXX_MOVE_ONLY_FUNCTION_H
