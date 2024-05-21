// Raw memory manipulators -*- C++ -*-

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

/*
 *
 * Copyright (c) 1994
 * Hewlett-Packard Company
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.  Hewlett-Packard Company makes no
 * representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 *
 *
 * Copyright (c) 1996,1997
 * Silicon Graphics Computer Systems, Inc.
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.  Silicon Graphics makes no
 * representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 */

/** @file bits/stl_uninitialized.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{memory}
 */

#ifndef _STL_UNINITIALIZED_H
#define _STL_UNINITIALIZED_H 1

#if __cplusplus >= 201103L
#include <type_traits>
#endif

#include <bits/stl_algobase.h>    // copy
#include <ext/alloc_traits.h>     // __alloc_traits

#if __cplusplus >= 201703L
#include <bits/stl_pair.h>
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  /** @addtogroup memory
   *  @{
   */

  /// @cond undocumented

#if __cplusplus >= 201103L
  template<typename _ValueType, typename _Tp>
    constexpr bool
    __check_constructible()
    {
      // Trivial types can have deleted constructors, but std::copy etc.
      // only use assignment (or memmove) not construction, so we need an
      // explicit check that construction from _Tp is actually valid,
      // otherwise some ill-formed uses of std::uninitialized_xxx would
      // compile without errors. This gives a nice clear error message.
      static_assert(is_constructible<_ValueType, _Tp>::value,
	  "result type must be constructible from input type");

      return true;
    }

// If the type is trivial we don't need to construct it, just assign to it.
// But trivial types can still have deleted or inaccessible assignment,
// so don't try to use std::copy or std::fill etc. if we can't assign.
# define _GLIBCXX_USE_ASSIGN_FOR_INIT(T, U) \
    __is_trivial(T) && __is_assignable(T&, U) \
    && std::__check_constructible<T, U>()
#else
// No need to check if is_constructible<T, U> for C++98. Trivial types have
// no user-declared constructors, so if the assignment is valid, construction
// should be too.
# define _GLIBCXX_USE_ASSIGN_FOR_INIT(T, U) \
    __is_trivial(T) && __is_assignable(T&, U)
