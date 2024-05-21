// Pointer Traits -*- C++ -*-

// Copyright (C) 2011-2022 Free Software Foundation, Inc.
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

/** @file bits/ptr_traits.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{memory}
 */

#ifndef _PTR_TRAITS_H
#define _PTR_TRAITS_H 1

#if __cplusplus >= 201103L

#include <bits/move.h>

#if __cplusplus > 201703L
#include <concepts>
# ifndef __cpp_lib_constexpr_memory
// Defined to a newer value in bits/unique_ptr.h for C++23
#  define __cpp_lib_constexpr_memory 201811L
# endif
namespace __gnu_debug { struct _Safe_iterator_base; }
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  class __undefined;

  // For a specialization `SomeTemplate<T, Types...>` the member `type` is T,
  // otherwise `type` is `__undefined`.
  template<typename _Tp>
    struct __get_first_arg
    { using type = __undefined; };

  template<template<typename, typename...> class _SomeTemplate, typename _Tp,
           typename... _Types>
    struct __get_first_arg<_SomeTemplate<_Tp, _Types...>>
    { using type = _Tp; };

  // For a specialization `SomeTemplate<T, Args...>` and a type `U` the member
  // `type` is `SomeTemplate<U, Args...>`, otherwise there is no member `type`.
  template<typename _Tp, typename _Up>
    struct __replace_first_arg
    { };

  template<template<typename, typename...> class _SomeTemplate, typename _Up,
           typename _Tp, typename... _Types>
    struct __replace_first_arg<_SomeTemplate<_Tp, _Types...>, _Up>
    { using type = _SomeTemplate<_Up, _Types...>; };

#if __cpp_concepts
  // When concepts are supported detection of _Ptr::element_type is done
  // by a requires-clause, so __ptr_traits_elem_t only needs to do this:
  template<typename _Ptr>
    using __ptr_traits_elem_t = typename __get_first_arg<_Ptr>::type;
#else
  // Detect the element type of a pointer-like type.
  template<typename _Ptr, typename = void>
    struct __ptr_traits_elem : __get_first_arg<_Ptr>
    { };

  // Use _Ptr::element_type if is a valid type.
  template<typename _Ptr>
    struct __ptr_traits_elem<_Ptr, __void_t<typename _Ptr::element_type>>
    { using type = typename _Ptr::element_type; };

  template<typename _Ptr>
    using __ptr_traits_elem_t = typename __ptr_traits_elem<_Ptr>::type;
#endif

  // Define pointer_traits<P>::pointer_to.
  template<typename _Ptr, typename _Elt, bool = is_void<_Elt>::value>
    struct __ptr_traits_ptr_to
    {
      using pointer = _Ptr;
      using element_type = _Elt;

      /**
       *  @brief  Obtain a pointer to an object
       *  @param  __r  A reference to an object of type `element_type`
       *  @return `pointer::pointer_to(__e)`
       *  @pre `pointer::pointer_to(__e)` is a valid expression.
      */
      static pointer
      pointer_to(element_type& __e)
#if __cpp_lib_concepts
      requires requires {
	{ pointer::pointer_to(__e) } -> convertible_to<pointer>;
      }
#endif
      { return pointer::pointer_to(__e); }
    };

  // Do not define pointer_traits<P>::pointer_to if element type is void.
  template<typename _Ptr, typename _Elt>
    struct __ptr_traits_ptr_to<_Ptr, _Elt, true>
    { };

  // Partial specialization defining pointer_traits<T*>::pointer_to(T&).
  template<typename _Tp>
    struct __ptr_traits_ptr_to<_Tp*, _Tp, false>
    {
      using pointer = _Tp*;
      using element_type = _Tp;

      /**
       *  @brief  Obtain a pointer to an object
       *  @param  __r  A reference to an object of type `element_type`
       *  @return `addressof(__r)`
      */
      static _GLIBCXX20_CONSTEXPR pointer
      pointer_to(element_type& __r) noexcept
      { return std::addressof(__r); }
    };

  template<typename _Ptr, typename _Elt>
    struct __ptr_traits_impl : __ptr_traits_ptr_to<_Ptr, _Elt>
    {
    private:
      template<typename _Tp, typename = void>
	struct __difference { using type = ptrdiff_t; };

      template<typename _Tp>
#if __cpp_concepts
	requires requires { typename _Tp::difference_type; }
	struct __difference<_Tp>
#else
	struct __difference<_Tp, __void_t<typename _Tp::difference_type>>
#endif
	{ using type = typename _Tp::difference_type; };

      template<typename _Tp, typename _Up, typename = void>
	struct __rebind : __replace_first_arg<_Tp, _Up> { };

      template<typename _Tp, typename _Up>
#if __cpp_concepts
	requires requires { typename _Tp::template rebind<_Up>; }
	struct __rebind<_Tp, _Up>
#else
	struct __rebind<_Tp, _Up, __void_t<typename _Tp::template rebind<_Up>>>
#endif
	{ using type = typename _Tp::template rebind<_Up>; };

    public:
      /// The pointer type.
      using pointer = _Ptr;

      /// The type pointed to.
      using element_type = _Elt;

      /// The type used to represent the difference between two pointers.
      using difference_type = typename __difference<_Ptr>::type;

      /// A pointer to a different type.
      template<typename _Up>
        using rebind = typename __rebind<_Ptr, _Up>::type;
    };

  // _GLIBCXX_RESOLVE_LIB_DEFECTS
  // 3545. std::pointer_traits should be SFINAE-friendly
  template<typename _Ptr>
    struct __ptr_traits_impl<_Ptr, __undefined>
    { };

  /**
   * @brief  Uniform interface to all pointer-like types
   * @ingroup pointer_abstractions
   * @since C++11
  */
  template<typename _Ptr>
    struct pointer_traits : __ptr_traits_impl<_Ptr, __ptr_traits_elem_t<_Ptr>>
    { };

