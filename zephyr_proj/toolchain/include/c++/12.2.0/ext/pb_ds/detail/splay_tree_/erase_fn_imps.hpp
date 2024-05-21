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
 * @file splay_tree_/erase_fn_imps.hpp
 * Contains an implementation class for splay_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
erase(key_const_reference r_key)
{
  point_iterator it = find(r_key);
  if (it == base_type::end())
    return false;
  erase(it);
  return true;
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::iterator
PB_DS_CLASS_C_DEC::
erase(iterator it)
{
  PB_DS_ASSERT_VALID((*this))
  if (it == base_type::end())
    return it;
  iterator ret_it = it;
  ++ret_it;
  erase_node(it.m_p_nd);
  PB_DS_ASSERT_VALID((*this))
  return ret_it;
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::reverse_iterator
PB_DS_CLASS_C_DEC::
erase(reverse_iterator it)
{
  PB_DS_ASSERT_VALID((*this))
  if (it.m_p_nd == base_type::m_p_head)
    return (it);
  reverse_iterator ret_it = it;
  ++ret_it;
  erase_node(it.m_p_nd);
  PB_DS_ASSERT_VALID((*this))
  return ret_it;
}

PB_DS_CLASS_T_DEC
template<typename Pred>
inline typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
erase_if(Pred pred)
{
  PB_DS_ASSERT_VALID((*this))
  size_type num_ersd = 0;
  iterator it = base_type::begin();
  while (it != base_type::end())
    {
      if (pred(*it))
        {
	  ++num_ersd;
	  it = erase(it);
        }
      else
	++it;
    }
  PB_DS_ASSERT_VALID((*this))
  return num_ersd;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
erase_node(node_pointer p_nd)
{
  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);
  splay(p_nd);

  PB_DS_ASSERT_VALID((*this))
  _GLIBCXX_DEBUG_ASSERT(p_nd == this->m_p_head->m_p_parent);

  node_pointer p_l = p_nd->m_p_left;
  node_pointer p_r = p_nd->m_p_right;

  base_type::update_min_max_for_erased_node(p_nd);
  base_type::actual_erase_node(p_nd);
  if (p_r == 0)
    {
      base_type::m_p_head->m_p_parent = p_l;
      if (p_l != 0)
	p_l->m_p_parent = base_type::m_p_head;
      PB_DS_ASSERT_VALID((*this))
      return;
    }

  node_pointer p_target_r = leftmost(p_r);
  _GLIBCXX_DEBUG_ASSERT(p_target_r != 0);
  p_r->m_p_parent = base_type::m_p_head;
  base_type::m_p_head->m_p_parent = p_r;
  splay(p_target_r);

  _GLIBCXX_DEBUG_ONLY(p_target_r->m_p_left = 0);
  _GLIBCXX_DEBUG_ASSERT(p_target_r->m_p_parent == this->m_p_head);
  _GLIBCXX_DEBUG_ASSERT(this->m_p_head->m_p_parent == p_target_r);

  p_target_r->m_p_left = p_l;
  if (p_l != 0)
    p_l->m_p_parent = p_target_r;
  PB_DS_ASSERT_VALID((*this))
  this->apply_update(p_target_r, (node_update*)this);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
leftmost(node_pointer p_nd)
{
  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);
  while (p_nd->m_p_left != 0)
    p_nd = p_nd->m_p_left;
  return p_nd;
}
#endif
