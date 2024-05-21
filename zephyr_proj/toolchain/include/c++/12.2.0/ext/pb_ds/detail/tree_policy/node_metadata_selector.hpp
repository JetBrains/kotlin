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
 * @file tree_policy/node_metadata_selector.hpp
 * Contains an implementation class for trees.
 */

#ifndef PB_DS_TREE_NODE_METADATA_DISPATCH_HPP
#define PB_DS_TREE_NODE_METADATA_DISPATCH_HPP

#include <ext/pb_ds/detail/branch_policy/null_node_metadata.hpp>
#include <ext/pb_ds/detail/types_traits.hpp>

namespace __gnu_pbds
{
  namespace detail
  {
    /**
     *  @addtogroup traits Traits
     *  @{
     */

    /// Tree metadata helper.
    template<typename Node_Update, bool _BTp>
      struct tree_metadata_helper;

    /// Specialization, false.
    template<typename Node_Update>
      struct tree_metadata_helper<Node_Update, false>
      {
	typedef typename Node_Update::metadata_type 	type;
      };

    /// Specialization, true.
    template<typename Node_Update>
      struct tree_metadata_helper<Node_Update, true>
      {
	typedef null_type 				type;
      };

    /// Tree node metadata dispatch.
    template<typename Key,
	     typename Data,
	     typename Cmp_Fn,
	     template<typename Node_CItr,
		      typename Const_Iterator,
		      typename Cmp_Fn_,
		      typename _Alloc_>
	     class Node_Update,
	     typename _Alloc>
    struct tree_node_metadata_dispatch
    {
    private:
      typedef dumnode_const_iterator<Key, Data, _Alloc>		__it_type;
      typedef Node_Update<__it_type, __it_type, Cmp_Fn, _Alloc>	__node_u;
      typedef null_node_update<__it_type, __it_type, Cmp_Fn, _Alloc> __nnode_u;

      enum
	{
	  null_update = is_same<__node_u, __nnode_u>::value
	};

    public:
      typedef typename tree_metadata_helper<__node_u, null_update>::type type;
    };
    ///@}
  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifndef PB_DS_TREE_NODE_METADATA_DISPATCH_HPP
