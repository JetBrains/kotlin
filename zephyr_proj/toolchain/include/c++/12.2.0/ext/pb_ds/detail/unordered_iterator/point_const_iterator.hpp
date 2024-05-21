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
 * @file unordered_iterator/point_const_iterator.hpp
 * Contains an iterator class returned by the tables' const find and insert
 * methods.
 *
 * * This file is intended to be included inside a class definition, with
 * PB_DS_CLASS_C_DEC defined to the name of the enclosing class.
 */

#ifdef PB_DS_CLASS_C_DEC
class point_iterator_;

/// Const point-type iterator.
class point_const_iterator_
{
public:
  /// Category.
  typedef trivial_iterator_tag iterator_category;

  /// Difference type.
  typedef trivial_iterator_difference_type difference_type;

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

  inline
  point_const_iterator_(const_pointer p_value) : m_p_value(p_value)
  { }

  /// Default constructor.
  inline
  point_const_iterator_() : m_p_value(0)
  { }

  /// Copy constructor.
  inline
  point_const_iterator_(const point_const_iterator_& other)
  : m_p_value(other.m_p_value)
  { }

  /// Copy constructor.
  inline
  point_const_iterator_(const point_iterator_& other)
  : m_p_value(other.m_p_value)
  { }

  /// Access.
  const_pointer
  operator->() const
  {
    _GLIBCXX_DEBUG_ASSERT(m_p_value != 0);
    return m_p_value;
  }

  /// Access.
  const_reference
  operator*() const
  {
    _GLIBCXX_DEBUG_ASSERT(m_p_value != 0);
    return *m_p_value;
  }

  /// Compares content to a different iterator object.
  bool
  operator==(const point_iterator_& other) const
  { return m_p_value == other.m_p_value; }

  /// Compares content to a different iterator object.
  bool
  operator==(const point_const_iterator_& other) const
  { return m_p_value == other.m_p_value; }

  /// Compares content (negatively) to a different iterator object.
  bool
  operator!=(const point_iterator_& other) const
  { return m_p_value != other.m_p_value; }

  /// Compares content (negatively) to a different iterator object.
  bool
  operator!=(const point_const_iterator_& other) const
  { return m_p_value != other.m_p_value; }

protected:
  const_pointer m_p_value;

  friend class point_iterator_;

  friend class PB_DS_CLASS_C_DEC;
};
#endif
