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
 * @file rb_tree_map_/debug_fn_imps.hpp
 * Contains an implementation for rb_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

#ifdef _GLIBCXX_DEBUG

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
assert_node_consistent(const node_pointer p_nd, const char* __file,
						int __line) const
{
  if (p_nd == 0)
    return 1;

  const size_type l_height =
    assert_node_consistent(p_nd->m_p_left, __file, __line);
  const size_type r_height =
    assert_node_consistent(p_nd->m_p_right, __file, __line);
  if (p_nd->m_red)
    {
      PB_DS_DEBUG_VERIFY(is_effectively_black(p_nd->m_p_left));
      PB_DS_DEBUG_VERIFY(is_effectively_black(p_nd->m_p_right));
    }
  PB_DS_DEBUG_VERIFY(l_height == r_height);
  return (p_nd->m_red ? 0 : 1) + l_height;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_valid(const char* __file, int __line) const
{
  base_type::assert_valid(__file, __line);
  const node_pointer p_head = base_type::m_p_head;
  PB_DS_DEBUG_VERIFY(p_head->m_red);
  if (p_head->m_p_parent != 0)
    {
      PB_DS_DEBUG_VERIFY(!p_head->m_p_parent->m_red);
      assert_node_consistent(p_head->m_p_parent, __file, __line);
    }
}

#endif 

#endif
