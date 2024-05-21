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
 * @file rb_tree_map_/erase_fn_imps.hpp
 * Contains an implementation for rb_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
erase(key_const_reference r_key)
{
  point_iterator it = this->find(r_key);
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
    return it;

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
  remove_node(p_nd);
  base_type::actual_erase_node(p_nd);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
remove_node(node_pointer p_z)
{
  this->update_min_max_for_erased_node(p_z);
  node_pointer p_y = p_z;
  node_pointer p_x = 0;
  node_pointer p_new_x_parent = 0;

  if (p_y->m_p_left == 0)
    p_x = p_y->m_p_right;
  else if (p_y->m_p_right == 0)
    p_x = p_y->m_p_left;
  else
    {
      p_y = p_y->m_p_right;
      while (p_y->m_p_left != 0)
	p_y = p_y->m_p_left;
      p_x = p_y->m_p_right;
    }

  if (p_y == p_z)
    {
      p_new_x_parent = p_y->m_p_parent;
      if (p_x != 0)
	p_x->m_p_parent = p_y->m_p_parent;

      if (base_type::m_p_head->m_p_parent == p_z)
	base_type::m_p_head->m_p_parent = p_x;
      else if (p_z->m_p_parent->m_p_left == p_z)
        {
	  p_y->m_p_left = p_z->m_p_parent;
	  p_z->m_p_parent->m_p_left = p_x;
        }
      else
        {
	  p_y->m_p_left = 0;
	  p_z->m_p_parent->m_p_right = p_x;
        }
    }
  else
    {
      p_z->m_p_left->m_p_parent = p_y;
      p_y->m_p_left = p_z->m_p_left;
      if (p_y != p_z->m_p_right)
        {
	  p_new_x_parent = p_y->m_p_parent;
	  if (p_x != 0)
	    p_x->m_p_parent = p_y->m_p_parent;
	  p_y->m_p_parent->m_p_left = p_x;
	  p_y->m_p_right = p_z->m_p_right;
	  p_z->m_p_right->m_p_parent = p_y;
        }
      else
	p_new_x_parent = p_y;

      if (base_type::m_p_head->m_p_parent == p_z)
	base_type::m_p_head->m_p_parent = p_y;
      else if (p_z->m_p_parent->m_p_left == p_z)
	p_z->m_p_parent->m_p_left = p_y;
      else
	p_z->m_p_parent->m_p_right = p_y;

      p_y->m_p_parent = p_z->m_p_parent;
      std::swap(p_y->m_red, p_z->m_red);
      p_y = p_z;
    }

  this->update_to_top(p_new_x_parent, (node_update* )this);

  if (p_y->m_red)
    return;

  remove_fixup(p_x, p_new_x_parent);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
remove_fixup(node_pointer p_x, node_pointer p_new_x_parent)
{
  _GLIBCXX_DEBUG_ASSERT(p_x == 0 || p_x->m_p_parent == p_new_x_parent);

  while (p_x != base_type::m_p_head->m_p_parent && is_effectively_black(p_x))
    if (p_x == p_new_x_parent->m_p_left)
      {
	node_pointer p_w = p_new_x_parent->m_p_right;
	if (p_w->m_red)
	  {
	    p_w->m_red = false;
	    p_new_x_parent->m_red = true;
	    base_type::rotate_left(p_new_x_parent);
	    p_w = p_new_x_parent->m_p_right;
	  }

	if (is_effectively_black(p_w->m_p_left) 
	    && is_effectively_black(p_w->m_p_right))
	  {
	    p_w->m_red = true;
	    p_x = p_new_x_parent;
	    p_new_x_parent = p_new_x_parent->m_p_parent;
	  }
	else
	  {
	    if (is_effectively_black(p_w->m_p_right))
	      {
		if (p_w->m_p_left != 0)
		  p_w->m_p_left->m_red = false;

		p_w->m_red = true;
		base_type::rotate_right(p_w);
		p_w = p_new_x_parent->m_p_right;
	      }

	    p_w->m_red = p_new_x_parent->m_red;
	    p_new_x_parent->m_red = false;

	    if (p_w->m_p_right != 0)
	      p_w->m_p_right->m_red = false;

	    base_type::rotate_left(p_new_x_parent);
	    this->update_to_top(p_new_x_parent, (node_update* )this);
	    break;
	  }
      }
    else
      {
	node_pointer p_w = p_new_x_parent->m_p_left;
	if (p_w->m_red == true)
	  {
	    p_w->m_red = false;
	    p_new_x_parent->m_red = true;
	    base_type::rotate_right(p_new_x_parent);
	    p_w = p_new_x_parent->m_p_left;
	  }

	if (is_effectively_black(p_w->m_p_right) 
	    && is_effectively_black(p_w->m_p_left))
	  {
	    p_w->m_red = true;
	    p_x = p_new_x_parent;
	    p_new_x_parent = p_new_x_parent->m_p_parent;
	  }
	else
	  {
	    if (is_effectively_black(p_w->m_p_left))
	      {
		if (p_w->m_p_right != 0)
		  p_w->m_p_right->m_red = false;

		p_w->m_red = true;
		base_type::rotate_left(p_w);
		p_w = p_new_x_parent->m_p_left;
	      }

	    p_w->m_red = p_new_x_parent->m_red;
	    p_new_x_parent->m_red = false;

	    if (p_w->m_p_left != 0)
	      p_w->m_p_left->m_red = false;

	    base_type::rotate_right(p_new_x_parent);
	    this->update_to_top(p_new_x_parent, (node_update* )this);
	    break;
	  }
      }

  if (p_x != 0)
    p_x->m_red = false;
}
#endif
