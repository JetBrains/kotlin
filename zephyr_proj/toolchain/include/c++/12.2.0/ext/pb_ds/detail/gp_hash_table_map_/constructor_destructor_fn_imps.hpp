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
 * @file gp_hash_table_map_/constructor_destructor_fn_imps.hpp
 * Contains implementations of gp_ht_map_'s constructors, destructor,
 * and related functions.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::entry_allocator
PB_DS_CLASS_C_DEC::s_entry_allocator;

PB_DS_CLASS_T_DEC
template<typename It>
void
PB_DS_CLASS_C_DEC::
copy_from_range(It first_it, It last_it)
{
  while (first_it != last_it)
    insert(*(first_it++));
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_GP_HASH_NAME() 
: ranged_probe_fn_base(resize_base::get_nearest_larger_size(1)),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_GP_HASH_NAME(const Hash_Fn& r_hash_fn)    
: ranged_probe_fn_base(resize_base::get_nearest_larger_size(1), r_hash_fn),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_GP_HASH_NAME(const Hash_Fn& r_hash_fn, const Eq_Fn& r_eq_fn) 
: hash_eq_fn_base(r_eq_fn),
  ranged_probe_fn_base(resize_base::get_nearest_larger_size(1), r_hash_fn),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_GP_HASH_NAME(const Hash_Fn& r_hash_fn, const Eq_Fn& r_eq_fn, 
		 const Comb_Probe_Fn& r_comb_hash_fn) 
: hash_eq_fn_base(r_eq_fn),
  ranged_probe_fn_base(resize_base::get_nearest_larger_size(1),
		       r_hash_fn, r_comb_hash_fn),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_GP_HASH_NAME(const Hash_Fn& r_hash_fn, const Eq_Fn& r_eq_fn, 
		 const Comb_Probe_Fn& comb_hash_fn, const Probe_Fn& prober) 
: hash_eq_fn_base(r_eq_fn),
  ranged_probe_fn_base(resize_base::get_nearest_larger_size(1),
		       r_hash_fn, comb_hash_fn, prober),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_GP_HASH_NAME(const Hash_Fn& r_hash_fn, const Eq_Fn& r_eq_fn, 
		 const Comb_Probe_Fn& comb_hash_fn, const Probe_Fn& prober, 
		 const Resize_Policy& r_resize_policy) 
: hash_eq_fn_base(r_eq_fn), resize_base(r_resize_policy),
  ranged_probe_fn_base(resize_base::get_nearest_larger_size(1),
		       r_hash_fn, comb_hash_fn, prober),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_GP_HASH_NAME(const PB_DS_CLASS_C_DEC& other) :
#ifdef _GLIBCXX_DEBUG
  debug_base(other),
#endif 
  hash_eq_fn_base(other),
  resize_base(other),
  ranged_probe_fn_base(other),
  m_num_e(other.m_num_e),
  m_num_used_e(other.m_num_used_e),
  m_entries(s_entry_allocator.allocate(m_num_e))
{
  for (size_type i = 0; i < m_num_e; ++i)
    m_entries[i].m_stat = (entry_status)empty_entry_status;

  __try
    {
      for (size_type i = 0; i < m_num_e; ++i)
        {
	  m_entries[i].m_stat = other.m_entries[i].m_stat;
	  if (m_entries[i].m_stat == valid_entry_status)
	    new (m_entries + i) entry(other.m_entries[i]);
        }
    }
  __catch(...)
    {
      deallocate_all();
      __throw_exception_again;
    }
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~PB_DS_GP_HASH_NAME()
{ deallocate_all(); }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
  std::swap(m_num_e, other.m_num_e);
  std::swap(m_num_used_e, other.m_num_used_e);
  std::swap(m_entries, other.m_entries);
  ranged_probe_fn_base::swap(other);
  hash_eq_fn_base::swap(other);
  resize_base::swap(other);
  _GLIBCXX_DEBUG_ONLY(debug_base::swap(other));
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
deallocate_all()
{
  clear();
  erase_all_valid_entries(m_entries, m_num_e);
  s_entry_allocator.deallocate(m_entries, m_num_e);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
erase_all_valid_entries(entry_array a_entries_resized, size_type len)
{
  for (size_type pos = 0; pos < len; ++pos)
    {
      entry_pointer p_e = &a_entries_resized[pos];
      if (p_e->m_stat == valid_entry_status)
	p_e->m_value.~value_type();
    }
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
initialize()
{
  Resize_Policy::notify_resized(m_num_e);
  Resize_Policy::notify_cleared();
  ranged_probe_fn_base::notify_resized(m_num_e);
  for (size_type i = 0; i < m_num_e; ++i)
    m_entries[i].m_stat = empty_entry_status;
}

#endif
