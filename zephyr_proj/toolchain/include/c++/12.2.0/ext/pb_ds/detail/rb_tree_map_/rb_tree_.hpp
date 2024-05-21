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
 * @file rb_tree_map_/rb_tree_.hpp
 * Contains an implementation for Red Black trees.
 */

#include <ext/pb_ds/detail/standard_policies.hpp>
#include <utility>
#include <vector>
#include <assert.h>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Mapped, typename Cmp_Fn, \
	     typename Node_And_It_Traits, typename _Alloc>

#ifdef PB_DS_DATA_TRUE_INDICATOR
# define PB_DS_RB_TREE_NAME rb_tree_map
# define PB_DS_RB_TREE_BASE_NAME bin_search_tree_map
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
# define PB_DS_RB_TREE_NAME rb_tree_set
# define PB_DS_RB_TREE_BASE_NAME bin_search_tree_set
#endif

#define PB_DS_CLASS_C_DEC \
    PB_DS_RB_TREE_NAME<Key, Mapped, Cmp_Fn, Node_And_It_Traits, _Alloc>

#define PB_DS_RB_TREE_BASE \
    PB_DS_RB_TREE_BASE_NAME<Key, Mapped, Cmp_Fn, Node_And_It_Traits, _Alloc>


    /**
     *  @brief Red-Black tree.
     *  @ingroup branch-detail
     *
     *  This implementation uses an idea from the SGI STL (using a
     *  @a header node which is needed for efficient iteration).
     */
    template<typename Key,
	     typename Mapped,
	     typename Cmp_Fn,
	     typename Node_And_It_Traits,
	     typename _Alloc>
    class PB_DS_RB_TREE_NAME : public PB_DS_RB_TREE_BASE
    {
    private:
      typedef PB_DS_RB_TREE_BASE 		       	 base_type;
      typedef typename base_type::node_pointer 		 node_pointer;

    public:
      typedef rb_tree_tag 				 container_category;
      typedef Cmp_Fn 					 cmp_fn;
      typedef _Alloc 					 allocator_type;
      typedef typename _Alloc::size_type 		 size_type;
      typedef typename _Alloc::difference_type 		 difference_type;
      typedef typename base_type::key_type 		 key_type;
      typedef typename base_type::key_pointer 		 key_pointer;
      typedef typename base_type::key_const_pointer 	 key_const_pointer;
      typedef typename base_type::key_reference 	 key_reference;
      typedef typename base_type::key_const_reference 	 key_const_reference;
      typedef typename base_type::mapped_type 		 mapped_type;
      typedef typename base_type::mapped_pointer 	 mapped_pointer;
      typedef typename base_type::mapped_const_pointer 	 mapped_const_pointer;
      typedef typename base_type::mapped_reference 	 mapped_reference;
      typedef typename base_type::mapped_const_reference mapped_const_reference;
      typedef typename base_type::value_type 		 value_type;
      typedef typename base_type::pointer 		 pointer;
      typedef typename base_type::const_pointer 	 const_pointer;
      typedef typename base_type::reference 		 reference;
      typedef typename base_type::const_reference 	 const_reference;
      typedef typename base_type::point_iterator 	 point_iterator;
      typedef typename base_type::const_iterator 	 point_const_iterator;
      typedef typename base_type::iterator 		 iterator;
      typedef typename base_type::const_iterator 	 const_iterator;
      typedef typename base_type::reverse_iterator 	 reverse_iterator;
      typedef typename base_type::const_reverse_iterator const_reverse_iterator;
      typedef typename base_type::node_update 		 node_update;

      PB_DS_RB_TREE_NAME();

      PB_DS_RB_TREE_NAME(const Cmp_Fn&);

      PB_DS_RB_TREE_NAME(const Cmp_Fn&, const node_update&);

      PB_DS_RB_TREE_NAME(const PB_DS_CLASS_C_DEC&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      template<typename It>
      void
      copy_from_range(It, It);

      inline std::pair<point_iterator, bool>
      insert(const_reference);

      inline mapped_reference
      operator[](key_const_reference r_key)
      {
#ifdef PB_DS_DATA_TRUE_INDICATOR
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	std::pair<point_iterator, bool> ins_pair =
	base_type::insert_leaf(value_type(r_key, mapped_type()));

	if (ins_pair.second == true)
	  {
	    ins_pair.first.m_p_nd->m_red = true;
	    _GLIBCXX_DEBUG_ONLY(this->structure_only_assert_valid(__FILE__, __LINE__);)
	    insert_fixup(ins_pair.first.m_p_nd);
	  }
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	return ins_pair.first.m_p_nd->m_value.second;
#else
	insert(r_key);
	return base_type::s_null_type;
#endif
      }

      inline bool
      erase(key_const_reference);

      inline iterator
      erase(iterator);

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

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;

      size_type
      assert_node_consistent(const node_pointer, const char*, int) const;
#endif

      inline static bool
      is_effectively_black(const node_pointer);

      void
      initialize();

      void
      insert_fixup(node_pointer);

      void
      erase_node(node_pointer);

      void
      remove_node(node_pointer);

      void
      remove_fixup(node_pointer, node_pointer);

      void
      split_imp(node_pointer, PB_DS_CLASS_C_DEC&);

      inline node_pointer
      split_min();

      std::pair<node_pointer, node_pointer>
      split_min_imp();

      void
      join_imp(node_pointer, node_pointer);

      std::pair<node_pointer, node_pointer>
      find_join_pos_right(node_pointer, size_type, size_type);

      std::pair<node_pointer, node_pointer>
      find_join_pos_left(node_pointer, size_type, size_type);

      inline size_type
      black_height(node_pointer);

      void
      split_at_node(node_pointer, PB_DS_CLASS_C_DEC&);
    };

#define PB_DS_STRUCT_ONLY_ASSERT_VALID(X)				\
  _GLIBCXX_DEBUG_ONLY(X.structure_only_assert_valid(__FILE__, __LINE__);)

#include <ext/pb_ds/detail/rb_tree_map_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/rb_tree_map_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/rb_tree_map_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/rb_tree_map_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/rb_tree_map_/split_join_fn_imps.hpp>
#include <ext/pb_ds/detail/rb_tree_map_/info_fn_imps.hpp>

#undef PB_DS_STRUCT_ONLY_ASSERT_VALID
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_RB_TREE_NAME
#undef PB_DS_RB_TREE_BASE_NAME
#undef PB_DS_RB_TREE_BASE
  } // namespace detail
} // namespace __gnu_pbds
