// Character Traits for use by standard string and iostream -*- C++ -*-

// Copyright (C) 1997-2022 Free Software Foundation, Inc.
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

/** @file bits/char_traits.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{string}
 */

//
// ISO C++ 14882: 21  Strings library
//

#ifndef _CHAR_TRAITS_H
#define _CHAR_TRAITS_H 1

#pragma GCC system_header

#include <bits/postypes.h>      // For streampos
#include <cwchar>               // For WEOF, wmemmove, wmemset, etc.
#if __cplusplus >= 201103L
# include <type_traits>
#endif
#if __cplusplus >= 202002L
# include <compare>
# include <bits/stl_construct.h>
#endif

#ifndef _GLIBCXX_ALWAYS_INLINE
# define _GLIBCXX_ALWAYS_INLINE inline __attribute__((__always_inline__))
#endif

namespace __gnu_cxx _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wstringop-overflow"
#pragma GCC diagnostic ignored "-Wstringop-overread"
#pragma GCC diagnostic ignored "-Warray-bounds"

  /**
   *  @brief  Mapping from character type to associated types.
   *
   *  @note This is an implementation class for the generic version
   *  of char_traits.  It defines int_type, off_type, pos_type, and
   *  state_type.  By default these are unsigned long, streamoff,
   *  streampos, and mbstate_t.  Users who need a different set of
   *  types, but who don't need to change the definitions of any function
   *  defined in char_traits, can specialize __gnu_cxx::_Char_types
   *  while leaving __gnu_cxx::char_traits alone. */
  template<typename _CharT>
    struct _Char_types
    {
      typedef unsigned long   int_type;
      typedef std::streampos  pos_type;
      typedef std::streamoff  off_type;
      typedef std::mbstate_t  state_type;
    };


  /**
   *  @brief  Base class used to implement std::char_traits.
   *
   *  @note For any given actual character type, this definition is
   *  probably wrong.  (Most of the member functions are likely to be
   *  right, but the int_type and state_type typedefs, and the eof()
   *  member function, are likely to be wrong.)  The reason this class
   *  exists is so users can specialize it.  Classes in namespace std
   *  may not be specialized for fundamental types, but classes in
   *  namespace __gnu_cxx may be.
   *
   *  See https://gcc.gnu.org/onlinedocs/libstdc++/manual/strings.html#strings.string.character_types
   *  for advice on how to make use of this class for @a unusual character
   *  types. Also, check out include/ext/pod_char_traits.h.  
   */
  template<typename _CharT>
    struct char_traits
    {
      typedef _CharT                                    char_type;
      typedef typename _Char_types<_CharT>::int_type    int_type;
      typedef typename _Char_types<_CharT>::pos_type    pos_type;
      typedef typename _Char_types<_CharT>::off_type    off_type;
      typedef typename _Char_types<_CharT>::state_type  state_type;
#if __cpp_lib_three_way_comparison
      using comparison_category = std::strong_ordering;
#endif

      static _GLIBCXX14_CONSTEXPR void
      assign(char_type& __c1, const char_type& __c2)
      {
#if __cpp_constexpr_dynamic_alloc
	if (std::__is_constant_evaluated())
	  std::construct_at(__builtin_addressof(__c1), __c2);
	else
#endif
	__c1 = __c2;
      }

      static _GLIBCXX_CONSTEXPR bool
      eq(const char_type& __c1, const char_type& __c2)
      { return __c1 == __c2; }

      static _GLIBCXX_CONSTEXPR bool
      lt(const char_type& __c1, const char_type& __c2)
      { return __c1 < __c2; }

      static _GLIBCXX14_CONSTEXPR int
      compare(const char_type* __s1, const char_type* __s2, std::size_t __n);

      static _GLIBCXX14_CONSTEXPR std::size_t
      length(const char_type* __s);

      static _GLIBCXX14_CONSTEXPR const char_type*
      find(const char_type* __s, std::size_t __n, const char_type& __a);

      static _GLIBCXX20_CONSTEXPR char_type*
      move(char_type* __s1, const char_type* __s2, std::size_t __n);

      static _GLIBCXX20_CONSTEXPR char_type*
      copy(char_type* __s1, const char_type* __s2, std::size_t __n);

      static _GLIBCXX20_CONSTEXPR char_type*
      assign(char_type* __s, std::size_t __n, char_type __a);

      static _GLIBCXX_CONSTEXPR char_type
      to_char_type(const int_type& __c)
      { return static_cast<char_type>(__c); }

      static _GLIBCXX_CONSTEXPR int_type
      to_int_type(const char_type& __c)
      { return static_cast<int_type>(__c); }

      static _GLIBCXX_CONSTEXPR bool
      eq_int_type(const int_type& __c1, const int_type& __c2)
      { return __c1 == __c2; }

      static _GLIBCXX_CONSTEXPR int_type
      eof()
      { return static_cast<int_type>(_GLIBCXX_STDIO_EOF); }

      static _GLIBCXX_CONSTEXPR int_type
      not_eof(const int_type& __c)
      { return !eq_int_type(__c, eof()) ? __c : to_int_type(char_type()); }
    };

  template<typename _CharT>
    _GLIBCXX14_CONSTEXPR int
    char_traits<_CharT>::
    compare(const char_type* __s1, const char_type* __s2, std::size_t __n)
    {
      for (std::size_t __i = 0; __i < __n; ++__i)
	if (lt(__s1[__i], __s2[__i]))
	  return -1;
	else if (lt(__s2[__i], __s1[__i]))
	  return 1;
      return 0;
    }

  template<typename _CharT>
    _GLIBCXX14_CONSTEXPR std::size_t
    char_traits<_CharT>::
    length(const char_type* __p)
    {
      std::size_t __i = 0;
      while (!eq(__p[__i], char_type()))
        ++__i;
      return __i;
    }

  template<typename _CharT>
    _GLIBCXX14_CONSTEXPR const typename char_traits<_CharT>::char_type*
    char_traits<_CharT>::
    find(const char_type* __s, std::size_t __n, const char_type& __a)
    {
      for (std::size_t __i = 0; __i < __n; ++__i)
        if (eq(__s[__i], __a))
          return __s + __i;
      return 0;
    }

  template<typename _CharT>
    _GLIBCXX20_CONSTEXPR
    typename char_traits<_CharT>::char_type*
    char_traits<_CharT>::
    move(char_type* __s1, const char_type* __s2, std::size_t __n)
    {
      if (__n == 0)
	return __s1;
#if __cplusplus >= 202002L
      if (std::__is_constant_evaluated())
	{
	  if (__s1 == __s2) // unlikely, but saves a lot of work
	    return __s1;
#if __cpp_constexpr_dynamic_alloc
	  // The overlap detection below fails due to PR c++/89074,
	  // so use a temporary buffer instead.
	  char_type* __tmp = new char_type[__n];
	  copy(__tmp, __s2, __n);
	  copy(__s1, __tmp, __n);
	  delete[] __tmp;
#else
	  const auto __end = __s2 + __n - 1;
	  bool __overlap = false;
	  for (std::size_t __i = 0; __i < __n - 1; ++__i)
	    {
	      if (__s1 + __i == __end)
		{
		  __overlap = true;
		  break;
		}
	    }
	  if (__overlap)
	    {
	      do
		{
		  --__n;
		  assign(__s1[__n], __s2[__n]);
		}
	      while (__n > 0);
	    }
	  else
	    copy(__s1, __s2, __n);
#endif
	  return __s1;
	}
#endif
      __builtin_memmove(__s1, __s2, __n * sizeof(char_type));
      return __s1;
    }

  template<typename _CharT>
    _GLIBCXX20_CONSTEXPR
    typename char_traits<_CharT>::char_type*
    char_traits<_CharT>::
    copy(char_type* __s1, const char_type* __s2, std::size_t __n)
    {
#if __cplusplus >= 202002L
      if (std::__is_constant_evaluated())
	{
	  for (std::size_t __i = 0; __i < __n; ++__i)
	    std::construct_at(__s1 + __i, __s2[__i]);
	  return __s1;
	}
#endif

      __builtin_memcpy(__s1, __s2, __n * sizeof(char_type));
      return __s1;
    }

  template<typename _CharT>
    _GLIBCXX20_CONSTEXPR
    typename char_traits<_CharT>::char_type*
    char_traits<_CharT>::
    assign(char_type* __s, std::size_t __n, char_type __a)
    {
#if __cplusplus >= 202002L
      if (std::__is_constant_evaluated())
	{
	  for (std::size_t __i = 0; __i < __n; ++__i)
	    std::construct_at(__s + __i, __a);
	  return __s;
	}
#endif

      if _GLIBCXX17_CONSTEXPR (sizeof(_CharT) == 1 && __is_trivial(_CharT))
	{
	  unsigned char __c;
	  __builtin_memcpy(&__c, __builtin_addressof(__a), 1);
	  __builtin_memset(__s, __c, __n);
	}
      else
	{
	  for (std::size_t __i = 0; __i < __n; ++__i)
	    __s[__i] = __a;
	}
      return __s;
    }

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#ifdef __cpp_lib_is_constant_evaluated
// Unofficial macro indicating P1032R1 support in C++20
# define __cpp_lib_constexpr_char_traits 201811L
#elif __cplusplus >= 201703L && _GLIBCXX_HAVE_IS_CONSTANT_EVALUATED
// Unofficial macro indicating P0426R1 support in C++17
# define __cpp_lib_constexpr_char_traits 201611L
#endif

  // 21.1
  /**
   *  @brief  Basis for explicit traits specializations.
   *
   *  @note  For any given actual character type, this definition is
   *  probably wrong.  Since this is just a thin wrapper around
   *  __gnu_cxx::char_traits, it is possible to achieve a more
   *  appropriate definition by specializing __gnu_cxx::char_traits.
   *
   *  See https://gcc.gnu.org/onlinedocs/libstdc++/manual/strings.html#strings.string.character_types
   *  for advice on how to make use of this class for @a unusual character
   *  types. Also, check out include/ext/pod_char_traits.h.
  */
  template<typename _CharT>
    struct char_traits : public __gnu_cxx::char_traits<_CharT>
    { };


  /// 21.1.3.1  char_traits specializations
  template<>
    struct char_traits<char>
    {
      typedef char              char_type;
      typedef int               int_type;
      typedef streampos         pos_type;
      typedef streamoff         off_type;
      typedef mbstate_t         state_type;
#if __cpp_lib_three_way_comparison
      using comparison_category = strong_ordering;
#endif

      static _GLIBCXX17_CONSTEXPR void
      assign(char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      {
#if __cpp_constexpr_dynamic_alloc
	if (std::__is_constant_evaluated())
	  std::construct_at(__builtin_addressof(__c1), __c2);
	else
#endif
	__c1 = __c2;
      }

      static _GLIBCXX_CONSTEXPR bool
      eq(const char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      { return __c1 == __c2; }

      static _GLIBCXX_CONSTEXPR bool
      lt(const char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      {
	// LWG 467.
	return (static_cast<unsigned char>(__c1)
		< static_cast<unsigned char>(__c2));
      }

      static _GLIBCXX17_CONSTEXPR int
      compare(const char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return 0;
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  {
	    for (size_t __i = 0; __i < __n; ++__i)
	      if (lt(__s1[__i], __s2[__i]))
		return -1;
	      else if (lt(__s2[__i], __s1[__i]))
		return 1;
	    return 0;
	  }
#endif
	return __builtin_memcmp(__s1, __s2, __n);
      }

      static _GLIBCXX17_CONSTEXPR size_t
      length(const char_type* __s)
      {
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::length(__s);
#endif
	return __builtin_strlen(__s);
      }

      static _GLIBCXX17_CONSTEXPR const char_type*
      find(const char_type* __s, size_t __n, const char_type& __a)
      {
	if (__n == 0)
	  return 0;
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::find(__s, __n, __a);
#endif
	return static_cast<const char_type*>(__builtin_memchr(__s, __a, __n));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      move(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::move(__s1, __s2, __n);
#endif
	return static_cast<char_type*>(__builtin_memmove(__s1, __s2, __n));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      copy(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::copy(__s1, __s2, __n);
#endif
	return static_cast<char_type*>(__builtin_memcpy(__s1, __s2, __n));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      assign(char_type* __s, size_t __n, char_type __a)
      {
	if (__n == 0)
	  return __s;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::assign(__s, __n, __a);
#endif
	return static_cast<char_type*>(__builtin_memset(__s, __a, __n));
      }

      static _GLIBCXX_CONSTEXPR char_type
      to_char_type(const int_type& __c) _GLIBCXX_NOEXCEPT
      { return static_cast<char_type>(__c); }

      // To keep both the byte 0xff and the eof symbol 0xffffffff
      // from ending up as 0xffffffff.
      static _GLIBCXX_CONSTEXPR int_type
      to_int_type(const char_type& __c) _GLIBCXX_NOEXCEPT
      { return static_cast<int_type>(static_cast<unsigned char>(__c)); }

      static _GLIBCXX_CONSTEXPR bool
      eq_int_type(const int_type& __c1, const int_type& __c2) _GLIBCXX_NOEXCEPT
      { return __c1 == __c2; }

      static _GLIBCXX_CONSTEXPR int_type
      eof() _GLIBCXX_NOEXCEPT
      { return static_cast<int_type>(_GLIBCXX_STDIO_EOF); }

      static _GLIBCXX_CONSTEXPR int_type
      not_eof(const int_type& __c) _GLIBCXX_NOEXCEPT
      { return (__c == eof()) ? 0 : __c; }
  };


#ifdef _GLIBCXX_USE_WCHAR_T
  /// 21.1.3.2  char_traits specializations
  template<>
    struct char_traits<wchar_t>
    {
      typedef wchar_t           char_type;
      typedef wint_t            int_type;
      typedef streamoff         off_type;
      typedef wstreampos        pos_type;
      typedef mbstate_t         state_type;
#if __cpp_lib_three_way_comparison
      using comparison_category = strong_ordering;
#endif

      static _GLIBCXX17_CONSTEXPR void
      assign(char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      {
#if __cpp_constexpr_dynamic_alloc
	if (std::__is_constant_evaluated())
	  std::construct_at(__builtin_addressof(__c1), __c2);
	else
#endif
	__c1 = __c2;
      }

      static _GLIBCXX_CONSTEXPR bool
      eq(const char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      { return __c1 == __c2; }

      static _GLIBCXX_CONSTEXPR bool
      lt(const char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      { return __c1 < __c2; }

      static _GLIBCXX17_CONSTEXPR int
      compare(const char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return 0;
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::compare(__s1, __s2, __n);
#endif
	return wmemcmp(__s1, __s2, __n);
      }

      static _GLIBCXX17_CONSTEXPR size_t
      length(const char_type* __s)
      {
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::length(__s);
#endif
	return wcslen(__s);
      }

      static _GLIBCXX17_CONSTEXPR const char_type*
      find(const char_type* __s, size_t __n, const char_type& __a)
      {
	if (__n == 0)
	  return 0;
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::find(__s, __n, __a);
#endif
	return wmemchr(__s, __a, __n);
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      move(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::move(__s1, __s2, __n);
#endif
	return wmemmove(__s1, __s2, __n);
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      copy(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::copy(__s1, __s2, __n);
#endif
	return wmemcpy(__s1, __s2, __n);
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      assign(char_type* __s, size_t __n, char_type __a)
      {
	if (__n == 0)
	  return __s;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::assign(__s, __n, __a);
#endif
	return wmemset(__s, __a, __n);
      }

      static _GLIBCXX_CONSTEXPR char_type
      to_char_type(const int_type& __c) _GLIBCXX_NOEXCEPT
      { return char_type(__c); }

      static _GLIBCXX_CONSTEXPR int_type
      to_int_type(const char_type& __c) _GLIBCXX_NOEXCEPT
      { return int_type(__c); }

      static _GLIBCXX_CONSTEXPR bool
      eq_int_type(const int_type& __c1, const int_type& __c2) _GLIBCXX_NOEXCEPT
      { return __c1 == __c2; }

      static _GLIBCXX_CONSTEXPR int_type
      eof() _GLIBCXX_NOEXCEPT
      { return static_cast<int_type>(WEOF); }

      static _GLIBCXX_CONSTEXPR int_type
      not_eof(const int_type& __c) _GLIBCXX_NOEXCEPT
      { return eq_int_type(__c, eof()) ? 0 : __c; }
  };
#else // _GLIBCXX_USE_WCHAR_T
  template<>
    struct char_traits<wchar_t> : public __gnu_cxx::char_traits<wchar_t>
    { };
#endif //_GLIBCXX_USE_WCHAR_T

#ifdef _GLIBCXX_USE_CHAR8_T
  template<>
    struct char_traits<char8_t>
    {
      typedef char8_t           char_type;
      typedef unsigned int      int_type;
      typedef u8streampos       pos_type;
      typedef streamoff         off_type;
      typedef mbstate_t         state_type;
#if __cpp_lib_three_way_comparison
      using comparison_category = strong_ordering;
#endif

      static _GLIBCXX17_CONSTEXPR void
      assign(char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      {
#if __cpp_constexpr_dynamic_alloc
	if (std::__is_constant_evaluated())
	  std::construct_at(__builtin_addressof(__c1), __c2);
	else
#endif
	__c1 = __c2;
      }

      static _GLIBCXX_CONSTEXPR bool
      eq(const char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      { return __c1 == __c2; }

      static _GLIBCXX_CONSTEXPR bool
      lt(const char_type& __c1, const char_type& __c2) _GLIBCXX_NOEXCEPT
      { return __c1 < __c2; }

      static _GLIBCXX17_CONSTEXPR int
      compare(const char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return 0;
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::compare(__s1, __s2, __n);
#endif
	return __builtin_memcmp(__s1, __s2, __n);
      }

      static _GLIBCXX17_CONSTEXPR size_t
      length(const char_type* __s)
      {
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::length(__s);
#endif
	size_t __i = 0;
	while (!eq(__s[__i], char_type()))
	  ++__i;
	return __i;
      }

      static _GLIBCXX17_CONSTEXPR const char_type*
      find(const char_type* __s, size_t __n, const char_type& __a)
      {
	if (__n == 0)
	  return 0;
#if __cplusplus >= 201703L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::find(__s, __n, __a);
#endif
	return static_cast<const char_type*>(__builtin_memchr(__s, __a, __n));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      move(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::move(__s1, __s2, __n);
#endif
	return static_cast<char_type*>(__builtin_memmove(__s1, __s2, __n));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      copy(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::copy(__s1, __s2, __n);
#endif
	return static_cast<char_type*>(__builtin_memcpy(__s1, __s2, __n));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      assign(char_type* __s, size_t __n, char_type __a)
      {
	if (__n == 0)
	  return __s;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::assign(__s, __n, __a);
#endif
	return static_cast<char_type*>(__builtin_memset(__s, __a, __n));
      }

      static _GLIBCXX_CONSTEXPR char_type
      to_char_type(const int_type& __c) _GLIBCXX_NOEXCEPT
      { return char_type(__c); }

      static _GLIBCXX_CONSTEXPR int_type
      to_int_type(const char_type& __c) _GLIBCXX_NOEXCEPT
      { return int_type(__c); }

      static _GLIBCXX_CONSTEXPR bool
      eq_int_type(const int_type& __c1, const int_type& __c2) _GLIBCXX_NOEXCEPT
      { return __c1 == __c2; }

      static _GLIBCXX_CONSTEXPR int_type
      eof() _GLIBCXX_NOEXCEPT
      { return static_cast<int_type>(-1); }

      static _GLIBCXX_CONSTEXPR int_type
      not_eof(const int_type& __c) _GLIBCXX_NOEXCEPT
      { return eq_int_type(__c, eof()) ? 0 : __c; }
    };
#endif //_GLIBCXX_USE_CHAR8_T

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#if __cplusplus >= 201103L

#include <cstdint>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  template<>
    struct char_traits<char16_t>
    {
      typedef char16_t          char_type;
#ifdef _GLIBCXX_USE_C99_STDINT_TR1
      typedef uint_least16_t    int_type;
#elif defined __UINT_LEAST16_TYPE__
      typedef __UINT_LEAST16_TYPE__	    int_type;
#else
      typedef make_unsigned<char16_t>::type int_type;
#endif
      typedef streamoff         off_type;
      typedef u16streampos      pos_type;
      typedef mbstate_t         state_type;
#if __cpp_lib_three_way_comparison
      using comparison_category = strong_ordering;
#endif

      static _GLIBCXX17_CONSTEXPR void
      assign(char_type& __c1, const char_type& __c2) noexcept
      {
#if __cpp_constexpr_dynamic_alloc
	if (std::__is_constant_evaluated())
	  std::construct_at(__builtin_addressof(__c1), __c2);
	else
#endif
	__c1 = __c2;
      }

      static constexpr bool
      eq(const char_type& __c1, const char_type& __c2) noexcept
      { return __c1 == __c2; }

      static constexpr bool
      lt(const char_type& __c1, const char_type& __c2) noexcept
      { return __c1 < __c2; }

      static _GLIBCXX17_CONSTEXPR int
      compare(const char_type* __s1, const char_type* __s2, size_t __n)
      {
	for (size_t __i = 0; __i < __n; ++__i)
	  if (lt(__s1[__i], __s2[__i]))
	    return -1;
	  else if (lt(__s2[__i], __s1[__i]))
	    return 1;
	return 0;
      }

      static _GLIBCXX17_CONSTEXPR size_t
      length(const char_type* __s)
      {
	size_t __i = 0;
	while (!eq(__s[__i], char_type()))
	  ++__i;
	return __i;
      }

      static _GLIBCXX17_CONSTEXPR const char_type*
      find(const char_type* __s, size_t __n, const char_type& __a)
      {
	for (size_t __i = 0; __i < __n; ++__i)
	  if (eq(__s[__i], __a))
	    return __s + __i;
	return 0;
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      move(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::move(__s1, __s2, __n);
#endif
	return (static_cast<char_type*>
		(__builtin_memmove(__s1, __s2, __n * sizeof(char_type))));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      copy(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::copy(__s1, __s2, __n);
#endif
	return (static_cast<char_type*>
		(__builtin_memcpy(__s1, __s2, __n * sizeof(char_type))));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      assign(char_type* __s, size_t __n, char_type __a)
      {
	for (size_t __i = 0; __i < __n; ++__i)
	  assign(__s[__i], __a);
	return __s;
      }

      static constexpr char_type
      to_char_type(const int_type& __c) noexcept
      { return char_type(__c); }

      static constexpr int_type
      to_int_type(const char_type& __c) noexcept
      { return __c == eof() ? int_type(0xfffd) : int_type(__c); }

      static constexpr bool
      eq_int_type(const int_type& __c1, const int_type& __c2) noexcept
      { return __c1 == __c2; }

      static constexpr int_type
      eof() noexcept
      { return static_cast<int_type>(-1); }

      static constexpr int_type
      not_eof(const int_type& __c) noexcept
      { return eq_int_type(__c, eof()) ? 0 : __c; }
    };

  template<>
    struct char_traits<char32_t>
    {
      typedef char32_t          char_type;
#ifdef _GLIBCXX_USE_C99_STDINT_TR1
      typedef uint_least32_t    int_type;
#elif defined __UINT_LEAST32_TYPE__
      typedef __UINT_LEAST32_TYPE__	    int_type;
#else
      typedef make_unsigned<char32_t>::type int_type;
#endif
      typedef streamoff         off_type;
      typedef u32streampos      pos_type;
      typedef mbstate_t         state_type;
#if __cpp_lib_three_way_comparison
      using comparison_category = strong_ordering;
#endif

      static _GLIBCXX17_CONSTEXPR void
      assign(char_type& __c1, const char_type& __c2) noexcept
      {
#if __cpp_constexpr_dynamic_alloc
	if (std::__is_constant_evaluated())
	  std::construct_at(__builtin_addressof(__c1), __c2);
	else
#endif
	__c1 = __c2;
      }

      static constexpr bool
      eq(const char_type& __c1, const char_type& __c2) noexcept
      { return __c1 == __c2; }

      static constexpr bool
      lt(const char_type& __c1, const char_type& __c2) noexcept
      { return __c1 < __c2; }

      static _GLIBCXX17_CONSTEXPR int
      compare(const char_type* __s1, const char_type* __s2, size_t __n)
      {
	for (size_t __i = 0; __i < __n; ++__i)
	  if (lt(__s1[__i], __s2[__i]))
	    return -1;
	  else if (lt(__s2[__i], __s1[__i]))
	    return 1;
	return 0;
      }

      static _GLIBCXX17_CONSTEXPR size_t
      length(const char_type* __s)
      {
	size_t __i = 0;
	while (!eq(__s[__i], char_type()))
	  ++__i;
	return __i;
      }

      static _GLIBCXX17_CONSTEXPR const char_type*
      find(const char_type* __s, size_t __n, const char_type& __a)
      {
	for (size_t __i = 0; __i < __n; ++__i)
	  if (eq(__s[__i], __a))
	    return __s + __i;
	return 0;
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      move(char_type* __s1, const char_type* __s2, size_t __n)
      {
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::move(__s1, __s2, __n);
#endif
	return (static_cast<char_type*>
		(__builtin_memmove(__s1, __s2, __n * sizeof(char_type))));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      copy(char_type* __s1, const char_type* __s2, size_t __n)
      { 
	if (__n == 0)
	  return __s1;
#if __cplusplus >= 202002L
	if (std::__is_constant_evaluated())
	  return __gnu_cxx::char_traits<char_type>::copy(__s1, __s2, __n);
#endif
	return (static_cast<char_type*>
		(__builtin_memcpy(__s1, __s2, __n * sizeof(char_type))));
      }

      static _GLIBCXX20_CONSTEXPR char_type*
      assign(char_type* __s, size_t __n, char_type __a)
      {
	for (size_t __i = 0; __i < __n; ++__i)
	  assign(__s[__i], __a);
	return __s;
      }

      static constexpr char_type
      to_char_type(const int_type& __c) noexcept
      { return char_type(__c); }

      static constexpr int_type
      to_int_type(const char_type& __c) noexcept
      { return int_type(__c); }

      static constexpr bool
      eq_int_type(const int_type& __c1, const int_type& __c2) noexcept
      { return __c1 == __c2; }

      static constexpr int_type
      eof() noexcept
      { return static_cast<int_type>(-1); }

      static constexpr int_type
      not_eof(const int_type& __c) noexcept
      { return eq_int_type(__c, eof()) ? 0 : __c; }
    };

#if __cpp_lib_three_way_comparison
  namespace __detail
  {
    template<typename _ChTraits>
      constexpr auto
      __char_traits_cmp_cat(int __cmp) noexcept
      {
	if constexpr (requires { typename _ChTraits::comparison_category; })
	  {
	    using _Cat = typename _ChTraits::comparison_category;
	    static_assert( !is_void_v<common_comparison_category_t<_Cat>> );
	    return static_cast<_Cat>(__cmp <=> 0);
	  }
	else
	  return static_cast<weak_ordering>(__cmp <=> 0);
      }
  } // namespace __detail
#endif // C++20

#pragma GCC diagnostic pop

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif  // C++11

#endif // _CHAR_TRAITS_H
