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
 * @file bin_search_tree_/debug_fn_imps.hpp
 * Contains an implementation class for bin_search_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

#ifdef _GLIBCXX_DEBUG

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_valid(const char* __file, int __line) const
{
  structure_only_assert_valid(__file, __line);
  assert_consistent_with_debug_base(__file, __line);
  assert_size(__file, __line);
  assert_iterators(__file, __line);
  if (m_p_head->m_p_parent == 0)
    {
      PB_DS_DEBUG_VERIFY(m_size == 0);
    }
  else
    {
      PB_DS_DEBUG_VERIFY(m_size > 0);
    }
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
structure_only_assert_valid(const char* __file, int __line) const
{
  PB_DS_DEBUG_VERIFY(m_p_head != 0);
  if (m_p_head->m_p_parent == 0)
    {
      PB_DS_DEBUG_VERIFY(m_p_head->m_p_left == m_p_head);
      PB_DS_DEBUG_VERIFY(m_p_head->m_p_right == m_p_head);
    }
  else
    {
      PB_DS_DEBUG_VERIFY(m_p_head->m_p_parent->m_p_parent == m_p_head);
      PB_DS_DEBUG_VERIFY(m_p_head->m_p_left != m_p_head);
      PB_DS_DEBUG_VERIFY(m_p_head->m_p_right != m_p_head);
    }

  if (m_p_head->m_p_parent != 0)
    assert_node_consistent(m_p_head->m_p_parent, __file, __line);
  assert_min(__file, __line);
  assert_max(__file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_node_consistent(const node_pointer p_nd,
		       const char* __file, int __line) const
{
  assert_node_consistent_(p_nd, __file, __line);
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_consistent_t
PB_DS_CLASS_C_DEC::
assert_node_consistent_(const node_pointer p_nd,
			const char* __file, int __line) const
{
  if (p_nd == 0)
    return (std::make_pair((const_pointer)0,(const_pointer)0));

  assert_node_consistent_with_left(p_nd, __file, __line);
  assert_node_consistent_with_right(p_nd, __file, __line);

  const std::pair<const_pointer, const_pointer>
    l_range = assert_node_consistent_(p_nd->m_p_left, __file, __line);

  if (l_range.second != 0)
    PB_DS_DEBUG_VERIFY(Cmp_Fn::operator()(PB_DS_V2F(*l_range.second),
					  PB_DS_V2F(p_nd->m_value)));

  const std::pair<const_pointer, const_pointer>
    r_range = assert_node_consistent_(p_nd->m_p_right, __file, __line);

  if (r_range.first != 0)
    PB_DS_DEBUG_VERIFY(Cmp_Fn::operator()(PB_DS_V2F(p_nd->m_value),
					     PB_DS_V2F(*r_range.first)));

  return std::make_pair((l_range.first != 0) ? l_range.first : &p_nd->m_value,
			(r_range.second != 0)? r_range.second : &p_nd->m_value);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_node_consistent_with_left(const node_pointer p_nd,
				 const char* __file, int __line) const
{
  if (p_nd->m_p_left == 0)
    return;
  PB_DS_DEBUG_VERIFY(p_nd->m_p_left->m_p_parent == p_nd);
  PB_DS_DEBUG_VERIFY(!Cmp_Fn::operator()(PB_DS_V2F(p_nd->m_value),
					 PB_DS_V2F(p_nd->m_p_left->m_value)));
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_node_consistent_with_right(const node_pointer p_nd,
				  const char* __file, int __line) const
{
  if (p_nd->m_p_right == 0)
    return;
  PB_DS_DEBUG_VERIFY(p_nd->m_p_right->m_p_parent == p_nd);
  PB_DS_DEBUG_VERIFY(!Cmp_Fn::operator()(PB_DS_V2F(p_nd->m_p_right->m_value),
					 PB_DS_V2F(p_nd->m_value)));
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_min(const char* __file, int __line) const
{
  assert_min_imp(m_p_head->m_p_parent, __file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_min_imp(const node_pointer p_nd, const char* __file, int __line) const
{
  if (p_nd == 0)
    {
      PB_DS_DEBUG_VERIFY(m_p_head->m_p_left == m_p_head);
      return;
    }

  if (p_nd->m_p_left == 0)
    {
      PB_DS_DEBUG_VERIFY(p_nd == m_p_head->m_p_left);
      return;
    }
  assert_min_imp(p_nd->m_p_left, __file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_max(const char* __file, int __line) const
{
  assert_max_imp(m_p_head->m_p_parent, __file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_max_imp(const node_pointer p_nd,
	       const char* __file, int __line) const
{
  if (p_nd == 0)
    {
      PB_DS_DEBUG_VERIFY(m_p_head->m_p_right == m_p_head);
      return;
    }

  if (p_nd->m_p_right == 0)
    {
      PB_DS_DEBUG_VERIFY(p_nd == m_p_head->m_p_right);
      return;
    }

  assert_max_imp(p_nd->m_p_right, __file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_iterators(const char* __file, int __line) const
{
  size_type iterated_num = 0;
  const_iterator prev_it = end();
  for (const_iterator it = begin(); it != end(); ++it)
    {
      ++iterated_num;
      PB_DS_DEBUG_VERIFY(lower_bound(PB_DS_V2F(*it)).m_p_nd == it.m_p_nd);
      const_iterator upper_bound_it = upper_bound(PB_DS_V2F(*it));
      --upper_bound_it;
      PB_DS_DEBUG_VERIFY(upper_bound_it.m_p_nd == it.m_p_nd);

      if (prev_it != end())
	PB_DS_DEBUG_VERIFY(Cmp_Fn::operator()(PB_DS_V2F(*prev_it),
					      PB_DS_V2F(*it)));
      prev_it = it;
    }

  PB_DS_DEBUG_VERIFY(iterated_num == m_size);
  size_type reverse_iterated_num = 0;
  const_reverse_iterator reverse_prev_it = rend();
  for (const_reverse_iterator reverse_it = rbegin(); reverse_it != rend();
       ++reverse_it)
    {
      ++reverse_iterated_num;
      PB_DS_DEBUG_VERIFY(lower_bound(
				   PB_DS_V2F(*reverse_it)).m_p_nd == reverse_it.m_p_nd);

      const_iterator upper_bound_it = upper_bound(PB_DS_V2F(*reverse_it));
      --upper_bound_it;
      PB_DS_DEBUG_VERIFY(upper_bound_it.m_p_nd == reverse_it.m_p_nd);
      if (reverse_prev_it != rend())
	PB_DS_DEBUG_VERIFY(!Cmp_Fn::operator()(PB_DS_V2F(*reverse_prev_it),
					       PB_DS_V2F(*reverse_it)));
      reverse_prev_it = reverse_it;
    }
  PB_DS_DEBUG_VERIFY(reverse_iterated_num == m_size);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_consistent_with_debug_base(const char* __file, int __line) const
{
  debug_base::check_size(m_size, __file, __line);
  assert_consistent_with_debug_base(m_p_head->m_p_parent, __file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_consistent_with_debug_base(const node_pointer p_nd,
				  const char* __file, int __line) const
{
  if (p_nd == 0)
    return;
  debug_base::check_key_exists(PB_DS_V2F(p_nd->m_value), __file, __line);
  assert_consistent_with_debug_base(p_nd->m_p_left, __file, __line);
  assert_consistent_with_debug_base(p_nd->m_p_right, __file, __line);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_size(const char* __file, int __line) const
{ PB_DS_DEBUG_VERIFY(recursive_count(m_p_head->m_p_parent) == m_size); }

#endif
#endif