#endif

  template<typename _InputIterator, typename _ForwardIterator>
    _GLIBCXX20_CONSTEXPR
    _ForwardIterator
    __do_uninit_copy(_InputIterator __first, _InputIterator __last,
		     _ForwardIterator __result)
    {
      _ForwardIterator __cur = __result;
      __try
	{
	  for (; __first != __last; ++__first, (void)++__cur)
	    std::_Construct(std::__addressof(*__cur), *__first);
	  return __cur;
	}
      __catch(...)
	{
	  std::_Destroy(__result, __cur);
	  __throw_exception_again;
	}
    }

  template<bool _TrivialValueTypes>
    struct __uninitialized_copy
    {
      template<typename _InputIterator, typename _ForwardIterator>
        static _ForwardIterator
        __uninit_copy(_InputIterator __first, _InputIterator __last,
		      _ForwardIterator __result)
	{ return std::__do_uninit_copy(__first, __last, __result); }
    };

  template<>
    struct __uninitialized_copy<true>
    {
      template<typename _InputIterator, typename _ForwardIterator>
        static _ForwardIterator
        __uninit_copy(_InputIterator __first, _InputIterator __last,
		      _ForwardIterator __result)
        { return std::copy(__first, __last, __result); }
    };

  /// @endcond

  /**
   *  @brief Copies the range [first,last) into result.
   *  @param  __first  An input iterator.
   *  @param  __last   An input iterator.
   *  @param  __result An output iterator.
   *  @return   __result + (__first - __last)
   *
   *  Like copy(), but does not require an initialized output range.
  */
  template<typename _InputIterator, typename _ForwardIterator>
    inline _ForwardIterator
    uninitialized_copy(_InputIterator __first, _InputIterator __last,
		       _ForwardIterator __result)
    {
      typedef typename iterator_traits<_InputIterator>::value_type
	_ValueType1;
      typedef typename iterator_traits<_ForwardIterator>::value_type
	_ValueType2;

      // _ValueType1 must be trivially-copyable to use memmove, so don't
      // bother optimizing to std::copy if it isn't.
      // XXX Unnecessary because std::copy would check it anyway?
      const bool __can_memmove = __is_trivial(_ValueType1);

#if __cplusplus < 201103L
      typedef typename iterator_traits<_InputIterator>::reference _From;
#else
      using _From = decltype(*__first);
#endif
      const bool __assignable
	= _GLIBCXX_USE_ASSIGN_FOR_INIT(_ValueType2, _From);

      return std::__uninitialized_copy<__can_memmove && __assignable>::
	__uninit_copy(__first, __last, __result);
    }

  /// @cond undocumented

  template<typename _ForwardIterator, typename _Tp>
    _GLIBCXX20_CONSTEXPR void
    __do_uninit_fill(_ForwardIterator __first, _ForwardIterator __last,
		     const _Tp& __x)
    {
      _ForwardIterator __cur = __first;
      __try
	{
	  for (; __cur != __last; ++__cur)
	    std::_Construct(std::__addressof(*__cur), __x);
	}
      __catch(...)
	{
	  std::_Destroy(__first, __cur);
	  __throw_exception_again;
	}
    }

  template<bool _TrivialValueType>
    struct __uninitialized_fill
    {
      template<typename _ForwardIterator, typename _Tp>
        static void
        __uninit_fill(_ForwardIterator __first, _ForwardIterator __last,
		      const _Tp& __x)
	{ std::__do_uninit_fill(__first, __last, __x); }
    };

  template<>
    struct __uninitialized_fill<true>
    {
      template<typename _ForwardIterator, typename _Tp>
        static void
        __uninit_fill(_ForwardIterator __first, _ForwardIterator __last,
		      const _Tp& __x)
        { std::fill(__first, __last, __x); }
    };

  /// @endcond

  /**
   *  @brief Copies the value x into the range [first,last).
   *  @param  __first  An input iterator.
   *  @param  __last   An input iterator.
   *  @param  __x      The source value.
   *  @return   Nothing.
   *
   *  Like fill(), but does not require an initialized output range.
  */
  template<typename _ForwardIterator, typename _Tp>
    inline void
    uninitialized_fill(_ForwardIterator __first, _ForwardIterator __last,
		       const _Tp& __x)
    {
      typedef typename iterator_traits<_ForwardIterator>::value_type
	_ValueType;

      // Trivial types do not need a constructor to begin their lifetime,
      // so try to use std::fill to benefit from its memset optimization.
      const bool __can_fill
	= _GLIBCXX_USE_ASSIGN_FOR_INIT(_ValueType, const _Tp&);

      std::__uninitialized_fill<__can_fill>::
	__uninit_fill(__first, __last, __x);
    }

  /// @cond undocumented

  template<typename _ForwardIterator, typename _Size, typename _Tp>
    _GLIBCXX20_CONSTEXPR
    _ForwardIterator
    __do_uninit_fill_n(_ForwardIterator __first, _Size __n, const _Tp& __x)
    {
      _ForwardIterator __cur = __first;
      __try
	{
	  for (; __n > 0; --__n, (void) ++__cur)
	    std::_Construct(std::__addressof(*__cur), __x);
	  return __cur;
	}
      __catch(...)
	{
	  std::_Destroy(__first, __cur);
	  __throw_exception_again;
	}
    }

  template<bool _TrivialValueType>
    struct __uninitialized_fill_n
    {
      template<typename _ForwardIterator, typename _Size, typename _Tp>
	static _ForwardIterator
        __uninit_fill_n(_ForwardIterator __first, _Size __n,
			const _Tp& __x)
	{ return std::__do_uninit_fill_n(__first, __n, __x); }
    };

  template<>
    struct __uninitialized_fill_n<true>
    {
      template<typename _ForwardIterator, typename _Size, typename _Tp>
	static _ForwardIterator
        __uninit_fill_n(_ForwardIterator __first, _Size __n,
			const _Tp& __x)
        { return std::fill_n(__first, __n, __x); }
    };

  /// @endcond

   // _GLIBCXX_RESOLVE_LIB_DEFECTS
   // DR 1339. uninitialized_fill_n should return the end of its range
  /**
   *  @brief Copies the value x into the range [first,first+n).
   *  @param  __first  An input iterator.
   *  @param  __n      The number of copies to make.
   *  @param  __x      The source value.
   *  @return   Nothing.
   *
   *  Like fill_n(), but does not require an initialized output range.
  */
  template<typename _ForwardIterator, typename _Size, typename _Tp>
    inline _ForwardIterator
    uninitialized_fill_n(_ForwardIterator __first, _Size __n, const _Tp& __x)
    {
      typedef typename iterator_traits<_ForwardIterator>::value_type
	_ValueType;

      // Trivial types do not need a constructor to begin their lifetime,
      // so try to use std::fill_n to benefit from its optimizations.
      const bool __can_fill
	= _GLIBCXX_USE_ASSIGN_FOR_INIT(_ValueType, const _Tp&)
      // For arbitrary class types and floating point types we can't assume
      // that __n > 0 and std::__size_to_integer(__n) > 0 are equivalent,
      // so only use std::fill_n when _Size is already an integral type.
	&& __is_integer<_Size>::__value;

      return __uninitialized_fill_n<__can_fill>::
	__uninit_fill_n(__first, __n, __x);
    }

