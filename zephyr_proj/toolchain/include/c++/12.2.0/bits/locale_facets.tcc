// Locale support -*- C++ -*-

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

/** @file bits/locale_facets.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{locale}
 */

#ifndef _LOCALE_FACETS_TCC
#define _LOCALE_FACETS_TCC 1

#pragma GCC system_header

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  // Routine to access a cache for the facet.  If the cache didn't
  // exist before, it gets constructed on the fly.
  template<typename _Facet>
    struct __use_cache
    {
      const _Facet*
      operator() (const locale& __loc) const;
    };

  // Specializations.
  template<typename _CharT>
    struct __use_cache<__numpunct_cache<_CharT> >
    {
      const __numpunct_cache<_CharT>*
      operator() (const locale& __loc) const
      {
	const size_t __i = numpunct<_CharT>::id._M_id();
	const locale::facet** __caches = __loc._M_impl->_M_caches;
	if (!__caches[__i])
	  {
	    __numpunct_cache<_CharT>* __tmp = 0;
	    __try
	      {
		__tmp = new __numpunct_cache<_CharT>;
		__tmp->_M_cache(__loc);
	      }
	    __catch(...)
	      {
		delete __tmp;
		__throw_exception_again;
	      }
	    __loc._M_impl->_M_install_cache(__tmp, __i);
	  }
	return static_cast<const __numpunct_cache<_CharT>*>(__caches[__i]);
      }
    };

  template<typename _CharT>
    void
    __numpunct_cache<_CharT>::_M_cache(const locale& __loc)
    {
      const numpunct<_CharT>& __np = use_facet<numpunct<_CharT> >(__loc);

      char* __grouping = 0;
      _CharT* __truename = 0;
      _CharT* __falsename = 0;
      __try
	{
	  const string& __g = __np.grouping();
	  _M_grouping_size = __g.size();
	  __grouping = new char[_M_grouping_size];
	  __g.copy(__grouping, _M_grouping_size);
	  _M_use_grouping = (_M_grouping_size
			     && static_cast<signed char>(__grouping[0]) > 0
			     && (__grouping[0]
				 != __gnu_cxx::__numeric_traits<char>::__max));

	  const basic_string<_CharT>& __tn = __np.truename();
	  _M_truename_size = __tn.size();
	  __truename = new _CharT[_M_truename_size];
	  __tn.copy(__truename, _M_truename_size);

	  const basic_string<_CharT>& __fn = __np.falsename();
	  _M_falsename_size = __fn.size();
	  __falsename = new _CharT[_M_falsename_size];
	  __fn.copy(__falsename, _M_falsename_size);

	  _M_decimal_point = __np.decimal_point();
	  _M_thousands_sep = __np.thousands_sep();

	  const ctype<_CharT>& __ct = use_facet<ctype<_CharT> >(__loc);
	  __ct.widen(__num_base::_S_atoms_out,
		     __num_base::_S_atoms_out
		     + __num_base::_S_oend, _M_atoms_out);
	  __ct.widen(__num_base::_S_atoms_in,
		     __num_base::_S_atoms_in
		     + __num_base::_S_iend, _M_atoms_in);

	  _M_grouping = __grouping;
	  _M_truename = __truename;
	  _M_falsename = __falsename;
	  _M_allocated = true;
	}
      __catch(...)
	{
	  delete [] __grouping;
	  delete [] __truename;
	  delete [] __falsename;
	  __throw_exception_again;
	}
    }

  // Used by both numeric and monetary facets.
  // Check to make sure that the __grouping_tmp string constructed in
  // money_get or num_get matches the canonical grouping for a given
  // locale.
  // __grouping_tmp is parsed L to R
  // 1,222,444 == __grouping_tmp of "\1\3\3"
  // __grouping is parsed R to L
  // 1,222,444 == __grouping of "\3" == "\3\3\3"
  _GLIBCXX_PURE bool
  __verify_grouping(const char* __grouping, size_t __grouping_size,
		    const string& __grouping_tmp) throw ();

