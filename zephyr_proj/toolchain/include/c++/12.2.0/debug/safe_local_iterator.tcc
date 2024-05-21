// Debugging iterator implementation (out of line) -*- C++ -*-

// Copyright (C) 2011-2022 Free Software Foundation, Inc.
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

/** @file debug/safe_local_iterator.tcc
 *  This file is a GNU debug extension to the Standard C++ Library.
 */

#ifndef _GLIBCXX_DEBUG_SAFE_LOCAL_ITERATOR_TCC
#define _GLIBCXX_DEBUG_SAFE_LOCAL_ITERATOR_TCC 1

namespace __gnu_debug
{
  template<typename _Iterator, typename _Sequence>
    typename _Distance_traits<_Iterator>::__type
    _Safe_local_iterator<_Iterator, _Sequence>::
    _M_get_distance_to(const _Safe_local_iterator& __rhs) const
    {
      if (base() == __rhs.base())
	return { 0, __dp_exact };

      if (_M_is_begin())
	{
	  if (__rhs._M_is_end())
	    return
	      {
		_M_get_sequence()->bucket_size(bucket()),
		__dp_exact
	      };

	  return { 1, __dp_sign };
	}

      if (_M_is_end())
	{
	  if (__rhs._M_is_begin())
	    return
	      {
		-_M_get_sequence()->bucket_size(bucket()),
		__dp_exact
	      };

	  return { -1, __dp_sign };
	}

      if (__rhs._M_is_begin())
	return { -1, __dp_sign };

      if (__rhs._M_is_end())
	return { 1, __dp_sign };

      return { 1, __dp_equality };
    }

  template<typename _Iterator, typename _Sequence>
    bool
    _Safe_local_iterator<_Iterator, _Sequence>::
    _M_valid_range(const _Safe_local_iterator& __rhs,
		std::pair<difference_type, _Distance_precision>& __dist) const
    {
      if (!_M_can_compare(__rhs))
	return false;

      if (bucket() != __rhs.bucket())
	return false;

      /* Determine if we can order the iterators without the help of
	 the container */
      __dist = _M_get_distance_to(__rhs);
      switch (__dist.second)
	{
	case __dp_equality:
	  if (__dist.first == 0)
	    return true;
	  break;

	case __dp_sign:
	case __dp_exact:
	  return __dist.first >= 0;
	}

      // Assume that this is a valid range; we can't check anything else
      return true;
    }
} // namespace __gnu_debug

#endif
