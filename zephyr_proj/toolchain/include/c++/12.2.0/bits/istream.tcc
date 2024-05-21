// istream classes -*- C++ -*-

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

/** @file bits/istream.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{istream}
 */

//
// ISO C++ 14882: 27.6.1  Input streams
//

#ifndef _ISTREAM_TCC
#define _ISTREAM_TCC 1

#pragma GCC system_header

#include <bits/cxxabi_forced.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>::sentry::
    sentry(basic_istream<_CharT, _Traits>& __in, bool __noskip) : _M_ok(false)
    {
      ios_base::iostate __err = ios_base::goodbit;
      if (__in.good())
	{
	  __try
	    {
	      if (__in.tie())
		__in.tie()->flush();
	      if (!__noskip && bool(__in.flags() & ios_base::skipws))
		{
		  const __int_type __eof = traits_type::eof();
		  __streambuf_type* __sb = __in.rdbuf();
		  __int_type __c = __sb->sgetc();

		  const __ctype_type& __ct = __check_facet(__in._M_ctype);
		  while (!traits_type::eq_int_type(__c, __eof)
			 && __ct.is(ctype_base::space,
				    traits_type::to_char_type(__c)))
		    __c = __sb->snextc();

		  // _GLIBCXX_RESOLVE_LIB_DEFECTS
		  // 195. Should basic_istream::sentry's constructor ever
		  // set eofbit?
		  if (traits_type::eq_int_type(__c, __eof))
		    __err |= ios_base::eofbit;
		}
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      __in._M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { __in._M_setstate(ios_base::badbit); }
	}

      if (__in.good() && __err == ios_base::goodbit)
	_M_ok = true;
      else
	{
	  __err |= ios_base::failbit;
	  __in.setstate(__err);
	}
    }

  template<typename _CharT, typename _Traits>
    template<typename _ValueT>
      basic_istream<_CharT, _Traits>&
      basic_istream<_CharT, _Traits>::
      _M_extract(_ValueT& __v)
      {
	sentry __cerb(*this, false);
	if (__cerb)
	  {
	    ios_base::iostate __err = ios_base::goodbit;
	    __try
	      {
		const __num_get_type& __ng = __check_facet(this->_M_num_get);
		__ng.get(*this, 0, *this, __err, __v);
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
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    operator>>(short& __n)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 118. basic_istream uses nonexistent num_get member functions.
      sentry __cerb(*this, false);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      long __l;
	      const __num_get_type& __ng = __check_facet(this->_M_num_get);
	      __ng.get(*this, 0, *this, __err, __l);

	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 696. istream::operator>>(int&) broken.
	      if (__l < __gnu_cxx::__numeric_traits<short>::__min)
		{
		  __err |= ios_base::failbit;
		  __n = __gnu_cxx::__numeric_traits<short>::__min;
		}
	      else if (__l > __gnu_cxx::__numeric_traits<short>::__max)
		{
		  __err |= ios_base::failbit;
		  __n = __gnu_cxx::__numeric_traits<short>::__max;
		}
	      else
		__n = short(__l);
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
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    operator>>(int& __n)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 118. basic_istream uses nonexistent num_get member functions.
      sentry __cerb(*this, false);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      long __l;
	      const __num_get_type& __ng = __check_facet(this->_M_num_get);
	      __ng.get(*this, 0, *this, __err, __l);

	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 696. istream::operator>>(int&) broken.
	      if (__l < __gnu_cxx::__numeric_traits<int>::__min)
		{
		  __err |= ios_base::failbit;
		  __n = __gnu_cxx::__numeric_traits<int>::__min;
		}
	      else if (__l > __gnu_cxx::__numeric_traits<int>::__max)
		{
		  __err |= ios_base::failbit;	      
		  __n = __gnu_cxx::__numeric_traits<int>::__max;
		}
	      else
		__n = int(__l);
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
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    operator>>(__streambuf_type* __sbout)
    {
      ios_base::iostate __err = ios_base::goodbit;
      sentry __cerb(*this, false);
      if (__cerb && __sbout)
	{
	  __try
	    {
	      bool __ineof;
	      if (!__copy_streambufs_eof(this->rdbuf(), __sbout, __ineof))
		__err |= ios_base::failbit;
	      if (__ineof)
		__err |= ios_base::eofbit;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::failbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::failbit); }
	}
      else if (!__sbout)
	__err |= ios_base::failbit;
      if (__err)
	this->setstate(__err);
      return *this;
    }

  template<typename _CharT, typename _Traits>
    typename basic_istream<_CharT, _Traits>::int_type
    basic_istream<_CharT, _Traits>::
    get(void)
    {
      const int_type __eof = traits_type::eof();
      int_type __c = __eof;
      _M_gcount = 0;
      ios_base::iostate __err = ios_base::goodbit;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  __try
	    {
	      __c = this->rdbuf()->sbumpc();
	      // 27.6.1.1 paragraph 3
	      if (!traits_type::eq_int_type(__c, __eof))
		_M_gcount = 1;
	      else
		__err |= ios_base::eofbit;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::badbit); }
	}
      if (!_M_gcount)
	__err |= ios_base::failbit;
      if (__err)
	this->setstate(__err);
      return __c;
    }

  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    get(char_type& __c)
    {
      _M_gcount = 0;
      ios_base::iostate __err = ios_base::goodbit;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  __try
	    {
	      const int_type __cb = this->rdbuf()->sbumpc();
	      // 27.6.1.1 paragraph 3
	      if (!traits_type::eq_int_type(__cb, traits_type::eof()))
		{
		  _M_gcount = 1;
		  __c = traits_type::to_char_type(__cb);
		}
	      else
		__err |= ios_base::eofbit;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::badbit); }
	}
      if (!_M_gcount)
	__err |= ios_base::failbit;
      if (__err)
	this->setstate(__err);
      return *this;
    }

  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    get(char_type* __s, streamsize __n, char_type __delim)
    {
      _M_gcount = 0;
      ios_base::iostate __err = ios_base::goodbit;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  __try
	    {
	      const int_type __idelim = traits_type::to_int_type(__delim);
	      const int_type __eof = traits_type::eof();
	      __streambuf_type* __sb = this->rdbuf();
	      int_type __c = __sb->sgetc();

	      while (_M_gcount + 1 < __n
		     && !traits_type::eq_int_type(__c, __eof)
		     && !traits_type::eq_int_type(__c, __idelim))
		{
		  *__s++ = traits_type::to_char_type(__c);
		  ++_M_gcount;
		  __c = __sb->snextc();
		}
	      if (traits_type::eq_int_type(__c, __eof))
		__err |= ios_base::eofbit;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::badbit); }
	}
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 243. get and getline when sentry reports failure.
      if (__n > 0)
	*__s = char_type();
      if (!_M_gcount)
	__err |= ios_base::failbit;
      if (__err)
	this->setstate(__err);
      return *this;
    }

  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    get(__streambuf_type& __sb, char_type __delim)
    {
      _M_gcount = 0;
      ios_base::iostate __err = ios_base::goodbit;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  __try
	    {
	      const int_type __idelim = traits_type::to_int_type(__delim);
	      const int_type __eof = traits_type::eof();
	      __streambuf_type* __this_sb = this->rdbuf();
	      int_type __c = __this_sb->sgetc();
	      char_type __c2 = traits_type::to_char_type(__c);
	      unsigned long long __gcount = 0;

	      while (!traits_type::eq_int_type(__c, __eof)
		     && !traits_type::eq_int_type(__c, __idelim)
		     && !traits_type::eq_int_type(__sb.sputc(__c2), __eof))
		{
		  ++__gcount;
		  __c = __this_sb->snextc();
		  __c2 = traits_type::to_char_type(__c);
		}
	      if (traits_type::eq_int_type(__c, __eof))
		__err |= ios_base::eofbit;
	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 3464. istream::gcount() can overflow
	      if (__gcount <= __gnu_cxx::__numeric_traits<streamsize>::__max)
		_M_gcount = __gcount;
	      else
		_M_gcount = __gnu_cxx::__numeric_traits<streamsize>::__max;
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::badbit); }
	}
      if (!_M_gcount)
	__err |= ios_base::failbit;
      if (__err)
	this->setstate(__err);
      return *this;
    }

  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    getline(char_type* __s, streamsize __n, char_type __delim)
    {
      _M_gcount = 0;
      ios_base::iostate __err = ios_base::goodbit;
      sentry __cerb(*this, true);
      if (__cerb)
        {
          __try
            {
              const int_type __idelim = traits_type::to_int_type(__delim);
              const int_type __eof = traits_type::eof();
              __streambuf_type* __sb = this->rdbuf();
              int_type __c = __sb->sgetc();

              while (_M_gcount + 1 < __n
                     && !traits_type::eq_int_type(__c, __eof)
                     && !traits_type::eq_int_type(__c, __idelim))
                {
                  *__s++ = traits_type::to_char_type(__c);
                  __c = __sb->snextc();
                  ++_M_gcount;
                }
              if (traits_type::eq_int_type(__c, __eof))
                __err |= ios_base::eofbit;
              else
                {
                  if (traits_type::eq_int_type(__c, __idelim))
                    {
                      __sb->sbumpc();
                      ++_M_gcount;
                    }
                  else
                    __err |= ios_base::failbit;
                }
            }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
          __catch(...)
            { this->_M_setstate(ios_base::badbit); }
        }
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 243. get and getline when sentry reports failure.
      if (__n > 0)
	*__s = char_type();
      if (!_M_gcount)
        __err |= ios_base::failbit;
      if (__err)
        this->setstate(__err);
      return *this;
    }

  // We provide three overloads, since the first two are much simpler
  // than the general case. Also, the latter two can thus adopt the
  // same "batchy" strategy used by getline above.
  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    ignore(void)
    {
      _M_gcount = 0;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      const int_type __eof = traits_type::eof();
	      __streambuf_type* __sb = this->rdbuf();

	      if (traits_type::eq_int_type(__sb->sbumpc(), __eof))
		__err |= ios_base::eofbit;
	      else
		_M_gcount = 1;
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
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    ignore(streamsize __n)
    {
      _M_gcount = 0;
      sentry __cerb(*this, true);
      if (__cerb && __n > 0)
        {
          ios_base::iostate __err = ios_base::goodbit;
          __try
            {
              const int_type __eof = traits_type::eof();
              __streambuf_type* __sb = this->rdbuf();
              int_type __c = __sb->sgetc();

	      // N.B. On LFS-enabled platforms streamsize is still 32 bits
	      // wide: if we want to implement the standard mandated behavior
	      // for n == max() (see 27.6.1.3/24) we are at risk of signed
	      // integer overflow: thus these contortions. Also note that,
	      // by definition, when more than 2G chars are actually ignored,
	      // _M_gcount (the return value of gcount, that is) cannot be
	      // really correct, being unavoidably too small.
	      bool __large_ignore = false;
	      while (true)
		{
		  while (_M_gcount < __n
			 && !traits_type::eq_int_type(__c, __eof))
		    {
		      ++_M_gcount;
		      __c = __sb->snextc();
		    }
		  if (__n == __gnu_cxx::__numeric_traits<streamsize>::__max
		      && !traits_type::eq_int_type(__c, __eof))
		    {
		      _M_gcount =
			__gnu_cxx::__numeric_traits<streamsize>::__min;
		      __large_ignore = true;
		    }
		  else
		    break;
		}

	      if (__n == __gnu_cxx::__numeric_traits<streamsize>::__max)
		{
		  if (__large_ignore)
		    _M_gcount = __gnu_cxx::__numeric_traits<streamsize>::__max;

		  if (traits_type::eq_int_type(__c, __eof))
		    __err |= ios_base::eofbit;
		}
	      else if (_M_gcount < __n)
		{
		  if (traits_type::eq_int_type(__c, __eof))
		    __err |= ios_base::eofbit;
		}
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
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    ignore(streamsize __n, int_type __delim)
    {
      _M_gcount = 0;
      sentry __cerb(*this, true);
      if (__cerb && __n > 0)
        {
          ios_base::iostate __err = ios_base::goodbit;
          __try
            {
              const int_type __eof = traits_type::eof();
              __streambuf_type* __sb = this->rdbuf();
              int_type __c = __sb->sgetc();

	      // See comment above.
	      bool __large_ignore = false;
	      while (true)
		{
		  while (_M_gcount < __n
			 && !traits_type::eq_int_type(__c, __eof)
			 && !traits_type::eq_int_type(__c, __delim))
		    {
		      ++_M_gcount;
		      __c = __sb->snextc();
		    }
		  if (__n == __gnu_cxx::__numeric_traits<streamsize>::__max
		      && !traits_type::eq_int_type(__c, __eof)
		      && !traits_type::eq_int_type(__c, __delim))
		    {
		      _M_gcount =
			__gnu_cxx::__numeric_traits<streamsize>::__min;
		      __large_ignore = true;
		    }
		  else
		    break;
		}

	      if (__n == __gnu_cxx::__numeric_traits<streamsize>::__max)
		{
		  if (__large_ignore)
		    _M_gcount = __gnu_cxx::__numeric_traits<streamsize>::__max;

		  if (traits_type::eq_int_type(__c, __eof))
		    __err |= ios_base::eofbit;
		  else
		    {
		      if (_M_gcount != __n)
			++_M_gcount;
		      __sb->sbumpc();
		    }
		}
	      else if (_M_gcount < __n) // implies __c == __delim or EOF
		{
		  if (traits_type::eq_int_type(__c, __eof))
		    __err |= ios_base::eofbit;
		  else
		    {
		      ++_M_gcount;
		      __sb->sbumpc();
		    }
		}
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
    typename basic_istream<_CharT, _Traits>::int_type
    basic_istream<_CharT, _Traits>::
    peek(void)
    {
      int_type __c = traits_type::eof();
      _M_gcount = 0;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      __c = this->rdbuf()->sgetc();
	      if (traits_type::eq_int_type(__c, traits_type::eof()))
		__err |= ios_base::eofbit;
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
      return __c;
    }

  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    read(char_type* __s, streamsize __n)
    {
      _M_gcount = 0;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      _M_gcount = this->rdbuf()->sgetn(__s, __n);
	      if (_M_gcount != __n)
		__err |= (ios_base::eofbit | ios_base::failbit);
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
    streamsize
    basic_istream<_CharT, _Traits>::
    readsome(char_type* __s, streamsize __n)
    {
      _M_gcount = 0;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      // Cannot compare int_type with streamsize generically.
	      const streamsize __num = this->rdbuf()->in_avail();
	      if (__num > 0)
		_M_gcount = this->rdbuf()->sgetn(__s, std::min(__num, __n));
	      else if (__num == -1)
		__err |= ios_base::eofbit;
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
      return _M_gcount;
    }

  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    putback(char_type __c)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 60. What is a formatted input function?
      _M_gcount = 0;
      // Clear eofbit per N3168.
      this->clear(this->rdstate() & ~ios_base::eofbit);
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      const int_type __eof = traits_type::eof();
	      __streambuf_type* __sb = this->rdbuf();
	      if (!__sb
		  || traits_type::eq_int_type(__sb->sputbackc(__c), __eof))
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
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    unget(void)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 60. What is a formatted input function?
      _M_gcount = 0;
      // Clear eofbit per N3168.
      this->clear(this->rdstate() & ~ios_base::eofbit);
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      const int_type __eof = traits_type::eof();
	      __streambuf_type* __sb = this->rdbuf();
	      if (!__sb
		  || traits_type::eq_int_type(__sb->sungetc(), __eof))
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
    int
    basic_istream<_CharT, _Traits>::
    sync(void)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR60.  Do not change _M_gcount.
      int __ret = -1;
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      __streambuf_type* __sb = this->rdbuf();
	      if (__sb)
		{
		  if (__sb->pubsync() == -1)
		    __err |= ios_base::badbit;
		  else
		    __ret = 0;
		}
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
      return __ret;
    }

  template<typename _CharT, typename _Traits>
    typename basic_istream<_CharT, _Traits>::pos_type
    basic_istream<_CharT, _Traits>::
    tellg(void)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR60.  Do not change _M_gcount.
      pos_type __ret = pos_type(-1);
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  __try
	    {
	      if (!this->fail())
		__ret = this->rdbuf()->pubseekoff(0, ios_base::cur,
						  ios_base::in);
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      this->_M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { this->_M_setstate(ios_base::badbit); }
	}
      return __ret;
    }

  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    seekg(pos_type __pos)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR60.  Do not change _M_gcount.
      // Clear eofbit per N3168.
      this->clear(this->rdstate() & ~ios_base::eofbit);
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      if (!this->fail())
		{
		  // 136.  seekp, seekg setting wrong streams?
		  const pos_type __p = this->rdbuf()->pubseekpos(__pos,
								 ios_base::in);
		  
		  // 129.  Need error indication from seekp() and seekg()
		  if (__p == pos_type(off_type(-1)))
		    __err |= ios_base::failbit;
		}
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
    basic_istream<_CharT, _Traits>&
    basic_istream<_CharT, _Traits>::
    seekg(off_type __off, ios_base::seekdir __dir)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR60.  Do not change _M_gcount.
      // Clear eofbit per N3168.
      this->clear(this->rdstate() & ~ios_base::eofbit);
      sentry __cerb(*this, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      if (!this->fail())
		{
		  // 136.  seekp, seekg setting wrong streams?
		  const pos_type __p = this->rdbuf()->pubseekoff(__off, __dir,
								 ios_base::in);
	      
		  // 129.  Need error indication from seekp() and seekg()
		  if (__p == pos_type(off_type(-1)))
		    __err |= ios_base::failbit;
		}
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

  // 27.6.1.2.3 Character extraction templates
  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    operator>>(basic_istream<_CharT, _Traits>& __in, _CharT& __c)
    {
      typedef basic_istream<_CharT, _Traits>		__istream_type;
      typedef typename __istream_type::int_type         __int_type;

      typename __istream_type::sentry __cerb(__in, false);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      const __int_type __cb = __in.rdbuf()->sbumpc();
	      if (!_Traits::eq_int_type(__cb, _Traits::eof()))
		__c = _Traits::to_char_type(__cb);
	      else
		__err |= (ios_base::eofbit | ios_base::failbit);
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      __in._M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { __in._M_setstate(ios_base::badbit); }
	  if (__err)
	    __in.setstate(__err);
	}
      return __in;
    }

  template<typename _CharT, typename _Traits>
    void
    __istream_extract(basic_istream<_CharT, _Traits>& __in, _CharT* __s,
		      streamsize __num)
    {
      typedef basic_istream<_CharT, _Traits>		__istream_type;
      typedef basic_streambuf<_CharT, _Traits>          __streambuf_type;
      typedef typename _Traits::int_type		int_type;
      typedef _CharT					char_type;
      typedef ctype<_CharT>				__ctype_type;

      streamsize __extracted = 0;
      ios_base::iostate __err = ios_base::goodbit;
      typename __istream_type::sentry __cerb(__in, false);
      if (__cerb)
	{
	  __try
	    {
	      // Figure out how many characters to extract.
	      streamsize __width = __in.width();
	      if (0 < __width && __width < __num)
		__num = __width;

	      const __ctype_type& __ct = use_facet<__ctype_type>(__in.getloc());

	      const int_type __eof = _Traits::eof();
	      __streambuf_type* __sb = __in.rdbuf();
	      int_type __c = __sb->sgetc();

	      while (__extracted < __num - 1
		     && !_Traits::eq_int_type(__c, __eof)
		     && !__ct.is(ctype_base::space,
				 _Traits::to_char_type(__c)))
		{
		  *__s++ = _Traits::to_char_type(__c);
		  ++__extracted;
		  __c = __sb->snextc();
		}

	      if (__extracted < __num - 1
		  && _Traits::eq_int_type(__c, __eof))
		__err |= ios_base::eofbit;

	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 68.  Extractors for char* should store null at end
	      *__s = char_type();
	      __in.width(0);
	    }
	  __catch(__cxxabiv1::__forced_unwind&)
	    {
	      __in._M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch(...)
	    { __in._M_setstate(ios_base::badbit); }
	}
      if (!__extracted)
	__err |= ios_base::failbit;
      if (__err)
	__in.setstate(__err);
    }

  // 27.6.1.4 Standard basic_istream manipulators
  template<typename _CharT, typename _Traits>
    basic_istream<_CharT, _Traits>&
    ws(basic_istream<_CharT, _Traits>& __in)
    {
      typedef basic_istream<_CharT, _Traits>		__istream_type;
      typedef basic_streambuf<_CharT, _Traits>          __streambuf_type;
      typedef typename __istream_type::int_type		__int_type;
      typedef ctype<_CharT>				__ctype_type;

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 451. behavior of std::ws
      typename __istream_type::sentry __cerb(__in, true);
      if (__cerb)
	{
	  ios_base::iostate __err = ios_base::goodbit;
	  __try
	    {
	      const __ctype_type& __ct = use_facet<__ctype_type>(__in.getloc());
	      const __int_type __eof = _Traits::eof();
	      __streambuf_type* __sb = __in.rdbuf();
	      __int_type __c = __sb->sgetc();

	      while (true)
		{
		  if (_Traits::eq_int_type(__c, __eof))
		    {
		      __err = ios_base::eofbit;
		      break;
		    }
		  if (!__ct.is(ctype_base::space, _Traits::to_char_type(__c)))
		    break;
		  __c = __sb->snextc();
		}
	    }
	  __catch (const __cxxabiv1::__forced_unwind&)
	    {
	      __in._M_setstate(ios_base::badbit);
	      __throw_exception_again;
	    }
	  __catch (...)
	    {
	      __in._M_setstate(ios_base::badbit);
	    }
	  if (__err)
	    __in.setstate(__err);
	}
      return __in;
    }

  // Inhibit implicit instantiations for required instantiations,
  // which are defined via explicit instantiations elsewhere.
#if _GLIBCXX_EXTERN_TEMPLATE
  extern template class basic_istream<char>;
  extern template istream& ws(istream&);
  extern template istream& operator>>(istream&, char&);
  extern template istream& operator>>(istream&, unsigned char&);
  extern template istream& operator>>(istream&, signed char&);

  extern template istream& istream::_M_extract(unsigned short&);
  extern template istream& istream::_M_extract(unsigned int&);  
  extern template istream& istream::_M_extract(long&);
  extern template istream& istream::_M_extract(unsigned long&);
  extern template istream& istream::_M_extract(bool&);
#ifdef _GLIBCXX_USE_LONG_LONG
  extern template istream& istream::_M_extract(long long&);
  extern template istream& istream::_M_extract(unsigned long long&);
#endif
  extern template istream& istream::_M_extract(float&);
  extern template istream& istream::_M_extract(double&);
  extern template istream& istream::_M_extract(long double&);
  extern template istream& istream::_M_extract(void*&);

  extern template class basic_iostream<char>;

#ifdef _GLIBCXX_USE_WCHAR_T
  extern template class basic_istream<wchar_t>;
  extern template wistream& ws(wistream&);
  extern template wistream& operator>>(wistream&, wchar_t&);
  extern template void __istream_extract(wistream&, wchar_t*, streamsize);

  extern template wistream& wistream::_M_extract(unsigned short&);
  extern template wistream& wistream::_M_extract(unsigned int&);  
  extern template wistream& wistream::_M_extract(long&);
  extern template wistream& wistream::_M_extract(unsigned long&);
  extern template wistream& wistream::_M_extract(bool&);
#ifdef _GLIBCXX_USE_LONG_LONG
  extern template wistream& wistream::_M_extract(long long&);
  extern template wistream& wistream::_M_extract(unsigned long long&);
#endif
  extern template wistream& wistream::_M_extract(float&);
  extern template wistream& wistream::_M_extract(double&);
  extern template wistream& wistream::_M_extract(long double&);
  extern template wistream& wistream::_M_extract(void*&);

  extern template class basic_iostream<wchar_t>;
#endif
#endif

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif
