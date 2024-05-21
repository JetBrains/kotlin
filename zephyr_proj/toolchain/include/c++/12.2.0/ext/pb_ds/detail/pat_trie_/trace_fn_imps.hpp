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
 * @file pat_trie_/trace_fn_imps.hpp
 * Contains an implementation class for pat_trie_.
 */

#ifdef PB_DS_CLASS_C_DEC

#ifdef PB_DS_PAT_TRIE_TRACE_

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
trace() const
{
  std::cerr << std::endl;
  if (m_p_head->m_p_parent == 0)
    return;
  trace_node(m_p_head->m_p_parent, 0);
  std::cerr << std::endl;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
trace_node(node_const_pointer p_nd, size_type level)
{
  for (size_type i = 0; i < level; ++i)
    std::cerr << ' ';
  std::cerr << p_nd << " ";
  std::cerr << ((p_nd->m_type == pat_trie_leaf_node_type) ? "l " : "i ");

  trace_node_metadata(p_nd, type_to_type<typename node::metadata_type>());
  typename access_traits::const_iterator el_it = pref_begin(p_nd);
  while (el_it != pref_end(p_nd))
    {
      std::cerr <<* el_it;
      ++el_it;
    }

  if (p_nd->m_type == pat_trie_leaf_node_type)
    {
      std::cerr << std::endl;
      return;
    }

  inode_const_pointer p_internal = static_cast<inode_const_pointer>(p_nd);

  std::cerr << " " <<
    static_cast<unsigned long>(p_internal->get_e_ind()) << std::endl;

  const size_type num_children = std::distance(p_internal->begin(),
					       p_internal->end());

  for (size_type child_i = 0; child_i < num_children; ++child_i)
    {
      typename inode::const_iterator child_it = p_internal->begin();
      std::advance(child_it, num_children - child_i - 1);
      trace_node(*child_it, level + 1);
    }
}

PB_DS_CLASS_T_DEC
template<typename Metadata_>
void
PB_DS_CLASS_C_DEC::
trace_node_metadata(node_const_pointer p_nd, type_to_type<Metadata_>)
{
  std::cerr << "(" << static_cast<unsigned long>(p_nd->get_metadata()) << ") ";
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
trace_node_metadata(node_const_pointer, type_to_type<null_type>)
{ }

#endif 

#endif
