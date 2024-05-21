// The template and inlines for the -*- C++ -*- internal _Array helper class.

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

/** @file bits/valarray_array.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{valarray}
 */

// Written by Gabriel Dos Reis <Gabriel.Dos-Reis@DPTMaths.ENS-Cachan.Fr>

#ifndef _VALARRAY_ARRAY_TCC
#define _VALARRAY_ARRAY_TCC 1

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  template<typename _Tp>
    void
    __valarray_fill(_Array<_Tp> __a, size_t __n, _Array<bool> __m,
		    const _Tp& __t)
    {
      _Tp* __p = __a._M_data;
      bool* __ok (__m._M_data);
      for (size_t __i=0; __i < __n; ++__i, ++__ok, ++__p)
	{
	  while (!*__ok)
	  {
	    ++__ok;
	    ++__p;
	  }
	  *__p = __t;
	}
    }

  // Copy n elements of a into consecutive elements of b.  When m is
  // false, the corresponding element of a is skipped.  m must contain
  // at least n true elements.  a must contain at least n elements and
  // enough elements to match up with m through the nth true element
  // of m.  I.e.  if n is 10, m has 15 elements with 5 false followed
  // by 10 true, a must have 15 elements.
  template<typename _Tp>
    void
    __valarray_copy(_Array<_Tp> __a, _Array<bool> __m, _Array<_Tp> __b,
		    size_t __n)
    {
      _Tp* __p (__a._M_data);
      bool* __ok (__m._M_data);
      for (_Tp* __q = __b._M_data; __q < __b._M_data + __n;
	   ++__q, ++__ok, ++__p)
	{
	  while (! *__ok)
	    {
	      ++__ok;
	      ++__p;
	    }
	  *__q = *__p;
	}
    }

  // Copy n consecutive elements from a into elements of b.  Elements
  // of b are skipped if the corresponding element of m is false.  m
  // must contain at least n true elements.  b must have at least as
  // many elements as the index of the nth true element of m.  I.e. if
  // m has 15 elements with 5 false followed by 10 true, b must have
  // at least 15 elements.
  template<typename _Tp>
    void
    __valarray_copy(_Array<_Tp> __a, size_t __n, _Array<_Tp> __b,
		    _Array<bool> __m)
    {
      _Tp* __q (__b._M_data);
      bool* __ok (__m._M_data);
      for (_Tp* __p = __a._M_data; __p < __a._M_data+__n;
	   ++__p, ++__ok, ++__q)
	{
	  while (! *__ok)
	    {
	      ++__ok;
	      ++__q;
	    }
	  *__q = *__p;
	}
    }

  // Copy n elements from a into elements of b.  Elements of a are
  // skipped if the corresponding element of m is false.  Elements of
  // b are skipped if the corresponding element of k is false.  m and
  // k must contain at least n true elements.  a and b must have at
  // least as many elements as the index of the nth true element of m.
  template<typename _Tp>
    void
    __valarray_copy(_Array<_Tp> __a, _Array<bool> __m, size_t __n,
		    _Array<_Tp> __b, _Array<bool> __k)
    {
      _Tp* __p (__a._M_data);
      _Tp* __q (__b._M_data);
      bool* __srcok (__m._M_data);
      bool* __dstok (__k._M_data);
      for (size_t __i = 0; __i < __n;
	   ++__srcok, ++__p, ++__dstok, ++__q, ++__i)
	{
	  while (! *__srcok)
	    {
	      ++__srcok;
	      ++__p;
	    }
	  while (! *__dstok) 
	    {
	      ++__dstok;
	      ++__q;
	    }
	  *__q = *__p;
	}
    }

  // Copy n consecutive elements of e into consecutive elements of a.
  // I.e. a[i] = e[i].
  template<typename _Tp, class _Dom>
    void
    __valarray_copy(const _Expr<_Dom, _Tp>& __e, size_t __n, _Array<_Tp> __a)
    {
      _Tp* __p (__a._M_data);
      for (size_t __i = 0; __i < __n; ++__i, ++__p)
	*__p = __e[__i];
    }

  // Copy n consecutive elements of e into elements of a using stride
  // s.  I.e., a[0] = e[0], a[s] = e[1], a[2*s] = e[2].
  template<typename _Tp, class _Dom>
    void
    __valarray_copy(const _Expr<_Dom, _Tp>& __e, size_t __n,
		     _Array<_Tp> __a, size_t __s)
    {
      _Tp* __p (__a._M_data);
      for (size_t __i = 0; __i < __n; ++__i, __p += __s)
	*__p = __e[__i];
    }

  // Copy n consecutive elements of e into elements of a indexed by
  // contents of i.  I.e., a[i[0]] = e[0].
  template<typename _Tp, class _Dom>
    void
    __valarray_copy(const _Expr<_Dom, _Tp>& __e, size_t __n,
		    _Array<_Tp> __a, _Array<size_t> __i)
    {
      size_t* __j (__i._M_data);
      for (size_t __k = 0; __k < __n; ++__k, ++__j)
	__a._M_data[*__j] = __e[__k];
    }

  // Copy n elements of e indexed by contents of f into elements of a
  // indexed by contents of i.  I.e., a[i[0]] = e[f[0]].
  template<typename _Tp>
    void
    __valarray_copy(_Array<_Tp> __e, _Array<size_t> __f,
		    size_t __n, 
		    _Array<_Tp> __a, _Array<size_t> __i)
    {
      size_t* __g (__f._M_data);
      size_t* __j (__i._M_data);
      for (size_t __k = 0; __k < __n; ++__k, ++__j, ++__g) 
	__a._M_data[*__j] = __e._M_data[*__g];
    }

  // Copy n consecutive elements of e into elements of a.  Elements of
  // a are skipped if the corresponding element of m is false.  m must
  // have at least n true elements and a must have at least as many
  // elements as the index of the nth true element of m.  I.e. if m
  // has 5 false followed by 10 true elements and n == 10, a must have
  // at least 15 elements.
  template<typename _Tp, class _Dom>
    void
    __valarray_copy(const _Expr<_Dom, _Tp>& __e, size_t __n,
		    _Array<_Tp> __a, _Array<bool> __m)
    {
      bool* __ok (__m._M_data);
      _Tp* __p (__a._M_data);
      for (size_t __i = 0; __i < __n; ++__i, ++__ok, ++__p)
	{
	  while (! *__ok)
	    {
	      ++__ok;
	      ++__p;
	    }
	  *__p = __e[__i];
	}
    }


  template<typename _Tp, class _Dom>
    void
    __valarray_copy_construct(const _Expr<_Dom, _Tp>& __e, size_t __n,
			      _Array<_Tp> __a)
    {
      _Tp* __p (__a._M_data);
      for (size_t __i = 0; __i < __n; ++__i, ++__p)
	new (__p) _Tp(__e[__i]);
    }


  template<typename _Tp>
    void
    __valarray_copy_construct(_Array<_Tp> __a, _Array<bool> __m,
			      _Array<_Tp> __b, size_t __n)
    {
      _Tp* __p (__a._M_data);
      bool* __ok (__m._M_data);
      for (_Tp* __q = __b._M_data; __q < __b._M_data+__n; ++__q, ++__ok, ++__p)
	{
	  while (! *__ok)
	    {
	      ++__ok;
	      ++__p;
	    }
	  new (__q) _Tp(*__p);
	}
    }

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif /* _VALARRAY_ARRAY_TCC */
