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
 * @file pat_trie_/erase_fn_imps.hpp
 * Contains an implementation class for pat_trie.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
erase(key_const_reference r_key)
{
  node_pointer p_nd = find_imp(r_key);
  if (p_nd == 0 || p_nd->m_type == i_node)
    {
      PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
      return false;
    }

  _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == leaf_node);
  if (!synth_access_traits::equal_keys(PB_DS_V2F(reinterpret_cast<leaf_pointer>(p_nd)->value()), r_key))
    {
      PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
      return false;
    }

  PB_DS_CHECK_KEY_EXISTS(r_key)
  erase_leaf(static_cast<leaf_pointer>(p_nd));
  PB_DS_ASSERT_VALID((*this))
  return true;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
erase_fixup(inode_pointer p_nd)
{
  _GLIBCXX_DEBUG_ASSERT(std::distance(p_nd->begin(), p_nd->end()) >= 1);
  if (std::distance(p_nd->begin(), p_nd->end()) == 1)
    {
      node_pointer p_parent = p_nd->m_p_parent;
      if (p_parent == m_p_head)
	m_p_head->m_p_parent = *p_nd->begin();
      else
	{
	  _GLIBCXX_DEBUG_ASSERT(p_parent->m_type == i_node);
	  node_pointer p_new_child = *p_nd->begin();

	  typedef inode_pointer inode_ptr;
	  inode_ptr p_internal = static_cast<inode_ptr>(p_parent);
	  p_internal->replace_child(p_new_child, pref_begin(p_new_child),
				    pref_end(p_new_child), this);
	}
      (*p_nd->begin())->m_p_parent = p_nd->m_p_parent;
      p_nd->~inode();
      s_inode_allocator.deallocate(p_nd, 1);

      if (p_parent == m_p_head)
	return;

      _GLIBCXX_DEBUG_ASSERT(p_parent->m_type == i_node);
      p_nd = static_cast<inode_pointer>(p_parent);
    }

  while (true)
    {
      _GLIBCXX_DEBUG_ASSERT(std::distance(p_nd->begin(), p_nd->end()) > 1);
      p_nd->update_prefixes(this);
      apply_update(p_nd, (node_update*)this);
      PB_DS_ASSERT_NODE_VALID(p_nd)
      if (p_nd->m_p_parent->m_type == head_node)
	return;

      _GLIBCXX_DEBUG_ASSERT(p_nd->m_p_parent->m_type == i_node);

      p_nd = static_cast<inode_pointer>(p_nd->m_p_parent);
    }
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
actual_erase_leaf(leaf_pointer p_l)
{
  _GLIBCXX_DEBUG_ASSERT(m_size > 0);
  --m_size;
  _GLIBCXX_DEBUG_ONLY(debug_base::erase_existing(PB_DS_V2F(p_l->value())));
  p_l->~leaf();
  s_leaf_allocator.deallocate(p_l, 1);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
clear()
{
  if (!empty())
    {
      clear_imp(m_p_head->m_p_parent);
      m_size = 0;
      initialize();
      _GLIBCXX_DEBUG_ONLY(debug_base::clear();)
      PB_DS_ASSERT_VALID((*this))
    }
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
clear_imp(node_pointer p_nd)
{
  if (p_nd->m_type == i_node)
    {
      _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == i_node);
      for (typename inode::iterator it =
	     static_cast<inode_pointer>(p_nd)->begin();
	   it != static_cast<inode_pointer>(p_nd)->end();
	   ++it)
	{
	  node_pointer p_child =* it;
	  clear_imp(p_child);
	}
      s_inode_allocator.deallocate(static_cast<inode_pointer>(p_nd), 1);
      return;
    }

  _GLIBCXX_DEBUG_ASSERT(p_nd->m_type == leaf_node);
  static_cast<leaf_pointer>(p_nd)->~leaf();
  s_leaf_allocator.deallocate(static_cast<leaf_pointer>(p_nd), 1);
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::const_iterator
PB_DS_CLASS_C_DEC::
erase(const_iterator it)
{
  PB_DS_ASSERT_VALID((*this))

  if (it == end())
    return it;

  const_iterator ret_it = it;
  ++ret_it;
  _GLIBCXX_DEBUG_ASSERT(it.m_p_nd->m_type == leaf_node);
  erase_leaf(static_cast<leaf_pointer>(it.m_p_nd));
  PB_DS_ASSERT_VALID((*this))
  return ret_it;
}

#ifdef PB_DS_DATA_TRUE_INDICATOR
PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::iterator
PB_DS_CLASS_C_DEC::
erase(iterator it)
{
  PB_DS_ASSERT_VALID((*this))

  if (it == end())
    return it;
  iterator ret_it = it;
  ++ret_it;
  _GLIBCXX_DEBUG_ASSERT(it.m_p_nd->m_type == leaf_node);
  erase_leaf(static_cast<leaf_pointer>(it.m_p_nd));
  PB_DS_ASSERT_VALID((*this))
  return ret_it;
}
#endif // #ifdef PB_DS_DATA_TRUE_INDICATOR

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::const_reverse_iterator
PB_DS_CLASS_C_DEC::
erase(const_reverse_iterator it)
{
  PB_DS_ASSERT_VALID((*this))

  if (it.m_p_nd == m_p_head)
    return it;
  const_reverse_iterator ret_it = it;
  ++ret_it;

  _GLIBCXX_DEBUG_ASSERT(it.m_p_nd->m_type == leaf_node);
  erase_leaf(static_cast<leaf_pointer>(it.m_p_nd));
  PB_DS_ASSERT_VALID((*this))
  return ret_it;
}

#ifdef PB_DS_DATA_TRUE_INDICATOR
PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::reverse_iterator
PB_DS_CLASS_C_DEC::
erase(reverse_iterator it)
{
  PB_DS_ASSERT_VALID((*this))

  if (it.m_p_nd == m_p_head)
    return it;
  reverse_iterator ret_it = it;
  ++ret_it;

  _GLIBCXX_DEBUG_ASSERT(it.m_p_nd->m_type == leaf_node);
  erase_leaf(static_cast<leaf_pointer>(it.m_p_nd));
  PB_DS_ASSERT_VALID((*this))
  return ret_it;
}
#endif // #ifdef PB_DS_DATA_TRUE_INDICATOR

PB_DS_CLASS_T_DEC
template<typename Pred>
inline typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
erase_if(Pred pred)
{
  size_type num_ersd = 0;
  PB_DS_ASSERT_VALID((*this))

  iterator it = begin();
  while (it != end())
    {
      PB_DS_ASSERT_VALID((*this))
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
erase_leaf(leaf_pointer p_l)
{
  update_min_max_for_erased_leaf(p_l);
  if (p_l->m_p_parent->m_type == head_node)
    {
      _GLIBCXX_DEBUG_ASSERT(size() == 1);
      clear();
      return;
    }

  _GLIBCXX_DEBUG_ASSERT(size() > 1);
  _GLIBCXX_DEBUG_ASSERT(p_l->m_p_parent->m_type == i_node);

  inode_pointer p_parent = static_cast<inode_pointer>(p_l->m_p_parent);

  p_parent->remove_child(p_l);
  erase_fixup(p_parent);
  actual_erase_leaf(p_l);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
update_min_max_for_erased_leaf(leaf_pointer p_l)
{
  if (m_size == 1)
    {
      m_p_head->m_p_min = m_p_head;
      m_p_head->m_p_max = m_p_head;
      return;
    }

  if (p_l == static_cast<leaf_const_pointer>(m_p_head->m_p_min))
    {
      iterator it(p_l);
      ++it;
      m_p_head->m_p_min = it.m_p_nd;
      return;
    }

  if (p_l == static_cast<leaf_const_pointer>(m_p_head->m_p_max))
    {
      iterator it(p_l);
      --it;
      m_p_head->m_p_max = it.m_p_nd;
    }
}
#endif
