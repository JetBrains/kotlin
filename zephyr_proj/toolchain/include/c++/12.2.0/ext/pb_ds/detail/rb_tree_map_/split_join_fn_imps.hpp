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
 * @file rb_tree_map_/split_join_fn_imps.hpp
 * Contains an implementation for rb_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
join(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
  if (base_type::join_prep(other) == false)
    {
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return;
    }

  const node_pointer p_x = other.split_min();
  join_imp(p_x, other.m_p_head->m_p_parent);
  base_type::join_finish(other);
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
 }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
join_imp(node_pointer p_x, node_pointer p_r)
{
  _GLIBCXX_DEBUG_ASSERT(p_x != 0);
  if (p_r != 0)
    p_r->m_red = false;

  const size_type h = black_height(base_type::m_p_head->m_p_parent);
  const size_type other_h = black_height(p_r);
  node_pointer p_x_l;
  node_pointer p_x_r;
  std::pair<node_pointer, node_pointer> join_pos;
  const bool right_join = h >= other_h;
  if (right_join)
    {
      join_pos = find_join_pos_right(base_type::m_p_head->m_p_parent, 
				     h, other_h);
      p_x_l = join_pos.first;
      p_x_r = p_r;
    }
  else
    {
      p_x_l = base_type::m_p_head->m_p_parent;
      base_type::m_p_head->m_p_parent = p_r;
      if (p_r != 0)
	p_r->m_p_parent = base_type::m_p_head;

      join_pos = find_join_pos_left(base_type::m_p_head->m_p_parent, 
				    h, other_h);
      p_x_r = join_pos.first;
    }

  node_pointer p_parent = join_pos.second;
  if (p_parent == base_type::m_p_head)
    {
      base_type::m_p_head->m_p_parent = p_x;
      p_x->m_p_parent = base_type::m_p_head;
    }
  else
    {
      p_x->m_p_parent = p_parent;
      if (right_join)
	p_x->m_p_parent->m_p_right = p_x;
      else
	p_x->m_p_parent->m_p_left = p_x;
    }

  p_x->m_p_left = p_x_l;
  if (p_x_l != 0)
    p_x_l->m_p_parent = p_x;

  p_x->m_p_right = p_x_r;
  if (p_x_r != 0)
    p_x_r->m_p_parent = p_x;

  p_x->m_red = true;

  base_type::initialize_min_max();
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
  base_type::update_to_top(p_x, (node_update* )this);
  insert_fixup(p_x);
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
split_min()
{
  node_pointer p_min = base_type::m_p_head->m_p_left;

#ifdef _GLIBCXX_DEBUG
  const node_pointer p_head = base_type::m_p_head;
  _GLIBCXX_DEBUG_ASSERT(p_min != p_head);
#endif 

  remove_node(p_min);
  return p_min;
}

PB_DS_CLASS_T_DEC
std::pair<
  typename PB_DS_CLASS_C_DEC::node_pointer,
  typename PB_DS_CLASS_C_DEC::node_pointer>
PB_DS_CLASS_C_DEC::
find_join_pos_right(node_pointer p_l, size_type h_l, size_type h_r)
{
  _GLIBCXX_DEBUG_ASSERT(h_l >= h_r);

  if (base_type::m_p_head->m_p_parent == 0)
    return (std::make_pair((node_pointer)0, base_type::m_p_head));

  node_pointer p_l_parent = base_type::m_p_head;
  while (h_l > h_r)
    {
      if (p_l->m_red == false)
        {
	  _GLIBCXX_DEBUG_ASSERT(h_l > 0);
	  --h_l;
        }

      p_l_parent = p_l;
      p_l = p_l->m_p_right;
    }

  if (!is_effectively_black(p_l))
    {
      p_l_parent = p_l;
      p_l = p_l->m_p_right;
    }

  _GLIBCXX_DEBUG_ASSERT(is_effectively_black(p_l));
  _GLIBCXX_DEBUG_ASSERT(black_height(p_l) == h_r);
  _GLIBCXX_DEBUG_ASSERT(p_l == 0 || p_l->m_p_parent == p_l_parent);
  return std::make_pair(p_l, p_l_parent);
}

PB_DS_CLASS_T_DEC
std::pair<
  typename PB_DS_CLASS_C_DEC::node_pointer,
  typename PB_DS_CLASS_C_DEC::node_pointer>
PB_DS_CLASS_C_DEC::
find_join_pos_left(node_pointer p_r, size_type h_l, size_type h_r)
{
  _GLIBCXX_DEBUG_ASSERT(h_r > h_l);
  if (base_type::m_p_head->m_p_parent == 0)
    return (std::make_pair((node_pointer)0,
			   base_type::m_p_head));
  node_pointer p_r_parent = base_type::m_p_head;
  while (h_r > h_l)
    {
      if (p_r->m_red == false)
        {
	  _GLIBCXX_DEBUG_ASSERT(h_r > 0);
	  --h_r;
        }

      p_r_parent = p_r;
      p_r = p_r->m_p_left;
    }

  if (!is_effectively_black(p_r))
    {
      p_r_parent = p_r;
      p_r = p_r->m_p_left;
    }

  _GLIBCXX_DEBUG_ASSERT(is_effectively_black(p_r));
  _GLIBCXX_DEBUG_ASSERT(black_height(p_r) == h_l);
  _GLIBCXX_DEBUG_ASSERT(p_r == 0 || p_r->m_p_parent == p_r_parent);
  return std::make_pair(p_r, p_r_parent);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
black_height(node_pointer p_nd)
{
  size_type h = 1;
  while (p_nd != 0)
    {
      if (p_nd->m_red == false)
	++h;
      p_nd = p_nd->m_p_left;
    }
  return h;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
split(key_const_reference r_key, PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)

  if (base_type::split_prep(r_key, other) == false)
    {
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return;
    }

  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
  PB_DS_STRUCT_ONLY_ASSERT_VALID(other)
  node_pointer p_nd = this->upper_bound(r_key).m_p_nd;
  do
    {
      node_pointer p_next_nd = p_nd->m_p_parent;
      if (Cmp_Fn::operator()(r_key, PB_DS_V2F(p_nd->m_value)))
	split_at_node(p_nd, other);

      PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
      PB_DS_STRUCT_ONLY_ASSERT_VALID(other)
      p_nd = p_next_nd;
    }
  while (p_nd != base_type::m_p_head);

  base_type::split_finish(other);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
split_at_node(node_pointer p_nd, PB_DS_CLASS_C_DEC& other)
{
  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);

  node_pointer p_l = p_nd->m_p_left;
  node_pointer p_r = p_nd->m_p_right;
  node_pointer p_parent = p_nd->m_p_parent;
  if (p_parent == base_type::m_p_head)
    {
      base_type::m_p_head->m_p_parent = p_l;
      if (p_l != 0)
        {
	  p_l->m_p_parent = base_type::m_p_head;
	  p_l->m_red = false;
        }
    }
  else
    {
      if (p_parent->m_p_left == p_nd)
	p_parent->m_p_left = p_l;
      else
	p_parent->m_p_right = p_l;

      if (p_l != 0)
	p_l->m_p_parent = p_parent;

      this->update_to_top(p_parent, (node_update* )this);

      if (!p_nd->m_red)
	remove_fixup(p_l, p_parent);
    }

  base_type::initialize_min_max();
  other.join_imp(p_nd, p_r);
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
  PB_DS_STRUCT_ONLY_ASSERT_VALID(other)
}

#endif
