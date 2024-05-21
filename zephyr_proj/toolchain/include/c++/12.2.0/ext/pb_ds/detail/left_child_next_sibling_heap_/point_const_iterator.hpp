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
 * @file left_child_next_sibling_heap_/point_const_iterator.hpp
 * Contains an iterator class returned by the table's const find and insert
 * methods.
 */

#ifndef PB_DS_LEFT_CHILD_NEXT_SIBLING_HEAP_CONST_FIND_ITERATOR_HPP
#define PB_DS_LEFT_CHILD_NEXT_SIBLING_HEAP_CONST_FIND_ITERATOR_HPP

#include <ext/pb_ds/tag_and_trait.hpp>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {

#define PB_DS_CLASS_T_DEC			\
    template<typename Node, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    left_child_next_sibling_heap_node_point_const_iterator_<Node, _Alloc>

    /// Const point-type iterator.
    template<typename Node, typename _Alloc>
    class left_child_next_sibling_heap_node_point_const_iterator_
    {
    protected:
      typedef typename rebind_traits<_Alloc, Node>::pointer node_pointer;

    public:
      /// Category.
      typedef trivial_iterator_tag iterator_category;

      /// Difference type.
      typedef trivial_iterator_difference_type difference_type;

      /// Iterator's value type.
      typedef typename Node::value_type value_type;

      /// Iterator's pointer type.
      typedef typename rebind_traits<_Alloc, value_type>::pointer pointer;

      /// Iterator's const pointer type.
      typedef typename rebind_traits<_Alloc, value_type>::const_pointer
	const_pointer;

      /// Iterator's reference type.
      typedef typename rebind_traits<_Alloc, value_type>::reference reference;

      /// Iterator's const reference type.
      typedef typename rebind_traits<_Alloc, value_type>::const_reference
	const_reference;

      inline
      left_child_next_sibling_heap_node_point_const_iterator_(node_pointer p_nd) : m_p_nd(p_nd)
      { }

      /// Default constructor.
      inline
      left_child_next_sibling_heap_node_point_const_iterator_() : m_p_nd(0)
      { }

      /// Copy constructor.
      inline
      left_child_next_sibling_heap_node_point_const_iterator_(const PB_DS_CLASS_C_DEC& other) : m_p_nd(other.m_p_nd)
      { }

      /// Access.
      const_pointer
      operator->() const
      {
	_GLIBCXX_DEBUG_ASSERT(m_p_nd != 0);
	return &m_p_nd->m_value;
      }

      /// Access.
      const_reference
      operator*() const
      {
	_GLIBCXX_DEBUG_ASSERT(m_p_nd != 0);
	return m_p_nd->m_value;
      }

      /// Compares content to a different iterator object.
      bool
      operator==(const PB_DS_CLASS_C_DEC& other) const
      { return m_p_nd == other.m_p_nd; }

      /// Compares content (negatively) to a different iterator object.
      bool
      operator!=(const PB_DS_CLASS_C_DEC& other) const
      { return m_p_nd != other.m_p_nd; }

      node_pointer m_p_nd;
    };

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

  } // namespace detail
} // namespace __gnu_pbds

#endif
