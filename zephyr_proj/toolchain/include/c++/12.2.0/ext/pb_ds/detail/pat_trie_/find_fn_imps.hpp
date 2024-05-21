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
 * @file pat_trie_/find_fn_imps.hpp
 * Contains an implementation class for pat_trie.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_iterator
PB_DS_CLASS_C_DEC::
find(key_const_reference r_key)
{
  PB_DS_ASSERT_VALID((*this))
  node_pointer p_nd = find_imp(r_key);

  if (p_nd == 0 || p_nd->m_type != leaf_node)
    {
      PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
      return end();
    }

  if (synth_access_traits::equal_keys(PB_DS_V2F(static_cast<leaf_pointer>(p_nd)->value()), r_key))
    {
      PB_DS_CHECK_KEY_EXISTS(r_key)
      return iterator(p_nd);
    }

  PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
  return end();
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_const_iterator
PB_DS_CLASS_C_DEC::
find(key_const_reference r_key) const
{
  PB_DS_ASSERT_VALID((*this))

  node_const_pointer p_nd = const_cast<PB_DS_CLASS_C_DEC* >(this)->find_imp(r_key);

  if (p_nd == 0 || p_nd->m_type != leaf_node)
    {
      PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
      return end();
    }

  if (synth_access_traits::equal_keys(PB_DS_V2F(static_cast<leaf_const_pointer>(p_nd)->value()), r_key))
    {
      PB_DS_CHECK_KEY_EXISTS(r_key)
      return const_iterator(const_cast<node_pointer>(p_nd));
    }

  PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
  return end();
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
find_imp(key_const_reference r_key)
{
  if (empty())
    return 0;

  typename synth_access_traits::const_iterator b_it =
    synth_access_traits::begin(r_key);
  typename synth_access_traits::const_iterator e_it =
    synth_access_traits::end(r_key);

  node_pointer p_nd = m_p_head->m_p_parent;
  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);

  while (p_nd->m_type != leaf_node)
    {
      _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == i_node);
      node_pointer p_next_nd = static_cast<inode_pointer>(p_nd)->get_child_node(b_it,  e_it,  this);

      if (p_next_nd == 0)
	return p_nd;
      p_nd = p_next_nd;
    }
  return p_nd;
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
lower_bound_imp(key_const_reference r_key)
{
  if (empty())
    return (m_p_head);

  node_pointer p_nd = m_p_head->m_p_parent;
  _GLIBCXX_DEBUG_ASSERT(p_nd != 0);

  typename PB_DS_CLASS_C_DEC::a_const_iterator b_it =
    synth_access_traits::begin(r_key);

  typename PB_DS_CLASS_C_DEC::a_const_iterator e_it =
    synth_access_traits::end(r_key);

  size_type checked_ind = 0;
  while (true)
    {
      if (p_nd->m_type == leaf_node)
        {
	  if (!synth_access_traits::cmp_keys(PB_DS_V2F(static_cast<leaf_const_pointer>(p_nd)->value()), r_key))
	    return p_nd;
	  iterator it(p_nd);
	  ++it;
	  return it.m_p_nd;
        }

      _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == i_node);
      const size_type new_checked_ind =
	static_cast<inode_pointer>(p_nd)->get_e_ind();

      p_nd =
	static_cast<inode_pointer>(p_nd)->get_lower_bound_child_node(                b_it, e_it, checked_ind, this);
      checked_ind = new_checked_ind;
    }
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_iterator
PB_DS_CLASS_C_DEC::
lower_bound(key_const_reference r_key)
{ return point_iterator(lower_bound_imp(r_key)); }

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_const_iterator
PB_DS_CLASS_C_DEC::
lower_bound(key_const_reference r_key) const
{
  return point_const_iterator(const_cast<PB_DS_CLASS_C_DEC* >(this)->lower_bound_imp(r_key));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_iterator
PB_DS_CLASS_C_DEC::
upper_bound(key_const_reference r_key)
{
  point_iterator l_bound_it = lower_bound(r_key);

  _GLIBCXX_DEBUG_ASSERT(l_bound_it == end() ||
		   !synth_access_traits::cmp_keys(PB_DS_V2F(*l_bound_it),
						    r_key));

  if (l_bound_it == end() ||
      synth_access_traits::cmp_keys(r_key, PB_DS_V2F(*l_bound_it)))
    return l_bound_it;

  return ++l_bound_it;
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::point_const_iterator
PB_DS_CLASS_C_DEC::
upper_bound(key_const_reference r_key) const
{
  point_const_iterator l_bound_it = lower_bound(r_key);

  _GLIBCXX_DEBUG_ASSERT(l_bound_it == end() ||
		   !synth_access_traits::cmp_keys(PB_DS_V2F(*l_bound_it),
						    r_key));

  if (l_bound_it == end() ||
      synth_access_traits::cmp_keys(r_key, PB_DS_V2F(*l_bound_it)))
    return l_bound_it;
  return ++l_bound_it;
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::a_const_iterator
PB_DS_CLASS_C_DEC::
pref_begin(node_const_pointer p_nd)
{
  if (p_nd->m_type == leaf_node)
    return (synth_access_traits::begin(PB_DS_V2F(static_cast<leaf_const_pointer>(p_nd)->value())));

  _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == i_node);
  return static_cast<inode_const_pointer>(p_nd)->pref_b_it();
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::a_const_iterator
PB_DS_CLASS_C_DEC::
pref_end(node_const_pointer p_nd)
{
  if (p_nd->m_type == leaf_node)
    return (synth_access_traits::end(PB_DS_V2F(static_cast<leaf_const_pointer>(p_nd)->value())));

  _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == i_node);
  return static_cast<inode_const_pointer>(p_nd)->pref_e_it();
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::leaf_const_pointer
PB_DS_CLASS_C_DEC::
leftmost_descendant(node_const_pointer p_nd)
{
  if (p_nd->m_type == leaf_node)
    return static_cast<leaf_const_pointer>(p_nd);
  return static_cast<inode_const_pointer>(p_nd)->leftmost_descendant();
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::leaf_pointer
PB_DS_CLASS_C_DEC::
leftmost_descendant(node_pointer p_nd)
{
  if (p_nd->m_type == leaf_node)
    return static_cast<leaf_pointer>(p_nd);
  return static_cast<inode_pointer>(p_nd)->leftmost_descendant();
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::leaf_const_pointer
PB_DS_CLASS_C_DEC::
rightmost_descendant(node_const_pointer p_nd)
{
  if (p_nd->m_type == leaf_node)
    return static_cast<leaf_const_pointer>(p_nd);
  return static_cast<inode_const_pointer>(p_nd)->rightmost_descendant();
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::leaf_pointer
PB_DS_CLASS_C_DEC::
rightmost_descendant(node_pointer p_nd)
{
  if (p_nd->m_type == leaf_node)
    return static_cast<leaf_pointer>(p_nd);
  return static_cast<inode_pointer>(p_nd)->rightmost_descendant();
}

#endif
