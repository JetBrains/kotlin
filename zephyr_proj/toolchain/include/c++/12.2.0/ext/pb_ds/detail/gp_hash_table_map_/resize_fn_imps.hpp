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
 * @file gp_hash_table_map_/resize_fn_imps.hpp
 * Contains implementations of gp_ht_map_'s resize related functions.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
do_resize_if_needed()
{
  if (!resize_base::is_resize_needed())
    return false;
  resize_imp(resize_base::get_new_size(m_num_e, m_num_used_e));
  return true;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
do_resize(size_type n)
{ resize_imp(resize_base::get_nearest_larger_size(n)); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
do_resize_if_needed_no_throw()
{
  if (!resize_base::is_resize_needed())
    return;

  __try
    {
      resize_imp(resize_base::get_new_size(m_num_e, m_num_used_e));
    }
  __catch(...)
    { }

  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
resize_imp(size_type new_size)
{
#ifdef PB_DS_REGRESSION
  typename _Alloc::group_adjustor adjust(m_num_e);
#endif 

  if (new_size == m_num_e)
    return;

  PB_DS_ASSERT_VALID((*this))
  const size_type old_size = m_num_e;
  entry_array a_entries_resized = 0;

  // Following line might throw an exception.
  a_entries_resized = s_entry_allocator.allocate(new_size);

  ranged_probe_fn_base::notify_resized(new_size);
  m_num_e = new_size;

  for (size_type i = 0; i < m_num_e; ++i)
    a_entries_resized[i].m_stat = empty_entry_status;

  __try
    {
      resize_imp(a_entries_resized, old_size);
    }
  __catch(...)
    {
      erase_all_valid_entries(a_entries_resized, new_size);
      m_num_e = old_size;
      s_entry_allocator.deallocate(a_entries_resized, new_size);
      ranged_probe_fn_base::notify_resized(old_size);
      __throw_exception_again;
    }

  // At this point no exceptions can be thrown.
  _GLIBCXX_DEBUG_ONLY(assert_entry_array_valid(a_entries_resized,
					       traits_base::m_store_extra_indicator,
					       __FILE__, __LINE__);)

  Resize_Policy::notify_resized(new_size);
  erase_all_valid_entries(m_entries, old_size);
  s_entry_allocator.deallocate(m_entries, old_size);
  m_entries = a_entries_resized;
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
resize_imp(entry_array a_entries_resized, size_type old_size)
{
  for (size_type pos = 0; pos < old_size; ++pos)
    if (m_entries[pos].m_stat == valid_entry_status)
      resize_imp_reassign(m_entries + pos, a_entries_resized, 
			  traits_base::m_store_extra_indicator);
}

#include <ext/pb_ds/detail/gp_hash_table_map_/resize_no_store_hash_fn_imps.hpp>
#include <ext/pb_ds/detail/gp_hash_table_map_/resize_store_hash_fn_imps.hpp>

#endif
