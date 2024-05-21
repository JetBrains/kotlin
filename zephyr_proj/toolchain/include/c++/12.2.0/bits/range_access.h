// Range access functions for containers -*- C++ -*-

// Copyright (C) 2010-2022 Free Software Foundation, Inc.
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

/** @file bits/range_access.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{iterator}
 */

#ifndef _GLIBCXX_RANGE_ACCESS_H
#define _GLIBCXX_RANGE_ACCESS_H 1

#pragma GCC system_header

#if __cplusplus >= 201103L
#include <initializer_list>
#include <type_traits>	    // common_type_t, make_signed_t
#include <bits/stl_iterator.h> // reverse_iterator

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  /**
   *  @brief  Return an iterator pointing to the first element of
   *          the container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    begin(_Container& __cont) -> decltype(__cont.begin())
    { return __cont.begin(); }

  /**
   *  @brief  Return an iterator pointing to the first element of
   *          the const container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    begin(const _Container& __cont) -> decltype(__cont.begin())
    { return __cont.begin(); }

  /**
   *  @brief  Return an iterator pointing to one past the last element of
   *          the container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    end(_Container& __cont) -> decltype(__cont.end())
    { return __cont.end(); }

  /**
   *  @brief  Return an iterator pointing to one past the last element of
   *          the const container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    end(const _Container& __cont) -> decltype(__cont.end())
    { return __cont.end(); }

  /**
   *  @brief  Return an iterator pointing to the first element of the array.
   *  @param  __arr  Array.
   */
  template<typename _Tp, size_t _Nm>
    [[__nodiscard__]]
    inline _GLIBCXX14_CONSTEXPR _Tp*
    begin(_Tp (&__arr)[_Nm]) noexcept
    { return __arr; }

  /**
   *  @brief  Return an iterator pointing to one past the last element
   *          of the array.
   *  @param  __arr  Array.
   */
  template<typename _Tp, size_t _Nm>
    [[__nodiscard__]]
    inline _GLIBCXX14_CONSTEXPR _Tp*
    end(_Tp (&__arr)[_Nm]) noexcept
    { return __arr + _Nm; }

#if __cplusplus >= 201402L

  template<typename _Tp> class valarray;
  // These overloads must be declared for cbegin and cend to use them.
  template<typename _Tp> _Tp* begin(valarray<_Tp>&) noexcept;
  template<typename _Tp> const _Tp* begin(const valarray<_Tp>&) noexcept;
  template<typename _Tp> _Tp* end(valarray<_Tp>&) noexcept;
  template<typename _Tp> const _Tp* end(const valarray<_Tp>&) noexcept;

  /**
   *  @brief  Return an iterator pointing to the first element of
   *          the const container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    constexpr auto
    cbegin(const _Container& __cont) noexcept(noexcept(std::begin(__cont)))
      -> decltype(std::begin(__cont))
    { return std::begin(__cont); }

  /**
   *  @brief  Return an iterator pointing to one past the last element of
   *          the const container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    constexpr auto
    cend(const _Container& __cont) noexcept(noexcept(std::end(__cont)))
      -> decltype(std::end(__cont))
    { return std::end(__cont); }

  /**
   *  @brief  Return a reverse iterator pointing to the last element of
   *          the container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    rbegin(_Container& __cont) -> decltype(__cont.rbegin())
    { return __cont.rbegin(); }

  /**
   *  @brief  Return a reverse iterator pointing to the last element of
   *          the const container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    rbegin(const _Container& __cont) -> decltype(__cont.rbegin())
    { return __cont.rbegin(); }

  /**
   *  @brief  Return a reverse iterator pointing one past the first element of
   *          the container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    rend(_Container& __cont) -> decltype(__cont.rend())
    { return __cont.rend(); }

  /**
   *  @brief  Return a reverse iterator pointing one past the first element of
   *          the const container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    rend(const _Container& __cont) -> decltype(__cont.rend())
    { return __cont.rend(); }

  /**
   *  @brief  Return a reverse iterator pointing to the last element of
   *          the array.
   *  @param  __arr  Array.
   */
  template<typename _Tp, size_t _Nm>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR reverse_iterator<_Tp*>
    rbegin(_Tp (&__arr)[_Nm]) noexcept
    { return reverse_iterator<_Tp*>(__arr + _Nm); }

  /**
   *  @brief  Return a reverse iterator pointing one past the first element of
   *          the array.
   *  @param  __arr  Array.
   */
  template<typename _Tp, size_t _Nm>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR reverse_iterator<_Tp*>
    rend(_Tp (&__arr)[_Nm]) noexcept
    { return reverse_iterator<_Tp*>(__arr); }

  /**
   *  @brief  Return a reverse iterator pointing to the last element of
   *          the initializer_list.
   *  @param  __il  initializer_list.
   */
  template<typename _Tp>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR reverse_iterator<const _Tp*>
    rbegin(initializer_list<_Tp> __il) noexcept
    { return reverse_iterator<const _Tp*>(__il.end()); }

  /**
   *  @brief  Return a reverse iterator pointing one past the first element of
   *          the initializer_list.
   *  @param  __il  initializer_list.
   */
  template<typename _Tp>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR reverse_iterator<const _Tp*>
    rend(initializer_list<_Tp> __il) noexcept
    { return reverse_iterator<const _Tp*>(__il.begin()); }

  /**
   *  @brief  Return a reverse iterator pointing to the last element of
   *          the const container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    crbegin(const _Container& __cont) -> decltype(std::rbegin(__cont))
    { return std::rbegin(__cont); }

  /**
   *  @brief  Return a reverse iterator pointing one past the first element of
   *          the const container.
   *  @param  __cont  Container.
   */
  template<typename _Container>
    [[__nodiscard__]]
    inline _GLIBCXX17_CONSTEXPR auto
    crend(const _Container& __cont) -> decltype(std::rend(__cont))
    { return std::rend(__cont); }

