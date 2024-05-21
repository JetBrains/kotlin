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
 * @file splay_tree_/splay_fn_imps.hpp
 * Contains an implementation class for splay_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
splay(node_pointer p_nd)
{
  while (p_nd->m_p_parent != base_type::m_p_head)
    {
#ifdef _GLIBCXX_DEBUG
      {
	node_pointer p_head = base_type::m_p_head;
	assert_special_imp(p_head, __FILE__, __LINE__);
      }
#endif

      PB_DS_ASSERT_BASE_NODE_CONSISTENT(p_nd)

      if (p_nd->m_p_parent->m_p_parent == base_type::m_p_head)
	{
	  base_type::rotate_parent(p_nd);
	  _GLIBCXX_DEBUG_ASSERT(p_nd == this->m_p_head->m_p_parent);
	}
      else
	{
	  const node_pointer p_parent = p_nd->m_p_parent;
	  const node_pointer p_grandparent = p_parent->m_p_parent;

#ifdef _GLIBCXX_DEBUG
	  const size_type total =
	    base_type::recursive_count(p_grandparent);
	  _GLIBCXX_DEBUG_ASSERT(total >= 3);
#endif

	  if (p_parent->m_p_left == p_nd &&
	      p_grandparent->m_p_right == p_parent)
	    splay_zig_zag_left(p_nd, p_parent, p_grandparent);
	  else if (p_parent->m_p_right == p_nd &&
		   p_grandparent->m_p_left == p_parent)
	    splay_zig_zag_right(p_nd, p_parent, p_grandparent);
	  else if (p_parent->m_p_left == p_nd &&
		   p_grandparent->m_p_left == p_parent)
	    splay_zig_zig_left(p_nd, p_parent, p_grandparent);
	  else
	    splay_zig_zig_right(p_nd, p_parent, p_grandparent);
	  _GLIBCXX_DEBUG_ASSERT(total ==this->recursive_count(p_nd));
	}

      PB_DS_ASSERT_BASE_NODE_CONSISTENT(p_nd)
    }
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
splay_zig_zag_left(node_pointer p_nd, node_pointer p_parent,
		   node_pointer p_grandparent)
{
  _GLIBCXX_DEBUG_ASSERT(p_parent == p_nd->m_p_parent);
  _GLIBCXX_DEBUG_ASSERT(p_grandparent == p_parent->m_p_parent);

  PB_DS_ASSERT_BASE_NODE_CONSISTENT(p_grandparent)

  _GLIBCXX_DEBUG_ASSERT(p_parent->m_p_left == p_nd &&
			p_grandparent->m_p_right == p_parent);

  splay_zz_start(p_nd, p_parent, p_grandparent);

  node_pointer p_b = p_nd->m_p_right;
  node_pointer p_c = p_nd->m_p_left;

  p_nd->m_p_right = p_parent;
  p_parent->m_p_parent = p_nd;

  p_nd->m_p_left = p_grandparent;
  p_grandparent->m_p_parent = p_nd;

  p_parent->m_p_left = p_b;
  if (p_b != 0)
    p_b->m_p_parent = p_parent;

  p_grandparent->m_p_right = p_c;
  if (p_c != 0)
    p_c->m_p_parent = p_grandparent;

  splay_zz_end(p_nd, p_parent, p_grandparent);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
splay_zig_zag_right(node_pointer p_nd, node_pointer p_parent,
		    node_pointer p_grandparent)
{
  _GLIBCXX_DEBUG_ASSERT(p_parent == p_nd->m_p_parent);
  _GLIBCXX_DEBUG_ASSERT(p_grandparent == p_parent->m_p_parent);

  PB_DS_ASSERT_BASE_NODE_CONSISTENT(p_grandparent)

  _GLIBCXX_DEBUG_ASSERT(p_parent->m_p_right == p_nd &&
	  		p_grandparent->m_p_left == p_parent);

  splay_zz_start(p_nd, p_parent, p_grandparent);

  node_pointer p_b = p_nd->m_p_left;
  node_pointer p_c = p_nd->m_p_right;

  p_nd->m_p_left = p_parent;
  p_parent->m_p_parent = p_nd;

  p_nd->m_p_right = p_grandparent;
  p_grandparent->m_p_parent = p_nd;

  p_parent->m_p_right = p_b;
  if (p_b != 0)
    p_b->m_p_parent = p_parent;

  p_grandparent->m_p_left = p_c;
  if (p_c != 0)
    p_c->m_p_parent = p_grandparent;

  splay_zz_end(p_nd, p_parent, p_grandparent);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
splay_zig_zig_left(node_pointer p_nd, node_pointer p_parent,
		   node_pointer p_grandparent)
{
  _GLIBCXX_DEBUG_ASSERT(p_parent == p_nd->m_p_parent);
  _GLIBCXX_DEBUG_ASSERT(p_grandparent == p_parent->m_p_parent);

  PB_DS_ASSERT_BASE_NODE_CONSISTENT(p_grandparent)

  _GLIBCXX_DEBUG_ASSERT(p_parent->m_p_left == p_nd &&
		     p_nd->m_p_parent->m_p_parent->m_p_left == p_nd->m_p_parent);

  splay_zz_start(p_nd, p_parent, p_grandparent);

  node_pointer p_b = p_nd->m_p_right;
  node_pointer p_c = p_parent->m_p_right;

  p_nd->m_p_right = p_parent;
  p_parent->m_p_parent = p_nd;

  p_parent->m_p_right = p_grandparent;
  p_grandparent->m_p_parent = p_parent;

  p_parent->m_p_left = p_b;
  if (p_b != 0)
    p_b->m_p_parent = p_parent;

  p_grandparent->m_p_left = p_c;
  if (p_c != 0)
    p_c->m_p_parent = p_grandparent;

  splay_zz_end(p_nd, p_parent, p_grandparent);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
splay_zig_zig_right(node_pointer p_nd, node_pointer p_parent,
		    node_pointer p_grandparent)
{
  _GLIBCXX_DEBUG_ASSERT(p_parent == p_nd->m_p_parent);
  _GLIBCXX_DEBUG_ASSERT(p_grandparent == p_parent->m_p_parent);
  PB_DS_ASSERT_BASE_NODE_CONSISTENT(p_grandparent)
  _GLIBCXX_DEBUG_ASSERT(p_parent->m_p_right == p_nd &&
		  p_nd->m_p_parent->m_p_parent->m_p_right == p_nd->m_p_parent);

  splay_zz_start(p_nd, p_parent, p_grandparent);

  node_pointer p_b = p_nd->m_p_left;
  node_pointer p_c = p_parent->m_p_left;

  p_nd->m_p_left = p_parent;
  p_parent->m_p_parent = p_nd;

  p_parent->m_p_left = p_grandparent;
  p_grandparent->m_p_parent = p_parent;

  p_parent->m_p_right = p_b;
  if (p_b != 0)
    p_b->m_p_parent = p_parent;

  p_grandparent->m_p_right = p_c;
  if (p_c != 0)
    p_c->m_p_parent = p_grandparent;

  base_type::update_to_top(p_grandparent, (node_update*)this);
  splay_zz_end(p_nd, p_parent, p_grandparent);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
splay_zz_start(node_pointer p_nd,
#ifdef _GLIBCXX_DEBUG
	       node_pointer p_parent,
#else
	       node_pointer /*p_parent*/,
#endif
	       node_pointer p_grandparent)
{
  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);
  _GLIBCXX_DEBUG_ASSERT(p_parent != 0);
  _GLIBCXX_DEBUG_ASSERT(p_grandparent != 0);

  const bool grandparent_head = p_grandparent->m_p_parent == base_type::m_p_head;

  if (grandparent_head)
    {
      base_type::m_p_head->m_p_parent = base_type::m_p_head->m_p_parent;
      p_nd->m_p_parent = base_type::m_p_head;
      return;
    }

  node_pointer p_greatgrandparent = p_grandparent->m_p_parent;

  p_nd->m_p_parent = p_greatgrandparent;

  if (p_grandparent == p_greatgrandparent->m_p_left)
    p_greatgrandparent->m_p_left = p_nd;
  else
    p_greatgrandparent->m_p_right = p_nd;
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
splay_zz_end(node_pointer p_nd, node_pointer p_parent,
	     node_pointer p_grandparent)
{
  if (p_nd->m_p_parent == base_type::m_p_head)
    base_type::m_p_head->m_p_parent = p_nd;

  this->apply_update(p_grandparent, (node_update*)this);
  this->apply_update(p_parent, (node_update*)this);
  this->apply_update(p_nd, (node_update*)this);
  PB_DS_ASSERT_BASE_NODE_CONSISTENT(p_nd)
}
#endif
