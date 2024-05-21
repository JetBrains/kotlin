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
 * @file bin_search_tree_/node_iterators.hpp
 * Contains an implementation class for bin_search_tree_.
 */

#ifndef PB_DS_BIN_SEARCH_TREE_NODE_ITERATORS_HPP
#define PB_DS_BIN_SEARCH_TREE_NODE_ITERATORS_HPP

#include <ext/pb_ds/tag_and_trait.hpp>

namespace __gnu_pbds
{
  namespace detail
  {
#define PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC \
    bin_search_tree_const_node_it_<Node, Const_Iterator, Iterator, _Alloc>

    /// Const node iterator.
    template<typename Node,
	     class Const_Iterator,
	     class Iterator,
	     typename _Alloc>
    class bin_search_tree_const_node_it_
    {
    private:
      typedef typename rebind_traits<_Alloc, Node>::pointer node_pointer;

    public:
      /// Category.
      typedef trivial_iterator_tag iterator_category;

      /// Difference type.
      typedef trivial_iterator_difference_type difference_type;

      /// Iterator's value type.
      typedef Const_Iterator value_type;

      /// Iterator's reference type.
      typedef Const_Iterator reference;

      /// Iterator's __const reference type.
      typedef Const_Iterator const_reference;

      /// Metadata type.
      typedef typename Node::metadata_type metadata_type;

      /// Const metadata reference type.
      typedef typename rebind_traits<_Alloc, metadata_type>::const_reference
	metadata_const_reference;


      bin_search_tree_const_node_it_(const node_pointer p_nd = 0)
      : m_p_nd(const_cast<node_pointer>(p_nd))
      { }

      /// Access.
      const_reference
      operator*() const
      { return Const_Iterator(m_p_nd); }

      /// Metadata access.
      metadata_const_reference
      get_metadata() const
      { return m_p_nd->get_metadata(); }

      /// Returns the __const node iterator associated with the left node.
      PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC
      get_l_child() const
      { return PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC(m_p_nd->m_p_left); }

      /// Returns the __const node iterator associated with the right node.
      PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC
      get_r_child() const
      { return PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC(m_p_nd->m_p_right); }

      /// Compares to a different iterator object.
      bool
      operator==(const PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC& other) const
      { return m_p_nd == other.m_p_nd; }

      /// Compares (negatively) to a different iterator object.
      bool
      operator!=(const PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC& other) const
      { return m_p_nd != other.m_p_nd; }

      node_pointer m_p_nd;
    };

#define PB_DS_TREE_NODE_ITERATOR_CLASS_C_DEC \
    bin_search_tree_node_it_<Node, Const_Iterator, Iterator, _Alloc>

    /// Node iterator.
    template<typename Node,
	     class Const_Iterator,
	     class Iterator,
	     typename _Alloc>
    class bin_search_tree_node_it_
    : public PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC
    {
    private:
      typedef typename rebind_traits<_Alloc, Node>::pointer node_pointer;

    public:
      /// Iterator's value type.
      typedef Iterator value_type;

      /// Iterator's reference type.
      typedef Iterator reference;

      /// Iterator's __const reference type.
      typedef Iterator const_reference;

      inline
      bin_search_tree_node_it_(const node_pointer p_nd = 0)
      : PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC(const_cast<node_pointer>(p_nd))
      { }

      /// Access.
      Iterator
      operator*() const
      { return Iterator(PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC::m_p_nd); }

      /// Returns the node iterator associated with the left node.
      PB_DS_TREE_NODE_ITERATOR_CLASS_C_DEC
      get_l_child() const
      {
	return PB_DS_TREE_NODE_ITERATOR_CLASS_C_DEC(
						    PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC::m_p_nd->m_p_left);
      }

      /// Returns the node iterator associated with the right node.
      PB_DS_TREE_NODE_ITERATOR_CLASS_C_DEC
      get_r_child() const
      {
	return PB_DS_TREE_NODE_ITERATOR_CLASS_C_DEC(
						    PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC::m_p_nd->m_p_right);
      }

    };

#undef PB_DS_TREE_CONST_NODE_ITERATOR_CLASS_C_DEC
#undef PB_DS_TREE_NODE_ITERATOR_CLASS_C_DEC

  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifndef PB_DS_BIN_SEARCH_TREE_NODE_ITERATORS_HPP
