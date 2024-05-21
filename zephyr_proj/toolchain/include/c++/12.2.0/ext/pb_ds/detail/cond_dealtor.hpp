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
 * @file detail/cond_dealtor.hpp
 * Contains a conditional deallocator.
 */

#ifndef PB_DS_COND_DEALTOR_HPP
#define PB_DS_COND_DEALTOR_HPP

#include <ext/pb_ds/detail/types_traits.hpp>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Conditional deallocate constructor argument.
    template<typename Entry, typename _Alloc>
      class cond_dealtor
      {
	typedef rebind_traits<_Alloc, Entry>	alloc_traits;

      public:
	typedef typename alloc_traits::allocator_type entry_allocator;
	typedef typename alloc_traits::pointer	      entry_pointer;

	cond_dealtor(entry_pointer p_e)
	: m_p_e(p_e), m_no_action_destructor(false) { }

	~cond_dealtor()
	{
	  if (m_no_action_destructor)
	    return;
	  s_alloc.deallocate(m_p_e, 1);
	}

	void
	set_no_action()
	{ m_no_action_destructor = true; }

      private:
	entry_pointer 		m_p_e;
	bool 			m_no_action_destructor;
	static entry_allocator 	s_alloc;
      };

    template<typename Entry, class _Alloc>
      typename cond_dealtor<Entry, _Alloc>::entry_allocator
      cond_dealtor<Entry, _Alloc>::s_alloc;
  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifndef PB_DS_COND_DEALTOR_HPP
