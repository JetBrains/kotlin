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
 * @file rc_binomial_heap_/debug_fn_imps.hpp
 * Contains an implementation for rc_binomial_heap_.
 */

#ifdef PB_DS_CLASS_C_DEC

#ifdef _GLIBCXX_DEBUG

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_valid(const char* __file, int __line) const
{
  base_type::assert_valid(false, __file, __line);
  if (!base_type::empty())
    {
      PB_DS_DEBUG_VERIFY(base_type::m_p_max != 0);
      base_type::assert_max(__file, __line);
    }

  m_rc.assert_valid(__file, __line);

  if (m_rc.empty())
    {
      base_type::assert_valid(true, __file, __line);
      PB_DS_DEBUG_VERIFY(next_2_pointer(base_type::m_p_root) == 0);
      return;
    }

  node_const_pointer p_nd = next_2_pointer(base_type::m_p_root);
  typename rc_t::const_iterator it = m_rc.end();
  --it;

  while (p_nd != 0)
    {
      PB_DS_DEBUG_VERIFY(*it == p_nd);
      node_const_pointer p_next = p_nd->m_p_next_sibling;
      PB_DS_DEBUG_VERIFY(p_next != 0);
      PB_DS_DEBUG_VERIFY(p_nd->m_metadata == p_next->m_metadata);
      PB_DS_DEBUG_VERIFY(p_next->m_p_next_sibling == 0 ||
		       p_next->m_metadata < p_next->m_p_next_sibling->m_metadata);

      --it;
      p_nd = next_2_pointer(next_after_0_pointer(p_nd));
    }
  PB_DS_DEBUG_VERIFY(it + 1 == m_rc.begin());
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_const_pointer
PB_DS_CLASS_C_DEC::
next_2_pointer(node_const_pointer p_nd)
{
  if (p_nd == 0)
    return 0;

  node_pointer p_next = p_nd->m_p_next_sibling;

  if (p_next == 0)
    return 0;

  if (p_nd->m_metadata == p_next->m_metadata)
    return p_nd;

  return next_2_pointer(p_next);
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_const_pointer
PB_DS_CLASS_C_DEC::
next_after_0_pointer(node_const_pointer p_nd)
{
  if (p_nd == 0)
    return 0;

  node_pointer p_next = p_nd->m_p_next_sibling;

  if (p_next == 0)
    return 0;

  if (p_nd->m_metadata < p_next->m_metadata)
    return p_next;

  return next_after_0_pointer(p_next);
}

#endif 
#endif
