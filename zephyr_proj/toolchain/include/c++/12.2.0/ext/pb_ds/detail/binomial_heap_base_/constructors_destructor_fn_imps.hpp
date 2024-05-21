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
 * @file binomial_heap_base_/constructors_destructor_fn_imps.hpp
 * Contains an implementation class for a base of binomial heaps.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
template<typename It>
void
PB_DS_CLASS_C_DEC::
copy_from_range(It first_it, It last_it)
{
  while (first_it != last_it)
    push(*(first_it++));
  PB_DS_ASSERT_VALID_COND((*this),false)
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
binomial_heap_base() : m_p_max(0)
{
  PB_DS_ASSERT_VALID_COND((*this),false)
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
binomial_heap_base(const Cmp_Fn& r_cmp_fn)
: base_type(r_cmp_fn), m_p_max(0)
{ PB_DS_ASSERT_VALID_COND((*this),false) }

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
binomial_heap_base(const PB_DS_CLASS_C_DEC& other)
: base_type(other), m_p_max(0)
{ PB_DS_ASSERT_VALID_COND((*this),false) }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID_COND((*this),false)
  base_type::swap(other);
  std::swap(m_p_max, other.m_p_max);
  PB_DS_ASSERT_VALID_COND((*this),false)
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~binomial_heap_base()
{ }
#endif
