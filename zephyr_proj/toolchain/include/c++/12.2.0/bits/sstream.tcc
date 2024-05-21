// String based streams -*- C++ -*-

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

/** @file bits/sstream.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{sstream}
 */

//
// ISO C++ 14882: 27.7  String-based streams
//

#ifndef _SSTREAM_TCC
#define _SSTREAM_TCC 1

#pragma GCC system_header

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  template <class _CharT, class _Traits, class _Alloc>
    typename basic_stringbuf<_CharT, _Traits, _Alloc>::int_type
    basic_stringbuf<_CharT, _Traits, _Alloc>::
    pbackfail(int_type __c)
    {
      int_type __ret = traits_type::eof();
      if (this->eback() < this->gptr())
	{
	  // Try to put back __c into input sequence in one of three ways.
	  // Order these tests done in is unspecified by the standard.
	  const bool __testeof = traits_type::eq_int_type(__c, __ret);
	  if (!__testeof)
	    {
	      const bool __testeq = traits_type::eq(traits_type::
						    to_char_type(__c),
						    this->gptr()[-1]);	  
	      const bool __testout = this->_M_mode & ios_base::out;
	      if (__testeq || __testout)
		{
		  this->gbump(-1);
		  if (!__testeq)
		    *this->gptr() = traits_type::to_char_type(__c);
		  __ret = __c;
		}
	    }
	  else
	    {
	      this->gbump(-1);
	      __ret = traits_type::not_eof(__c);
	    }
	}
      return __ret;
    }

  template <class _CharT, class _Traits, class _Alloc>
    typename basic_stringbuf<_CharT, _Traits, _Alloc>::int_type
    basic_stringbuf<_CharT, _Traits, _Alloc>::
    overflow(int_type __c)
    {
      const bool __testout = this->_M_mode & ios_base::out;
      if (__builtin_expect(!__testout, false))
	return traits_type::eof();

      const bool __testeof = traits_type::eq_int_type(__c, traits_type::eof());
      if (__builtin_expect(__testeof, false))
	return traits_type::not_eof(__c);

      const __size_type __capacity = _M_string.capacity();

#if _GLIBCXX_USE_CXX11_ABI
      if (size_t(this->epptr() - this->pbase()) < __capacity)
	{
	  // There is additional capacity in _M_string that can be used.
	  char_type* __base = const_cast<char_type*>(_M_string.data());
	  _M_pbump(__base, __base + __capacity, this->pptr() - this->pbase());
	  if (_M_mode & ios_base::in)
	    {
	      const __size_type __nget = this->gptr() - this->eback();
	      const __size_type __eget = this->egptr() - this->eback();
	      this->setg(__base, __base + __nget, __base + __eget + 1);
	    }
	  *this->pptr() = traits_type::to_char_type(__c);
	  this->pbump(1);
	  return __c;
	}
#endif

      const __size_type __max_size = _M_string.max_size();
      const bool __testput = this->pptr() < this->epptr();
      if (__builtin_expect(!__testput && __capacity == __max_size, false))
	return traits_type::eof();

      // Try to append __c into output sequence in one of two ways.
      // Order these tests done in is unspecified by the standard.
      const char_type __conv = traits_type::to_char_type(__c);
      if (!__testput)
	{
	  // NB: Start ostringstream buffers at 512 chars.  This is an
	  // experimental value (pronounced "arbitrary" in some of the
	  // hipper English-speaking countries), and can be changed to
	  // suit particular needs.
	  //
	  // _GLIBCXX_RESOLVE_LIB_DEFECTS
	  // 169. Bad efficiency of overflow() mandated
	  // 432. stringbuf::overflow() makes only one write position
	  //      available
	  const __size_type __opt_len = std::max(__size_type(2 * __capacity),
						 __size_type(512));
	  const __size_type __len = std::min(__opt_len, __max_size);
	  __string_type __tmp(_M_string.get_allocator());
	  __tmp.reserve(__len);
	  if (this->pbase())
	    __tmp.assign(this->pbase(), this->epptr() - this->pbase());
	  __tmp.push_back(__conv);
	  _M_string.swap(__tmp);
	  _M_sync(const_cast<char_type*>(_M_string.data()),
		  this->gptr() - this->eback(), this->pptr() - this->pbase());
	}
      else
	*this->pptr() = __conv;
      this->pbump(1);
      return __c;
    }

  template <class _CharT, class _Traits, class _Alloc>
    typename basic_stringbuf<_CharT, _Traits, _Alloc>::int_type
    basic_stringbuf<_CharT, _Traits, _Alloc>::
    underflow()
    {
      int_type __ret = traits_type::eof();
      const bool __testin = this->_M_mode & ios_base::in;
      if (__testin)
	{
	  // Update egptr() to match the actual string end.
	  _M_update_egptr();

	  if (this->gptr() < this->egptr())
	    __ret = traits_type::to_int_type(*this->gptr());
	}
      return __ret;
    }

  template <class _CharT, class _Traits, class _Alloc>
    typename basic_stringbuf<_CharT, _Traits, _Alloc>::pos_type
    basic_stringbuf<_CharT, _Traits, _Alloc>::
    seekoff(off_type __off, ios_base::seekdir __way, ios_base::openmode __mode)
    {
      pos_type __ret =  pos_type(off_type(-1));
      bool __testin = (ios_base::in & this->_M_mode & __mode) != 0;
      bool __testout = (ios_base::out & this->_M_mode & __mode) != 0;
      const bool __testboth = __testin && __testout && __way != ios_base::cur;
      __testin &= !(__mode & ios_base::out);
      __testout &= !(__mode & ios_base::in);

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 453. basic_stringbuf::seekoff need not always fail for an empty stream.
      const char_type* __beg = __testin ? this->eback() : this->pbase();
      if ((__beg || !__off) && (__testin || __testout || __testboth))
	{
	  _M_update_egptr();

	  off_type __newoffi = __off;
	  off_type __newoffo = __newoffi;
	  if (__way == ios_base::cur)
	    {
	      __newoffi += this->gptr() - __beg;
	      __newoffo += this->pptr() - __beg;
	    }
	  else if (__way == ios_base::end)
	    __newoffo = __newoffi += this->egptr() - __beg;

	  if ((__testin || __testboth)
	      && __newoffi >= 0
	      && this->egptr() - __beg >= __newoffi)
	    {
	      this->setg(this->eback(), this->eback() + __newoffi,
			 this->egptr());
	      __ret = pos_type(__newoffi);
	    }
	  if ((__testout || __testboth)
	      && __newoffo >= 0
	      && this->egptr() - __beg >= __newoffo)
	    {
	      _M_pbump(this->pbase(), this->epptr(), __newoffo);
	      __ret = pos_type(__newoffo);
	    }
	}
      return __ret;
    }

  template <class _CharT, class _Traits, class _Alloc>
    typename basic_stringbuf<_CharT, _Traits, _Alloc>::pos_type
    basic_stringbuf<_CharT, _Traits, _Alloc>::
    seekpos(pos_type __sp, ios_base::openmode __mode)
    {
      pos_type __ret =  pos_type(off_type(-1));
      const bool __testin = (ios_base::in & this->_M_mode & __mode) != 0;
      const bool __testout = (ios_base::out & this->_M_mode & __mode) != 0;

      const char_type* __beg = __testin ? this->eback() : this->pbase();
      if ((__beg || !off_type(__sp)) && (__testin || __testout))
	{
	  _M_update_egptr();

	  const off_type __pos(__sp);
	  const bool __testpos = (0 <= __pos
				  && __pos <= this->egptr() - __beg);
	  if (__testpos)
	    {
	      if (__testin)
		this->setg(this->eback(), this->eback() + __pos,
			   this->egptr());
	      if (__testout)
		_M_pbump(this->pbase(), this->epptr(), __pos);
	      __ret = __sp;
	    }
	}
      return __ret;
    }

  template <class _CharT, class _Traits, class _Alloc>
    void
    basic_stringbuf<_CharT, _Traits, _Alloc>::
    _M_sync(char_type* __base, __size_type __i, __size_type __o)
    {
      const bool __testin = _M_mode & ios_base::in;
      const bool __testout = _M_mode & ios_base::out;
      char_type* __endg = __base + _M_string.size();
      char_type* __endp = __base + _M_string.capacity();

      if (__base != _M_string.data())
	{
	  // setbuf: __i == size of buffer area (_M_string.size() == 0).
	  __endg += __i;
	  __i = 0;
	  __endp = __endg;
	}

      if (__testin)
	this->setg(__base, __base + __i, __endg);
      if (__testout)
	{
	  _M_pbump(__base, __endp, __o);
	  // egptr() always tracks the string end.  When !__testin,
	  // for the correct functioning of the streambuf inlines
	  // the other get area pointers are identical.
	  if (!__testin)
	    this->setg(__endg, __endg, __endg);
	}
    }

  template <class _CharT, class _Traits, class _Alloc>
    void
    basic_stringbuf<_CharT, _Traits, _Alloc>::
    _M_pbump(char_type* __pbeg, char_type* __pend, off_type __off)
    {
      this->setp(__pbeg, __pend);
      while (__off > __gnu_cxx::__numeric_traits<int>::__max)
	{
	  this->pbump(__gnu_cxx::__numeric_traits<int>::__max);
	  __off -= __gnu_cxx::__numeric_traits<int>::__max;
	}
      this->pbump(__off);
    }

  // Inhibit implicit instantiations for required instantiations,
  // which are defined via explicit instantiations elsewhere.
#if _GLIBCXX_EXTERN_TEMPLATE
  extern template class basic_stringbuf<char>;
  extern template class basic_istringstream<char>;
  extern template class basic_ostringstream<char>;
  extern template class basic_stringstream<char>;

#ifdef _GLIBCXX_USE_WCHAR_T
  extern template class basic_stringbuf<wchar_t>;
  extern template class basic_istringstream<wchar_t>;
  extern template class basic_ostringstream<wchar_t>;
  extern template class basic_stringstream<wchar_t>;
#endif
#endif

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif
