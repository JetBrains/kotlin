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
 * @file pat_trie_/pat_trie_.hpp
 * Contains an implementation class for a patricia tree.
 */

#include <iterator>
#include <utility>
#include <algorithm>
#include <functional>
#include <assert.h>
#include <list>
#include <ext/pb_ds/exception.hpp>
#include <ext/pb_ds/tag_and_trait.hpp>
#include <ext/pb_ds/tree_policy.hpp>
#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/detail/type_utils.hpp>
#include <ext/pb_ds/detail/types_traits.hpp>
#include <ext/pb_ds/detail/eq_fn/eq_by_less.hpp>
#include <ext/pb_ds/detail/pat_trie_/synth_access_traits.hpp>
#include <ext/pb_ds/detail/pat_trie_/pat_trie_base.hpp>
#ifdef _GLIBCXX_DEBUG
#include <ext/pb_ds/detail/debug_map_base.hpp>
#endif
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#ifdef PB_DS_DATA_TRUE_INDICATOR
#define PB_DS_PAT_TRIE_NAME pat_trie_map
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
#define PB_DS_PAT_TRIE_NAME pat_trie_set
#endif

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Mapped, typename Node_And_It_Traits, \
	     typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    PB_DS_PAT_TRIE_NAME<Key, Mapped, Node_And_It_Traits, _Alloc>

#define PB_DS_PAT_TRIE_TRAITS_BASE \
    types_traits<Key, Mapped, _Alloc, false>

#ifdef _GLIBCXX_DEBUG
#define PB_DS_DEBUG_MAP_BASE_C_DEC \
    debug_map_base<Key,	eq_by_less<Key, std::less<Key> >, \
		 typename rebind_traits<_Alloc, Key>::const_reference>
#endif


    /**
     *  @brief PATRICIA trie.
     *  @ingroup branch-detail
     *
     *  This implementation loosely borrows ideas from:
     *  1) Fast Mergeable Integer Maps, Okasaki, Gill 1998
     *  2) Ptset: Sets of integers implemented as Patricia trees,
     *     Jean-Christophe Filliatr, 2000
     */
    template<typename Key, typename Mapped, typename Node_And_It_Traits,
	     typename _Alloc>
    class PB_DS_PAT_TRIE_NAME :
#ifdef _GLIBCXX_DEBUG
      public PB_DS_DEBUG_MAP_BASE_C_DEC,
