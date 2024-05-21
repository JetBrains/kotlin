// List implementation (out of line) -*- C++ -*-

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
 * Copyright (c) 1996,1997
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

/** @file bits/list.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{list}
 */

#ifndef _LIST_TCC
#define _LIST_TCC 1

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
_GLIBCXX_BEGIN_NAMESPACE_CONTAINER

  template<typename _Tp, typename _Alloc>
    void
    _List_base<_Tp, _Alloc>::
    _M_clear() _GLIBCXX_NOEXCEPT
    {
      typedef _List_node<_Tp>  _Node;
      __detail::_List_node_base* __cur = _M_impl._M_node._M_next;
      while (__cur != &_M_impl._M_node)
	{
	  _Node* __tmp = static_cast<_Node*>(__cur);
	  __cur = __tmp->_M_next;
	  _Tp* __val = __tmp->_M_valptr();
#if __cplusplus >= 201103L
	  _Node_alloc_traits::destroy(_M_get_Node_allocator(), __val);
#else
	  _Tp_alloc_type(_M_get_Node_allocator()).destroy(__val);
#endif
	  _M_put_node(__tmp);
	}
    }

#if __cplusplus >= 201103L
  template<typename _Tp, typename _Alloc>
    template<typename... _Args>
      typename list<_Tp, _Alloc>::iterator
      list<_Tp, _Alloc>::
      emplace(const_iterator __position, _Args&&... __args)
      {
	_Node* __tmp = _M_create_node(std::forward<_Args>(__args)...);
	__tmp->_M_hook(__position._M_const_cast()._M_node);
	this->_M_inc_size(1);
	return iterator(__tmp);
      }
#endif

  template<typename _Tp, typename _Alloc>
    typename list<_Tp, _Alloc>::iterator
    list<_Tp, _Alloc>::
#if __cplusplus >= 201103L
    insert(const_iterator __position, const value_type& __x)
#else
    insert(iterator __position, const value_type& __x)
#endif
    {
      _Node* __tmp = _M_create_node(__x);
      __tmp->_M_hook(__position._M_const_cast()._M_node);
      this->_M_inc_size(1);
      return iterator(__tmp);
    }

#if __cplusplus >= 201103L
  template<typename _Tp, typename _Alloc>
    typename list<_Tp, _Alloc>::iterator
    list<_Tp, _Alloc>::
    insert(const_iterator __position, size_type __n, const value_type& __x)
    {
      if (__n)
	{
	  list __tmp(__n, __x, get_allocator());
	  iterator __it = __tmp.begin();
	  splice(__position, __tmp);
	  return __it;
	}
      return __position._M_const_cast();
    }

  template<typename _Tp, typename _Alloc>
    template<typename _InputIterator, typename>
      typename list<_Tp, _Alloc>::iterator
      list<_Tp, _Alloc>::
      insert(const_iterator __position, _InputIterator __first,
	     _InputIterator __last)
      {
	list __tmp(__first, __last, get_allocator());
	if (!__tmp.empty())
	  {
	    iterator __it = __tmp.begin();
	    splice(__position, __tmp);
	    return __it;
	  }
	return __position._M_const_cast();
      }
#endif

  template<typename _Tp, typename _Alloc>
    typename list<_Tp, _Alloc>::iterator
    list<_Tp, _Alloc>::
#if __cplusplus >= 201103L
    erase(const_iterator __position) noexcept
#else
    erase(iterator __position)
#endif
    {
      iterator __ret = iterator(__position._M_node->_M_next);
      _M_erase(__position._M_const_cast());
      return __ret;
    }

  // Return a const_iterator indicating the position to start inserting or
  // erasing elements (depending whether the list is growing or shrinking),
  // and set __new_size to the number of new elements that must be appended.
  // Equivalent to the following, but performed optimally:
  // if (__new_size < size()) {
  //   __new_size = 0;
  //   return std::next(begin(), __new_size);
  // } else {
  //   __newsize -= size();
  //   return end();
  // }
  template<typename _Tp, typename _Alloc>
    typename list<_Tp, _Alloc>::const_iterator
    list<_Tp, _Alloc>::
    _M_resize_pos(size_type& __new_size) const
    {
      const_iterator __i;
#if _GLIBCXX_USE_CXX11_ABI
      const size_type __len = size();
      if (__new_size < __len)
	{
	  if (__new_size <= __len / 2)
	    {
	      __i = begin();
	      std::advance(__i, __new_size);
	    }
	  else
	    {
	      __i = end();
	      ptrdiff_t __num_erase = __len - __new_size;
	      std::advance(__i, -__num_erase);
	    }
	  __new_size = 0;
	  return __i;
	}
      else
	__i = end();
#else
      size_type __len = 0;
      for (__i = begin(); __i != end() && __len < __new_size; ++__i, ++__len)
        ;
#endif
      __new_size -= __len;
      return __i;
    }