_GLIBCXX_BEGIN_NAMESPACE_LDBL

  template<typename _CharT, typename _InIter>
    _GLIBCXX_DEFAULT_ABI_TAG
    _InIter
    num_get<_CharT, _InIter>::
    _M_extract_float(_InIter __beg, _InIter __end, ios_base& __io,
		     ios_base::iostate& __err, string& __xtrc) const
    {
      typedef char_traits<_CharT>			__traits_type;
      typedef __numpunct_cache<_CharT>                  __cache_type;
      __use_cache<__cache_type> __uc;
      const locale& __loc = __io._M_getloc();
      const __cache_type* __lc = __uc(__loc);
      const _CharT* __lit = __lc->_M_atoms_in;
      char_type __c = char_type();

      // True if __beg becomes equal to __end.
      bool __testeof = __beg == __end;

      // First check for sign.
      if (!__testeof)
	{
	  __c = *__beg;
	  const bool __plus = __c == __lit[__num_base::_S_iplus];
	  if ((__plus || __c == __lit[__num_base::_S_iminus])
	      && !(__lc->_M_use_grouping && __c == __lc->_M_thousands_sep)
	      && !(__c == __lc->_M_decimal_point))
	    {
	      __xtrc += __plus ? '+' : '-';
	      if (++__beg != __end)
		__c = *__beg;
	      else
		__testeof = true;
	    }
	}

      // Next, look for leading zeros.
      bool __found_mantissa = false;
      int __sep_pos = 0;
      while (!__testeof)
	{
	  if ((__lc->_M_use_grouping && __c == __lc->_M_thousands_sep)
	      || __c == __lc->_M_decimal_point)
	    break;
	  else if (__c == __lit[__num_base::_S_izero])
	    {
	      if (!__found_mantissa)
		{
		  __xtrc += '0';
		  __found_mantissa = true;
		}
	      ++__sep_pos;

	      if (++__beg != __end)
		__c = *__beg;
	      else
		__testeof = true;
	    }
	  else
	    break;
	}

      // Only need acceptable digits for floating point numbers.
      bool __found_dec = false;
      bool __found_sci = false;
      string __found_grouping;
      if (__lc->_M_use_grouping)
	__found_grouping.reserve(32);
      const char_type* __lit_zero = __lit + __num_base::_S_izero;

      if (!__lc->_M_allocated)
	// "C" locale
	while (!__testeof)
	  {
	    const int __digit = _M_find(__lit_zero, 10, __c);
	    if (__digit != -1)
	      {
		__xtrc += '0' + __digit;
		__found_mantissa = true;
	      }
	    else if (__c == __lc->_M_decimal_point
		     && !__found_dec && !__found_sci)
	      {
		__xtrc += '.';
		__found_dec = true;
	      }
	    else if ((__c == __lit[__num_base::_S_ie] 
		      || __c == __lit[__num_base::_S_iE])
		     && !__found_sci && __found_mantissa)
	      {
		// Scientific notation.
		__xtrc += 'e';
		__found_sci = true;
		
		// Remove optional plus or minus sign, if they exist.
		if (++__beg != __end)
		  {
		    __c = *__beg;
		    const bool __plus = __c == __lit[__num_base::_S_iplus];
		    if (__plus || __c == __lit[__num_base::_S_iminus])
		      __xtrc += __plus ? '+' : '-';
		    else
		      continue;
		  }
		else
		  {
		    __testeof = true;
		    break;
		  }
	      }
	    else
	      break;

	    if (++__beg != __end)
	      __c = *__beg;
	    else
	      __testeof = true;
	  }
      else
	while (!__testeof)
	  {
	    // According to 22.2.2.1.2, p8-9, first look for thousands_sep
	    // and decimal_point.
	    if (__lc->_M_use_grouping && __c == __lc->_M_thousands_sep)
	      {
		if (!__found_dec && !__found_sci)
		  {
		    // NB: Thousands separator at the beginning of a string
		    // is a no-no, as is two consecutive thousands separators.
		    if (__sep_pos)
		      {
			__found_grouping += static_cast<char>(__sep_pos);
			__sep_pos = 0;
		      }
		    else
		      {
			// NB: __convert_to_v will not assign __v and will
			// set the failbit.
			__xtrc.clear();
			break;
		      }
		  }
		else
		  break;
	      }
	    else if (__c == __lc->_M_decimal_point)
	      {
		if (!__found_dec && !__found_sci)
		  {
		    // If no grouping chars are seen, no grouping check
		    // is applied. Therefore __found_grouping is adjusted
		    // only if decimal_point comes after some thousands_sep.
		    if (__found_grouping.size())
		      __found_grouping += static_cast<char>(__sep_pos);
		    __xtrc += '.';
		    __found_dec = true;
		  }
		else
		  break;
	      }
	    else
	      {
		const char_type* __q =
		  __traits_type::find(__lit_zero, 10, __c);
		if (__q)
		  {
		    __xtrc += '0' + (__q - __lit_zero);
		    __found_mantissa = true;
		    ++__sep_pos;
		  }
		else if ((__c == __lit[__num_base::_S_ie] 
			  || __c == __lit[__num_base::_S_iE])
			 && !__found_sci && __found_mantissa)
		  {
		    // Scientific notation.
		    if (__found_grouping.size() && !__found_dec)
		      __found_grouping += static_cast<char>(__sep_pos);
		    __xtrc += 'e';
		    __found_sci = true;
		    
		    // Remove optional plus or minus sign, if they exist.
		    if (++__beg != __end)
		      {
			__c = *__beg;
			const bool __plus = __c == __lit[__num_base::_S_iplus];
			if ((__plus || __c == __lit[__num_base::_S_iminus])
			    && !(__lc->_M_use_grouping
				 && __c == __lc->_M_thousands_sep)
			    && !(__c == __lc->_M_decimal_point))
		      __xtrc += __plus ? '+' : '-';
			else
			  continue;
		      }
		    else
		      {
			__testeof = true;
			break;
		      }
		  }
		else
		  break;
	      }
	    
	    if (++__beg != __end)
	      __c = *__beg;
	    else
	      __testeof = true;
	  }

      // Digit grouping is checked. If grouping and found_grouping don't
      // match, then get very very upset, and set failbit.
      if (__found_grouping.size())
        {
          // Add the ending grouping if a decimal or 'e'/'E' wasn't found.
	  if (!__found_dec && !__found_sci)
	    __found_grouping += static_cast<char>(__sep_pos);

          if (!std::__verify_grouping(__lc->_M_grouping, 
				      __lc->_M_grouping_size,
				      __found_grouping))
	    __err = ios_base::failbit;
        }

      return __beg;
    }

  template<typename _CharT, typename _InIter>
    template<typename _ValueT>
      _GLIBCXX_DEFAULT_ABI_TAG
      _InIter
      num_get<_CharT, _InIter>::
      _M_extract_int(_InIter __beg, _InIter __end, ios_base& __io,
		     ios_base::iostate& __err, _ValueT& __v) const
      {
        typedef char_traits<_CharT>			    __traits_type;
	using __gnu_cxx::__add_unsigned;
	typedef typename __add_unsigned<_ValueT>::__type    __unsigned_type;
	typedef __numpunct_cache<_CharT>                    __cache_type;
	__use_cache<__cache_type> __uc;
	const locale& __loc = __io._M_getloc();
	const __cache_type* __lc = __uc(__loc);
	const _CharT* __lit = __lc->_M_atoms_in;
	char_type __c = char_type();

	// NB: Iff __basefield == 0, __base can change based on contents.
	const ios_base::fmtflags __basefield = __io.flags()
	                                       & ios_base::basefield;
	const bool __oct = __basefield == ios_base::oct;
	int __base = __oct ? 8 : (__basefield == ios_base::hex ? 16 : 10);

	// True if __beg becomes equal to __end.
	bool __testeof = __beg == __end;

	// First check for sign.
	bool __negative = false;
	if (!__testeof)
	  {
	    __c = *__beg;
	    __negative = __c == __lit[__num_base::_S_iminus];
	    if ((__negative || __c == __lit[__num_base::_S_iplus])
		&& !(__lc->_M_use_grouping && __c == __lc->_M_thousands_sep)
		&& !(__c == __lc->_M_decimal_point))
	      {
		if (++__beg != __end)
		  __c = *__beg;
		else
		  __testeof = true;
	      }
	  }

	// Next, look for leading zeros and check required digits
	// for base formats.
	bool __found_zero = false;
	int __sep_pos = 0;
	while (!__testeof)
	  {
	    if ((__lc->_M_use_grouping && __c == __lc->_M_thousands_sep)
		|| __c == __lc->_M_decimal_point)
	      break;
	    else if (__c == __lit[__num_base::_S_izero] 
		     && (!__found_zero || __base == 10))
	      {
		__found_zero = true;
		++__sep_pos;
		if (__basefield == 0)
		  __base = 8;
		if (__base == 8)
		  __sep_pos = 0;
	      }
	    else if (__found_zero
		     && (__c == __lit[__num_base::_S_ix]
			 || __c == __lit[__num_base::_S_iX]))
	      {
		if (__basefield == 0)
		  __base = 16;
		if (__base == 16)
		  {
		    __found_zero = false;
		    __sep_pos = 0;
		  }
		else
		  break;
	      }
	    else
	      break;

	    if (++__beg != __end)
	      {
		__c = *__beg;
		if (!__found_zero)
		  break;
	      }
	    else
	      __testeof = true;
	  }
	
	// At this point, base is determined. If not hex, only allow
	// base digits as valid input.
	const size_t __len = (__base == 16 ? __num_base::_S_iend
			      - __num_base::_S_izero : __base);

	// Extract.
	typedef __gnu_cxx::__numeric_traits<_ValueT> __num_traits;
	string __found_grouping;
	if (__lc->_M_use_grouping)
	  __found_grouping.reserve(32);
	bool __testfail = false;
	bool __testoverflow = false;
	const __unsigned_type __max =
	  (__negative && __num_traits::__is_signed)
	  ? -static_cast<__unsigned_type>(__num_traits::__min)
	  : __num_traits::__max;
	const __unsigned_type __smax = __max / __base;
	__unsigned_type __result = 0;
	int __digit = 0;
	const char_type* __lit_zero = __lit + __num_base::_S_izero;

	if (!__lc->_M_allocated)
	  // "C" locale
	  while (!__testeof)
	    {
	      __digit = _M_find(__lit_zero, __len, __c);
	      if (__digit == -1)
		break;
	      
	      if (__result > __smax)
		__testoverflow = true;
	      else
		{
		  __result *= __base;
		  __testoverflow |= __result > __max - __digit;
		  __result += __digit;
		  ++__sep_pos;
		}
	      
	      if (++__beg != __end)
		__c = *__beg;
	      else
		__testeof = true;
	    }
	else
	  while (!__testeof)
	    {
	      // According to 22.2.2.1.2, p8-9, first look for thousands_sep
	      // and decimal_point.
	      if (__lc->_M_use_grouping && __c == __lc->_M_thousands_sep)
		{
		  // NB: Thousands separator at the beginning of a string
		  // is a no-no, as is two consecutive thousands separators.
		  if (__sep_pos)
		    {
		      __found_grouping += static_cast<char>(__sep_pos);
		      __sep_pos = 0;
		    }
		  else
		    {
		      __testfail = true;
		      break;
		    }
		}
	      else if (__c == __lc->_M_decimal_point)
		break;
	      else
		{
		  const char_type* __q =
		    __traits_type::find(__lit_zero, __len, __c);
		  if (!__q)
		    break;
		  
		  __digit = __q - __lit_zero;
		  if (__digit > 15)
		    __digit -= 6;
		  if (__result > __smax)
		    __testoverflow = true;
		  else
		    {
		      __result *= __base;
		      __testoverflow |= __result > __max - __digit;
		      __result += __digit;
		      ++__sep_pos;
		    }
		}
	      
	      if (++__beg != __end)
		__c = *__beg;
	      else
		__testeof = true;
	    }
	
	// Digit grouping is checked. If grouping and found_grouping don't
	// match, then get very very upset, and set failbit.
	if (__found_grouping.size())
	  {
	    // Add the ending grouping.
	    __found_grouping += static_cast<char>(__sep_pos);

	    if (!std::__verify_grouping(__lc->_M_grouping,
					__lc->_M_grouping_size,
					__found_grouping))
	      __err = ios_base::failbit;
	  }

	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 23. Num_get overflow result.
	if ((!__sep_pos && !__found_zero && !__found_grouping.size())
	    || __testfail)
	  {
	    __v = 0;
	    __err = ios_base::failbit;
	  }
	else if (__testoverflow)
	  {
	    if (__negative && __num_traits::__is_signed)
	      __v = __num_traits::__min;
	    else
	      __v = __num_traits::__max;
	    __err = ios_base::failbit;
	  }
	else
	  __v = __negative ? -__result : __result;

	if (__testeof)
	  __err |= ios_base::eofbit;
	return __beg;
      }

  // _GLIBCXX_RESOLVE_LIB_DEFECTS
  // 17.  Bad bool parsing
  template<typename _CharT, typename _InIter>
    _InIter
    num_get<_CharT, _InIter>::
    do_get(iter_type __beg, iter_type __end, ios_base& __io,
           ios_base::iostate& __err, bool& __v) const
    {
      if (!(__io.flags() & ios_base::boolalpha))
        {
	  // Parse bool values as long.
          // NB: We can't just call do_get(long) here, as it might
          // refer to a derived class.
	  long __l = -1;
          __beg = _M_extract_int(__beg, __end, __io, __err, __l);
	  if (__l == 0 || __l == 1)
	    __v = bool(__l);
	  else
	    {
	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 23. Num_get overflow result.
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

  template<typename _CharT, typename _InIter>
    _InIter
    num_get<_CharT, _InIter>::
    do_get(iter_type __beg, iter_type __end, ios_base& __io,
	   ios_base::iostate& __err, float& __v) const
    {
      string __xtrc;
      __xtrc.reserve(32);
      __beg = _M_extract_float(__beg, __end, __io, __err, __xtrc);
      std::__convert_to_v(__xtrc.c_str(), __v, __err, _S_get_c_locale());
      if (__beg == __end)
	__err |= ios_base::eofbit;
      return __beg;
    }

  template<typename _CharT, typename _InIter>
    _InIter
    num_get<_CharT, _InIter>::
    do_get(iter_type __beg, iter_type __end, ios_base& __io,
           ios_base::iostate& __err, double& __v) const
    {
      string __xtrc;
      __xtrc.reserve(32);
      __beg = _M_extract_float(__beg, __end, __io, __err, __xtrc);
      std::__convert_to_v(__xtrc.c_str(), __v, __err, _S_get_c_locale());
      if (__beg == __end)
	__err |= ios_base::eofbit;
      return __beg;
    }

#if defined _GLIBCXX_LONG_DOUBLE_COMPAT && defined __LONG_DOUBLE_128__
  template<typename _CharT, typename _InIter>
    _InIter
    num_get<_CharT, _InIter>::
    __do_get(iter_type __beg, iter_type __end, ios_base& __io,
	     ios_base::iostate& __err, double& __v) const
    {
      string __xtrc;
      __xtrc.reserve(32);
      __beg = _M_extract_float(__beg, __end, __io, __err, __xtrc);
      std::__convert_to_v(__xtrc.c_str(), __v, __err, _S_get_c_locale());
      if (__beg == __end)
	__err |= ios_base::eofbit;
      return __beg;
    }
#endif

  template<typename _CharT, typename _InIter>
    _InIter
    num_get<_CharT, _InIter>::
    do_get(iter_type __beg, iter_type __end, ios_base& __io,
           ios_base::iostate& __err, long double& __v) const
    {
      string __xtrc;
      __xtrc.reserve(32);
      __beg = _M_extract_float(__beg, __end, __io, __err, __xtrc);
      std::__convert_to_v(__xtrc.c_str(), __v, __err, _S_get_c_locale());
      if (__beg == __end)
	__err |= ios_base::eofbit;
      return __beg;
    }

  template<typename _CharT, typename _InIter>
    _InIter
    num_get<_CharT, _InIter>::
    do_get(iter_type __beg, iter_type __end, ios_base& __io,
           ios_base::iostate& __err, void*& __v) const
    {
      // Prepare for hex formatted input.
      typedef ios_base::fmtflags        fmtflags;
      const fmtflags __fmt = __io.flags();
      __io.flags((__fmt & ~ios_base::basefield) | ios_base::hex);

      typedef __gnu_cxx::__conditional_type<(sizeof(void*)
					     <= sizeof(unsigned long)),
	unsigned long, unsigned long long>::__type _UIntPtrType;       

      _UIntPtrType __ul;
      __beg = _M_extract_int(__beg, __end, __io, __err, __ul);

      // Reset from hex formatted input.
      __io.flags(__fmt);

      __v = reinterpret_cast<void*>(__ul);
      return __beg;
    }

#if defined _GLIBCXX_LONG_DOUBLE_ALT128_COMPAT \
      && defined __LONG_DOUBLE_IEEE128__
  template<typename _CharT, typename _InIter>
    _InIter
    num_get<_CharT, _InIter>::
    __do_get(iter_type __beg, iter_type __end, ios_base& __io,
	     ios_base::iostate& __err, __ibm128& __v) const
    {
      string __xtrc;
      __xtrc.reserve(32);
      __beg = _M_extract_float(__beg, __end, __io, __err, __xtrc);
      std::__convert_to_v(__xtrc.c_str(), __v, __err, _S_get_c_locale());
      if (__beg == __end)
	__err |= ios_base::eofbit;
      return __beg;
    }
#endif

  // For use by integer and floating-point types after they have been
  // converted into a char_type string.
  template<typename _CharT, typename _OutIter>
    void
    num_put<_CharT, _OutIter>::
    _M_pad(_CharT __fill, streamsize __w, ios_base& __io,
	   _CharT* __new, const _CharT* __cs, int& __len) const
    {
      // [22.2.2.2.2] Stage 3.
      // If necessary, pad.
      __pad<_CharT, char_traits<_CharT> >::_S_pad(__io, __fill, __new,
						  __cs, __w, __len);
      __len = static_cast<int>(__w);
    }

_GLIBCXX_END_NAMESPACE_LDBL

  template<typename _CharT, typename _ValueT>
    int
    __int_to_char(_CharT* __bufend, _ValueT __v, const _CharT* __lit,
		  ios_base::fmtflags __flags, bool __dec)
    {
      _CharT* __buf = __bufend;
      if (__builtin_expect(__dec, true))
	{
	  // Decimal.
	  do
	    {
	      *--__buf = __lit[(__v % 10) + __num_base::_S_odigits];
	      __v /= 10;
	    }
	  while (__v != 0);
	}
      else if ((__flags & ios_base::basefield) == ios_base::oct)
	{
	  // Octal.
	  do
	    {
	      *--__buf = __lit[(__v & 0x7) + __num_base::_S_odigits];
	      __v >>= 3;
	    }
	  while (__v != 0);
	}
      else
	{
	  // Hex.
	  const bool __uppercase = __flags & ios_base::uppercase;
	  const int __case_offset = __uppercase ? __num_base::_S_oudigits
	                                        : __num_base::_S_odigits;
	  do
	    {
	      *--__buf = __lit[(__v & 0xf) + __case_offset];
	      __v >>= 4;
	    }
	  while (__v != 0);
	}
      return __bufend - __buf;
    }

_GLIBCXX_BEGIN_NAMESPACE_LDBL

  template<typename _CharT, typename _OutIter>
    void
    num_put<_CharT, _OutIter>::
    _M_group_int(const char* __grouping, size_t __grouping_size, _CharT __sep,
		 ios_base&, _CharT* __new, _CharT* __cs, int& __len) const
    {
      _CharT* __p = std::__add_grouping(__new, __sep, __grouping,
					__grouping_size, __cs, __cs + __len);
      __len = __p - __new;
    }
  
  template<typename _CharT, typename _OutIter>
    template<typename _ValueT>
      _OutIter
      num_put<_CharT, _OutIter>::
      _M_insert_int(_OutIter __s, ios_base& __io, _CharT __fill,
		    _ValueT __v) const
      {
	using __gnu_cxx::__add_unsigned;
	typedef typename __add_unsigned<_ValueT>::__type __unsigned_type;
	typedef __numpunct_cache<_CharT>	             __cache_type;
	__use_cache<__cache_type> __uc;
	const locale& __loc = __io._M_getloc();
	const __cache_type* __lc = __uc(__loc);
	const _CharT* __lit = __lc->_M_atoms_out;
	const ios_base::fmtflags __flags = __io.flags();

	// Long enough to hold hex, dec, and octal representations.
	const int __ilen = 5 * sizeof(_ValueT);
	_CharT* __cs = static_cast<_CharT*>(__builtin_alloca(sizeof(_CharT)
							     * __ilen));

	// [22.2.2.2.2] Stage 1, numeric conversion to character.
	// Result is returned right-justified in the buffer.
	const ios_base::fmtflags __basefield = __flags & ios_base::basefield;
	const bool __dec = (__basefield != ios_base::oct
			    && __basefield != ios_base::hex);
	const __unsigned_type __u = ((__v > 0 || !__dec)
				     ? __unsigned_type(__v)
				     : -__unsigned_type(__v));
 	int __len = __int_to_char(__cs + __ilen, __u, __lit, __flags, __dec);
	__cs += __ilen - __len;

	// Add grouping, if necessary.
	if (__lc->_M_use_grouping)
	  {
	    // Grouping can add (almost) as many separators as the number
	    // of digits + space is reserved for numeric base or sign.
	    _CharT* __cs2 = static_cast<_CharT*>(__builtin_alloca(sizeof(_CharT)
								  * (__len + 1)
								  * 2));
	    _M_group_int(__lc->_M_grouping, __lc->_M_grouping_size,
			 __lc->_M_thousands_sep, __io, __cs2 + 2, __cs, __len);
	    __cs = __cs2 + 2;
	  }

	// Complete Stage 1, prepend numeric base or sign.
	if (__builtin_expect(__dec, true))
	  {
	    // Decimal.
	    if (__v >= 0)
	      {
		if (bool(__flags & ios_base::showpos)
		    && __gnu_cxx::__numeric_traits<_ValueT>::__is_signed)
		  *--__cs = __lit[__num_base::_S_oplus], ++__len;
	      }
	    else
	      *--__cs = __lit[__num_base::_S_ominus], ++__len;
	  }
	else if (bool(__flags & ios_base::showbase) && __v)
	  {
	    if (__basefield == ios_base::oct)
	      *--__cs = __lit[__num_base::_S_odigits], ++__len;
	    else
	      {
		// 'x' or 'X'
		const bool __uppercase = __flags & ios_base::uppercase;
		*--__cs = __lit[__num_base::_S_ox + __uppercase];
		// '0'
		*--__cs = __lit[__num_base::_S_odigits];
		__len += 2;
	      }
	  }

	// Pad.
	const streamsize __w = __io.width();
	if (__w > static_cast<streamsize>(__len))
	  {
	    _CharT* __cs3 = static_cast<_CharT*>(__builtin_alloca(sizeof(_CharT)
								  * __w));
	    _M_pad(__fill, __w, __io, __cs3, __cs, __len);
	    __cs = __cs3;
	  }
	__io.width(0);

	// [22.2.2.2.2] Stage 4.
	// Write resulting, fully-formatted string to output iterator.
	return std::__write(__s, __cs, __len);
      }

  template<typename _CharT, typename _OutIter>
    void
    num_put<_CharT, _OutIter>::
    _M_group_float(const char* __grouping, size_t __grouping_size,
		   _CharT __sep, const _CharT* __p, _CharT* __new,
		   _CharT* __cs, int& __len) const
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 282. What types does numpunct grouping refer to?
      // Add grouping, if necessary.
      const int __declen = __p ? __p - __cs : __len;
      _CharT* __p2 = std::__add_grouping(__new, __sep, __grouping,
					 __grouping_size,
					 __cs, __cs + __declen);

      // Tack on decimal part.
      int __newlen = __p2 - __new;
      if (__p)
	{
	  char_traits<_CharT>::copy(__p2, __p, __len - __declen);
	  __newlen += __len - __declen;
	}
      __len = __newlen;
    }

  // The following code uses vsnprintf (or vsprintf(), when
  // _GLIBCXX_USE_C99_STDIO is not defined) to convert floating point
  // values for insertion into a stream.  An optimization would be to
  // replace them with code that works directly on a wide buffer and
  // then use __pad to do the padding.  It would be good to replace
  // them anyway to gain back the efficiency that C++ provides by
  // knowing up front the type of the values to insert.  Also, sprintf
  // is dangerous since may lead to accidental buffer overruns.  This
  // implementation follows the C++ standard fairly directly as
  // outlined in 22.2.2.2 [lib.locale.num.put]
  template<typename _CharT, typename _OutIter>
    template<typename _ValueT>
      _OutIter
      num_put<_CharT, _OutIter>::
      _M_insert_float(_OutIter __s, ios_base& __io, _CharT __fill, char __mod,
		       _ValueT __v) const
      {
	typedef __numpunct_cache<_CharT>                __cache_type;
	__use_cache<__cache_type> __uc;
	const locale& __loc = __io._M_getloc();
	const __cache_type* __lc = __uc(__loc);

	// Use default precision if out of range.
	const streamsize __prec = __io.precision() < 0 ? 6 : __io.precision();

	const int __max_digits =
	  __gnu_cxx::__numeric_traits<_ValueT>::__digits10;

	// [22.2.2.2.2] Stage 1, numeric conversion to character.
	int __len;
	// Long enough for the max format spec.
	char __fbuf[16];
	__num_base::_S_format_float(__io, __fbuf, __mod);

#if _GLIBCXX_USE_C99_STDIO && !_GLIBCXX_HAVE_BROKEN_VSNPRINTF
	// Precision is always used except for hexfloat format.
	const bool __use_prec =
	  (__io.flags() & ios_base::floatfield) != ios_base::floatfield;

	// First try a buffer perhaps big enough (most probably sufficient
	// for non-ios_base::fixed outputs)
	int __cs_size = __max_digits * 3;
	char* __cs = static_cast<char*>(__builtin_alloca(__cs_size));
	if (__use_prec)
	  __len = std::__convert_from_v(_S_get_c_locale(), __cs, __cs_size,
					__fbuf, __prec, __v);
	else
	  __len = std::__convert_from_v(_S_get_c_locale(), __cs, __cs_size,
					__fbuf, __v);

	// If the buffer was not large enough, try again with the correct size.
	if (__len >= __cs_size)
	  {
	    __cs_size = __len + 1;
	    __cs = static_cast<char*>(__builtin_alloca(__cs_size));
	    if (__use_prec)
	      __len = std::__convert_from_v(_S_get_c_locale(), __cs, __cs_size,
					    __fbuf, __prec, __v);
	    else
	      __len = std::__convert_from_v(_S_get_c_locale(), __cs, __cs_size,
					    __fbuf, __v);
	  }
#else
	// Consider the possibility of long ios_base::fixed outputs
	const bool __fixed = __io.flags() & ios_base::fixed;
	const int __max_exp =
	  __gnu_cxx::__numeric_traits<_ValueT>::__max_exponent10;

	// The size of the output string is computed as follows.
	// ios_base::fixed outputs may need up to __max_exp + 1 chars
	// for the integer part + __prec chars for the fractional part
	// + 3 chars for sign, decimal point, '\0'. On the other hand,
	// for non-fixed outputs __max_digits * 2 + __prec chars are
	// largely sufficient.
	const int __cs_size = __fixed ? __max_exp + __prec + 4
	                              : __max_digits * 2 + __prec;
	char* __cs = static_cast<char*>(__builtin_alloca(__cs_size));
	__len = std::__convert_from_v(_S_get_c_locale(), __cs, 0, __fbuf, 
				      __prec, __v);
#endif

	// [22.2.2.2.2] Stage 2, convert to char_type, using correct
	// numpunct.decimal_point() values for '.' and adding grouping.
	const ctype<_CharT>& __ctype = use_facet<ctype<_CharT> >(__loc);
	
	_CharT* __ws = static_cast<_CharT*>(__builtin_alloca(sizeof(_CharT)
							     * __len));
	__ctype.widen(__cs, __cs + __len, __ws);
	
	// Replace decimal point.
	_CharT* __wp = 0;
	const char* __p = char_traits<char>::find(__cs, __len, '.');
	if (__p)
	  {
	    __wp = __ws + (__p - __cs);
	    *__wp = __lc->_M_decimal_point;
	  }
	
	// Add grouping, if necessary.
	// N.B. Make sure to not group things like 2e20, i.e., no decimal
	// point, scientific notation.
	if (__lc->_M_use_grouping
	    && (__wp || __len < 3 || (__cs[1] <= '9' && __cs[2] <= '9'
				      && __cs[1] >= '0' && __cs[2] >= '0')))
	  {
	    // Grouping can add (almost) as many separators as the
	    // number of digits, but no more.
	    _CharT* __ws2 = static_cast<_CharT*>(__builtin_alloca(sizeof(_CharT)
								  * __len * 2));
	    
	    streamsize __off = 0;
	    if (__cs[0] == '-' || __cs[0] == '+')
	      {
		__off = 1;
		__ws2[0] = __ws[0];
		__len -= 1;
	      }
	    
	    _M_group_float(__lc->_M_grouping, __lc->_M_grouping_size,
			   __lc->_M_thousands_sep, __wp, __ws2 + __off,
			   __ws + __off, __len);
	    __len += __off;
	    
	    __ws = __ws2;
	  }

	// Pad.
	const streamsize __w = __io.width();
	if (__w > static_cast<streamsize>(__len))
	  {
	    _CharT* __ws3 = static_cast<_CharT*>(__builtin_alloca(sizeof(_CharT)
								  * __w));
	    _M_pad(__fill, __w, __io, __ws3, __ws, __len);
	    __ws = __ws3;
	  }
	__io.width(0);
	
	// [22.2.2.2.2] Stage 4.
	// Write resulting, fully-formatted string to output iterator.
	return std::__write(__s, __ws, __len);
      }
  
  template<typename _CharT, typename _OutIter>
    _OutIter
    num_put<_CharT, _OutIter>::
    do_put(iter_type __s, ios_base& __io, char_type __fill, bool __v) const
    {
      const ios_base::fmtflags __flags = __io.flags();
      if ((__flags & ios_base::boolalpha) == 0)
        {
          const long __l = __v;
          __s = _M_insert_int(__s, __io, __fill, __l);
        }
      else
        {
	  typedef __numpunct_cache<_CharT>              __cache_type;
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

  template<typename _CharT, typename _OutIter>
    _OutIter
    num_put<_CharT, _OutIter>::
    do_put(iter_type __s, ios_base& __io, char_type __fill, double __v) const
    { return _M_insert_float(__s, __io, __fill, char(), __v); }

#if defined _GLIBCXX_LONG_DOUBLE_COMPAT && defined __LONG_DOUBLE_128__
  template<typename _CharT, typename _OutIter>
    _OutIter
    num_put<_CharT, _OutIter>::
    __do_put(iter_type __s, ios_base& __io, char_type __fill, double __v) const
    { return _M_insert_float(__s, __io, __fill, char(), __v); }
#endif

  template<typename _CharT, typename _OutIter>
    _OutIter
    num_put<_CharT, _OutIter>::
    do_put(iter_type __s, ios_base& __io, char_type __fill,
	   long double __v) const
    { return _M_insert_float(__s, __io, __fill, 'L', __v); }

  template<typename _CharT, typename _OutIter>
    _OutIter
    num_put<_CharT, _OutIter>::
    do_put(iter_type __s, ios_base& __io, char_type __fill,
           const void* __v) const
    {
      const ios_base::fmtflags __flags = __io.flags();
      const ios_base::fmtflags __fmt = ~(ios_base::basefield
					 | ios_base::uppercase);
      __io.flags((__flags & __fmt) | (ios_base::hex | ios_base::showbase));

      typedef __gnu_cxx::__conditional_type<(sizeof(const void*)
					     <= sizeof(unsigned long)),
	unsigned long, unsigned long long>::__type _UIntPtrType;       

      __s = _M_insert_int(__s, __io, __fill,
			  reinterpret_cast<_UIntPtrType>(__v));
      __io.flags(__flags);
      return __s;
    }

#if defined _GLIBCXX_LONG_DOUBLE_ALT128_COMPAT \
      && defined __LONG_DOUBLE_IEEE128__
  template<typename _CharT, typename _OutIter>
    _OutIter
    num_put<_CharT, _OutIter>::
    __do_put(iter_type __s, ios_base& __io, char_type __fill,
	     __ibm128 __v) const
    { return _M_insert_float(__s, __io, __fill, 'L', __v); }
#endif
_GLIBCXX_END_NAMESPACE_LDBL

  // Construct correctly padded string, as per 22.2.2.2.2
  // Assumes
  // __newlen > __oldlen
  // __news is allocated for __newlen size

  // NB: Of the two parameters, _CharT can be deduced from the
  // function arguments. The other (_Traits) has to be explicitly specified.
  template<typename _CharT, typename _Traits>
    void
    __pad<_CharT, _Traits>::_S_pad(ios_base& __io, _CharT __fill,
				   _CharT* __news, const _CharT* __olds,
				   streamsize __newlen, streamsize __oldlen)
    {
      const size_t __plen = static_cast<size_t>(__newlen - __oldlen);
      const ios_base::fmtflags __adjust = __io.flags() & ios_base::adjustfield;

      // Padding last.
      if (__adjust == ios_base::left)
	{
	  _Traits::copy(__news, __olds, __oldlen);
	  _Traits::assign(__news + __oldlen, __plen, __fill);
	  return;
	}

      size_t __mod = 0;
      if (__adjust == ios_base::internal)
	{
	  // Pad after the sign, if there is one.
	  // Pad after 0[xX], if there is one.
	  // Who came up with these rules, anyway? Jeeze.
          const locale& __loc = __io._M_getloc();
	  const ctype<_CharT>& __ctype = use_facet<ctype<_CharT> >(__loc);

	  if (__ctype.widen('-') == __olds[0]
	      || __ctype.widen('+') == __olds[0])
	    {
	      __news[0] = __olds[0];
	      __mod = 1;
	      ++__news;
	    }
	  else if (__ctype.widen('0') == __olds[0]
		   && __oldlen > 1
		   && (__ctype.widen('x') == __olds[1]
		       || __ctype.widen('X') == __olds[1]))
	    {
	      __news[0] = __olds[0];
	      __news[1] = __olds[1];
	      __mod = 2;
	      __news += 2;
	    }
	  // else Padding first.
	}
      _Traits::assign(__news, __plen, __fill);
      _Traits::copy(__news + __plen, __olds + __mod, __oldlen - __mod);
    }

  template<typename _CharT>
    _CharT*
    __add_grouping(_CharT* __s, _CharT __sep,
		   const char* __gbeg, size_t __gsize,
		   const _CharT* __first, const _CharT* __last)
    {
      size_t __idx = 0;
      size_t __ctr = 0;

      while (__last - __first > __gbeg[__idx]
	     && static_cast<signed char>(__gbeg[__idx]) > 0
	     && __gbeg[__idx] != __gnu_cxx::__numeric_traits<char>::__max)
	{
	  __last -= __gbeg[__idx];
	  __idx < __gsize - 1 ? ++__idx : ++__ctr;
	}

      while (__first != __last)
	*__s++ = *__first++;

      while (__ctr--)
	{
	  *__s++ = __sep;	  
	  for (char __i = __gbeg[__idx]; __i > 0; --__i)
	    *__s++ = *__first++;
	}

      while (__idx--)
	{
	  *__s++ = __sep;	  
	  for (char __i = __gbeg[__idx]; __i > 0; --__i)
	    *__s++ = *__first++;
	}

      return __s;
    }

  // Inhibit implicit instantiations for required instantiations,
  // which are defined via explicit instantiations elsewhere.
#if _GLIBCXX_EXTERN_TEMPLATE
  extern template class _GLIBCXX_NAMESPACE_CXX11 numpunct<char>;
  extern template class _GLIBCXX_NAMESPACE_CXX11 numpunct_byname<char>;
  extern template class _GLIBCXX_NAMESPACE_LDBL num_get<char>;
  extern template class _GLIBCXX_NAMESPACE_LDBL num_put<char>;
  extern template class ctype_byname<char>;

  extern template
    const ctype<char>&
    use_facet<ctype<char> >(const locale&);

  extern template
    const numpunct<char>&
    use_facet<numpunct<char> >(const locale&);

  extern template
    const num_put<char>&
    use_facet<num_put<char> >(const locale&);

  extern template
    const num_get<char>&
    use_facet<num_get<char> >(const locale&);

  extern template
    bool
    has_facet<ctype<char> >(const locale&);

  extern template
    bool
    has_facet<numpunct<char> >(const locale&);

  extern template
    bool
    has_facet<num_put<char> >(const locale&);

  extern template
    bool
    has_facet<num_get<char> >(const locale&);

#ifdef _GLIBCXX_USE_WCHAR_T
  extern template class _GLIBCXX_NAMESPACE_CXX11 numpunct<wchar_t>;
  extern template class _GLIBCXX_NAMESPACE_CXX11 numpunct_byname<wchar_t>;
  extern template class _GLIBCXX_NAMESPACE_LDBL num_get<wchar_t>;
  extern template class _GLIBCXX_NAMESPACE_LDBL num_put<wchar_t>;
  extern template class ctype_byname<wchar_t>;

  extern template
    const ctype<wchar_t>&
    use_facet<ctype<wchar_t> >(const locale&);

  extern template
    const numpunct<wchar_t>&
    use_facet<numpunct<wchar_t> >(const locale&);

  extern template
    const num_put<wchar_t>&
    use_facet<num_put<wchar_t> >(const locale&);

  extern template
    const num_get<wchar_t>&
    use_facet<num_get<wchar_t> >(const locale&);

 extern template
    bool
    has_facet<ctype<wchar_t> >(const locale&);

  extern template
    bool
    has_facet<numpunct<wchar_t> >(const locale&);

  extern template
    bool
    has_facet<num_put<wchar_t> >(const locale&);

  extern template
    bool
    has_facet<num_get<wchar_t> >(const locale&);
#endif
#endif

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif
