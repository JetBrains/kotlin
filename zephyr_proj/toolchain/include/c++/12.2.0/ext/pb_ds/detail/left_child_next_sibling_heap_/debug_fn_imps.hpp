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
 * @file left_child_next_sibling_heap_/debug_fn_imps.hpp
 * Contains an implementation class for left_child_next_sibling_heap_.
 */

#ifdef PB_DS_CLASS_C_DEC

#ifdef _GLIBCXX_DEBUG

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_valid(const char* __file, int __line) const
{
  PB_DS_DEBUG_VERIFY(m_p_root == 0 || m_p_root->m_p_prev_or_parent == 0);

  if (m_p_root != 0)
    assert_node_consistent(m_p_root, Single_Link_Roots, __file, __line);
  assert_size(__file, __line);
  assert_iterators(__file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_node_consistent(node_const_pointer p_nd, bool single_link,
		       const char* __file, int __line) const
{
  if (p_nd == 0)
    return;

  assert_node_consistent(p_nd->m_p_l_child, false, __file, __line);
  assert_node_consistent(p_nd->m_p_next_sibling, single_link, __file, __line);

  if (single_link)
    PB_DS_DEBUG_VERIFY(p_nd->m_p_prev_or_parent == 0);
  else if (p_nd->m_p_next_sibling != 0)
    PB_DS_DEBUG_VERIFY(p_nd->m_p_next_sibling->m_p_prev_or_parent == p_nd);

  if (p_nd->m_p_l_child == 0)
    return;

  node_const_pointer p_child = p_nd->m_p_l_child;
  while (p_child != 0)
    {
      node_const_pointer p_next_child = p_child->m_p_next_sibling;
      PB_DS_DEBUG_VERIFY(!Cmp_Fn::operator()(p_nd->m_value, p_child->m_value));
      p_child = p_next_child;
    }
  PB_DS_DEBUG_VERIFY(p_nd->m_p_l_child->m_p_prev_or_parent == p_nd);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_iterators(const char* __file, int __line) const
{
  PB_DS_DEBUG_VERIFY(std::distance(begin(), end()) == size());
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_size(const char* __file, int __line) const
{
  PB_DS_DEBUG_VERIFY(size_from_node(m_p_root) == m_size);
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
size_under_node(node_const_pointer p_nd)
{ return 1 + size_from_node(p_nd->m_p_l_child); }

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
size_from_node(node_const_pointer p_nd)
{
  size_type ret = 0;
  while (p_nd != 0)
    {
      ret += 1 + size_from_node(p_nd->m_p_l_child);
      p_nd = p_nd->m_p_next_sibling;
    }
  return ret;
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
degree(node_const_pointer p_nd)
{
  size_type ret = 0;
  node_const_pointer p_child = p_nd->m_p_l_child;
  while (p_child != 0)
    {
      ++ret;
      p_child = p_child->m_p_next_sibling;
    }
  return ret;
}

#endif 
#endif
