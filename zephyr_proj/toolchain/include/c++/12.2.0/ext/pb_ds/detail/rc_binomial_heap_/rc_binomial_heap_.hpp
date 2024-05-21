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
 * @file rc_binomial_heap_/rc_binomial_heap_.hpp
 * Contains an implementation for redundant-counter binomial heap.
 */

#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/detail/type_utils.hpp>
#include <ext/pb_ds/detail/binomial_heap_base_/binomial_heap_base_.hpp>
#include <ext/pb_ds/detail/rc_binomial_heap_/rc.hpp>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#define PB_DS_CLASS_T_DEC \
    template<typename Value_Type, typename Cmp_Fn, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    rc_binomial_heap<Value_Type, Cmp_Fn, _Alloc>

#define PB_DS_RC_C_DEC \
    rc<typename binomial_heap_base<Value_Type, Cmp_Fn, _Alloc>::node, _Alloc>

    /**
     *  Redundant-counter binomial heap.
     *
     *  @ingroup heap-detail
     */
    template<typename Value_Type, typename Cmp_Fn, typename _Alloc>
    class rc_binomial_heap
    : public binomial_heap_base<Value_Type, Cmp_Fn, _Alloc>
    {
    private:
      typedef binomial_heap_base<Value_Type, Cmp_Fn, _Alloc>
      							base_type;
      typedef typename base_type::node_pointer 		node_pointer;
      typedef typename base_type::node_const_pointer 	node_const_pointer;
      typedef PB_DS_RC_C_DEC 				rc_t;

    public:
      typedef Value_Type 				value_type;
      typedef typename _Alloc::size_type 		size_type;
      typedef typename _Alloc::difference_type 		difference_type;
      typedef typename base_type::pointer 		pointer;
      typedef typename base_type::const_pointer 	const_pointer;
      typedef typename base_type::reference 		reference;
      typedef typename base_type::const_reference 	const_reference;
      typedef typename base_type::point_const_iterator 	point_const_iterator;
      typedef typename base_type::point_iterator 	point_iterator;
      typedef typename base_type::const_iterator 	const_iterator;
      typedef typename base_type::iterator 		iterator;
      typedef typename base_type::cmp_fn 		cmp_fn;
      typedef typename base_type::allocator_type 	allocator_type;

      rc_binomial_heap();

      rc_binomial_heap(const Cmp_Fn&);

      rc_binomial_heap(const PB_DS_CLASS_C_DEC&);

      ~rc_binomial_heap();

      void
      swap(PB_DS_CLASS_C_DEC&);

      inline point_iterator
      push(const_reference);

      void
      modify(point_iterator, const_reference);

      inline void
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

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;
#endif

#ifdef PB_DS_RC_BINOMIAL_HEAP_TRACE_
      void
      trace() const;
#endif

    private:

      inline node_pointer
      link_with_next_sibling(node_pointer);

      void
      make_0_exposed();

      void
      make_binomial_heap();

#ifdef _GLIBCXX_DEBUG
      static node_const_pointer
      next_2_pointer(node_const_pointer);

      static node_const_pointer
      next_after_0_pointer(node_const_pointer);
#endif

      rc_t 			m_rc;
    };

#include <ext/pb_ds/detail/rc_binomial_heap_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/rc_binomial_heap_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/rc_binomial_heap_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/rc_binomial_heap_/trace_fn_imps.hpp>
#include <ext/pb_ds/detail/rc_binomial_heap_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/rc_binomial_heap_/split_join_fn_imps.hpp>

#undef PB_DS_CLASS_C_DEC
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_RC_C_DEC
  } // namespace detail
} // namespace __gnu_pbds
