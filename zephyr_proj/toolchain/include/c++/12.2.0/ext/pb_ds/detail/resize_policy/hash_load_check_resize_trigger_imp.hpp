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
 * @file hash_load_check_resize_trigger_imp.hpp
 * Contains a resize trigger implementation.
 */

#ifdef PB_DS_CLASS_C_DEC

#define PB_DS_ASSERT_VALID(X)						\
  _GLIBCXX_DEBUG_ONLY(X.assert_valid(__FILE__, __LINE__);)

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
hash_load_check_resize_trigger(float load_min, float load_max)
: m_load_min(load_min), m_load_max(load_max), m_next_shrink_size(0),
  m_next_grow_size(0), m_resize_needed(false)
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_start()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_collision()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_find_search_end()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_start()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_collision()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_insert_search_end()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_start()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_collision()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erase_search_end()
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_inserted(size_type num_entries)
{
  m_resize_needed = (num_entries >= m_next_grow_size);
  size_base::set_size(num_entries);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
notify_erased(size_type num_entries)
{
  size_base::set_size(num_entries);
  m_resize_needed = num_entries <= m_next_shrink_size;
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
is_resize_needed() const
{
  PB_DS_ASSERT_VALID((*this))
  return m_resize_needed;
}

PB_DS_CLASS_T_DEC
inline bool
PB_DS_CLASS_C_DEC::
is_grow_needed(size_type /*size*/, size_type num_entries) const
{
  _GLIBCXX_DEBUG_ASSERT(m_resize_needed);
  return num_entries >= m_next_grow_size;
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~hash_load_check_resize_trigger() { }

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
notify_resized(size_type new_size)
{
  m_resize_needed = false;
  m_next_grow_size = size_type(m_load_max * new_size - 1);
  m_next_shrink_size = size_type(m_load_min * new_size);

#ifdef PB_DS_HT_MAP_RESIZE_TRACE_
  std::cerr << "hlcrt::notify_resized "  << std::endl
	    << "1 " << new_size << std::endl
	    << "2 " << m_load_min << std::endl
	    << "3 " << m_load_max << std::endl
	    << "4 " << m_next_shrink_size << std::endl
	    << "5 " << m_next_grow_size << std::endl;
#endif

  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
notify_externally_resized(size_type new_size)
{
  m_resize_needed = false;
  size_type new_grow_size = size_type(m_load_max * new_size - 1);
  size_type new_shrink_size = size_type(m_load_min * new_size);

#ifdef PB_DS_HT_MAP_RESIZE_TRACE_
  std::cerr << "hlcrt::notify_externally_resized "  << std::endl
	    << "1 " << new_size << std::endl
	    << "2 " << m_load_min << std::endl
	    << "3 " << m_load_max << std::endl
	    << "4 " << m_next_shrink_size << std::endl
	    << "5 " << m_next_grow_size << std::endl
	    << "6 " << new_shrink_size << std::endl
	    << "7 " << new_grow_size << std::endl;
#endif

  if (new_grow_size >= m_next_grow_size)
    {
      _GLIBCXX_DEBUG_ASSERT(new_shrink_size >= m_next_shrink_size);
      m_next_grow_size = new_grow_size;
    }
  else
    {
      _GLIBCXX_DEBUG_ASSERT(new_shrink_size <= m_next_shrink_size);
      m_next_shrink_size = new_shrink_size;
    }

  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
notify_cleared()
{
  PB_DS_ASSERT_VALID((*this))
  size_base::set_size(0);
  m_resize_needed = (0 < m_next_shrink_size);
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)

  size_base::swap(other);
  std::swap(m_load_min, other.m_load_min);
  std::swap(m_load_max, other.m_load_max);
  std::swap(m_resize_needed, other.m_resize_needed);
  std::swap(m_next_grow_size, other.m_next_grow_size);
  std::swap(m_next_shrink_size, other.m_next_shrink_size);

  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}

PB_DS_CLASS_T_DEC
inline std::pair<float, float>
PB_DS_CLASS_C_DEC::
get_loads() const
{
  PB_DS_STATIC_ASSERT(access, external_load_access);
  return std::make_pair(m_load_min, m_load_max);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
set_loads(std::pair<float, float> load_pair)
{
  PB_DS_STATIC_ASSERT(access, external_load_access);
  const float old_load_min = m_load_min;
  const float old_load_max = m_load_max;
  const size_type old_next_shrink_size = m_next_shrink_size;
  const size_type old_next_grow_size = m_next_grow_size;
  const bool old_resize_needed = m_resize_needed;

  __try
    {
      m_load_min = load_pair.first;
      m_load_max = load_pair.second;
      do_resize(static_cast<size_type>(size_base::get_size() / ((m_load_min + m_load_max) / 2)));
    }
  __catch(...)
    {
      m_load_min = old_load_min;
      m_load_max = old_load_max;
      m_next_shrink_size = old_next_shrink_size;
      m_next_grow_size = old_next_grow_size;
      m_resize_needed = old_resize_needed;
      __throw_exception_again;
    }
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
do_resize(size_type)
{ std::abort(); }

#ifdef _GLIBCXX_DEBUG
# define PB_DS_DEBUG_VERIFY(_Cond)					\
  _GLIBCXX_DEBUG_VERIFY_AT(_Cond,					\
			   _M_message(#_Cond" assertion from %1;:%2;")	\
			   ._M_string(__FILE__)._M_integer(__LINE__)	\
			   ,__file,__line)

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
assert_valid(const char* __file, int __line) const
{
  PB_DS_DEBUG_VERIFY(m_load_max > m_load_min);
  PB_DS_DEBUG_VERIFY(m_next_grow_size >= m_next_shrink_size);
}
# undef PB_DS_DEBUG_VERIFY
#endif
#undef PB_DS_ASSERT_VALID
#endif