#if __cplusplus >= 201103L
  template<typename _Tp, typename _Alloc>
    void
    list<_Tp, _Alloc>::
    _M_default_append(size_type __n)
    {
      size_type __i = 0;
      __try
	{
	  for (; __i < __n; ++__i)
	    emplace_back();
	}
      __catch(...)
	{
	  for (; __i; --__i)
	    pop_back();
	  __throw_exception_again;
	}
    }

  template<typename _Tp, typename _Alloc>
    void
    list<_Tp, _Alloc>::
    resize(size_type __new_size)
    {
      const_iterator __i = _M_resize_pos(__new_size);
      if (__new_size)
	_M_default_append(__new_size);
      else
        erase(__i, end());
    }

  template<typename _Tp, typename _Alloc>
    void
    list<_Tp, _Alloc>::
    resize(size_type __new_size, const value_type& __x)
    {
      const_iterator __i = _M_resize_pos(__new_size);
      if (__new_size)
        insert(end(), __new_size, __x);
      else
        erase(__i, end());
    }
#else
  template<typename _Tp, typename _Alloc>
    void
    list<_Tp, _Alloc>::
    resize(size_type __new_size, value_type __x)
    {
      const_iterator __i = _M_resize_pos(__new_size);
      if (__new_size)
        insert(end(), __new_size, __x);
      else
        erase(__i._M_const_cast(), end());
    }
#endif

  template<typename _Tp, typename _Alloc>
    list<_Tp, _Alloc>&
    list<_Tp, _Alloc>::
    operator=(const list& __x)
    {
      if (this != std::__addressof(__x))
	{
#if __cplusplus >= 201103L
	  if (_Node_alloc_traits::_S_propagate_on_copy_assign())
	    {
              auto& __this_alloc = this->_M_get_Node_allocator();
              auto& __that_alloc = __x._M_get_Node_allocator();
              if (!_Node_alloc_traits::_S_always_equal()
	          && __this_alloc != __that_alloc)
	        {
		  // replacement allocator cannot free existing storage
		  clear();
		}
	      std::__alloc_on_copy(__this_alloc, __that_alloc);
            }
#endif
	  _M_assign_dispatch(__x.begin(), __x.end(), __false_type());
	}
      return *this;
    }

  template<typename _Tp, typename _Alloc>
    void
    list<_Tp, _Alloc>::
    _M_fill_assign(size_type __n, const value_type& __val)
    {
      iterator __i = begin();
      for (; __i != end() && __n > 0; ++__i, --__n)
        *__i = __val;
      if (__n > 0)
        insert(end(), __n, __val);
      else
        erase(__i, end());
    }

  template<typename _Tp, typename _Alloc>
    template <typename _InputIterator>
      void
      list<_Tp, _Alloc>::
      _M_assign_dispatch(_InputIterator __first2, _InputIterator __last2,
			 __false_type)
      {
        iterator __first1 = begin();
        iterator __last1 = end();
        for (; __first1 != __last1 && __first2 != __last2;
	     ++__first1, (void)++__first2)
          *__first1 = *__first2;
        if (__first2 == __last2)
          erase(__first1, __last1);
        else
          insert(__last1, __first2, __last2);
      }

#if __cplusplus > 201703L
# define _GLIBCXX20_ONLY(__expr) __expr
#else
# define _GLIBCXX20_ONLY(__expr)
#endif

  template<typename _Tp, typename _Alloc>
    typename list<_Tp, _Alloc>::__remove_return_type
    list<_Tp, _Alloc>::
    remove(const value_type& __value)
    {
#if !_GLIBCXX_USE_CXX11_ABI
      size_type __removed __attribute__((__unused__)) = 0;
#endif
      list __to_destroy(get_allocator());
      iterator __first = begin();
      iterator __last = end();
      while (__first != __last)
	{
	  iterator __next = __first;
	  ++__next;
	  if (*__first == __value)
	    {
	      // _GLIBCXX_RESOLVE_LIB_DEFECTS
	      // 526. Is it undefined if a function in the standard changes
	      // in parameters?
	      __to_destroy.splice(__to_destroy.begin(), *this, __first);
#if !_GLIBCXX_USE_CXX11_ABI
	      _GLIBCXX20_ONLY( __removed++ );
#endif
	    }

	  __first = __next;
	}

#if !_GLIBCXX_USE_CXX11_ABI
	return _GLIBCXX20_ONLY( __removed );
#else
	return _GLIBCXX20_ONLY( __to_destroy.size() );
#endif
    }

  template<typename _Tp, typename _Alloc>
    typename list<_Tp, _Alloc>::__remove_return_type
    list<_Tp, _Alloc>::
    unique()
    {
      iterator __first = begin();
      iterator __last = end();
      if (__first == __last)
	return _GLIBCXX20_ONLY( 0 );
#if !_GLIBCXX_USE_CXX11_ABI
      size_type __removed __attribute__((__unused__)) = 0;
#endif
      list __to_destroy(get_allocator());
      iterator __next = __first;
      while (++__next != __last)
	{
	  if (*__first == *__next)
	    {
	      __to_destroy.splice(__to_destroy.begin(), *this, __next);
#if !_GLIBCXX_USE_CXX11_ABI
	      _GLIBCXX20_ONLY( __removed++ );
#endif
	    }
	  else
	    __first = __next;
	  __next = __first;
	}

#if !_GLIBCXX_USE_CXX11_ABI
      return _GLIBCXX20_ONLY( __removed );
#else
      return _GLIBCXX20_ONLY( __to_destroy.size() );
#endif
    }

  template<typename _Tp, typename _Alloc>
    void
    list<_Tp, _Alloc>::
