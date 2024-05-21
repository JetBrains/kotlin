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
 * @file thin_heap_/thin_heap_.hpp
 * Contains an implementation class for a thin heap.
 */

#ifndef PB_DS_THIN_HEAP_HPP
#define PB_DS_THIN_HEAP_HPP

#include <algorithm>
#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/detail/type_utils.hpp>
#include <ext/pb_ds/detail/left_child_next_sibling_heap_/left_child_next_sibling_heap_.hpp>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#define PB_DS_CLASS_T_DEC \
    template<typename Value_Type, typename Cmp_Fn, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    thin_heap<Value_Type, Cmp_Fn, _Alloc>

#ifdef _GLIBCXX_DEBUG
#define PB_DS_BASE_T_P \
    <Value_Type, Cmp_Fn, typename _Alloc::size_type, _Alloc, true>
#else
#define PB_DS_BASE_T_P \
    <Value_Type, Cmp_Fn, typename _Alloc::size_type, _Alloc>
#endif


    /**
     *  Thin heap.
     *
     *  @ingroup heap-detail
     *
     *  See Tarjan and Kaplan.
     */
    template<typename Value_Type, typename Cmp_Fn, typename _Alloc>
    class thin_heap
    : public left_child_next_sibling_heap PB_DS_BASE_T_P
    {
    private:
      typedef rebind_traits<_Alloc, Value_Type>		  __rebind_a;
      typedef left_child_next_sibling_heap PB_DS_BASE_T_P base_type;

    protected:
      typedef typename base_type::node 			node;
      typedef typename base_type::node_pointer 		node_pointer;
      typedef typename base_type::node_const_pointer 	node_const_pointer;

    public:
      typedef Value_Type 				value_type;
      typedef Cmp_Fn 					cmp_fn;
      typedef _Alloc 					allocator_type;
      typedef typename _Alloc::size_type 		size_type;
      typedef typename _Alloc::difference_type 		difference_type;

      typedef typename __rebind_a::pointer		pointer;
      typedef typename __rebind_a::const_pointer	const_pointer;
      typedef typename __rebind_a::reference		reference;
      typedef typename __rebind_a::const_reference     	const_reference;

      typedef typename base_type::point_iterator 	point_iterator;
      typedef typename base_type::point_const_iterator 	point_const_iterator;
      typedef typename base_type::iterator 		iterator;
      typedef typename base_type::const_iterator 	const_iterator;


      inline point_iterator
      push(const_reference);

      void
      modify(point_iterator, const_reference);

      inline const_reference
      top() const;

      void
      pop();

      void
      erase(point_iterator);

      inline void
      clear();

      template<typename Pred>
      size_type
      erase_if(Pred);

      template<typename Pred>
      void
      split(Pred, PB_DS_CLASS_C_DEC&);

      void
      join(PB_DS_CLASS_C_DEC&);

    protected:
      thin_heap();

      thin_heap(const Cmp_Fn&);

      thin_heap(const PB_DS_CLASS_C_DEC&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      ~thin_heap();

      template<typename It>
      void
      copy_from_range(It, It);

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;

      void
      assert_max(const char*, int) const;
#endif

#ifdef PB_DS_THIN_HEAP_TRACE_
      void
      trace() const;
#endif

    private:
      enum
	{
	  max_rank = (sizeof(size_type) << 4) + 2
	};

      void
      initialize();

      inline void
      update_max(node_pointer);

      inline void
      fix(node_pointer);

      inline void
      fix_root(node_pointer);

      inline void
      fix_sibling_rank_1_unmarked(node_pointer);

      inline void
      fix_sibling_rank_1_marked(node_pointer);

      inline void
      fix_sibling_general_unmarked(node_pointer);

      inline void
      fix_sibling_general_marked(node_pointer);

      inline void
      fix_child(node_pointer);

      inline static void
      make_root(node_pointer);

      inline void
      make_root_and_link(node_pointer);

      inline void
      remove_max_node();

      void
      to_aux_except_max();

      inline void
      add_to_aux(node_pointer);

      inline void
      make_from_aux();

      inline size_type
      rank_bound();

      inline void
      make_child_of(node_pointer, node_pointer);

      inline void
      remove_node(node_pointer);

      inline node_pointer
      join(node_pointer, node_pointer) const;

#ifdef _GLIBCXX_DEBUG
      void
      assert_node_consistent(node_const_pointer, bool, const char*, int) const;

      void
      assert_aux_null(const char*, int) const;
#endif

      node_pointer 	m_p_max;
      node_pointer 	m_a_aux[max_rank];
    };

    enum
      {
	num_distinct_rank_bounds = 48
      };

    // Taken from the SGI implementation; acknowledged in the docs.
    static const std::size_t g_a_rank_bounds[num_distinct_rank_bounds] =
      {
	/* Dealing cards... */
	/* 0     */ 0ul,
	/* 1     */ 1ul,
	/* 2     */ 1ul,
	/* 3     */ 2ul,
	/* 4     */ 4ul,
	/* 5     */ 6ul,
	/* 6     */ 11ul,
	/* 7     */ 17ul,
	/* 8     */ 29ul,
	/* 9     */ 46ul,
	/* 10    */ 76ul,
	/* 11    */ 122ul,
	/* 12    */ 199ul,
	/* 13    */ 321ul,
	/* 14    */ 521ul,
	/* 15    */ 842ul,
	/* 16    */ 1364ul,
	/* 17    */ 2206ul,
	/* 18    */ 3571ul,
	/* 19    */ 5777ul,
	/* 20    */ 9349ul,
	/* 21    */ 15126ul,
	/* 22    */ 24476ul,
	/* 23    */ 39602ul,
	/* 24    */ 64079ul
#if __SIZE_MAX__ > 0xfffful
	,
	/* 25    */ 103681ul,
	/* 26    */ 167761ul,
	/* 27    */ 271442ul,
	/* 28    */ 439204ul,
	/* 29    */ 710646ul
#if __SIZE_MAX__ > 0xffffful
	,
	/* 30    */ 1149851ul,
	/* 31    */ 1860497ul,
	/* 32    */ 3010349ul,
	/* 33    */ 4870846ul,
	/* 34    */ 7881196ul,
	/* 35    */ 12752042ul
#if __SIZE_MAX__ > 0xfffffful
	,
	/* 36    */ 20633239ul,
	/* 37    */ 33385282ul,
	/* 38    */ 54018521ul,
	/* 39    */ 87403803ul,
	/* 40    */ 141422324ul,
	/* 41    */ 228826127ul,
	/* 42    */ 370248451ul,
	/* 43    */ 599074578ul,
	/* 44    */ 969323029ul,
	/* 45    */ 1568397607ul,
	/* 46    */ 2537720636ul,
	/* 47    */ 4106118243ul
#endif
#endif
#endif
	/* Pot's good, let's play */
      };

#define PB_DS_ASSERT_NODE_CONSISTENT(_Node, _Bool)			\
  _GLIBCXX_DEBUG_ONLY(assert_node_consistent(_Node, _Bool,		\
					     __FILE__, __LINE__);)

#define PB_DS_ASSERT_AUX_NULL(X)					\
  _GLIBCXX_DEBUG_ONLY(X.assert_aux_null(__FILE__, __LINE__);)

#include <ext/pb_ds/detail/thin_heap_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/thin_heap_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/thin_heap_/trace_fn_imps.hpp>
#include <ext/pb_ds/detail/thin_heap_/find_fn_imps.hpp>
#include <ext/pb_ds/detail/thin_heap_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/thin_heap_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/thin_heap_/split_join_fn_imps.hpp>

#undef PB_DS_ASSERT_AUX_NULL
#undef PB_DS_ASSERT_NODE_CONSISTENT
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_BASE_T_P

  } // namespace detail
} // namespace __gnu_pbds

#endif
