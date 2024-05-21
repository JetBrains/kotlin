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
 * @file detail/standard_policies.hpp
 * Contains standard policies for containers.
 */

#ifndef PB_DS_STANDARD_POLICIES_HPP
#define PB_DS_STANDARD_POLICIES_HPP

#include <memory>
#include <ext/pb_ds/hash_policy.hpp>
#include <ext/pb_ds/list_update_policy.hpp>
#include <ext/pb_ds/detail/branch_policy/null_node_metadata.hpp>
#include <ext/pb_ds/tree_policy.hpp>
#include <ext/pb_ds/trie_policy.hpp>
#include <ext/pb_ds/tag_and_trait.hpp>
#include <tr1/functional>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Primary template, default_hash_fn.
    template<typename Key>
    struct default_hash_fn
    {
	/// Dispatched type.
      typedef std::tr1::hash<Key> 				type;
    };

    /// Primary template, default_eq_fn.
    template<typename Key>
    struct default_eq_fn
    {
	/// Dispatched type.
      typedef std::equal_to<Key> 				type;
    };

    /// Enumeration for default behavior of stored hash data.
    enum
      {
	default_store_hash = false
      };

    /// Primary template, default_comb_hash_fn.
    struct default_comb_hash_fn
    {
	/// Dispatched type.
      typedef direct_mask_range_hashing<> 			type;
    };

    /// Primary template, default_resize_policy.
    template<typename Comb_Hash_Fn>
    struct default_resize_policy
    {
    private:
      typedef typename Comb_Hash_Fn::size_type 			size_type;

      typedef direct_mask_range_hashing<size_type> 		default_fn;
      typedef is_same<default_fn, Comb_Hash_Fn> 		same_type;
      typedef hash_exponential_size_policy<size_type> 		iftrue;
      typedef hash_prime_size_policy 				iffalse;
      typedef __conditional_type<same_type::value, iftrue, iffalse> cond_type;
      typedef typename cond_type::__type 		       size_policy_type;

      typedef hash_load_check_resize_trigger<false, size_type> 	trigger;

    public:
	/// Dispatched type.
      typedef hash_standard_resize_policy<size_policy_type, trigger, 
					  false, size_type> 	type;
    };

    /// Default update policy.
    struct default_update_policy
    {
	/// Dispatched type.
      typedef lu_move_to_front_policy<> 			type;
    };

    /// Primary template, default_probe_fn.
    template<typename Comb_Probe_Fn>
    struct default_probe_fn
    {
    private:
      typedef typename Comb_Probe_Fn::size_type 		size_type;
      typedef direct_mask_range_hashing<size_type> 		default_fn;
      typedef is_same<default_fn, Comb_Probe_Fn> 		same_type;
      typedef linear_probe_fn<size_type> 			iftrue;
      typedef quadratic_probe_fn<size_type> 			iffalse;
      typedef __conditional_type<same_type::value, iftrue, iffalse> cond_type;

    public:
	/// Dispatched type.
      typedef typename cond_type::__type 			type;
    };


    /// Primary template, default_trie_access_traits.
    template<typename Key>
      struct default_trie_access_traits;

#define __dtrie_alloc std::allocator<char>  
#define __dtrie_string std::basic_string<Char, Char_Traits, __dtrie_alloc> 

    /// Partial specialization, default_trie_access_traits.
    template<typename Char, typename Char_Traits>
      struct default_trie_access_traits<__dtrie_string>
      {
      private:
	typedef __dtrie_string					string_type;

      public:
	/// Dispatched type.
	typedef trie_string_access_traits<string_type> 		type;
      };

#undef __dtrie_alloc
#undef __dtrie_string

  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifndef PB_DS_STANDARD_POLICIES_HPP
