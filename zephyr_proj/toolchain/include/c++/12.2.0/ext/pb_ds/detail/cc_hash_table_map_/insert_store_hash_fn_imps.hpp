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
 * @file cc_hash_table_map_/insert_store_hash_fn_imps.hpp
 * Contains implementations of cc_ht_map_'s insert related functions,
 * when the hash value is stored.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline std::pair<typename PB_DS_CLASS_C_DEC::point_iterator, bool>
PB_DS_CLASS_C_DEC::
insert_imp(const_reference r_val, true_type)
{
  PB_DS_ASSERT_VALID((*this))
  key_const_reference key = PB_DS_V2F(r_val);
  comp_hash pos_hash_pair = ranged_hash_fn_base::operator()(key);
  entry_pointer p_e = m_entries[pos_hash_pair.first];
  resize_base::notify_insert_search_start();

  while (p_e != 0 && !hash_eq_fn_base::operator()(PB_DS_V2F(p_e->m_value),
						     p_e->m_hash,
						    key, pos_hash_pair.second))
    {
      resize_base::notify_insert_search_collision();
      p_e = p_e->m_p_next;
    }

  resize_base::notify_insert_search_end();
  if (p_e != 0)
    {
      PB_DS_CHECK_KEY_EXISTS(key)
      return std::make_pair(&p_e->m_value, false);
    }

  PB_DS_CHECK_KEY_DOES_NOT_EXIST(key)
  return std::make_pair(insert_new_imp(r_val, pos_hash_pair), true);
}

#endif
