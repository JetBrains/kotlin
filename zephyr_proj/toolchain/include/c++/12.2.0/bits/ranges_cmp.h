// Concept-constrained comparison implementations -*- C++ -*-

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

/** @file bits/ranges_cmp.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{functional}
 */

#ifndef _RANGES_CMP_H
#define _RANGES_CMP_H 1

#if __cplusplus > 201703L
# include <bits/move.h>
# include <concepts>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  struct __is_transparent; // not defined

  // Define std::identity here so that <iterator> and <ranges>
  // don't need to include <bits/stl_function.h> to get it.

  /// [func.identity] The identity function.
  struct identity
  {
    template<typename _Tp>
      [[nodiscard]]
      constexpr _Tp&&
      operator()(_Tp&& __t) const noexcept
      { return std::forward<_Tp>(__t); }

    using is_transparent = __is_transparent;
  };

#ifdef __cpp_lib_concepts
// Define this here, included by all the headers that need to define it.
#define __cpp_lib_ranges 202110L

namespace ranges
{
  namespace __detail
  {
    // BUILTIN-PTR-CMP(T, <, U)
    // This determines whether t < u results in a call to a built-in operator<
    // comparing pointers. It doesn't work for function pointers (PR 93628).
    template<typename _Tp, typename _Up>
      concept __less_builtin_ptr_cmp
	= requires (_Tp&& __t, _Up&& __u) { { __t < __u } -> same_as<bool>; }
	  && convertible_to<_Tp, const volatile void*>
	  && convertible_to<_Up, const volatile void*>
	  && (! requires(_Tp&& __t, _Up&& __u)
	      { operator<(std::forward<_Tp>(__t), std::forward<_Up>(__u)); }
	      && ! requires(_Tp&& __t, _Up&& __u)
	      { std::forward<_Tp>(__t).operator<(std::forward<_Up>(__u)); });
  } // namespace __detail

  // [range.cmp] Concept-constrained comparisons

  // _GLIBCXX_RESOLVE_LIB_DEFECTS
  // 3530 BUILTIN-PTR-MEOW should not opt the type out of syntactic checks

  /// ranges::equal_to function object type.
  struct equal_to
  {
    template<typename _Tp, typename _Up>
      requires equality_comparable_with<_Tp, _Up>
      constexpr bool
      operator()(_Tp&& __t, _Up&& __u) const
      noexcept(noexcept(std::declval<_Tp>() == std::declval<_Up>()))
      { return std::forward<_Tp>(__t) == std::forward<_Up>(__u); }

    using is_transparent = __is_transparent;
  };

  /// ranges::not_equal_to function object type.
  struct not_equal_to
  {
    template<typename _Tp, typename _Up>
      requires equality_comparable_with<_Tp, _Up>
      constexpr bool
      operator()(_Tp&& __t, _Up&& __u) const
      noexcept(noexcept(std::declval<_Up>() == std::declval<_Tp>()))
      { return !equal_to{}(std::forward<_Tp>(__t), std::forward<_Up>(__u)); }

    using is_transparent = __is_transparent;
  };

  /// ranges::less function object type.
  struct less
  {
    template<typename _Tp, typename _Up>
      requires totally_ordered_with<_Tp, _Up>
      constexpr bool
      operator()(_Tp&& __t, _Up&& __u) const
      noexcept(noexcept(std::declval<_Tp>() < std::declval<_Up>()))
      {
	if constexpr (__detail::__less_builtin_ptr_cmp<_Tp, _Up>)
	  {
	    if (std::__is_constant_evaluated())
	      return __t < __u;

	    auto __x = reinterpret_cast<__UINTPTR_TYPE__>(
	      static_cast<const volatile void*>(std::forward<_Tp>(__t)));
	    auto __y = reinterpret_cast<__UINTPTR_TYPE__>(
	      static_cast<const volatile void*>(std::forward<_Up>(__u)));
	    return __x < __y;
	  }
	else
	  return std::forward<_Tp>(__t) < std::forward<_Up>(__u);
      }

    using is_transparent = __is_transparent;
  };

  /// ranges::greater function object type.
  struct greater
  {
    template<typename _Tp, typename _Up>
      requires totally_ordered_with<_Tp, _Up>
      constexpr bool
      operator()(_Tp&& __t, _Up&& __u) const
      noexcept(noexcept(std::declval<_Up>() < std::declval<_Tp>()))
      { return less{}(std::forward<_Up>(__u), std::forward<_Tp>(__t)); }

    using is_transparent = __is_transparent;
  };

  /// ranges::greater_equal function object type.
  struct greater_equal
  {
    template<typename _Tp, typename _Up>
      requires totally_ordered_with<_Tp, _Up>
      constexpr bool
      operator()(_Tp&& __t, _Up&& __u) const
      noexcept(noexcept(std::declval<_Tp>() < std::declval<_Up>()))
      { return !less{}(std::forward<_Tp>(__t), std::forward<_Up>(__u)); }

    using is_transparent = __is_transparent;
  };

  /// ranges::less_equal function object type.
  struct less_equal
  {
    template<typename _Tp, typename _Up>
      requires totally_ordered_with<_Tp, _Up>
      constexpr bool
      operator()(_Tp&& __t, _Up&& __u) const
      noexcept(noexcept(std::declval<_Up>() < std::declval<_Tp>()))
      { return !less{}(std::forward<_Up>(__u), std::forward<_Tp>(__t)); }

    using is_transparent = __is_transparent;
  };

} // namespace ranges
#endif // library concepts
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif // C++20
#endif // _RANGES_CMP_H
