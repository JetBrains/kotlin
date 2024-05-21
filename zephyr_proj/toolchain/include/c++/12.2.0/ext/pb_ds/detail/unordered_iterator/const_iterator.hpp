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
 * @file unordered_iterator/const_iterator.hpp
 * Contains an iterator class used for const ranging over the elements of the
 * table.
 *
 * This file is intended to be included inside a class definition, with
 * PB_DS_CLASS_C_DEC defined to the name of the enclosing class.
 */

#ifdef PB_DS_CLASS_C_DEC
/// Const range-type iterator.
class const_iterator_
: public point_const_iterator_
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
  const_iterator_() : m_p_tbl(0)
  { }

  /// Increments.
  const_iterator_&
  operator++()
  {
    m_p_tbl->inc_it_state(base_type::m_p_value, m_pos);
    return *this;
  }

  /// Increments.
  const_iterator_
  operator++(int)
  {
    const_iterator_ ret =* this;
    m_p_tbl->inc_it_state(base_type::m_p_value, m_pos);
    return ret;
  }

protected:
  typedef point_const_iterator_ base_type;

  /**
   *  Constructor used by the table to initiate the generalized
   *      pointer and position (e.g., this is called from within a find()
   *      of a table.
   * */
  const_iterator_(const_pointer_ p_value,  PB_DS_GEN_POS pos,
		  const PB_DS_CLASS_C_DEC* p_tbl)
  : point_const_iterator_(p_value), m_p_tbl(p_tbl), m_pos(pos)
  { }

  /**
   *  Pointer to the table object which created the iterator (used for
   *      incrementing its position.
   * */
  const PB_DS_CLASS_C_DEC* m_p_tbl;

  PB_DS_GEN_POS m_pos;

  friend class PB_DS_CLASS_C_DEC;
};
#endif
