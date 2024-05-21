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
 * @file pairing_heap_/insert_fn_imps.hpp
 * Contains an implementation class for a pairing heap.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_iterator
PB_DS_CLASS_C_DEC::
push(const_reference r_val)
{
  PB_DS_ASSERT_VALID((*this))
  node_pointer p_new_nd = base_type::get_new_node_for_insert(r_val);
  push_imp(p_new_nd);
  PB_DS_ASSERT_VALID((*this))
  return point_iterator(p_new_nd);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
push_imp(node_pointer p_nd)
{
  p_nd->m_p_l_child = 0;
  if (base_type::m_p_root == 0)
    {
      p_nd->m_p_next_sibling = p_nd->m_p_prev_or_parent = 0;
      base_type::m_p_root = p_nd;
    }
  else if (Cmp_Fn::operator()(base_type::m_p_root->m_value, p_nd->m_value))
    {
      p_nd->m_p_next_sibling = p_nd->m_p_prev_or_parent = 0;
      base_type::make_child_of(base_type::m_p_root, p_nd);
      PB_DS_ASSERT_NODE_CONSISTENT(p_nd, false)
      base_type::m_p_root = p_nd;
    }
  else
    {
      base_type::make_child_of(p_nd, base_type::m_p_root);
      PB_DS_ASSERT_NODE_CONSISTENT(base_type::m_p_root, false)
    }
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
modify(point_iterator it, const_reference r_new_val)
{
  PB_DS_ASSERT_VALID((*this))
  remove_node(it.m_p_nd);
  it.m_p_nd->m_value = r_new_val;
  push_imp(it.m_p_nd);
  PB_DS_ASSERT_VALID((*this))
}
#endif