#if __cplusplus >= 201103L
    merge(list&& __x)
#else
    merge(list& __x)
#endif
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 300. list::merge() specification incomplete
      if (this != std::__addressof(__x))
	{
	  _M_check_equal_allocators(__x);

	  iterator __first1 = begin();
	  iterator __last1 = end();
	  iterator __first2 = __x.begin();
	  iterator __last2 = __x.end();

	  const _Finalize_merge __fin(*this, __x, __first2);

	  while (__first1 != __last1 && __first2 != __last2)
	    if (*__first2 < *__first1)
	      {
		iterator __next = __first2;
		_M_transfer(__first1, __first2, ++__next);
		__first2 = __next;
	      }
	    else
	      ++__first1;
	  if (__first2 != __last2)
	    {
	      _M_transfer(__last1, __first2, __last2);
	      __first2 = __last2;
	    }
	}
    }

  template<typename _Tp, typename _Alloc>
    template <typename _StrictWeakOrdering>
      void
      list<_Tp, _Alloc>::
#if __cplusplus >= 201103L
      merge(list&& __x, _StrictWeakOrdering __comp)
#else
      merge(list& __x, _StrictWeakOrdering __comp)
#endif
      {
	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 300. list::merge() specification incomplete
	if (this != std::__addressof(__x))
	  {
	    _M_check_equal_allocators(__x);

	    iterator __first1 = begin();
	    iterator __last1 = end();
	    iterator __first2 = __x.begin();
	    iterator __last2 = __x.end();

	    const _Finalize_merge __fin(*this, __x, __first2);

	    while (__first1 != __last1 && __first2 != __last2)
	      if (__comp(*__first2, *__first1))
		{
		  iterator __next = __first2;
		  _M_transfer(__first1, __first2, ++__next);
		  __first2 = __next;
		}
	      else
		++__first1;
	    if (__first2 != __last2)
	      {
		_M_transfer(__last1, __first2, __last2);
		__first2 = __last2;
	      }
	  }
      }

  template<typename _Tp, typename _Alloc>
    void
    list<_Tp, _Alloc>::
    sort()
    {
      // Do nothing if the list has length 0 or 1.
      if (this->_M_impl._M_node._M_next != &this->_M_impl._M_node
	  && this->_M_impl._M_node._M_next->_M_next != &this->_M_impl._M_node)
      {
	using __detail::_Scratch_list;
	// The algorithm used here is largely unchanged from the SGI STL
	// and is described in The C++ Standard Template Library by Plauger,
	// Stepanov, Lee, Musser.
	// Each element of *this is spliced out and merged into one of the
	// sorted lists in __tmp, then all the lists in __tmp are merged
	// together and then swapped back into *this.
	// Because all nodes end up back in *this we do not need to update
	// this->size() while nodes are temporarily moved out.
	_Scratch_list __carry;
	_Scratch_list __tmp[64];
	_Scratch_list* __fill = __tmp;
	_Scratch_list* __counter;

	_Scratch_list::_Ptr_cmp<iterator, void> __ptr_comp;

	__try
	  {
	    do
	      {
		__carry._M_take_one(begin()._M_node);

		for(__counter = __tmp;
		    __counter != __fill && !__counter->empty();
		    ++__counter)
		  {

		    __counter->merge(__carry, __ptr_comp);
		    __carry.swap(*__counter);
		  }
		__carry.swap(*__counter);
		if (__counter == __fill)
		  ++__fill;
	      }
	    while ( !empty() );

	    for (__counter = __tmp + 1; __counter != __fill; ++__counter)
	      __counter->merge(__counter[-1], __ptr_comp);
	    __fill[-1].swap(this->_M_impl._M_node);
	  }
	__catch(...)
	  {
	    // Move all nodes back into *this.
	    __carry._M_put_all(end()._M_node);
	    for (int __i = 0; __i < sizeof(__tmp)/sizeof(__tmp[0]); ++__i)
	      __tmp[__i]._M_put_all(end()._M_node);
	    __throw_exception_again;
	  }
      }
    }

  template<typename _Tp, typename _Alloc>
    template <typename _Predicate>
      typename list<_Tp, _Alloc>::__remove_return_type
      list<_Tp, _Alloc>::
      remove_if(_Predicate __pred)
      {
#if !_GLIBCXX_USE_CXX11_ABI
	size_type __removed __attribute__((__unused__)) = 0;
#endif
	list __to_destroy(get_allocator());
	iterator __first = begin();
	iterator __last = end();
	while (__first != __last)
	  {
	    iterator __next = __first;
	    ++__next;
	    if (__pred(*__first))
	      {
		__to_destroy.splice(__to_destroy.begin(), *this, __first);
#if !_GLIBCXX_USE_CXX11_ABI
		_GLIBCXX20_ONLY( __removed++ );
#endif
	      }
	    __first = __next;
	  }

#if !_GLIBCXX_USE_CXX11_ABI
	return _GLIBCXX20_ONLY( __removed );
#else
	return _GLIBCXX20_ONLY( __to_destroy.size() );
#endif
      }

  template<typename _Tp, typename _Alloc>
    template <typename _BinaryPredicate>
      typename list<_Tp, _Alloc>::__remove_return_type
      list<_Tp, _Alloc>::
      unique(_BinaryPredicate __binary_pred)
      {
        iterator __first = begin();
        iterator __last = end();
        if (__first == __last)
	  return _GLIBCXX20_ONLY(0);
#if !_GLIBCXX_USE_CXX11_ABI
        size_type __removed __attribute__((__unused__)) = 0;
#endif
	list __to_destroy(get_allocator());
        iterator __next = __first;
        while (++__next != __last)
	  {
	    if (__binary_pred(*__first, *__next))
	      {
		__to_destroy.splice(__to_destroy.begin(), *this, __next);
#if !_GLIBCXX_USE_CXX11_ABI
		_GLIBCXX20_ONLY( __removed++ );
#endif
	      }
	    else
	      __first = __next;
	    __next = __first;
	  }

#if !_GLIBCXX_USE_CXX11_ABI
	return _GLIBCXX20_ONLY( __removed );
#else
	return _GLIBCXX20_ONLY( __to_destroy.size() );
#endif
      }

