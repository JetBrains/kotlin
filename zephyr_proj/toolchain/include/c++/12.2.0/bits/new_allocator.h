// Allocator that wraps operator new -*- C++ -*-

// Copyright (C) 2001-2022 Free Software Foundation, Inc.
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

/** @file bits/new_allocator.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{memory}
 */

#ifndef _STD_NEW_ALLOCATOR_H
#define _STD_NEW_ALLOCATOR_H 1

#include <bits/c++config.h>
#include <new>
#include <bits/functexcept.h>
#include <bits/move.h>
#if __cplusplus >= 201103L
#include <type_traits>
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  /**
   *  @brief  An allocator that uses global new, as per C++03 [20.4.1].
   *  @ingroup allocators
   *
   *  This is precisely the allocator defined in the C++ Standard.
   *    - all allocation calls operator new
   *    - all deallocation calls operator delete
   *
   *  @tparam  _Tp  Type of allocated object.
   */
  template<typename _Tp>
    class __new_allocator
    {
    public:
      typedef _Tp        value_type;
      typedef std::size_t     size_type;
      typedef std::ptrdiff_t  difference_type;
#if __cplusplus <= 201703L
      typedef _Tp*       pointer;
      typedef const _Tp* const_pointer;
      typedef _Tp&       reference;
      typedef const _Tp& const_reference;

      template<typename _Tp1>
	struct rebind
	{ typedef __new_allocator<_Tp1> other; };
#endif

#if __cplusplus >= 201103L
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 2103. propagate_on_container_move_assignment
      typedef std::true_type propagate_on_container_move_assignment;
#endif

      _GLIBCXX20_CONSTEXPR
      __new_allocator() _GLIBCXX_USE_NOEXCEPT { }

      _GLIBCXX20_CONSTEXPR
      __new_allocator(const __new_allocator&) _GLIBCXX_USE_NOEXCEPT { }

      template<typename _Tp1>
	_GLIBCXX20_CONSTEXPR
	__new_allocator(const __new_allocator<_Tp1>&) _GLIBCXX_USE_NOEXCEPT { }

#if __cplusplus <= 201703L
      ~__new_allocator() _GLIBCXX_USE_NOEXCEPT { }

      pointer
      address(reference __x) const _GLIBCXX_NOEXCEPT
      { return std::__addressof(__x); }

      const_pointer
      address(const_reference __x) const _GLIBCXX_NOEXCEPT
      { return std::__addressof(__x); }
#endif

#if __has_builtin(__builtin_operator_new) >= 201802L
# define _GLIBCXX_OPERATOR_NEW __builtin_operator_new
# define _GLIBCXX_OPERATOR_DELETE __builtin_operator_delete
#else
# define _GLIBCXX_OPERATOR_NEW ::operator new
# define _GLIBCXX_OPERATOR_DELETE ::operator delete
#endif

      // NB: __n is permitted to be 0.  The C++ standard says nothing
      // about what the return value is when __n == 0.
      _GLIBCXX_NODISCARD _Tp*
      allocate(size_type __n, const void* = static_cast<const void*>(0))
      {
#if __cplusplus >= 201103L
	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 3308. std::allocator<void>().allocate(n)
	static_assert(sizeof(_Tp) != 0, "cannot allocate incomplete types");
#endif

	if (__builtin_expect(__n > this->_M_max_size(), false))
	  {
	    // _GLIBCXX_RESOLVE_LIB_DEFECTS
	    // 3190. allocator::allocate sometimes returns too little storage
	    if (__n > (std::size_t(-1) / sizeof(_Tp)))
	      std::__throw_bad_array_new_length();
	    std::__throw_bad_alloc();
	  }

#if __cpp_aligned_new
	if (alignof(_Tp) > __STDCPP_DEFAULT_NEW_ALIGNMENT__)
	  {
	    std::align_val_t __al = std::align_val_t(alignof(_Tp));
	    return static_cast<_Tp*>(_GLIBCXX_OPERATOR_NEW(__n * sizeof(_Tp),
							   __al));
	  }
#endif
	return static_cast<_Tp*>(_GLIBCXX_OPERATOR_NEW(__n * sizeof(_Tp)));
      }

      // __p is not permitted to be a null pointer.
      void
      deallocate(_Tp* __p, size_type __n __attribute__ ((__unused__)))
      {
#if __cpp_sized_deallocation
# define _GLIBCXX_SIZED_DEALLOC(p, n) (p), (n) * sizeof(_Tp)
#else
# define _GLIBCXX_SIZED_DEALLOC(p, n) (p)
#endif

#if __cpp_aligned_new
	if (alignof(_Tp) > __STDCPP_DEFAULT_NEW_ALIGNMENT__)
	  {
	    _GLIBCXX_OPERATOR_DELETE(_GLIBCXX_SIZED_DEALLOC(__p, __n),
				     std::align_val_t(alignof(_Tp)));
	    return;
	  }
#endif
	_GLIBCXX_OPERATOR_DELETE(_GLIBCXX_SIZED_DEALLOC(__p, __n));
      }

#undef _GLIBCXX_SIZED_DEALLOC
#undef _GLIBCXX_OPERATOR_DELETE
#undef _GLIBCXX_OPERATOR_NEW

#if __cplusplus <= 201703L
      size_type
      max_size() const _GLIBCXX_USE_NOEXCEPT
      { return _M_max_size(); }

#if __cplusplus >= 201103L
      template<typename _Up, typename... _Args>
	void
	construct(_Up* __p, _Args&&... __args)
	noexcept(std::is_nothrow_constructible<_Up, _Args...>::value)
	{ ::new((void *)__p) _Up(std::forward<_Args>(__args)...); }

      template<typename _Up>
	void
	destroy(_Up* __p)
	noexcept(std::is_nothrow_destructible<_Up>::value)
	{ __p->~_Up(); }
#else
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 402. wrong new expression in [some_] allocator::construct
      void
      construct(pointer __p, const _Tp& __val)
      { ::new((void *)__p) _Tp(__val); }

      void
      destroy(pointer __p) { __p->~_Tp(); }
#endif
#endif // ! C++20

      template<typename _Up>
	friend _GLIBCXX20_CONSTEXPR bool
	operator==(const __new_allocator&, const __new_allocator<_Up>&)
	_GLIBCXX_NOTHROW
	{ return true; }

#if __cpp_impl_three_way_comparison < 201907L
      template<typename _Up>
	friend _GLIBCXX20_CONSTEXPR bool
	operator!=(const __new_allocator&, const __new_allocator<_Up>&)
	_GLIBCXX_NOTHROW
	{ return false; }
#endif

    private:
      _GLIBCXX_CONSTEXPR size_type
      _M_max_size() const _GLIBCXX_USE_NOEXCEPT
      {
#if __PTRDIFF_MAX__ < __SIZE_MAX__
	return std::size_t(__PTRDIFF_MAX__) / sizeof(_Tp);
#else
	return std::size_t(-1) / sizeof(_Tp);
#endif
      }
    };

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif
