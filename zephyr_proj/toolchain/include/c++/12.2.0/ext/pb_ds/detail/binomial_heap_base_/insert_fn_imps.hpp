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
 * @file binomial_heap_base_/insert_fn_imps.hpp
 * Contains an implementation class for a base of binomial heaps.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_iterator
PB_DS_CLASS_C_DEC::
push(const_reference r_val)
{
  PB_DS_ASSERT_VALID_COND((*this),true)
  node_pointer p_nd = base_type::get_new_node_for_insert(r_val);
  insert_node(p_nd);
  m_p_max = 0;
  PB_DS_ASSERT_VALID_COND((*this),true)
  return point_iterator(p_nd);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
insert_node(node_pointer p_nd)
{
  if (base_type::m_p_root == 0)
    {
      p_nd->m_p_next_sibling = 0;
      p_nd->m_p_prev_or_parent = 0;
      p_nd->m_p_l_child = 0;
      p_nd->m_metadata = 0;
      base_type::m_p_root = p_nd;
      return;
    }

  if (base_type::m_p_root->m_metadata > 0)
    {
      p_nd->m_p_prev_or_parent = p_nd->m_p_l_child = 0;
      p_nd->m_p_next_sibling = base_type::m_p_root;
      base_type::m_p_root->m_p_prev_or_parent = p_nd;
      base_type::m_p_root = p_nd;
      p_nd->m_metadata = 0;
      return;
    }

  if (Cmp_Fn::operator()(base_type::m_p_root->m_value, p_nd->m_value))
    {
      p_nd->m_p_next_sibling = base_type::m_p_root->m_p_next_sibling;
      p_nd->m_p_prev_or_parent = 0;
      p_nd->m_metadata = 1;
      p_nd->m_p_l_child = base_type::m_p_root;
      base_type::m_p_root->m_p_prev_or_parent = p_nd;
      base_type::m_p_root->m_p_next_sibling = 0;
      base_type::m_p_root = p_nd;
    }
  else
    {
      p_nd->m_p_next_sibling = 0;
      p_nd->m_p_l_child = 0;
      p_nd->m_p_prev_or_parent = base_type::m_p_root;
      p_nd->m_metadata = 0;
      _GLIBCXX_DEBUG_ASSERT(base_type::m_p_root->m_p_l_child == 0);
      base_type::m_p_root->m_p_l_child = p_nd;
      base_type::m_p_root->m_metadata = 1;
    }

  base_type::m_p_root = fix(base_type::m_p_root);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
fix(node_pointer p_nd) const
{
  while (p_nd->m_p_next_sibling != 0 &&
	 p_nd->m_metadata == p_nd->m_p_next_sibling->m_metadata)
    {
      node_pointer p_next = p_nd->m_p_next_sibling;
      if (Cmp_Fn::operator()(p_nd->m_value, p_next->m_value))
	{
	  p_next->m_p_prev_or_parent = p_nd->m_p_prev_or_parent;

	  if (p_nd->m_p_prev_or_parent != 0)
	    p_nd->m_p_prev_or_parent->m_p_next_sibling = p_next;

	  base_type::make_child_of(p_nd, p_next);
	  ++p_next->m_metadata;
	  p_nd = p_next;
	}
      else
	{
	  p_nd->m_p_next_sibling = p_next->m_p_next_sibling;

	  if (p_nd->m_p_next_sibling != 0)
	    p_next->m_p_next_sibling = 0;

	  base_type::make_child_of(p_next, p_nd);
	  ++p_nd->m_metadata;
	}
    }

  if (p_nd->m_p_next_sibling != 0)
    p_nd->m_p_next_sibling->m_p_prev_or_parent = p_nd;

  return p_nd;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
modify(point_iterator it, const_reference r_new_val)
{
  PB_DS_ASSERT_VALID_COND((*this),true)
  node_pointer p_nd = it.m_p_nd;

  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);
  PB_DS_ASSERT_BASE_NODE_CONSISTENT(p_nd, false)

  const bool bubble_up = Cmp_Fn::operator()(p_nd->m_value, r_new_val);
  p_nd->m_value = r_new_val;

  if (bubble_up)
    {
      node_pointer p_parent = base_type::parent(p_nd);
      while (p_parent != 0 &&
	     Cmp_Fn::operator()(p_parent->m_value, p_nd->m_value))
	{
	  base_type::swap_with_parent(p_nd, p_parent);
	  p_parent = base_type::parent(p_nd);
	}

      if (p_nd->m_p_prev_or_parent == 0)
	base_type::m_p_root = p_nd;

      m_p_max = 0;
      PB_DS_ASSERT_VALID_COND((*this),true)
      return;
    }

  base_type::bubble_to_top(p_nd);
  remove_parentless_node(p_nd);
  insert_node(p_nd);
  m_p_max = 0;
  PB_DS_ASSERT_VALID_COND((*this),true)
}
#endif
