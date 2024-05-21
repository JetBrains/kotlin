// -*- C++ -*-

// Copyright (C) 2005-2022 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the terms
// of the GNU General Public License as published by the Free Software
// Foundation; either version 3, or (at your option) any later
// version.

// This library is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.

// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

// Copyright (C) 2004 Ami Tavory and Vladimir Dreizin, IBM-HRL.

// Permission to use, copy, modify, sell, and distribute this software
// is hereby granted without fee, provided that the above copyright
// notice appears in all copies, and that both that copyright notice
// and this permission notice appear in supporting documentation. None
// of the above authors, nor IBM Haifa Research Laboratories, make any
// representation about the suitability of this software for any
// purpose. It is provided "as is" without express or implied
// warranty.

/**
 * @file detail/type_utils.hpp
 * Contains utilities for handling types. All of these classes are based on
 * Modern C++ by Andrei Alxandrescu.
 */

#ifndef PB_DS_TYPE_UTILS_HPP
#define PB_DS_TYPE_UTILS_HPP

#include <cstddef>
#include <utility>
#include <tr1/type_traits>
#include <ext/type_traits.h>
#include <ext/numeric_traits.h>

namespace __gnu_pbds
{
  namespace detail
  {
    using std::tr1::is_same;
    using std::tr1::is_const;
    using std::tr1::is_pointer;
    using std::tr1::is_reference;
    using std::tr1::is_fundamental;
    using std::tr1::is_member_object_pointer;
    using std::tr1::is_member_pointer;
    using std::tr1::is_base_of;
    using std::tr1::remove_const;
    using std::tr1::remove_reference;

    // Need integral_const<bool, true> <-> integral_const<int, 1>, so
    // because of this use the following typedefs instead of importing
    // std::tr1's.
    using std::tr1::integral_constant;
    typedef std::tr1::integral_constant<int, 1> true_type;
    typedef std::tr1::integral_constant<int, 0> false_type;

    using __gnu_cxx::__conditional_type;
    using __gnu_cxx::__numeric_traits;

    template<typename T>
    struct is_const_pointer
    {
      enum
	{
	  value = is_const<T>::value && is_pointer<T>::value
	};
    };

    template<typename T>
    struct is_const_reference
    {
      enum
	{
	  value = is_const<T>::value && is_reference<T>::value
	};
    };

    template<typename T>
    struct is_simple
    {
      enum
	{
	  value = is_fundamental<typename remove_const<T>::type>::value 
	  || is_pointer<typename remove_const<T>::type>::value 
	  || is_member_pointer<T>::value 
	};
    };

    template<typename T>
    class is_pair
    {
    private:
      template<typename U>
      struct is_pair_imp
      {
	enum
	  {
	    value = 0
	  };
      };

      template<typename U, typename V>
      struct is_pair_imp<std::pair<U,V> >
      {
	enum
	  {
	    value = 1
	  };
      };

    public:
      enum
	{
	  value = is_pair_imp<T>::value
	};
    };

    // Use C++11's static_assert if possible.
#if __cplusplus >= 201103L
#define PB_DS_STATIC_ASSERT(UNIQUE, E)  static_assert(E, #UNIQUE)
#else
    template<bool>
    struct __static_assert;

    template<>
    struct __static_assert<true>
    { };

    template<int>
    struct __static_assert_dumclass
    {
      enum
	{
	  v = 1
	};
    };

#define PB_DS_STATIC_ASSERT(UNIQUE, E)  \
    typedef __gnu_pbds::detail::__static_assert_dumclass<sizeof(__gnu_pbds::detail::__static_assert<bool(E)>)> UNIQUE##__static_assert_type

#endif

    template<typename Type>
    struct type_to_type
    {
      typedef Type type;
    };
  } // namespace detail
} // namespace __gnu_pbds

#endif 
