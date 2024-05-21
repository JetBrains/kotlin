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
 * @file binary_heap_/constructors_destructor_fn_imps.hpp
 * Contains an implementation class for binary_heap_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::entry_allocator
PB_DS_CLASS_C_DEC::s_entry_allocator;

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::value_allocator
PB_DS_CLASS_C_DEC::s_value_allocator;

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::no_throw_copies_t
PB_DS_CLASS_C_DEC::s_no_throw_copies_ind;

PB_DS_CLASS_T_DEC
template<typename It>
void
PB_DS_CLASS_C_DEC::
copy_from_range(It first_it, It last_it)
{
  while (first_it != last_it)
    {
      insert_value(*first_it, s_no_throw_copies_ind);
      ++first_it;
    }
  make_heap();
 PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
binary_heap()
: m_size(0), m_actual_size(resize_policy::min_size),
  m_a_entries(s_entry_allocator.allocate(m_actual_size))
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
binary_heap(const Cmp_Fn& r_cmp_fn)
: entry_cmp(r_cmp_fn), m_size(0), m_actual_size(resize_policy::min_size),
  m_a_entries(s_entry_allocator.allocate(m_actual_size))
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
binary_heap(const PB_DS_CLASS_C_DEC& other)
: entry_cmp(other), resize_policy(other), m_size(0),
  m_actual_size(other.m_actual_size),
  m_a_entries(s_entry_allocator.allocate(m_actual_size))
{
  PB_DS_ASSERT_VALID(other)
  _GLIBCXX_DEBUG_ASSERT(m_a_entries != other.m_a_entries);

  __try
    {
      copy_from_range(other.begin(), other.end());
    }
  __catch(...)
    {
      for (size_type i = 0; i < m_size; ++i)
	erase_at(m_a_entries, i, s_no_throw_copies_ind);

      s_entry_allocator.deallocate(m_a_entries, m_actual_size);
      __throw_exception_again;
    }
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
  _GLIBCXX_DEBUG_ASSERT(m_a_entries != other.m_a_entries);
  value_swap(other);
  std::swap((entry_cmp&)(*this), (entry_cmp&)other);
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
value_swap(PB_DS_CLASS_C_DEC& other)
{
  std::swap(m_a_entries, other.m_a_entries);
  std::swap(m_size, other.m_size);
  std::swap(m_actual_size, other.m_actual_size);
  static_cast<resize_policy*>(this)->swap(other);
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~binary_heap()
{
  for (size_type i = 0; i < m_size; ++i)
    erase_at(m_a_entries, i, s_no_throw_copies_ind);
  s_entry_allocator.deallocate(m_a_entries, m_actual_size);
}
#endif
