// Utility functions for uses-allocator construction -*- C++ -*-

// Copyright (C) 2019-2022 Free Software Foundation, Inc.
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

/** @file include/bits/uses_allocator_args.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{memory}
 */

#ifndef _USES_ALLOCATOR_ARGS
#define _USES_ALLOCATOR_ARGS 1

#pragma GCC system_header

#if __cplusplus > 201703L && __cpp_concepts

#include <new>			// for placement operator new
#include <tuple>		// for tuple, make_tuple, make_from_tuple
#include <bits/stl_construct.h> // construct_at
#include <bits/stl_pair.h>      // pair

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  template<typename _Tp>
    concept _Std_pair = __is_pair<_Tp>;

/** @addtogroup allocators
 *  @{
 */

// Not specified by C++20, used internally
#define __cpp_lib_make_obj_using_allocator 201811L

  template<typename _Tp, typename _Alloc, typename... _Args>
    constexpr auto
    uses_allocator_construction_args(const _Alloc& __a,
				     _Args&&... __args) noexcept
    requires (! _Std_pair<_Tp>)
    {
      if constexpr (uses_allocator_v<remove_cv_t<_Tp>, _Alloc>)
	{
	  if constexpr (is_constructible_v<_Tp, allocator_arg_t,
					   const _Alloc&, _Args...>)
	    {
	      return tuple<allocator_arg_t, const _Alloc&, _Args&&...>(
		  allocator_arg, __a, std::forward<_Args>(__args)...);
	    }
	  else
	    {
	      static_assert(is_constructible_v<_Tp, _Args..., const _Alloc&>,
		  "construction with an allocator must be possible"
		  " if uses_allocator is true");

	      return tuple<_Args&&..., const _Alloc&>(
		  std::forward<_Args>(__args)..., __a);
	    }
	}
      else
	{
	  static_assert(is_constructible_v<_Tp, _Args...>);

	  return tuple<_Args&&...>(std::forward<_Args>(__args)...);
	}
    }

  template<_Std_pair _Tp, typename _Alloc, typename _Tuple1, typename _Tuple2>
    constexpr auto
    uses_allocator_construction_args(const _Alloc& __a, piecewise_construct_t,
				     _Tuple1&& __x, _Tuple2&& __y) noexcept;

  template<_Std_pair _Tp, typename _Alloc>
    constexpr auto
    uses_allocator_construction_args(const _Alloc&) noexcept;

  template<_Std_pair _Tp, typename _Alloc, typename _Up, typename _Vp>
    constexpr auto
    uses_allocator_construction_args(const _Alloc&, _Up&&, _Vp&&) noexcept;

  template<_Std_pair _Tp, typename _Alloc, typename _Up, typename _Vp>
    constexpr auto
    uses_allocator_construction_args(const _Alloc&,
				     const pair<_Up, _Vp>&) noexcept;

  template<_Std_pair _Tp, typename _Alloc, typename _Up, typename _Vp>
    constexpr auto
    uses_allocator_construction_args(const _Alloc&, pair<_Up, _Vp>&&) noexcept;

  template<_Std_pair _Tp, typename _Alloc, typename _Tuple1, typename _Tuple2>
    constexpr auto
    uses_allocator_construction_args(const _Alloc& __a, piecewise_construct_t,
				     _Tuple1&& __x, _Tuple2&& __y) noexcept
    {
      using _Tp1 = typename _Tp::first_type;
      using _Tp2 = typename _Tp::second_type;

      return std::make_tuple(piecewise_construct,
	  std::apply([&__a](auto&&... __args1) {
	      return std::uses_allocator_construction_args<_Tp1>(
		  __a, std::forward<decltype(__args1)>(__args1)...);
	  }, std::forward<_Tuple1>(__x)),
	  std::apply([&__a](auto&&... __args2) {
	      return std::uses_allocator_construction_args<_Tp2>(
		  __a, std::forward<decltype(__args2)>(__args2)...);
	  }, std::forward<_Tuple2>(__y)));
    }

  template<_Std_pair _Tp, typename _Alloc>
    constexpr auto
    uses_allocator_construction_args(const _Alloc& __a) noexcept
    {
      using _Tp1 = typename _Tp::first_type;
      using _Tp2 = typename _Tp::second_type;

      return std::make_tuple(piecewise_construct,
	  std::uses_allocator_construction_args<_Tp1>(__a),
	  std::uses_allocator_construction_args<_Tp2>(__a));
    }

  template<_Std_pair _Tp, typename _Alloc, typename _Up, typename _Vp>
    constexpr auto
    uses_allocator_construction_args(const _Alloc& __a, _Up&& __u, _Vp&& __v)
      noexcept
    {
      using _Tp1 = typename _Tp::first_type;
      using _Tp2 = typename _Tp::second_type;

      return std::make_tuple(piecewise_construct,
	  std::uses_allocator_construction_args<_Tp1>(__a,
	    std::forward<_Up>(__u)),
	  std::uses_allocator_construction_args<_Tp2>(__a,
	    std::forward<_Vp>(__v)));
    }

  template<_Std_pair _Tp, typename _Alloc, typename _Up, typename _Vp>
    constexpr auto
    uses_allocator_construction_args(const _Alloc& __a,
				     const pair<_Up, _Vp>& __pr) noexcept
    {
      using _Tp1 = typename _Tp::first_type;
      using _Tp2 = typename _Tp::second_type;

      return std::make_tuple(piecewise_construct,
	  std::uses_allocator_construction_args<_Tp1>(__a, __pr.first),
	  std::uses_allocator_construction_args<_Tp2>(__a, __pr.second));
    }

  template<_Std_pair _Tp, typename _Alloc, typename _Up, typename _Vp>
    constexpr auto
    uses_allocator_construction_args(const _Alloc& __a,
				     pair<_Up, _Vp>&& __pr) noexcept
    {
      using _Tp1 = typename _Tp::first_type;
      using _Tp2 = typename _Tp::second_type;

      return std::make_tuple(piecewise_construct,
	  std::uses_allocator_construction_args<_Tp1>(__a,
	    std::move(__pr).first),
	  std::uses_allocator_construction_args<_Tp2>(__a,
	    std::move(__pr).second));
    }

  template<typename _Tp, typename _Alloc, typename... _Args>
    constexpr _Tp
    make_obj_using_allocator(const _Alloc& __a, _Args&&... __args)
    {
      return std::make_from_tuple<_Tp>(
	  std::uses_allocator_construction_args<_Tp>(__a,
	    std::forward<_Args>(__args)...));
    }

  template<typename _Tp, typename _Alloc, typename... _Args>
    constexpr _Tp*
    uninitialized_construct_using_allocator(_Tp* __p, const _Alloc& __a,
					    _Args&&... __args)
    {
      return std::apply([&](auto&&... __xs) {
	return std::construct_at(__p, std::forward<decltype(__xs)>(__xs)...);
      }, std::uses_allocator_construction_args<_Tp>(__a,
	std::forward<_Args>(__args)...));
    }
/// @}
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif // C++20
#endif // _USES_ALLOCATOR_ARGS
