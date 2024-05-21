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
 * @file gp_hash_table_map_/resize_no_store_hash_fn_imps.hpp
 * Contains implementations of gp_ht_map_'s resize related functions, when the
 * hash value is not stored.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
resize_imp_reassign(entry_pointer p_e, entry_array a_entries_resized, 
		    false_type)
{
  key_const_reference r_key = PB_DS_V2F(p_e->m_value);
  size_type hash = ranged_probe_fn_base::operator()(r_key);
  size_type i;
  for (i = 0; i < m_num_e; ++i)
    {
      const size_type pos = ranged_probe_fn_base::operator()(r_key, hash, i);
      entry_pointer p_new_e = a_entries_resized + pos;
      switch(p_new_e->m_stat)
        {
        case empty_entry_status:
	  new (&p_new_e->m_value) value_type(p_e->m_value);
	  p_new_e->m_stat = valid_entry_status;
	  return;
        case erased_entry_status:
	  _GLIBCXX_DEBUG_ASSERT(0);
	  break;
        case valid_entry_status:
	  break;
        default:
	  _GLIBCXX_DEBUG_ASSERT(0);
        };
    }
  __throw_insert_error();
}

#endif
