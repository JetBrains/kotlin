// Components for manipulating sequences of characters -*- C++ -*-

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

/** @file bits/basic_string.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{string}
 */

//
// ISO C++ 14882: 21  Strings library
//

// Written by Jason Merrill based upon the specification by Takanori Adachi
// in ANSI X3J16/94-0013R2.  Rewritten by Nathan Myers to ISO-14882.
// Non-reference-counted implementation written by Paolo Carlini and
// updated by Jonathan Wakely for ISO-14882-2011.

#ifndef _BASIC_STRING_TCC
#define _BASIC_STRING_TCC 1

#pragma GCC system_header

#include <bits/cxxabi_forced.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#if _GLIBCXX_USE_CXX11_ABI

  template<typename _CharT, typename _Traits, typename _Alloc>
    const typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::npos;

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    void
    basic_string<_CharT, _Traits, _Alloc>::
    swap(basic_string& __s) _GLIBCXX_NOEXCEPT
    {
      if (this == std::__addressof(__s))
	return;

      _Alloc_traits::_S_on_swap(_M_get_allocator(), __s._M_get_allocator());

      if (_M_is_local())
	if (__s._M_is_local())
	  {
	    if (length() && __s.length())
	      {
		_CharT __tmp_data[_S_local_capacity + 1];
		traits_type::copy(__tmp_data, __s._M_local_buf,
				  __s.length() + 1);
		traits_type::copy(__s._M_local_buf, _M_local_buf,
				  length() + 1);
		traits_type::copy(_M_local_buf, __tmp_data,
				  __s.length() + 1);
	      }
	    else if (__s.length())
	      {
		traits_type::copy(_M_local_buf, __s._M_local_buf,
				  __s.length() + 1);
		_M_length(__s.length());
		__s._M_set_length(0);
		return;
	      }
	    else if (length())
	      {
		traits_type::copy(__s._M_local_buf, _M_local_buf,
				  length() + 1);
		__s._M_length(length());
		_M_set_length(0);
		return;
	      }
	  }
	else
	  {
	    const size_type __tmp_capacity = __s._M_allocated_capacity;
	    traits_type::copy(__s._M_local_buf, _M_local_buf,
			      length() + 1);
	    _M_data(__s._M_data());
	    __s._M_data(__s._M_local_buf);
	    _M_capacity(__tmp_capacity);
	  }
      else
	{
	  const size_type __tmp_capacity = _M_allocated_capacity;
	  if (__s._M_is_local())
	    {
	      traits_type::copy(_M_local_buf, __s._M_local_buf,
				__s.length() + 1);
	      __s._M_data(_M_data());
	      _M_data(_M_local_buf);
	    }
	  else
	    {
	      pointer __tmp_ptr = _M_data();
	      _M_data(__s._M_data());
	      __s._M_data(__tmp_ptr);
	      _M_capacity(__s._M_allocated_capacity);
	    }
	  __s._M_capacity(__tmp_capacity);
	}

      const size_type __tmp_length = length();
      _M_length(__s.length());
      __s._M_length(__tmp_length);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::pointer
    basic_string<_CharT, _Traits, _Alloc>::
    _M_create(size_type& __capacity, size_type __old_capacity)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 83.  String::npos vs. string::max_size()
      if (__capacity > max_size())
	std::__throw_length_error(__N("basic_string::_M_create"));

      // The below implements an exponential growth policy, necessary to
      // meet amortized linear time requirements of the library: see
      // http://gcc.gnu.org/ml/libstdc++/2001-07/msg00085.html.
      if (__capacity > __old_capacity && __capacity < 2 * __old_capacity)
	{
	  __capacity = 2 * __old_capacity;
	  // Never allocate a string bigger than max_size.
	  if (__capacity > max_size())
	    __capacity = max_size();
	}

      // NB: Need an array of char_type[__capacity], plus a terminating
      // null char_type() element.
      return _Alloc_traits::allocate(_M_get_allocator(), __capacity + 1);
    }

  // NB: This is the special case for Input Iterators, used in
  // istreambuf_iterators, etc.
  // Input Iterators have a cost structure very different from
  // pointers, calling for a different coding style.
  template<typename _CharT, typename _Traits, typename _Alloc>
    template<typename _InIterator>
      _GLIBCXX20_CONSTEXPR
      void
      basic_string<_CharT, _Traits, _Alloc>::
      _M_construct(_InIterator __beg, _InIterator __end,
		   std::input_iterator_tag)
      {
	size_type __len = 0;
	size_type __capacity = size_type(_S_local_capacity);

	pointer __p = _M_use_local_data();

	while (__beg != __end && __len < __capacity)
	  {
	    __p[__len++] = *__beg;
	    ++__beg;
	  }

	struct _Guard
	{
	  _GLIBCXX20_CONSTEXPR
	  explicit _Guard(basic_string* __s) : _M_guarded(__s) { }

	  _GLIBCXX20_CONSTEXPR
	  ~_Guard() { if (_M_guarded) _M_guarded->_M_dispose(); }

	  basic_string* _M_guarded;
	} __guard(this);

	while (__beg != __end)
	  {
	    if (__len == __capacity)
	      {
		// Allocate more space.
		__capacity = __len + 1;
		pointer __another = _M_create(__capacity, __len);
		this->_S_copy(__another, _M_data(), __len);
		_M_dispose();
		_M_data(__another);
		_M_capacity(__capacity);
	      }
	    traits_type::assign(_M_data()[__len++], *__beg);
	    ++__beg;
	  }

	__guard._M_guarded = 0;

	_M_set_length(__len);
      }

  template<typename _CharT, typename _Traits, typename _Alloc>
    template<typename _InIterator>
      _GLIBCXX20_CONSTEXPR
      void
      basic_string<_CharT, _Traits, _Alloc>::
      _M_construct(_InIterator __beg, _InIterator __end,
		   std::forward_iterator_tag)
      {
	size_type __dnew = static_cast<size_type>(std::distance(__beg, __end));

	if (__dnew > size_type(_S_local_capacity))
	  {
	    _M_data(_M_create(__dnew, size_type(0)));
	    _M_capacity(__dnew);
	  }
	else
	  _M_use_local_data();

	// Check for out_of_range and length_error exceptions.
	struct _Guard
	{
	  _GLIBCXX20_CONSTEXPR
	  explicit _Guard(basic_string* __s) : _M_guarded(__s) { }

	  _GLIBCXX20_CONSTEXPR
	  ~_Guard() { if (_M_guarded) _M_guarded->_M_dispose(); }

	  basic_string* _M_guarded;
	} __guard(this);

	this->_S_copy_chars(_M_data(), __beg, __end);

	__guard._M_guarded = 0;

	_M_set_length(__dnew);
      }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    void
    basic_string<_CharT, _Traits, _Alloc>::
    _M_construct(size_type __n, _CharT __c)
    {
      if (__n > size_type(_S_local_capacity))
	{
	  _M_data(_M_create(__n, size_type(0)));
	  _M_capacity(__n);
	}
      else
	_M_use_local_data();

      if (__n)
	this->_S_assign(_M_data(), __n, __c);

      _M_set_length(__n);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    void
    basic_string<_CharT, _Traits, _Alloc>::
    _M_assign(const basic_string& __str)
    {
      if (this != std::__addressof(__str))
	{
	  const size_type __rsize = __str.length();
	  const size_type __capacity = capacity();

	  if (__rsize > __capacity)
	    {
	      size_type __new_capacity = __rsize;
	      pointer __tmp = _M_create(__new_capacity, __capacity);
	      _M_dispose();
	      _M_data(__tmp);
	      _M_capacity(__new_capacity);
	    }

	  if (__rsize)
	    this->_S_copy(_M_data(), __str._M_data(), __rsize);

	  _M_set_length(__rsize);
	}
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    void
    basic_string<_CharT, _Traits, _Alloc>::
    reserve(size_type __res)
    {
      const size_type __capacity = capacity();
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 2968. Inconsistencies between basic_string reserve and
      // vector/unordered_map/unordered_set reserve functions
      // P0966 reserve should not shrink
      if (__res <= __capacity)
	return;

      pointer __tmp = _M_create(__res, __capacity);
      this->_S_copy(__tmp, _M_data(), length() + 1);
      _M_dispose();
      _M_data(__tmp);
      _M_capacity(__res);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    void
    basic_string<_CharT, _Traits, _Alloc>::
    _M_mutate(size_type __pos, size_type __len1, const _CharT* __s,
	      size_type __len2)
    {
      const size_type __how_much = length() - __pos - __len1;

      size_type __new_capacity = length() + __len2 - __len1;
      pointer __r = _M_create(__new_capacity, capacity());

      if (__pos)
	this->_S_copy(__r, _M_data(), __pos);
      if (__s && __len2)
	this->_S_copy(__r + __pos, __s, __len2);
      if (__how_much)
	this->_S_copy(__r + __pos + __len2,
		      _M_data() + __pos + __len1, __how_much);

      _M_dispose();
      _M_data(__r);
      _M_capacity(__new_capacity);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    void
    basic_string<_CharT, _Traits, _Alloc>::
    _M_erase(size_type __pos, size_type __n)
    {
      const size_type __how_much = length() - __pos - __n;

      if (__how_much && __n)
	this->_S_move(_M_data() + __pos, _M_data() + __pos + __n, __how_much);

      _M_set_length(length() - __n);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    void
    basic_string<_CharT, _Traits, _Alloc>::
    reserve()
    {
      if (_M_is_local())
	return;

      const size_type __length = length();
      const size_type __capacity = _M_allocated_capacity;

      if (__length <= size_type(_S_local_capacity))
	{
	  this->_S_copy(_M_use_local_data(), _M_data(), __length + 1);
	  _M_destroy(__capacity);
	  _M_data(_M_local_data());
	}
#if __cpp_exceptions
      else if (__length < __capacity)
	try
	  {
	    pointer __tmp
	      = _Alloc_traits::allocate(_M_get_allocator(), __length + 1);
	    this->_S_copy(__tmp, _M_data(), __length + 1);
	    _M_dispose();
	    _M_data(__tmp);
	    _M_capacity(__length);
	  }
	catch (const __cxxabiv1::__forced_unwind&)
	  { throw; }
	catch (...)
	  { /* swallow the exception */ }
#endif
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    void
    basic_string<_CharT, _Traits, _Alloc>::
    resize(size_type __n, _CharT __c)
    {
      const size_type __size = this->size();
      if (__size < __n)
	this->append(__n - __size, __c);
      else if (__n < __size)
	this->_M_set_length(__n);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    _M_append(const _CharT* __s, size_type __n)
    {
      const size_type __len = __n + this->size();

      if (__len <= this->capacity())
	{
	  if (__n)
	    this->_S_copy(this->_M_data() + this->size(), __s, __n);
	}
      else
	this->_M_mutate(this->size(), size_type(0), __s, __n);

      this->_M_set_length(__len);
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    template<typename _InputIterator>
      _GLIBCXX20_CONSTEXPR
      basic_string<_CharT, _Traits, _Alloc>&
      basic_string<_CharT, _Traits, _Alloc>::
      _M_replace_dispatch(const_iterator __i1, const_iterator __i2,
			  _InputIterator __k1, _InputIterator __k2,
			  std::__false_type)
      {
	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 2788. unintentionally require a default constructible allocator
	const basic_string __s(__k1, __k2, this->get_allocator());
	const size_type __n1 = __i2 - __i1;
	return _M_replace(__i1 - begin(), __n1, __s._M_data(),
			  __s.size());
      }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    _M_replace_aux(size_type __pos1, size_type __n1, size_type __n2,
		   _CharT __c)
    {
      _M_check_length(__n1, __n2, "basic_string::_M_replace_aux");

      const size_type __old_size = this->size();
      const size_type __new_size = __old_size + __n2 - __n1;

      if (__new_size <= this->capacity())
	{
	  pointer __p = this->_M_data() + __pos1;

	  const size_type __how_much = __old_size - __pos1 - __n1;
	  if (__how_much && __n1 != __n2)
	    this->_S_move(__p + __n2, __p + __n1, __how_much);
	}
      else
	this->_M_mutate(__pos1, __n1, 0, __n2);

      if (__n2)
	this->_S_assign(this->_M_data() + __pos1, __n2, __c);

      this->_M_set_length(__new_size);
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    _M_replace(size_type __pos, size_type __len1, const _CharT* __s,
	       const size_type __len2)
    {
      _M_check_length(__len1, __len2, "basic_string::_M_replace");

      const size_type __old_size = this->size();
      const size_type __new_size = __old_size + __len2 - __len1;

      if (__new_size <= this->capacity())
	{
	  pointer __p = this->_M_data() + __pos;

	  const size_type __how_much = __old_size - __pos - __len1;
#if __cpp_lib_is_constant_evaluated
	  if (std::is_constant_evaluated())
	    {
	      auto __newp = _Alloc_traits::allocate(_M_get_allocator(),
						    __new_size);
	      _S_copy(__newp, this->_M_data(), __pos);
	      _S_copy(__newp + __pos, __s, __len2);
	      _S_copy(__newp + __pos + __len2, __p + __len1, __how_much);
	      _S_copy(this->_M_data(), __newp, __new_size);
	      this->_M_get_allocator().deallocate(__newp, __new_size);
	    }
	  else
#endif
	  if (_M_disjunct(__s))
	    {
	      if (__how_much && __len1 != __len2)
		this->_S_move(__p + __len2, __p + __len1, __how_much);
	      if (__len2)
		this->_S_copy(__p, __s, __len2);
	    }
	  else
	    {
	      // Work in-place.
	      if (__len2 && __len2 <= __len1)
		this->_S_move(__p, __s, __len2);
	      if (__how_much && __len1 != __len2)
		this->_S_move(__p + __len2, __p + __len1, __how_much);
	      if (__len2 > __len1)
		{
		  if (__s + __len2 <= __p + __len1)
		    this->_S_move(__p, __s, __len2);
		  else if (__s >= __p + __len1)
		    {
		      // Hint to middle end that __p and __s overlap
		      // (PR 98465).
		      const size_type __poff = (__s - __p) + (__len2 - __len1);
		      this->_S_copy(__p, __p + __poff, __len2);
		    }
		  else
		    {
		      const size_type __nleft = (__p + __len1) - __s;
		      this->_S_move(__p, __s, __nleft);
		      this->_S_copy(__p + __nleft, __p + __len2,
				    __len2 - __nleft);
		    }
		}
	    }
	}
      else
	this->_M_mutate(__pos, __len1, __s, __len2);

      this->_M_set_length(__new_size);
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    copy(_CharT* __s, size_type __n, size_type __pos) const
    {
      _M_check(__pos, "basic_string::copy");
      __n = _M_limit(__pos, __n);
      __glibcxx_requires_string_len(__s, __n);
      if (__n)
	_S_copy(__s, _M_data() + __pos, __n);
      // 21.3.5.7 par 3: do not append null.  (good.)
      return __n;
    }

#if __cplusplus > 202002L
  template<typename _CharT, typename _Traits, typename _Alloc>
  template<typename _Operation>
    constexpr void
    basic_string<_CharT, _Traits, _Alloc>::
    resize_and_overwrite(size_type __n, _Operation __op)
    {
      const size_type __capacity = capacity();
      _CharT* __p;
      if (__n > __capacity)
	{
	  __p = _M_create(__n, __capacity);
	  this->_S_copy(__p, _M_data(), length()); // exclude trailing null
#if __cpp_lib_is_constant_evaluated
	  if (std::is_constant_evaluated())
	    traits_type::assign(__p + length(), __n - length(), _CharT());
#endif
	  _M_dispose();
	  _M_data(__p);
	  _M_capacity(__n);
	}
      else
	__p = _M_data();
      struct _Terminator {
	constexpr ~_Terminator() { _M_this->_M_set_length(_M_r); }
	basic_string* _M_this;
	size_type _M_r;
      };
      _Terminator __term{this};
      const size_type __n2 [[maybe_unused]] = __n;
      __term._M_r = std::move(__op)(__p, __n);
      _GLIBCXX_DEBUG_ASSERT(__term._M_r >= 0 && __term._M_r <= __n2);
    }
#endif // C++23

#endif  // _GLIBCXX_USE_CXX11_ABI
   
#if __cpp_lib_constexpr_string >= 201907L
# define _GLIBCXX_STRING_CONSTEXPR constexpr
#else
# define _GLIBCXX_STRING_CONSTEXPR
#endif

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    basic_string<_CharT, _Traits, _Alloc>
    operator+(const _CharT* __lhs,
	      const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    {
      __glibcxx_requires_string(__lhs);
      typedef basic_string<_CharT, _Traits, _Alloc> __string_type;
      typedef typename __string_type::size_type	  __size_type;
      typedef typename __gnu_cxx::__alloc_traits<_Alloc>::template
	rebind<_CharT>::other _Char_alloc_type;
      typedef __gnu_cxx::__alloc_traits<_Char_alloc_type> _Alloc_traits;
      const __size_type __len = _Traits::length(__lhs);
      __string_type __str(_Alloc_traits::_S_select_on_copy(
          __rhs.get_allocator()));
      __str.reserve(__len + __rhs.size());
      __str.append(__lhs, __len);
      __str.append(__rhs);
      return __str;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    basic_string<_CharT, _Traits, _Alloc>
    operator+(_CharT __lhs, const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    {
      typedef basic_string<_CharT, _Traits, _Alloc> __string_type;
      typedef typename __string_type::size_type	  __size_type;
      typedef typename __gnu_cxx::__alloc_traits<_Alloc>::template
	rebind<_CharT>::other _Char_alloc_type;
      typedef __gnu_cxx::__alloc_traits<_Char_alloc_type> _Alloc_traits;
      __string_type __str(_Alloc_traits::_S_select_on_copy(
          __rhs.get_allocator()));
      const __size_type __len = __rhs.size();
      __str.reserve(__len + 1);
      __str.append(__size_type(1), __lhs);
      __str.append(__rhs);
      return __str;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    find(const _CharT* __s, size_type __pos, size_type __n) const
    _GLIBCXX_NOEXCEPT
    {
      __glibcxx_requires_string_len(__s, __n);
      const size_type __size = this->size();

      if (__n == 0)
	return __pos <= __size ? __pos : npos;
      if (__pos >= __size)
	return npos;

      const _CharT __elem0 = __s[0];
      const _CharT* const __data = data();
      const _CharT* __first = __data + __pos;
      const _CharT* const __last = __data + __size;
      size_type __len = __size - __pos;

      while (__len >= __n)
	{
	  // Find the first occurrence of __elem0:
	  __first = traits_type::find(__first, __len - __n + 1, __elem0);
	  if (!__first)
	    return npos;
	  // Compare the full strings from the first occurrence of __elem0.
	  // We already know that __first[0] == __s[0] but compare them again
	  // anyway because __s is probably aligned, which helps memcmp.
	  if (traits_type::compare(__first, __s, __n) == 0)
	    return __first - __data;
	  __len = __last - ++__first;
	}
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    find(_CharT __c, size_type __pos) const _GLIBCXX_NOEXCEPT
    {
      size_type __ret = npos;
      const size_type __size = this->size();
      if (__pos < __size)
	{
	  const _CharT* __data = _M_data();
	  const size_type __n = __size - __pos;
	  const _CharT* __p = traits_type::find(__data + __pos, __n, __c);
	  if (__p)
	    __ret = __p - __data;
	}
      return __ret;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    rfind(const _CharT* __s, size_type __pos, size_type __n) const
    _GLIBCXX_NOEXCEPT
    {
      __glibcxx_requires_string_len(__s, __n);
      const size_type __size = this->size();
      if (__n <= __size)
	{
	  __pos = std::min(size_type(__size - __n), __pos);
	  const _CharT* __data = _M_data();
	  do
	    {
	      if (traits_type::compare(__data + __pos, __s, __n) == 0)
		return __pos;
	    }
	  while (__pos-- > 0);
	}
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    rfind(_CharT __c, size_type __pos) const _GLIBCXX_NOEXCEPT
    {
      size_type __size = this->size();
      if (__size)
	{
	  if (--__size > __pos)
	    __size = __pos;
	  for (++__size; __size-- > 0; )
	    if (traits_type::eq(_M_data()[__size], __c))
	      return __size;
	}
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    find_first_of(const _CharT* __s, size_type __pos, size_type __n) const
    _GLIBCXX_NOEXCEPT
    {
      __glibcxx_requires_string_len(__s, __n);
      for (; __n && __pos < this->size(); ++__pos)
	{
	  const _CharT* __p = traits_type::find(__s, __n, _M_data()[__pos]);
	  if (__p)
	    return __pos;
	}
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    find_last_of(const _CharT* __s, size_type __pos, size_type __n) const
    _GLIBCXX_NOEXCEPT
    {
      __glibcxx_requires_string_len(__s, __n);
      size_type __size = this->size();
      if (__size && __n)
	{
	  if (--__size > __pos)
	    __size = __pos;
	  do
	    {
	      if (traits_type::find(__s, __n, _M_data()[__size]))
		return __size;
	    }
	  while (__size-- != 0);
	}
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    find_first_not_of(const _CharT* __s, size_type __pos, size_type __n) const
    _GLIBCXX_NOEXCEPT
    {
      __glibcxx_requires_string_len(__s, __n);
      for (; __pos < this->size(); ++__pos)
	if (!traits_type::find(__s, __n, _M_data()[__pos]))
	  return __pos;
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    find_first_not_of(_CharT __c, size_type __pos) const _GLIBCXX_NOEXCEPT
    {
      for (; __pos < this->size(); ++__pos)
	if (!traits_type::eq(_M_data()[__pos], __c))
	  return __pos;
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    find_last_not_of(const _CharT* __s, size_type __pos, size_type __n) const
    _GLIBCXX_NOEXCEPT
    {
      __glibcxx_requires_string_len(__s, __n);
      size_type __size = this->size();
      if (__size)
	{
	  if (--__size > __pos)
	    __size = __pos;
	  do
	    {
	      if (!traits_type::find(__s, __n, _M_data()[__size]))
		return __size;
	    }
	  while (__size--);
	}
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    find_last_not_of(_CharT __c, size_type __pos) const _GLIBCXX_NOEXCEPT
    {
      size_type __size = this->size();
      if (__size)
	{
	  if (--__size > __pos)
	    __size = __pos;
	  do
	    {
	      if (!traits_type::eq(_M_data()[__size], __c))
		return __size;
	    }
	  while (__size--);
	}
      return npos;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    int
    basic_string<_CharT, _Traits, _Alloc>::
    compare(size_type __pos, size_type __n, const basic_string& __str) const
    {
      _M_check(__pos, "basic_string::compare");
      __n = _M_limit(__pos, __n);
      const size_type __osize = __str.size();
      const size_type __len = std::min(__n, __osize);
      int __r = traits_type::compare(_M_data() + __pos, __str.data(), __len);
      if (!__r)
	__r = _S_compare(__n, __osize);
      return __r;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    int
    basic_string<_CharT, _Traits, _Alloc>::
    compare(size_type __pos1, size_type __n1, const basic_string& __str,
	    size_type __pos2, size_type __n2) const
    {
      _M_check(__pos1, "basic_string::compare");
      __str._M_check(__pos2, "basic_string::compare");
      __n1 = _M_limit(__pos1, __n1);
      __n2 = __str._M_limit(__pos2, __n2);
      const size_type __len = std::min(__n1, __n2);
      int __r = traits_type::compare(_M_data() + __pos1,
				     __str.data() + __pos2, __len);
      if (!__r)
	__r = _S_compare(__n1, __n2);
      return __r;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    int
    basic_string<_CharT, _Traits, _Alloc>::
    compare(const _CharT* __s) const _GLIBCXX_NOEXCEPT
    {
      __glibcxx_requires_string(__s);
      const size_type __size = this->size();
      const size_type __osize = traits_type::length(__s);
      const size_type __len = std::min(__size, __osize);
      int __r = traits_type::compare(_M_data(), __s, __len);
      if (!__r)
	__r = _S_compare(__size, __osize);
      return __r;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    int
    basic_string <_CharT, _Traits, _Alloc>::
    compare(size_type __pos, size_type __n1, const _CharT* __s) const
    {
      __glibcxx_requires_string(__s);
      _M_check(__pos, "basic_string::compare");
      __n1 = _M_limit(__pos, __n1);
      const size_type __osize = traits_type::length(__s);
      const size_type __len = std::min(__n1, __osize);
      int __r = traits_type::compare(_M_data() + __pos, __s, __len);
      if (!__r)
	__r = _S_compare(__n1, __osize);
      return __r;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX_STRING_CONSTEXPR
    int
    basic_string <_CharT, _Traits, _Alloc>::
    compare(size_type __pos, size_type __n1, const _CharT* __s,
	    size_type __n2) const
    {
      __glibcxx_requires_string_len(__s, __n2);
      _M_check(__pos, "basic_string::compare");
      __n1 = _M_limit(__pos, __n1);
      const size_type __len = std::min(__n1, __n2);
      int __r = traits_type::compare(_M_data() + __pos, __s, __len);
      if (!__r)
	__r = _S_compare(__n1, __n2);
      return __r;
    }

#undef _GLIBCXX_STRING_CONSTEXPR

  // 21.3.7.9 basic_string::getline and operators
  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_istream<_CharT, _Traits>&
    operator>>(basic_istream<_CharT, _Traits>& __in,
	       basic_string<_CharT, _Traits, _Alloc>& __str)
    {
      typedef basic_istream<_CharT, _Traits>		__istream_type;
      typedef basic_string<_CharT, _Traits, _Alloc>	__string_type;
      typedef typename __istream_type::ios_base         __ios_base;
      typedef typename __istream_type::int_type		__int_type;
      typedef typename __string_type::size_type		__size_type;
      typedef ctype<_CharT>				__ctype_type;
      typedef typename __ctype_type::ctype_base         __ctype_base;

      __size_type __extracted = 0;
      typename __ios_base::iostate __err = __ios_base::goodbit;
      typename __istream_type::sentry __cerb(__in, false);
      if (__cerb)
	{
	  __try
	    {
	      // Avoid reallocation for common case.
	      __str.erase();
	      _CharT __buf[128];
	      __size_type __len = 0;	      
	      const streamsize __w = __in.width();
	      const __size_type __n = __w > 0 ? static_cast<__size_type>(__w)
		                              : __str.max_size();
	      const __ctype_type& __ct = use_facet<__ctype_type>(__in.getloc());
	      const __int_type __eof = _Traits::eof();
	      __int_type __c = __in.rdbuf()->sgetc();

	      while (__extracted < __n
		     && !_Traits::eq_int_type(__c, __eof)
		     && !__ct.is(__ctype_base::space,
				 _Traits::to_char_type(__c)))
		{
		  if (__len == sizeof(__buf) / sizeof(_CharT))
		    {
		      __str.append(__buf, sizeof(__buf) / sizeof(_CharT));
		      __len = 0;
		    }
		  __buf[__len++] = _Traits::to_char_type(__c);
		  ++__extracted;
		  __c = __in.rdbuf()->snextc();
		}
	      __str.append(__buf, __len);

	      if (__extracted < __n && _Traits::eq_int_type(__c, __eof))
		__err |= __ios_base::eofbit;
	      __in.width(0);
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      __in._M_setstate(__ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    {
	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 91. Description of operator>> and getline() for string<>
	      // might cause endless loop
	      __in._M_setstate(__ios_base::badbit);
	    }
	}
      // 211.  operator>>(istream&, string&) doesn't set failbit
      if (!__extracted)
	__err |= __ios_base::failbit;
      if (__err)
	__in.setstate(__err);
      return __in;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_istream<_CharT, _Traits>&
    getline(basic_istream<_CharT, _Traits>& __in,
	    basic_string<_CharT, _Traits, _Alloc>& __str, _CharT __delim)
    {
      typedef basic_istream<_CharT, _Traits>		__istream_type;
      typedef basic_string<_CharT, _Traits, _Alloc>	__string_type;
      typedef typename __istream_type::ios_base         __ios_base;
      typedef typename __istream_type::int_type		__int_type;
      typedef typename __string_type::size_type		__size_type;

      __size_type __extracted = 0;
      const __size_type __n = __str.max_size();
      typename __ios_base::iostate __err = __ios_base::goodbit;
      typename __istream_type::sentry __cerb(__in, true);
      if (__cerb)
	{
	  __try
	    {
	      __str.erase();
	      const __int_type __idelim = _Traits::to_int_type(__delim);
	      const __int_type __eof = _Traits::eof();
	      __int_type __c = __in.rdbuf()->sgetc();

	      while (__extracted < __n
		     && !_Traits::eq_int_type(__c, __eof)
		     && !_Traits::eq_int_type(__c, __idelim))
		{
		  __str += _Traits::to_char_type(__c);
		  ++__extracted;
		  __c = __in.rdbuf()->snextc();
		}

	      if (_Traits::eq_int_type(__c, __eof))
		__err |= __ios_base::eofbit;
	      else if (_Traits::eq_int_type(__c, __idelim))
		{
		  ++__extracted;		  
		  __in.rdbuf()->sbumpc();
		}
	      else
		__err |= __ios_base::failbit;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      __in._M_setstate(__ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    {
	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 91. Description of operator>> and getline() for string<>
	      // might cause endless loop
	      __in._M_setstate(__ios_base::badbit);
	    }
	}
      if (!__extracted)
	__err |= __ios_base::failbit;
      if (__err)
	__in.setstate(__err);
      return __in;
    }

  // Inhibit implicit instantiations for required instantiations,
  // which are defined via explicit instantiations elsewhere.
#if _GLIBCXX_EXTERN_TEMPLATE
  // The explicit instantiation definitions in src/c++11/string-inst.cc and
  // src/c++17/string-inst.cc only instantiate the members required for C++17
  // and earlier standards (so not C++20's starts_with and ends_with).
  // Suppress the explicit instantiation declarations for C++20, so C++20
  // code will implicitly instantiate std::string and std::wstring as needed.
# if __cplusplus <= 201703L && _GLIBCXX_EXTERN_TEMPLATE > 0
  extern template class basic_string<char>;
# elif ! _GLIBCXX_USE_CXX11_ABI
  // Still need to prevent implicit instantiation of the COW empty rep,
  // to ensure the definition in libstdc++.so is unique (PR 86138).
  extern template basic_string<char>::size_type
    basic_string<char>::_Rep::_S_empty_rep_storage[];
# endif

  extern template
    basic_istream<char>&
    operator>>(basic_istream<char>&, string&);
  extern template
    basic_ostream<char>&
    operator<<(basic_ostream<char>&, const string&);
  extern template
    basic_istream<char>&
    getline(basic_istream<char>&, string&, char);
  extern template
    basic_istream<char>&
    getline(basic_istream<char>&, string&);

#ifdef _GLIBCXX_USE_WCHAR_T
# if __cplusplus <= 201703L && _GLIBCXX_EXTERN_TEMPLATE > 0
  extern template class basic_string<wchar_t>;
# elif ! _GLIBCXX_USE_CXX11_ABI
  extern template basic_string<wchar_t>::size_type
    basic_string<wchar_t>::_Rep::_S_empty_rep_storage[];
# endif

  extern template
    basic_istream<wchar_t>&
    operator>>(basic_istream<wchar_t>&, wstring&);
  extern template
    basic_ostream<wchar_t>&
    operator<<(basic_ostream<wchar_t>&, const wstring&);
  extern template
    basic_istream<wchar_t>&
    getline(basic_istream<wchar_t>&, wstring&, wchar_t);
  extern template
    basic_istream<wchar_t>&
    getline(basic_istream<wchar_t>&, wstring&);
#endif // _GLIBCXX_USE_WCHAR_T
#endif // _GLIBCXX_EXTERN_TEMPLATE

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif
