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
 *  @file bin_search_tree_/bin_search_tree_.hpp
 *  Contains an implementation class for binary search tree.
 */

#include <ext/pb_ds/exception.hpp>
#include <ext/pb_ds/tree_policy.hpp>
#include <ext/pb_ds/detail/eq_fn/eq_by_less.hpp>
#include <ext/pb_ds/detail/types_traits.hpp>
#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/detail/type_utils.hpp>
#include <ext/pb_ds/detail/tree_trace_base.hpp>
#ifdef _GLIBCXX_DEBUG
#include <ext/pb_ds/detail/debug_map_base.hpp>
#endif
#include <utility>
#include <functional>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#ifdef PB_DS_DATA_TRUE_INDICATOR
#define PB_DS_BIN_TREE_NAME bin_search_tree_map
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
#define PB_DS_BIN_TREE_NAME bin_search_tree_set
#endif

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Mapped, typename Cmp_Fn, \
	     typename Node_And_It_Traits, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    PB_DS_BIN_TREE_NAME<Key, Mapped, Cmp_Fn, Node_And_It_Traits, _Alloc>

#define PB_DS_BIN_TREE_TRAITS_BASE \
    types_traits<Key, Mapped, _Alloc, false>

#ifdef _GLIBCXX_DEBUG
#define PB_DS_DEBUG_MAP_BASE_C_DEC  \
    debug_map_base<Key,	eq_by_less<Key, Cmp_Fn>, \
	      typename rebind_traits<_Alloc, Key>::const_reference>
#endif

#ifdef PB_DS_TREE_TRACE
#define PB_DS_TREE_TRACE_BASE_C_DEC \
    tree_trace_base<typename Node_And_It_Traits::node_const_iterator, \
		    typename Node_And_It_Traits::node_iterator,	      \
		    Cmp_Fn, true, _Alloc>
#endif


    /*
     *  @brief Binary search tree (BST).
     *
     *  This implementation uses an idea from the SGI STL (using a @a
     *  header node which is needed for efficient iteration).
     */
    template<typename Key, typename Mapped, typename Cmp_Fn,
	     typename Node_And_It_Traits, typename _Alloc>
    class PB_DS_BIN_TREE_NAME :
#ifdef _GLIBCXX_DEBUG
      public PB_DS_DEBUG_MAP_BASE_C_DEC,
#endif
#ifdef PB_DS_TREE_TRACE
      public PB_DS_TREE_TRACE_BASE_C_DEC,
