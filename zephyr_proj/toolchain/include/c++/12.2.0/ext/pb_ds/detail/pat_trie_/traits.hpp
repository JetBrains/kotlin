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
 * @file pat_trie_/traits.hpp
 * Contains an implementation class for pat_trie_.
 */

#ifndef PB_DS_PAT_TRIE_NODE_AND_IT_TRAITS_HPP
#define PB_DS_PAT_TRIE_NODE_AND_IT_TRAITS_HPP

#include <ext/pb_ds/detail/pat_trie_/pat_trie_base.hpp>
#include <ext/pb_ds/detail/pat_trie_/synth_access_traits.hpp>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Specialization.
    /// @ingroup traits
    template<typename Key,
	     typename Mapped,
	     typename _ATraits,
	     template<typename Node_CItr,
		      typename Node_Itr,
		      typename Cmp_Fn_,
		      typename _Alloc_>
	     class Node_Update,
	     typename _Alloc>
    struct trie_traits<Key, Mapped, _ATraits, Node_Update, pat_trie_tag, _Alloc>
    {
    private:
      typedef pat_trie_base				    	base_type;
      typedef types_traits<Key, Mapped, _Alloc, false> 		type_traits;

    public:
      typedef typename trie_node_metadata_dispatch<Key, Mapped, _ATraits, Node_Update, _Alloc>::type metadata_type;
      typedef base_type::_Metadata<metadata_type, _Alloc>      	metadata;
      typedef _ATraits 		       			       	access_traits;

      /// Type for synthesized traits.
      typedef __gnu_pbds::detail::synth_access_traits<type_traits, false, access_traits>   synth_access_traits;

      typedef base_type::_Node_base<synth_access_traits, metadata>     	node;
      typedef base_type::_Head<synth_access_traits, metadata>	       	head;
      typedef base_type::_Leaf<synth_access_traits, metadata>	       	leaf;
      typedef base_type::_Inode<synth_access_traits, metadata> 		inode;

      typedef base_type::_Iter<node, leaf, head, inode, true>	       	iterator;
      typedef base_type::_CIter<node, leaf, head, inode, true> 	 	const_iterator;
      typedef base_type::_Iter<node, leaf, head, inode, false> 		reverse_iterator;
      typedef base_type::_CIter<node, leaf, head, inode, false>	 	const_reverse_iterator;

      /// This is an iterator to an iterator: it iterates over nodes,
      /// and de-referencing it returns one of the tree's iterators.
      typedef base_type::_Node_citer<node, leaf, head, inode, const_iterator, iterator, _Alloc> node_const_iterator;

      typedef base_type::_Node_iter<node, leaf, head, inode, const_iterator, iterator, _Alloc> node_iterator;

      /// Type for node update.
      typedef Node_Update<node_const_iterator, node_iterator, _ATraits, _Alloc> 		node_update;

      typedef null_node_update<node_const_iterator, node_iterator, _ATraits, _Alloc>* 		null_node_update_pointer;
    };


    /// Specialization.
    /// @ingroup traits
    template<typename Key,
	     typename _ATraits,
	     template<typename Node_CItr,
		      typename Node_Itr,
		      typename Cmp_Fn_,
		      typename _Alloc_>
	     class Node_Update,
	     typename _Alloc>
    struct trie_traits<Key, null_type, _ATraits, Node_Update, pat_trie_tag, _Alloc>
    {
    private:
      typedef pat_trie_base					base_type;
      typedef types_traits<Key, null_type, _Alloc, false> 	type_traits;

    public:
      typedef typename trie_node_metadata_dispatch<Key, null_type, _ATraits, Node_Update, _Alloc>::type metadata_type;
      typedef base_type::_Metadata<metadata_type, _Alloc>				metadata;
      typedef _ATraits 				     					access_traits;

      /// Type for synthesized traits.
      typedef __gnu_pbds::detail::synth_access_traits<type_traits, true, access_traits> synth_access_traits;

      typedef base_type::_Node_base<synth_access_traits, metadata>     	node;
      typedef base_type::_Head<synth_access_traits, metadata> 	       	head;
      typedef base_type::_Leaf<synth_access_traits, metadata> 	       	leaf;
      typedef base_type::_Inode<synth_access_traits, metadata> 	       	inode;

      typedef base_type::_CIter<node, leaf, head, inode, true> 		const_iterator;
      typedef const_iterator 					       	iterator;
      typedef base_type::_CIter<node, leaf, head, inode, false>	       	const_reverse_iterator;
      typedef const_reverse_iterator 			       		reverse_iterator;

      /// This is an iterator to an iterator: it iterates over nodes,
      /// and de-referencing it returns one of the tree's iterators.
      typedef base_type::_Node_citer<node, leaf, head, inode, const_iterator, iterator, _Alloc> node_const_iterator;

      typedef node_const_iterator 								node_iterator;

      /// Type for node update.
      typedef Node_Update<node_const_iterator, node_iterator, _ATraits, _Alloc> 		node_update;

      typedef null_node_update<node_const_iterator, node_const_iterator, _ATraits, _Alloc>* 	null_node_update_pointer;
    };

  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifndef PB_DS_PAT_TRIE_NODE_AND_IT_TRAITS_HPP
