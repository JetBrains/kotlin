// TR2 <bool_set> support files -*- C++ -*-

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

/** @file tr2/bool_set.tcc
 *  This is a TR2 C++ Library header.
 */

#ifndef _GLIBCXX_TR2_BOOL_SET_TCC
#define _GLIBCXX_TR2_BOOL_SET_TCC 1

#pragma GCC system_header

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

namespace tr2
{
  bool_set::_Bool_set_val
  bool_set::_S_not[4] =
  { _S_true_, _S_false, _S_indet, _S_empty };

  bool_set::_Bool_set_val
  bool_set::_S_xor[4][4] =
  { { _S_false, _S_true_, _S_indet, _S_empty },
    { _S_true_, _S_false, _S_indet, _S_empty },
    { _S_indet, _S_indet, _S_indet, _S_empty },
    { _S_empty, _S_empty, _S_empty, _S_empty } };

  bool_set::_Bool_set_val
  bool_set::_S_or[4][4] =
  { { _S_false, _S_true_, _S_indet, _S_empty },
    { _S_true_, _S_true_, _S_true_, _S_empty },
    { _S_indet, _S_true_, _S_indet, _S_empty },
    { _S_empty, _S_empty, _S_empty, _S_empty } };

  bool_set::_Bool_set_val
  bool_set::_S_and[4][4] =
  { { _S_false, _S_false, _S_false, _S_empty },
    { _S_false, _S_true_, _S_indet, _S_empty },
    { _S_false, _S_indet, _S_indet, _S_empty },
    { _S_empty, _S_empty, _S_empty, _S_empty } };

  bool_set::_Bool_set_val
  bool_set::_S_eq[4][4] =
  { { _S_true_, _S_false, _S_indet, _S_empty },
    { _S_false, _S_true_, _S_indet, _S_empty },
    { _S_indet, _S_indet, _S_indet, _S_empty },
    { _S_empty, _S_empty, _S_empty, _S_empty } };
}

_GLIBCXX_END_NAMESPACE_VERSION
}

//  I object to these things.
//  The stuff in locale facets are for basic types.
//  I think we could hack operator<< and operator>>.

      /**
       *  @brief  Numeric parsing.
       *
       *  Parses the input stream into the bool @a v.  It does so by calling
       *  num_get::do_get().
       *
       *  If ios_base::boolalpha is set, attempts to read
       *  ctype<CharT>::truename() or ctype<CharT>::falsename().  Sets
       *  @a v to true or false if successful.  Sets err to
       *  ios_base::failbit if reading the string fails.  Sets err to
       *  ios_base::eofbit if the stream is emptied.
       *
       *  If ios_base::boolalpha is not set, proceeds as with reading a long,
       *  except if the value is 1, sets @a v to true, if the value is 0, sets
       *  @a v to false, and otherwise set err to ios_base::failbit.
       *
       *  @param  in  Start of input stream.
       *  @param  end  End of input stream.
       *  @param  io  Source of locale and flags.
       *  @param  err  Error flags to set.
       *  @param  v  Value to format and insert.
       *  @return  Iterator after reading.
      iter_type
      get(iter_type __in, iter_type __end, ios_base& __io,
	  ios_base::iostate& __err, bool& __v) const
      { return this->do_get(__in, __end, __io, __err, __v); }
       */
