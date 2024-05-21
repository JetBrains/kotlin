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
 * @file sample_update_policy.hpp
 * Contains a sample policy for list update containers.
 */

#ifndef PB_DS_SAMPLE_UPDATE_POLICY_HPP
#define PB_DS_SAMPLE_UPDATE_POLICY_HPP

namespace __gnu_pbds
{
  /// A sample list-update policy.
  struct sample_update_policy
  {
    /// Default constructor.
    sample_update_policy();

    /// Copy constructor.
    sample_update_policy(const sample_update_policy&);

    /// Swaps content.
    inline void
    swap(sample_update_policy& other);

  protected:
    /// Metadata on which this functor operates.
    typedef some_metadata_type metadata_type;

    /// Creates a metadata object.
    metadata_type
    operator()() const;

    /// Decides whether a metadata object should be moved to the front
    /// of the list. A list-update based containers object will call
    /// this method to decide whether to move a node to the front of
    /// the list. The method shoule return true if the node should be
    /// moved to the front of the list.
    bool
    operator()(metadata_reference) const;
  };
}
#endif
