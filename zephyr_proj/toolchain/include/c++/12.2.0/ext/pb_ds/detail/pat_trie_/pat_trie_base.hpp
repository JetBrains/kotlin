// -*- C++ -*-

// Copyright (C) 2005-2022 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the terms
// of the GNU General Public License as published by the Free Software
// Foundation; either version 3, or (at your option) any later
// version.

// This library is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.

// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

// Copyright (C) 2004 Ami Tavory and Vladimir Dreizin, IBM-HRL.

// Permission to use, copy, modify, sell, and distribute this software
// is hereby granted without fee, provided that the above copyright
// notice appears in all copies, and that both that copyright notice
// and this permission notice appear in supporting documentation. None
// of the above authors, nor IBM Haifa Research Laboratories, make any
// representation about the suitability of this software for any
// purpose. It is provided "as is" without express or implied
// warranty.

/**
 * @file pat_trie_/pat_trie_base.hpp
 * Contains the base class for a patricia tree.
 */

#ifndef PB_DS_PAT_TRIE_BASE
#define PB_DS_PAT_TRIE_BASE

#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Base type for PATRICIA trees.
    struct pat_trie_base
    {
      /**
       *  @brief  Three types of nodes.
       *
       *  i_node is used by _Inode, leaf_node by _Leaf, and head_node by _Head.
       */
      enum node_type
	{
	  i_node,
	  leaf_node,
	  head_node
	};

      /// Metadata base primary template.
      template<typename Metadata, typename _Alloc>
	struct _Metadata
	{
	  typedef Metadata     					metadata_type;
	  typedef _Alloc     					allocator_type;
	  typedef typename detail::rebind_traits<_Alloc, Metadata>::const_reference
	    const_reference;

	  const_reference
	  get_metadata() const
	  { return m_metadata; }

	  metadata_type 					m_metadata;
	};

      /// Specialization for null metadata.
      template<typename _Alloc>
	struct _Metadata<null_type, _Alloc>
	{
	  typedef null_type 					metadata_type;
	  typedef _Alloc     					allocator_type;
	};


      /// Node base.
      template<typename _ATraits, typename Metadata>
      struct _Node_base
      : public Metadata
      {
      private:
	typedef typename Metadata::allocator_type		_Alloc;

      public:
	typedef _Alloc						allocator_type;
	typedef _ATraits					access_traits;
	typedef typename _ATraits::type_traits			type_traits;
	typedef typename detail::rebind_traits<_Alloc, _Node_base>::pointer
	  node_pointer;

	node_pointer 						m_p_parent;
	const node_type 	       				m_type;

	_Node_base(node_type type) : m_type(type)
	{ }

	typedef typename detail::rebind_traits<_Alloc, _ATraits>::const_pointer
	  a_const_pointer;
	typedef typename _ATraits::const_iterator	      a_const_iterator;

#ifdef _GLIBCXX_DEBUG
	typedef std::pair<a_const_iterator, a_const_iterator> node_debug_info;

	void
	assert_valid(a_const_pointer p_traits,
		     const char* __file, int __line) const
	{ assert_valid_imp(p_traits, __file, __line); }

	virtual node_debug_info
	assert_valid_imp(a_const_pointer, const char*, int) const = 0;
#endif
      };


    /// Head node for PATRICIA tree.
    template<typename _ATraits, typename Metadata>
    struct _Head
    : public _Node_base<_ATraits, Metadata>
    {
      typedef _Node_base<_ATraits, Metadata> 			base_type;
      typedef typename base_type::type_traits			type_traits;
      typedef typename base_type::node_pointer			node_pointer;

      node_pointer						m_p_min;
      node_pointer						m_p_max;

      _Head() : base_type(head_node) { }

#ifdef _GLIBCXX_DEBUG
      typedef typename base_type::node_debug_info      	       node_debug_info;
      typedef typename base_type::a_const_pointer 	       a_const_pointer;

      virtual node_debug_info
      assert_valid_imp(a_const_pointer, const char* __file, int __line) const
      {
	_GLIBCXX_DEBUG_VERIFY_AT(false,
				 _M_message("Assertion from %1;:%2;")
				 ._M_string(__FILE__)._M_integer(__LINE__),
				 __file, __line);
	return node_debug_info();
      }
#endif
    };


    /// Leaf node for PATRICIA tree.
    template<typename _ATraits, typename Metadata>
    struct _Leaf
    : public _Node_base<_ATraits, Metadata>
    {
      typedef _Node_base<_ATraits, Metadata> 	   	    	base_type;
      typedef typename base_type::type_traits			type_traits;
      typedef typename type_traits::value_type			value_type;
      typedef typename type_traits::reference			reference;
      typedef typename type_traits::const_reference    		const_reference;

    private:
      value_type						m_value;

      _Leaf(const _Leaf&);

    public:
      _Leaf(const_reference other)
      : base_type(leaf_node), m_value(other) { }

      reference
      value()
      { return m_value; }

      const_reference
      value() const
      { return m_value; }

#ifdef _GLIBCXX_DEBUG
      typedef typename base_type::node_debug_info      		node_debug_info;
      typedef typename base_type::a_const_pointer      		a_const_pointer;

      virtual node_debug_info
      assert_valid_imp(a_const_pointer p_traits,
		       const char* __file, int __line) const
      {
	PB_DS_DEBUG_VERIFY(base_type::m_type == leaf_node);
	node_debug_info ret;
	const_reference r_val = value();
	return std::make_pair(p_traits->begin(p_traits->extract_key(r_val)),
			      p_traits->end(p_traits->extract_key(r_val)));
      }

      virtual
      ~_Leaf() { }
#endif
    };


    /// Internal node type, PATRICIA tree.
    template<typename _ATraits, typename Metadata>
    struct _Inode
    : public _Node_base<_ATraits, Metadata>
    {
      typedef _Node_base<_ATraits, Metadata>		base_type;
      typedef typename base_type::type_traits		type_traits;
      typedef typename base_type::access_traits		access_traits;
      typedef typename type_traits::value_type		value_type;
      typedef typename base_type::allocator_type	_Alloc;
      typedef _Alloc					allocator_type;
      typedef typename _Alloc::size_type		size_type;

    private:
      typedef typename base_type::a_const_pointer	a_const_pointer;
      typedef typename base_type::a_const_iterator	a_const_iterator;

      typedef typename base_type::node_pointer		node_pointer;
      typedef typename detail::rebind_traits<_Alloc, base_type>::const_pointer
	node_const_pointer;

      typedef _Leaf<_ATraits, Metadata>	 		leaf;
      typedef typename detail::rebind_traits<_Alloc, leaf>	__rebind_l;
      typedef typename __rebind_l::pointer 		leaf_pointer;
      typedef typename __rebind_l::const_pointer 	leaf_const_pointer;

      typedef detail::rebind_traits<_Alloc, _Inode>	__rebind_in;
      typedef typename __rebind_in::pointer		inode_pointer;
      typedef typename __rebind_in::const_pointer 	inode_const_pointer;

      inline size_type
      get_pref_pos(a_const_iterator, a_const_iterator, a_const_pointer) const;

    public:
      typedef detail::rebind_traits<_Alloc, node_pointer>	__rebind_np;
      typedef typename __rebind_np::pointer 		node_pointer_pointer;
      typedef typename __rebind_np::reference 		node_pointer_reference;

      enum
	{
	  arr_size = _ATraits::max_size + 1
	};
      PB_DS_STATIC_ASSERT(min_arr_size, arr_size >= 2);


      /// Constant child iterator.
      struct const_iterator
      {
	node_pointer_pointer 				m_p_p_cur;
	node_pointer_pointer 				m_p_p_end;

	typedef std::forward_iterator_tag 		iterator_category;
	typedef typename _Alloc::difference_type 	difference_type;
	typedef node_pointer 				value_type;
	typedef node_pointer_pointer 			pointer;
	typedef node_pointer_reference 			reference;

	const_iterator(node_pointer_pointer p_p_cur = 0,
		       node_pointer_pointer p_p_end = 0)
	: m_p_p_cur(p_p_cur), m_p_p_end(p_p_end)
	{ }

	bool
	operator==(const const_iterator& other) const
	{ return m_p_p_cur == other.m_p_p_cur; }

	bool
	operator!=(const const_iterator& other) const
	{ return m_p_p_cur != other.m_p_p_cur; }

	const_iterator&
	operator++()
	{
	  do
	    ++m_p_p_cur;
	  while (m_p_p_cur != m_p_p_end && *m_p_p_cur == 0);
	  return *this;
	}

	const_iterator
	operator++(int)
	{
	  const_iterator ret_it(*this);
	  operator++();
	  return ret_it;
	}

	const node_pointer_pointer
	operator->() const
	{
	  _GLIBCXX_DEBUG_ONLY(assert_referencible();)
	  return m_p_p_cur;
	}

	node_const_pointer
	operator*() const
	{
	  _GLIBCXX_DEBUG_ONLY(assert_referencible();)
	  return *m_p_p_cur;
	}

      protected:
#ifdef _GLIBCXX_DEBUG
	void
	assert_referencible() const
	{ _GLIBCXX_DEBUG_ASSERT(m_p_p_cur != m_p_p_end && *m_p_p_cur != 0); }
#endif
      };


      /// Child iterator.
      struct iterator : public const_iterator
      {
      public:
	typedef std::forward_iterator_tag 		iterator_category;
	typedef typename _Alloc::difference_type 	difference_type;
	typedef node_pointer 				value_type;
	typedef node_pointer_pointer 			pointer;
	typedef node_pointer_reference 			reference;

	inline
	iterator(node_pointer_pointer p_p_cur = 0,
		 node_pointer_pointer p_p_end = 0)
	: const_iterator(p_p_cur, p_p_end) { }

	bool
	operator==(const iterator& other) const
	{ return const_iterator::m_p_p_cur == other.m_p_p_cur; }

	bool
	operator!=(const iterator& other) const
	{ return const_iterator::m_p_p_cur != other.m_p_p_cur; }

	iterator&
	operator++()
	{
	  const_iterator::operator++();
	  return *this;
	}

	iterator
	operator++(int)
	{
	  iterator ret_it(*this);
	  operator++();
	  return ret_it;
	}

	node_pointer_pointer
	operator->()
	{
	  _GLIBCXX_DEBUG_ONLY(const_iterator::assert_referencible();)
	  return const_iterator::m_p_p_cur;
	}

	node_pointer
	operator*()
	{
	  _GLIBCXX_DEBUG_ONLY(const_iterator::assert_referencible();)
	  return *const_iterator::m_p_p_cur;
	}
      };


      _Inode(size_type, const a_const_iterator);

      void
      update_prefixes(a_const_pointer);

      const_iterator
      begin() const;

      iterator
      begin();

      const_iterator
      end() const;

      iterator
      end();

      inline node_pointer
      get_child_node(a_const_iterator, a_const_iterator, a_const_pointer);

      inline node_const_pointer
      get_child_node(a_const_iterator, a_const_iterator, a_const_pointer) const;

      inline iterator
      get_child_it(a_const_iterator, a_const_iterator, a_const_pointer);

      inline node_pointer
      get_lower_bound_child_node(a_const_iterator, a_const_iterator,
				 size_type, a_const_pointer);

      inline node_pointer
      add_child(node_pointer, a_const_iterator, a_const_iterator,
		a_const_pointer);

      inline node_const_pointer
      get_join_child(node_const_pointer, a_const_pointer) const;

      inline node_pointer
      get_join_child(node_pointer, a_const_pointer);

      void
      remove_child(node_pointer);

      void
      remove_child(iterator);

      void
      replace_child(node_pointer, a_const_iterator, a_const_iterator,
		    a_const_pointer);

      inline a_const_iterator
      pref_b_it() const;

      inline a_const_iterator
      pref_e_it() const;

      bool
      should_be_mine(a_const_iterator, a_const_iterator, size_type,
		     a_const_pointer) const;

      leaf_pointer
      leftmost_descendant();

      leaf_const_pointer
      leftmost_descendant() const;

      leaf_pointer
      rightmost_descendant();

      leaf_const_pointer
      rightmost_descendant() const;

#ifdef _GLIBCXX_DEBUG
      typedef typename base_type::node_debug_info 	       node_debug_info;

      virtual node_debug_info
      assert_valid_imp(a_const_pointer, const char*, int) const;
#endif

      size_type
      get_e_ind() const
      { return m_e_ind; }

    private:
      _Inode(const _Inode&);

      size_type
      get_begin_pos() const;

      static __rebind_l			s_leaf_alloc;
      static __rebind_in 		s_inode_alloc;

      const size_type 			m_e_ind;
      a_const_iterator 			m_pref_b_it;
      a_const_iterator 			m_pref_e_it;
      node_pointer 			m_a_p_children[arr_size];
    };

#define PB_DS_CONST_IT_C_DEC \
    _CIter<Node, Leaf, Head, Inode, Is_Forward_Iterator>

#define PB_DS_CONST_ODIR_IT_C_DEC \
    _CIter<Node, Leaf, Head, Inode, !Is_Forward_Iterator>

#define PB_DS_IT_C_DEC \
    _Iter<Node, Leaf, Head, Inode, Is_Forward_Iterator>

#define PB_DS_ODIR_IT_C_DEC \
    _Iter<Node, Leaf, Head, Inode, !Is_Forward_Iterator>


    /// Const iterator.
    template<typename Node, typename Leaf, typename Head, typename Inode,
	     bool Is_Forward_Iterator>
    class _CIter
    {
    public:
      // These types are all the same for the first four template arguments.
      typedef typename Node::allocator_type		allocator_type;
      typedef typename Node::type_traits		type_traits;

      typedef std::bidirectional_iterator_tag 		iterator_category;
      typedef typename allocator_type::difference_type	difference_type;
      typedef typename type_traits::value_type		value_type;
      typedef typename type_traits::pointer 		pointer;
      typedef typename type_traits::reference 		reference;
      typedef typename type_traits::const_pointer 	const_pointer;
      typedef typename type_traits::const_reference 	const_reference;

      typedef allocator_type				_Alloc;
      typedef typename rebind_traits<_Alloc, Node>::pointer	node_pointer;
      typedef typename rebind_traits<_Alloc, Leaf>::pointer	leaf_pointer;
      typedef typename rebind_traits<_Alloc, Leaf>::const_pointer	leaf_const_pointer;
      typedef typename rebind_traits<_Alloc, Head>::pointer	head_pointer;

      typedef typename rebind_traits<_Alloc, Inode>::pointer 	inode_pointer;
      typedef typename Inode::iterator			inode_iterator;

      node_pointer 					m_p_nd;

      _CIter(node_pointer p_nd = 0) : m_p_nd(p_nd)
      { }

      _CIter(const PB_DS_CONST_ODIR_IT_C_DEC& other)
      : m_p_nd(other.m_p_nd)
      { }

      _CIter&
      operator=(const _CIter& other)
      {
	m_p_nd = other.m_p_nd;
	return *this;
      }

      _CIter&
      operator=(const PB_DS_CONST_ODIR_IT_C_DEC& other)
      {
	m_p_nd = other.m_p_nd;
	return *this;
      }

      const_pointer
      operator->() const
      {
	_GLIBCXX_DEBUG_ASSERT(m_p_nd->m_type == leaf_node);
	return &static_cast<leaf_pointer>(m_p_nd)->value();
      }

      const_reference
      operator*() const
      {
	_GLIBCXX_DEBUG_ASSERT(m_p_nd->m_type == leaf_node);
	return static_cast<leaf_pointer>(m_p_nd)->value();
      }

      bool
      operator==(const _CIter& other) const
      { return m_p_nd == other.m_p_nd; }

      bool
      operator==(const PB_DS_CONST_ODIR_IT_C_DEC& other) const
      { return m_p_nd == other.m_p_nd; }

      bool
      operator!=(const _CIter& other) const
      { return m_p_nd != other.m_p_nd; }

      bool
      operator!=(const PB_DS_CONST_ODIR_IT_C_DEC& other) const
      { return m_p_nd != other.m_p_nd; }

      _CIter&
      operator++()
      {
	inc(integral_constant<int, Is_Forward_Iterator>());
	return *this;
      }

      _CIter
      operator++(int)
      {
	_CIter ret_it(m_p_nd);
	operator++();
	return ret_it;
      }

      _CIter&
      operator--()
      {
	dec(integral_constant<int, Is_Forward_Iterator>());
	return *this;
      }

      _CIter
      operator--(int)
      {
	_CIter ret_it(m_p_nd);
	operator--();
	return ret_it;
      }

    protected:
      void
      inc(false_type)
      { dec(true_type()); }

      void
      inc(true_type)
      {
	if (m_p_nd->m_type == head_node)
	  {
	    m_p_nd = static_cast<head_pointer>(m_p_nd)->m_p_min;
	    return;
	  }

	node_pointer p_y = m_p_nd->m_p_parent;
	while (p_y->m_type != head_node && get_larger_sibling(m_p_nd) == 0)
	  {
	    m_p_nd = p_y;
	    p_y = p_y->m_p_parent;
	  }

	if (p_y->m_type == head_node)
	  {
	    m_p_nd = p_y;
	    return;
	  }
	m_p_nd = leftmost_descendant(get_larger_sibling(m_p_nd));
      }

      void
      dec(false_type)
      { inc(true_type()); }

      void
      dec(true_type)
      {
	if (m_p_nd->m_type == head_node)
	  {
	    m_p_nd = static_cast<head_pointer>(m_p_nd)->m_p_max;
	    return;
	  }

	node_pointer p_y = m_p_nd->m_p_parent;
	while (p_y->m_type != head_node && get_smaller_sibling(m_p_nd) == 0)
	  {
	    m_p_nd = p_y;
	    p_y = p_y->m_p_parent;
	  }

	if (p_y->m_type == head_node)
	  {
	    m_p_nd = p_y;
	    return;
	  }
	m_p_nd = rightmost_descendant(get_smaller_sibling(m_p_nd));
      }

      static node_pointer
      get_larger_sibling(node_pointer p_nd)
      {
	inode_pointer p_parent = static_cast<inode_pointer>(p_nd->m_p_parent);

	inode_iterator it = p_parent->begin();
	while (*it != p_nd)
	  ++it;

	inode_iterator next_it = it;
	++next_it;
	return (next_it == p_parent->end())? 0 : *next_it;
      }

      static node_pointer
      get_smaller_sibling(node_pointer p_nd)
      {
	inode_pointer p_parent = static_cast<inode_pointer>(p_nd->m_p_parent);

	inode_iterator it = p_parent->begin();
	if (*it == p_nd)
	  return 0;

	inode_iterator prev_it;
	do
	  {
	    prev_it = it;
	    ++it;
	    if (*it == p_nd)
	      return *prev_it;
	  }
	while (true);

	_GLIBCXX_DEBUG_ASSERT(false);
	return 0;
      }

      static leaf_pointer
      leftmost_descendant(node_pointer p_nd)
      {
	if (p_nd->m_type == leaf_node)
	  return static_cast<leaf_pointer>(p_nd);
	return static_cast<inode_pointer>(p_nd)->leftmost_descendant();
      }

      static leaf_pointer
      rightmost_descendant(node_pointer p_nd)
      {
	if (p_nd->m_type == leaf_node)
	  return static_cast<leaf_pointer>(p_nd);
	return static_cast<inode_pointer>(p_nd)->rightmost_descendant();
      }
    };


    /// Iterator.
    template<typename Node, typename Leaf, typename Head, typename Inode,
	     bool Is_Forward_Iterator>
    class _Iter
    : public _CIter<Node, Leaf, Head, Inode, Is_Forward_Iterator>
    {
    public:
      typedef _CIter<Node, Leaf, Head, Inode, Is_Forward_Iterator>
      							base_type;
      typedef typename base_type::allocator_type	allocator_type;
      typedef typename base_type::type_traits		type_traits;
      typedef typename type_traits::value_type		value_type;
      typedef typename type_traits::pointer 		pointer;
      typedef typename type_traits::reference 		reference;

      typedef typename base_type::node_pointer		node_pointer;
      typedef typename base_type::leaf_pointer		leaf_pointer;
      typedef typename base_type::leaf_const_pointer	leaf_const_pointer;
      typedef typename base_type::head_pointer		head_pointer;
      typedef typename base_type::inode_pointer 	inode_pointer;

      _Iter(node_pointer p_nd = 0)
      : base_type(p_nd) { }

      _Iter(const PB_DS_ODIR_IT_C_DEC& other)
      : base_type(other.m_p_nd) { }

      _Iter&
      operator=(const _Iter& other)
      {
	base_type::m_p_nd = other.m_p_nd;
	return *this;
      }

      _Iter&
      operator=(const PB_DS_ODIR_IT_C_DEC& other)
      {
	base_type::m_p_nd = other.m_p_nd;
	return *this;
      }

      pointer
      operator->() const
      {
	_GLIBCXX_DEBUG_ASSERT(base_type::m_p_nd->m_type == leaf_node);
	return &static_cast<leaf_pointer>(base_type::m_p_nd)->value();
      }

      reference
      operator*() const
      {
	_GLIBCXX_DEBUG_ASSERT(base_type::m_p_nd->m_type == leaf_node);
	return static_cast<leaf_pointer>(base_type::m_p_nd)->value();
      }

      _Iter&
      operator++()
      {
	base_type::operator++();
	return *this;
      }

      _Iter
      operator++(int)
      {
	_Iter ret(base_type::m_p_nd);
	operator++();
	return ret;
      }

      _Iter&
      operator--()
      {
	base_type::operator--();
	return *this;
      }

      _Iter
      operator--(int)
      {
	_Iter ret(base_type::m_p_nd);
	operator--();
	return ret;
      }
    };

#undef PB_DS_CONST_ODIR_IT_C_DEC
#undef PB_DS_ODIR_IT_C_DEC


#define PB_DS_PAT_TRIE_NODE_CONST_ITERATOR_C_DEC \
    _Node_citer<Node, Leaf, Head, Inode, _CIterator, Iterator, _ATraits, _Alloc>

#define PB_DS_PAT_TRIE_NODE_ITERATOR_C_DEC \
    _Node_iter<Node, Leaf, Head, Inode, _CIterator, Iterator, _ATraits, _Alloc>

    /// Node const iterator.
    template<typename Node,
	     typename Leaf,
	     typename Head,
	     typename Inode,
	     typename _CIterator,
	     typename Iterator,
	     typename _Alloc>
    class _Node_citer
    {
    protected:
      typedef typename rebind_traits<_Alloc, Node>::pointer	node_pointer;

      typedef typename rebind_traits<_Alloc, Leaf>::pointer	leaf_pointer;
      typedef typename rebind_traits<_Alloc, Leaf>::const_pointer	leaf_const_pointer;

      typedef typename rebind_traits<_Alloc, Inode>::pointer 	inode_pointer;
      typedef typename rebind_traits<_Alloc, Inode>::const_pointer inode_const_pointer;

      typedef typename Node::a_const_pointer		a_const_pointer;
      typedef typename Node::a_const_iterator		a_const_iterator;

    private:
      a_const_iterator
      pref_begin() const
      {
	if (m_p_nd->m_type == leaf_node)
	  {
	    leaf_const_pointer lcp = static_cast<leaf_const_pointer>(m_p_nd);
	    return m_p_traits->begin(m_p_traits->extract_key(lcp->value()));
	  }
	_GLIBCXX_DEBUG_ASSERT(m_p_nd->m_type == i_node);
	return static_cast<inode_const_pointer>(m_p_nd)->pref_b_it();
      }

      a_const_iterator
      pref_end() const
      {
	if (m_p_nd->m_type == leaf_node)
	  {
	    leaf_const_pointer lcp = static_cast<leaf_const_pointer>(m_p_nd);
	    return m_p_traits->end(m_p_traits->extract_key(lcp->value()));
	  }
	_GLIBCXX_DEBUG_ASSERT(m_p_nd->m_type == i_node);
	return static_cast<inode_const_pointer>(m_p_nd)->pref_e_it();
      }

    public:
      typedef trivial_iterator_tag 			iterator_category;
      typedef trivial_iterator_difference_type 		difference_type;
      typedef typename _Alloc::size_type 		size_type;

      typedef _CIterator 		       		value_type;
      typedef value_type 				reference;
      typedef value_type 				const_reference;

      /// Metadata type.
      typedef typename Node::metadata_type 		metadata_type;

      /// Const metadata reference type.
      typedef typename rebind_traits<_Alloc, metadata_type>::const_reference    metadata_const_reference;

      inline
      _Node_citer(node_pointer p_nd = 0, a_const_pointer p_traits = 0)
      : m_p_nd(const_cast<node_pointer>(p_nd)), m_p_traits(p_traits)
      { }

      /// Subtree valid prefix.
      std::pair<a_const_iterator, a_const_iterator>
      valid_prefix() const
      { return std::make_pair(pref_begin(), pref_end()); }

      /// Const access; returns the __const iterator* associated with
      /// the current leaf.
      const_reference
      operator*() const
      {
	_GLIBCXX_DEBUG_ASSERT(num_children() == 0);
	return _CIterator(m_p_nd);
      }

      /// Metadata access.
      metadata_const_reference
      get_metadata() const
      { return m_p_nd->get_metadata(); }

      /// Returns the number of children in the corresponding node.
      size_type
      num_children() const
      {
	if (m_p_nd->m_type == leaf_node)
	  return 0;
	_GLIBCXX_DEBUG_ASSERT(m_p_nd->m_type == i_node);
	inode_pointer inp = static_cast<inode_pointer>(m_p_nd);
	return std::distance(inp->begin(), inp->end());
      }

      /// Returns a __const node __iterator to the corresponding node's
      /// i-th child.
      _Node_citer
      get_child(size_type i) const
      {
	_GLIBCXX_DEBUG_ASSERT(m_p_nd->m_type == i_node);
	inode_pointer inp = static_cast<inode_pointer>(m_p_nd);
	typename Inode::iterator it = inp->begin();
	std::advance(it, i);
	return _Node_citer(*it, m_p_traits);
      }

      /// Compares content to a different iterator object.
      bool
      operator==(const _Node_citer& other) const
      { return m_p_nd == other.m_p_nd; }

      /// Compares content (negatively) to a different iterator object.
      bool
      operator!=(const _Node_citer& other) const
      { return m_p_nd != other.m_p_nd; }

      node_pointer 			m_p_nd;
      a_const_pointer 			m_p_traits;
    };


    /// Node iterator.
    template<typename Node,
	     typename Leaf,
	     typename Head,
	     typename Inode,
	     typename _CIterator,
	     typename Iterator,
	     typename _Alloc>
    class _Node_iter
    : public _Node_citer<Node, Leaf, Head, Inode, _CIterator, Iterator, _Alloc>
    {
    private:
      typedef _Node_citer<Node, Leaf, Head, Inode,
			  _CIterator, Iterator, _Alloc>	base_type;
      typedef typename rebind_traits<_Alloc, Node>::pointer	node_pointer;
      typedef typename base_type::inode_pointer 	inode_pointer;
      typedef typename base_type::a_const_pointer 	a_const_pointer;
      typedef Iterator 					iterator;

    public:
      typedef typename base_type::size_type 		size_type;

      typedef Iterator 					value_type;
      typedef value_type 				reference;
      typedef value_type 				const_reference;

      _Node_iter(node_pointer p_nd = 0, a_const_pointer p_traits = 0)
      : base_type(p_nd, p_traits)
      { }

      /// Access; returns the iterator*  associated with the current leaf.
      reference
      operator*() const
      {
	_GLIBCXX_DEBUG_ASSERT(base_type::num_children() == 0);
	return iterator(base_type::m_p_nd);
      }

      /// Returns a node __iterator to the corresponding node's i-th child.
      _Node_iter
      get_child(size_type i) const
      {
	_GLIBCXX_DEBUG_ASSERT(base_type::m_p_nd->m_type == i_node);

	typename Inode::iterator it =
	  static_cast<inode_pointer>(base_type::m_p_nd)->begin();

	std::advance(it, i);
	return _Node_iter(*it, base_type::m_p_traits);
      }
    };
    };


