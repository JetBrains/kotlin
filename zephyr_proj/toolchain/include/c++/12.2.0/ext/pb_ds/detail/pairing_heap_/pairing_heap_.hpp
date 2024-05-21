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
 * @file pairing_heap_/pairing_heap_.hpp
 * Contains an implementation class for a pairing heap.
 */

/*
 * Pairing heap:
 * Michael L. Fredman, Robert Sedgewick, Daniel Dominic Sleator,
 *    and Robert Endre Tarjan, The Pairing Heap:
 *    A New Form of Self-Adjusting Heap, Algorithmica, 1(1):111-129, 1986.
 */

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
  pairing_heap<Value_Type, Cmp_Fn, _Alloc>

#ifdef _GLIBCXX_DEBUG
#define PB_DS_P_HEAP_BASE \
  left_child_next_sibling_heap<Value_Type, Cmp_Fn, null_type, _Alloc, false>
#else
#define PB_DS_P_HEAP_BASE \
  left_child_next_sibling_heap<Value_Type, Cmp_Fn, null_type, _Alloc>
#endif

    /**
     *  Pairing heap.
     *
     *  @ingroup heap-detail
     */
    template<typename Value_Type, typename Cmp_Fn, typename _Alloc>
    class pairing_heap : public PB_DS_P_HEAP_BASE
    {
    private:
      typedef PB_DS_P_HEAP_BASE				base_type;
      typedef typename base_type::node_pointer 		node_pointer;

      typedef rebind_traits<_Alloc, Value_Type>		__rebind_a;

    public:
      typedef Value_Type 				value_type;
      typedef Cmp_Fn 					cmp_fn;
      typedef _Alloc 					allocator_type;
      typedef typename _Alloc::size_type 		size_type;
      typedef typename _Alloc::difference_type 		difference_type;

      typedef typename __rebind_a::pointer 		pointer;
      typedef typename __rebind_a::const_pointer 	const_pointer;
      typedef typename __rebind_a::reference		reference;
      typedef typename __rebind_a::const_reference 	const_reference;

      typedef typename base_type::point_const_iterator	point_const_iterator;
      typedef typename base_type::point_iterator 	point_iterator;
      typedef typename base_type::const_iterator 	const_iterator;
      typedef typename base_type::iterator 		iterator;

      pairing_heap();

      pairing_heap(const Cmp_Fn&);

      pairing_heap(const pairing_heap&);

      void
      swap(pairing_heap&);

      ~pairing_heap();

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

      template<typename Pred>
      size_type
      erase_if(Pred);

      template<typename Pred>
      void
      split(Pred, pairing_heap&);

      void
      join(pairing_heap&);

    protected:

      template<typename It>
      void
      copy_from_range(It, It);

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;
#endif

    private:

      inline void
      push_imp(node_pointer);

      node_pointer
      join_node_children(node_pointer);

      node_pointer
      forward_join(node_pointer, node_pointer);

      node_pointer
      back_join(node_pointer, node_pointer);

      void
      remove_node(node_pointer);
    };

#define PB_DS_ASSERT_NODE_CONSISTENT(_Node, _Bool) \
 _GLIBCXX_DEBUG_ONLY(base_type::assert_node_consistent(_Node, _Bool,	\
						       __FILE__, __LINE__);)

#include <ext/pb_ds/detail/pairing_heap_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/pairing_heap_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/pairing_heap_/find_fn_imps.hpp>
#include <ext/pb_ds/detail/pairing_heap_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/pairing_heap_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/pairing_heap_/split_join_fn_imps.hpp>

#undef PB_DS_ASSERT_NODE_CONSISTENT
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_P_HEAP_BASE

  } // namespace detail
} // namespace __gnu_pbds
