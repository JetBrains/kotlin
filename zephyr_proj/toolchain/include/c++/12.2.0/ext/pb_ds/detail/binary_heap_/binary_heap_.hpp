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
 * @file binary_heap_/binary_heap_.hpp
 * Contains an implementation class for a binary heap.
 */

#ifndef PB_DS_BINARY_HEAP_HPP
#define PB_DS_BINARY_HEAP_HPP

#include <queue>
#include <algorithm>
#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/detail/type_utils.hpp>
#include <ext/pb_ds/detail/binary_heap_/entry_cmp.hpp>
#include <ext/pb_ds/detail/binary_heap_/entry_pred.hpp>
#include <ext/pb_ds/detail/binary_heap_/resize_policy.hpp>
#include <ext/pb_ds/detail/binary_heap_/point_const_iterator.hpp>
#include <ext/pb_ds/detail/binary_heap_/const_iterator.hpp>
#ifdef PB_DS_BINARY_HEAP_TRACE_
#include <iostream>
#endif
#include <ext/pb_ds/detail/type_utils.hpp>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#define PB_DS_CLASS_T_DEC \
    template<typename Value_Type, typename Cmp_Fn, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    binary_heap<Value_Type, Cmp_Fn, _Alloc>

#define PB_DS_ENTRY_CMP_DEC \
    entry_cmp<Value_Type, Cmp_Fn, _Alloc, is_simple<Value_Type>::value>::type

#define PB_DS_RESIZE_POLICY_DEC	\
    __gnu_pbds::detail::resize_policy<typename _Alloc::size_type>

    /**
     *  Binary heaps composed of resize and compare policies.
     *
     *  @ingroup heap-detail
     *
     *  Based on CLRS.
     */
    template<typename Value_Type, typename Cmp_Fn, typename _Alloc>
    class binary_heap
    : public PB_DS_ENTRY_CMP_DEC, public PB_DS_RESIZE_POLICY_DEC
    {
    public:
      typedef Value_Type 				value_type;
      typedef Cmp_Fn 					cmp_fn;
      typedef _Alloc 					allocator_type;
      typedef typename _Alloc::size_type 		size_type;
      typedef typename _Alloc::difference_type 		difference_type;
      typedef typename PB_DS_ENTRY_CMP_DEC 		entry_cmp;
      typedef PB_DS_RESIZE_POLICY_DEC 			resize_policy;
      typedef cond_dealtor<value_type, _Alloc> 		cond_dealtor_t;

    private:
      enum
	{
	  simple_value = is_simple<value_type>::value
	};

      typedef integral_constant<int, simple_value> 	no_throw_copies_t;

      typedef rebind_traits<_Alloc, value_type>		__rebind_v;
      typedef typename __rebind_v::allocator_type 	value_allocator;

    public:
      typedef typename __rebind_v::pointer		pointer;
      typedef typename __rebind_v::const_pointer	const_pointer;
      typedef typename __rebind_v::reference	reference;
      typedef typename __rebind_v::const_reference	const_reference;

      typedef typename __conditional_type<simple_value,
					  value_type, pointer>::__type
      							entry;

      typedef typename rebind_traits<_Alloc, entry>::allocator_type
      							entry_allocator;

      typedef typename rebind_traits<_Alloc, entry>::pointer 	entry_pointer;

      typedef binary_heap_point_const_iterator_<value_type, entry,
						simple_value, _Alloc>
      							point_const_iterator;

      typedef point_const_iterator 			point_iterator;

      typedef binary_heap_const_iterator_<value_type, entry,
					  simple_value, _Alloc>
      							const_iterator;

      typedef const_iterator 				iterator;


      binary_heap();

      binary_heap(const cmp_fn&);

      binary_heap(const binary_heap&);

      void
      swap(binary_heap&);

      ~binary_heap();

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

      inline point_iterator
      push(const_reference);

      void
      modify(point_iterator, const_reference);

      inline const_reference
      top() const;

      inline void
      pop();

      inline void
      erase(point_iterator);

      template<typename Pred>
	size_type
	erase_if(Pred);

      inline void
      erase_at(entry_pointer, size_type, false_type);

      inline void
      erase_at(entry_pointer, size_type, true_type);

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

      template<typename Pred>
	void
	split(Pred, binary_heap&);

      void
      join(binary_heap&);

#ifdef PB_DS_BINARY_HEAP_TRACE_
      void
      trace() const;
#endif

    protected:
      template<typename It>
	void
	copy_from_range(It, It);

    private:
      void
      value_swap(binary_heap&);

      inline void
      insert_value(const_reference, false_type);

      inline void
      insert_value(value_type, true_type);

      inline void
      resize_for_insert_if_needed();

      inline void
      swap_value_imp(entry_pointer, value_type, true_type);

      inline void
      swap_value_imp(entry_pointer, const_reference, false_type);

      void
      fix(entry_pointer);

      inline const_reference
      top_imp(true_type) const;

      inline const_reference
      top_imp(false_type) const;

      inline static size_type
      left_child(size_type);

      inline static size_type
      right_child(size_type);

      inline static size_type
      parent(size_type);

      inline void
      resize_for_erase_if_needed();

      template<typename Pred>
      size_type
      partition(Pred);

      void
      make_heap()
      {
	const entry_cmp& m_cmp = static_cast<entry_cmp&>(*this);
	entry_pointer end = m_a_entries + m_size;
	std::make_heap(m_a_entries, end, m_cmp);
      }

      void
      push_heap()
      {
	const entry_cmp& m_cmp = static_cast<entry_cmp&>(*this);
	entry_pointer end = m_a_entries + m_size;
	std::push_heap(m_a_entries, end, m_cmp);
      }

      void
      pop_heap()
      {
	const entry_cmp& m_cmp = static_cast<entry_cmp&>(*this);
	entry_pointer end = m_a_entries + m_size;
	std::pop_heap(m_a_entries, end, m_cmp);
      }

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;
#endif

#ifdef PB_DS_BINARY_HEAP_TRACE_
      void
      trace_entry(const entry&, false_type) const;

      void
      trace_entry(const entry&, true_type) const;
#endif

      static entry_allocator 	s_entry_allocator;
      static value_allocator 	s_value_allocator;
      static no_throw_copies_t 	s_no_throw_copies_ind;

      size_type 		m_size;
      size_type 		m_actual_size;
      entry_pointer 		m_a_entries;
    };

#define PB_DS_ASSERT_VALID(X) \
  _GLIBCXX_DEBUG_ONLY(X.assert_valid(__FILE__, __LINE__);)

#define PB_DS_DEBUG_VERIFY(_Cond)					\
  _GLIBCXX_DEBUG_VERIFY_AT(_Cond,					\
			   _M_message(#_Cond" assertion from %1;:%2;")	\
			   ._M_string(__FILE__)._M_integer(__LINE__)	\
			   ,__file,__line)

#include <ext/pb_ds/detail/binary_heap_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/constructors_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/iterators_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/trace_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/info_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/find_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/split_join_fn_imps.hpp>
#include <ext/pb_ds/detail/binary_heap_/policy_access_fn_imps.hpp>

#undef PB_DS_CLASS_C_DEC
#undef PB_DS_CLASS_T_DEC
#undef PB_DS_ENTRY_CMP_DEC
#undef PB_DS_RESIZE_POLICY_DEC

  } // namespace detail
} // namespace __gnu_pbds

#endif
