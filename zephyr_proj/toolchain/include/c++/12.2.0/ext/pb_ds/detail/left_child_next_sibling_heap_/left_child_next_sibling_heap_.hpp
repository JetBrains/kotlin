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
 * @file left_child_next_sibling_heap_/left_child_next_sibling_heap_.hpp
 * Contains an implementation class for a basic heap.
 */

#ifndef PB_DS_LEFT_CHILD_NEXT_SIBLING_HEAP_HPP
#define PB_DS_LEFT_CHILD_NEXT_SIBLING_HEAP_HPP

/*
 * Based on CLRS.
 */

#include <iterator>
#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/detail/type_utils.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/node.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/point_const_iterator.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/const_iterator.hpp>
#ifdef PB_DS_LC_NS_HEAP_TRACE_
#include <iostream>
#endif
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#ifdef _GLIBCXX_DEBUG
#define PB_DS_CLASS_T_DEC \
    template<typename Value_Type, typename Cmp_Fn, typename Node_Metadata, \
	     typename _Alloc, bool Single_Link_Roots>

#define PB_DS_CLASS_C_DEC \
    left_child_next_sibling_heap<Value_Type, Cmp_Fn, Node_Metadata,	\
				  _Alloc, Single_Link_Roots>
#else
#define PB_DS_CLASS_T_DEC \
    template<typename Value_Type, typename Cmp_Fn, typename Node_Metadata, \
	     typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    left_child_next_sibling_heap<Value_Type, Cmp_Fn, Node_Metadata, _Alloc>
#endif

    /// Base class for a basic heap.
    template<typename Value_Type,
	     typename Cmp_Fn,
	     typename Node_Metadata,
	     typename _Alloc
#ifdef _GLIBCXX_DEBUG
	     ,bool Single_Link_Roots>
#else
	     >
#endif
    class left_child_next_sibling_heap : public Cmp_Fn
    {
    public:
      typedef
	left_child_next_sibling_heap_node_<Value_Type, Node_Metadata, _Alloc>
	node;

    private:
      typedef typename detail::rebind_traits<_Alloc, node> alloc_traits;

    protected:
      typedef typename alloc_traits::allocator_type node_allocator;

      typedef typename alloc_traits::pointer 		node_pointer;
      typedef typename alloc_traits::const_pointer	node_const_pointer;
      typedef Node_Metadata node_metadata;
      typedef std::pair<node_pointer, node_pointer> 	node_pointer_pair;

    private:
      typedef cond_dealtor< node, _Alloc> 		cond_dealtor_t;

      enum
	{
	  simple_value = is_simple<Value_Type>::value
	};

      typedef integral_constant<int, simple_value> 	no_throw_copies_t;
      typedef rebind_traits<_Alloc, Value_Type>	__rebind_v;

    public:
      typedef typename _Alloc::size_type 		size_type;
      typedef typename _Alloc::difference_type 	difference_type;
      typedef Value_Type 				value_type;

      typedef typename __rebind_v::pointer 	pointer;
      typedef typename __rebind_v::const_pointer const_pointer;
      typedef typename __rebind_v::reference	reference;
      typedef typename __rebind_v::const_reference const_reference;

      typedef left_child_next_sibling_heap_node_point_const_iterator_<node, _Alloc>
      point_const_iterator;

      typedef point_const_iterator 			point_iterator;

      typedef left_child_next_sibling_heap_const_iterator_<node, _Alloc>
      const_iterator;

      typedef const_iterator 				iterator;
      typedef Cmp_Fn 					cmp_fn;
      typedef _Alloc 					allocator_type;

      left_child_next_sibling_heap();
      left_child_next_sibling_heap(const Cmp_Fn&);
      left_child_next_sibling_heap(const left_child_next_sibling_heap&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      ~left_child_next_sibling_heap();

      _GLIBCXX_NODISCARD inline bool
      empty() const;

      inline size_type
      size() const;

      inline size_type
      max_size() const;

      Cmp_Fn&
      get_cmp_fn();

      const Cmp_Fn&
      get_cmp_fn() const;

      inline iterator
      begin();

      inline const_iterator
      begin() const;

      inline iterator
      end();

      inline const_iterator
      end() const;

      void
      clear();

#ifdef PB_DS_LC_NS_HEAP_TRACE_
      void
      trace() const;
#endif

    protected:
      inline node_pointer
      get_new_node_for_insert(const_reference);

      inline static void
      make_child_of(node_pointer, node_pointer);

      void
      value_swap(left_child_next_sibling_heap&);

      inline static node_pointer
      parent(node_pointer);

      inline void
      swap_with_parent(node_pointer, node_pointer);

      void
      bubble_to_top(node_pointer);

      inline void
      actual_erase_node(node_pointer);

      void
      clear_imp(node_pointer);

      void
      to_linked_list();

      template<typename Pred>
      node_pointer
      prune(Pred);

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;

      void
      assert_node_consistent(node_const_pointer, bool, const char*, int) const;

      static size_type
      size_under_node(node_const_pointer);

      static size_type
      degree(node_const_pointer);
#endif

#ifdef PB_DS_LC_NS_HEAP_TRACE_
      static void
      trace_node(node_const_pointer, size_type);
#endif

    private:
#ifdef _GLIBCXX_DEBUG
      void
      assert_iterators(const char*, int) const;

      void
      assert_size(const char*, int) const;

      static size_type
      size_from_node(node_const_pointer);
#endif

      node_pointer
      recursive_copy_node(node_const_pointer);

      inline node_pointer
      get_new_node_for_insert(const_reference, false_type);

      inline node_pointer
      get_new_node_for_insert(const_reference, true_type);

#ifdef PB_DS_LC_NS_HEAP_TRACE_
      template<typename Metadata_>
      static void
      trace_node_metadata(node_const_pointer, type_to_type<Metadata_>);

      static void
      trace_node_metadata(node_const_pointer, type_to_type<null_type>);
#endif

      static node_allocator 	s_node_allocator;
      static no_throw_copies_t 	s_no_throw_copies_ind;

    protected:
      node_pointer 		m_p_root;
      size_type 		m_size;
    };

#include <ext/pb_ds/detail/left_child_next_sibling_heap_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/iterators_fn_imps.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/trace_fn_imps.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/info_fn_imps.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/policy_access_fn_imps.hpp>

#undef PB_DS_CLASS_C_DEC
#undef PB_DS_CLASS_T_DEC

  } // namespace detail
} // namespace __gnu_pbds

#endif
