// Safe container implementation  -*- C++ -*-

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

/** @file debug/safe_unordered_container.tcc
 *  This file is a GNU debug extension to the Standard C++ Library.
 */

#ifndef _GLIBCXX_DEBUG_SAFE_UNORDERED_CONTAINER_TCC
#define _GLIBCXX_DEBUG_SAFE_UNORDERED_CONTAINER_TCC 1

namespace __gnu_debug
{
  template<typename _Container>
    template<typename _Predicate>
      void
      _Safe_unordered_container<_Container>::
      _M_invalidate_if(_Predicate __pred)
      {
	typedef typename _Container::iterator iterator;
	typedef typename _Container::const_iterator const_iterator;

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


  template<typename _Container>
    template<typename _Predicate>
      void
      _Safe_unordered_container<_Container>::
      _M_invalidate_local_if(_Predicate __pred)
      {
	typedef typename _Container::local_iterator local_iterator;
	typedef typename _Container::const_local_iterator const_local_iterator;

	__gnu_cxx::__scoped_lock sentry(this->_M_get_mutex());
	for (_Safe_iterator_base* __iter = _M_local_iterators; __iter;)
	  {
	    local_iterator* __victim = static_cast<local_iterator*>(__iter);
	    __iter = __iter->_M_next;
	    if (!__victim->_M_singular() && __pred(__victim->base()))
	      {
		__victim->_M_invalidate();
	      }
	  }

	for (_Safe_iterator_base* __iter2 = _M_const_local_iterators; __iter2;)
	  {
	    const_local_iterator* __victim =
	      static_cast<const_local_iterator*>(__iter2);
	    __iter2 = __iter2->_M_next;
	    if (!__victim->_M_singular() && __pred(__victim->base()))
	      {
		__victim->_M_invalidate();
	      }
	  }
      }

} // namespace __gnu_debug

#endif
