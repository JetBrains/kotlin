// TR2 <dynamic_bitset> -*- C++ -*-

// Copyright (C) 2009-2022 Free Software Foundation, Inc.
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

/** @file tr2/dynamic_bitset.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{tr2/dynamic_bitset}
 */

#ifndef _GLIBCXX_TR2_DYNAMIC_BITSET_TCC
#define _GLIBCXX_TR2_DYNAMIC_BITSET_TCC 1

#pragma GCC system_header

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

namespace tr2
{
  // Definitions of non-inline functions from __dynamic_bitset_base.
  template<typename _WordT, typename _Alloc>
    void
    __dynamic_bitset_base<_WordT, _Alloc>::_M_do_left_shift(size_t __shift)
    {
      if (__builtin_expect(__shift != 0, 1))
	{
	  const size_t __wshift = __shift / _S_bits_per_block;
	  const size_t __offset = __shift % _S_bits_per_block;

	  if (__offset == 0)
	    for (size_t __n = this->_M_w.size() - 1; __n >= __wshift; --__n)
	      this->_M_w[__n] = this->_M_w[__n - __wshift];
	  else
	    {
	      const size_t __sub_offset = _S_bits_per_block - __offset;
	      for (size_t __n = _M_w.size() - 1; __n > __wshift; --__n)
		this->_M_w[__n] = ((this->_M_w[__n - __wshift] << __offset)
			     | (this->_M_w[__n - __wshift - 1] >> __sub_offset));
	      this->_M_w[__wshift] = this->_M_w[0] << __offset;
	    }

	  //// std::fill(this->_M_w.begin(), this->_M_w.begin() + __wshift,
	  ////          static_cast<_WordT>(0));
	}
    }

  template<typename _WordT, typename _Alloc>
    void
    __dynamic_bitset_base<_WordT, _Alloc>::_M_do_right_shift(size_t __shift)
    {
      if (__builtin_expect(__shift != 0, 1))
	{
	  const size_t __wshift = __shift / _S_bits_per_block;
	  const size_t __offset = __shift % _S_bits_per_block;
	  const size_t __limit = this->_M_w.size() - __wshift - 1;

	  if (__offset == 0)
	    for (size_t __n = 0; __n <= __limit; ++__n)
	      this->_M_w[__n] = this->_M_w[__n + __wshift];
	  else
	    {
	      const size_t __sub_offset = (_S_bits_per_block
					   - __offset);
	      for (size_t __n = 0; __n < __limit; ++__n)
		this->_M_w[__n] = ((this->_M_w[__n + __wshift] >> __offset)
			     | (this->_M_w[__n + __wshift + 1] << __sub_offset));
	      this->_M_w[__limit] = this->_M_w[_M_w.size()-1] >> __offset;
	    }

	  ////std::fill(this->_M_w.begin() + __limit + 1, this->_M_w.end(),
	  ////          static_cast<_WordT>(0));
	}
    }

  template<typename _WordT, typename _Alloc>
    unsigned long
    __dynamic_bitset_base<_WordT, _Alloc>::_M_do_to_ulong() const
    {
      size_t __n = sizeof(unsigned long) / sizeof(block_type);
      for (size_t __i = __n; __i < this->_M_w.size(); ++__i)
	if (this->_M_w[__i])
	  __throw_overflow_error(__N("__dynamic_bitset_base::_M_do_to_ulong"));
      unsigned long __res = 0UL;
      for (size_t __i = 0; __i < __n && __i < this->_M_w.size(); ++__i)
	__res += this->_M_w[__i] << (__i * _S_bits_per_block);
      return __res;
    }

  template<typename _WordT, typename _Alloc>
    unsigned long long
    __dynamic_bitset_base<_WordT, _Alloc>::_M_do_to_ullong() const
    {
      size_t __n = sizeof(unsigned long long) / sizeof(block_type);
      for (size_t __i = __n; __i < this->_M_w.size(); ++__i)
	if (this->_M_w[__i])
	  __throw_overflow_error(__N("__dynamic_bitset_base::_M_do_to_ullong"));
      unsigned long long __res = 0ULL;
      for (size_t __i = 0; __i < __n && __i < this->_M_w.size(); ++__i)
	__res += this->_M_w[__i] << (__i * _S_bits_per_block);
      return __res;
    }

  template<typename _WordT, typename _Alloc>
    size_t
    __dynamic_bitset_base<_WordT, _Alloc>
    ::_M_do_find_first(size_t __not_found) const
    {
      for (size_t __i = 0; __i < this->_M_w.size(); ++__i)
	{
	  _WordT __thisword = this->_M_w[__i];
	  if (__thisword != static_cast<_WordT>(0))
	    return (__i * _S_bits_per_block
		    + __builtin_ctzll(__thisword));
	}
      // not found, so return an indication of failure.
      return __not_found;
    }

