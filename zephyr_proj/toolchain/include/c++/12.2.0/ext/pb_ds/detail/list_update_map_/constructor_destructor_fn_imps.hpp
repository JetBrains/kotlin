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
 * @file list_update_map_/constructor_destructor_fn_imps.hpp
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
typename PB_DS_CLASS_C_DEC::entry_allocator
PB_DS_CLASS_C_DEC::s_entry_allocator;

PB_DS_CLASS_T_DEC
Eq_Fn PB_DS_CLASS_C_DEC::s_eq_fn;

PB_DS_CLASS_T_DEC
null_type PB_DS_CLASS_C_DEC::s_null_type;

PB_DS_CLASS_T_DEC
Update_Policy PB_DS_CLASS_C_DEC::s_update_policy;

PB_DS_CLASS_T_DEC
type_to_type<
  typename PB_DS_CLASS_C_DEC::update_metadata> PB_DS_CLASS_C_DEC::s_metadata_type_indicator;

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
PB_DS_LU_NAME() : m_p_l(0)
{ PB_DS_ASSERT_VALID((*this)) }

PB_DS_CLASS_T_DEC
template<typename It>
PB_DS_CLASS_C_DEC::
PB_DS_LU_NAME(It first_it, It last_it) : m_p_l(0)
{
  copy_from_range(first_it, last_it);
  PB_DS_ASSERT_VALID((*this));
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
PB_DS_LU_NAME(const PB_DS_CLASS_C_DEC& other) :
m_p_l(0)
{
  __try
    {
      for (const_iterator it = other.begin(); it != other.end(); ++it)
	{
	  entry_pointer p_l = allocate_new_entry(*it,
				      traits_base::m_no_throw_copies_indicator);

	  p_l->m_p_next = m_p_l;
	  m_p_l = p_l;
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
void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
  _GLIBCXX_DEBUG_ONLY(debug_base::swap(other);)
  std::swap(m_p_l, other.m_p_l);
  PB_DS_ASSERT_VALID((*this))
  PB_DS_ASSERT_VALID(other)
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
deallocate_all()
{
  entry_pointer p_l = m_p_l;
  while (p_l != 0)
    {
      entry_pointer p_next_l = p_l->m_p_next;
      actual_erase_entry(p_l);
      p_l = p_next_l;
    }
  m_p_l = 0;
}

PB_DS_CLASS_T_DEC
PB_DS_CLASS_C_DEC::
~PB_DS_LU_NAME()
{ deallocate_all(); }
#endif
