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
 * @file tag_and_trait.hpp
 * Contains tags and traits, e.g., ones describing underlying
 * data structures.
 */

#ifndef PB_DS_TAG_AND_TRAIT_HPP
#define PB_DS_TAG_AND_TRAIT_HPP

#include <bits/c++config.h>
#include <ext/pb_ds/detail/type_utils.hpp>

/**
 * @namespace __gnu_pbds
 * @brief GNU extensions for policy-based data structures for public use.
 */
namespace __gnu_pbds
{
  /** @defgroup pbds Policy-Based Data Structures
   *  @ingroup extensions
   *
   *  This is a library of policy-based elementary data structures:
   *  associative containers and priority queues. It is designed for
   *  high-performance, flexibility, semantic safety, and conformance
   *  to the corresponding containers in std (except for some points
   *  where it differs by design).
   *
   *  For details, see:
   *  http://gcc.gnu.org/onlinedocs/libstdc++/ext/pb_ds/index.html
   *
   *  @{
   */

  /**
   *  @defgroup tags Tags
   *  @{   
   */
  /// A trivial iterator tag. Signifies that the iterators has none of
  /// std::iterators's movement abilities.
  struct trivial_iterator_tag
  { };

  /// Prohibit moving trivial iterators.
  typedef void trivial_iterator_difference_type;


  /**
   *  @defgroup invalidation_tags  Invalidation Guarantees
   *  @ingroup tags
   *  @{
   */

  /**
   *  Signifies a basic invalidation guarantee that any iterator,
   *  pointer, or reference to a container object's mapped value type
   *  is valid as long as the container is not modified.
   */
  struct basic_invalidation_guarantee
  { };

  /**
   *  Signifies an invalidation guarantee that includes all those of
   *  its base, and additionally, that any point-type iterator,
   *  pointer, or reference to a container object's mapped value type
   *  is valid as long as its corresponding entry has not be erased,
   *  regardless of modifications to the container object.
   */
  struct point_invalidation_guarantee : public basic_invalidation_guarantee
  { };

  /**
   *  Signifies an invalidation guarantee that includes all those of
   *  its base, and additionally, that any range-type iterator
   *  (including the returns of begin() and end()) is in the correct
   *  relative positions to other range-type iterators as long as its
   *  corresponding entry has not be erased, regardless of
   *  modifications to the container object.
   */
  struct range_invalidation_guarantee : public point_invalidation_guarantee
  { };
  ///@}


  /**
   *  @defgroup ds_tags Data Structure Type
   *  @ingroup tags
   *  @{
   */
  /// Base data structure tag.
  struct container_tag
  { };

  /// Basic sequence.
  struct sequence_tag : public container_tag { };

  /// Basic string container, inclusive of strings, ropes, etc.
  struct string_tag : public sequence_tag { };

  /// Basic associative-container.
  struct associative_tag : public container_tag { };

  /// Basic hash structure.
  struct basic_hash_tag : public associative_tag { };

  /// Collision-chaining hash.
  struct cc_hash_tag : public basic_hash_tag { };

  /// General-probing hash.
  struct gp_hash_tag : public basic_hash_tag { };

  /// Basic branch structure.
  struct basic_branch_tag : public associative_tag { };

  /// Basic tree structure.
  struct tree_tag : public basic_branch_tag { };

  /// Red-black tree.
  struct rb_tree_tag : public tree_tag { };

  /// Splay tree.
  struct splay_tree_tag : public tree_tag { };

  /// Ordered-vector tree.
  struct ov_tree_tag : public tree_tag { };

  /// Basic trie structure.
  struct trie_tag : public basic_branch_tag { };

  /// PATRICIA trie.
  struct pat_trie_tag : public trie_tag { };

  /// List-update.
  struct list_update_tag : public associative_tag { };

  /// Basic priority-queue.
  struct priority_queue_tag : public container_tag { };

  /// Pairing-heap.
  struct pairing_heap_tag : public priority_queue_tag { };

  /// Binomial-heap.
  struct binomial_heap_tag : public priority_queue_tag { };

  /// Redundant-counter binomial-heap.
  struct rc_binomial_heap_tag : public priority_queue_tag { };

  /// Binary-heap (array-based).
  struct binary_heap_tag : public priority_queue_tag { };

  /// Thin heap.
  struct thin_heap_tag : public priority_queue_tag { };
  ///@}
  ///@}


  /**
   *  @defgroup traits Traits
   *  @{
   */

