// Deque implementation (out of line) -*- C++ -*-

// Copyright (C) 2001-2022 Free Software Foundation, Inc.
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

/*
 *
 * Copyright (c) 1994
 * Hewlett-Packard Company
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.  Hewlett-Packard Company makes no
 * representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 *
 *
 * Copyright (c) 1997
 * Silicon Graphics Computer Systems, Inc.
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.  Silicon Graphics makes no
 * representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 */

/** @file bits/deque.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{deque}
 */

#ifndef _DEQUE_TCC
#define _DEQUE_TCC 1

#include <bits/stl_algobase.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
_GLIBCXX_BEGIN_NAMESPACE_CONTAINER

#if __cplusplus >= 201103L
  template <typename _Tp, typename _Alloc>
    void
    deque<_Tp, _Alloc>::
    _M_default_initialize()
    {
      _Map_pointer __cur;
      __try
	{
	  for (__cur = this->_M_impl._M_start._M_node;
	       __cur < this->_M_impl._M_finish._M_node;
	       ++__cur)
	    std::__uninitialized_default_a(*__cur, *__cur + _S_buffer_size(),
					   _M_get_Tp_allocator());
	  std::__uninitialized_default_a(this->_M_impl._M_finish._M_first,
					 this->_M_impl._M_finish._M_cur,
					 _M_get_Tp_allocator());
	}
      __catch(...)
	{
	  std::_Destroy(this->_M_impl._M_start, iterator(*__cur, __cur),
			_M_get_Tp_allocator());
	  __throw_exception_again;
	}
    }
#endif

  template <typename _Tp, typename _Alloc>
    deque<_Tp, _Alloc>&
    deque<_Tp, _Alloc>::
    operator=(const deque& __x)
    {
      if (std::__addressof(__x) != this)
	{
#if __cplusplus >= 201103L
	  if (_Alloc_traits::_S_propagate_on_copy_assign())
	    {
	      if (!_Alloc_traits::_S_always_equal()
		  && _M_get_Tp_allocator() != __x._M_get_Tp_allocator())
		{
		  // Replacement allocator cannot free existing storage,
		  // so deallocate everything and take copy of __x's data.
		  _M_replace_map(__x, __x.get_allocator());
		  std::__alloc_on_copy(_M_get_Tp_allocator(),
				       __x._M_get_Tp_allocator());
		  return *this;
		}
	      std::__alloc_on_copy(_M_get_Tp_allocator(),
				   __x._M_get_Tp_allocator());
	    }
#endif
	  const size_type __len = size();
	  if (__len >= __x.size())
	    _M_erase_at_end(std::copy(__x.begin(), __x.end(),
				      this->_M_impl._M_start));
	  else
	    {
	      const_iterator __mid = __x.begin() + difference_type(__len);
	      std::copy(__x.begin(), __mid, this->_M_impl._M_start);
	      _M_range_insert_aux(this->_M_impl._M_finish, __mid, __x.end(),
				  std::random_access_iterator_tag());
	    }
	}
      return *this;
    }

#if __cplusplus >= 201103L
  template<typename _Tp, typename _Alloc>
    template<typename... _Args>
#if __cplusplus > 201402L
      typename deque<_Tp, _Alloc>::reference
#else
      void
#endif
      deque<_Tp, _Alloc>::
      emplace_front(_Args&&... __args)
      {
	if (this->_M_impl._M_start._M_cur != this->_M_impl._M_start._M_first)
	  {
	    _Alloc_traits::construct(this->_M_impl,
				     this->_M_impl._M_start._M_cur - 1,
				     std::forward<_Args>(__args)...);
	    --this->_M_impl._M_start._M_cur;
	  }
	else
	  _M_push_front_aux(std::forward<_Args>(__args)...);
#if __cplusplus > 201402L
	return front();
#endif
      }

  template<typename _Tp, typename _Alloc>
    template<typename... _Args>
#if __cplusplus > 201402L
      typename deque<_Tp, _Alloc>::reference
#else
      void
#endif
      deque<_Tp, _Alloc>::
      emplace_back(_Args&&... __args)
      {
	if (this->_M_impl._M_finish._M_cur
	    != this->_M_impl._M_finish._M_last - 1)
	  {
	    _Alloc_traits::construct(this->_M_impl,
				     this->_M_impl._M_finish._M_cur,
				     std::forward<_Args>(__args)...);
	    ++this->_M_impl._M_finish._M_cur;
	  }
	else
	  _M_push_back_aux(std::forward<_Args>(__args)...);
#if __cplusplus > 201402L
	return back();
#endif
      }
#endif

#if __cplusplus >= 201103L
  template<typename _Tp, typename _Alloc>
    template<typename... _Args>
      typename deque<_Tp, _Alloc>::iterator
      deque<_Tp, _Alloc>::
      emplace(const_iterator __position, _Args&&... __args)
      {
	if (__position._M_cur == this->_M_impl._M_start._M_cur)
	  {
	    emplace_front(std::forward<_Args>(__args)...);
	    return this->_M_impl._M_start;
	  }
	else if (__position._M_cur == this->_M_impl._M_finish._M_cur)
	  {
	    emplace_back(std::forward<_Args>(__args)...);
	    iterator __tmp = this->_M_impl._M_finish;
	    --__tmp;
	    return __tmp;
	  }
	else
	  return _M_insert_aux(__position._M_const_cast(),
			       std::forward<_Args>(__args)...);
      }
#endif

  template <typename _Tp, typename _Alloc>
    typename deque<_Tp, _Alloc>::iterator
    deque<_Tp, _Alloc>::
#if __cplusplus >= 201103L
    insert(const_iterator __position, const value_type& __x)
#else
    insert(iterator __position, const value_type& __x)
