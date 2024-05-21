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
 * @file list_update_policy.hpp
 * Contains policies for list update containers.
 */

#ifndef PB_DS_LU_POLICY_HPP
#define PB_DS_LU_POLICY_HPP

#include <bits/c++config.h>
#include <cstdlib>
#include <ext/pb_ds/detail/types_traits.hpp>
#include <ext/pb_ds/detail/list_update_policy/lu_counter_metadata.hpp>
#include <ext/pb_ds/tag_and_trait.hpp>

namespace __gnu_pbds
{
  /**
   *  A list-update policy that unconditionally moves elements to the
   *  front of the list. A null type means that each link in a
   *  list-based container does not actually need metadata.
   */
 template<typename _Alloc = std::allocator<char> >
   class lu_move_to_front_policy
   {
   public:
     typedef _Alloc 					allocator_type;

     /// Metadata on which this functor operates.
     typedef null_type 					metadata_type;

   public:
     /// Reference to metadata on which this functor operates.
     typedef typename detail::rebind_traits<_Alloc, metadata_type>::reference
       metadata_reference;

     /// Creates a metadata object.
     metadata_type
     operator()() const
     { return s_metadata; }

     /// Decides whether a metadata object should be moved to the front
     /// of the list.
     inline bool
     operator()(metadata_reference r_metadata) const
     { return true; }

   private:
     static null_type 					s_metadata;
   };

  /**
   *  A list-update policy that moves elements to the front of the
   *  list based on the counter algorithm.
   */
  template<std::size_t Max_Count = 5, typename _Alloc = std::allocator<char> >
    class lu_counter_policy
    : private detail::lu_counter_policy_base<typename _Alloc::size_type>
    {
    public:
      typedef _Alloc 					allocator_type;
      typedef typename allocator_type::size_type       	size_type;

      enum
	{
	  /// When some element is accessed this number of times, it
	  /// will be moved to the front of the list.
	  max_count = Max_Count
	};

      /// Metadata on which this functor operates.
      typedef detail::lu_counter_metadata<size_type> 	metadata_type;

    private:
      typedef detail::lu_counter_policy_base<size_type> 	base_type;

    public:
      /// Reference to metadata on which this functor operates.
     typedef typename detail::rebind_traits<_Alloc, metadata_type>::reference
       metadata_reference;

      /// Creates a metadata object.
      metadata_type
      operator()() const
      { return base_type::operator()(max_count); }

      /// Decides whether a metadata object should be moved to the front
      /// of the list.
      bool
      operator()(metadata_reference r_data) const
      { return base_type::operator()(r_data, max_count); }
    };
} // namespace __gnu_pbds

#endif
