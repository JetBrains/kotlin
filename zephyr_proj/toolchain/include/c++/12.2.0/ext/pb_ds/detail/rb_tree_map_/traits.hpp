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
 * @file rb_tree_map_/traits.hpp
 * Contains an implementation for rb_tree_.
 */

#ifndef PB_DS_RB_TREE_NODE_AND_IT_TRAITS_HPP
#define PB_DS_RB_TREE_NODE_AND_IT_TRAITS_HPP

#include <ext/pb_ds/detail/rb_tree_map_/node.hpp>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Specialization.
    /// @ingroup traits
    template<typename Key,
	     typename Mapped,
	     typename Cmp_Fn,
	     template<typename Node_CItr,
		      typename Node_Itr,
		      typename Cmp_Fn_,
		      typename _Alloc_>
	     class Node_Update,
	     typename _Alloc>
    struct tree_traits<Key, Mapped, Cmp_Fn, Node_Update, rb_tree_tag,_Alloc> 
    : public bin_search_tree_traits<
      Key,
      Mapped,
      Cmp_Fn,
      Node_Update,
      rb_tree_node_<
	typename types_traits<Key, Mapped, _Alloc, false>::value_type,
	typename tree_node_metadata_dispatch<Key, Mapped, Cmp_Fn, Node_Update,
					     _Alloc>::type,
	_Alloc>,
      _Alloc>
    { };

    /// Specialization.
    /// @ingroup traits
    template<typename Key,
	     typename Cmp_Fn,
	     template<typename Node_CItr,
		      typename Node_Itr,
		      typename Cmp_Fn_,
		      typename _Alloc_>
	     class Node_Update,
	     typename _Alloc>
    struct tree_traits<Key, null_type, Cmp_Fn, Node_Update, rb_tree_tag,_Alloc> 
    : public bin_search_tree_traits<
      Key,
      null_type,
      Cmp_Fn,
      Node_Update,
      rb_tree_node_<
      typename types_traits<Key, null_type, _Alloc, false>::value_type,
      typename tree_node_metadata_dispatch<Key, null_type, Cmp_Fn, Node_Update,
					   _Alloc>::type,
	_Alloc>,
	  _Alloc>
    { };

  } // namespace detail
} // namespace __gnu_pbds

#endif 
