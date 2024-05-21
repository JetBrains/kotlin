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
 * @file splay_tree_/splay_tree_.hpp
 * Contains an implementation class for splay trees.
 */
/*
 * This implementation uses an idea from the SGI STL (using a @a header node
 *    which is needed for efficient iteration). Following is the SGI STL
 *    copyright.
 *
 * Copyright (c) 1996,1997
 * Silicon Graphics Computer Systems, Inc.
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.    Silicon Graphics makes no
 * representations about the suitability of this software for any
 * purpose.    It is provided "as is" without express or implied warranty.
 *
 *
 * Copyright (c) 1994
 * Hewlett-Packard Company
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.    Hewlett-Packard Company makes no
 * representations about the suitability of this software for any
 * purpose.    It is provided "as is" without express or implied warranty.
 *
 *
 */

#include <utility>
#include <vector>
#include <assert.h>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#ifdef PB_DS_DATA_TRUE_INDICATOR
# define PB_DS_S_TREE_NAME splay_tree_map
# define PB_DS_S_TREE_BASE_NAME bin_search_tree_map
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
# define PB_DS_S_TREE_NAME splay_tree_set
# define PB_DS_S_TREE_BASE_NAME bin_search_tree_set
#endif

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Mapped, typename Cmp_Fn, \
	     typename Node_And_It_Traits, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    PB_DS_S_TREE_NAME<Key, Mapped, Cmp_Fn, Node_And_It_Traits, _Alloc>

#define PB_DS_S_TREE_BASE \
    PB_DS_S_TREE_BASE_NAME<Key, Mapped, Cmp_Fn, Node_And_It_Traits, _Alloc>


    /**
     *  @brief Splay tree.
     *  @ingroup branch-detail
     */
    template<typename Key, typename Mapped, typename Cmp_Fn,
	     typename Node_And_It_Traits, typename _Alloc>
    class PB_DS_S_TREE_NAME : public PB_DS_S_TREE_BASE
    {
    private:
      typedef PB_DS_S_TREE_BASE 		       	 base_type;
#ifdef _GLIBCXX_DEBUG
      typedef base_type debug_base;
#endif
      typedef typename base_type::node_pointer 		 node_pointer;

    public:
      typedef splay_tree_tag 				 container_category;
      typedef _Alloc 					 allocator_type;
      typedef typename _Alloc::size_type 		 size_type;
      typedef typename _Alloc::difference_type 		 difference_type;
      typedef Cmp_Fn 					 cmp_fn;
      typedef typename base_type::key_type 		 key_type;
      typedef typename base_type::key_pointer 		 key_pointer;
      typedef typename base_type::key_const_pointer 	 key_const_pointer;
      typedef typename base_type::key_reference 	 key_reference;
      typedef typename base_type::key_const_reference 	 key_const_reference;
      typedef typename base_type::mapped_type 	 	 mapped_type;
      typedef typename base_type::mapped_pointer 	 mapped_pointer;
      typedef typename base_type::mapped_const_pointer 	 mapped_const_pointer;
      typedef typename base_type::mapped_reference 	 mapped_reference;
      typedef typename base_type::mapped_const_reference mapped_const_reference;
      typedef typename base_type::value_type 		 value_type;
      typedef typename base_type::pointer 		 pointer;
      typedef typename base_type::const_pointer 	 const_pointer;
      typedef typename base_type::reference 	 	 reference;
      typedef typename base_type::const_reference 	 const_reference;
      typedef typename base_type::point_iterator 	 point_iterator;
      typedef typename base_type::const_iterator 	 point_const_iterator;
      typedef typename base_type::iterator 		 iterator;
      typedef typename base_type::const_iterator 	 const_iterator;
      typedef typename base_type::reverse_iterator 	 reverse_iterator;
      typedef typename base_type::const_reverse_iterator const_reverse_iterator;
      typedef typename base_type::node_update 		 node_update;

      PB_DS_S_TREE_NAME();

      PB_DS_S_TREE_NAME(const Cmp_Fn&);

      PB_DS_S_TREE_NAME(const Cmp_Fn&, const node_update&);

      PB_DS_S_TREE_NAME(const PB_DS_CLASS_C_DEC&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      template<typename It>
      void
      copy_from_range(It, It);

      void
      initialize();

      inline std::pair<point_iterator, bool>
      insert(const_reference r_value);

      inline mapped_reference
      operator[](key_const_reference r_key)
      {
#ifdef PB_DS_DATA_TRUE_INDICATOR
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	std::pair<point_iterator, bool> ins_pair =
	  insert_leaf_imp(value_type(r_key, mapped_type()));

	ins_pair.first.m_p_nd->m_special = false;
	_GLIBCXX_DEBUG_ONLY(base_type::assert_valid(__FILE__, __LINE__));
	splay(ins_pair.first.m_p_nd);
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	return ins_pair.first.m_p_nd->m_value.second;
#else
	insert(r_key);
	return base_type::s_null_type;
#endif
      }

      inline point_iterator
      find(key_const_reference);

      inline point_const_iterator
      find(key_const_reference) const;

      inline bool
      erase(key_const_reference);

      inline iterator
      erase(iterator it);

      inline reverse_iterator
      erase(reverse_iterator);

      template<typename Pred>
      inline size_type
      erase_if(Pred);

      void
      join(PB_DS_CLASS_C_DEC&);

      void
      split(key_const_reference, PB_DS_CLASS_C_DEC&);

    private:
      inline std::pair<point_iterator, bool>
      insert_leaf_imp(const_reference);

      inline node_pointer
      find_imp(key_const_reference);

      inline const node_pointer
      find_imp(key_const_reference) const;

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char* file, int line) const;

      void
      assert_special_imp(const node_pointer, const char* file, int line) const;
#endif

      void
      splay(node_pointer);

      inline void
      splay_zig_zag_left(node_pointer, node_pointer, node_pointer);

      inline void
      splay_zig_zag_right(node_pointer, node_pointer, node_pointer);

      inline void
      splay_zig_zig_left(node_pointer, node_pointer, node_pointer);

      inline void
      splay_zig_zig_right(node_pointer, node_pointer, node_pointer);

      inline void
      splay_zz_start(node_pointer, node_pointer, node_pointer);

      inline void
      splay_zz_end(node_pointer, node_pointer, node_pointer);

      inline node_pointer
      leftmost(node_pointer);

      void
      erase_node(node_pointer);
    };

#define PB_DS_ASSERT_BASE_NODE_CONSISTENT(_Node)			\
  _GLIBCXX_DEBUG_ONLY(base_type::assert_node_consistent(_Node,		\
							__FILE__, __LINE__);)

#include <ext/pb_ds/detail/splay_tree_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/splay_tree_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/splay_tree_/splay_fn_imps.hpp>
#include <ext/pb_ds/detail/splay_tree_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/splay_tree_/find_fn_imps.hpp>
#include <ext/pb_ds/detail/splay_tree_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/splay_tree_/split_join_fn_imps.hpp>

#undef PB_DS_ASSERT_BASE_NODE_CONSISTENT
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_S_TREE_NAME
#undef PB_DS_S_TREE_BASE_NAME
#undef PB_DS_S_TREE_BASE
  } // namespace detail
} // namespace __gnu_pbds