#endif
    {
      if (__position._M_cur == this->_M_impl._M_start._M_cur)
	{
	  push_front(__x);
	  return this->_M_impl._M_start;
	}
      else if (__position._M_cur == this->_M_impl._M_finish._M_cur)
	{
	  push_back(__x);
	  iterator __tmp = this->_M_impl._M_finish;
	  --__tmp;
	  return __tmp;
	}
      else
	return _M_insert_aux(__position._M_const_cast(), __x);
   }

  template <typename _Tp, typename _Alloc>
    typename deque<_Tp, _Alloc>::iterator
    deque<_Tp, _Alloc>::
    _M_erase(iterator __position)
    {
      iterator __next = __position;
      ++__next;
      const difference_type __index = __position - begin();
      if (static_cast<size_type>(__index) < (size() >> 1))
	{
	  if (__position != begin())
	    _GLIBCXX_MOVE_BACKWARD3(begin(), __position, __next);
	  pop_front();
	}
      else
	{
	  if (__next != end())
	    _GLIBCXX_MOVE3(__next, end(), __position);
	  pop_back();
	}
      return begin() + __index;
    }

  template <typename _Tp, typename _Alloc>
    typename deque<_Tp, _Alloc>::iterator
    deque<_Tp, _Alloc>::
    _M_erase(iterator __first, iterator __last)
    {
      if (__first == __last)
	return __first;
      else if (__first == begin() && __last == end())
	{
	  clear();
	  return end();
	}
      else
	{
	  const difference_type __n = __last - __first;
	  const difference_type __elems_before = __first - begin();
	  if (static_cast<size_type>(__elems_before) <= (size() - __n) / 2)
	    {
	      if (__first != begin())
		_GLIBCXX_MOVE_BACKWARD3(begin(), __first, __last);
	      _M_erase_at_begin(begin() + __n);
	    }
	  else
	    {
	      if (__last != end())
		_GLIBCXX_MOVE3(__last, end(), __first);
	      _M_erase_at_end(end() - __n);
	    }
	  return begin() + __elems_before;
	}
    }

  template <typename _Tp, class _Alloc>
    template <typename _InputIterator>
      void
      deque<_Tp, _Alloc>::
      _M_assign_aux(_InputIterator __first, _InputIterator __last,
		    std::input_iterator_tag)
      {
	iterator __cur = begin();
	for (; __first != __last && __cur != end(); ++__cur, (void)++__first)
	  *__cur = *__first;
	if (__first == __last)
	  _M_erase_at_end(__cur);
	else
	  _M_range_insert_aux(end(), __first, __last,
			      std::__iterator_category(__first));
      }

  template <typename _Tp, typename _Alloc>
    void
    deque<_Tp, _Alloc>::
    _M_fill_insert(iterator __pos, size_type __n, const value_type& __x)
    {
      if (__pos._M_cur == this->_M_impl._M_start._M_cur)
	{
	  iterator __new_start = _M_reserve_elements_at_front(__n);
	  __try
	    {
	      std::__uninitialized_fill_a(__new_start, this->_M_impl._M_start,
					  __x, _M_get_Tp_allocator());
	      this->_M_impl._M_start = __new_start;
	    }
	  __catch(...)
	    {
	      _M_destroy_nodes(__new_start._M_node,
			       this->_M_impl._M_start._M_node);
	      __throw_exception_again;
	    }
	}
      else if (__pos._M_cur == this->_M_impl._M_finish._M_cur)
	{
	  iterator __new_finish = _M_reserve_elements_at_back(__n);
	  __try
	    {
	      std::__uninitialized_fill_a(this->_M_impl._M_finish,
					  __new_finish, __x,
					  _M_get_Tp_allocator());
	      this->_M_impl._M_finish = __new_finish;
	    }
	  __catch(...)
	    {
	      _M_destroy_nodes(this->_M_impl._M_finish._M_node + 1,
			       __new_finish._M_node + 1);
	      __throw_exception_again;
	    }
	}
      else
	_M_insert_aux(__pos, __n, __x);
    }

#if __cplusplus >= 201103L
  template <typename _Tp, typename _Alloc>
    void
    deque<_Tp, _Alloc>::
    _M_default_append(size_type __n)
    {
      if (__n)
	{
	  iterator __new_finish = _M_reserve_elements_at_back(__n);
	  __try
	    {
	      std::__uninitialized_default_a(this->_M_impl._M_finish,
					     __new_finish,
					     _M_get_Tp_allocator());
	      this->_M_impl._M_finish = __new_finish;
	    }
	  __catch(...)
	    {
	      _M_destroy_nodes(this->_M_impl._M_finish._M_node + 1,
			       __new_finish._M_node + 1);
	      __throw_exception_again;
	    }
	}
    }

  template <typename _Tp, typename _Alloc>
    bool
    deque<_Tp, _Alloc>::
    _M_shrink_to_fit()
    {
      const difference_type __front_capacity
	= (this->_M_impl._M_start._M_cur - this->_M_impl._M_start._M_first);
      if (__front_capacity == 0)
	return false;

      const difference_type __back_capacity
	= (this->_M_impl._M_finish._M_last - this->_M_impl._M_finish._M_cur);
      if (__front_capacity + __back_capacity < _S_buffer_size())
	return false;

      return std::__shrink_to_fit_aux<deque>::_S_do_it(*this);
    }