#define PB_DS_CLASS_T_DEC \
    template<typename _ATraits, typename Metadata>

#define PB_DS_CLASS_C_DEC \
    pat_trie_base::_Inode<_ATraits, Metadata>

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::__rebind_l
    PB_DS_CLASS_C_DEC::s_leaf_alloc;

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::__rebind_in
    PB_DS_CLASS_C_DEC::s_inode_alloc;

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::size_type
    PB_DS_CLASS_C_DEC::
    get_pref_pos(a_const_iterator b_it, a_const_iterator e_it,
		 a_const_pointer p_traits) const
    {
      if (static_cast<std::size_t>(std::distance(b_it, e_it)) <= m_e_ind)
	return 0;
      std::advance(b_it, m_e_ind);
      return 1 + p_traits->e_pos(*b_it);
    }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    _Inode(size_type len, const a_const_iterator it)
    : base_type(i_node), m_e_ind(len), m_pref_b_it(it), m_pref_e_it(it)
    {
      std::advance(m_pref_e_it, m_e_ind);
      std::fill(m_a_p_children, m_a_p_children + arr_size,
		static_cast<node_pointer>(0));
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    update_prefixes(a_const_pointer p_traits)
    {
      node_pointer p_first = *begin();
      if (p_first->m_type == leaf_node)
	{
	  leaf_const_pointer p = static_cast<leaf_const_pointer>(p_first);
	  m_pref_b_it = p_traits->begin(access_traits::extract_key(p->value()));
	}
      else
	{
	  inode_pointer p = static_cast<inode_pointer>(p_first);
	  _GLIBCXX_DEBUG_ASSERT(p_first->m_type == i_node);
	  m_pref_b_it = p->pref_b_it();
	}
      m_pref_e_it = m_pref_b_it;
      std::advance(m_pref_e_it, m_e_ind);
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::const_iterator
    PB_DS_CLASS_C_DEC::
    begin() const
    {
      typedef node_pointer_pointer pointer_type;
      pointer_type p = const_cast<pointer_type>(m_a_p_children);
      return const_iterator(p + get_begin_pos(), p + arr_size);
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::iterator
    PB_DS_CLASS_C_DEC::
    begin()
    {
      return iterator(m_a_p_children + get_begin_pos(),
		      m_a_p_children + arr_size);
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::const_iterator
    PB_DS_CLASS_C_DEC::
    end() const
    {
      typedef node_pointer_pointer pointer_type;
      pointer_type p = const_cast<pointer_type>(m_a_p_children) + arr_size;
      return const_iterator(p, p);
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::iterator
    PB_DS_CLASS_C_DEC::
    end()
    { return iterator(m_a_p_children + arr_size, m_a_p_children + arr_size); }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::node_pointer
    PB_DS_CLASS_C_DEC::
    get_child_node(a_const_iterator b_it, a_const_iterator e_it,
		   a_const_pointer p_traits)
    {
      const size_type i = get_pref_pos(b_it, e_it, p_traits);
      _GLIBCXX_DEBUG_ASSERT(i < arr_size);
      return m_a_p_children[i];
    }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::iterator
    PB_DS_CLASS_C_DEC::
    get_child_it(a_const_iterator b_it, a_const_iterator e_it,
		 a_const_pointer p_traits)
    {
      const size_type i = get_pref_pos(b_it, e_it, p_traits);
      _GLIBCXX_DEBUG_ASSERT(i < arr_size);
      _GLIBCXX_DEBUG_ASSERT(m_a_p_children[i] != 0);
      return iterator(m_a_p_children + i, m_a_p_children + i);
    }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::node_const_pointer
    PB_DS_CLASS_C_DEC::
    get_child_node(a_const_iterator b_it, a_const_iterator e_it,
		   a_const_pointer p_traits) const
    { return const_cast<node_pointer>(get_child_node(b_it, e_it, p_traits)); }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::node_pointer
    PB_DS_CLASS_C_DEC::
    get_lower_bound_child_node(a_const_iterator b_it, a_const_iterator e_it,
			       size_type checked_ind,
			       a_const_pointer p_traits)
    {
      if (!should_be_mine(b_it, e_it, checked_ind, p_traits))
	{
	  if (p_traits->cmp_prefixes(b_it, e_it, m_pref_b_it,
				     m_pref_e_it, true))
	    return leftmost_descendant();
	  return rightmost_descendant();
	}

      size_type i = get_pref_pos(b_it, e_it, p_traits);
      _GLIBCXX_DEBUG_ASSERT(i < arr_size);

      if (m_a_p_children[i] != 0)
	return m_a_p_children[i];

      while (++i < arr_size)
	if (m_a_p_children[i] != 0)
	  {
	    const node_type& __nt = m_a_p_children[i]->m_type;
	    node_pointer ret = m_a_p_children[i];

	    if (__nt == leaf_node)
	      return ret;

	    _GLIBCXX_DEBUG_ASSERT(__nt == i_node);
	    inode_pointer inp = static_cast<inode_pointer>(ret);
	    return inp->leftmost_descendant();
	  }

      return rightmost_descendant();
    }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::node_pointer
    PB_DS_CLASS_C_DEC::
    add_child(node_pointer p_nd, a_const_iterator b_it, a_const_iterator e_it,
	      a_const_pointer p_traits)
    {
      const size_type i = get_pref_pos(b_it, e_it, p_traits);
      _GLIBCXX_DEBUG_ASSERT(i < arr_size);
      if (m_a_p_children[i] == 0)
	{
	  m_a_p_children[i] = p_nd;
	  p_nd->m_p_parent = this;
	  return p_nd;
	}
      return m_a_p_children[i];
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::node_const_pointer
    PB_DS_CLASS_C_DEC::
    get_join_child(node_const_pointer p_nd,
		   a_const_pointer p_tr) const
    {
      node_pointer p = const_cast<node_pointer>(p_nd);
      return const_cast<inode_pointer>(this)->get_join_child(p, p_tr);
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::node_pointer
    PB_DS_CLASS_C_DEC::
    get_join_child(node_pointer p_nd, a_const_pointer p_traits)
    {
      size_type i;
      a_const_iterator b_it;
      a_const_iterator e_it;
      if (p_nd->m_type == leaf_node)
	{
	  leaf_const_pointer p = static_cast<leaf_const_pointer>(p_nd);

	  typedef typename type_traits::key_const_reference kcr;
	  kcr r_key = access_traits::extract_key(p->value());
	  b_it = p_traits->begin(r_key);
	  e_it = p_traits->end(r_key);
	}
      else
	{
	  b_it = static_cast<inode_pointer>(p_nd)->pref_b_it();
	  e_it = static_cast<inode_pointer>(p_nd)->pref_e_it();
	}
      i = get_pref_pos(b_it, e_it, p_traits);
      _GLIBCXX_DEBUG_ASSERT(i < arr_size);
      return m_a_p_children[i];
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    remove_child(node_pointer p_nd)
    {
      size_type i = 0;
      for (; i < arr_size; ++i)
	if (m_a_p_children[i] == p_nd)
	  {
	    m_a_p_children[i] = 0;
	    return;
	  }
      _GLIBCXX_DEBUG_ASSERT(i != arr_size);
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    remove_child(iterator it)
    { *it.m_p_p_cur = 0; }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    replace_child(node_pointer p_nd, a_const_iterator b_it,
		  a_const_iterator e_it,
		  a_const_pointer p_traits)
    {
      const size_type i = get_pref_pos(b_it, e_it, p_traits);
      _GLIBCXX_DEBUG_ASSERT(i < arr_size);
      m_a_p_children[i] = p_nd;
      p_nd->m_p_parent = this;
    }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::a_const_iterator
    PB_DS_CLASS_C_DEC::
    pref_b_it() const
    { return m_pref_b_it; }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::a_const_iterator
    PB_DS_CLASS_C_DEC::
    pref_e_it() const
    { return m_pref_e_it; }

    PB_DS_CLASS_T_DEC
    bool
    PB_DS_CLASS_C_DEC::
    should_be_mine(a_const_iterator b_it, a_const_iterator e_it,
		   size_type checked_ind,
		   a_const_pointer p_traits) const
    {
      if (m_e_ind == 0)
	return true;

      const size_type num_es = std::distance(b_it, e_it);
      if (num_es < m_e_ind)
	return false;

      a_const_iterator key_b_it = b_it;
      std::advance(key_b_it, checked_ind);
      a_const_iterator key_e_it = b_it;
      std::advance(key_e_it, m_e_ind);

      a_const_iterator value_b_it = m_pref_b_it;
      std::advance(value_b_it, checked_ind);
      a_const_iterator value_e_it = m_pref_b_it;
      std::advance(value_e_it, m_e_ind);

      return p_traits->equal_prefixes(key_b_it, key_e_it, value_b_it,
				      value_e_it);
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::leaf_pointer
    PB_DS_CLASS_C_DEC::
    leftmost_descendant()
    {
      node_pointer p_pot = *begin();
      if (p_pot->m_type == leaf_node)
	return (static_cast<leaf_pointer>(p_pot));
      _GLIBCXX_DEBUG_ASSERT(p_pot->m_type == i_node);
      return static_cast<inode_pointer>(p_pot)->leftmost_descendant();
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::leaf_const_pointer
    PB_DS_CLASS_C_DEC::
    leftmost_descendant() const
    { return const_cast<inode_pointer>(this)->leftmost_descendant(); }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::leaf_pointer
    PB_DS_CLASS_C_DEC::
    rightmost_descendant()
    {
      const size_type num_children = std::distance(begin(), end());
      _GLIBCXX_DEBUG_ASSERT(num_children >= 2);

      iterator it = begin();
      std::advance(it, num_children - 1);
      node_pointer p_pot =* it;
      if (p_pot->m_type == leaf_node)
	return static_cast<leaf_pointer>(p_pot);
      _GLIBCXX_DEBUG_ASSERT(p_pot->m_type == i_node);
      return static_cast<inode_pointer>(p_pot)->rightmost_descendant();
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::leaf_const_pointer
    PB_DS_CLASS_C_DEC::
    rightmost_descendant() const
    { return const_cast<inode_pointer>(this)->rightmost_descendant(); }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::size_type
    PB_DS_CLASS_C_DEC::
    get_begin_pos() const
    {
      size_type i = 0;
      for (; i < arr_size && m_a_p_children[i] == 0; ++i)
	;
      return i;
    }

#ifdef _GLIBCXX_DEBUG
    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::node_debug_info
    PB_DS_CLASS_C_DEC::
    assert_valid_imp(a_const_pointer p_traits,
		     const char* __file, int __line) const
    {
      PB_DS_DEBUG_VERIFY(base_type::m_type == i_node);
      PB_DS_DEBUG_VERIFY(static_cast<size_type>(std::distance(pref_b_it(), pref_e_it())) == m_e_ind);
      PB_DS_DEBUG_VERIFY(std::distance(begin(), end()) >= 2);

      for (typename _Inode::const_iterator it = begin(); it != end(); ++it)
	{
	  node_const_pointer p_nd = *it;
	  PB_DS_DEBUG_VERIFY(p_nd->m_p_parent == this);
	  node_debug_info child_ret = p_nd->assert_valid_imp(p_traits,
							     __file, __line);

	  PB_DS_DEBUG_VERIFY(static_cast<size_type>(std::distance(child_ret.first, child_ret.second)) >= m_e_ind);
	  PB_DS_DEBUG_VERIFY(should_be_mine(child_ret.first, child_ret.second, 0, p_traits));
	  PB_DS_DEBUG_VERIFY(get_pref_pos(child_ret.first, child_ret.second, p_traits) == static_cast<size_type>(it.m_p_p_cur - m_a_p_children));
	}
      return std::make_pair(pref_b_it(), pref_e_it());
    }
#endif

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC
  } // namespace detail
} // namespace __gnu_pbds

#endif
