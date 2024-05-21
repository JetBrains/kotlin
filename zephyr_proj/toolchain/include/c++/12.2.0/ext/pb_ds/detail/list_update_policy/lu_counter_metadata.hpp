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
 * @file lu_counter_metadata.hpp
 * Contains implementation of a lu counter policy's metadata.
 */

namespace __gnu_pbds
{
  namespace detail
  {
    template<typename Size_Type>
      class lu_counter_policy_base;

    /// A list-update metadata type that moves elements to the front of
    /// the list based on the counter algorithm.
    template<typename Size_Type = std::size_t>
      class lu_counter_metadata
      {
      public:
	typedef Size_Type 	size_type;

      private:
	lu_counter_metadata(size_type init_count) : m_count(init_count)
	{ }

	friend class lu_counter_policy_base<size_type>;

	mutable size_type 	m_count;
    };

    /// Base class for list-update counter policy.
    template<typename Size_Type>
      class lu_counter_policy_base
      {
      protected:
	typedef Size_Type 	size_type;

	lu_counter_metadata<size_type>
	operator()(size_type max_size) const
	{ return lu_counter_metadata<Size_Type>(std::rand() % max_size); }

	template<typename Metadata_Reference>
	bool
	operator()(Metadata_Reference r_data, size_type m_max_count) const
	{
	  if (++r_data.m_count != m_max_count)
	    return false;
	  r_data.m_count = 0;
	  return true;
	}
      };
  } // namespace detail
} // namespace __gnu_pbds
