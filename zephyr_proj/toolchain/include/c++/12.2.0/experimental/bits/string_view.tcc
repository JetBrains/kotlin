// Components for manipulating non-owning sequences of characters -*- C++ -*-

// Copyright (C) 2013-2022 Free Software Foundation, Inc.
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

/** @file experimental/bits/string_view.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{experimental/string_view}
 */

//
// N3762 basic_string_view library
//

#ifndef _GLIBCXX_EXPERIMENTAL_STRING_VIEW_TCC
#define _GLIBCXX_EXPERIMENTAL_STRING_VIEW_TCC 1

#pragma GCC system_header

#if __cplusplus >= 201402L

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

namespace experimental
{
inline namespace fundamentals_v1
{
  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    find(const _CharT* __str, size_type __pos, size_type __n) const noexcept
    {
      __glibcxx_requires_string_len(__str, __n);

      if (__n == 0)
	return __pos <= this->_M_len ? __pos : npos;

      if (__n <= this->_M_len)
	{
	  for (; __pos <= this->_M_len - __n; ++__pos)
	    if (traits_type::eq(this->_M_str[__pos], __str[0])
		&& traits_type::compare(this->_M_str + __pos + 1,
					__str + 1, __n - 1) == 0)
	      return __pos;
	}
      return npos;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    find(_CharT __c, size_type __pos) const noexcept
    {
      size_type __ret = npos;
      if (__pos < this->_M_len)
	{
	  const size_type __n = this->_M_len - __pos;
	  const _CharT* __p = traits_type::find(this->_M_str + __pos, __n, __c);
	  if (__p)
	    __ret = __p - this->_M_str;
	}
      return __ret;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    rfind(const _CharT* __str, size_type __pos, size_type __n) const noexcept
    {
      __glibcxx_requires_string_len(__str, __n);

      if (__n <= this->_M_len)
	{
	  __pos = std::min(size_type(this->_M_len - __n), __pos);
	  do
	    {
	      if (traits_type::compare(this->_M_str + __pos, __str, __n) == 0)
		return __pos;
	    }
	  while (__pos-- > 0);
	}
      return npos;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    rfind(_CharT __c, size_type __pos) const noexcept
    {
      size_type __size = this->_M_len;
      if (__size > 0)
	{
	  if (--__size > __pos)
	    __size = __pos;
	  for (++__size; __size-- > 0; )
	    if (traits_type::eq(this->_M_str[__size], __c))
	      return __size;
	}
      return npos;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    find_first_of(const _CharT* __str, size_type __pos, size_type __n) const
    {
      __glibcxx_requires_string_len(__str, __n);
      for (; __n && __pos < this->_M_len; ++__pos)
	{
	  const _CharT* __p = traits_type::find(__str, __n,
						this->_M_str[__pos]);
	  if (__p)
	    return __pos;
	}
      return npos;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    find_last_of(const _CharT* __str, size_type __pos, size_type __n) const
    {
      __glibcxx_requires_string_len(__str, __n);
      size_type __size = this->size();
      if (__size && __n)
	{
	  if (--__size > __pos)
	    __size = __pos;
	  do
	    {
	      if (traits_type::find(__str, __n, this->_M_str[__size]))
		return __size;
	    }
	  while (__size-- != 0);
	}
      return npos;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    find_first_not_of(const _CharT* __str, size_type __pos, size_type __n) const
    {
      __glibcxx_requires_string_len(__str, __n);
      for (; __pos < this->_M_len; ++__pos)
	if (!traits_type::find(__str, __n, this->_M_str[__pos]))
	  return __pos;
      return npos;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    find_first_not_of(_CharT __c, size_type __pos) const noexcept
    {
      for (; __pos < this->_M_len; ++__pos)
	if (!traits_type::eq(this->_M_str[__pos], __c))
	  return __pos;
      return npos;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    find_last_not_of(const _CharT* __str, size_type __pos, size_type __n) const
    {
      __glibcxx_requires_string_len(__str, __n);
      size_type __size = this->_M_len;
      if (__size)
	{
	  if (--__size > __pos)
	    __size = __pos;
	  do
	    {
	      if (!traits_type::find(__str, __n, this->_M_str[__size]))
		return __size;
	    }
	  while (__size--);
	}
      return npos;
    }

  template<typename _CharT, typename _Traits>
    constexpr typename basic_string_view<_CharT, _Traits>::size_type
    basic_string_view<_CharT, _Traits>::
    find_last_not_of(_CharT __c, size_type __pos) const noexcept
    {
      size_type __size = this->_M_len;
      if (__size)
	{
	  if (--__size > __pos)
	    __size = __pos;
	  do
	    {
	      if (!traits_type::eq(this->_M_str[__size], __c))
		return __size;
	    }
	  while (__size--);
	}
      return npos;
    }
} // namespace fundamentals_v1
} // namespace experimental

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif // __cplusplus <= 201103L

#endif // _GLIBCXX_EXPERIMENTAL_STRING_VIEW_TCC
