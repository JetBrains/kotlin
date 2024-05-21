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
 * @file ov_tree_map_/erase_fn_imps.hpp
 * Contains an implementation class for ov_tree_.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
clear()
{
  PB_DS_ASSERT_VALID((*this))
  if (m_size == 0)
    {
      return;
    }
  else
    {
      reallocate_metadata((node_update* )this, 0);
      cond_dtor<size_type> cd(m_a_values, m_end_it, m_size);
    }

  _GLIBCXX_DEBUG_ONLY(debug_base::clear();)
  m_a_values = 0;
  m_size = 0;
  m_end_it = m_a_values;
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
template<typename Pred>
inline typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
erase_if(Pred pred)
{
  PB_DS_ASSERT_VALID((*this))

#ifdef PB_DS_REGRESSION
    typename _Alloc::group_adjustor adjust(m_size);
#endif

  size_type new_size = 0;
  size_type num_val_ersd = 0;

  for (iterator source_it = begin(); source_it != m_end_it; ++source_it)
    if (!pred(*source_it))
      ++new_size;
    else
      ++num_val_ersd;

  if (new_size == 0)
    {
      clear();
      return num_val_ersd;
    }

  value_vector a_new_values = s_value_alloc.allocate(new_size);
  iterator target_it = a_new_values;
  cond_dtor<size_type> cd(a_new_values, target_it, new_size);
  _GLIBCXX_DEBUG_ONLY(debug_base::clear());
  for (iterator source_it = begin(); source_it != m_end_it; ++source_it)
    {
      if (!pred(*source_it))
	{
	  new (const_cast<void*>(static_cast<const void*>(target_it)))
	    value_type(*source_it);

	  _GLIBCXX_DEBUG_ONLY(debug_base::insert_new(PB_DS_V2F(*source_it)));
	  ++target_it;
	}
    }

  reallocate_metadata((node_update*)this, new_size);
  cd.set_no_action();

  {
    cond_dtor<size_type> cd1(m_a_values, m_end_it, m_size);
  }

  m_a_values = a_new_values;
  m_size = new_size;
  m_end_it = target_it;
  update(node_begin(), (node_update*)this);
  PB_DS_ASSERT_VALID((*this))
  return num_val_ersd;
}

PB_DS_CLASS_T_DEC
template<typename It>
It
PB_DS_CLASS_C_DEC::
erase_imp(It it)
{
  PB_DS_ASSERT_VALID((*this))
  if (it == end())
    return end();

  PB_DS_CHECK_KEY_EXISTS(PB_DS_V2F(*it))

#ifdef PB_DS_REGRESSION
    typename _Alloc::group_adjustor adjust(m_size);
#endif

  _GLIBCXX_DEBUG_ASSERT(m_size > 0);
  value_vector a_values = s_value_alloc.allocate(m_size - 1);
  iterator source_it = begin();
  iterator source_end_it = end();
  iterator target_it = a_values;
  iterator ret_it = end();

  cond_dtor<size_type> cd(a_values, target_it, m_size - 1);

  _GLIBCXX_DEBUG_ONLY(size_type cnt = 0;)

  while (source_it != source_end_it)
    {
      if (source_it != it)
	{
	  _GLIBCXX_DEBUG_ONLY(++cnt;)
	  _GLIBCXX_DEBUG_ASSERT(cnt != m_size);
	  new (const_cast<void*>(static_cast<const void*>(target_it)))
	      value_type(*source_it);

	  ++target_it;
	}
      else
	ret_it = target_it;
    ++source_it;
    }

  _GLIBCXX_DEBUG_ASSERT(m_size > 0);
  reallocate_metadata((node_update*)this, m_size - 1);
  cd.set_no_action();
  _GLIBCXX_DEBUG_ONLY(debug_base::erase_existing(PB_DS_V2F(*it));)
  {
    cond_dtor<size_type> cd1(m_a_values, m_end_it, m_size);
  }

  m_a_values = a_values;
  --m_size;
  m_end_it = m_a_values + m_size;
  update(node_begin(), (node_update*)this);
  PB_DS_ASSERT_VALID((*this))
  return It(ret_it);
}

PB_DS_CLASS_T_DEC
bool
PB_DS_CLASS_C_DEC::
erase(key_const_reference r_key)
{
  point_iterator it = find(r_key);
  if (it == end())
    return false;
  erase(it);
  return true;
}
#endif
