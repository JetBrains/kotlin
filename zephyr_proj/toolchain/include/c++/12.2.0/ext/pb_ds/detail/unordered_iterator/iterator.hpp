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
 * @file iterator.hpp
 * Contains an iterator_ class used for ranging over the elements of the
 * table.
 *
 * This file is intended to be included inside a class definition, with
 * PB_DS_CLASS_C_DEC defined to the name of the enclosing class.
 */

#ifdef PB_DS_CLASS_C_DEC
/// Range-type iterator.
class iterator_
: public const_iterator_
{
public:
  /// Category.
  typedef std::forward_iterator_tag iterator_category;

  /// Difference type.
  typedef typename _Alloc::difference_type difference_type;

  /// Iterator's value type.
  typedef value_type_ value_type;

  /// Iterator's pointer type.
  typedef pointer_ pointer;

  /// Iterator's const pointer type.
  typedef const_pointer_ const_pointer;

  /// Iterator's reference type.
  typedef reference_ reference;

  /// Iterator's const reference type.
  typedef const_reference_ const_reference;

  /// Default constructor.
  inline
  iterator_()
  : const_iterator_(0, PB_DS_GEN_POS(), 0) { }

  /// Conversion to a point-type iterator.
  inline
  operator point_iterator_()
  { return point_iterator_(const_cast<pointer>(const_iterator_::m_p_value)); }

  /// Conversion to a point-type iterator.
  inline
  operator const point_iterator_() const
  { return point_iterator_(const_cast<pointer>(const_iterator_::m_p_value)); }

  /// Access.
  pointer
  operator->() const
  {
    _GLIBCXX_DEBUG_ASSERT(base_type::m_p_value != 0);
    return (const_cast<pointer>(base_type::m_p_value));
  }

  /// Access.
  reference
  operator*() const
  {
    _GLIBCXX_DEBUG_ASSERT(base_type::m_p_value != 0);
    return (const_cast<reference>(*base_type::m_p_value));
  }

  /// Increments.
  iterator_&
  operator++()
  {
    base_type::m_p_tbl->inc_it_state(base_type::m_p_value, base_type::m_pos);
    return *this;
  }

  /// Increments.
  iterator_
  operator++(int)
  {
    iterator_ ret =* this;
    base_type::m_p_tbl->inc_it_state(base_type::m_p_value, base_type::m_pos);
    return ret;
  }

protected:
  typedef const_iterator_ base_type;

  /**
   *  Constructor used by the table to initiate the generalized
   *      pointer and position (e.g., this is called from within a find()
   *      of a table.
   * */
  inline
  iterator_(pointer p_value, PB_DS_GEN_POS pos, PB_DS_CLASS_C_DEC* p_tbl)
  : const_iterator_(p_value, pos, p_tbl)
  { }

  friend class PB_DS_CLASS_C_DEC;
};
#endif
