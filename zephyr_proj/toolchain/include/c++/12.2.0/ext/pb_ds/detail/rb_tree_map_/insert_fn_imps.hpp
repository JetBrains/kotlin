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
 * @file rb_tree_map_/insert_fn_imps.hpp
 * Contains an implementation for rb_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline std::pair<typename PB_DS_CLASS_C_DEC::point_iterator, bool>
PB_DS_CLASS_C_DEC::
insert(const_reference r_value)
{
  PB_DS_ASSERT_VALID((*this))
  std::pair<point_iterator, bool> ins_pair = base_type::insert_leaf(r_value);
  if (ins_pair.second == true)
    {
      ins_pair.first.m_p_nd->m_red = true;
      PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
      insert_fixup(ins_pair.first.m_p_nd);
    }

  PB_DS_ASSERT_VALID((*this))
  return ins_pair;
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
insert_fixup(node_pointer p_nd)
{
  _GLIBCXX_DEBUG_ASSERT(p_nd->m_red == true);
  while (p_nd != base_type::m_p_head->m_p_parent && p_nd->m_p_parent->m_red)
    {
      if (p_nd->m_p_parent == p_nd->m_p_parent->m_p_parent->m_p_left)
        {
	  node_pointer p_y = p_nd->m_p_parent->m_p_parent->m_p_right;
	  if (p_y != 0 && p_y->m_red)
            {
	      p_nd->m_p_parent->m_red = false;
	      p_y->m_red = false;
	      p_nd->m_p_parent->m_p_parent->m_red = true;
	      p_nd = p_nd->m_p_parent->m_p_parent;
            }
	  else
            {
	      if (p_nd == p_nd->m_p_parent->m_p_right)
                {
		  p_nd = p_nd->m_p_parent;
		  base_type::rotate_left(p_nd);
                }
	      p_nd->m_p_parent->m_red = false;
	      p_nd->m_p_parent->m_p_parent->m_red = true;
	      base_type::rotate_right(p_nd->m_p_parent->m_p_parent);
            }
        }
      else
        {
	  node_pointer p_y = p_nd->m_p_parent->m_p_parent->m_p_left;
	  if (p_y != 0 && p_y->m_red)
            {
	      p_nd->m_p_parent->m_red = false;
	      p_y->m_red = false;
	      p_nd->m_p_parent->m_p_parent->m_red = true;
	      p_nd = p_nd->m_p_parent->m_p_parent;
            }
	  else
            {
	      if (p_nd == p_nd->m_p_parent->m_p_left)
                {
		  p_nd = p_nd->m_p_parent;
		  base_type::rotate_right(p_nd);
                }
	      p_nd->m_p_parent->m_red = false;
	      p_nd->m_p_parent->m_p_parent->m_red = true;
	      base_type::rotate_left(p_nd->m_p_parent->m_p_parent);
            }
        }
    }

  base_type::update_to_top(p_nd, (node_update* )this);
  base_type::m_p_head->m_p_parent->m_red = false;
}
#endif
