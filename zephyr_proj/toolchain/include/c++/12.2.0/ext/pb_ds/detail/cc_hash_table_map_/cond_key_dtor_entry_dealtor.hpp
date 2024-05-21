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
 * @file cc_hash_table_map_/cond_key_dtor_entry_dealtor.hpp
 * Contains a conditional key destructor, used for exception handling.
 */

namespace __gnu_pbds
{
  namespace detail
  {
    /// Conditional dey destructor, cc_hash.
    template<typename HT_Map>
    class cond_dealtor
    {
    public:
      typedef typename HT_Map::entry 		entry;
      typedef typename HT_Map::entry_allocator 	entry_allocator;
      typedef typename HT_Map::key_type 	key_type;

      cond_dealtor(entry_allocator* p_a, entry* p_e)
      : m_p_a(p_a), m_p_e(p_e), m_key_destruct(false),
	m_no_action_destructor(false)
      { }

      inline
      ~cond_dealtor();

      void
      set_key_destruct()
      { m_key_destruct = true; }

      void
      set_no_action_destructor()
      { m_no_action_destructor = true; }

    protected:
      entry_allocator* const 			m_p_a;
      entry* const 				m_p_e;

      bool 					m_key_destruct;
      bool 					m_no_action_destructor;
    };

    template<typename HT_Map>
    inline
    cond_dealtor<HT_Map>::
    ~cond_dealtor()
    {
      if (m_no_action_destructor)
	return;
      if (m_key_destruct)
	m_p_e->m_value.first.~key_type();
      m_p_a->deallocate(m_p_e, 1);
    }
  } // namespace detail
} // namespace __gnu_pbds
