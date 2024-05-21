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
 * @file ov_tree_map_/ov_tree_map_.hpp
 * Contains an implementation class for ov_tree.
 */

#include <map>
#include <set>
#include <ext/pb_ds/exception.hpp>
#include <ext/pb_ds/tree_policy.hpp>
#include <ext/pb_ds/detail/eq_fn/eq_by_less.hpp>
#include <ext/pb_ds/detail/types_traits.hpp>
#include <ext/pb_ds/detail/type_utils.hpp>
#include <ext/pb_ds/detail/tree_trace_base.hpp>
#ifdef _GLIBCXX_DEBUG
#include <ext/pb_ds/detail/debug_map_base.hpp>
#endif
#include <utility>
#include <functional>
#include <algorithm>
#include <vector>
#include <assert.h>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#ifdef PB_DS_DATA_TRUE_INDICATOR
#define PB_DS_OV_TREE_NAME ov_tree_map
#define PB_DS_CONST_NODE_ITERATOR_NAME ov_tree_node_const_iterator_map
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
#define PB_DS_OV_TREE_NAME ov_tree_set
#define PB_DS_CONST_NODE_ITERATOR_NAME ov_tree_node_const_iterator_set
#endif

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Mapped, typename Cmp_Fn, \
	     typename Node_And_It_Traits, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
   PB_DS_OV_TREE_NAME<Key, Mapped, Cmp_Fn, Node_And_It_Traits, _Alloc>

#define PB_DS_OV_TREE_TRAITS_BASE \
    types_traits<Key, Mapped, _Alloc, false>

#ifdef _GLIBCXX_DEBUG
#define PB_DS_DEBUG_MAP_BASE_C_DEC \
    debug_map_base<Key, eq_by_less<Key, Cmp_Fn>, \
		   typename rebind_traits<_Alloc, Key>::const_reference>
#endif

#ifdef PB_DS_TREE_TRACE
#define PB_DS_TREE_TRACE_BASE_C_DEC \
    tree_trace_base<typename Node_And_It_Traits::node_const_iterator,	\
		    typename Node_And_It_Traits::node_iterator,		\
		    Cmp_Fn, false, _Alloc>
#endif

#ifndef PB_DS_CHECK_KEY_EXISTS
#  error Missing definition
#endif

    /**
     *  @brief Ordered-vector tree associative-container.
     *  @ingroup branch-detail
     */
    template<typename Key, typename Mapped, typename Cmp_Fn,
	     typename Node_And_It_Traits, typename _Alloc>
    class PB_DS_OV_TREE_NAME :
#ifdef _GLIBCXX_DEBUG
      protected PB_DS_DEBUG_MAP_BASE_C_DEC,
#endif
#ifdef PB_DS_TREE_TRACE
      public PB_DS_TREE_TRACE_BASE_C_DEC,
#endif
      public Cmp_Fn,
      public Node_And_It_Traits::node_update,
      public PB_DS_OV_TREE_TRAITS_BASE
    {
    private:
      typedef PB_DS_OV_TREE_TRAITS_BASE	       		traits_base;
      typedef Node_And_It_Traits			traits_type;

      typedef typename remove_const<typename traits_base::value_type>::type non_const_value_type;

      typedef rebind_traits<_Alloc, non_const_value_type> value_alloc_traits;
      typedef typename value_alloc_traits::allocator_type value_allocator;
      typedef typename value_alloc_traits::pointer	  value_vector;

#ifdef _GLIBCXX_DEBUG
      typedef PB_DS_DEBUG_MAP_BASE_C_DEC 		debug_base;
#endif

#ifdef PB_DS_TREE_TRACE
      typedef PB_DS_TREE_TRACE_BASE_C_DEC 		trace_base;
#endif

      typedef typename traits_base::pointer 		mapped_pointer_;
      typedef typename traits_base::const_pointer 	mapped_const_pointer_;

      typedef typename traits_type::metadata_type 	metadata_type;

      typedef rebind_traits<_Alloc, metadata_type>	metadata_alloc_traits;
      typedef typename metadata_alloc_traits::allocator_type metadata_allocator;
      typedef typename metadata_alloc_traits::pointer	metadata_pointer;
      typedef typename metadata_alloc_traits::const_reference metadata_const_reference;
      typedef typename metadata_alloc_traits::reference	metadata_reference;

      typedef typename traits_type::null_node_update_pointer
      null_node_update_pointer;

    public:
      typedef ov_tree_tag 				container_category;
      typedef _Alloc 					allocator_type;
      typedef typename _Alloc::size_type 		size_type;
      typedef typename _Alloc::difference_type 		difference_type;
      typedef Cmp_Fn 					cmp_fn;

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

      typedef const_pointer 				point_const_iterator;
#ifdef PB_DS_DATA_TRUE_INDICATOR
      typedef pointer 					point_iterator;
#else
      typedef point_const_iterator 			point_iterator;
#endif

      typedef point_iterator 				iterator;
      typedef point_const_iterator 			const_iterator;

      /// Conditional destructor.
      template<typename Size_Type>
        class cond_dtor
        {
	public:
	  cond_dtor(value_vector a_vec, iterator& r_last_it, 
		    Size_Type total_size) 
	  : m_a_vec(a_vec), m_r_last_it(r_last_it), m_max_size(total_size),
	    m_no_action(false)
	  { }

	  ~cond_dtor()
	  {
	    if (m_no_action)
	      return;
	    iterator it = m_a_vec;
	    while (it != m_r_last_it)
	      {
		it->~value_type();
		++it;
	      }
	    
	    if (m_max_size > 0)
	      value_allocator().deallocate(m_a_vec, m_max_size);
	  }

	  inline void
	  set_no_action()
	  { m_no_action = true; }
	  
	protected:
	  value_vector 		m_a_vec;
	  iterator& 		m_r_last_it;
	  const Size_Type 	m_max_size;
	  bool 			m_no_action;
       };
      
      typedef typename traits_type::node_update 	node_update;
      typedef typename traits_type::node_iterator 	node_iterator;
      typedef typename traits_type::node_const_iterator	node_const_iterator;


      PB_DS_OV_TREE_NAME();

      PB_DS_OV_TREE_NAME(const Cmp_Fn&);

      PB_DS_OV_TREE_NAME(const Cmp_Fn&, const node_update&);

      PB_DS_OV_TREE_NAME(const PB_DS_CLASS_C_DEC&);

      ~PB_DS_OV_TREE_NAME();

      void
      swap(PB_DS_CLASS_C_DEC&);

      template<typename It>
      void
      copy_from_range(It, It);

      inline size_type
      max_size() const;

      _GLIBCXX_NODISCARD inline bool
      empty() const;

      inline size_type
      size() const;

      Cmp_Fn&
      get_cmp_fn();

      const Cmp_Fn&
      get_cmp_fn() const;

      inline mapped_reference
      operator[](key_const_reference r_key)
      {
#ifdef PB_DS_DATA_TRUE_INDICATOR
	PB_DS_ASSERT_VALID((*this))
	point_iterator it = lower_bound(r_key);
	if (it != end() && !Cmp_Fn::operator()(r_key, PB_DS_V2F(*it)))
	  {
	    PB_DS_CHECK_KEY_EXISTS(r_key)
	    PB_DS_ASSERT_VALID((*this))
	     return it->second;
	  }
	return insert_new_val(it, std::make_pair(r_key, mapped_type()))->second;
#else
	insert(r_key);
	return traits_base::s_null_type;
#endif
      }

      inline std::pair<point_iterator, bool>
      insert(const_reference r_value)
      {
	PB_DS_ASSERT_VALID((*this))
	key_const_reference r_key = PB_DS_V2F(r_value);
	point_iterator it = lower_bound(r_key);

	if (it != end()&&  !Cmp_Fn::operator()(r_key, PB_DS_V2F(*it)))
	  {
	    PB_DS_ASSERT_VALID((*this))
	    PB_DS_CHECK_KEY_EXISTS(r_key)
	    return std::make_pair(it, false);
	  }

	return std::make_pair(insert_new_val(it, r_value), true);
      }

      inline point_iterator
      lower_bound(key_const_reference r_key)
      {
	pointer it = m_a_values;
	pointer e_it = m_a_values + m_size;
	while (it != e_it)
	  {
	    pointer mid_it = it + ((e_it - it) >> 1);
	    if (cmp_fn::operator()(PB_DS_V2F(*mid_it), r_key))
	      it = ++mid_it;
	    else
	      e_it = mid_it;
	  }
	return it;
      }

      inline point_const_iterator
      lower_bound(key_const_reference r_key) const
      { return const_cast<PB_DS_CLASS_C_DEC& >(*this).lower_bound(r_key); }

      inline point_iterator
      upper_bound(key_const_reference r_key)
      {
	iterator pot_it = lower_bound(r_key);
	if (pot_it != end() && !Cmp_Fn::operator()(r_key, PB_DS_V2F(*pot_it)))
	  {
	    PB_DS_CHECK_KEY_EXISTS(r_key)
	    return ++pot_it;
	  }

	PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
	return pot_it;
      }

      inline point_const_iterator
      upper_bound(key_const_reference r_key) const
      { return const_cast<PB_DS_CLASS_C_DEC&>(*this).upper_bound(r_key); }

      inline point_iterator
      find(key_const_reference r_key)
      {
	PB_DS_ASSERT_VALID((*this))
	iterator pot_it = lower_bound(r_key);
	if (pot_it != end() && !Cmp_Fn::operator()(r_key, PB_DS_V2F(*pot_it)))
	  {
	    PB_DS_CHECK_KEY_EXISTS(r_key)
	    return pot_it;
	  }

	PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
	return end();
      }

      inline point_const_iterator
      find(key_const_reference r_key) const
      { return (const_cast<PB_DS_CLASS_C_DEC&>(*this).find(r_key)); }

      bool
      erase(key_const_reference);

      template<typename Pred>
      inline size_type
      erase_if(Pred);

      inline iterator
      erase(iterator it)
      { return erase_imp<iterator>(it); }

      void
      clear();

      void
      join(PB_DS_CLASS_C_DEC&);

      void
      split(key_const_reference, PB_DS_CLASS_C_DEC&);

      inline iterator
      begin()
      { return m_a_values; }

      inline const_iterator
      begin() const
      { return m_a_values; }

      inline iterator
      end()
      { return m_end_it; }

      inline const_iterator
      end() const
      { return m_end_it; }

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

    private:

      inline void
      update(node_iterator, null_node_update_pointer);

      template<typename Node_Update>
      void
      update(node_iterator, Node_Update*);

      void
      reallocate_metadata(null_node_update_pointer, size_type);

      template<typename Node_Update_>
      void
      reallocate_metadata(Node_Update_*, size_type);

      template<typename It>
      void
      copy_from_ordered_range(It, It);

      void
      value_swap(PB_DS_CLASS_C_DEC&);

      template<typename It>
      void
      copy_from_ordered_range(It, It, It, It);

      template<typename Ptr>
      inline static Ptr
      mid_pointer(Ptr p_begin, Ptr p_end)
      {
	_GLIBCXX_DEBUG_ASSERT(p_end >= p_begin);
	return (p_begin + (p_end - p_begin) / 2);
      }

      inline iterator
      insert_new_val(iterator it, const_reference r_value)
      {
#ifdef PB_DS_REGRESSION
	typename _Alloc::group_adjustor adjust(m_size);
#endif

	PB_DS_CHECK_KEY_DOES_NOT_EXIST(PB_DS_V2F(r_value))

	value_vector a_values = s_value_alloc.allocate(m_size + 1);

	iterator source_it = begin();
	iterator source_end_it = end();
	iterator target_it = a_values;
	iterator ret_it;

	cond_dtor<size_type> cd(a_values, target_it, m_size + 1);
	while (source_it != it)
	  {
	    new (const_cast<void*>(static_cast<const void*>(target_it)))
	      value_type(*source_it++);
	    ++target_it;
	  }

	new (const_cast<void*>(static_cast<const void*>(ret_it = target_it)))
	  value_type(r_value);
	++target_it;

	while (source_it != source_end_it)
	  {
	    new (const_cast<void*>(static_cast<const void*>(target_it)))
	      value_type(*source_it++);
	    ++target_it;
	  }

	reallocate_metadata((node_update*)this, m_size + 1);
	cd.set_no_action();
	if (m_size != 0)
	  {
	    cond_dtor<size_type> cd1(m_a_values, m_end_it, m_size);
	  }

	++m_size;
	m_a_values = a_values;
	m_end_it = m_a_values + m_size;
	_GLIBCXX_DEBUG_ONLY(debug_base::insert_new(PB_DS_V2F(r_value)));
	update(node_begin(), (node_update* )this);
	PB_DS_ASSERT_VALID((*this))
	return ret_it;
      }

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;

      void
      assert_iterators(const char*, int) const;
#endif

      template<typename It>
      It
      erase_imp(It);

      inline node_const_iterator
      PB_DS_node_begin_imp() const;

      inline node_const_iterator
      PB_DS_node_end_imp() const;

      inline node_iterator
      PB_DS_node_begin_imp();

      inline node_iterator
      PB_DS_node_end_imp();

    private:
      static value_allocator 	s_value_alloc;
      static metadata_allocator s_metadata_alloc;

      value_vector 		m_a_values;
      metadata_pointer 		m_a_metadata;
      iterator 			m_end_it;
      size_type 		m_size;
    };

#include <ext/pb_ds/detail/ov_tree_map_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/ov_tree_map_/iterators_fn_imps.hpp>
#include <ext/pb_ds/detail/ov_tree_map_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/ov_tree_map_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/ov_tree_map_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/ov_tree_map_/info_fn_imps.hpp>
#include <ext/pb_ds/detail/ov_tree_map_/split_join_fn_imps.hpp>
#include <ext/pb_ds/detail/bin_search_tree_/policy_access_fn_imps.hpp>

#undef PB_DS_CLASS_C_DEC
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_OV_TREE_NAME
#undef PB_DS_OV_TREE_TRAITS_BASE
#undef PB_DS_DEBUG_MAP_BASE_C_DEC
#ifdef PB_DS_TREE_TRACE
#undef PB_DS_TREE_TRACE_BASE_C_DEC
#endif
#undef PB_DS_CONST_NODE_ITERATOR_NAME
  } // namespace detail
} // namespace __gnu_pbds
