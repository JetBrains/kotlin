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
 * @file hash_standard_resize_policy_imp.hpp
 * Contains a resize policy implementation.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
hash_standard_resize_policy() 
: m_size(Size_Policy::get_nearest_larger_size(1))
{ trigger_policy_base::notify_externally_resized(m_size); }

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
hash_standard_resize_policy(const Size_Policy& r_size_policy) 
: Size_Policy(r_size_policy), m_size(Size_Policy::get_nearest_larger_size(1))
{ trigger_policy_base::notify_externally_resized(m_size); }

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
hash_standard_resize_policy(const Size_Policy& r_size_policy, 
			    const Trigger_Policy& r_trigger_policy) 
: Size_Policy(r_size_policy), Trigger_Policy(r_trigger_policy),
  m_size(Size_Policy::get_nearest_larger_size(1))
{ trigger_policy_base::notify_externally_resized(m_size); }

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~hash_standard_resize_policy()
{ }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  trigger_policy_base::swap(other);
  size_policy_base::swap(other);
  std::swap(m_size, other.m_size);
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_start()
{ trigger_policy_base::notify_find_search_start(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_collision()
{ trigger_policy_base::notify_find_search_collision(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_end()
{ trigger_policy_base::notify_find_search_end(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_start()
{ trigger_policy_base::notify_insert_search_start(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_collision()
{ trigger_policy_base::notify_insert_search_collision(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_end()
{ trigger_policy_base::notify_insert_search_end(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_start()
{ trigger_policy_base::notify_erase_search_start(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_collision()
{ trigger_policy_base::notify_erase_search_collision(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_end()
{ trigger_policy_base::notify_erase_search_end(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_inserted(size_type num_e)
{ trigger_policy_base::notify_inserted(num_e); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erased(size_type num_e)
{ trigger_policy_base::notify_erased(num_e); }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
notify_cleared()
{ trigger_policy_base::notify_cleared(); }

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
is_resize_needed() const
{ return trigger_policy_base::is_resize_needed(); }

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
get_new_size(size_type size, size_type num_used_e) const
{
  if (trigger_policy_base::is_grow_needed(size, num_used_e))
    return size_policy_base::get_nearest_larger_size(size);
  return size_policy_base::get_nearest_smaller_size(size);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
notify_resized(size_type new_size)
{
  trigger_policy_base::notify_resized(new_size);
  m_size = new_size;
}

PB_DS_CLASS_T_DEC
inline typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
get_actual_size() const
{
  PB_DS_STATIC_ASSERT(access, external_size_access);
  return m_size;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
resize(size_type new_size)
{
  PB_DS_STATIC_ASSERT(access, external_size_access);
  size_type actual_size = size_policy_base::get_nearest_larger_size(1);
  while (actual_size < new_size)
    {
      const size_type pot = size_policy_base::get_nearest_larger_size(actual_size);

      if (pot == actual_size && pot < new_size)
	__throw_resize_error();
      actual_size = pot;
    }

  if (actual_size > 0)
    --actual_size;

  const size_type old_size = m_size;
  __try
    {
      do_resize(actual_size - 1);
    }
  __catch(insert_error& )
    {
      m_size = old_size;
      __throw_resize_error();
    }
  __catch(...)
    {
      m_size = old_size;
      __throw_exception_again;
    }
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
do_resize(size_type)
{
  // Do nothing
}

PB_DS_CLASS_T_DEC
Trigger_Policy& 
PB_DS_CLASS_C_DEC::
get_trigger_policy()
{ return *this; }

PB_DS_CLASS_T_DEC
const Trigger_Policy& 
PB_DS_CLASS_C_DEC::
get_trigger_policy() const
{ return *this; }

PB_DS_CLASS_T_DEC
Size_Policy& 
PB_DS_CLASS_C_DEC::
get_size_policy()
{ return *this; }

PB_DS_CLASS_T_DEC
const Size_Policy& 
PB_DS_CLASS_C_DEC::
get_size_policy() const
{ return *this; }

#endif