#endif // C++14

#if __cplusplus >= 201703L
#define __cpp_lib_nonmember_container_access 201411L

  /**
   *  @brief  Return the size of a container.
   *  @param  __cont  Container.
   */
  template <typename _Container>
    [[nodiscard]]
    constexpr auto
    size(const _Container& __cont) noexcept(noexcept(__cont.size()))
    -> decltype(__cont.size())
    { return __cont.size(); }

  /**
   *  @brief  Return the size of an array.
   */
  template <typename _Tp, size_t _Nm>
    [[nodiscard]]
    constexpr size_t
    size(const _Tp (&)[_Nm]) noexcept
    { return _Nm; }

  /**
   *  @brief  Return whether a container is empty.
   *  @param  __cont  Container.
   */
  template <typename _Container>
    [[nodiscard]] constexpr auto
    empty(const _Container& __cont) noexcept(noexcept(__cont.empty()))
    -> decltype(__cont.empty())
    { return __cont.empty(); }

  /**
   *  @brief  Return whether an array is empty (always false).
   */
  template <typename _Tp, size_t _Nm>
    [[nodiscard]] constexpr bool
    empty(const _Tp (&)[_Nm]) noexcept
    { return false; }

  /**
   *  @brief  Return whether an initializer_list is empty.
   *  @param  __il  Initializer list.
   */
  template <typename _Tp>
    [[nodiscard]] constexpr bool
    empty(initializer_list<_Tp> __il) noexcept
    { return __il.size() == 0;}

  /**
   *  @brief  Return the data pointer of a container.
   *  @param  __cont  Container.
   */
  template <typename _Container>
    [[nodiscard]]
    constexpr auto
    data(_Container& __cont) noexcept(noexcept(__cont.data()))
    -> decltype(__cont.data())
    { return __cont.data(); }

  /**
   *  @brief  Return the data pointer of a const container.
   *  @param  __cont  Container.
   */
  template <typename _Container>
    [[nodiscard]]
    constexpr auto
    data(const _Container& __cont) noexcept(noexcept(__cont.data()))
    -> decltype(__cont.data())
    { return __cont.data(); }

  /**
   *  @brief  Return the data pointer of an array.
   *  @param  __array  Array.
   */
  template <typename _Tp, size_t _Nm>
    [[nodiscard]]
    constexpr _Tp*
    data(_Tp (&__array)[_Nm]) noexcept
    { return __array; }

  /**
   *  @brief  Return the data pointer of an initializer list.
   *  @param  __il  Initializer list.
   */
  template <typename _Tp>
    [[nodiscard]]
    constexpr const _Tp*
    data(initializer_list<_Tp> __il) noexcept
    { return __il.begin(); }

#if __cplusplus > 201703L
#define __cpp_lib_ssize 201902L
  template<typename _Container>
    [[nodiscard]]
    constexpr auto
    ssize(const _Container& __cont)
    noexcept(noexcept(__cont.size()))
    -> common_type_t<ptrdiff_t, make_signed_t<decltype(__cont.size())>>
    {
      using type = make_signed_t<decltype(__cont.size())>;
      return static_cast<common_type_t<ptrdiff_t, type>>(__cont.size());
    }

  template<typename _Tp, ptrdiff_t _Num>
    [[nodiscard]]
    constexpr ptrdiff_t
    ssize(const _Tp (&)[_Num]) noexcept
    { return _Num; }
#endif // C++20

#endif // C++17
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif // C++11
#endif // _GLIBCXX_RANGE_ACCESS_H
