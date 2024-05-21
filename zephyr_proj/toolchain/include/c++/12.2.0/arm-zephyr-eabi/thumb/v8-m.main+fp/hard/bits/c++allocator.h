// Base to std::allocator -*- C++ -*-

// Copyright (C) 2004-2022 Free Software Foundation, Inc.
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

/** @file bits/c++allocator.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{memory}
 */

#ifndef _GLIBCXX_CXX_ALLOCATOR_H
#define _GLIBCXX_CXX_ALLOCATOR_H 1

#include <bits/new_allocator.h>

#if __cplusplus >= 201103L
namespace std
{
  /**
   *  @brief  An alias to the base class for std::allocator.
   *
   *  Used to set the std::allocator base class to std::__new_allocator.
   *
   *  @ingroup allocators
   *  @tparam  _Tp  Type of allocated object.
    */
  template<typename _Tp>
    using __allocator_base = __new_allocator<_Tp>;
}
#else
// Define __new_allocator as the base class to std::allocator.
# define __allocator_base  __new_allocator
#endif

#ifndef _GLIBCXX_SANITIZE_STD_ALLOCATOR
# if defined(__SANITIZE_ADDRESS__)
#  define _GLIBCXX_SANITIZE_STD_ALLOCATOR 1
# elif defined __has_feature
#  if __has_feature(address_sanitizer)
#   define _GLIBCXX_SANITIZE_STD_ALLOCATOR 1
#  endif
# endif
#endif

#endif
