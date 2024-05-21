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
 * @file pat_trie_/r_erase_fn_imps.hpp
 * Contains an implementation class for pat_trie.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
actual_erase_node(node_pointer p_z)
{
  _GLIBCXX_DEBUG_ASSERT(m_size > 0);
  --m_size;
  _GLIBCXX_DEBUG_ONLY(debug_base::erase_existing(PB_DS_V2F(p_z->m_value)));
  p_z->~node();
  s_node_allocator.deallocate(p_z, 1);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
update_min_max_for_erased_node(node_pointer p_z)
{
  if (m_size == 1)
    {
      m_p_head->m_p_left = m_p_head->m_p_right = m_p_head;
      return;
    }

  if (m_p_head->m_p_left == p_z)
    {
      iterator it(p_z);
      ++it;
      m_p_head->m_p_left = it.m_p_nd;
    }
  else if (m_p_head->m_p_right == p_z)
    {
      iterator it(p_z);
      --it;
      m_p_head->m_p_right = it.m_p_nd;
    }
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
clear()
{
  _GLIBCXX_DEBUG_ONLY(assert_valid(true, true);)
  clear_imp(m_p_head->m_p_parent);
  m_size = 0;
  initialize();
  _GLIBCXX_DEBUG_ONLY(debug_base::clear();)
  _GLIBCXX_DEBUG_ONLY(assert_valid(true, true);)
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
clear_imp(node_pointer p_nd)
{
  if (p_nd == 0)
    return;
  clear_imp(p_nd->m_p_left);
  clear_imp(p_nd->m_p_right);
  p_nd->~Node();
  s_node_allocator.deallocate(p_nd, 1);
}

#endif
