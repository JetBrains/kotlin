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
 * @file bin_search_tree_/iterators_fn_imps.hpp
 * Contains an implementation class for bin_search_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::iterator
PB_DS_CLASS_C_DEC::
begin()
{
  return (iterator(m_p_head->m_p_left));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::const_iterator
PB_DS_CLASS_C_DEC::
begin() const
{
  return (const_iterator(m_p_head->m_p_left));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::iterator
PB_DS_CLASS_C_DEC::
end()
{
  return (iterator(m_p_head));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::const_iterator
PB_DS_CLASS_C_DEC::
end() const
{
  return (const_iterator(m_p_head));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::const_reverse_iterator
PB_DS_CLASS_C_DEC::
rbegin() const
{
  return (const_reverse_iterator(m_p_head->m_p_right));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::reverse_iterator
PB_DS_CLASS_C_DEC::
rbegin()
{
  return (reverse_iterator(m_p_head->m_p_right));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::reverse_iterator
PB_DS_CLASS_C_DEC::
rend()
{
  return (reverse_iterator(m_p_head));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::const_reverse_iterator
PB_DS_CLASS_C_DEC::
rend() const
{
  return (const_reverse_iterator(m_p_head));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_const_iterator
PB_DS_CLASS_C_DEC::
node_begin() const
{
  return (node_const_iterator(m_p_head->m_p_parent));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_iterator
PB_DS_CLASS_C_DEC::
node_begin()
{
  return (node_iterator(m_p_head->m_p_parent));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_const_iterator
PB_DS_CLASS_C_DEC::
node_end() const
{
  return (node_const_iterator(0));
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::node_iterator
PB_DS_CLASS_C_DEC::
node_end()
{
  return (node_iterator(0));
}

#endif