  /**
   *  @brief Represents no type, or absence of type, for template tricks.
   *
   *  In a mapped-policy, indicates that an associative container is a set.
   *
   *  In a list-update policy, indicates that each link does not need
   *  metadata.
   *
   *  In a hash policy, indicates that the combining hash function
   *  is actually a ranged hash function.
   *
   *  In a probe policy, indicates that the combining probe function
   *  is actually a ranged probe function.
   */
  struct null_type { };

  /// A null node updator, indicating that no node updates are required.
  template<typename _Tp1, typename _Tp2, typename _Tp3, typename _Tp4>
    struct null_node_update : public null_type
    { };


  /// Primary template, container traits base.
  template<typename _Tag>
    struct container_traits_base;

  /// Specialization, cc hash.
  template<>
  struct container_traits_base<cc_hash_tag>
  {
    typedef cc_hash_tag 				container_category;
    typedef point_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = false,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = false
      };
  };

  /// Specialization, gp hash.
  template<>
  struct container_traits_base<gp_hash_tag>
  {
    typedef gp_hash_tag 				container_category;
    typedef basic_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = false,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = false
      };
  };

  /// Specialization, rb tree.
  template<>
  struct container_traits_base<rb_tree_tag>
  {
    typedef rb_tree_tag 				container_category;
    typedef range_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = true,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = true
      };
  };

  /// Specialization, splay tree.
  template<>
  struct container_traits_base<splay_tree_tag>
  {
    typedef splay_tree_tag 				container_category;
    typedef range_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = true,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = true
      };
  };

  /// Specialization, ov tree.
  template<>
  struct container_traits_base<ov_tree_tag>
  {
    typedef ov_tree_tag 				container_category;
    typedef basic_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = true,
	erase_can_throw = true,
	split_join_can_throw = true,
	reverse_iteration = false
      };
  };

  /// Specialization, pat trie.
  template<>
  struct container_traits_base<pat_trie_tag>
  {
    typedef pat_trie_tag 				container_category;
    typedef range_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = true,
	erase_can_throw = false,
	split_join_can_throw = true,
	reverse_iteration = true
      };
  };

  /// Specialization, list update.
  template<>
  struct container_traits_base<list_update_tag>
  {
    typedef list_update_tag 				container_category;
    typedef point_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = false,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = false
      };
  };

  /// Specialization, pairing heap.
  template<>
  struct container_traits_base<pairing_heap_tag>
  {
    typedef pairing_heap_tag 				container_category;
    typedef point_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = false,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = false
      };
  };

  /// Specialization, thin heap.
  template<>
  struct container_traits_base<thin_heap_tag>
  {
    typedef thin_heap_tag 				container_category;
    typedef point_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = false,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = false
      };
  };

  /// Specialization, binomial heap.
  template<>
  struct container_traits_base<binomial_heap_tag>
  {
    typedef binomial_heap_tag 				container_category;
    typedef point_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = false,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = false
      };
  };

  /// Specialization, rc binomial heap.
  template<>
  struct container_traits_base<rc_binomial_heap_tag>
  {
    typedef rc_binomial_heap_tag 			container_category;
    typedef point_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = false,
	erase_can_throw = false,
	split_join_can_throw = false,
	reverse_iteration = false
      };
  };

  /// Specialization, binary heap.
  template<>
  struct container_traits_base<binary_heap_tag>
  {
    typedef binary_heap_tag 				container_category;
    typedef basic_invalidation_guarantee 		invalidation_guarantee;

    enum
      {
	order_preserving = false,
	erase_can_throw = false,
	split_join_can_throw = true,
	reverse_iteration = false
      };
  };


  /// Container traits.
  // See Matt Austern for the name, S. Meyers MEFC++ #2, others.
  template<typename Cntnr>
  struct container_traits
  : public container_traits_base<typename Cntnr::container_category>
  {
    typedef Cntnr 				       container_type;
    typedef typename Cntnr::container_category         container_category;
    typedef container_traits_base<container_category>  base_type;
    typedef typename base_type::invalidation_guarantee invalidation_guarantee;

    enum
      {
	/// True only if Cntnr objects guarantee storing  keys by order.
	order_preserving = base_type::order_preserving,

	/// True only if erasing a key can throw.
	erase_can_throw = base_type::erase_can_throw,

	/// True only if split or join operations can throw.
	split_join_can_throw = base_type::split_join_can_throw,

	/// True only reverse iterators are supported.
	reverse_iteration = base_type::reverse_iteration
      };
  };
  ///@}


  namespace detail
  {
    /// Dispatch mechanism, primary template for associative types.
    template<typename Key, typename Mapped, typename _Alloc, typename Tag,
	     typename Policy_Tl = null_type>
      struct container_base_dispatch;
  } // namespace detail
  ///@}
} // namespace __gnu_pbds

#endif
