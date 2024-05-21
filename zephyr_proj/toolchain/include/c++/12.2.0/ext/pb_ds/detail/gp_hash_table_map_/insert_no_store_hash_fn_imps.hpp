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
 * @file gp_hash_table_map_/insert_no_store_hash_fn_imps.hpp
 * Contains implementations of gp_ht_map_'s insert related functions,
 * when the hash value is not stored.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
find_ins_pos(key_const_reference r_key, false_type)
{
  size_type hash = ranged_probe_fn_base::operator()(r_key);
  size_type i;

  /* The insertion position is initted to a non-legal value to indicate
   *     that it has not been initted yet.
   */
  size_type ins_pos = m_num_e;
  resize_base::notify_insert_search_start();
  for (i = 0; i < m_num_e; ++i)
    {
      const size_type pos = ranged_probe_fn_base::operator()(r_key, hash, i);
      _GLIBCXX_DEBUG_ASSERT(pos < m_num_e);
      entry* const p_e = m_entries + pos;
      switch(p_e->m_stat)
        {
        case empty_entry_status:
	  {
            resize_base::notify_insert_search_end();
	    PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
	    return (ins_pos == m_num_e) ? pos : ins_pos;
	  }
	  break;
        case erased_entry_status:
	  if (ins_pos == m_num_e)
	    ins_pos = pos;
	  break;
        case valid_entry_status:
	  if (hash_eq_fn_base::operator()(PB_DS_V2F(p_e->m_value), r_key))
            {
	      resize_base::notify_insert_search_end();
	      PB_DS_CHECK_KEY_EXISTS(r_key)
                return pos;
            }
	  break;
        default:
	  _GLIBCXX_DEBUG_ASSERT(0);
        };

      resize_base::notify_insert_search_collision();
    }
  resize_base::notify_insert_search_end();
  if (ins_pos == m_num_e)
    __throw_insert_error();
  return ins_pos;
}

PB_DS_CLASS_T_DEC
inline std::pair<typename PB_DS_CLASS_C_DEC::point_iterator, bool>
PB_DS_CLASS_C_DEC::
insert_imp(const_reference r_val, false_type)
{
  key_const_reference r_key = PB_DS_V2F(r_val);
  const size_type pos = find_ins_pos(r_key, 
				     traits_base::m_store_extra_indicator);

  if (m_entries[pos].m_stat == valid_entry_status)
    {
      PB_DS_CHECK_KEY_EXISTS(r_key)
      return std::make_pair(&(m_entries + pos)->m_value, false);
    }

  PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
  return std::make_pair(insert_new_imp(r_val, pos), true);
}

#endif
