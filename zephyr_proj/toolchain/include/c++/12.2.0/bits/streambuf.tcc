// Stream buffer classes -*- C++ -*-

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

/** @file bits/streambuf.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{streambuf}
 */

//
// ISO C++ 14882: 27.5  Stream buffers
//

#ifndef _STREAMBUF_TCC
#define _STREAMBUF_TCC 1

#pragma GCC system_header

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  template<typename _CharT, typename _Traits>
    streamsize
    basic_streambuf<_CharT, _Traits>::
    xsgetn(char_type* __s, streamsize __n)
    {
      streamsize __ret = 0;
      while (__ret < __n)
	{
	  const streamsize __buf_len = this->egptr() - this->gptr();
	  if (__buf_len)
	    {
	      const streamsize __remaining = __n - __ret;
	      const streamsize __len = std::min(__buf_len, __remaining);
	      traits_type::copy(__s, this->gptr(), __len);
	      __ret += __len;
	      __s += __len;
	      this->__safe_gbump(__len);
	    }

	  if (__ret < __n)
	    {
	      const int_type __c = this->uflow();
	      if (!traits_type::eq_int_type(__c, traits_type::eof()))
		{
		  traits_type::assign(*__s++, traits_type::to_char_type(__c));
		  ++__ret;
		}
	      else
		break;
	    }
	}
      return __ret;
    }

  template<typename _CharT, typename _Traits>
    streamsize
    basic_streambuf<_CharT, _Traits>::
    xsputn(const char_type* __s, streamsize __n)
    {
      streamsize __ret = 0;
      while (__ret < __n)
	{
	  const streamsize __buf_len = this->epptr() - this->pptr();
	  if (__buf_len)
	    {
	      const streamsize __remaining = __n - __ret;
	      const streamsize __len = std::min(__buf_len, __remaining);
	      traits_type::copy(this->pptr(), __s, __len);
	      __ret += __len;
	      __s += __len;
	      this->__safe_pbump(__len);
	    }

	  if (__ret < __n)
	    {
	      int_type __c = this->overflow(traits_type::to_int_type(*__s));
	      if (!traits_type::eq_int_type(__c, traits_type::eof()))
		{
		  ++__ret;
		  ++__s;
		}
	      else
		break;
	    }
	}
      return __ret;
    }

  // Conceivably, this could be used to implement buffer-to-buffer
  // copies, if this was ever desired in an un-ambiguous way by the
  // standard.
  template<typename _CharT, typename _Traits>
    streamsize
    __copy_streambufs_eof(basic_streambuf<_CharT, _Traits>* __sbin,
			  basic_streambuf<_CharT, _Traits>* __sbout,
			  bool& __ineof)
    {
      streamsize __ret = 0;
      __ineof = true;
      typename _Traits::int_type __c = __sbin->sgetc();
      while (!_Traits::eq_int_type(__c, _Traits::eof()))
	{
	  __c = __sbout->sputc(_Traits::to_char_type(__c));
	  if (_Traits::eq_int_type(__c, _Traits::eof()))
	    {
	      __ineof = false;
	      break;
	    }
	  ++__ret;
	  __c = __sbin->snextc();
	}
      return __ret;
    }

  template<typename _CharT, typename _Traits>
    inline streamsize
    __copy_streambufs(basic_streambuf<_CharT, _Traits>* __sbin,
		      basic_streambuf<_CharT, _Traits>* __sbout)
    {
      bool __ineof;
      return __copy_streambufs_eof(__sbin, __sbout, __ineof);
    }

  // Inhibit implicit instantiations for required instantiations,
  // which are defined via explicit instantiations elsewhere.
#if _GLIBCXX_EXTERN_TEMPLATE
  extern template class basic_streambuf<char>;

  extern template
    streamsize
    __copy_streambufs(basic_streambuf<char>*,
		      basic_streambuf<char>*);

#ifdef _GLIBCXX_USE_WCHAR_T
  extern template class basic_streambuf<wchar_t>;

  extern template
    streamsize
    __copy_streambufs(basic_streambuf<wchar_t>*,
		      basic_streambuf<wchar_t>*);
#endif
#endif

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif
