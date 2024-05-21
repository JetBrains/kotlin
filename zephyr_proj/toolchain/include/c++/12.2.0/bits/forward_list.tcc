// <forward_list.tcc> -*- C++ -*-

// Copyright (C) 2008-2022 Free Software Foundation, Inc.
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

/** @file bits/forward_list.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{forward_list}
 */

#ifndef _FORWARD_LIST_TCC
#define _FORWARD_LIST_TCC 1

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
_GLIBCXX_BEGIN_NAMESPACE_CONTAINER

  template<typename _Tp, typename _Alloc>
    _Fwd_list_base<_Tp, _Alloc>::
    _Fwd_list_base(_Fwd_list_base&& __lst, _Node_alloc_type&& __a)
    : _M_impl(std::move(__a))
    {
      if (__lst._M_get_Node_allocator() == _M_get_Node_allocator())
	this->_M_impl._M_head = std::move(__lst._M_impl._M_head);
    }

  template<typename _Tp, typename _Alloc>
    template<typename... _Args>
      _Fwd_list_node_base*
      _Fwd_list_base<_Tp, _Alloc>::
      _M_insert_after(const_iterator __pos, _Args&&... __args)
      {
	_Fwd_list_node_base* __to
	  = const_cast<_Fwd_list_node_base*>(__pos._M_node);
	_Node* __thing = _M_create_node(std::forward<_Args>(__args)...);
	__thing->_M_next = __to->_M_next;
	__to->_M_next = __thing;
	return __to->_M_next;
      }

  template<typename _Tp, typename _Alloc>
    _Fwd_list_node_base*
    _Fwd_list_base<_Tp, _Alloc>::
    _M_erase_after(_Fwd_list_node_base* __pos)
    {
      _Node* __curr = static_cast<_Node*>(__pos->_M_next);
      __pos->_M_next = __curr->_M_next;
      _Node_alloc_traits::destroy(_M_get_Node_allocator(),
				  __curr->_M_valptr());
      __curr->~_Node();
      _M_put_node(__curr);
      return __pos->_M_next;
    }

  template<typename _Tp, typename _Alloc>
    _Fwd_list_node_base*
    _Fwd_list_base<_Tp, _Alloc>::
    _M_erase_after(_Fwd_list_node_base* __pos,
		   _Fwd_list_node_base* __last)
    {
      _Node* __curr = static_cast<_Node*>(__pos->_M_next);
      while (__curr != __last)
	{
	  _Node* __temp = __curr;
	  __curr = static_cast<_Node*>(__curr->_M_next);
	  _Node_alloc_traits::destroy(_M_get_Node_allocator(),
				      __temp->_M_valptr());
	  __temp->~_Node();
	  _M_put_node(__temp);
	}
      __pos->_M_next = __last;
      return __last;
    }

  // Called by the range constructor to implement [23.3.4.2]/9
  template<typename _Tp, typename _Alloc>
    template<typename _InputIterator>
      void
      forward_list<_Tp, _Alloc>::
      _M_range_initialize(_InputIterator __first, _InputIterator __last)
      {
	_Node_base* __to = &this->_M_impl._M_head;
	for (; __first != __last; ++__first)
	  {
	    __to->_M_next = this->_M_create_node(*__first);
	    __to = __to->_M_next;
	  }
      }

  // Called by forward_list(n,v,a).
  template<typename _Tp, typename _Alloc>
    void
    forward_list<_Tp, _Alloc>::
    _M_fill_initialize(size_type __n, const value_type& __value)
    {
      _Node_base* __to = &this->_M_impl._M_head;
      for (; __n; --__n)
	{
	  __to->_M_next = this->_M_create_node(__value);
	  __to = __to->_M_next;
	}
    }

  template<typename _Tp, typename _Alloc>
    void
    forward_list<_Tp, _Alloc>::
    _M_default_initialize(size_type __n)
    {
      _Node_base* __to = &this->_M_impl._M_head;
      for (; __n; --__n)
	{
	  __to->_M_next = this->_M_create_node();
	  __to = __to->_M_next;
	}
    }

  template<typename _Tp, typename _Alloc>
    forward_list<_Tp, _Alloc>&
    forward_list<_Tp, _Alloc>::
    operator=(const forward_list& __list)
    {
      if (std::__addressof(__list) != this)
	{
	  if (_Node_alloc_traits::_S_propagate_on_copy_assign())
	    {
	      auto& __this_alloc = this->_M_get_Node_allocator();
	      auto& __that_alloc = __list._M_get_Node_allocator();
	      if (!_Node_alloc_traits::_S_always_equal()
		  && __this_alloc != __that_alloc)
		{
		  // replacement allocator cannot free existing storage
		  clear();
		}
	      std::__alloc_on_copy(__this_alloc, __that_alloc);
	    }
	  assign(__list.cbegin(), __list.cend());
	}
      return *this;
    }

  template<typename _Tp, typename _Alloc>
    void
    forward_list<_Tp, _Alloc>::
    _M_default_insert_after(const_iterator __pos, size_type __n)
    {
      const_iterator __saved_pos = __pos;
      __try
	{
	  for (; __n; --__n)
	    __pos = emplace_after(__pos);
	}
      __catch(...)
	{
	  erase_after(__saved_pos, ++__pos);
	  __throw_exception_again;
	}
    }

  template<typename _Tp, typename _Alloc>
    void
    forward_list<_Tp, _Alloc>::
    resize(size_type __sz)
    {
      iterator __k = before_begin();

      size_type __len = 0;
      while (__k._M_next() != end() && __len < __sz)
	{
	  ++__k;
	  ++__len;
	}
      if (__len == __sz)
	erase_after(__k, end());
      else
	_M_default_insert_after(__k, __sz - __len);
    }

  template<typename _Tp, typename _Alloc>
    void
    forward_list<_Tp, _Alloc>::
    resize(size_type __sz, const value_type& __val)
    {
      iterator __k = before_begin();

      size_type __len = 0;
      while (__k._M_next() != end() && __len < __sz)
	{
	  ++__k;
	  ++__len;
	}
      if (__len == __sz)
	erase_after(__k, end());
      else
	insert_after(__k, __sz - __len, __val);
    }

  template<typename _Tp, typename _Alloc>
    typename forward_list<_Tp, _Alloc>::iterator
    forward_list<_Tp, _Alloc>::
    _M_splice_after(const_iterator __pos,
		    const_iterator __before, const_iterator __last)
    {
      _Node_base* __tmp = const_cast<_Node_base*>(__pos._M_node);
      _Node_base* __b = const_cast<_Node_base*>(__before._M_node);
      _Node_base* __end = __b;

      while (__end && __end->_M_next != __last._M_node)
	__end = __end->_M_next;

      if (__b != __end)
	return iterator(__tmp->_M_transfer_after(__b, __end));
      else
	return iterator(__tmp);
    }

  template<typename _Tp, typename _Alloc>
    void
    forward_list<_Tp, _Alloc>::
    splice_after(const_iterator __pos, forward_list&&,
		 const_iterator __i) noexcept
    {
      const_iterator __j = __i;
      ++__j;

      if (__pos == __i || __pos == __j)
	return;

      _Node_base* __tmp = const_cast<_Node_base*>(__pos._M_node);
      __tmp->_M_transfer_after(const_cast<_Node_base*>(__i._M_node),
			       const_cast<_Node_base*>(__j._M_node));
    }

  template<typename _Tp, typename _Alloc>
    typename forward_list<_Tp, _Alloc>::iterator
    forward_list<_Tp, _Alloc>::
    insert_after(const_iterator __pos, size_type __n, const _Tp& __val)
    {
      if (__n)
	{
	  forward_list __tmp(__n, __val, get_allocator());
	  return _M_splice_after(__pos, __tmp.before_begin(), __tmp.end());
	}
      else
	return iterator(const_cast<_Node_base*>(__pos._M_node));
    }

  template<typename _Tp, typename _Alloc>
    template<typename _InputIterator, typename>
      typename forward_list<_Tp, _Alloc>::iterator
      forward_list<_Tp, _Alloc>::
      insert_after(const_iterator __pos,
		   _InputIterator __first, _InputIterator __last)
      {
	forward_list __tmp(__first, __last, get_allocator());
	if (!__tmp.empty())
	  return _M_splice_after(__pos, __tmp.before_begin(), __tmp.end());
	else
	  return iterator(const_cast<_Node_base*>(__pos._M_node));
      }

#if __cplusplus > 201703L
# define _GLIBCXX20_ONLY(__expr) __expr
#else
# define _GLIBCXX20_ONLY(__expr)
#endif

  template<typename _Tp, typename _Alloc>
    auto
    forward_list<_Tp, _Alloc>::
    remove(const _Tp& __val) -> __remove_return_type
    {
      size_type __removed __attribute__((__unused__)) = 0;
      forward_list __to_destroy(get_allocator());

      auto __prev_it = cbefore_begin();
      while (_Node* __tmp = static_cast<_Node*>(__prev_it._M_node->_M_next))
	if (*__tmp->_M_valptr() == __val)
	  {
	    __to_destroy.splice_after(__to_destroy.cbefore_begin(),
				      *this, __prev_it);
	    _GLIBCXX20_ONLY( __removed++ );
	  }
	else
	  ++__prev_it;

      return _GLIBCXX20_ONLY( __removed );
    }

  template<typename _Tp, typename _Alloc>
    template<typename _Pred>
      auto
      forward_list<_Tp, _Alloc>::
      remove_if(_Pred __pred) -> __remove_return_type
      {
	size_type __removed __attribute__((__unused__)) = 0;
	forward_list __to_destroy(get_allocator());

	auto __prev_it = cbefore_begin();
	while (_Node* __tmp = static_cast<_Node*>(__prev_it._M_node->_M_next))
	  if (__pred(*__tmp->_M_valptr()))
	    {
	      __to_destroy.splice_after(__to_destroy.cbefore_begin(),
					*this, __prev_it);
	      _GLIBCXX20_ONLY( __removed++ );
	    }
	  else
	    ++__prev_it;

	return _GLIBCXX20_ONLY( __removed );
      }

  template<typename _Tp, typename _Alloc>
    template<typename _BinPred>
      auto
      forward_list<_Tp, _Alloc>::
      unique(_BinPred __binary_pred) -> __remove_return_type
      {
	iterator __first = begin();
	iterator __last = end();
	if (__first == __last)
	  return _GLIBCXX20_ONLY(0);

	forward_list __to_destroy(get_allocator());
	size_type __removed __attribute__((__unused__)) = 0;
	iterator __next = __first;
	while (++__next != __last)
	{
	  if (__binary_pred(*__first, *__next))
	    {
	      __to_destroy.splice_after(__to_destroy.cbefore_begin(),
					*this, __first);
	      _GLIBCXX20_ONLY( __removed++ );
	    }
	  else
	    __first = __next;
	  __next = __first;
	}

	return _GLIBCXX20_ONLY( __removed );
      }

#undef _GLIBCXX20_ONLY

  template<typename _Tp, typename _Alloc>
    template<typename _Comp>
      void
      forward_list<_Tp, _Alloc>::
      merge(forward_list&& __list, _Comp __comp)
      {
	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 3088. forward_list::merge behavior unclear when passed *this
	if (std::__addressof(__list) == this)
	  return;

	_Node_base* __node = &this->_M_impl._M_head;
	while (__node->_M_next && __list._M_impl._M_head._M_next)
	  {
	    if (__comp(*static_cast<_Node*>
		       (__list._M_impl._M_head._M_next)->_M_valptr(),
		       *static_cast<_Node*>
		       (__node->_M_next)->_M_valptr()))
	      __node->_M_transfer_after(&__list._M_impl._M_head,
					__list._M_impl._M_head._M_next);
	    __node = __node->_M_next;
	  }

	if (__list._M_impl._M_head._M_next)
	  *__node = std::move(__list._M_impl._M_head);
      }

  template<typename _Tp, typename _Alloc>
    bool
    operator==(const forward_list<_Tp, _Alloc>& __lx,
	       const forward_list<_Tp, _Alloc>& __ly)
    {
      //  We don't have size() so we need to walk through both lists
      //  making sure both iterators are valid.
      auto __ix = __lx.cbegin();
      auto __iy = __ly.cbegin();
      while (__ix != __lx.cend() && __iy != __ly.cend())
	{
	  if (!(*__ix == *__iy))
	    return false;
	  ++__ix;
	  ++__iy;
	}
      if (__ix == __lx.cend() && __iy == __ly.cend())
	return true;
      else
	return false;
    }

  template<typename _Tp, class _Alloc>
    template<typename _Comp>
      void
      forward_list<_Tp, _Alloc>::
      sort(_Comp __comp)
      {
	// If `next' is nullptr, return immediately.
	_Node* __list = static_cast<_Node*>(this->_M_impl._M_head._M_next);
	if (!__list)
	  return;

	unsigned long __insize = 1;

	while (1)
	  {
	    _Node* __p = __list;
	    __list = nullptr;
	    _Node* __tail = nullptr;

	    // Count number of merges we do in this pass.
	    unsigned long __nmerges = 0;

	    while (__p)
	      {
		++__nmerges;
		// There exists a merge to be done.
		// Step `insize' places along from p.
		_Node* __q = __p;
		unsigned long __psize = 0;
		for (unsigned long __i = 0; __i < __insize; ++__i)
		  {
		    ++__psize;
		    __q = static_cast<_Node*>(__q->_M_next);
		    if (!__q)
		      break;
		  }

		// If q hasn't fallen off end, we have two lists to merge.
		unsigned long __qsize = __insize;

		// Now we have two lists; merge them.
		while (__psize > 0 || (__qsize > 0 && __q))
		  {
		    // Decide whether next node of merge comes from p or q.
		    _Node* __e;
		    if (__psize == 0)
		      {
			// p is empty; e must come from q.
			__e = __q;
			__q = static_cast<_Node*>(__q->_M_next);
			--__qsize;
		      }
		    else if (__qsize == 0 || !__q)
		      {
			// q is empty; e must come from p.
			__e = __p;
			__p = static_cast<_Node*>(__p->_M_next);
			--__psize;
		      }
		    else if (!__comp(*__q->_M_valptr(), *__p->_M_valptr()))
		      {
			// First node of q is not lower; e must come from p.
			__e = __p;
			__p = static_cast<_Node*>(__p->_M_next);
			--__psize;
		      }
		    else
		      {
			// First node of q is lower; e must come from q.
			__e = __q;
			__q = static_cast<_Node*>(__q->_M_next);
			--__qsize;
		      }

		    // Add the next node to the merged list.
		    if (__tail)
		      __tail->_M_next = __e;
		    else
		      __list = __e;
		    __tail = __e;
		  }

		// Now p has stepped `insize' places along, and q has too.
		__p = __q;
	      }
	    __tail->_M_next = nullptr;

	    // If we have done only one merge, we're finished.
	    // Allow for nmerges == 0, the empty list case.
	    if (__nmerges <= 1)
	      {
		this->_M_impl._M_head._M_next = __list;
		return;
	      }

	    // Otherwise repeat, merging lists twice the size.
	    __insize *= 2;
	  }
      }

_GLIBCXX_END_NAMESPACE_CONTAINER
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif /* _FORWARD_LIST_TCC */
