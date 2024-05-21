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
 * @file hash_load_check_resize_trigger_size_base.hpp
 * Contains an base holding size for some resize policies.
 */

#ifndef PB_DS_HASH_LOAD_CHECK_RESIZE_TRIGGER_SIZE_BASE_HPP
#define PB_DS_HASH_LOAD_CHECK_RESIZE_TRIGGER_SIZE_BASE_HPP

namespace __gnu_pbds
{
  namespace detail
  {
    /// Primary template.
    template<typename Size_Type, bool Hold_Size>
    class hash_load_check_resize_trigger_size_base;

    /// Specializations.
    template<typename Size_Type>
    class hash_load_check_resize_trigger_size_base<Size_Type, true>
    {
    protected:
      typedef Size_Type size_type;

      hash_load_check_resize_trigger_size_base(): m_size(0)
      { }

      inline void
      swap(hash_load_check_resize_trigger_size_base& other)
      { std::swap(m_size, other.m_size); }

      inline void
      set_size(size_type size)
      { m_size = size; }

      inline size_type
      get_size() const
      { return m_size; }

    private:
      size_type m_size;
    };

    template<typename Size_Type>
    class hash_load_check_resize_trigger_size_base<Size_Type, false>
    {
    protected:
      typedef Size_Type size_type;

    protected:
      inline void
      swap(hash_load_check_resize_trigger_size_base& other) { }

      inline void
      set_size(size_type size) { }
    };
  } // namespace detail
} // namespace __gnu_pbds

#endif
