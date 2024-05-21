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
 * @file thin_heap_/erase_fn_imps.hpp
 * Contains an implementation for thin_heap_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
pop()
{
  PB_DS_ASSERT_VALID((*this))
  _GLIBCXX_DEBUG_ASSERT(!base_type::empty());
  _GLIBCXX_DEBUG_ASSERT(m_p_max != 0);

  node_pointer p_nd = m_p_max;
  remove_max_node();
  base_type::actual_erase_node(p_nd);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
remove_max_node()
{
  to_aux_except_max();
  make_from_aux();
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
to_aux_except_max()
{
  node_pointer p_add = base_type::m_p_root;
  while (p_add != m_p_max)
    {
      node_pointer p_next_add = p_add->m_p_next_sibling;
      add_to_aux(p_add);
      p_add = p_next_add;
    }

  p_add = m_p_max->m_p_l_child;
  while (p_add != 0)
    {
      node_pointer p_next_add = p_add->m_p_next_sibling;
      p_add->m_metadata = p_add->m_p_l_child == 0 ?
	0 : p_add->m_p_l_child->m_metadata + 1;

      add_to_aux(p_add);
      p_add = p_next_add;
    }

  p_add = m_p_max->m_p_next_sibling;
  while (p_add != 0)
    {
      node_pointer p_next_add = p_add->m_p_next_sibling;
      add_to_aux(p_add);
      p_add = p_next_add;
    }
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
add_to_aux(node_pointer p_nd)
{
  size_type r = p_nd->m_metadata;
  while (m_a_aux[r] != 0)
    {
      _GLIBCXX_DEBUG_ASSERT(p_nd->m_metadata < rank_bound());
      if (Cmp_Fn::operator()(m_a_aux[r]->m_value, p_nd->m_value))
	make_child_of(m_a_aux[r], p_nd);
      else
	{
	  make_child_of(p_nd, m_a_aux[r]);
	  p_nd = m_a_aux[r];
	}

      m_a_aux[r] = 0;
      ++r;
    }

  _GLIBCXX_DEBUG_ASSERT(p_nd->m_metadata < rank_bound());

  m_a_aux[r] = p_nd;
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
make_child_of(node_pointer p_nd, node_pointer p_new_parent)
{
  _GLIBCXX_DEBUG_ASSERT(p_nd->m_metadata == p_new_parent->m_metadata);
  _GLIBCXX_DEBUG_ASSERT(m_a_aux[p_nd->m_metadata] == p_nd ||
		   m_a_aux[p_nd->m_metadata] == p_new_parent);

  ++p_new_parent->m_metadata;
  base_type::make_child_of(p_nd, p_new_parent);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
make_from_aux()
{
  base_type::m_p_root = m_p_max = 0;
  const size_type rnk_bnd = rank_bound();
  size_type i = 0;
  while (i < rnk_bnd)
    {
      if (m_a_aux[i] != 0)
	{
	  make_root_and_link(m_a_aux[i]);
	  m_a_aux[i] = 0;
	}
      ++i;
    }

  PB_DS_ASSERT_AUX_NULL((*this))
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
remove_node(node_pointer p_nd)
{
  node_pointer p_parent = p_nd;
  while (base_type::parent(p_parent) != 0)
    p_parent = base_type::parent(p_parent);

  base_type::bubble_to_top(p_nd);
  m_p_max = p_nd;

  node_pointer p_fix = base_type::m_p_root;
  while (p_fix != 0&&  p_fix->m_p_next_sibling != p_parent)
    p_fix = p_fix->m_p_next_sibling;

  if (p_fix != 0)
    p_fix->m_p_next_sibling = p_nd;

  remove_max_node();
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
  PB_DS_ASSERT_VALID((*this))
  _GLIBCXX_DEBUG_ASSERT(!base_type::empty());

  node_pointer p_nd = it.m_p_nd;
  remove_node(p_nd);
  base_type::actual_erase_node(p_nd);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
template<typename Pred>
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
erase_if(Pred pred)
{
  PB_DS_ASSERT_VALID((*this))
  if (base_type::empty())
    {
      PB_DS_ASSERT_VALID((*this))
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
  m_p_max = base_type::m_p_root = 0;
  while (p_cur != 0)
    {
      node_pointer p_next = p_cur->m_p_next_sibling;
      make_root_and_link(p_cur);
      p_cur = p_next;
    }

  PB_DS_ASSERT_VALID((*this))
  return ersd;
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
rank_bound()
{
  using namespace std;
  const size_t* const p_upper =
    std::upper_bound(g_a_rank_bounds,
		     g_a_rank_bounds + num_distinct_rank_bounds,
		     base_type::m_size);

  if (p_upper == g_a_rank_bounds + num_distinct_rank_bounds)
    return max_rank;

  return (p_upper - g_a_rank_bounds);
}
#endif
