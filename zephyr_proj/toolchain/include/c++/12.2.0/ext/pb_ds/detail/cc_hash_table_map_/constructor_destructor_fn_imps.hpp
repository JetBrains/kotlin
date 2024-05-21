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
 * @file cc_hash_table_map_/constructor_destructor_fn_imps.hpp
 * Contains implementations of cc_ht_map_'s constructors, destructor,
 * and related functions.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::entry_allocator
PB_DS_CLASS_C_DEC::s_entry_allocator;

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::entry_pointer_allocator
PB_DS_CLASS_C_DEC::s_entry_pointer_allocator;

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
PB_DS_CC_HASH_NAME() :
  ranged_hash_fn_base(resize_base::get_nearest_larger_size(1)),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_pointer_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_CC_HASH_NAME(const Hash_Fn& r_hash_fn) :
  ranged_hash_fn_base(resize_base::get_nearest_larger_size(1), r_hash_fn),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_pointer_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_CC_HASH_NAME(const Hash_Fn& r_hash_fn, const Eq_Fn& r_eq_fn) :
  PB_DS_HASH_EQ_FN_C_DEC(r_eq_fn),
  ranged_hash_fn_base(resize_base::get_nearest_larger_size(1), r_hash_fn),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_pointer_allocator.allocate(m_num_e))
{
  std::fill(m_entries, m_entries + m_num_e, (entry_pointer)0);
  Resize_Policy::notify_cleared();
  ranged_hash_fn_base::notify_resized(m_num_e);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_CC_HASH_NAME(const Hash_Fn& r_hash_fn, const Eq_Fn& r_eq_fn, 
		   const Comb_Hash_Fn& r_comb_hash_fn) :
  PB_DS_HASH_EQ_FN_C_DEC(r_eq_fn),
  ranged_hash_fn_base(resize_base::get_nearest_larger_size(1),
		      r_hash_fn, r_comb_hash_fn),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_pointer_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_CC_HASH_NAME(const Hash_Fn& r_hash_fn, const Eq_Fn& r_eq_fn, 
		   const Comb_Hash_Fn& r_comb_hash_fn, 
		   const Resize_Policy& r_resize_policy) :
  PB_DS_HASH_EQ_FN_C_DEC(r_eq_fn),
  Resize_Policy(r_resize_policy),
  ranged_hash_fn_base(resize_base::get_nearest_larger_size(1),
		      r_hash_fn, r_comb_hash_fn),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_pointer_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_CC_HASH_NAME(const PB_DS_CLASS_C_DEC& other) :
  PB_DS_HASH_EQ_FN_C_DEC(other),
  resize_base(other), ranged_hash_fn_base(other),
  m_num_e(resize_base::get_nearest_larger_size(1)), m_num_used_e(0),
  m_entries(s_entry_pointer_allocator.allocate(m_num_e))
{
  initialize();
  PB_DS_ASSERT_VALID((*this))
    __try
      {
	copy_from_range(other.begin(), other.end());
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
~PB_DS_CC_HASH_NAME()
{ deallocate_all(); }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)

  std::swap(m_entries, other.m_entries);
  std::swap(m_num_e, other.m_num_e);
  std::swap(m_num_used_e, other.m_num_used_e);
  ranged_hash_fn_base::swap(other);
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
  s_entry_pointer_allocator.deallocate(m_entries, m_num_e);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
initialize()
{
  std::fill(m_entries, m_entries + m_num_e, entry_pointer(0));
  Resize_Policy::notify_resized(m_num_e);
  Resize_Policy::notify_cleared();
  ranged_hash_fn_base::notify_resized(m_num_e);
}
#endif
