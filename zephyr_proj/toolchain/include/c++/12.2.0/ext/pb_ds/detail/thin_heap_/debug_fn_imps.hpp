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
 * @file thin_heap_/debug_fn_imps.hpp
 * Contains an implementation for thin_heap_.
 */

#ifdef PB_DS_CLASS_C_DEC

#ifdef _GLIBCXX_DEBUG

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_valid(const char* __file, int __line) const
{
  base_type::assert_valid(__file, __line);
  assert_node_consistent(base_type::m_p_root, true, __file, __line);
  assert_max(__file, __line);
  assert_aux_null(__file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_aux_null(const char* __file, int __line) const
{
  for (size_type i = 0; i < max_rank; ++i)
    PB_DS_DEBUG_VERIFY(m_a_aux[i] == 0);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_max(const char* __file, int __line) const
{
  if (m_p_max == 0)
    {
      PB_DS_DEBUG_VERIFY(base_type::empty());
      return;
    }

  PB_DS_DEBUG_VERIFY(!base_type::empty());
  PB_DS_DEBUG_VERIFY(base_type::parent(m_p_max) == 0);
  PB_DS_DEBUG_VERIFY(m_p_max->m_p_prev_or_parent == 0);
  for (const_iterator it = base_type::begin(); it != base_type::end(); ++it)
    PB_DS_DEBUG_VERIFY(!Cmp_Fn::operator()(m_p_max->m_value, it.m_p_nd->m_value));
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_node_consistent(node_const_pointer p_nd, bool root,
		       const char* __file, int __line) const
{
  base_type::assert_node_consistent(p_nd, root, __file, __line);
  if (p_nd == 0)
    return;

  assert_node_consistent(p_nd->m_p_next_sibling, root, __file, __line);
  assert_node_consistent(p_nd->m_p_l_child, false, __file, __line);
  if (!root)
    {
      if (p_nd->m_metadata == 0)
	PB_DS_DEBUG_VERIFY(p_nd->m_p_next_sibling == 0);
      else
	PB_DS_DEBUG_VERIFY(p_nd->m_metadata == p_nd->m_p_next_sibling->m_metadata + 1);
    }

  if (p_nd->m_p_l_child != 0)
    PB_DS_DEBUG_VERIFY(p_nd->m_p_l_child->m_metadata + 1 == base_type::degree(p_nd));

  const bool unmarked_valid =
    (p_nd->m_p_l_child == 0 && p_nd->m_metadata == 0)
    || (p_nd->m_p_l_child != 0
	 && p_nd->m_metadata == p_nd->m_p_l_child->m_metadata + 1);

  const bool marked_valid =
    (p_nd->m_p_l_child == 0 && p_nd->m_metadata == 1)
    || (p_nd->m_p_l_child != 0
	&& p_nd->m_metadata == p_nd->m_p_l_child->m_metadata + 2);

  PB_DS_DEBUG_VERIFY(unmarked_valid || marked_valid);
  if (root)
    PB_DS_DEBUG_VERIFY(unmarked_valid);
}

#endif 
#endif