#if __cpp_concepts
  template<typename _Ptr> requires requires { typename _Ptr::element_type; }
    struct pointer_traits<_Ptr>
    : __ptr_traits_impl<_Ptr, typename _Ptr::element_type>
    { };
#endif

  /**
   * @brief  Partial specialization for built-in pointers.
   * @ingroup pointer_abstractions
   * @since C++11
  */
  template<typename _Tp>
    struct pointer_traits<_Tp*> : __ptr_traits_ptr_to<_Tp*, _Tp>
    {
      /// The pointer type
      typedef _Tp* pointer;
      /// The type pointed to
      typedef _Tp  element_type;
      /// Type used to represent the difference between two pointers
      typedef ptrdiff_t difference_type;
      /// A pointer to a different type.
      template<typename _Up> using rebind = _Up*;
    };

  /// Convenience alias for rebinding pointers.
  template<typename _Ptr, typename _Tp>
    using __ptr_rebind = typename pointer_traits<_Ptr>::template rebind<_Tp>;

  template<typename _Tp>
    constexpr _Tp*
    __to_address(_Tp* __ptr) noexcept
    {
      static_assert(!std::is_function<_Tp>::value, "not a function pointer");
      return __ptr;
    }

#if __cplusplus <= 201703L
  template<typename _Ptr>
    constexpr typename std::pointer_traits<_Ptr>::element_type*
    __to_address(const _Ptr& __ptr)
    { return std::__to_address(__ptr.operator->()); }
#else
  template<typename _Ptr>
    constexpr auto
    __to_address(const _Ptr& __ptr) noexcept
    -> decltype(std::pointer_traits<_Ptr>::to_address(__ptr))
    { return std::pointer_traits<_Ptr>::to_address(__ptr); }

  template<typename _Ptr, typename... _None>
    constexpr auto
    __to_address(const _Ptr& __ptr, _None...) noexcept
    {
      if constexpr (is_base_of_v<__gnu_debug::_Safe_iterator_base, _Ptr>)
	return std::__to_address(__ptr.base().operator->());
      else
	return std::__to_address(__ptr.operator->());
    }

#define __cpp_lib_to_address 201711L

  /**
   * @brief Obtain address referenced by a pointer to an object
   * @param __ptr A pointer to an object
   * @return @c __ptr
   * @ingroup pointer_abstractions
  */
  template<typename _Tp>
    constexpr _Tp*
    to_address(_Tp* __ptr) noexcept
    { return std::__to_address(__ptr); }

  /**
   * @brief Obtain address referenced by a pointer to an object
   * @param __ptr A pointer to an object
   * @return @c pointer_traits<_Ptr>::to_address(__ptr) if that expression is
             well-formed, otherwise @c to_address(__ptr.operator->())
   * @ingroup pointer_abstractions
  */
  template<typename _Ptr>
    constexpr auto
    to_address(const _Ptr& __ptr) noexcept
    { return std::__to_address(__ptr); }
#endif // C++2a

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif

#endif
