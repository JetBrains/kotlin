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
 * @file bin_search_tree_/constructors_destructor_fn_imps.hpp
 * Contains an implementation class for bin_search_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_allocator
PB_DS_CLASS_C_DEC::s_node_allocator;

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_BIN_TREE_NAME() : m_p_head(s_node_allocator.allocate(1)), m_size(0)
{
  initialize();
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_BIN_TREE_NAME(const Cmp_Fn& r_cmp_fn) :
  Cmp_Fn(r_cmp_fn), m_p_head(s_node_allocator.allocate(1)), m_size(0)
{
  initialize();
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_BIN_TREE_NAME(const Cmp_Fn& r_cmp_fn, const node_update& r_node_update) :
  Cmp_Fn(r_cmp_fn),
  node_update(r_node_update),
  m_p_head(s_node_allocator.allocate(1)),
  m_size(0)
{
  initialize();
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_BIN_TREE_NAME(const PB_DS_CLASS_C_DEC& other) :
#ifdef _GLIBCXX_DEBUG
  debug_base(other),
#endif 
#ifdef PB_DS_TREE_TRACE
  PB_DS_TREE_TRACE_BASE_C_DEC(other),
#endif 
  Cmp_Fn(other),
  node_update(other),
  m_p_head(s_node_allocator.allocate(1)),
  m_size(0)
{
  initialize();
  m_size = other.m_size;
  PB_DS_STRUCT_ONLY_ASSERT_VALID(other)

    __try
      {
        m_p_head->m_p_parent = recursive_copy_node(other.m_p_head->m_p_parent);
        if (m_p_head->m_p_parent != 0)
	  m_p_head->m_p_parent->m_p_parent = m_p_head;
        m_size = other.m_size;
        initialize_min_max();
      }
    __catch(...)
      {
        _GLIBCXX_DEBUG_ONLY(debug_base::clear();)
	s_node_allocator.deallocate(m_p_head, 1);
        __throw_exception_again;
      }
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
  PB_DS_STRUCT_ONLY_ASSERT_VALID(other)
  value_swap(other);
  std::swap((Cmp_Fn& )(*this), (Cmp_Fn& )other);
  PB_DS_STRUCT_ONLY_ASSERT_VALID((*this))
  PB_DS_STRUCT_ONLY_ASSERT_VALID(other)
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
~PB_DS_BIN_TREE_NAME()
{
  clear();
  s_node_allocator.deallocate(m_p_head, 1);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
initialize()
{
  m_p_head->m_p_parent = 0;
  m_p_head->m_p_left = m_p_head;
  m_p_head->m_p_right = m_p_head;
  m_size = 0;
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
recursive_copy_node(const node_pointer p_nd)
{
  if (p_nd == 0)
    return (0);

  node_pointer p_ret = s_node_allocator.allocate(1);
  __try
    {
      new (p_ret) node(*p_nd);
    }
  __catch(...)
    {
      s_node_allocator.deallocate(p_ret, 1);
      __throw_exception_again;
    }

  p_ret->m_p_left = p_ret->m_p_right = 0;

  __try
    {
      p_ret->m_p_left = recursive_copy_node(p_nd->m_p_left);
      p_ret->m_p_right = recursive_copy_node(p_nd->m_p_right);
    }
  __catch(...)
    {
      clear_imp(p_ret);
      __throw_exception_again;
    }

  if (p_ret->m_p_left != 0)
    p_ret->m_p_left->m_p_parent = p_ret;

  if (p_ret->m_p_right != 0)
    p_ret->m_p_right->m_p_parent = p_ret;

  PB_DS_ASSERT_NODE_CONSISTENT(p_ret)
  return p_ret;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
initialize_min_max()
{
  if (m_p_head->m_p_parent == 0)
    {
      m_p_head->m_p_left = m_p_head->m_p_right = m_p_head;
      return;
    }

  {
    node_pointer p_min = m_p_head->m_p_parent;
    while (p_min->m_p_left != 0)
      p_min = p_min->m_p_left;
    m_p_head->m_p_left = p_min;
  }

  {
    node_pointer p_max = m_p_head->m_p_parent;
    while (p_max->m_p_right != 0)
      p_max = p_max->m_p_right;
    m_p_head->m_p_right = p_max;
  }
}
#endif