#undef _GLIBCXX_USE_ASSIGN_FOR_INIT

  /// @cond undocumented

  // Extensions: versions of uninitialized_copy, uninitialized_fill,
  //  and uninitialized_fill_n that take an allocator parameter.
  //  We dispatch back to the standard versions when we're given the
  //  default allocator.  For nondefault allocators we do not use
  //  any of the POD optimizations.

  template<typename _InputIterator, typename _ForwardIterator,
	   typename _Allocator>
    _GLIBCXX20_CONSTEXPR
    _ForwardIterator
    __uninitialized_copy_a(_InputIterator __first, _InputIterator __last,
			   _ForwardIterator __result, _Allocator& __alloc)
    {
      _ForwardIterator __cur = __result;
      __try
	{
	  typedef __gnu_cxx::__alloc_traits<_Allocator> __traits;
	  for (; __first != __last; ++__first, (void)++__cur)
	    __traits::construct(__alloc, std::__addressof(*__cur), *__first);
	  return __cur;
	}
      __catch(...)
	{
	  std::_Destroy(__result, __cur, __alloc);
	  __throw_exception_again;
	}
    }

  template<typename _InputIterator, typename _ForwardIterator, typename _Tp>
    _GLIBCXX20_CONSTEXPR
    inline _ForwardIterator
    __uninitialized_copy_a(_InputIterator __first, _InputIterator __last,
			   _ForwardIterator __result, allocator<_Tp>&)
    {
#ifdef __cpp_lib_is_constant_evaluated
      if (std::is_constant_evaluated())
	return std::__do_uninit_copy(__first, __last, __result);
#endif
      return std::uninitialized_copy(__first, __last, __result);
    }

  template<typename _InputIterator, typename _ForwardIterator,
	   typename _Allocator>
    _GLIBCXX20_CONSTEXPR
    inline _ForwardIterator
    __uninitialized_move_a(_InputIterator __first, _InputIterator __last,
			   _ForwardIterator __result, _Allocator& __alloc)
    {
      return std::__uninitialized_copy_a(_GLIBCXX_MAKE_MOVE_ITERATOR(__first),
					 _GLIBCXX_MAKE_MOVE_ITERATOR(__last),
					 __result, __alloc);
    }

  template<typename _InputIterator, typename _ForwardIterator,
	   typename _Allocator>
    _GLIBCXX20_CONSTEXPR
    inline _ForwardIterator
    __uninitialized_move_if_noexcept_a(_InputIterator __first,
				       _InputIterator __last,
				       _ForwardIterator __result,
				       _Allocator& __alloc)
    {
      return std::__uninitialized_copy_a
	(_GLIBCXX_MAKE_MOVE_IF_NOEXCEPT_ITERATOR(__first),
	 _GLIBCXX_MAKE_MOVE_IF_NOEXCEPT_ITERATOR(__last), __result, __alloc);
    }

  template<typename _ForwardIterator, typename _Tp, typename _Allocator>
    _GLIBCXX20_CONSTEXPR
    void
    __uninitialized_fill_a(_ForwardIterator __first, _ForwardIterator __last,
			   const _Tp& __x, _Allocator& __alloc)
    {
      _ForwardIterator __cur = __first;
      __try
	{
	  typedef __gnu_cxx::__alloc_traits<_Allocator> __traits;
	  for (; __cur != __last; ++__cur)
	    __traits::construct(__alloc, std::__addressof(*__cur), __x);
	}
      __catch(...)
	{
	  std::_Destroy(__first, __cur, __alloc);
	  __throw_exception_again;
	}
    }

  template<typename _ForwardIterator, typename _Tp, typename _Tp2>
    _GLIBCXX20_CONSTEXPR
    inline void
    __uninitialized_fill_a(_ForwardIterator __first, _ForwardIterator __last,
			   const _Tp& __x, allocator<_Tp2>&)
    {
#ifdef __cpp_lib_is_constant_evaluated
      if (std::is_constant_evaluated())
	return std::__do_uninit_fill(__first, __last, __x);
#endif
      std::uninitialized_fill(__first, __last, __x);
    }

  template<typename _ForwardIterator, typename _Size, typename _Tp,
	   typename _Allocator>
     _GLIBCXX20_CONSTEXPR
    _ForwardIterator
    __uninitialized_fill_n_a(_ForwardIterator __first, _Size __n,
			     const _Tp& __x, _Allocator& __alloc)
    {
      _ForwardIterator __cur = __first;
      __try
	{
	  typedef __gnu_cxx::__alloc_traits<_Allocator> __traits;
	  for (; __n > 0; --__n, (void) ++__cur)
	    __traits::construct(__alloc, std::__addressof(*__cur), __x);
	  return __cur;
	}
      __catch(...)
	{
	  std::_Destroy(__first, __cur, __alloc);
	  __throw_exception_again;
	}
    }

  template<typename _ForwardIterator, typename _Size, typename _Tp,
	   typename _Tp2>
    _GLIBCXX20_CONSTEXPR
    inline _ForwardIterator
    __uninitialized_fill_n_a(_ForwardIterator __first, _Size __n,
			     const _Tp& __x, allocator<_Tp2>&)
    {
#ifdef __cpp_lib_is_constant_evaluated
      if (std::is_constant_evaluated())
	return std::__do_uninit_fill_n(__first, __n, __x);
#endif
      return std::uninitialized_fill_n(__first, __n, __x);
    }


  // Extensions: __uninitialized_copy_move, __uninitialized_move_copy,
  // __uninitialized_fill_move, __uninitialized_move_fill.
  // All of these algorithms take a user-supplied allocator, which is used
  // for construction and destruction.

  // __uninitialized_copy_move
  // Copies [first1, last1) into [result, result + (last1 - first1)), and
  //  move [first2, last2) into
  //  [result, result + (last1 - first1) + (last2 - first2)).
  template<typename _InputIterator1, typename _InputIterator2,
	   typename _ForwardIterator, typename _Allocator>
    inline _ForwardIterator
    __uninitialized_copy_move(_InputIterator1 __first1,
			      _InputIterator1 __last1,
			      _InputIterator2 __first2,
			      _InputIterator2 __last2,
			      _ForwardIterator __result,
			      _Allocator& __alloc)
    {
      _ForwardIterator __mid = std::__uninitialized_copy_a(__first1, __last1,
							   __result,
							   __alloc);
      __try
	{
	  return std::__uninitialized_move_a(__first2, __last2, __mid, __alloc);
	}
      __catch(...)
	{
	  std::_Destroy(__result, __mid, __alloc);
	  __throw_exception_again;
	}
    }

  // __uninitialized_move_copy
  // Moves [first1, last1) into [result, result + (last1 - first1)), and
  //  copies [first2, last2) into
  //  [result, result + (last1 - first1) + (last2 - first2)).
  template<typename _InputIterator1, typename _InputIterator2,
	   typename _ForwardIterator, typename _Allocator>
    inline _ForwardIterator
    __uninitialized_move_copy(_InputIterator1 __first1,
			      _InputIterator1 __last1,
			      _InputIterator2 __first2,
			      _InputIterator2 __last2,
			      _ForwardIterator __result,
			      _Allocator& __alloc)
    {
      _ForwardIterator __mid = std::__uninitialized_move_a(__first1, __last1,
							   __result,
							   __alloc);
      __try
	{
	  return std::__uninitialized_copy_a(__first2, __last2, __mid, __alloc);
	}
      __catch(...)
	{
	  std::_Destroy(__result, __mid, __alloc);
	  __throw_exception_again;
	}
    }

  // __uninitialized_fill_move
  // Fills [result, mid) with x, and moves [first, last) into
  //  [mid, mid + (last - first)).
  template<typename _ForwardIterator, typename _Tp, typename _InputIterator,
	   typename _Allocator>
    inline _ForwardIterator
    __uninitialized_fill_move(_ForwardIterator __result, _ForwardIterator __mid,
			      const _Tp& __x, _InputIterator __first,
			      _InputIterator __last, _Allocator& __alloc)
    {
      std::__uninitialized_fill_a(__result, __mid, __x, __alloc);
      __try
	{
	  return std::__uninitialized_move_a(__first, __last, __mid, __alloc);
	}
      __catch(...)
	{
	  std::_Destroy(__result, __mid, __alloc);
	  __throw_exception_again;
	}
    }

  // __uninitialized_move_fill
  // Moves [first1, last1) into [first2, first2 + (last1 - first1)), and
  //  fills [first2 + (last1 - first1), last2) with x.
  template<typename _InputIterator, typename _ForwardIterator, typename _Tp,
	   typename _Allocator>
    inline void
    __uninitialized_move_fill(_InputIterator __first1, _InputIterator __last1,
			      _ForwardIterator __first2,
			      _ForwardIterator __last2, const _Tp& __x,
			      _Allocator& __alloc)
    {
      _ForwardIterator __mid2 = std::__uninitialized_move_a(__first1, __last1,
							    __first2,
							    __alloc);
      __try
	{
	  std::__uninitialized_fill_a(__mid2, __last2, __x, __alloc);
	}
      __catch(...)
	{
	  std::_Destroy(__first2, __mid2, __alloc);
	  __throw_exception_again;
	}
    }

  /// @endcond

