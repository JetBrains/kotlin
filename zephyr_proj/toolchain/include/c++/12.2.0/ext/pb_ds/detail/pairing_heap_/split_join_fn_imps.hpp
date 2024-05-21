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
 * @file pairing_heap_/split_join_fn_imps.hpp
 * Contains an implementation class for a pairing heap.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
template<typename Pred>
void
PB_DS_CLASS_C_DEC::
split(Pred pred, PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)

  other.clear();

  if (base_type::empty())
    {
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return;
    }

  base_type::to_linked_list();
  node_pointer p_out = base_type::prune(pred);
  while (p_out != 0)
    {
      _GLIBCXX_DEBUG_ASSERT(base_type::m_size > 0);
      --base_type::m_size;
      ++other.m_size;
      node_pointer p_next = p_out->m_p_next_sibling;
      p_out->m_p_l_child = p_out->m_p_next_sibling = p_out->m_p_prev_or_parent = 0;

      other.push_imp(p_out);
      p_out = p_next;
    }

  PB_DS_ASSERT_VALID(other)
  node_pointer p_cur = base_type::m_p_root;
  base_type::m_p_root = 0;
  while (p_cur != 0)
    {
      node_pointer p_next = p_cur->m_p_next_sibling;
      p_cur->m_p_l_child = p_cur->m_p_next_sibling = p_cur->m_p_prev_or_parent = 0;

      push_imp(p_cur);
      p_cur = p_next;
    }

  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
join(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)

  if (other.m_p_root == 0)
    {
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
      return;
    }

  if (base_type::m_p_root == 0)
    base_type::m_p_root = other.m_p_root;
  else if (Cmp_Fn::operator()(base_type::m_p_root->m_value, other.m_p_root->m_value))
    {
      base_type::make_child_of(base_type::m_p_root, other.m_p_root);
      PB_DS_ASSERT_NODE_CONSISTENT(other.m_p_root, false)
      base_type::m_p_root = other.m_p_root;
    }
  else
    {
      base_type::make_child_of(other.m_p_root, base_type::m_p_root);
      PB_DS_ASSERT_NODE_CONSISTENT(base_type::m_p_root, false)
    }

  base_type::m_size += other.m_size;
  other.m_p_root = 0;
  other.m_size = 0;
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}
#endif
