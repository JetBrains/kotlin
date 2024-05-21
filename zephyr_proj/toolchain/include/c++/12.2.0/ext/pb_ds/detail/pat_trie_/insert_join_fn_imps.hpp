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
 * @file pat_trie_/insert_join_fn_imps.hpp
 * Contains an implementation class for pat_trie.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
join(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
  branch_bag bag;
  if (!join_prep(other, bag))
    {
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return;
    }

  m_p_head->m_p_parent = rec_join(m_p_head->m_p_parent,
				  other.m_p_head->m_p_parent, 0, bag);

  m_p_head->m_p_parent->m_p_parent = m_p_head;
  m_size += other.m_size;
  other.initialize();
  PB_DS_ASSERT_VALID(other)
  m_p_head->m_p_min = leftmost_descendant(m_p_head->m_p_parent);
  m_p_head->m_p_max = rightmost_descendant(m_p_head->m_p_parent);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
bool
PB_DS_CLASS_C_DEC::
join_prep(PB_DS_CLASS_C_DEC& other, branch_bag& r_bag)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
  if (other.m_size == 0)
    return false;

  if (m_size == 0)
    {
      value_swap(other);
      return false;
    }

  const bool greater =
    synth_access_traits::cmp_keys(PB_DS_V2F(static_cast<leaf_const_pointer>(m_p_head->m_p_max)->value()),
				    PB_DS_V2F(static_cast<leaf_const_pointer>(other.m_p_head->m_p_min)->value()));

  const bool lesser =
    synth_access_traits::cmp_keys(PB_DS_V2F(static_cast<leaf_const_pointer>(other.m_p_head->m_p_max)->value()),
				    PB_DS_V2F(static_cast<leaf_const_pointer>(m_p_head->m_p_min)->value()));

  if (!greater && !lesser)
    __throw_join_error();

  rec_join_prep(m_p_head->m_p_parent, other.m_p_head->m_p_parent, r_bag);
  _GLIBCXX_DEBUG_ONLY(debug_base::join(other, false);)
  return true;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
rec_join_prep(node_const_pointer p_l, node_const_pointer p_r, 
	      branch_bag& r_bag)
{
  if (p_l->m_type == leaf_node)
    {
      if (p_r->m_type == leaf_node)
	{
	  rec_join_prep(static_cast<leaf_const_pointer>(p_l),
			static_cast<leaf_const_pointer>(p_r), r_bag);
	  return;
	}

      _GLIBCXX_DEBUG_ASSERT(p_r->m_type == i_node);
      rec_join_prep(static_cast<leaf_const_pointer>(p_l),
		    static_cast<inode_const_pointer>(p_r), r_bag);
      return;
    }

  _GLIBCXX_DEBUG_ASSERT(p_l->m_type == i_node);
  if (p_r->m_type == leaf_node)
    {
      rec_join_prep(static_cast<inode_const_pointer>(p_l),
		    static_cast<leaf_const_pointer>(p_r), r_bag);
      return;
    }

  _GLIBCXX_DEBUG_ASSERT(p_r->m_type == i_node);

  rec_join_prep(static_cast<inode_const_pointer>(p_l),
		static_cast<inode_const_pointer>(p_r), r_bag);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
rec_join_prep(leaf_const_pointer /*p_l*/, leaf_const_pointer /*p_r*/,
	      branch_bag& r_bag)
{ r_bag.add_branch(); }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
rec_join_prep(leaf_const_pointer /*p_l*/, inode_const_pointer /*p_r*/,
	      branch_bag& r_bag)
{ r_bag.add_branch(); }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
rec_join_prep(inode_const_pointer /*p_l*/, leaf_const_pointer /*p_r*/,
	      branch_bag& r_bag)
{ r_bag.add_branch(); }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
rec_join_prep(inode_const_pointer p_l, inode_const_pointer p_r,
	      branch_bag& r_bag)
{
  if (p_l->get_e_ind() == p_r->get_e_ind() &&
      synth_access_traits::equal_prefixes(p_l->pref_b_it(), p_l->pref_e_it(),
					    p_r->pref_b_it(), p_r->pref_e_it()))
    {
      for (typename inode::const_iterator it = p_r->begin();
	   it != p_r->end(); ++ it)
	{
	  node_const_pointer p_l_join_child = p_l->get_join_child(*it, this);
	  if (p_l_join_child != 0)
	    rec_join_prep(p_l_join_child, * it, r_bag);
	}
      return;
    }

  if (p_r->get_e_ind() < p_l->get_e_ind() &&
      p_r->should_be_mine(p_l->pref_b_it(), p_l->pref_e_it(), 0, this))
    {
      node_const_pointer p_r_join_child = p_r->get_join_child(p_l, this);
      if (p_r_join_child != 0)
	rec_join_prep(p_r_join_child, p_l, r_bag);
      return;
    }

  if (p_r->get_e_ind() < p_l->get_e_ind() &&
      p_r->should_be_mine(p_l->pref_b_it(), p_l->pref_e_it(), 0, this))
    {
      node_const_pointer p_r_join_child = p_r->get_join_child(p_l, this);
      if (p_r_join_child != 0)
	rec_join_prep(p_r_join_child, p_l, r_bag);
      return;
    }
  r_bag.add_branch();
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
rec_join(node_pointer p_l, node_pointer p_r, size_type checked_ind, 
	 branch_bag& r_bag)
{
  _GLIBCXX_DEBUG_ASSERT(p_r != 0);
  if (p_l == 0)
    {
      apply_update(p_r, (node_update*)this);
      return (p_r);
    }

  if (p_l->m_type == leaf_node)
    {
      if (p_r->m_type == leaf_node)
	{
	  node_pointer p_ret = rec_join(static_cast<leaf_pointer>(p_l),
					static_cast<leaf_pointer>(p_r), r_bag);
	  apply_update(p_ret, (node_update*)this);
	  return p_ret;
	}

      _GLIBCXX_DEBUG_ASSERT(p_r->m_type == i_node);
      node_pointer p_ret = rec_join(static_cast<leaf_pointer>(p_l),
				    static_cast<inode_pointer>(p_r),
				    checked_ind, r_bag);
      apply_update(p_ret, (node_update*)this);
      return p_ret;
    }

  _GLIBCXX_DEBUG_ASSERT(p_l->m_type == i_node);
  if (p_r->m_type == leaf_node)
    {
      node_pointer p_ret = rec_join(static_cast<inode_pointer>(p_l),
				    static_cast<leaf_pointer>(p_r),
				    checked_ind, r_bag);
      apply_update(p_ret, (node_update*)this);
      return p_ret;
    }

  _GLIBCXX_DEBUG_ASSERT(p_r->m_type == i_node);
  node_pointer p_ret = rec_join(static_cast<inode_pointer>(p_l),
				static_cast<inode_pointer>(p_r),
				r_bag);

  apply_update(p_ret, (node_update*)this);
  return p_ret;
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
rec_join(leaf_pointer p_l, leaf_pointer p_r, branch_bag& r_bag)
{
  _GLIBCXX_DEBUG_ASSERT(p_r != 0);
  if (p_l == 0)
    return (p_r);
  node_pointer p_ret = insert_branch(p_l, p_r, r_bag);
  _GLIBCXX_DEBUG_ASSERT(PB_DS_RECURSIVE_COUNT_LEAFS(p_ret) == 2);
  return p_ret;
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
rec_join(leaf_pointer p_l, inode_pointer p_r, size_type checked_ind,
	 branch_bag& r_bag)
{
#ifdef _GLIBCXX_DEBUG
  const size_type lhs_leafs = PB_DS_RECURSIVE_COUNT_LEAFS(p_l);
  const size_type rhs_leafs = PB_DS_RECURSIVE_COUNT_LEAFS(p_r);
#endif

  _GLIBCXX_DEBUG_ASSERT(p_r != 0);
  node_pointer p_ret = rec_join(p_r, p_l, checked_ind, r_bag);
  _GLIBCXX_DEBUG_ASSERT(PB_DS_RECURSIVE_COUNT_LEAFS(p_ret) == lhs_leafs + rhs_leafs);
  return p_ret;
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
rec_join(inode_pointer p_l, leaf_pointer p_r, size_type checked_ind, branch_bag& r_bag)
{
  _GLIBCXX_DEBUG_ASSERT(p_l != 0);
  _GLIBCXX_DEBUG_ASSERT(p_r != 0);

#ifdef _GLIBCXX_DEBUG
  const size_type lhs_leafs = PB_DS_RECURSIVE_COUNT_LEAFS(p_l);
  const size_type rhs_leafs = PB_DS_RECURSIVE_COUNT_LEAFS(p_r);
#endif

  if (!p_l->should_be_mine(pref_begin(p_r), pref_end(p_r), checked_ind, this))
    {
      node_pointer p_ret = insert_branch(p_l, p_r, r_bag);
      PB_DS_ASSERT_NODE_VALID(p_ret)
      _GLIBCXX_DEBUG_ASSERT(PB_DS_RECURSIVE_COUNT_LEAFS(p_ret) ==
       			    lhs_leafs + rhs_leafs);
      return p_ret;
    }

  node_pointer p_pot_child = p_l->add_child(p_r, pref_begin(p_r),
					    pref_end(p_r), this);
  if (p_pot_child != p_r)
    {
      node_pointer p_new_child = rec_join(p_pot_child, p_r, p_l->get_e_ind(),
					  r_bag);

      p_l->replace_child(p_new_child, pref_begin(p_new_child),
			 pref_end(p_new_child), this);
    }

  PB_DS_ASSERT_NODE_VALID(p_l)
  _GLIBCXX_DEBUG_ASSERT(PB_DS_RECURSIVE_COUNT_LEAFS(p_l) == lhs_leafs + rhs_leafs);
  return p_l;
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
rec_join(inode_pointer p_l, inode_pointer p_r, 
	 branch_bag& r_bag)
{
  _GLIBCXX_DEBUG_ASSERT(p_l != 0);
  _GLIBCXX_DEBUG_ASSERT(p_r != 0);

#ifdef _GLIBCXX_DEBUG
  const size_type lhs_leafs = PB_DS_RECURSIVE_COUNT_LEAFS(p_l);
  const size_type rhs_leafs = PB_DS_RECURSIVE_COUNT_LEAFS(p_r);
#endif

  if (p_l->get_e_ind() == p_r->get_e_ind() &&
      synth_access_traits::equal_prefixes(p_l->pref_b_it(), p_l->pref_e_it(),
					    p_r->pref_b_it(), p_r->pref_e_it()))
    {
      for (typename inode::iterator it = p_r->begin();
	   it != p_r->end(); ++ it)
	{
	  node_pointer p_new_child = rec_join(p_l->get_join_child(*it, this),
					      * it, 0, r_bag);
	  p_l->replace_child(p_new_child, pref_begin(p_new_child),
			     pref_end(p_new_child), this);
	}

      p_r->~inode();
      s_inode_allocator.deallocate(p_r, 1);
      PB_DS_ASSERT_NODE_VALID(p_l)
      _GLIBCXX_DEBUG_ASSERT(PB_DS_RECURSIVE_COUNT_LEAFS(p_l) == lhs_leafs + rhs_leafs);
      return p_l;
    }

  if (p_l->get_e_ind() < p_r->get_e_ind() &&
      p_l->should_be_mine(p_r->pref_b_it(), p_r->pref_e_it(), 0, this))
    {
      node_pointer p_new_child = rec_join(p_l->get_join_child(p_r, this),
					  p_r, 0, r_bag);
      p_l->replace_child(p_new_child, pref_begin(p_new_child),
			 pref_end(p_new_child), this);
      PB_DS_ASSERT_NODE_VALID(p_l)
      return p_l;
    }

  if (p_r->get_e_ind() < p_l->get_e_ind() &&
      p_r->should_be_mine(p_l->pref_b_it(), p_l->pref_e_it(), 0, this))
    {
      node_pointer p_new_child = rec_join(p_r->get_join_child(p_l, this), p_l,
					  0, r_bag);

      p_r->replace_child(p_new_child, pref_begin(p_new_child),
			 pref_end(p_new_child), this);

      PB_DS_ASSERT_NODE_VALID(p_r)
      _GLIBCXX_DEBUG_ASSERT(PB_DS_RECURSIVE_COUNT_LEAFS(p_r) == lhs_leafs + rhs_leafs);
      return p_r;
    }

  node_pointer p_ret = insert_branch(p_l, p_r, r_bag);
  PB_DS_ASSERT_NODE_VALID(p_ret)
  _GLIBCXX_DEBUG_ASSERT(PB_DS_RECURSIVE_COUNT_LEAFS(p_ret) == lhs_leafs + rhs_leafs);
  return p_ret;
}

PB_DS_CLASS_T_DEC
inline std::pair<typename PB_DS_CLASS_C_DEC::iterator, bool>
PB_DS_CLASS_C_DEC::
insert(const_reference r_val)
{
  node_pointer p_lf = find_imp(PB_DS_V2F(r_val));
  if (p_lf != 0 && p_lf->m_type == leaf_node &&
      synth_access_traits::equal_keys(PB_DS_V2F(static_cast<leaf_pointer>(p_lf)->value()), PB_DS_V2F(r_val)))
    {
      PB_DS_CHECK_KEY_EXISTS(PB_DS_V2F(r_val))
      PB_DS_ASSERT_VALID((*this))
      return std::make_pair(iterator(p_lf), false);
    }

  PB_DS_CHECK_KEY_DOES_NOT_EXIST(PB_DS_V2F(r_val))

  leaf_pointer p_new_lf = s_leaf_allocator.allocate(1);
  cond_dealtor cond(p_new_lf);

  new (p_new_lf) leaf(r_val);
  apply_update(p_new_lf, (node_update*)this);
  cond.set_call_destructor();
  branch_bag bag;
  bag.add_branch();
  m_p_head->m_p_parent = rec_join(m_p_head->m_p_parent, p_new_lf, 0, bag);
  m_p_head->m_p_parent->m_p_parent = m_p_head;
  cond.set_no_action_dtor();
  ++m_size;
  update_min_max_for_inserted_leaf(p_new_lf);
  _GLIBCXX_DEBUG_ONLY(debug_base::insert_new(PB_DS_V2F(r_val));)
  PB_DS_ASSERT_VALID((*this))
  return std::make_pair(point_iterator(p_new_lf), true);
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
keys_diff_ind(typename access_traits::const_iterator b_l,
	      typename access_traits::const_iterator e_l,
	      typename access_traits::const_iterator b_r,
	      typename access_traits::const_iterator e_r)
{
  size_type diff_pos = 0;
  while (b_l != e_l)
    {
      if (b_r == e_r)
	return (diff_pos);
      if (access_traits::e_pos(*b_l) != access_traits::e_pos(*b_r))
	return (diff_pos);
      ++b_l;
      ++b_r;
      ++diff_pos;
    }
  _GLIBCXX_DEBUG_ASSERT(b_r != e_r);
  return diff_pos;
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::inode_pointer
PB_DS_CLASS_C_DEC::
insert_branch(node_pointer p_l, node_pointer p_r, branch_bag& r_bag)
{
  typename synth_access_traits::const_iterator left_b_it = pref_begin(p_l);
  typename synth_access_traits::const_iterator left_e_it = pref_end(p_l);
  typename synth_access_traits::const_iterator right_b_it = pref_begin(p_r);
  typename synth_access_traits::const_iterator right_e_it = pref_end(p_r);

  const size_type diff_ind = keys_diff_ind(left_b_it, left_e_it,
					   right_b_it, right_e_it);

  inode_pointer p_new_nd = r_bag.get_branch();
  new (p_new_nd) inode(diff_ind, left_b_it);
  p_new_nd->add_child(p_l, left_b_it, left_e_it, this);
  p_new_nd->add_child(p_r, right_b_it, right_e_it, this);
  p_l->m_p_parent = p_new_nd;
  p_r->m_p_parent = p_new_nd;
  PB_DS_ASSERT_NODE_VALID(p_new_nd)
  return (p_new_nd);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
update_min_max_for_inserted_leaf(leaf_pointer p_new_lf)
{
  if (m_p_head->m_p_min == m_p_head ||
      synth_access_traits::cmp_keys(PB_DS_V2F(p_new_lf->value()),
				      PB_DS_V2F(static_cast<leaf_const_pointer>(m_p_head->m_p_min)->value())))
    m_p_head->m_p_min = p_new_lf;

  if (m_p_head->m_p_max == m_p_head ||
      synth_access_traits::cmp_keys(PB_DS_V2F(static_cast<leaf_const_pointer>(m_p_head->m_p_max)->value()), PB_DS_V2F(p_new_lf->value())))
    m_p_head->m_p_max = p_new_lf;
}
#endif
