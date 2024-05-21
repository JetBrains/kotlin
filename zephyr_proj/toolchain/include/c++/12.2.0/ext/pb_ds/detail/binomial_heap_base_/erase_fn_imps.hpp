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
 * @file binomial_heap_base_/erase_fn_imps.hpp
 * Contains an implementation class for a base of binomial heaps.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
pop()
{
  PB_DS_ASSERT_VALID_COND((*this),true)
  _GLIBCXX_DEBUG_ASSERT(!base_type::empty());

  if (m_p_max == 0)
    find_max();

  _GLIBCXX_DEBUG_ASSERT(m_p_max != 0);
  node_pointer p_nd = m_p_max;
  remove_parentless_node(m_p_max);
  base_type::actual_erase_node(p_nd);
  m_p_max = 0;
  PB_DS_ASSERT_VALID_COND((*this),true)
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
remove_parentless_node(node_pointer p_nd)
{
  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);
  _GLIBCXX_DEBUG_ASSERT(base_type::parent(p_nd) == 0);

  node_pointer p_cur_root = p_nd == base_type::m_p_root?
    p_nd->m_p_next_sibling : base_type::m_p_root;

  if (p_cur_root != 0)
    p_cur_root->m_p_prev_or_parent = 0;

  if (p_nd->m_p_prev_or_parent != 0)
    p_nd->m_p_prev_or_parent->m_p_next_sibling = p_nd->m_p_next_sibling;

  if (p_nd->m_p_next_sibling != 0)
    p_nd->m_p_next_sibling->m_p_prev_or_parent = p_nd->m_p_prev_or_parent;

  node_pointer p_child = p_nd->m_p_l_child;
  if (p_child != 0)
    {
      p_child->m_p_prev_or_parent = 0;
      while (p_child->m_p_next_sibling != 0)
	p_child = p_child->m_p_next_sibling;
    }

  m_p_max = 0;
  base_type::m_p_root = join(p_cur_root, p_child);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
clear()
{
  base_type::clear();
  m_p_max = 0;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
erase(point_iterator it)
{
  PB_DS_ASSERT_VALID_COND((*this),true)
  _GLIBCXX_DEBUG_ASSERT(!base_type::empty());

  base_type::bubble_to_top(it.m_p_nd);
  remove_parentless_node(it.m_p_nd);
  base_type::actual_erase_node(it.m_p_nd);
  m_p_max = 0;
  PB_DS_ASSERT_VALID_COND((*this),true)
}

PB_DS_CLASS_T_DEC
template<typename Pred>
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
erase_if(Pred pred)
{
  PB_DS_ASSERT_VALID_COND((*this),true)

  if (base_type::empty())
    {
      PB_DS_ASSERT_VALID_COND((*this),true)
      return 0;
    }

  base_type::to_linked_list();
  node_pointer p_out = base_type::prune(pred);
  size_type ersd = 0;
  while (p_out != 0)
    {
      ++ersd;
      node_pointer p_next = p_out->m_p_next_sibling;
      base_type::actual_erase_node(p_out);
      p_out = p_next;
    }

  node_pointer p_cur = base_type::m_p_root;
  base_type::m_p_root = 0;
  while (p_cur != 0)
    {
      node_pointer p_next = p_cur->m_p_next_sibling;
      p_cur->m_p_l_child = p_cur->m_p_prev_or_parent = 0;
      p_cur->m_metadata = 0;
      p_cur->m_p_next_sibling = base_type::m_p_root;

      if (base_type::m_p_root != 0)
	base_type::m_p_root->m_p_prev_or_parent = p_cur;

      base_type::m_p_root = p_cur;
      base_type::m_p_root = fix(base_type::m_p_root);
      p_cur = p_next;
    }

  m_p_max = 0;
  PB_DS_ASSERT_VALID_COND((*this),true)
  return ersd;
}
#endif
