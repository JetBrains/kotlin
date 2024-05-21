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
 * @file binary_heap_/point_const_iterator.hpp
 * Contains an iterator class returned by the table's const find and insert
 * methods.
 */

#ifndef PB_DS_BINARY_HEAP_CONST_FIND_ITERATOR_HPP
#define PB_DS_BINARY_HEAP_CONST_FIND_ITERATOR_HPP

#include <ext/pb_ds/tag_and_trait.hpp>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Const point-type iterator.
    template<typename Value_Type, typename Entry, bool Simple, 
	     typename _Alloc>
    class binary_heap_point_const_iterator_
    {
    protected:
      typedef typename rebind_traits<_Alloc, Entry>::pointer entry_pointer;

    public:
      /// Category.
      typedef trivial_iterator_tag iterator_category;

      /// Difference type.
      typedef trivial_iterator_difference_type difference_type;

      /// Iterator's value type.
      typedef Value_Type value_type;

      /// Iterator's pointer type.
      typedef typename rebind_traits<_Alloc, value_type>::pointer pointer;

      /// Iterator's const pointer type.
      typedef typename rebind_traits<_Alloc, value_type>::const_pointer
	const_pointer;

      /// Iterator's reference type.
      typedef typename rebind_traits<_Alloc, value_type>::reference
	reference;

      /// Iterator's const reference type.
      typedef typename rebind_traits<_Alloc, value_type>::const_reference
	const_reference;

      inline
      binary_heap_point_const_iterator_(entry_pointer p_e) : m_p_e(p_e)
      { }

      /// Default constructor.
      inline
      binary_heap_point_const_iterator_() : m_p_e(0) { }

      /// Copy constructor.
      inline
      binary_heap_point_const_iterator_(const binary_heap_point_const_iterator_& other)
      : m_p_e(other.m_p_e)
      { }

      /// Access.
      inline const_pointer
      operator->() const
      {
	_GLIBCXX_DEBUG_ASSERT(m_p_e != 0);
	return to_ptr(integral_constant<int, Simple>());
      }

      /// Access.
      inline const_reference
      operator*() const
      {
	_GLIBCXX_DEBUG_ASSERT(m_p_e != 0);
	return *to_ptr(integral_constant<int, Simple>());
      }

      /// Compares content to a different iterator object.
      inline bool
      operator==(const binary_heap_point_const_iterator_& other) const
      { return m_p_e == other.m_p_e; }

      /// Compares content (negatively) to a different iterator object.
      inline bool
      operator!=(const binary_heap_point_const_iterator_& other) const
      { return m_p_e != other.m_p_e; }

    private:
      inline const_pointer
      to_ptr(true_type) const
      { return m_p_e; }

      inline const_pointer
      to_ptr(false_type) const
      { return *m_p_e; }

    public:
      entry_pointer m_p_e;
    };
  } // namespace detail
} // namespace __gnu_pbds

#endif 