/*
  template<typename _CharT, typename _InIter>
    _InIter
    num_get<_CharT, _InIter>::
    do_get(iter_type __beg, iter_type __end, ios_base& __io,
	   ios_base::iostate& __err, bool_set& __v) const
    {
      if (!(__io.flags() & ios_base::boolalpha))
	{
	  // Parse bool values as long.
	  // NB: We can't just call do_get(long) here, as it might
	  // refer to a derived class.
	  long __l = -1;
	  __beg = _M_extract_int(__beg, __end, __io, __err, __l);
	  if (__c >= _S_false && __c < _S_empty)
	    __b._M_b = static_cast<_Bool_set_val>(__c);
	  else
	    {
	      // What should we do here?
	      __v = true;
	      __err = ios_base::failbit;
	      if (__beg == __end)
		__err |= ios_base::eofbit;
	    }
	}
      else
	{
	  // Parse bool values as alphanumeric.
	  typedef __numpunct_cache<_CharT>  __cache_type;
	  __use_cache<__cache_type> __uc;
	  const locale& __loc = __io._M_getloc();
	  const __cache_type* __lc = __uc(__loc);

	  bool __testf = true;
	  bool __testt = true;
	  bool __donef = __lc->_M_falsename_size == 0;
	  bool __donet = __lc->_M_truename_size == 0;
	  bool __testeof = false;
	  size_t __n = 0;
	  while (!__donef || !__donet)
	    {
	      if (__beg == __end)
		{
		  __testeof = true;
		  break;
		}

	      const char_type __c = *__beg;

	      if (!__donef)
		__testf = __c == __lc->_M_falsename[__n];

	      if (!__testf && __donet)
		break;

	      if (!__donet)
		__testt = __c == __lc->_M_truename[__n];

	      if (!__testt && __donef)
		break;

	      if (!__testt && !__testf)
		break;

	      ++__n;
	      ++__beg;

	      __donef = !__testf || __n >= __lc->_M_falsename_size;
	      __donet = !__testt || __n >= __lc->_M_truename_size;
	    }
	  if (__testf && __n == __lc->_M_falsename_size && __n)
	    {
	      __v = false;
	      if (__testt && __n == __lc->_M_truename_size)
		__err = ios_base::failbit;
	      else
		__err = __testeof ? ios_base::eofbit : ios_base::goodbit;
	    }
	  else if (__testt && __n == __lc->_M_truename_size && __n)
	    {
	      __v = true;
	      __err = __testeof ? ios_base::eofbit : ios_base::goodbit;
	    }
	  else
	    {
	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 23. Num_get overflow result.
	      __v = false;
	      __err = ios_base::failbit;
	      if (__testeof)
		__err |= ios_base::eofbit;
	    }
	}
      return __beg;
    }
*/

      /**
       *  @brief  Numeric formatting.
       *
       *  Formats the boolean @a v and inserts it into a stream.  It does so
       *  by calling num_put::do_put().
       *
       *  If ios_base::boolalpha is set, writes ctype<CharT>::truename() or
       *  ctype<CharT>::falsename().  Otherwise formats @a v as an int.
       *
       *  @param  s  Stream to write to.
       *  @param  io  Source of locale and flags.
       *  @param  fill  Char_type to use for filling.
       *  @param  v  Value to format and insert.
       *  @return  Iterator after writing.
      iter_type
      put(iter_type __s, ios_base& __f, char_type __fill, bool __v) const
      { return this->do_put(__s, __f, __fill, __v); }
       */

/*
  template<typename _CharT, typename _OutIter>
    _OutIter
    num_put<_CharT, _OutIter>::
    do_put(iter_type __s, ios_base& __io, char_type __fill, bool_set __v) const
    {
      const ios_base::fmtflags __flags = __io.flags();
      if ((__flags & ios_base::boolalpha) == 0)
	{
	  const long __l = __v;
	  __s = _M_insert_int(__s, __io, __fill, __l);
	}
      else
	{
	  typedef __numpunct_cache<_CharT> __cache_type;
	  __use_cache<__cache_type> __uc;
	  const locale& __loc = __io._M_getloc();
	  const __cache_type* __lc = __uc(__loc);

	  const _CharT* __name = __v ? __lc->_M_truename
				     : __lc->_M_falsename;
	  int __len = __v ? __lc->_M_truename_size
			  : __lc->_M_falsename_size;

	  const streamsize __w = __io.width();
	  if (__w > static_cast<streamsize>(__len))
	    {
	      const streamsize __plen = __w - __len;
	      _CharT* __ps
		= static_cast<_CharT*>(__builtin_alloca(sizeof(_CharT)
							* __plen));

	      char_traits<_CharT>::assign(__ps, __plen, __fill);
	      __io.width(0);

	      if ((__flags & ios_base::adjustfield) == ios_base::left)
		{
		  __s = std::__write(__s, __name, __len);
		  __s = std::__write(__s, __ps, __plen);
		}
	      else
		{
		  __s = std::__write(__s, __ps, __plen);
		  __s = std::__write(__s, __name, __len);
		}
	      return __s;
	    }
	  __io.width(0);
	  __s = std::__write(__s, __name, __len);
	}
      return __s;
    }
*/

#endif // _GLIBCXX_TR2_BOOL_SET_TCC