#endif
      public Node_And_It_Traits::synth_access_traits,
      public Node_And_It_Traits::node_update,
      public PB_DS_PAT_TRIE_TRAITS_BASE,
      public pat_trie_base
    {
    private:
      typedef pat_trie_base				base_type;
      typedef PB_DS_PAT_TRIE_TRAITS_BASE 		traits_base;
      typedef Node_And_It_Traits			traits_type;

      typedef typename traits_type::synth_access_traits synth_access_traits;
      typedef typename synth_access_traits::const_iterator a_const_iterator;

      typedef typename traits_type::node		node;
      typedef rebind_traits<_Alloc, node>		__rebind_n;
      typedef typename __rebind_n::const_pointer	node_const_pointer;
      typedef typename __rebind_n::pointer		node_pointer;

      typedef typename traits_type::head		head;
      typedef rebind_traits<_Alloc, head>		__rebind_h;
      typedef typename __rebind_h::allocator_type 	head_allocator;
      typedef typename __rebind_h::pointer 		head_pointer;

      typedef typename traits_type::leaf		leaf;
      typedef rebind_traits<_Alloc, leaf>		__rebind_l;
      typedef typename __rebind_l::allocator_type	leaf_allocator;
      typedef typename __rebind_l::pointer 		leaf_pointer;
      typedef typename __rebind_l::const_pointer	leaf_const_pointer;

      typedef typename traits_type::inode		inode;
      typedef typename inode::iterator 			inode_iterator;
      typedef rebind_traits<_Alloc, inode>		__rebind_in;
      typedef typename __rebind_in::allocator_type	inode_allocator;
      typedef typename __rebind_in::pointer 		inode_pointer;
      typedef typename __rebind_in::const_pointer	inode_const_pointer;


      /// Conditional deallocator.
      class cond_dealtor
      {
      protected:
	leaf_pointer 		m_p_nd;
	bool 			m_no_action_dtor;
	bool 			m_call_destructor;

      public:
	cond_dealtor(leaf_pointer p_nd)
	: m_p_nd(p_nd), m_no_action_dtor(false), m_call_destructor(false)
	{ }

	void
	set_no_action_dtor()
	{ m_no_action_dtor = true; }

	void
	set_call_destructor()
	{ m_call_destructor = true; }

	~cond_dealtor()
	{
	  if (m_no_action_dtor)
	    return;

	  if (m_call_destructor)
	    m_p_nd->~leaf();

	  s_leaf_allocator.deallocate(m_p_nd, 1);
	}
      };


      /// Branch bag, for split-join.
      class branch_bag
      {
      private:
	typedef inode_pointer 			       	__inp;
	typedef typename rebind_traits<_Alloc, __inp>::allocator_type
	  __rebind_inp;

#ifdef _GLIBCXX_DEBUG
	typedef std::_GLIBCXX_STD_C::list<__inp, __rebind_inp> 	bag_type;
#else
	typedef std::list<__inp, __rebind_inp> 			bag_type;
#endif

	bag_type 						m_bag;
      public:
	void
	add_branch()
	{
	  inode_pointer p_nd = s_inode_allocator.allocate(1);
	  __try
	    {
	      m_bag.push_back(p_nd);
	    }
	  __catch(...)
	    {
	      s_inode_allocator.deallocate(p_nd, 1);
	      __throw_exception_again;
	    }
	}

	inode_pointer
	get_branch()
	{
	  _GLIBCXX_DEBUG_ASSERT(!m_bag.empty());
	  inode_pointer p_nd = *m_bag.begin();
	  m_bag.pop_front();
	  return p_nd;
	}

	~branch_bag()
	{
	  while (!m_bag.empty())
	    {
	      inode_pointer p_nd = *m_bag.begin();
	      s_inode_allocator.deallocate(p_nd, 1);
	      m_bag.pop_front();
	    }
	}

	_GLIBCXX_NODISCARD inline bool
	empty() const
	{ return m_bag.empty(); }
      };

#ifdef _GLIBCXX_DEBUG
      typedef PB_DS_DEBUG_MAP_BASE_C_DEC 		debug_base;
#endif

      typedef typename traits_type::null_node_update_pointer null_node_update_pointer;

    public:
      typedef pat_trie_tag 				container_category;
      typedef _Alloc 					allocator_type;
      typedef typename _Alloc::size_type 		size_type;
      typedef typename _Alloc::difference_type 		difference_type;

      typedef typename traits_base::key_type 		key_type;
      typedef typename traits_base::key_pointer 	key_pointer;
      typedef typename traits_base::key_const_pointer 	key_const_pointer;
      typedef typename traits_base::key_reference 	key_reference;
      typedef typename traits_base::key_const_reference key_const_reference;
      typedef typename traits_base::mapped_type 	mapped_type;
      typedef typename traits_base::mapped_pointer 	mapped_pointer;
      typedef typename traits_base::mapped_const_pointer mapped_const_pointer;
      typedef typename traits_base::mapped_reference 	mapped_reference;
      typedef typename traits_base::mapped_const_reference mapped_const_reference;
      typedef typename traits_base::value_type 		value_type;
      typedef typename traits_base::pointer 		pointer;
      typedef typename traits_base::const_pointer 	const_pointer;
      typedef typename traits_base::reference 		reference;
      typedef typename traits_base::const_reference 	const_reference;

      typedef typename traits_type::access_traits 	access_traits;
      typedef typename traits_type::const_iterator 	point_const_iterator;
      typedef typename traits_type::iterator 		point_iterator;
      typedef point_const_iterator 			const_iterator;
      typedef point_iterator 				iterator;

      typedef typename traits_type::reverse_iterator 	reverse_iterator;
      typedef typename traits_type::const_reverse_iterator const_reverse_iterator;
      typedef typename traits_type::node_const_iterator node_const_iterator;
      typedef typename traits_type::node_iterator 	node_iterator;
      typedef typename traits_type::node_update 	node_update;

      PB_DS_PAT_TRIE_NAME();

      PB_DS_PAT_TRIE_NAME(const access_traits&);

      PB_DS_PAT_TRIE_NAME(const PB_DS_CLASS_C_DEC&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      ~PB_DS_PAT_TRIE_NAME();

      _GLIBCXX_NODISCARD inline bool
      empty() const;

      inline size_type
      size() const;

      inline size_type
      max_size() const;

      access_traits&
      get_access_traits();

      const access_traits&
      get_access_traits() const;

      node_update&
      get_node_update();

      const node_update&
      get_node_update() const;

      inline std::pair<point_iterator, bool>
      insert(const_reference);

      inline mapped_reference
      operator[](key_const_reference r_key)
      {
#ifdef PB_DS_DATA_TRUE_INDICATOR
	return insert(std::make_pair(r_key, mapped_type())).first->second;
#else
	insert(r_key);
	return traits_base::s_null_type;
#endif
      }

      inline point_iterator
      find(key_const_reference);

      inline point_const_iterator
      find(key_const_reference) const;

      inline point_iterator
      lower_bound(key_const_reference);

      inline point_const_iterator
      lower_bound(key_const_reference) const;

      inline point_iterator
      upper_bound(key_const_reference);

      inline point_const_iterator
      upper_bound(key_const_reference) const;

      void
      clear();

      inline bool
      erase(key_const_reference);

      inline const_iterator
      erase(const_iterator);

#ifdef PB_DS_DATA_TRUE_INDICATOR
      inline iterator
      erase(iterator);
#endif

      inline const_reverse_iterator
      erase(const_reverse_iterator);

#ifdef PB_DS_DATA_TRUE_INDICATOR
      inline reverse_iterator
      erase(reverse_iterator);
#endif

      template<typename Pred>
      inline size_type
      erase_if(Pred);

      void
      join(PB_DS_CLASS_C_DEC&);

      void
      split(key_const_reference, PB_DS_CLASS_C_DEC&);

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

#ifdef PB_DS_PAT_TRIE_TRACE_
      void
      trace() const;
#endif

    protected:
      template<typename It>
      void
      copy_from_range(It, It);

      void
      value_swap(PB_DS_CLASS_C_DEC&);

      node_pointer
      recursive_copy_node(node_const_pointer);

    private:
      void
      initialize();

      inline void
      apply_update(node_pointer, null_node_update_pointer);

      template<typename Node_Update_>
      inline void
      apply_update(node_pointer, Node_Update_*);

      bool
      join_prep(PB_DS_CLASS_C_DEC&, branch_bag&);

      void
      rec_join_prep(node_const_pointer, node_const_pointer, branch_bag&);

      void
      rec_join_prep(leaf_const_pointer, leaf_const_pointer, branch_bag&);

      void
      rec_join_prep(leaf_const_pointer, inode_const_pointer, branch_bag&);

      void
      rec_join_prep(inode_const_pointer, leaf_const_pointer, branch_bag&);

      void
      rec_join_prep(inode_const_pointer, inode_const_pointer, branch_bag&);

      node_pointer
      rec_join(node_pointer, node_pointer, size_type, branch_bag&);

      node_pointer
      rec_join(leaf_pointer, leaf_pointer, branch_bag&);

      node_pointer
      rec_join(leaf_pointer, inode_pointer, size_type, branch_bag&);

      node_pointer
      rec_join(inode_pointer, leaf_pointer, size_type, branch_bag&);

      node_pointer
      rec_join(inode_pointer, inode_pointer, branch_bag&);

      size_type
      keys_diff_ind(typename access_traits::const_iterator,
		    typename access_traits::const_iterator,
		    typename access_traits::const_iterator,
		    typename access_traits::const_iterator);

      inode_pointer
      insert_branch(node_pointer, node_pointer, branch_bag&);

      void
      update_min_max_for_inserted_leaf(leaf_pointer);

      void
      erase_leaf(leaf_pointer);

      inline void
      actual_erase_leaf(leaf_pointer);

      void
      clear_imp(node_pointer);

      void
      erase_fixup(inode_pointer);

      void
      update_min_max_for_erased_leaf(leaf_pointer);

      static inline a_const_iterator
      pref_begin(node_const_pointer);

      static inline a_const_iterator
      pref_end(node_const_pointer);

      inline node_pointer
      find_imp(key_const_reference);

      inline node_pointer
      lower_bound_imp(key_const_reference);

      inline node_pointer
      upper_bound_imp(key_const_reference);

      inline static leaf_const_pointer
      leftmost_descendant(node_const_pointer);

      inline static leaf_pointer
      leftmost_descendant(node_pointer);

      inline static leaf_const_pointer
      rightmost_descendant(node_const_pointer);

      inline static leaf_pointer
      rightmost_descendant(node_pointer);

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;

      void
      assert_iterators(const char*, int) const;

      void
      assert_reverse_iterators(const char*, int) const;

      static size_type
      recursive_count_leafs(node_const_pointer, const char*, int);
#endif

#ifdef PB_DS_PAT_TRIE_TRACE_
      static void
      trace_node(node_const_pointer, size_type);

      template<typename Metadata_>
      static void
      trace_node_metadata(node_const_pointer, type_to_type<Metadata_>);

      static void
      trace_node_metadata(node_const_pointer, type_to_type<null_type>);
#endif

      leaf_pointer
      split_prep(key_const_reference, PB_DS_CLASS_C_DEC&, branch_bag&);

      node_pointer
      rec_split(node_pointer, a_const_iterator, a_const_iterator,
		PB_DS_CLASS_C_DEC&, branch_bag&);

      void
      split_insert_branch(size_type, a_const_iterator, inode_iterator,
			  size_type, branch_bag&);

      static head_allocator 		s_head_allocator;
      static inode_allocator 		s_inode_allocator;
      static leaf_allocator 		s_leaf_allocator;

      head_pointer 			m_p_head;
      size_type 			m_size;
    };

#define PB_DS_ASSERT_NODE_VALID(X) \
  _GLIBCXX_DEBUG_ONLY(X->assert_valid(this, __FILE__, __LINE__);)

#define PB_DS_RECURSIVE_COUNT_LEAFS(X) \
  recursive_count_leafs(X, __FILE__, __LINE__)

#include <ext/pb_ds/detail/pat_trie_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/iterators_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/insert_join_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/find_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/info_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/policy_access_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/split_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/trace_fn_imps.hpp>
#include <ext/pb_ds/detail/pat_trie_/update_fn_imps.hpp>

#undef PB_DS_RECURSIVE_COUNT_LEAFS
#undef PB_DS_ASSERT_NODE_VALID
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_PAT_TRIE_NAME
#undef PB_DS_PAT_TRIE_TRAITS_BASE
#undef PB_DS_DEBUG_MAP_BASE_C_DEC
  } // namespace detail
} // namespace __gnu_pbds
