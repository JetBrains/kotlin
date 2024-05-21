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
 * @file tree_policy/order_statistics_imp.hpp
 * Contains forward declarations for order_statistics_key
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::iterator
PB_DS_CLASS_C_DEC::
find_by_order(size_type order)
{
  node_iterator it = node_begin();
  node_iterator end_it = node_end();

  while (it != end_it)
    {
      node_iterator l_it = it.get_l_child();
      const size_type o = (l_it == end_it)? 0 : l_it.get_metadata();

      if (order == o)
	return *it;
      else if (order < o)
	it = l_it;
      else
        {
	  order -= o + 1;
	  it = it.get_r_child();
        }
    }

  return base_type::end_iterator();
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::const_iterator
PB_DS_CLASS_C_DEC::
find_by_order(size_type order) const
{ return const_cast<PB_DS_CLASS_C_DEC*>(this)->find_by_order(order); }

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
order_of_key(key_const_reference r_key) const
{
  node_const_iterator it = node_begin();
  node_const_iterator end_it = node_end();

  const cmp_fn& r_cmp_fn = const_cast<PB_DS_CLASS_C_DEC*>(this)->get_cmp_fn();
  size_type ord = 0;
  while (it != end_it)
    {
      node_const_iterator l_it = it.get_l_child();

      if (r_cmp_fn(r_key, this->extract_key(*(*it))))
	it = l_it;
      else if (r_cmp_fn(this->extract_key(*(*it)), r_key))
        {
	  ord += (l_it == end_it)? 1 : 1 + l_it.get_metadata();
	  it = it.get_r_child();
        }
      else
        {
	  ord += (l_it == end_it)? 0 : l_it.get_metadata();
	  it = end_it;
        }
    }
  return ord;
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
operator()(node_iterator node_it, node_const_iterator end_nd_it) const
{
  node_iterator l_it = node_it.get_l_child();
  const size_type l_rank = (l_it == end_nd_it) ? 0 : l_it.get_metadata();

  node_iterator r_it = node_it.get_r_child();
  const size_type r_rank = (r_it == end_nd_it) ? 0 : r_it.get_metadata();

  const_cast<metadata_reference>(node_it.get_metadata())= 1 + l_rank + r_rank;
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~tree_order_statistics_node_update()
{ }
#endif
