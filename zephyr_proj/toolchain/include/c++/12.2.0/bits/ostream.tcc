// ostream classes -*- C++ -*-

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

/** @file bits/ostream.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{ostream}
 */

//
// ISO C++ 14882: 27.6.2  Output streams
//

#ifndef _OSTREAM_TCC
#define _OSTREAM_TCC 1

#pragma GCC system_header

#include <bits/cxxabi_forced.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>::sentry::
    sentry(basic_ostream<_CharT, _Traits>& __os)
    : _M_ok(false), _M_os(__os)
    {
      // XXX MT
      if (__os.tie() && __os.good())
	__os.tie()->flush();

      if (__os.good())
	_M_ok = true;
      else if (__os.bad())
	__os.setstate(ios_base::failbit);
    }

  template<typename _CharT, typename _Traits>
    template<typename _ValueT>
      basic_ostream<_CharT, _Traits>&
      basic_ostream<_CharT, _Traits>::
      _M_insert(_ValueT __v)
      {
	sentry __cerb(*this);
	if (__cerb)
	  {
	    ios_base::iostate __err = ios_base::goodbit;
	    __try
	      {
		const __num_put_type& __np = __check_facet(this->_M_num_put);
		if (__np.put(*this, *this, this->fill(), __v).failed())
		  __err |= ios_base::badbit;
	      }
	    __catch(__cxxabiv1::__forced_unwind&)
	      {
		this->_M_setstate(ios_base::badbit);		
		__throw_exception_again;
	      }
	    __catch(...)
	      { this->_M_setstate(ios_base::badbit); }
	    if (__err)
	      this->setstate(__err);
	  }
	return *this;
      }

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    basic_ostream<_CharT, _Traits>::
    operator<<(short __n)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 117. basic_ostream uses nonexistent num_put member functions.
      const ios_base::fmtflags __fmt = this->flags() & ios_base::basefield;
      if (__fmt == ios_base::oct || __fmt == ios_base::hex)
	return _M_insert(static_cast<long>(static_cast<unsigned short>(__n)));
      else
	return _M_insert(static_cast<long>(__n));
    }

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    basic_ostream<_CharT, _Traits>::
    operator<<(int __n)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 117. basic_ostream uses nonexistent num_put member functions.
      const ios_base::fmtflags __fmt = this->flags() & ios_base::basefield;
      if (__fmt == ios_base::oct || __fmt == ios_base::hex)
	return _M_insert(static_cast<long>(static_cast<unsigned int>(__n)));
      else
	return _M_insert(static_cast<long>(__n));
    }
  
  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    basic_ostream<_CharT, _Traits>::
    operator<<(__streambuf_type* __sbin)
    {
      ios_base::iostate __err = ios_base::goodbit;
      sentry __cerb(*this);
      if (__cerb && __sbin)
	{
	  __try
	    {
	      if (!__copy_streambufs(__sbin, this->rdbuf()))
		__err |= ios_base::failbit;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);		
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::failbit); }
	}
      else if (!__sbin)
	__err |= ios_base::badbit;
      if (__err)
	this->setstate(__err);
      return *this;
    }

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    basic_ostream<_CharT, _Traits>::
    put(char_type __c)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR 60. What is a formatted input function?
      // basic_ostream::put(char_type) is an unformatted output function.
      // DR 63. Exception-handling policy for unformatted output.
      // Unformatted output functions should catch exceptions thrown
      // from streambuf members.
      sentry __cerb(*this);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      const int_type __put = this->rdbuf()->sputc(__c);
	      if (traits_type::eq_int_type(__put, traits_type::eof()))
		__err |= ios_base::badbit;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);		
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::badbit); }
	  if (__err)
	    this->setstate(__err);
	}
      return *this;
    }

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    basic_ostream<_CharT, _Traits>::
    write(const _CharT* __s, streamsize __n)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR 60. What is a formatted input function?
      // basic_ostream::write(const char_type*, streamsize) is an
      // unformatted output function.
      // DR 63. Exception-handling policy for unformatted output.
      // Unformatted output functions should catch exceptions thrown
      // from streambuf members.
      sentry __cerb(*this);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      if (this->rdbuf()->sputn(__s, __n) != __n)
		__err = ios_base::badbit;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);		
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::badbit); }
	  if (__err)
	    this->setstate(ios_base::badbit);
	}
      return *this;
    }

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    basic_ostream<_CharT, _Traits>::
    flush()
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR 60. What is a formatted input function?
      // basic_ostream::flush() is *not* an unformatted output function.
      // 581. flush() not unformatted function
      // basic_ostream::flush() *is* an unformatted output function.
      if (__streambuf_type* __buf = this->rdbuf())
	{
	  sentry __cerb(*this);
	  if (__cerb)
	    {
	      ios_base::iostate __err = ios_base::goodbit;
	      __try
		{
		  if (this->rdbuf()->pubsync() == -1)
		    __err |= ios_base::badbit;
		}
	      __catch(__cxxabiv1::__forced_unwind&)
		{
		  this->_M_setstate(ios_base::badbit);
		  __throw_exception_again;
		}
	      __catch(...)
		{ this->_M_setstate(ios_base::badbit); }
	      if (__err)
		this->setstate(__err);
	    }
	}
      return *this;
    }

  template<typename _CharT, typename _Traits>
    typename basic_ostream<_CharT, _Traits>::pos_type
    basic_ostream<_CharT, _Traits>::
    tellp()
    {
      sentry __cerb(*this);
      pos_type __ret = pos_type(-1);
      if (!this->fail())
	__ret = this->rdbuf()->pubseekoff(0, ios_base::cur, ios_base::out);
      return __ret;
    }

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    basic_ostream<_CharT, _Traits>::
    seekp(pos_type __pos)
    {
      sentry __cerb(*this);
      if (!this->fail())
	{
	  // _GLIBCXX_RESOLVE_LIB_DEFECTS
	  // 136.  seekp, seekg setting wrong streams?
	  const pos_type __p = this->rdbuf()->pubseekpos(__pos, ios_base::out);

	  // 129. Need error indication from seekp() and seekg()
	  if (__p == pos_type(off_type(-1)))
	    this->setstate(ios_base::failbit);
	}
      return *this;
    }

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    basic_ostream<_CharT, _Traits>::
    seekp(off_type __off, ios_base::seekdir __dir)
    {
      sentry __cerb(*this);
      if (!this->fail())
	{
	  // _GLIBCXX_RESOLVE_LIB_DEFECTS
	  // 136.  seekp, seekg setting wrong streams?
	  const pos_type __p = this->rdbuf()->pubseekoff(__off, __dir,
							 ios_base::out);

	  // 129. Need error indication from seekp() and seekg()
	  if (__p == pos_type(off_type(-1)))
	    this->setstate(ios_base::failbit);
	}
      return *this;
    }

  template<typename _CharT, typename _Traits>
    basic_ostream<_CharT, _Traits>&
    operator<<(basic_ostream<_CharT, _Traits>& __out, const char* __s)
    {
      if (!__s)
	__out.setstate(ios_base::badbit);
      else
	{
	  // _GLIBCXX_RESOLVE_LIB_DEFECTS
	  // 167.  Improper use of traits_type::length()
	  const size_t __clen = char_traits<char>::length(__s);
	  __try
	    {
	      struct __ptr_guard
	      {
		_CharT *__p;
		__ptr_guard (_CharT *__ip): __p(__ip) { }
		~__ptr_guard() { delete[] __p; }
		_CharT* __get() { return __p; }
	      } __pg (new _CharT[__clen]);

	      _CharT *__ws = __pg.__get();
	      for (size_t  __i = 0; __i < __clen; ++__i)
		__ws[__i] = __out.widen(__s[__i]);
	      __ostream_insert(__out, __ws, __clen);
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      __out._M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { __out._M_setstate(ios_base::badbit); }
	}
      return __out;
    }

  // Inhibit implicit instantiations for required instantiations,
  // which are defined via explicit instantiations elsewhere.
#if _GLIBCXX_EXTERN_TEMPLATE
  extern template class basic_ostream<char>;
  extern template ostream& endl(ostream&);
  extern template ostream& ends(ostream&);
  extern template ostream& flush(ostream&);
  extern template ostream& operator<<(ostream&, char);
  extern template ostream& operator<<(ostream&, unsigned char);
  extern template ostream& operator<<(ostream&, signed char);
  extern template ostream& operator<<(ostream&, const char*);
  extern template ostream& operator<<(ostream&, const unsigned char*);
  extern template ostream& operator<<(ostream&, const signed char*);

  extern template ostream& ostream::_M_insert(long);
  extern template ostream& ostream::_M_insert(unsigned long);
  extern template ostream& ostream::_M_insert(bool);
#ifdef _GLIBCXX_USE_LONG_LONG
  extern template ostream& ostream::_M_insert(long long);
  extern template ostream& ostream::_M_insert(unsigned long long);
#endif
  extern template ostream& ostream::_M_insert(double);
  extern template ostream& ostream::_M_insert(long double);
  extern template ostream& ostream::_M_insert(const void*);

#ifdef _GLIBCXX_USE_WCHAR_T
  extern template class basic_ostream<wchar_t>;
  extern template wostream& endl(wostream&);
  extern template wostream& ends(wostream&);
  extern template wostream& flush(wostream&);
  extern template wostream& operator<<(wostream&, wchar_t);
  extern template wostream& operator<<(wostream&, char);
  extern template wostream& operator<<(wostream&, const wchar_t*);
  extern template wostream& operator<<(wostream&, const char*);

  extern template wostream& wostream::_M_insert(long);
  extern template wostream& wostream::_M_insert(unsigned long);
  extern template wostream& wostream::_M_insert(bool);
#ifdef _GLIBCXX_USE_LONG_LONG
  extern template wostream& wostream::_M_insert(long long);
  extern template wostream& wostream::_M_insert(unsigned long long);
#endif
  extern template wostream& wostream::_M_insert(double);
  extern template wostream& wostream::_M_insert(long double);
  extern template wostream& wostream::_M_insert(const void*);
#endif
#endif

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif
