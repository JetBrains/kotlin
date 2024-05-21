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
 * @file bin_search_tree_/find_fn_imps.hpp
 * Contains an implementation class for bin_search_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_const_iterator
PB_DS_CLASS_C_DEC::
lower_bound(key_const_reference r_key) const
{
  node_pointer p_pot = m_p_head;
  node_pointer p_nd = m_p_head->m_p_parent;

  while (p_nd != 0)
    if (Cmp_Fn::operator()(PB_DS_V2F(p_nd->m_value), r_key))
      p_nd = p_nd->m_p_right;
    else
      {
	p_pot = p_nd;
	p_nd = p_nd->m_p_left;
      }
  return iterator(p_pot);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_iterator
PB_DS_CLASS_C_DEC::
lower_bound(key_const_reference r_key)
{
  node_pointer p_pot = m_p_head;
  node_pointer p_nd = m_p_head->m_p_parent;

  while (p_nd != 0)
    if (Cmp_Fn::operator()(PB_DS_V2F(p_nd->m_value), r_key))
      p_nd = p_nd->m_p_right;
    else
      {
	p_pot = p_nd;
	p_nd = p_nd->m_p_left;
      }
  return iterator(p_pot);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_const_iterator
PB_DS_CLASS_C_DEC::
upper_bound(key_const_reference r_key) const
{
  node_pointer p_pot = m_p_head;
  node_pointer p_nd = m_p_head->m_p_parent;

  while (p_nd != 0)
    if (Cmp_Fn::operator()(r_key, PB_DS_V2F(p_nd->m_value)))
      {
	p_pot = p_nd;
	p_nd = p_nd->m_p_left;
      }
    else
      p_nd = p_nd->m_p_right;
  return const_iterator(p_pot);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_iterator
PB_DS_CLASS_C_DEC::
upper_bound(key_const_reference r_key)
{
  node_pointer p_pot = m_p_head;
  node_pointer p_nd = m_p_head->m_p_parent;

  while (p_nd != 0)
    if (Cmp_Fn::operator()(r_key, PB_DS_V2F(p_nd->m_value)))
      {
	p_pot = p_nd;
	p_nd = p_nd->m_p_left;
      }
    else
      p_nd = p_nd->m_p_right;
  return point_iterator(p_pot);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_iterator
PB_DS_CLASS_C_DEC::
find(key_const_reference r_key)
{
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
  node_pointer p_pot = m_p_head;
  node_pointer p_nd = m_p_head->m_p_parent;

  while (p_nd != 0)
    if (!Cmp_Fn::operator()(PB_DS_V2F(p_nd->m_value), r_key))
      {
	p_pot = p_nd;
	p_nd = p_nd->m_p_left;
      }
    else
      p_nd = p_nd->m_p_right;

  node_pointer ret = p_pot;
  if (p_pot != m_p_head)
    {
      const bool __cmp = Cmp_Fn::operator()(r_key, PB_DS_V2F(p_pot->m_value));
      if (__cmp)
	ret = m_p_head;
    }
  return point_iterator(ret);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_const_iterator
PB_DS_CLASS_C_DEC::
find(key_const_reference r_key) const
{
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
  node_pointer p_pot = m_p_head;
  node_pointer p_nd = m_p_head->m_p_parent;

  while (p_nd != 0)
    if (!Cmp_Fn::operator()(PB_DS_V2F(p_nd->m_value), r_key))
      {
	p_pot = p_nd;
	p_nd = p_nd->m_p_left;
      }
    else
      p_nd = p_nd->m_p_right;

  node_pointer ret = p_pot;
  if (p_pot != m_p_head)
    {
      const bool __cmp = Cmp_Fn::operator()(r_key, PB_DS_V2F(p_pot->m_value));
      if (__cmp)
	ret = m_p_head;
    }
  return point_const_iterator(ret);
}
#endif