#if __cplusplus >= 201103L
  /// @cond undocumented

  // Extensions: __uninitialized_default, __uninitialized_default_n,
  // __uninitialized_default_a, __uninitialized_default_n_a.

  template<bool _TrivialValueType>
    struct __uninitialized_default_1
    {
      template<typename _ForwardIterator>
        static void
        __uninit_default(_ForwardIterator __first, _ForwardIterator __last)
        {
	  _ForwardIterator __cur = __first;
	  __try
	    {
	      for (; __cur != __last; ++__cur)
		std::_Construct(std::__addressof(*__cur));
	    }
	  __catch(...)
	    {
	      std::_Destroy(__first, __cur);
	      __throw_exception_again;
	    }
	}
    };

  template<>
    struct __uninitialized_default_1<true>
    {
      template<typename _ForwardIterator>
        static void
        __uninit_default(_ForwardIterator __first, _ForwardIterator __last)
        {
	  if (__first == __last)
	    return;

	  typename iterator_traits<_ForwardIterator>::value_type* __val
	    = std::__addressof(*__first);
	  std::_Construct(__val);
	  if (++__first != __last)
	    std::fill(__first, __last, *__val);
	}
    };

  template<bool _TrivialValueType>
    struct __uninitialized_default_n_1
    {
      template<typename _ForwardIterator, typename _Size>
	_GLIBCXX20_CONSTEXPR
        static _ForwardIterator
        __uninit_default_n(_ForwardIterator __first, _Size __n)
        {
	  _ForwardIterator __cur = __first;
	  __try
	    {
	      for (; __n > 0; --__n, (void) ++__cur)
		std::_Construct(std::__addressof(*__cur));
	      return __cur;
	    }
	  __catch(...)
	    {
	      std::_Destroy(__first, __cur);
	      __throw_exception_again;
	    }
	}
    };

  template<>
    struct __uninitialized_default_n_1<true>
    {
      template<typename _ForwardIterator, typename _Size>
	_GLIBCXX20_CONSTEXPR
        static _ForwardIterator
        __uninit_default_n(_ForwardIterator __first, _Size __n)
        {
	  if (__n > 0)
	    {
	      typename iterator_traits<_ForwardIterator>::value_type* __val
		= std::__addressof(*__first);
	      std::_Construct(__val);
	      ++__first;
	      __first = std::fill_n(__first, __n - 1, *__val);
	    }
	  return __first;
	}
    };

  // __uninitialized_default
  // Fills [first, last) with value-initialized value_types.
  template<typename _ForwardIterator>
    inline void
    __uninitialized_default(_ForwardIterator __first,
			    _ForwardIterator __last)
    {
      typedef typename iterator_traits<_ForwardIterator>::value_type
	_ValueType;
      // trivial types can have deleted assignment
      const bool __assignable = is_copy_assignable<_ValueType>::value;

      std::__uninitialized_default_1<__is_trivial(_ValueType)
				     && __assignable>::
	__uninit_default(__first, __last);
    }

  // __uninitialized_default_n
  // Fills [first, first + n) with value-initialized value_types.
  template<typename _ForwardIterator, typename _Size>
    _GLIBCXX20_CONSTEXPR
    inline _ForwardIterator
    __uninitialized_default_n(_ForwardIterator __first, _Size __n)
    {
      typedef typename iterator_traits<_ForwardIterator>::value_type
	_ValueType;
      // See uninitialized_fill_n for the conditions for using std::fill_n.
      constexpr bool __can_fill
	= __and_<is_integral<_Size>, is_copy_assignable<_ValueType>>::value;

      return __uninitialized_default_n_1<__is_trivial(_ValueType)
					 && __can_fill>::
	__uninit_default_n(__first, __n);
    }


  // __uninitialized_default_a
  // Fills [first, last) with value_types constructed by the allocator
  // alloc, with no arguments passed to the construct call.
  template<typename _ForwardIterator, typename _Allocator>
    void
    __uninitialized_default_a(_ForwardIterator __first,
			      _ForwardIterator __last,
			      _Allocator& __alloc)
    {
      _ForwardIterator __cur = __first;
      __try
	{
	  typedef __gnu_cxx::__alloc_traits<_Allocator> __traits;
	  for (; __cur != __last; ++__cur)
	    __traits::construct(__alloc, std::__addressof(*__cur));
	}
      __catch(...)
	{
	  std::_Destroy(__first, __cur, __alloc);
	  __throw_exception_again;
	}
    }

  template<typename _ForwardIterator, typename _Tp>
    inline void
    __uninitialized_default_a(_ForwardIterator __first,
			      _ForwardIterator __last,
			      allocator<_Tp>&)
    { std::__uninitialized_default(__first, __last); }


  // __uninitialized_default_n_a
  // Fills [first, first + n) with value_types constructed by the allocator
  // alloc, with no arguments passed to the construct call.
  template<typename _ForwardIterator, typename _Size, typename _Allocator>
    _GLIBCXX20_CONSTEXPR _ForwardIterator
    __uninitialized_default_n_a(_ForwardIterator __first, _Size __n,
				_Allocator& __alloc)
    {
      _ForwardIterator __cur = __first;
      __try
	{
	  typedef __gnu_cxx::__alloc_traits<_Allocator> __traits;
	  for (; __n > 0; --__n, (void) ++__cur)
	    __traits::construct(__alloc, std::__addressof(*__cur));
	  return __cur;
	}
      __catch(...)
	{
	  std::_Destroy(__first, __cur, __alloc);
	  __throw_exception_again;
	}
    }

  // __uninitialized_default_n_a specialization for std::allocator,
  // which ignores the allocator and value-initializes the elements.
  template<typename _ForwardIterator, typename _Size, typename _Tp>
    _GLIBCXX20_CONSTEXPR
    inline _ForwardIterator
    __uninitialized_default_n_a(_ForwardIterator __first, _Size __n,
				allocator<_Tp>&)
    { return std::__uninitialized_default_n(__first, __n); }

  template<bool _TrivialValueType>
    struct __uninitialized_default_novalue_1
    {
      template<typename _ForwardIterator>
	static void
	__uninit_default_novalue(_ForwardIterator __first,
				 _ForwardIterator __last)
	{
	  _ForwardIterator __cur = __first;
	  __try
	    {
	      for (; __cur != __last; ++__cur)
		std::_Construct_novalue(std::__addressof(*__cur));
	    }
	  __catch(...)
	    {
	      std::_Destroy(__first, __cur);
	      __throw_exception_again;
	    }
	}
    };

  template<>
    struct __uninitialized_default_novalue_1<true>
    {
      template<typename _ForwardIterator>
        static void
        __uninit_default_novalue(_ForwardIterator __first,
				 _ForwardIterator __last)
	{
	}
    };

  template<bool _TrivialValueType>
    struct __uninitialized_default_novalue_n_1
    {
      template<typename _ForwardIterator, typename _Size>
	static _ForwardIterator
	__uninit_default_novalue_n(_ForwardIterator __first, _Size __n)
	{
	  _ForwardIterator __cur = __first;
	  __try
	    {
	      for (; __n > 0; --__n, (void) ++__cur)
		std::_Construct_novalue(std::__addressof(*__cur));
	      return __cur;
	    }
	  __catch(...)
	    {
	      std::_Destroy(__first, __cur);
	      __throw_exception_again;
	    }
	}
    };

  template<>
    struct __uninitialized_default_novalue_n_1<true>
    {
      template<typename _ForwardIterator, typename _Size>
	static _ForwardIterator
	__uninit_default_novalue_n(_ForwardIterator __first, _Size __n)
	{ return std::next(__first, __n); }
    };

  // __uninitialized_default_novalue
  // Fills [first, last) with default-initialized value_types.
  template<typename _ForwardIterator>
    inline void
    __uninitialized_default_novalue(_ForwardIterator __first,
				    _ForwardIterator __last)
    {
      typedef typename iterator_traits<_ForwardIterator>::value_type
	_ValueType;

      std::__uninitialized_default_novalue_1<
	is_trivially_default_constructible<_ValueType>::value>::
	__uninit_default_novalue(__first, __last);
    }

  // __uninitialized_default_novalue_n
  // Fills [first, first + n) with default-initialized value_types.
  template<typename _ForwardIterator, typename _Size>
    inline _ForwardIterator
    __uninitialized_default_novalue_n(_ForwardIterator __first, _Size __n)
    {
      typedef typename iterator_traits<_ForwardIterator>::value_type
	_ValueType;

      return __uninitialized_default_novalue_n_1<
	is_trivially_default_constructible<_ValueType>::value>::
	__uninit_default_novalue_n(__first, __n);
    }

  template<typename _InputIterator, typename _Size,
	   typename _ForwardIterator>
    _ForwardIterator
    __uninitialized_copy_n(_InputIterator __first, _Size __n,
			   _ForwardIterator __result, input_iterator_tag)
    {
      _ForwardIterator __cur = __result;
      __try
	{
	  for (; __n > 0; --__n, (void) ++__first, ++__cur)
	    std::_Construct(std::__addressof(*__cur), *__first);
	  return __cur;
	}
      __catch(...)
	{
	  std::_Destroy(__result, __cur);
	  __throw_exception_again;
	}
    }

  template<typename _RandomAccessIterator, typename _Size,
	   typename _ForwardIterator>
    inline _ForwardIterator
    __uninitialized_copy_n(_RandomAccessIterator __first, _Size __n,
			   _ForwardIterator __result,
			   random_access_iterator_tag)
    { return std::uninitialized_copy(__first, __first + __n, __result); }

  template<typename _InputIterator, typename _Size,
	   typename _ForwardIterator>
    pair<_InputIterator, _ForwardIterator>
    __uninitialized_copy_n_pair(_InputIterator __first, _Size __n,
			   _ForwardIterator __result, input_iterator_tag)
    {
      _ForwardIterator __cur = __result;
      __try
	{
	  for (; __n > 0; --__n, (void) ++__first, ++__cur)
	    std::_Construct(std::__addressof(*__cur), *__first);
	  return {__first, __cur};
	}
      __catch(...)
	{
	  std::_Destroy(__result, __cur);
	  __throw_exception_again;
	}
    }

  template<typename _RandomAccessIterator, typename _Size,
	   typename _ForwardIterator>
    inline pair<_RandomAccessIterator, _ForwardIterator>
    __uninitialized_copy_n_pair(_RandomAccessIterator __first, _Size __n,
			   _ForwardIterator __result,
			   random_access_iterator_tag)
    {
      auto __second_res = uninitialized_copy(__first, __first + __n, __result);
      auto __first_res = std::next(__first, __n);
      return {__first_res, __second_res};
    }

  /// @endcond

  /**
   *  @brief Copies the range [first,first+n) into result.
   *  @param  __first  An input iterator.
   *  @param  __n      The number of elements to copy.
   *  @param  __result An output iterator.
   *  @return  __result + __n
   *  @since C++11
   *
   *  Like copy_n(), but does not require an initialized output range.
  */
  template<typename _InputIterator, typename _Size, typename _ForwardIterator>
    inline _ForwardIterator
    uninitialized_copy_n(_InputIterator __first, _Size __n,
			 _ForwardIterator __result)
    { return std::__uninitialized_copy_n(__first, __n, __result,
					 std::__iterator_category(__first)); }

  /// @cond undocumented
  template<typename _InputIterator, typename _Size, typename _ForwardIterator>
    inline pair<_InputIterator, _ForwardIterator>
    __uninitialized_copy_n_pair(_InputIterator __first, _Size __n,
			      _ForwardIterator __result)
    {
      return
	std::__uninitialized_copy_n_pair(__first, __n, __result,
					 std::__iterator_category(__first));
    }
  /// @endcond
