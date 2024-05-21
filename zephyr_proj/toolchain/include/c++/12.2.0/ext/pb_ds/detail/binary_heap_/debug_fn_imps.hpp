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
 * @file binary_heap_/debug_fn_imps.hpp
 * Contains an implementation class for a binary_heap.
 */

#ifdef PB_DS_CLASS_C_DEC

#ifdef _GLIBCXX_DEBUG

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_valid(const char* __file, int __line) const
{
#ifdef PB_DS_REGRESSION
  s_entry_allocator.check_allocated(m_a_entries, m_actual_size);
#endif

  resize_policy::assert_valid(__file, __line);
  PB_DS_DEBUG_VERIFY(m_size <= m_actual_size);
  for (size_type i = 0; i < m_size; ++i)
    {
#ifdef PB_DS_REGRESSION
      s_value_allocator.check_allocated(m_a_entries[i], 1);
#endif

      if (left_child(i) < m_size)
	PB_DS_DEBUG_VERIFY(!entry_cmp::operator()(m_a_entries[i], m_a_entries[left_child(i)]));

      PB_DS_DEBUG_VERIFY(parent(left_child(i)) == i);

      if (right_child(i) < m_size)
	PB_DS_DEBUG_VERIFY(!entry_cmp::operator()(m_a_entries[i], m_a_entries[right_child(i)]));

      PB_DS_DEBUG_VERIFY(parent(right_child(i)) == i);
    }
}

#endif
#endif
