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
 * @file hash_eq_fn.hpp
 * Contains 2 equivalence functions, one employing a hash value,
 * and one ignoring it.
 */

#ifndef PB_DS_HASH_EQ_FN_HPP
#define PB_DS_HASH_EQ_FN_HPP

#include <utility>
#include <ext/pb_ds/detail/types_traits.hpp>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Primary template.
    template<typename Key, typename Eq_Fn, typename _Alloc, bool Store_Hash>
      struct hash_eq_fn;

    /// Specialization 1 - The client requests that hash values not be stored.
    template<typename Key, typename Eq_Fn, typename _Alloc>
    struct hash_eq_fn<Key, Eq_Fn, _Alloc, false> : public Eq_Fn
    {
      typedef Eq_Fn 					   eq_fn_base;
      typedef typename rebind_traits<_Alloc, Key>::const_reference
	key_const_reference;

      hash_eq_fn() { }

      hash_eq_fn(const Eq_Fn& r_eq_fn) : Eq_Fn(r_eq_fn) { }

      bool
      operator()(key_const_reference r_lhs_key, 
		 key_const_reference r_rhs_key) const
      { return eq_fn_base::operator()(r_lhs_key, r_rhs_key); }

      void
      swap(const hash_eq_fn& other)
      { std::swap((Eq_Fn&)(*this), (Eq_Fn&)other); }
    };


    /// Specialization 2 - The client requests that hash values be stored.
    template<typename Key, class Eq_Fn, class _Alloc>
    struct hash_eq_fn<Key, Eq_Fn, _Alloc, true> : public Eq_Fn
    {
      typedef typename _Alloc::size_type 		   size_type;
      typedef Eq_Fn 					   eq_fn_base;
      typedef typename rebind_traits<_Alloc, Key>::const_reference
	key_const_reference;

      hash_eq_fn() { }

      hash_eq_fn(const Eq_Fn& r_eq_fn) : Eq_Fn(r_eq_fn) { }

      bool
      operator()(key_const_reference r_lhs_key, size_type lhs_hash, 
		 key_const_reference r_rhs_key, size_type rhs_hash) const
      {
	_GLIBCXX_DEBUG_ASSERT(!eq_fn_base::operator()(r_lhs_key, r_rhs_key) 
			      || lhs_hash == rhs_hash);

	return (lhs_hash == rhs_hash && 
		eq_fn_base::operator()(r_lhs_key, r_rhs_key));
      }

      void
      swap(const hash_eq_fn& other)
      { std::swap((Eq_Fn&)(*this), (Eq_Fn&)(other)); }
    };
  } // namespace detail
} // namespace __gnu_pbds

#endif 
