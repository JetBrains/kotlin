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
 * @file trie_policy/trie_policy_base.hpp
 * Contains an implementation of trie_policy_base.
 */

#ifndef PB_DS_TRIE_POLICY_BASE_HPP
#define PB_DS_TRIE_POLICY_BASE_HPP

#include <ext/pb_ds/detail/branch_policy/branch_policy.hpp>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Base class for trie policies.
    template<typename Node_CItr, typename Node_Itr,
	     typename _ATraits, typename _Alloc>
    class trie_policy_base
    : public branch_policy<Node_CItr, Node_Itr, _Alloc>
    {
      typedef branch_policy<Node_CItr, Node_Itr, _Alloc> base_type;

    public:
      typedef _ATraits 				access_traits;
      typedef _Alloc 					allocator_type;
      typedef typename allocator_type::size_type 	size_type;
      typedef null_type 				metadata_type;
      typedef Node_CItr 				node_const_iterator;
      typedef Node_Itr 					node_iterator;
      typedef typename node_const_iterator::value_type 	const_iterator;
      typedef typename node_iterator::value_type 	iterator;
      typedef typename base_type::key_type 		key_type;
      typedef typename base_type::key_const_reference 	key_const_reference;

    protected:
      virtual const_iterator
      end() const = 0;

      virtual iterator
      end() = 0;

      virtual node_const_iterator
      node_begin() const = 0;

      virtual node_iterator
      node_begin() = 0;

      virtual node_const_iterator
      node_end() const = 0;

      virtual node_iterator
      node_end() = 0;

      virtual const access_traits&
      get_access_traits() const = 0;

    private:
      typedef typename access_traits::const_iterator 	e_const_iterator;
      typedef std::pair<e_const_iterator, e_const_iterator> prefix_range_t;

    protected:
      static size_type
      common_prefix_len(node_iterator, e_const_iterator,
			e_const_iterator, const access_traits&);

      static iterator
      leftmost_it(node_iterator);

      static iterator
      rightmost_it(node_iterator);

      static bool
      less(e_const_iterator, e_const_iterator, e_const_iterator,
	   e_const_iterator, const access_traits&);
    };


#define PB_DS_CLASS_T_DEC \
    template<typename Node_CItr, typename Node_Itr, \
	     typename _ATraits, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
    trie_policy_base<Node_CItr, Node_Itr, _ATraits, _Alloc>

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::size_type
    PB_DS_CLASS_C_DEC::
    common_prefix_len(node_iterator nd_it, e_const_iterator b_r,
		      e_const_iterator e_r, const access_traits& r_traits)
    {
      prefix_range_t pref_range = nd_it.valid_prefix();

      e_const_iterator b_l = pref_range.first;
      e_const_iterator e_l = pref_range.second;

      const size_type range_length_l = std::distance(b_l, e_l);
      const size_type range_length_r = std::distance(b_r, e_r);

      if (range_length_r < range_length_l)
	{
	  std::swap(b_l, b_r);
	  std::swap(e_l, e_r);
	}

      size_type ret = 0;
      while (b_l != e_l)
	{
	  if (r_traits.e_pos(*b_l) != r_traits.e_pos(*b_r))
	    return ret;

	  ++ret;
	  ++b_l;
	  ++b_r;
	}

      return ret;
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::iterator
    PB_DS_CLASS_C_DEC::
    leftmost_it(node_iterator nd_it)
    {
      if (nd_it.num_children() == 0)
	return *nd_it;

      return leftmost_it(nd_it.get_child(0));
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::iterator
    PB_DS_CLASS_C_DEC::
    rightmost_it(node_iterator nd_it)
    {
      const size_type num_children = nd_it.num_children();

      if (num_children == 0)
	return *nd_it;

      return rightmost_it(nd_it.get_child(num_children - 1));
    }

    PB_DS_CLASS_T_DEC
    bool
    PB_DS_CLASS_C_DEC::
    less(e_const_iterator b_l, e_const_iterator e_l,
	 e_const_iterator b_r, e_const_iterator e_r,
	 const access_traits& r_traits)
    {
      while (b_l != e_l)
	{
	  if (b_r == e_r)
	    return false;

	  size_type l_pos = r_traits.e_pos(*b_l);
	  size_type r_pos = r_traits.e_pos(*b_r);
	  if (l_pos != r_pos)
	    return (l_pos < r_pos);

	  ++b_l;
	  ++b_r;
	}
      return b_r != e_r;
    }

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifndef PB_DS_TRIE_POLICY_BASE_HPP
