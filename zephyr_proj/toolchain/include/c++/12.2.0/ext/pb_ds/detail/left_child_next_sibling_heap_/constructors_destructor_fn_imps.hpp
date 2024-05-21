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
 * @file left_child_next_sibling_heap_/constructors_destructor_fn_imps.hpp
 * Contains an implementation class for left_child_next_sibling_heap_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_allocator
PB_DS_CLASS_C_DEC::s_node_allocator;

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::no_throw_copies_t
PB_DS_CLASS_C_DEC::s_no_throw_copies_ind;

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
left_child_next_sibling_heap() :
  m_p_root(0),
  m_size(0)
{
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
left_child_next_sibling_heap(const Cmp_Fn& r_cmp_fn) :
  Cmp_Fn(r_cmp_fn),
  m_p_root(0),
  m_size(0)
{
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
left_child_next_sibling_heap(const PB_DS_CLASS_C_DEC& other) 
: Cmp_Fn(other), m_p_root(0), m_size(0)
{
  m_size = other.m_size;
  PB_DS_ASSERT_VALID(other)
  m_p_root = recursive_copy_node(other.m_p_root);
  m_size = other.m_size;
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
  std::swap((Cmp_Fn& )(*this), (Cmp_Fn& )other);
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
value_swap(PB_DS_CLASS_C_DEC& other)
{
  std::swap(m_p_root, other.m_p_root);
  std::swap(m_size, other.m_size);
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~left_child_next_sibling_heap()
{
  clear();
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_pointer
PB_DS_CLASS_C_DEC::
recursive_copy_node(node_const_pointer p_nd)
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

  p_ret->m_p_l_child = p_ret->m_p_next_sibling =
    p_ret->m_p_prev_or_parent = 0;

  __try
    {
      p_ret->m_p_l_child = recursive_copy_node(p_nd->m_p_l_child);
      p_ret->m_p_next_sibling = recursive_copy_node(p_nd->m_p_next_sibling);
    }
  __catch(...)
    {
      clear_imp(p_ret);
      __throw_exception_again;
    }

  if (p_ret->m_p_l_child != 0)
    p_ret->m_p_l_child->m_p_prev_or_parent = p_ret;

  if (p_ret->m_p_next_sibling != 0)
    p_ret->m_p_next_sibling->m_p_prev_or_parent =
      p_nd->m_p_next_sibling->m_p_prev_or_parent == p_nd ? p_ret : 0;

  return p_ret;
}

#endif