  template<typename _WordT, typename _Alloc>
    size_t
    __dynamic_bitset_base<_WordT, _Alloc>
    ::_M_do_find_next(size_t __prev, size_t __not_found) const
    {
      // make bound inclusive
      ++__prev;

      // check out of bounds
      if (__prev >= this->_M_w.size() * _S_bits_per_block)
	return __not_found;

      // search first word
      size_t __i = _S_whichword(__prev);
      _WordT __thisword = this->_M_w[__i];

      // mask off bits below bound
      __thisword &= (~static_cast<_WordT>(0)) << _S_whichbit(__prev);

      if (__thisword != static_cast<_WordT>(0))
	return (__i * _S_bits_per_block
		+ __builtin_ctzll(__thisword));

      // check subsequent words
      for (++__i; __i < this->_M_w.size(); ++__i)
	{
	  __thisword = this->_M_w[__i];
	  if (__thisword != static_cast<_WordT>(0))
	    return (__i * _S_bits_per_block
		    + __builtin_ctzll(__thisword));
	}
      // not found, so return an indication of failure.
      return __not_found;
    } // end _M_do_find_next

  // Definitions of non-inline member functions.
  template<typename _WordT, typename _Alloc>
    template<typename _Traits, typename _CharT>
      void
      dynamic_bitset<_WordT, _Alloc>::
      _M_copy_from_ptr(const _CharT* __str, size_t __len,
		       size_t __pos, size_t __n, _CharT __zero, _CharT __one)
      {
	reset();
	const size_t __nbits = std::min(_M_Nb, std::min(__n, __len - __pos));
	for (size_t __i = __nbits; __i > 0; --__i)
	  {
	    const _CharT __c = __str[__pos + __nbits - __i];
	    if (_Traits::eq(__c, __zero))
	      ;
	    else if (_Traits::eq(__c, __one))
	      _M_unchecked_set(__i - 1);
	    else
	      __throw_invalid_argument(__N("dynamic_bitset::_M_copy_from_ptr"));
	  }
      }

  /**
   *  @brief Stream input operator for dynamic_bitset.
   *  @ingroup dynamic_bitset
   *
   *  Input will skip whitespace and only accept '0' and '1' characters.
   *  The %dynamic_bitset will grow as necessary to hold the string of bits.
   */
  template<typename _CharT, typename _Traits,
	   typename _WordT, typename _Alloc>
    std::basic_istream<_CharT, _Traits>&
    operator>>(std::basic_istream<_CharT, _Traits>& __is,
	       dynamic_bitset<_WordT, _Alloc>& __x)
    {
      typedef typename _Traits::char_type          char_type;
      typedef std::basic_istream<_CharT, _Traits>  __istream_type;
      typedef typename __istream_type::ios_base    __ios_base;

      std::basic_string<_CharT, _Traits> __tmp;
      __tmp.reserve(__x.size());

      const char_type __zero = __is.widen('0');
      const char_type __one = __is.widen('1');

      typename __ios_base::iostate __state = __ios_base::goodbit;
      typename __istream_type::sentry __sentry(__is);
      if (__sentry)
	{
	  __try
	    {
	      while (1)
		{
		  static typename _Traits::int_type __eof = _Traits::eof();

		  typename _Traits::int_type __c1 = __is.rdbuf()->sbumpc();
		  if (_Traits::eq_int_type(__c1, __eof))
		    {
		      __state |= __ios_base::eofbit;
		      break;
		    }
		  else
		    {
		      const char_type __c2 = _Traits::to_char_type(__c1);
		      if (_Traits::eq(__c2, __zero))
			__tmp.push_back(__zero);
		      else if (_Traits::eq(__c2, __one))
			__tmp.push_back(__one);
		      else if (_Traits::
			       eq_int_type(__is.rdbuf()->sputbackc(__c2),
					   __eof))
			{
			  __state |= __ios_base::failbit;
			  break;
			}
		      else
			break;
		    }
		}
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      __is._M_setstate(__ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { __is._M_setstate(__ios_base::badbit); }
	}

      __x.resize(__tmp.size());

      if (__tmp.empty() && __x.size())
	__state |= __ios_base::failbit;
      else
	__x._M_copy_from_string(__tmp, static_cast<size_t>(0), __x.size(),
				__zero, __one);
      if (__state)
	__is.setstate(__state);
      return __is;
    }
} // tr2

_GLIBCXX_END_NAMESPACE_VERSION
} // std

#endif /* _GLIBCXX_TR2_DYNAMIC_BITSET_TCC */
