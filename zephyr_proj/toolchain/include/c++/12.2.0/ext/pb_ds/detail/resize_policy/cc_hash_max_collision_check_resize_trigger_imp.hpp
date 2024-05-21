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
 * @file cc_hash_max_collision_check_resize_trigger_imp.hpp
 * Contains a resize trigger implementation.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
cc_hash_max_collision_check_resize_trigger(float load) :
  m_load(load),
  m_size(0),
  m_num_col(0),
  m_max_col(0),
  m_resize_needed(false)
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_start()
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_collision()
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_end()
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_start()
{ m_num_col = 0; }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_collision()
{ ++m_num_col; }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_end()
{ calc_resize_needed(); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_start()
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_collision()
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_end()
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_inserted(size_type)
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erased(size_type)
{ m_resize_needed = true; }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
notify_cleared()
{ m_resize_needed = false; }

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
is_resize_needed() const
{ return m_resize_needed; }

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
is_grow_needed(size_type /*size*/, size_type /*num_used_e*/) const
{ return m_num_col >= m_max_col; }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
notify_resized(size_type new_size)
{
  m_size = new_size;

#ifdef PB_DS_HT_MAP_RESIZE_TRACE_
  std::cerr << "chmccrt::notify_resized " 
	    << static_cast<unsigned long>(new_size) << std::endl;
#endif 

  calc_max_num_coll();
  calc_resize_needed();
  m_num_col = 0;
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
calc_max_num_coll()
{
  // max_col <-- \sqrt{2 load \ln( 2 m \ln( m ) ) }
  const double ln_arg = 2 * m_size * std::log(double(m_size));
  m_max_col = size_type(std::ceil(std::sqrt(2 * m_load * std::log(ln_arg))));

#ifdef PB_DS_HT_MAP_RESIZE_TRACE_
  std::cerr << "chmccrt::calc_max_num_coll " 
	    << static_cast<unsigned long>(m_size) <<    "    " 
	    << static_cast<unsigned long>(m_max_col) << std::endl;
#endif 
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
notify_externally_resized(size_type new_size)
{ notify_resized(new_size); }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  std::swap(m_load, other.m_load);
  std::swap(m_size, other.m_size);
  std::swap(m_num_col, other.m_num_col);
  std::swap(m_max_col, other.m_max_col);
  std::swap(m_resize_needed, other.m_resize_needed);
}

PB_DS_CLASS_T_DEC
inline float
PB_DS_CLASS_C_DEC::
get_load() const
{
  PB_DS_STATIC_ASSERT(access, external_load_access);
  return m_load;
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
calc_resize_needed()
{ m_resize_needed = m_resize_needed || m_num_col >= m_max_col; }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
set_load(float load)
{
  PB_DS_STATIC_ASSERT(access, external_load_access);
  m_load = load;
  calc_max_num_coll();
  calc_resize_needed();
}

#endif
