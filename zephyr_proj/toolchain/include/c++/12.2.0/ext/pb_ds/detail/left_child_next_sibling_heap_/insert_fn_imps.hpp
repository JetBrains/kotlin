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
 * @file left_child_next_sibling_heap_/insert_fn_imps.hpp
 * Contains an implementation class for left_child_next_sibling_heap_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
get_new_node_for_insert(const_reference r_val)
{
  return get_new_node_for_insert(r_val, s_no_throw_copies_ind);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
get_new_node_for_insert(const_reference r_val, false_type)
{
  node_pointer p_new_nd = s_node_allocator.allocate(1);

  cond_dealtor_t cond(p_new_nd);

  new (const_cast<void* >(
			  static_cast<const void* >(&p_new_nd->m_value)))
    typename node::value_type(r_val);

  cond.set_no_action();

  ++m_size;

  return (p_new_nd);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
get_new_node_for_insert(const_reference r_val, true_type)
{
  node_pointer p_new_nd = s_node_allocator.allocate(1);

  new (const_cast<void* >(
			  static_cast<const void* >(&p_new_nd->m_value)))
    typename node::value_type(r_val);

  ++m_size;

  return (p_new_nd);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
make_child_of(node_pointer p_nd, node_pointer p_new_parent)
{
  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);
  _GLIBCXX_DEBUG_ASSERT(p_new_parent != 0);

  p_nd->m_p_next_sibling = p_new_parent->m_p_l_child;

  if (p_new_parent->m_p_l_child != 0)
    p_new_parent->m_p_l_child->m_p_prev_or_parent = p_nd;

  p_nd->m_p_prev_or_parent = p_new_parent;

  p_new_parent->m_p_l_child = p_nd;
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
parent(node_pointer p_nd)
{
  while (true)
    {
      node_pointer p_pot = p_nd->m_p_prev_or_parent;

      if (p_pot == 0 || p_pot->m_p_l_child == p_nd)
	return p_pot;

      p_nd = p_pot;
    }
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
swap_with_parent(node_pointer p_nd, node_pointer p_parent)
{
  if (p_parent == m_p_root)
    m_p_root = p_nd;

  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);
  _GLIBCXX_DEBUG_ASSERT(p_parent != 0);
  _GLIBCXX_DEBUG_ASSERT(parent(p_nd) == p_parent);

  const bool nd_direct_child = p_parent->m_p_l_child == p_nd;
  const bool parent_root = p_parent->m_p_prev_or_parent == 0;
  const bool parent_direct_child =
    !parent_root&&  p_parent->m_p_prev_or_parent->m_p_l_child == p_parent;

  std::swap(p_parent->m_p_prev_or_parent, p_nd->m_p_prev_or_parent);
  std::swap(p_parent->m_p_next_sibling, p_nd->m_p_next_sibling);
  std::swap(p_parent->m_p_l_child, p_nd->m_p_l_child);
  std::swap(p_parent->m_metadata, p_nd->m_metadata);

  _GLIBCXX_DEBUG_ASSERT(p_nd->m_p_l_child != 0);
  _GLIBCXX_DEBUG_ASSERT(p_parent->m_p_prev_or_parent != 0);

  if (p_nd->m_p_next_sibling != 0)
    p_nd->m_p_next_sibling->m_p_prev_or_parent = p_nd;

  if (p_parent->m_p_next_sibling != 0)
    p_parent->m_p_next_sibling->m_p_prev_or_parent = p_parent;

  if (p_parent->m_p_l_child != 0)
    p_parent->m_p_l_child->m_p_prev_or_parent = p_parent;

  if (parent_direct_child)
    p_nd->m_p_prev_or_parent->m_p_l_child = p_nd;
  else if (!parent_root)
    p_nd->m_p_prev_or_parent->m_p_next_sibling = p_nd;

  if (!nd_direct_child)
    {
      p_nd->m_p_l_child->m_p_prev_or_parent = p_nd;

      p_parent->m_p_prev_or_parent->m_p_next_sibling = p_parent;
    }
  else
    {
      _GLIBCXX_DEBUG_ASSERT(p_nd->m_p_l_child == p_nd);
      _GLIBCXX_DEBUG_ASSERT(p_parent->m_p_prev_or_parent == p_parent);

      p_nd->m_p_l_child = p_parent;
      p_parent->m_p_prev_or_parent = p_nd;
    }

  _GLIBCXX_DEBUG_ASSERT(parent(p_parent) == p_nd);
}

#endif
