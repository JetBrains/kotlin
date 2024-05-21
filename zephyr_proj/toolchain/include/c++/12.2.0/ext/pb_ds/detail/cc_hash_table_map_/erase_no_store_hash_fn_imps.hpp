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
 * @file cc_hash_table_map_/erase_no_store_hash_fn_imps.hpp
 * Contains implementations of cc_ht_map_'s erase related functions,
 * when the hash value is not stored.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
erase(key_const_reference r_key)
{
  PB_DS_ASSERT_VALID((*this))
  return erase_in_pos_imp(r_key, ranged_hash_fn_base::operator()(r_key));
}

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
erase_in_pos_imp(key_const_reference r_key, size_type pos)
{
  PB_DS_ASSERT_VALID((*this))
  entry_pointer p_e = m_entries[pos];
  resize_base::notify_erase_search_start();
  if (p_e == 0)
    {
      resize_base::notify_erase_search_end();
      PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
      PB_DS_ASSERT_VALID((*this))
      return false;
    }

  if (hash_eq_fn_base::operator()(PB_DS_V2F(p_e->m_value), r_key))
    {
      resize_base::notify_erase_search_end();
      PB_DS_CHECK_KEY_EXISTS(r_key)
      erase_entry_pointer(m_entries[pos]);
      do_resize_if_needed_no_throw();
      PB_DS_ASSERT_VALID((*this))
      return true;
    }

  while (true)
    {
      entry_pointer p_next_e = p_e->m_p_next;
      if (p_next_e == 0)
	{
	  resize_base::notify_erase_search_end();
	  PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
	  PB_DS_ASSERT_VALID((*this))
	  return false;
	}

      if (hash_eq_fn_base::operator()(PB_DS_V2F(p_next_e->m_value), r_key))
	{
	  resize_base::notify_erase_search_end();
	  PB_DS_CHECK_KEY_EXISTS(r_key)
	  erase_entry_pointer(p_e->m_p_next);
	  do_resize_if_needed_no_throw();
	  PB_DS_ASSERT_VALID((*this))
	  return true;
	}
      resize_base::notify_erase_search_collision();
      p_e = p_next_e;
    }
}
#endif
