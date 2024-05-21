// Safe sequence implementation  -*- C++ -*-

// Copyright (C) 2010-2022 Free Software Foundation, Inc.
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

/** @file debug/safe_sequence.tcc
 *  This file is a GNU debug extension to the Standard C++ Library.
 */

#ifndef _GLIBCXX_DEBUG_SAFE_SEQUENCE_TCC
#define _GLIBCXX_DEBUG_SAFE_SEQUENCE_TCC 1

namespace __gnu_debug
{
  template<typename _Sequence>
    template<typename _Predicate>
      void
      _Safe_sequence<_Sequence>::
      _M_invalidate_if(_Predicate __pred)
      {
	typedef typename _Sequence::iterator iterator;
	typedef typename _Sequence::const_iterator const_iterator;

	__gnu_cxx::__scoped_lock sentry(this->_M_get_mutex());
	for (_Safe_iterator_base* __iter = _M_iterators; __iter;)
	  {
	    iterator* __victim = static_cast<iterator*>(__iter);
	    __iter = __iter->_M_next;
	    if (!__victim->_M_singular() && __pred(__victim->base()))
	      {
		__victim->_M_invalidate();
	      }
	  }

	for (_Safe_iterator_base* __iter2 = _M_const_iterators; __iter2;)
	  {
	    const_iterator* __victim = static_cast<const_iterator*>(__iter2);
	    __iter2 = __iter2->_M_next;
	    if (!__victim->_M_singular() && __pred(__victim->base()))
	      {
		__victim->_M_invalidate();
	      }
	  }
      }

  template<typename _Sequence>
    template<typename _Predicate>
      void
      _Safe_sequence<_Sequence>::
      _M_transfer_from_if(_Safe_sequence& __from, _Predicate __pred)
      {
	if (this == std::__addressof(__from))
	  return;

	typedef typename _Sequence::iterator iterator;
	typedef typename _Sequence::const_iterator const_iterator;

	_Safe_iterator_base* __transfered_iterators = 0;
	_Safe_iterator_base* __transfered_const_iterators = 0;
	_Safe_iterator_base* __last_iterator = 0;
	_Safe_iterator_base* __last_const_iterator = 0;
	{
	  // We lock __from first and detach iterator(s) to transfer
	  __gnu_cxx::__scoped_lock sentry(__from._M_get_mutex());

	  for (_Safe_iterator_base* __iter = __from._M_iterators; __iter;)
	    {
	      _Safe_iterator_base* __victim_base = __iter;
	      iterator* __victim = static_cast<iterator*>(__victim_base);
	      __iter = __iter->_M_next;
	      if (!__victim->_M_singular() && __pred(__victim->base()))
		{
		  __victim->_M_detach_single();
		  if (__transfered_iterators)
		    {
		      __victim_base->_M_next = __transfered_iterators;
		      __transfered_iterators->_M_prior = __victim_base;
		    }
		  else
		    __last_iterator = __victim_base;
		  __victim_base->_M_sequence = this;
		  __victim_base->_M_version = this->_M_version;
		  __transfered_iterators = __victim_base;
		}
	    }

	  for (_Safe_iterator_base* __iter2 = __from._M_const_iterators;
		 __iter2;)
	    {
	      _Safe_iterator_base* __victim_base = __iter2;
	      const_iterator* __victim =
		static_cast<const_iterator*>(__victim_base);
	      __iter2 = __iter2->_M_next;
	      if (!__victim->_M_singular() && __pred(__victim->base()))
		{
		  __victim->_M_detach_single();
		  if (__transfered_const_iterators)
		    {
		      __victim_base->_M_next = __transfered_const_iterators;
		      __transfered_const_iterators->_M_prior = __victim_base;
		    }
		  else
		    __last_const_iterator = __victim;
		  __victim_base->_M_sequence = this;
		  __victim_base->_M_version = this->_M_version;
		  __transfered_const_iterators = __victim_base;
		}
	    }
	}

	// Now we can lock *this and add the transfered iterators if any
	if (__last_iterator || __last_const_iterator)
	  {
	    __gnu_cxx::__scoped_lock sentry(this->_M_get_mutex());
	    if (__last_iterator)
	      {
		if (this->_M_iterators)
		  {
		    this->_M_iterators->_M_prior = __last_iterator;
		    __last_iterator->_M_next = this->_M_iterators;
		  }
		this->_M_iterators = __transfered_iterators;
	      }
	    if (__last_const_iterator)
	      {
		if (this->_M_const_iterators)
		  {
		    this->_M_const_iterators->_M_prior = __last_const_iterator;
		    __last_const_iterator->_M_next = this->_M_const_iterators;
		  }
		this->_M_const_iterators = __transfered_const_iterators;
	      }
	  }
      }
} // namespace __gnu_debug

#endif
