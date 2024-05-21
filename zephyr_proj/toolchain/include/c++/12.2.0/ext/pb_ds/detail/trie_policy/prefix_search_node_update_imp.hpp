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
 * @file trie_policy/prefix_search_node_update_imp.hpp
 * Contains an implementation of prefix_search_node_update.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
std::pair<
  typename PB_DS_CLASS_C_DEC::const_iterator,
  typename PB_DS_CLASS_C_DEC::const_iterator>
PB_DS_CLASS_C_DEC::
prefix_range(key_const_reference r_key) const
{
  const access_traits& r_traits = get_access_traits();
  return (prefix_range(r_traits.begin(r_key), r_traits.end(r_key)));
}

PB_DS_CLASS_T_DEC
std::pair<
  typename PB_DS_CLASS_C_DEC::iterator,
  typename PB_DS_CLASS_C_DEC::iterator>
PB_DS_CLASS_C_DEC::
prefix_range(key_const_reference r_key)
{
  return (prefix_range(get_access_traits().begin(r_key),
		       get_access_traits().end(r_key)));
}

PB_DS_CLASS_T_DEC
std::pair<
  typename PB_DS_CLASS_C_DEC::const_iterator,
  typename PB_DS_CLASS_C_DEC::const_iterator>
PB_DS_CLASS_C_DEC::
prefix_range(typename access_traits::const_iterator b,
	     typename access_traits::const_iterator e) const
{
  const std::pair<iterator, iterator> non_const_ret =
    const_cast<PB_DS_CLASS_C_DEC* >(this)->prefix_range(b, e);

  return (std::make_pair(const_iterator(non_const_ret.first),
			 const_iterator(non_const_ret.second)));
}

PB_DS_CLASS_T_DEC
std::pair<
  typename PB_DS_CLASS_C_DEC::iterator,
  typename PB_DS_CLASS_C_DEC::iterator>
PB_DS_CLASS_C_DEC::
prefix_range(typename access_traits::const_iterator b,
	     typename access_traits::const_iterator e)
{
  Node_Itr nd_it = node_begin();
  Node_Itr end_nd_it = node_end();

  const access_traits& r_traits = get_access_traits();
  const size_type given_range_length = std::distance(b, e);

  while (true)
    {
      if (nd_it == end_nd_it)
	return (std::make_pair(end(), end()));

      const size_type common_range_length =
	base_type::common_prefix_len(nd_it, b, e, r_traits);

      if (common_range_length >= given_range_length)
	{
	  iterator ret_b = this->leftmost_it(nd_it);
	  iterator ret_e = this->rightmost_it(nd_it);
	  return (std::make_pair(ret_b, ++ret_e));
	}
      nd_it = next_child(nd_it, b, e, end_nd_it, r_traits);
    }
}

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::node_iterator
PB_DS_CLASS_C_DEC::
next_child(node_iterator nd_it, typename access_traits::const_iterator b,
	   typename access_traits::const_iterator e, node_iterator end_nd_it,
	   const access_traits& r_traits)
{
  const size_type num_children = nd_it.num_children();
  node_iterator ret = end_nd_it;
  size_type max_length = 0;
  for (size_type i = 0; i < num_children; ++i)
    {
      node_iterator pot = nd_it.get_child(i);
      const size_type common_range_length =
	base_type::common_prefix_len(pot, b, e, r_traits);

      if (common_range_length > max_length)
	{
	  ret = pot;
	  max_length = common_range_length;
	}
    }
  return (ret);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
operator()(node_iterator /*nd_it*/, node_const_iterator /*end_nd_it*/) const
{ }
#endif
