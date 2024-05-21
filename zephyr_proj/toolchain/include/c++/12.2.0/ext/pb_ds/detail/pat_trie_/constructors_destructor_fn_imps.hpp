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
 * @file pat_trie_/constructors_destructor_fn_imps.hpp
 * Contains an implementation class for pat_trie.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::head_allocator
PB_DS_CLASS_C_DEC::s_head_allocator;

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::inode_allocator
PB_DS_CLASS_C_DEC::s_inode_allocator;

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::leaf_allocator
PB_DS_CLASS_C_DEC::s_leaf_allocator;

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_PAT_TRIE_NAME() :
  m_p_head(s_head_allocator.allocate(1)),
  m_size(0)
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_PAT_TRIE_NAME(const access_traits& r_access_traits) :
  synth_access_traits(r_access_traits),
  m_p_head(s_head_allocator.allocate(1)),
  m_size(0)
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_PAT_TRIE_NAME(const PB_DS_CLASS_C_DEC& other) :
#ifdef _GLIBCXX_DEBUG
  debug_base(other),
#endif
  synth_access_traits(other),
  node_update(other),
  m_p_head(s_head_allocator.allocate(1)),
  m_size(0)
{
  initialize();
  m_size = other.m_size;
  PB_DS_ASSERT_VALID(other)
    if (other.m_p_head->m_p_parent == 0)
      {
	PB_DS_ASSERT_VALID((*this))
	return;
      }
  __try
    {
      m_p_head->m_p_parent = recursive_copy_node(other.m_p_head->m_p_parent);
    }
  __catch(...)
    {
      s_head_allocator.deallocate(m_p_head, 1);
      __throw_exception_again;
    }

  m_p_head->m_p_min = leftmost_descendant(m_p_head->m_p_parent);
  m_p_head->m_p_max = rightmost_descendant(m_p_head->m_p_parent);
  m_p_head->m_p_parent->m_p_parent = m_p_head;
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
  value_swap(other);
  std::swap((access_traits& )(*this), (access_traits& )other);
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
value_swap(PB_DS_CLASS_C_DEC& other)
{
  _GLIBCXX_DEBUG_ONLY(debug_base::swap(other);)
  std::swap(m_p_head, other.m_p_head);
  std::swap(m_size, other.m_size);
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~PB_DS_PAT_TRIE_NAME()
{
  clear();
  s_head_allocator.deallocate(m_p_head, 1);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
initialize()
{
  new (m_p_head) head();
  m_p_head->m_p_parent = 0;
  m_p_head->m_p_min = m_p_head;
  m_p_head->m_p_max = m_p_head;
  m_size = 0;
}

PB_DS_CLASS_T_DEC
template<typename It>
void
PB_DS_CLASS_C_DEC::
copy_from_range(It first_it, It last_it)
{
  while (first_it != last_it)
    insert(*(first_it++));
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
recursive_copy_node(node_const_pointer p_ncp)
{
  _GLIBCXX_DEBUG_ASSERT(p_ncp != 0);
  if (p_ncp->m_type == leaf_node)
    {
      leaf_const_pointer p_other_lf = static_cast<leaf_const_pointer>(p_ncp);
      leaf_pointer p_new_lf = s_leaf_allocator.allocate(1);
      cond_dealtor cond(p_new_lf);
      new (p_new_lf) leaf(p_other_lf->value());
      apply_update(p_new_lf, (node_update*)this);
      cond.set_no_action_dtor();
      return (p_new_lf);
    }

  _GLIBCXX_DEBUG_ASSERT(p_ncp->m_type == i_node);
  node_pointer a_p_children[inode::arr_size];
  size_type child_i = 0;
  inode_const_pointer p_icp = static_cast<inode_const_pointer>(p_ncp);

  typename inode::const_iterator child_it = p_icp->begin();

  inode_pointer p_ret;
  __try
    {
      while (child_it != p_icp->end())
	{
	  a_p_children[child_i] = recursive_copy_node(*(child_it));
	  child_i++;
	  child_it++;
	}
      p_ret = s_inode_allocator.allocate(1);
    }
  __catch(...)
    {
      while (child_i-- > 0)
	clear_imp(a_p_children[child_i]);
      __throw_exception_again;
    }

  new (p_ret) inode(p_icp->get_e_ind(), pref_begin(a_p_children[0]));

  --child_i;
  _GLIBCXX_DEBUG_ASSERT(child_i >= 1);
  do
    p_ret->add_child(a_p_children[child_i], pref_begin(a_p_children[child_i]),
		     pref_end(a_p_children[child_i]), this);
  while (child_i-- > 0);
  apply_update(p_ret, (node_update*)this);
  return p_ret;
}
#endif