#endif
      public Cmp_Fn,
      public PB_DS_BIN_TREE_TRAITS_BASE,
      public Node_And_It_Traits::node_update
    {
      typedef Node_And_It_Traits 			traits_type;
      typedef rebind_traits<_Alloc, typename traits_type::node>
	node_alloc_traits;

    protected:
      typedef PB_DS_BIN_TREE_TRAITS_BASE 	       	traits_base;

      typedef
      typename node_alloc_traits::allocator_type	node_allocator;

      typedef typename node_alloc_traits::value_type 	node;
      typedef typename node_alloc_traits::pointer 	node_pointer;

      typedef typename traits_type::null_node_update_pointer
      null_node_update_pointer;

    private:
      typedef cond_dealtor<node, _Alloc> 		cond_dealtor_t;

#ifdef _GLIBCXX_DEBUG
      typedef PB_DS_DEBUG_MAP_BASE_C_DEC 		debug_base;
#endif

    public:
      typedef typename _Alloc::size_type 		size_type;
      typedef typename _Alloc::difference_type 	difference_type;
      typedef typename traits_base::key_type 		key_type;
      typedef typename traits_base::key_pointer 	key_pointer;
      typedef typename traits_base::key_const_pointer 	key_const_pointer;
      typedef typename traits_base::key_reference 	key_reference;
      typedef typename traits_base::key_const_reference key_const_reference;

#ifdef PB_DS_DATA_TRUE_INDICATOR
      typedef typename traits_base::mapped_type 	mapped_type;
      typedef typename traits_base::mapped_pointer 	mapped_pointer;
      typedef typename traits_base::mapped_const_pointer mapped_const_pointer;
      typedef typename traits_base::mapped_reference 	mapped_reference;
      typedef typename traits_base::mapped_const_reference mapped_const_reference;
#endif

      typedef typename traits_base::value_type 		value_type;
      typedef typename traits_base::pointer 		pointer;
      typedef typename traits_base::const_pointer 	const_pointer;
      typedef typename traits_base::reference 		reference;
      typedef typename traits_base::const_reference 	const_reference;
      typedef typename traits_type::point_const_iterator point_const_iterator;

      typedef point_const_iterator 			const_iterator;
      typedef typename traits_type::point_iterator 	point_iterator;
      typedef point_iterator 				iterator;

      typedef typename traits_type::const_reverse_iterator const_reverse_iterator;

      typedef typename traits_type::reverse_iterator 	reverse_iterator;
      typedef typename traits_type::node_const_iterator node_const_iterator;
      typedef typename traits_type::node_iterator 	node_iterator;
      typedef typename traits_type::node_update 	node_update;

      typedef Cmp_Fn 					cmp_fn;
      typedef _Alloc 					allocator_type;

      PB_DS_BIN_TREE_NAME();

      PB_DS_BIN_TREE_NAME(const Cmp_Fn&);

      PB_DS_BIN_TREE_NAME(const Cmp_Fn&, const node_update&);

      PB_DS_BIN_TREE_NAME(const PB_DS_CLASS_C_DEC&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      ~PB_DS_BIN_TREE_NAME();

      inline bool
      empty() const;

      inline size_type
      size() const;

      inline size_type
      max_size() const;

      Cmp_Fn&
      get_cmp_fn();

      const Cmp_Fn&
      get_cmp_fn() const;

      inline point_iterator
      lower_bound(key_const_reference);

      inline point_const_iterator
      lower_bound(key_const_reference) const;

      inline point_iterator
      upper_bound(key_const_reference);

      inline point_const_iterator
      upper_bound(key_const_reference) const;

      inline point_iterator
      find(key_const_reference);

      inline point_const_iterator
      find(key_const_reference) const;

      inline iterator
      begin();

      inline const_iterator
      begin() const;

      inline iterator
      end();

      inline const_iterator
      end() const;

      inline reverse_iterator
      rbegin();

      inline const_reverse_iterator
      rbegin() const;

      inline reverse_iterator
      rend();

      inline const_reverse_iterator
      rend() const;

      /// Returns a const node_iterator corresponding to the node at the
      /// root of the tree.
      inline node_const_iterator
      node_begin() const;

      /// Returns a node_iterator corresponding to the node at the
      /// root of the tree.
      inline node_iterator
      node_begin();

      /// Returns a const node_iterator corresponding to a node just
      /// after a leaf of the tree.
      inline node_const_iterator
      node_end() const;

      /// Returns a node_iterator corresponding to a node just
      /// after a leaf of the tree.
      inline node_iterator
      node_end();

      void
      clear();

    protected:
      void
      value_swap(PB_DS_CLASS_C_DEC&);

      void
      initialize_min_max();

      inline iterator
      insert_imp_empty(const_reference);

      inline iterator
      insert_leaf_new(const_reference, node_pointer, bool);

      inline node_pointer
      get_new_node_for_leaf_insert(const_reference, false_type);

      inline node_pointer
      get_new_node_for_leaf_insert(const_reference, true_type);

      inline void
      actual_erase_node(node_pointer);

      inline std::pair<node_pointer, bool>
      erase(node_pointer);

      inline void
      update_min_max_for_erased_node(node_pointer);

      static void
      clear_imp(node_pointer);

      inline std::pair<point_iterator, bool>
      insert_leaf(const_reference);

      inline void
      rotate_left(node_pointer);

      inline void
      rotate_right(node_pointer);

      inline void
      rotate_parent(node_pointer);

      inline void
      apply_update(node_pointer, null_node_update_pointer);

      template<typename Node_Update_>
	inline void
	apply_update(node_pointer, Node_Update_*);

      inline void
      update_to_top(node_pointer, null_node_update_pointer);

      template<typename Node_Update_>
	inline void
	update_to_top(node_pointer, Node_Update_*);

      bool
      join_prep(PB_DS_CLASS_C_DEC&);

      void
      join_finish(PB_DS_CLASS_C_DEC&);

      bool
      split_prep(key_const_reference, PB_DS_CLASS_C_DEC&);

      void
      split_finish(PB_DS_CLASS_C_DEC&);

      size_type
      recursive_count(node_pointer) const;

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;

      void
      structure_only_assert_valid(const char*, int) const;

      void
      assert_node_consistent(const node_pointer, const char*, int) const;
#endif

    private:
#ifdef _GLIBCXX_DEBUG
      void
      assert_iterators(const char*, int) const;

      void
      assert_consistent_with_debug_base(const char*, int) const;

      void
      assert_node_consistent_with_left(const node_pointer,
				       const char*, int) const;

      void
      assert_node_consistent_with_right(const node_pointer,
					const char*, int) const;

      void
      assert_consistent_with_debug_base(const node_pointer,
					const char*, int) const;

      void
      assert_min(const char*, int) const;

      void
      assert_min_imp(const node_pointer, const char*, int) const;

      void
      assert_max(const char*, int) const;

      void
      assert_max_imp(const node_pointer, const char*, int) const;

      void
      assert_size(const char*, int) const;

      typedef std::pair<const_pointer, const_pointer> node_consistent_t;

      node_consistent_t
      assert_node_consistent_(const node_pointer, const char*, int) const;
#endif

      void
      initialize();

      node_pointer
      recursive_copy_node(const node_pointer);

    protected:
      node_pointer 		m_p_head;
      size_type 		m_size;
      static node_allocator 	s_node_allocator;
    };

#define PB_DS_STRUCT_ONLY_ASSERT_VALID(X)				\
  _GLIBCXX_DEBUG_ONLY(X.structure_only_assert_valid(__FILE__, __LINE__);)

#define PB_DS_ASSERT_NODE_CONSISTENT(_Node)				\
  _GLIBCXX_DEBUG_ONLY(assert_node_consistent(_Node, __FILE__, __LINE__);)

#include <ext/pb_ds/detail/bin_search_tree_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/iterators_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/find_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/info_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/split_join_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/rotate_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/policy_access_fn_imps.hpp>

#undef PB_DS_ASSERT_NODE_CONSISTENT
#undef PB_DS_STRUCT_ONLY_ASSERT_VALID
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_BIN_TREE_NAME
#undef PB_DS_BIN_TREE_TRAITS_BASE
#undef PB_DS_DEBUG_MAP_BASE_C_DEC

#ifdef PB_DS_TREE_TRACE
#undef PB_DS_TREE_TRACE_BASE_C_DEC
#endif
  } // namespace detail
} // namespace __gnu_pbds
