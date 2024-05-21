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
 * @file detail/tree_trace_base.hpp
 * Contains tree-related policies.
 */

#ifndef PB_DS_TREE_TRACE_BASE_HPP
#define PB_DS_TREE_TRACE_BASE_HPP

#ifdef PB_DS_TREE_TRACE

#include <ext/pb_ds/detail/branch_policy/branch_policy.hpp>
#include <ext/pb_ds/detail/branch_policy/null_node_metadata.hpp>

namespace __gnu_pbds
{
  namespace detail
  {
#ifdef PB_DS_TREE_TRACE

#define PB_DS_CLASS_T_DEC						\
    template<typename Node_CItr, typename Node_Itr,	\
	     typename Cmp_Fn, bool Node_Based, typename _Alloc>

#define PB_DS_CLASS_C_DEC						\
    tree_trace_base<Node_CItr, Node_Itr, Cmp_Fn,         \
		    Node_Based, _Alloc>

#define PB_DS_TRACE_BASE \
    branch_policy<Node_CItr, Node_Itr, _Alloc>

    /// Tracing base class.
    template<typename Node_CItr, typename Node_Itr,
	     typename Cmp_Fn, bool Node_Based, typename _Alloc>
    class tree_trace_base : private PB_DS_TRACE_BASE
    {
    public:
      void
      trace() const;

    private:
      typedef PB_DS_TRACE_BASE 			base_type;
      typedef Node_CItr 		node_const_iterator;
      typedef typename _Alloc::size_type 	size_type;

      void
      trace_node(node_const_iterator, size_type) const;

      _GLIBCXX_NODISCARD virtual bool
      empty() const = 0;

      virtual node_const_iterator
      node_begin() const = 0;

      virtual node_const_iterator
      node_end() const = 0;

      static void
      print_node_pointer(Node_CItr, integral_constant<int,true>);

      static void
      print_node_pointer(Node_CItr, integral_constant<int,false>);

      template<typename Metadata_>
      static void
      trace_it_metadata(Node_CItr, type_to_type<Metadata_>);

      static void
      trace_it_metadata(Node_CItr, type_to_type<null_type>);
    };

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    trace() const
    {
      if (empty())
	return;
      trace_node(node_begin(), 0);
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    trace_node(node_const_iterator nd_it, size_type level) const
    {
      if (nd_it.get_r_child() != node_end())
	trace_node(nd_it.get_r_child(), level + 1);

      for (size_type i = 0; i < level; ++i)
	std::cerr << ' ';

      print_node_pointer(nd_it, integral_constant<int,Node_Based>());
      std::cerr << base_type::extract_key(*(*nd_it));

      typedef type_to_type<typename node_const_iterator::metadata_type>
	m_type_ind_t;

      trace_it_metadata(nd_it, m_type_ind_t());

      std::cerr << std::endl;

      if (nd_it.get_l_child() != node_end())
	trace_node(nd_it.get_l_child(), level + 1);
    }

    PB_DS_CLASS_T_DEC
    template<typename Metadata_>
    void
    PB_DS_CLASS_C_DEC::
    trace_it_metadata(Node_CItr nd_it, type_to_type<Metadata_>)
    {
      const unsigned long ul = static_cast<unsigned long>(nd_it.get_metadata());
      std::cerr << " (" << ul << ") ";
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    trace_it_metadata(Node_CItr, type_to_type<null_type>)
    { }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    print_node_pointer(Node_CItr nd_it, integral_constant<int,true>)
    { std::cerr << nd_it.m_p_nd << " "; }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    print_node_pointer(Node_CItr nd_it, integral_constant<int,false>)
    { std::cerr << *nd_it << " "; }

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_TRACE_BASE
#endif // #ifdef    PB_DS_TREE_TRACE

  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifdef PB_DS_TREE_TRACE

#endif // #ifndef PB_DS_TREE_TRACE_BASE_HPP
