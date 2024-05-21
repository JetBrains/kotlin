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
 * @file pat_trie_/split_fn_imps.hpp
 * Contains an implementation class for pat_trie.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
split(key_const_reference r_key, PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
  branch_bag bag;
  leaf_pointer p_split_lf = split_prep(r_key, other, bag);
  if (p_split_lf == 0)
    {
      _GLIBCXX_DEBUG_ASSERT(bag.empty());
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return;
    }

  _GLIBCXX_DEBUG_ASSERT(!bag.empty());
  other.clear();

  m_p_head->m_p_parent = rec_split(m_p_head->m_p_parent, pref_begin(p_split_lf),
				   pref_end(p_split_lf), other, bag);

  m_p_head->m_p_parent->m_p_parent = m_p_head;

  head_pointer __ohead = other.m_p_head;
  __ohead->m_p_max = m_p_head->m_p_max;
  m_p_head->m_p_max = rightmost_descendant(m_p_head->m_p_parent);
  __ohead->m_p_min = other.leftmost_descendant(__ohead->m_p_parent);

  other.m_size = std::distance(other.PB_DS_CLASS_C_DEC::begin(),
			       other.PB_DS_CLASS_C_DEC::end());
  m_size -= other.m_size;
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::leaf_pointer
PB_DS_CLASS_C_DEC::
split_prep(key_const_reference r_key, PB_DS_CLASS_C_DEC& other,
	   branch_bag& r_bag)
{
  _GLIBCXX_DEBUG_ASSERT(r_bag.empty());
  if (m_size == 0)
    {
      other.clear();
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return 0;
    }

  if (synth_access_traits::cmp_keys(r_key,
				    PB_DS_V2F(static_cast<leaf_const_pointer>(m_p_head->m_p_min)->value())))
    {
      other.clear();
      value_swap(other);
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return 0;
    }

  if (!synth_access_traits::cmp_keys(r_key,
				       PB_DS_V2F(static_cast<leaf_const_pointer>(m_p_head->m_p_max)->value())))
    {
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return 0;
    }

  iterator it = lower_bound(r_key);

  if (!synth_access_traits::equal_keys(PB_DS_V2F(*it), r_key))
    --it;

  node_pointer p_nd = it.m_p_nd;
  _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == leaf_node);
  leaf_pointer p_ret_l = static_cast<leaf_pointer>(p_nd);
  while (p_nd->m_type != head_node)
    {
      r_bag.add_branch();
      p_nd = p_nd->m_p_parent;
    }
  _GLIBCXX_DEBUG_ONLY(debug_base::split(r_key,(synth_access_traits&)(*this), other);)

  return p_ret_l;
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
rec_split(node_pointer p_nd, a_const_iterator b_it, a_const_iterator e_it,
	  PB_DS_CLASS_C_DEC& other, branch_bag& r_bag)
{
  if (p_nd->m_type == leaf_node)
    {
      _GLIBCXX_DEBUG_ASSERT(other.m_p_head->m_p_parent == 0);
      return p_nd;
    }

  _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == i_node);
  inode_pointer p_ind = static_cast<inode_pointer>(p_nd);

  node_pointer pfirst = p_ind->get_child_node(b_it, e_it, this);
  node_pointer p_child_ret = rec_split(pfirst, b_it, e_it, other, r_bag);
  PB_DS_ASSERT_NODE_VALID(p_child_ret)
  p_ind->replace_child(p_child_ret, b_it, e_it, this);
  apply_update(p_ind, (node_update*)this);

  inode_iterator child_it = p_ind->get_child_it(b_it, e_it, this);
  const size_type lhs_dist = std::distance(p_ind->begin(), child_it);
  const size_type lhs_num_children = lhs_dist + 1;
  _GLIBCXX_DEBUG_ASSERT(lhs_num_children > 0);

  const size_type rhs_dist =  std::distance(p_ind->begin(), p_ind->end());
  size_type rhs_num_children = rhs_dist - lhs_num_children;
  if (rhs_num_children == 0)
    {
      apply_update(p_ind, (node_update*)this);
      return p_ind;
    }

  other.split_insert_branch(p_ind->get_e_ind(), b_it, child_it,
			    rhs_num_children, r_bag);

  child_it = p_ind->get_child_it(b_it, e_it, this);
  while (rhs_num_children != 0)
    {
      ++child_it;
      p_ind->remove_child(child_it);
      --rhs_num_children;
    }
  apply_update(p_ind, (node_update*)this);

  const size_type int_dist = std::distance(p_ind->begin(), p_ind->end());
  _GLIBCXX_DEBUG_ASSERT(int_dist >= 1);
  if (int_dist > 1)
    {
      p_ind->update_prefixes(this);
      PB_DS_ASSERT_NODE_VALID(p_ind)
      apply_update(p_ind, (node_update*)this);
      return p_ind;
    }

  node_pointer p_ret = *p_ind->begin();
  p_ind->~inode();
  s_inode_allocator.deallocate(p_ind, 1);
  apply_update(p_ret, (node_update*)this);
  return p_ret;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
split_insert_branch(size_type e_ind, a_const_iterator b_it,
		    inode_iterator child_b_it,
		    size_type num_children, branch_bag& r_bag)
{
#ifdef _GLIBCXX_DEBUG
  if (m_p_head->m_p_parent != 0)
    PB_DS_ASSERT_NODE_VALID(m_p_head->m_p_parent)
#endif

  const size_type start = m_p_head->m_p_parent == 0 ? 0 : 1;
  const size_type total_num_children = start + num_children;
  if (total_num_children == 0)
    {
      _GLIBCXX_DEBUG_ASSERT(m_p_head->m_p_parent == 0);
      return;
    }

  if (total_num_children == 1)
    {
      if (m_p_head->m_p_parent != 0)
	{
	  PB_DS_ASSERT_NODE_VALID(m_p_head->m_p_parent)
	  return;
	}

      _GLIBCXX_DEBUG_ASSERT(m_p_head->m_p_parent == 0);
      ++child_b_it;
      m_p_head->m_p_parent = *child_b_it;
      m_p_head->m_p_parent->m_p_parent = m_p_head;
      apply_update(m_p_head->m_p_parent, (node_update*)this);
      PB_DS_ASSERT_NODE_VALID(m_p_head->m_p_parent)
      return;
    }

  _GLIBCXX_DEBUG_ASSERT(total_num_children > 1);
  inode_pointer p_new_root = r_bag.get_branch();
  new (p_new_root) inode(e_ind, b_it);
  size_type num_inserted = 0;
  while (num_inserted++ < num_children)
    {
      ++child_b_it;
      PB_DS_ASSERT_NODE_VALID((*child_b_it))
      p_new_root->add_child(*child_b_it, pref_begin(*child_b_it),
			    pref_end(*child_b_it), this);
    }

  if (m_p_head->m_p_parent != 0)
    p_new_root->add_child(m_p_head->m_p_parent,
			  pref_begin(m_p_head->m_p_parent),
			  pref_end(m_p_head->m_p_parent), this);

  m_p_head->m_p_parent = p_new_root;
  p_new_root->m_p_parent = m_p_head;
  apply_update(m_p_head->m_p_parent, (node_update*)this);
  PB_DS_ASSERT_NODE_VALID(m_p_head->m_p_parent)
}
#endif
