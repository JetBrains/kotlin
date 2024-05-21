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
 * @file binary_heap_/erase_fn_imps.hpp
 * Contains an implementation class for a binary_heap.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
clear()
{
  for (size_type i = 0; i < m_size; ++i)
    erase_at(m_a_entries, i, s_no_throw_copies_ind);

  __try
    {
      const size_type new_size = resize_policy::get_new_size_for_arbitrary(0);
      entry_pointer new_entries = s_entry_allocator.allocate(new_size);
      resize_policy::notify_arbitrary(new_size);
      s_entry_allocator.deallocate(m_a_entries, m_actual_size);
      m_actual_size = new_size;
      m_a_entries = new_entries;
    }
  __catch(...)
    { }

  m_size = 0;
  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
erase_at(entry_pointer a_entries, size_type i, false_type)
{
  a_entries[i]->~value_type();
  s_value_allocator.deallocate(a_entries[i], 1);
}

PB_DS_CLASS_T_DEC
void
PB_DS_CLASS_C_DEC::
erase_at(entry_pointer, size_type, true_type)
{ }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
pop()
{
  PB_DS_ASSERT_VALID((*this))
  _GLIBCXX_DEBUG_ASSERT(!empty());

  pop_heap();
  erase_at(m_a_entries, m_size - 1, s_no_throw_copies_ind);
  resize_for_erase_if_needed();
  _GLIBCXX_DEBUG_ASSERT(m_size > 0);
  --m_size;

  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
template<typename Pred>
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
erase_if(Pred pred)
{
  PB_DS_ASSERT_VALID((*this))

  typedef typename entry_pred<value_type, Pred, _Alloc, simple_value>::type
    pred_t;

  const size_type left = partition(pred_t(pred));
  _GLIBCXX_DEBUG_ASSERT(m_size >= left);
  const size_type ersd = m_size - left;
  for (size_type i = left; i < m_size; ++i)
    erase_at(m_a_entries, i, s_no_throw_copies_ind);

  __try
    {
      const size_type new_size =
	resize_policy::get_new_size_for_arbitrary(left);

      entry_pointer new_entries = s_entry_allocator.allocate(new_size);
      std::copy(m_a_entries, m_a_entries + left, new_entries);
      s_entry_allocator.deallocate(m_a_entries, m_actual_size);
      m_actual_size = new_size;
      resize_policy::notify_arbitrary(m_actual_size);
    }
  __catch(...)
    { };

  m_size = left;
  make_heap();
  PB_DS_ASSERT_VALID((*this))
  return ersd;
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
erase(point_iterator it)
{
  PB_DS_ASSERT_VALID((*this))
  _GLIBCXX_DEBUG_ASSERT(!empty());

  const size_type fix_pos = it.m_p_e - m_a_entries;
  std::swap(*it.m_p_e, m_a_entries[m_size - 1]);
  erase_at(m_a_entries, m_size - 1, s_no_throw_copies_ind);
  resize_for_erase_if_needed();

  _GLIBCXX_DEBUG_ASSERT(m_size > 0);
  --m_size;
  _GLIBCXX_DEBUG_ASSERT(fix_pos <= m_size);

  if (fix_pos != m_size)
    fix(m_a_entries + fix_pos);

  PB_DS_ASSERT_VALID((*this))
}

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
resize_for_erase_if_needed()
{
  if (!resize_policy::resize_needed_for_shrink(m_size))
    return;

  __try
    {
      const size_type new_size = resize_policy::get_new_size_for_shrink();
      entry_pointer new_entries = s_entry_allocator.allocate(new_size);
      resize_policy::notify_shrink_resize();

      _GLIBCXX_DEBUG_ASSERT(m_size > 0);
      std::copy(m_a_entries, m_a_entries + m_size - 1, new_entries);
      s_entry_allocator.deallocate(m_a_entries, m_actual_size);
      m_actual_size = new_size;
      m_a_entries = new_entries;
    }
  __catch(...)
    { }
}

PB_DS_CLASS_T_DEC
template<typename Pred>
typename PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
partition(Pred pred)
{
  size_type left = 0;
  size_type right = m_size - 1;

  while (right + 1 != left)
    {
      _GLIBCXX_DEBUG_ASSERT(left <= m_size);

      if (!pred(m_a_entries[left]))
	++left;
      else if (pred(m_a_entries[right]))
	--right;
      else
	{
	  _GLIBCXX_DEBUG_ASSERT(left < right);
	  std::swap(m_a_entries[left], m_a_entries[right]);
	  ++left;
	  --right;
	}
    }

  return left;
}
#endif