#endif

#if __cplusplus >= 201703L
# define __cpp_lib_raw_memory_algorithms 201606L

  /**
   *  @brief Default-initializes objects in the range [first,last).
   *  @param  __first  A forward iterator.
   *  @param  __last   A forward iterator.
   *  @since C++17
  */
  template <typename _ForwardIterator>
    inline void
    uninitialized_default_construct(_ForwardIterator __first,
				    _ForwardIterator __last)
    {
      __uninitialized_default_novalue(__first, __last);
    }

  /**
   *  @brief Default-initializes objects in the range [first,first+count).
   *  @param  __first  A forward iterator.
   *  @param  __count  The number of objects to construct.
   *  @return   __first + __count
   *  @since C++17
  */
  template <typename _ForwardIterator, typename _Size>
    inline _ForwardIterator
    uninitialized_default_construct_n(_ForwardIterator __first, _Size __count)
    {
      return __uninitialized_default_novalue_n(__first, __count);
    }

  /**
   *  @brief Value-initializes objects in the range [first,last).
   *  @param  __first  A forward iterator.
   *  @param  __last   A forward iterator.
   *  @since C++17
  */
  template <typename _ForwardIterator>
    inline void
    uninitialized_value_construct(_ForwardIterator __first,
				  _ForwardIterator __last)
    {
      return __uninitialized_default(__first, __last);
    }

  /**
   *  @brief Value-initializes objects in the range [first,first+count).
   *  @param  __first  A forward iterator.
   *  @param  __count  The number of objects to construct.
   *  @return   __result + __count
   *  @since C++17
  */
  template <typename _ForwardIterator, typename _Size>
    inline _ForwardIterator
    uninitialized_value_construct_n(_ForwardIterator __first, _Size __count)
    {
      return __uninitialized_default_n(__first, __count);
    }

  /**
   *  @brief Move-construct from the range [first,last) into result.
   *  @param  __first  An input iterator.
   *  @param  __last   An input iterator.
   *  @param  __result An output iterator.
   *  @return   __result + (__first - __last)
   *  @since C++17
  */
  template <typename _InputIterator, typename _ForwardIterator>
    inline _ForwardIterator
    uninitialized_move(_InputIterator __first, _InputIterator __last,
		       _ForwardIterator __result)
    {
      return std::uninitialized_copy
	(_GLIBCXX_MAKE_MOVE_ITERATOR(__first),
	 _GLIBCXX_MAKE_MOVE_ITERATOR(__last), __result);
    }

  /**
   *  @brief Move-construct from the range [first,first+count) into result.
   *  @param  __first  An input iterator.
   *  @param  __count  The number of objects to initialize.
   *  @param  __result An output iterator.
   *  @return  __result + __count
   *  @since C++17
  */
  template <typename _InputIterator, typename _Size, typename _ForwardIterator>
    inline pair<_InputIterator, _ForwardIterator>
    uninitialized_move_n(_InputIterator __first, _Size __count,
			 _ForwardIterator __result)
    {
      auto __res = std::__uninitialized_copy_n_pair
	(_GLIBCXX_MAKE_MOVE_ITERATOR(__first),
	 __count, __result);
      return {__res.first.base(), __res.second};
    }
