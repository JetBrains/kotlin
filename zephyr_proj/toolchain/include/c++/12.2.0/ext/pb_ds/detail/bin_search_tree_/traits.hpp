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
 * @file bin_search_tree_/traits.hpp
 * Contains an implementation for bin_search_tree_.
 */

#ifndef PB_DS_BIN_SEARCH_TREE_NODE_AND_IT_TRAITS_HPP
#define PB_DS_BIN_SEARCH_TREE_NODE_AND_IT_TRAITS_HPP

#include <ext/pb_ds/detail/bin_search_tree_/point_iterators.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/node_iterators.hpp>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Binary search tree traits, primary template
    /// @ingroup traits
    template<typename Key,
	     typename Mapped,
	     class Cmp_Fn,
	     template<typename Node_CItr,
		      class Node_Itr,
		      class _Cmp_Fn,
		      typename _Alloc>
	     class Node_Update,
	     class Node,
	     typename _Alloc>
    struct bin_search_tree_traits
    {
    private:
      typedef types_traits<Key, Mapped, _Alloc, false> type_traits;
      typedef rebind_traits<_Alloc, Node> node_alloc_traits;

    public:
      typedef Node node;

      typedef
      bin_search_tree_const_it_<
	typename node_alloc_traits::pointer,
	typename type_traits::value_type,
	typename type_traits::pointer,
	typename type_traits::const_pointer,
	typename type_traits::reference,
	typename type_traits::const_reference,
	true,
	_Alloc>
      point_const_iterator;

      typedef
      bin_search_tree_it_<
	typename node_alloc_traits::pointer,
	typename type_traits::value_type,
	typename type_traits::pointer,
	typename type_traits::const_pointer,
	typename type_traits::reference,
	typename type_traits::const_reference,
	true,
	_Alloc>
      point_iterator;

      typedef
      bin_search_tree_const_it_<
	typename node_alloc_traits::pointer,
	typename type_traits::value_type,
	typename type_traits::pointer,
	typename type_traits::const_pointer,
	typename type_traits::reference,
	typename type_traits::const_reference,
	false,
	_Alloc>
      const_reverse_iterator;

      typedef
      bin_search_tree_it_<
	typename node_alloc_traits::pointer,
	typename type_traits::value_type,
	typename type_traits::pointer,
	typename type_traits::const_pointer,
	typename type_traits::reference,
	typename type_traits::const_reference,
	false,
	_Alloc>
      reverse_iterator;

      /// This is an iterator to an iterator: it iterates over nodes,
      /// and de-referencing it returns one of the tree's iterators.
      typedef
      bin_search_tree_const_node_it_<
	Node,
	point_const_iterator,
	point_iterator,
	_Alloc>
      node_const_iterator;

      typedef
      bin_search_tree_node_it_<
	Node,
	point_const_iterator,
	point_iterator,
	_Alloc>
      node_iterator;

      typedef
      Node_Update<
	node_const_iterator,
	node_iterator,
	Cmp_Fn,
	_Alloc>
      node_update;

      typedef
      __gnu_pbds::null_node_update<
	node_const_iterator,
	node_iterator,
	Cmp_Fn,
	_Alloc>* 
      null_node_update_pointer;
    };

    /// Specialization.
    /// @ingroup traits
    template<typename Key,
	     class Cmp_Fn,
	     template<typename Node_CItr,
		      class Node_Itr,
		      class _Cmp_Fn,
		      typename _Alloc>
	     class Node_Update,
	     class Node,
	     typename _Alloc>
    struct
    bin_search_tree_traits<Key, null_type, Cmp_Fn, Node_Update, Node, _Alloc>
    {
    private:
      typedef types_traits<Key, null_type, _Alloc, false> type_traits;
      typedef rebind_traits<_Alloc, Node> node_alloc_traits;

    public:
      typedef Node node;

      typedef
      bin_search_tree_const_it_<
	typename node_alloc_traits::pointer,
	typename type_traits::value_type,
	typename type_traits::pointer,
	typename type_traits::const_pointer,
	typename type_traits::reference,
	typename type_traits::const_reference,
	true,
	_Alloc>
      point_const_iterator;

      typedef point_const_iterator point_iterator;

      typedef
      bin_search_tree_const_it_<
	typename node_alloc_traits::pointer,
	typename type_traits::value_type,
	typename type_traits::pointer,
	typename type_traits::const_pointer,
	typename type_traits::reference,
	typename type_traits::const_reference,
	false,
	_Alloc>
      const_reverse_iterator;

      typedef const_reverse_iterator reverse_iterator;

      /// This is an iterator to an iterator: it iterates over nodes,
      /// and de-referencing it returns one of the tree's iterators.
      typedef
      bin_search_tree_const_node_it_<
	Node,
	point_const_iterator,
	point_iterator,
	_Alloc>
      node_const_iterator;

      typedef node_const_iterator node_iterator;

      typedef
      Node_Update<node_const_iterator, node_iterator, Cmp_Fn, _Alloc>
      node_update;

      typedef
      __gnu_pbds::null_node_update<
	node_const_iterator,
	node_iterator,
	Cmp_Fn,
	_Alloc>* 
      null_node_update_pointer;
    };

  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifndef PB_DS_BIN_SEARCH_TREE_NODE_AND_IT_TRAITS_HPP
