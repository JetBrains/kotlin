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
 * @file cc_hash_table_map_/resize_fn_imps.hpp
 * Contains implementations of cc_ht_map_'s resize related functions.
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
do_resize(size_type len)
{ resize_imp(resize_base::get_nearest_larger_size(len)); }

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
  PB_DS_ASSERT_VALID((*this))
  if (new_size == m_num_e)
    return;

  const size_type old_size = m_num_e;
  entry_pointer_array a_p_entries_resized;

  // Following line might throw an exception.
  ranged_hash_fn_base::notify_resized(new_size);

  __try
    {
      // Following line might throw an exception.
      a_p_entries_resized = s_entry_pointer_allocator.allocate(new_size);
      m_num_e = new_size;
    }
  __catch(...)
    {
      ranged_hash_fn_base::notify_resized(old_size);
      __throw_exception_again;
    }

  // At this point no exceptions can be thrown.
  resize_imp_no_exceptions(new_size, a_p_entries_resized, old_size);
  Resize_Policy::notify_resized(new_size);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
resize_imp_no_exceptions(size_type new_size, entry_pointer_array a_p_entries_resized, size_type old_size)
{
  std::fill(a_p_entries_resized, a_p_entries_resized + m_num_e,
	    entry_pointer(0));

  for (size_type pos = 0; pos < old_size; ++pos)
    {
      entry_pointer p_e = m_entries[pos];
      while (p_e != 0)
	p_e = resize_imp_no_exceptions_reassign_pointer(p_e, a_p_entries_resized,  traits_base::m_store_extra_indicator);
    }

  m_num_e = new_size;
  _GLIBCXX_DEBUG_ONLY(assert_entry_pointer_array_valid(a_p_entries_resized,
						       __FILE__, __LINE__);)
  s_entry_pointer_allocator.deallocate(m_entries, old_size);
  m_entries = a_p_entries_resized;
  PB_DS_ASSERT_VALID((*this))
}

#include <ext/pb_ds/detail/cc_hash_table_map_/resize_no_store_hash_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/resize_store_hash_fn_imps.hpp>

#endif