#endif // C++17

#if __cplusplus >= 201103L
  /// @cond undocumented

  template<typename _Tp, typename _Up, typename _Allocator>
    _GLIBCXX20_CONSTEXPR
    inline void
    __relocate_object_a(_Tp* __restrict __dest, _Up* __restrict __orig,
			_Allocator& __alloc)
    noexcept(noexcept(std::allocator_traits<_Allocator>::construct(__alloc,
			 __dest, std::move(*__orig)))
	     && noexcept(std::allocator_traits<_Allocator>::destroy(
			    __alloc, std::__addressof(*__orig))))
    {
      typedef std::allocator_traits<_Allocator> __traits;
      __traits::construct(__alloc, __dest, std::move(*__orig));
      __traits::destroy(__alloc, std::__addressof(*__orig));
    }

  // This class may be specialized for specific types.
  // Also known as is_trivially_relocatable.
  template<typename _Tp, typename = void>
    struct __is_bitwise_relocatable
    : is_trivial<_Tp> { };

  template <typename _InputIterator, typename _ForwardIterator,
	    typename _Allocator>
    _GLIBCXX20_CONSTEXPR
    inline _ForwardIterator
    __relocate_a_1(_InputIterator __first, _InputIterator __last,
		   _ForwardIterator __result, _Allocator& __alloc)
    noexcept(noexcept(std::__relocate_object_a(std::addressof(*__result),
					       std::addressof(*__first),
					       __alloc)))
    {
      typedef typename iterator_traits<_InputIterator>::value_type
	_ValueType;
      typedef typename iterator_traits<_ForwardIterator>::value_type
	_ValueType2;
      static_assert(std::is_same<_ValueType, _ValueType2>::value,
	  "relocation is only possible for values of the same type");
      _ForwardIterator __cur = __result;
      for (; __first != __last; ++__first, (void)++__cur)
	std::__relocate_object_a(std::__addressof(*__cur),
				 std::__addressof(*__first), __alloc);
      return __cur;
    }

  template <typename _Tp, typename _Up>
    _GLIBCXX20_CONSTEXPR
    inline __enable_if_t<std::__is_bitwise_relocatable<_Tp>::value, _Tp*>
    __relocate_a_1(_Tp* __first, _Tp* __last,
		   _Tp* __result,
		   [[__maybe_unused__]] allocator<_Up>& __alloc) noexcept
    {
      ptrdiff_t __count = __last - __first;
      if (__count > 0)
	{
#ifdef __cpp_lib_is_constant_evaluated
	  if (std::is_constant_evaluated())
	    {
	      // Can't use memmove. Wrap the pointer so that __relocate_a_1
	      // resolves to the non-trivial overload above.
	      __gnu_cxx::__normal_iterator<_Tp*, void> __out(__result);
	      __out = std::__relocate_a_1(__first, __last, __out, __alloc);
	      return __out.base();
	    }
#endif
	  __builtin_memmove(__result, __first, __count * sizeof(_Tp));
	}
      return __result + __count;
    }


  template <typename _InputIterator, typename _ForwardIterator,
	    typename _Allocator>
    _GLIBCXX20_CONSTEXPR
    inline _ForwardIterator
    __relocate_a(_InputIterator __first, _InputIterator __last,
		 _ForwardIterator __result, _Allocator& __alloc)
    noexcept(noexcept(__relocate_a_1(std::__niter_base(__first),
				     std::__niter_base(__last),
				     std::__niter_base(__result), __alloc)))
    {
      return std::__relocate_a_1(std::__niter_base(__first),
				 std::__niter_base(__last),
				 std::__niter_base(__result), __alloc);
    }

  /// @endcond
#endif

  /// @} group memory

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif /* _STL_UNINITIALIZED_H */