#endif

  template <typename _Tp, typename _Alloc>
    void
    deque<_Tp, _Alloc>::
    _M_fill_initialize(const value_type& __value)
    {
      _Map_pointer __cur;
      __try
	{
	  for (__cur = this->_M_impl._M_start._M_node;
	       __cur < this->_M_impl._M_finish._M_node;
	       ++__cur)
	    std::__uninitialized_fill_a(*__cur, *__cur + _S_buffer_size(),
					__value, _M_get_Tp_allocator());
	  std::__uninitialized_fill_a(this->_M_impl._M_finish._M_first,
				      this->_M_impl._M_finish._M_cur,
				      __value, _M_get_Tp_allocator());
	}
      __catch(...)
	{
	  std::_Destroy(this->_M_impl._M_start, iterator(*__cur, __cur),
			_M_get_Tp_allocator());
	  __throw_exception_again;
	}
    }

  template <typename _Tp, typename _Alloc>
    template <typename _InputIterator>
      void
      deque<_Tp, _Alloc>::
      _M_range_initialize(_InputIterator __first, _InputIterator __last,
			  std::input_iterator_tag)
      {
	this->_M_initialize_map(0);
	__try
	  {
	    for (; __first != __last; ++__first)
#if __cplusplus >= 201103L
	      emplace_back(*__first);
#else
	      push_back(*__first);
#endif
	  }
	__catch(...)
	  {
	    clear();
	    __throw_exception_again;
	  }
      }

  template <typename _Tp, typename _Alloc>
    template <typename _ForwardIterator>
      void
      deque<_Tp, _Alloc>::
      _M_range_initialize(_ForwardIterator __first, _ForwardIterator __last,
			  std::forward_iterator_tag)
      {
	const size_type __n = std::distance(__first, __last);
	this->_M_initialize_map(_S_check_init_len(__n, _M_get_Tp_allocator()));

	_Map_pointer __cur_node;
	__try
	  {
	    for (__cur_node = this->_M_impl._M_start._M_node;
		 __cur_node < this->_M_impl._M_finish._M_node;
		 ++__cur_node)
	      {
		if (__n < _S_buffer_size())
		  __builtin_unreachable(); // See PR 100516

		_ForwardIterator __mid = __first;
		std::advance(__mid, _S_buffer_size());
		std::__uninitialized_copy_a(__first, __mid, *__cur_node,
					    _M_get_Tp_allocator());
		__first = __mid;
	      }
	    std::__uninitialized_copy_a(__first, __last,
					this->_M_impl._M_finish._M_first,
					_M_get_Tp_allocator());
	  }
	__catch(...)
	  {
	    std::_Destroy(this->_M_impl._M_start,
			  iterator(*__cur_node, __cur_node),
			  _M_get_Tp_allocator());
	    __throw_exception_again;
	  }
      }

  // Called only if _M_impl._M_finish._M_cur == _M_impl._M_finish._M_last - 1.
  template<typename _Tp, typename _Alloc>
#if __cplusplus >= 201103L
    template<typename... _Args>
      void
      deque<_Tp, _Alloc>::
      _M_push_back_aux(_Args&&... __args)
#else
      void
      deque<_Tp, _Alloc>::
      _M_push_back_aux(const value_type& __t)
#endif
      {
	if (size() == max_size())
	  __throw_length_error(
	      __N("cannot create std::deque larger than max_size()"));

	_M_reserve_map_at_back();
	*(this->_M_impl._M_finish._M_node + 1) = this->_M_allocate_node();
	__try
	  {
#if __cplusplus >= 201103L
	    _Alloc_traits::construct(this->_M_impl,
				     this->_M_impl._M_finish._M_cur,
				     std::forward<_Args>(__args)...);
#else
	    this->_M_impl.construct(this->_M_impl._M_finish._M_cur, __t);
#endif
	    this->_M_impl._M_finish._M_set_node(this->_M_impl._M_finish._M_node
						+ 1);
	    this->_M_impl._M_finish._M_cur = this->_M_impl._M_finish._M_first;
	  }
	__catch(...)
	  {
	    _M_deallocate_node(*(this->_M_impl._M_finish._M_node + 1));
	    __throw_exception_again;
	  }
      }

  // Called only if _M_impl._M_start._M_cur == _M_impl._M_start._M_first.
  template<typename _Tp, typename _Alloc>
#if __cplusplus >= 201103L
    template<typename... _Args>
      void
      deque<_Tp, _Alloc>::
      _M_push_front_aux(_Args&&... __args)
#else
      void
      deque<_Tp, _Alloc>::
      _M_push_front_aux(const value_type& __t)
#endif
      {
	if (size() == max_size())
	  __throw_length_error(
	      __N("cannot create std::deque larger than max_size()"));

	_M_reserve_map_at_front();
	*(this->_M_impl._M_start._M_node - 1) = this->_M_allocate_node();
	__try
	  {
	    this->_M_impl._M_start._M_set_node(this->_M_impl._M_start._M_node
					       - 1);
	    this->_M_impl._M_start._M_cur = this->_M_impl._M_start._M_last - 1;
#if __cplusplus >= 201103L
	    _Alloc_traits::construct(this->_M_impl,
				     this->_M_impl._M_start._M_cur,
				     std::forward<_Args>(__args)...);
#else
	    this->_M_impl.construct(this->_M_impl._M_start._M_cur, __t);
#endif
	  }
	__catch(...)
	  {
	    ++this->_M_impl._M_start;
	    _M_deallocate_node(*(this->_M_impl._M_start._M_node - 1));
	    __throw_exception_again;
	  }
      }

  // Called only if _M_impl._M_finish._M_cur == _M_impl._M_finish._M_first.
  template <typename _Tp, typename _Alloc>
    void deque<_Tp, _Alloc>::
    _M_pop_back_aux()
    {
      _M_deallocate_node(this->_M_impl._M_finish._M_first);
      this->_M_impl._M_finish._M_set_node(this->_M_impl._M_finish._M_node - 1);
      this->_M_impl._M_finish._M_cur = this->_M_impl._M_finish._M_last - 1;
      _Alloc_traits::destroy(_M_get_Tp_allocator(),
			     this->_M_impl._M_finish._M_cur);
    }

  // Called only if _M_impl._M_start._M_cur == _M_impl._M_start._M_last - 1.
  // Note that if the deque has at least one element (a precondition for this
  // member function), and if
  //   _M_impl._M_start._M_cur == _M_impl._M_start._M_last,
  // then the deque must have at least two nodes.
  template <typename _Tp, typename _Alloc>
    void deque<_Tp, _Alloc>::
    _M_pop_front_aux()
    {
      _Alloc_traits::destroy(_M_get_Tp_allocator(),
			     this->_M_impl._M_start._M_cur);
      _M_deallocate_node(this->_M_impl._M_start._M_first);
      this->_M_impl._M_start._M_set_node(this->_M_impl._M_start._M_node + 1);
      this->_M_impl._M_start._M_cur = this->_M_impl._M_start._M_first;
    }

  template <typename _Tp, typename _Alloc>
    template <typename _InputIterator>
      void
      deque<_Tp, _Alloc>::
      _M_range_insert_aux(iterator __pos,
			  _InputIterator __first, _InputIterator __last,
			  std::input_iterator_tag)
      { std::copy(__first, __last, std::inserter(*this, __pos)); }

  template <typename _Tp, typename _Alloc>
    template <typename _ForwardIterator>
      void
      deque<_Tp, _Alloc>::
      _M_range_insert_aux(iterator __pos,
			  _ForwardIterator __first, _ForwardIterator __last,
			  std::forward_iterator_tag)
      {
	const size_type __n = std::distance(__first, __last);
	if (__pos._M_cur == this->_M_impl._M_start._M_cur)
	  {
	    iterator __new_start = _M_reserve_elements_at_front(__n);
	    __try
	      {
		std::__uninitialized_copy_a(__first, __last, __new_start,
					    _M_get_Tp_allocator());
		this->_M_impl._M_start = __new_start;
	      }
	    __catch(...)
	      {
		_M_destroy_nodes(__new_start._M_node,
				 this->_M_impl._M_start._M_node);
		__throw_exception_again;
	      }
	  }
	else if (__pos._M_cur == this->_M_impl._M_finish._M_cur)
	  {
	    iterator __new_finish = _M_reserve_elements_at_back(__n);
	    __try
	      {
		std::__uninitialized_copy_a(__first, __last,
					    this->_M_impl._M_finish,
					    _M_get_Tp_allocator());
		this->_M_impl._M_finish = __new_finish;
	      }
	    __catch(...)
	      {
		_M_destroy_nodes(this->_M_impl._M_finish._M_node + 1,
				 __new_finish._M_node + 1);
		__throw_exception_again;
	      }
	  }
	else
	  _M_insert_aux(__pos, __first, __last, __n);
      }

  template<typename _Tp, typename _Alloc>
