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
 * @file rc_binomial_heap_/erase_fn_imps.hpp
 * Contains an implementation for rc_binomial_heap_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
pop()
{
  make_binomial_heap();
  _GLIBCXX_DEBUG_ASSERT(!base_type::empty());
  base_type::pop();
  base_type::find_max();
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
clear()
{
  base_type::clear();
  m_rc.clear();
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
make_binomial_heap()
{
  node_pointer p_nd = base_type::m_p_root;
  while (p_nd != 0)
    {
      node_pointer p_next = p_nd->m_p_next_sibling;
      if (p_next == 0)
	p_nd = p_next;
      else if (p_nd->m_metadata == p_next->m_metadata)
	p_nd = link_with_next_sibling(p_nd);
      else if (p_nd->m_metadata < p_next->m_metadata)
	p_nd = p_next;
#ifdef _GLIBCXX_DEBUG
      else
	_GLIBCXX_DEBUG_ASSERT(0);
#endif 
    }

  m_rc.clear();
}

PB_DS_CLASS_T_DEC
template<typename Pred>
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
erase_if(Pred pred)
{
  make_binomial_heap();
  const size_type ersd = base_type::erase_if(pred);
  base_type::find_max();
  PB_DS_ASSERT_VALID((*this))
  return ersd;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
erase(point_iterator it)
{
  make_binomial_heap();
  base_type::erase(it);
  base_type::find_max();
}

#endif