#undef _GLIBCXX20_ONLY

  template<typename _Tp, typename _Alloc>
    template <typename _StrictWeakOrdering>
      void
      list<_Tp, _Alloc>::
      sort(_StrictWeakOrdering __comp)
      {
	// Do nothing if the list has length 0 or 1.
	if (this->_M_impl._M_node._M_next != &this->_M_impl._M_node
	    && this->_M_impl._M_node._M_next->_M_next != &this->_M_impl._M_node)
	{
	  using __detail::_Scratch_list;
	  _Scratch_list __carry;
	  _Scratch_list __tmp[64];
	  _Scratch_list* __fill = __tmp;
	  _Scratch_list* __counter;

	_Scratch_list::_Ptr_cmp<iterator, _StrictWeakOrdering> __ptr_comp
	  = { __comp };

	  __try
	    {
	      do
		{
		  __carry._M_take_one(begin()._M_node);

		  for(__counter = __tmp;
		      __counter != __fill && !__counter->empty();
		      ++__counter)
		    {

		      __counter->merge(__carry, __ptr_comp);
		      __carry.swap(*__counter);
		    }
		  __carry.swap(*__counter);
		  if (__counter == __fill)
		    ++__fill;
		}
	      while ( !empty() );

	      for (__counter = __tmp + 1; __counter != __fill; ++__counter)
		__counter->merge(__counter[-1], __ptr_comp);
	      __fill[-1].swap(this->_M_impl._M_node);
	    }
	  __catch(...)
	    {
	      // Move all nodes back into *this.
	      __carry._M_put_all(end()._M_node);
	      for (int __i = 0; __i < sizeof(__tmp)/sizeof(__tmp[0]); ++__i)
		__tmp[__i]._M_put_all(end()._M_node);
	      __throw_exception_again;
	    }
	}
      }

_GLIBCXX_END_NAMESPACE_CONTAINER
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif /* _LIST_TCC */