#if __cplusplus >= 201103L
    template<typename... _Args>
      typename deque<_Tp, _Alloc>::iterator
      deque<_Tp, _Alloc>::
      _M_insert_aux(iterator __pos, _Args&&... __args)
      {
	value_type __x_copy(std::forward<_Args>(__args)...); // XXX copy
#else
    typename deque<_Tp, _Alloc>::iterator
      deque<_Tp, _Alloc>::
      _M_insert_aux(iterator __pos, const value_type& __x)
      {
	value_type __x_copy = __x; // XXX copy
#endif
	difference_type __index = __pos - this->_M_impl._M_start;
	if (static_cast<size_type>(__index) < size() / 2)
	  {
	    push_front(_GLIBCXX_MOVE(front()));
	    iterator __front1 = this->_M_impl._M_start;
	    ++__front1;
	    iterator __front2 = __front1;
	    ++__front2;
	    __pos = this->_M_impl._M_start + __index;
	    iterator __pos1 = __pos;
	    ++__pos1;
	    _GLIBCXX_MOVE3(__front2, __pos1, __front1);
	  }
	else
	  {
	    push_back(_GLIBCXX_MOVE(back()));
	    iterator __back1 = this->_M_impl._M_finish;
	    --__back1;
	    iterator __back2 = __back1;
	    --__back2;
	    __pos = this->_M_impl._M_start + __index;
	    _GLIBCXX_MOVE_BACKWARD3(__pos, __back2, __back1);
	  }
	*__pos = _GLIBCXX_MOVE(__x_copy);
	return __pos;
      }

  template <typename _Tp, typename _Alloc>
    void
    deque<_Tp, _Alloc>::
    _M_insert_aux(iterator __pos, size_type __n, const value_type& __x)
    {
      const difference_type __elems_before = __pos - this->_M_impl._M_start;
      const size_type __length = this->size();
      value_type __x_copy = __x;
      if (__elems_before < difference_type(__length / 2))
	{
	  iterator __new_start = _M_reserve_elements_at_front(__n);
	  iterator __old_start = this->_M_impl._M_start;
	  __pos = this->_M_impl._M_start + __elems_before;
	  __try
	    {
	      if (__elems_before >= difference_type(__n))
		{
		  iterator __start_n = (this->_M_impl._M_start
					+ difference_type(__n));
		  std::__uninitialized_move_a(this->_M_impl._M_start,
					      __start_n, __new_start,
					      _M_get_Tp_allocator());
		  this->_M_impl._M_start = __new_start;
		  _GLIBCXX_MOVE3(__start_n, __pos, __old_start);
		  std::fill(__pos - difference_type(__n), __pos, __x_copy);
		}
	      else
		{
		  std::__uninitialized_move_fill(this->_M_impl._M_start,
						 __pos, __new_start,
						 this->_M_impl._M_start,
						 __x_copy,
						 _M_get_Tp_allocator());
		  this->_M_impl._M_start = __new_start;
		  std::fill(__old_start, __pos, __x_copy);
		}
	    }
	  __catch(...)
	    {
	      _M_destroy_nodes(__new_start._M_node,
			       this->_M_impl._M_start._M_node);
	      __throw_exception_again;
	    }
	}
      else
	{
	  iterator __new_finish = _M_reserve_elements_at_back(__n);
	  iterator __old_finish = this->_M_impl._M_finish;
	  const difference_type __elems_after =
	    difference_type(__length) - __elems_before;
	  __pos = this->_M_impl._M_finish - __elems_after;
	  __try
	    {
	      if (__elems_after > difference_type(__n))
		{
		  iterator __finish_n = (this->_M_impl._M_finish
					 - difference_type(__n));
		  std::__uninitialized_move_a(__finish_n,
					      this->_M_impl._M_finish,
					      this->_M_impl._M_finish,
					      _M_get_Tp_allocator());
		  this->_M_impl._M_finish = __new_finish;
		  _GLIBCXX_MOVE_BACKWARD3(__pos, __finish_n, __old_finish);
		  std::fill(__pos, __pos + difference_type(__n), __x_copy);
		}
	      else
		{
		  std::__uninitialized_fill_move(this->_M_impl._M_finish,
						 __pos + difference_type(__n),
						 __x_copy, __pos,
						 this->_M_impl._M_finish,
						 _M_get_Tp_allocator());
		  this->_M_impl._M_finish = __new_finish;
		  std::fill(__pos, __old_finish, __x_copy);
		}
	    }
	  __catch(...)
	    {
	      _M_destroy_nodes(this->_M_impl._M_finish._M_node + 1,
			       __new_finish._M_node + 1);
	      __throw_exception_again;
	    }
	}
    }

  template <typename _Tp, typename _Alloc>
    template <typename _ForwardIterator>
      void
      deque<_Tp, _Alloc>::
      _M_insert_aux(iterator __pos,
		    _ForwardIterator __first, _ForwardIterator __last,
		    size_type __n)
      {
	const difference_type __elemsbefore = __pos - this->_M_impl._M_start;
	const size_type __length = size();
	if (static_cast<size_type>(__elemsbefore) < __length / 2)
	  {
	    iterator __new_start = _M_reserve_elements_at_front(__n);
	    iterator __old_start = this->_M_impl._M_start;
	    __pos = this->_M_impl._M_start + __elemsbefore;
	    __try
	      {
		if (__elemsbefore >= difference_type(__n))
		  {
		    iterator __start_n = (this->_M_impl._M_start
					  + difference_type(__n));
		    std::__uninitialized_move_a(this->_M_impl._M_start,
						__start_n, __new_start,
						_M_get_Tp_allocator());
		    this->_M_impl._M_start = __new_start;
		    _GLIBCXX_MOVE3(__start_n, __pos, __old_start);
		    std::copy(__first, __last, __pos - difference_type(__n));
		  }
		else
		  {
		    _ForwardIterator __mid = __first;
		    std::advance(__mid, difference_type(__n) - __elemsbefore);
		    std::__uninitialized_move_copy(this->_M_impl._M_start,
						   __pos, __first, __mid,
						   __new_start,
						   _M_get_Tp_allocator());
		    this->_M_impl._M_start = __new_start;
		    std::copy(__mid, __last, __old_start);
		  }
	      }
	    __catch(...)
	      {
		_M_destroy_nodes(__new_start._M_node,
				 this->_M_impl._M_start._M_node);
		__throw_exception_again;
	      }
	  }
	else
	{
	  iterator __new_finish = _M_reserve_elements_at_back(__n);
	  iterator __old_finish = this->_M_impl._M_finish;
	  const difference_type __elemsafter =
	    difference_type(__length) - __elemsbefore;
	  __pos = this->_M_impl._M_finish - __elemsafter;
	  __try
	    {
	      if (__elemsafter > difference_type(__n))
		{
		  iterator __finish_n = (this->_M_impl._M_finish
					 - difference_type(__n));
		  std::__uninitialized_move_a(__finish_n,
					      this->_M_impl._M_finish,
					      this->_M_impl._M_finish,
					      _M_get_Tp_allocator());
		  this->_M_impl._M_finish = __new_finish;
		  _GLIBCXX_MOVE_BACKWARD3(__pos, __finish_n, __old_finish);
		  std::copy(__first, __last, __pos);
		}
	      else
		{
		  _ForwardIterator __mid = __first;
		  std::advance(__mid, __elemsafter);
		  std::__uninitialized_copy_move(__mid, __last, __pos,
						 this->_M_impl._M_finish,
						 this->_M_impl._M_finish,
						 _M_get_Tp_allocator());
		  this->_M_impl._M_finish = __new_finish;
		  std::copy(__first, __mid, __pos);
		}
	    }
	  __catch(...)
	    {
	      _M_destroy_nodes(this->_M_impl._M_finish._M_node + 1,
			       __new_finish._M_node + 1);
	      __throw_exception_again;
	    }
	}
      }

   template<typename _Tp, typename _Alloc>
     void
     deque<_Tp, _Alloc>::
     _M_destroy_data_aux(iterator __first, iterator __last)
     {
       for (_Map_pointer __node = __first._M_node + 1;
	    __node < __last._M_node; ++__node)
	 std::_Destroy(*__node, *__node + _S_buffer_size(),
		       _M_get_Tp_allocator());

       if (__first._M_node != __last._M_node)
	 {
	   std::_Destroy(__first._M_cur, __first._M_last,
			 _M_get_Tp_allocator());
	   std::_Destroy(__last._M_first, __last._M_cur,
			 _M_get_Tp_allocator());
	 }
       else
	 std::_Destroy(__first._M_cur, __last._M_cur,
		       _M_get_Tp_allocator());
     }

  template <typename _Tp, typename _Alloc>
    void
    deque<_Tp, _Alloc>::
    _M_new_elements_at_front(size_type __new_elems)
    {
      if (this->max_size() - this->size() < __new_elems)
	__throw_length_error(__N("deque::_M_new_elements_at_front"));

      const size_type __new_nodes = ((__new_elems + _S_buffer_size() - 1)
				     / _S_buffer_size());
      _M_reserve_map_at_front(__new_nodes);
      size_type __i;
      __try
	{
	  for (__i = 1; __i <= __new_nodes; ++__i)
	    *(this->_M_impl._M_start._M_node - __i) = this->_M_allocate_node();
	}
      __catch(...)
	{
	  for (size_type __j = 1; __j < __i; ++__j)
	    _M_deallocate_node(*(this->_M_impl._M_start._M_node - __j));
	  __throw_exception_again;
	}
    }

  template <typename _Tp, typename _Alloc>
    void
    deque<_Tp, _Alloc>::
    _M_new_elements_at_back(size_type __new_elems)
    {
      if (this->max_size() - this->size() < __new_elems)
	__throw_length_error(__N("deque::_M_new_elements_at_back"));

      const size_type __new_nodes = ((__new_elems + _S_buffer_size() - 1)
				     / _S_buffer_size());
      _M_reserve_map_at_back(__new_nodes);
      size_type __i;
      __try
	{
	  for (__i = 1; __i <= __new_nodes; ++__i)
	    *(this->_M_impl._M_finish._M_node + __i) = this->_M_allocate_node();
	}
      __catch(...)
	{
	  for (size_type __j = 1; __j < __i; ++__j)
	    _M_deallocate_node(*(this->_M_impl._M_finish._M_node + __j));
	  __throw_exception_again;
	}
    }

  template <typename _Tp, typename _Alloc>
    void
    deque<_Tp, _Alloc>::
    _M_reallocate_map(size_type __nodes_to_add, bool __add_at_front)
    {
      const size_type __old_num_nodes
	= this->_M_impl._M_finish._M_node - this->_M_impl._M_start._M_node + 1;
      const size_type __new_num_nodes = __old_num_nodes + __nodes_to_add;

      _Map_pointer __new_nstart;
      if (this->_M_impl._M_map_size > 2 * __new_num_nodes)
	{
	  __new_nstart = this->_M_impl._M_map + (this->_M_impl._M_map_size
					 - __new_num_nodes) / 2
			 + (__add_at_front ? __nodes_to_add : 0);
	  if (__new_nstart < this->_M_impl._M_start._M_node)
	    std::copy(this->_M_impl._M_start._M_node,
		      this->_M_impl._M_finish._M_node + 1,
		      __new_nstart);
	  else
	    std::copy_backward(this->_M_impl._M_start._M_node,
			       this->_M_impl._M_finish._M_node + 1,
			       __new_nstart + __old_num_nodes);
	}
      else
	{
	  size_type __new_map_size = this->_M_impl._M_map_size
				     + std::max(this->_M_impl._M_map_size,
						__nodes_to_add) + 2;

	  _Map_pointer __new_map = this->_M_allocate_map(__new_map_size);
	  __new_nstart = __new_map + (__new_map_size - __new_num_nodes) / 2
			 + (__add_at_front ? __nodes_to_add : 0);
	  std::copy(this->_M_impl._M_start._M_node,
		    this->_M_impl._M_finish._M_node + 1,
		    __new_nstart);
	  _M_deallocate_map(this->_M_impl._M_map, this->_M_impl._M_map_size);

	  this->_M_impl._M_map = __new_map;
	  this->_M_impl._M_map_size = __new_map_size;
	}

      this->_M_impl._M_start._M_set_node(__new_nstart);
      this->_M_impl._M_finish._M_set_node(__new_nstart + __old_num_nodes - 1);
    }

_GLIBCXX_END_NAMESPACE_CONTAINER

  // Overload for deque::iterators, exploiting the "segmented-iterator
  // optimization".
  template<typename _Tp, typename _VTp>
    void
    __fill_a1(const _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*>& __first,
	      const _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*>& __last,
	      const _VTp& __value)
    {
      typedef _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*> _Iter;
      if (__first._M_node != __last._M_node)
	{
	  std::__fill_a1(__first._M_cur, __first._M_last, __value);

	  for (typename _Iter::_Map_pointer __node = __first._M_node + 1;
	       __node < __last._M_node; ++__node)
	    std::__fill_a1(*__node, *__node + _Iter::_S_buffer_size(), __value);

	  std::__fill_a1(__last._M_first, __last._M_cur, __value);
	}
      else
	std::__fill_a1(__first._M_cur, __last._M_cur, __value);
    }

  template<bool _IsMove,
	   typename _Tp, typename _Ref, typename _Ptr, typename _OI>
    _OI
    __copy_move_dit(_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __first,
		    _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __last,
		    _OI __result)
    {
      typedef _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> _Iter;
      if (__first._M_node != __last._M_node)
	{
	  __result
	    = std::__copy_move_a1<_IsMove>(__first._M_cur, __first._M_last,
					   __result);

	  for (typename _Iter::_Map_pointer __node = __first._M_node + 1;
	       __node != __last._M_node; ++__node)
	    __result
	      = std::__copy_move_a1<_IsMove>(*__node,
					     *__node + _Iter::_S_buffer_size(),
					     __result);

	  return std::__copy_move_a1<_IsMove>(__last._M_first, __last._M_cur,
					      __result);
	}

      return std::__copy_move_a1<_IsMove>(__first._M_cur, __last._M_cur,
					  __result);
    }

  template<bool _IsMove,
	   typename _Tp, typename _Ref, typename _Ptr, typename _OI>
    _OI
    __copy_move_a1(_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __first,
		   _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __last,
		   _OI __result)
    { return __copy_move_dit<_IsMove>(__first, __last, __result); }

  template<bool _IsMove,
	   typename _ITp, typename _IRef, typename _IPtr, typename _OTp>
    _GLIBCXX_STD_C::_Deque_iterator<_OTp, _OTp&, _OTp*>
    __copy_move_a1(_GLIBCXX_STD_C::_Deque_iterator<_ITp, _IRef, _IPtr> __first,
		   _GLIBCXX_STD_C::_Deque_iterator<_ITp, _IRef, _IPtr> __last,
		   _GLIBCXX_STD_C::_Deque_iterator<_OTp, _OTp&, _OTp*> __result)
    { return __copy_move_dit<_IsMove>(__first, __last, __result); }

  template<bool _IsMove, typename _II, typename _Tp>
    typename __gnu_cxx::__enable_if<
      __is_random_access_iter<_II>::__value,
      _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*> >::__type
    __copy_move_a1(_II __first, _II __last,
		   _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*> __result)
    {
      typedef _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*> _Iter;
      typedef typename _Iter::difference_type difference_type;

      difference_type __len = __last - __first;
      while (__len > 0)
	{
	  const difference_type __clen
	    = std::min(__len, __result._M_last - __result._M_cur);
	  std::__copy_move_a1<_IsMove>(__first, __first + __clen,
				       __result._M_cur);

	  __first += __clen;
	  __result += __clen;
	  __len -= __clen;
	}

      return __result;
    }

  template<bool _IsMove, typename _CharT>
    typename __gnu_cxx::__enable_if<
      __is_char<_CharT>::__value,
      _GLIBCXX_STD_C::_Deque_iterator<_CharT, _CharT&, _CharT*> >::__type
    __copy_move_a2(
	istreambuf_iterator<_CharT, char_traits<_CharT> > __first,
	istreambuf_iterator<_CharT, char_traits<_CharT> > __last,
	_GLIBCXX_STD_C::_Deque_iterator<_CharT, _CharT&, _CharT*> __result)
    {
      if (__first == __last)
	return __result;

      for (;;)
	{
	  const std::ptrdiff_t __len = __result._M_last - __result._M_cur;
	  const std::ptrdiff_t __nb
	    = std::__copy_n_a(__first, __len, __result._M_cur, false)
	    - __result._M_cur;
	  __result += __nb;

	  if (__nb != __len)
	    break;
	}

      return __result;
    }

  template<typename _CharT, typename _Size>
    typename __gnu_cxx::__enable_if<
      __is_char<_CharT>::__value,
      _GLIBCXX_STD_C::_Deque_iterator<_CharT, _CharT&, _CharT*> >::__type
    __copy_n_a(
      istreambuf_iterator<_CharT, char_traits<_CharT> > __it, _Size __size,
      _GLIBCXX_STD_C::_Deque_iterator<_CharT, _CharT&, _CharT*> __result,
      bool __strict)
    {
      if (__size == 0)
	return __result;

      do
	{
	  const _Size __len
	    = std::min<_Size>(__result._M_last - __result._M_cur, __size);
	  std::__copy_n_a(__it, __len, __result._M_cur, __strict);
	  __result += __len;
	  __size -= __len;
	}
      while (__size != 0);
      return __result;
    }

  template<bool _IsMove,
	   typename _Tp, typename _Ref, typename _Ptr, typename _OI>
    _OI
    __copy_move_backward_dit(
		_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __first,
		_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __last,
		_OI __result)
    {
      typedef _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> _Iter;
      if (__first._M_node != __last._M_node)
	{
	  __result = std::__copy_move_backward_a1<_IsMove>(
		__last._M_first, __last._M_cur, __result);

	  for (typename _Iter::_Map_pointer __node = __last._M_node - 1;
	       __node != __first._M_node; --__node)
	    __result = std::__copy_move_backward_a1<_IsMove>(
		*__node, *__node + _Iter::_S_buffer_size(), __result);

	  return std::__copy_move_backward_a1<_IsMove>(
			__first._M_cur, __first._M_last, __result);
	}

      return std::__copy_move_backward_a1<_IsMove>(
		__first._M_cur, __last._M_cur, __result);
    }

  template<bool _IsMove,
	   typename _Tp, typename _Ref, typename _Ptr, typename _OI>
    _OI
    __copy_move_backward_a1(
		_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __first,
		_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __last,
		_OI __result)
    { return __copy_move_backward_dit<_IsMove>(__first, __last, __result); }

  template<bool _IsMove,
	   typename _ITp, typename _IRef, typename _IPtr, typename _OTp>
    _GLIBCXX_STD_C::_Deque_iterator<_OTp, _OTp&, _OTp*>
    __copy_move_backward_a1(
		_GLIBCXX_STD_C::_Deque_iterator<_ITp, _IRef, _IPtr> __first,
		_GLIBCXX_STD_C::_Deque_iterator<_ITp, _IRef, _IPtr> __last,
		_GLIBCXX_STD_C::_Deque_iterator<_OTp, _OTp&, _OTp*> __result)
    { return __copy_move_backward_dit<_IsMove>(__first, __last, __result); }

  template<bool _IsMove, typename _II, typename _Tp>
    typename __gnu_cxx::__enable_if<
      __is_random_access_iter<_II>::__value,
      _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*> >::__type
    __copy_move_backward_a1(_II __first, _II __last,
		_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*> __result)
    {
      typedef _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Tp&, _Tp*> _Iter;
      typedef typename _Iter::difference_type difference_type;

      difference_type __len = __last - __first;
      while (__len > 0)
	{
	  difference_type __rlen = __result._M_cur - __result._M_first;
	  _Tp* __rend = __result._M_cur;
	  if (!__rlen)
	    {
	      __rlen = _Iter::_S_buffer_size();
	      __rend = *(__result._M_node - 1) + __rlen;
	    }

	  const difference_type __clen = std::min(__len, __rlen);
	  std::__copy_move_backward_a1<_IsMove>(__last - __clen, __last, __rend);

	  __last -= __clen;
	  __result -= __clen;
	  __len -= __clen;
	}

      return __result;
    }

  template<typename _Tp, typename _Ref, typename _Ptr, typename _II>
    bool
    __equal_dit(
	const _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr>& __first1,
	const _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr>& __last1,
	_II __first2)
    {
      typedef _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> _Iter;
      if (__first1._M_node != __last1._M_node)
	{
	  if (!std::__equal_aux1(__first1._M_cur, __first1._M_last, __first2))
	    return false;

	  __first2 += __first1._M_last - __first1._M_cur;
	  for (typename _Iter::_Map_pointer __node = __first1._M_node + 1;
	       __node != __last1._M_node;
	       __first2 += _Iter::_S_buffer_size(), ++__node)
	    if (!std::__equal_aux1(*__node, *__node + _Iter::_S_buffer_size(),
				  __first2))
	      return false;

	  return std::__equal_aux1(__last1._M_first, __last1._M_cur, __first2);
	}

      return std::__equal_aux1(__first1._M_cur, __last1._M_cur, __first2);
    }

  template<typename _Tp, typename _Ref, typename _Ptr, typename _II>
    typename __gnu_cxx::__enable_if<
      __is_random_access_iter<_II>::__value, bool>::__type
    __equal_aux1(_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __first1,
		 _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __last1,
		 _II __first2)
    { return std::__equal_dit(__first1, __last1, __first2); }

  template<typename _Tp1, typename _Ref1, typename _Ptr1,
	   typename _Tp2, typename _Ref2, typename _Ptr2>
    bool
    __equal_aux1(_GLIBCXX_STD_C::_Deque_iterator<_Tp1, _Ref1, _Ptr1> __first1,
		 _GLIBCXX_STD_C::_Deque_iterator<_Tp1, _Ref1, _Ptr1> __last1,
		 _GLIBCXX_STD_C::_Deque_iterator<_Tp2, _Ref2, _Ptr2> __first2)
    { return std::__equal_dit(__first1, __last1, __first2); }

  template<typename _II, typename _Tp, typename _Ref, typename _Ptr>
    typename __gnu_cxx::__enable_if<
      __is_random_access_iter<_II>::__value, bool>::__type
    __equal_aux1(_II __first1, _II __last1,
		_GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> __first2)
    {
      typedef _GLIBCXX_STD_C::_Deque_iterator<_Tp, _Ref, _Ptr> _Iter;
      typedef typename _Iter::difference_type difference_type;

      difference_type __len = __last1 - __first1;
      while (__len > 0)
	{
	  const difference_type __clen
	    = std::min(__len, __first2._M_last - __first2._M_cur);
	  if (!std::__equal_aux1(__first1, __first1 + __clen, __first2._M_cur))
	    return false;

	  __first1 += __clen;
	  __len -= __clen;
	  __first2 += __clen;
	}

      return true;
    }

  template<typename _Tp1, typename _Ref, typename _Ptr, typename _Tp2>
    int
    __lex_cmp_dit(
	_GLIBCXX_STD_C::_Deque_iterator<_Tp1, _Ref, _Ptr> __first1,
	_GLIBCXX_STD_C::_Deque_iterator<_Tp1, _Ref, _Ptr> __last1,
	const _Tp2* __first2, const _Tp2* __last2)
    {
      const bool __simple =
	(__is_memcmp_ordered_with<_Tp1, _Tp2>::__value
	 && __is_pointer<_Ptr>::__value
#if __cplusplus > 201703L && __cpp_lib_concepts
	 // For C++20 iterator_traits<volatile T*>::value_type is non-volatile
	 // so __is_byte<T> could be true, but we can't use memcmp with
	 // volatile data.
	 && !is_volatile_v<_Tp1>
	 && !is_volatile_v<_Tp2>
#endif
	 );
      typedef std::__lexicographical_compare<__simple> _Lc;

      while (__first1._M_node != __last1._M_node)
	{
	  const ptrdiff_t __len1 = __first1._M_last - __first1._M_cur;
	  const ptrdiff_t __len2 = __last2 - __first2;
	  const ptrdiff_t __len = std::min(__len1, __len2);
	  // if __len1 > __len2 this will return a positive value:
	  if (int __ret = _Lc::__3way(__first1._M_cur, __first1._M_last,
				      __first2, __first2 + __len))
	    return __ret;

	  __first1 += __len;
	  __first2 += __len;
	}
      return _Lc::__3way(__first1._M_cur, __last1._M_cur,
			 __first2, __last2);
    }

  template<typename _Tp1, typename _Ref1, typename _Ptr1,
	   typename _Tp2>
    inline bool
    __lexicographical_compare_aux1(
	_GLIBCXX_STD_C::_Deque_iterator<_Tp1, _Ref1, _Ptr1> __first1,
	_GLIBCXX_STD_C::_Deque_iterator<_Tp1, _Ref1, _Ptr1> __last1,
	_Tp2* __first2, _Tp2* __last2)
    { return std::__lex_cmp_dit(__first1, __last1, __first2, __last2) < 0; }

  template<typename _Tp1,
	   typename _Tp2, typename _Ref2, typename _Ptr2>
    inline  bool
    __lexicographical_compare_aux1(_Tp1* __first1, _Tp1* __last1,
	_GLIBCXX_STD_C::_Deque_iterator<_Tp2, _Ref2, _Ptr2> __first2,
	_GLIBCXX_STD_C::_Deque_iterator<_Tp2, _Ref2, _Ptr2> __last2)
    { return std::__lex_cmp_dit(__first2, __last2, __first1, __last1) > 0; }

  template<typename _Tp1, typename _Ref1, typename _Ptr1,
	   typename _Tp2, typename _Ref2, typename _Ptr2>
    inline bool
    __lexicographical_compare_aux1(
		_GLIBCXX_STD_C::_Deque_iterator<_Tp1, _Ref1, _Ptr1> __first1,
		_GLIBCXX_STD_C::_Deque_iterator<_Tp1, _Ref1, _Ptr1> __last1,
		_GLIBCXX_STD_C::_Deque_iterator<_Tp2, _Ref2, _Ptr2> __first2,
		_GLIBCXX_STD_C::_Deque_iterator<_Tp2, _Ref2, _Ptr2> __last2)
    {
      const bool __simple =
	(__is_memcmp_ordered_with<_Tp1, _Tp2>::__value
	 && __is_pointer<_Ptr1>::__value
	 && __is_pointer<_Ptr2>::__value
#if __cplusplus > 201703L && __cpp_lib_concepts
	 // For C++20 iterator_traits<volatile T*>::value_type is non-volatile
	 // so __is_byte<T> could be true, but we can't use memcmp with
	 // volatile data.
	 && !is_volatile_v<_Tp1>
	 && !is_volatile_v<_Tp2>
#endif
	 );
      typedef std::__lexicographical_compare<__simple> _Lc;

      while (__first1 != __last1)
	{
	  const ptrdiff_t __len2 = __first2._M_node == __last2._M_node
	    ? __last2._M_cur - __first2._M_cur
	    : __first2._M_last - __first2._M_cur;
	  if (__len2 == 0)
	    return false;
	  const ptrdiff_t __len1 = __first1._M_node == __last1._M_node
	    ? __last1._M_cur - __first1._M_cur
	    : __first1._M_last - __first1._M_cur;
	  const ptrdiff_t __len = std::min(__len1, __len2);
	  if (int __ret = _Lc::__3way(__first1._M_cur, __first1._M_cur + __len,
				      __first2._M_cur, __first2._M_cur + __len))
	    return __ret < 0;

	  __first1 += __len;
	  __first2 += __len;
	}

      return __last2 != __first2;
    }

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif
