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
 * @file exception.hpp
 * Contains exception classes.
 */

#ifndef PB_DS_EXCEPTION_HPP
#define PB_DS_EXCEPTION_HPP

#include <bits/c++config.h>
#include <stdexcept>
#include <cstdlib>

namespace __gnu_pbds
{
  /**
   *  @defgroup exceptions-pbds Exceptions
   *  @ingroup pbds
   *  @{
   */

  /// Base class for exceptions.
  struct container_error : public std::logic_error
  {
    container_error()
    : std::logic_error(__N("__gnu_pbds::container_error")) { }
  };

  /// An entry cannot be inserted into a container object for logical
  /// reasons (not, e.g., if memory is unabvailable, in which case
  /// the allocator_type's exception will be thrown).
  struct insert_error : public container_error { };

  /// A join cannot be performed logical reasons (i.e., the ranges of
  /// the two container objects being joined overlaps.
  struct join_error : public container_error { };

  /// A container cannot be resized.
  struct resize_error : public container_error { };

  inline void
  __throw_container_error()
  { _GLIBCXX_THROW_OR_ABORT(container_error()); }

  inline void
  __throw_insert_error()
  { _GLIBCXX_THROW_OR_ABORT(insert_error()); }

  inline void
  __throw_join_error()
  { _GLIBCXX_THROW_OR_ABORT(join_error()); }

  inline void
  __throw_resize_error()
  { _GLIBCXX_THROW_OR_ABORT(resize_error()); }
  ///@}
} // namespace __gnu_pbds

#endif
