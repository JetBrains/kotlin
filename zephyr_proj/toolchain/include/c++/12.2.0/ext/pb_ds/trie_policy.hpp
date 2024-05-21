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
 * @file trie_policy.hpp
 * Contains trie-related policies.
 */

#ifndef PB_DS_TRIE_POLICY_HPP
#define PB_DS_TRIE_POLICY_HPP

#include <bits/c++config.h>
#include <string>
#include <ext/pb_ds/detail/type_utils.hpp>
#include <ext/pb_ds/detail/trie_policy/trie_policy_base.hpp>

namespace __gnu_pbds
{
#define PB_DS_CLASS_T_DEC \
  template<typename String, typename String::value_type Min_E_Val, \
	   typename String::value_type Max_E_Val, bool Reverse, \
	   typename _Alloc>

#define PB_DS_CLASS_C_DEC \
  trie_string_access_traits<String, Min_E_Val,Max_E_Val,Reverse,_Alloc>

  /**
   *  Element access traits for string types.
   *
   *  @tparam String 	    	String type.
   *  @tparam Min_E_Val        	Minimal element value.
   *  @tparam Max_E_Val	    	Maximum element value.
   *  @tparam Reverse	        Reverse iteration should be used.
   *                            Default: false.
   *  @tparam _Alloc 	    	Allocator type.
   */
  template<typename String = std::string,
	   typename String::value_type Min_E_Val = detail::__numeric_traits<typename String::value_type>::__min,
	   typename String::value_type Max_E_Val = detail::__numeric_traits<typename String::value_type>::__max,
	   bool Reverse = false,
	   typename _Alloc = std::allocator<char> >
  struct trie_string_access_traits
  {
  public:
    typedef typename _Alloc::size_type			  size_type;
    typedef String 					  key_type;
    typedef typename detail::rebind_traits<_Alloc, key_type>::const_reference
      key_const_reference;

    enum
      {
	reverse = Reverse
      };

    /// Element const iterator type.
    typedef typename detail::__conditional_type<Reverse, \
		       typename String::const_reverse_iterator, \
		       typename String::const_iterator>::__type const_iterator;

    /// Element type.
    typedef typename std::iterator_traits<const_iterator>::value_type e_type;

    enum
      {
	min_e_val = Min_E_Val,
	max_e_val = Max_E_Val,
	max_size = max_e_val - min_e_val + 1
      };
    PB_DS_STATIC_ASSERT(min_max_size, max_size >= 2);

    /// Returns a const_iterator to the first element of
    /// key_const_reference agumnet.
    inline static const_iterator
    begin(key_const_reference);

    /// Returns a const_iterator to the after-last element of
    /// key_const_reference argument.
    inline static const_iterator
    end(key_const_reference);

    /// Maps an element to a position.
    inline static size_type
    e_pos(e_type e);

  private:
    inline static const_iterator
    begin_imp(key_const_reference, detail::false_type);

    inline static const_iterator
    begin_imp(key_const_reference, detail::true_type);

    inline static const_iterator
    end_imp(key_const_reference, detail::false_type);

    inline static const_iterator
    end_imp(key_const_reference, detail::true_type);

    static detail::integral_constant<int, Reverse> s_rev_ind;
  };

#include <ext/pb_ds/detail/trie_policy/trie_string_access_traits_imp.hpp>

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

#define PB_DS_CLASS_T_DEC \
  template<typename Node_CItr,typename Node_Itr, \
	   typename _ATraits, typename _Alloc>

#define PB_DS_CLASS_C_DEC \
  trie_prefix_search_node_update<Node_CItr, Node_Itr, \
				 _ATraits,_Alloc>

#define PB_DS_TRIE_POLICY_BASE \
  detail::trie_policy_base<Node_CItr,Node_Itr,_ATraits, _Alloc>

  /// A node updator that allows tries to be searched for the range of
  /// values that match a certain prefix.
  template<typename Node_CItr,
	   typename Node_Itr,
	   typename _ATraits,
	   typename _Alloc>
  class trie_prefix_search_node_update : private PB_DS_TRIE_POLICY_BASE
  {
  private:
    typedef PB_DS_TRIE_POLICY_BASE 		       	base_type;

  public:
    typedef typename base_type::key_type 		key_type;
    typedef typename base_type::key_const_reference 	key_const_reference;

    /// Element access traits.
    typedef _ATraits 				access_traits;

    /// Const element iterator.
    typedef typename access_traits::const_iterator 	a_const_iterator;

    /// _Alloc type.
    typedef _Alloc 	       				allocator_type;

    /// Size type.
    typedef typename allocator_type::size_type 		size_type;
    typedef null_type 					metadata_type;
    typedef Node_Itr 					node_iterator;
    typedef Node_CItr 					node_const_iterator;
    typedef typename node_iterator::value_type 		iterator;
    typedef typename node_const_iterator::value_type 	const_iterator;

    /// Finds the const iterator range corresponding to all values
    /// whose prefixes match r_key.
    std::pair<const_iterator, const_iterator>
    prefix_range(key_const_reference) const;

    /// Finds the iterator range corresponding to all values whose
    /// prefixes match r_key.
    std::pair<iterator, iterator>
    prefix_range(key_const_reference);

    /// Finds the const iterator range corresponding to all values
    /// whose prefixes match [b, e).
    std::pair<const_iterator, const_iterator>
    prefix_range(a_const_iterator, a_const_iterator) const;

    /// Finds the iterator range corresponding to all values whose
    /// prefixes match [b, e).
    std::pair<iterator, iterator>
    prefix_range(a_const_iterator, a_const_iterator);

  protected:
    /// Called to update a node's metadata.
    inline void
    operator()(node_iterator node_it, node_const_iterator end_nd_it) const;

  private:
    node_iterator
    next_child(node_iterator, a_const_iterator, a_const_iterator,
	       node_iterator, const access_traits&);

    /// Returns the const iterator associated with the just-after last element.
    virtual const_iterator
    end() const = 0;

    /// Returns the iterator associated with the just-after last element.
    virtual iterator
    end() = 0;

    /// Returns the node_const_iterator associated with the trie's root node.
    virtual node_const_iterator
    node_begin() const = 0;

    /// Returns the node_iterator associated with the trie's root node.
    virtual node_iterator
    node_begin() = 0;

    /// Returns the node_const_iterator associated with a just-after leaf node.
    virtual node_const_iterator
    node_end() const = 0;

    /// Returns the node_iterator associated with a just-after leaf node.
    virtual node_iterator
    node_end() = 0;

    /// Access to the cmp_fn object.
    virtual const access_traits&
    get_access_traits() const = 0;
  };

#include <ext/pb_ds/detail/trie_policy/prefix_search_node_update_imp.hpp>

#undef PB_DS_CLASS_C_DEC

#define PB_DS_CLASS_C_DEC \
  trie_order_statistics_node_update<Node_CItr, Node_Itr, \
				    _ATraits, _Alloc>

  /// Functor updating ranks of entrees.
  template<typename Node_CItr,
	   typename Node_Itr,
	   typename _ATraits,
	   typename _Alloc>
  class trie_order_statistics_node_update : private PB_DS_TRIE_POLICY_BASE
  {
  private:
    typedef PB_DS_TRIE_POLICY_BASE 		       	base_type;

  public:
    typedef _ATraits 				access_traits;
    typedef typename access_traits::const_iterator 	a_const_iterator;
    typedef _Alloc 					allocator_type;
    typedef typename allocator_type::size_type 		size_type;
    typedef typename base_type::key_type 		key_type;
    typedef typename base_type::key_const_reference 	key_const_reference;

    typedef size_type 					metadata_type;
    typedef Node_CItr 					node_const_iterator;
    typedef Node_Itr 					node_iterator;
    typedef typename node_const_iterator::value_type 	const_iterator;
    typedef typename node_iterator::value_type 		iterator;

    /// Finds an entry by __order. Returns a const_iterator to the
    /// entry with the __order order, or a const_iterator to the
    /// container object's end if order is at least the size of the
    /// container object.
    inline const_iterator
    find_by_order(size_type) const;

    /// Finds an entry by __order. Returns an iterator to the entry
    /// with the __order order, or an iterator to the container
    /// object's end if order is at least the size of the container
    /// object.
    inline iterator
    find_by_order(size_type);

    /// Returns the order of a key within a sequence. For exapmle, if
    /// r_key is the smallest key, this method will return 0; if r_key
    /// is a key between the smallest and next key, this method will
    /// return 1; if r_key is a key larger than the largest key, this
    /// method will return the size of r_c.
    inline size_type
    order_of_key(key_const_reference) const;

    /// Returns the order of a prefix within a sequence. For exapmle,
    /// if [b, e] is the smallest prefix, this method will return 0; if
    /// r_key is a key between the smallest and next key, this method
    /// will return 1; if r_key is a key larger than the largest key,
    /// this method will return the size of r_c.
    inline size_type
    order_of_prefix(a_const_iterator, a_const_iterator) const;

  protected:
    /// Updates the rank of a node through a node_iterator node_it;
    /// end_nd_it is the end node iterator.
    inline void
    operator()(node_iterator, node_const_iterator) const;

  private:
    typedef typename base_type::const_reference 	const_reference;
    typedef typename base_type::const_pointer 		const_pointer;

    typedef typename _Alloc::template rebind<metadata_type> __rebind_m;
    typedef typename __rebind_m::other 			__rebind_ma;
    typedef typename __rebind_ma::const_reference      metadata_const_reference;
    typedef typename __rebind_ma::reference 		metadata_reference;

    /// Returns true if the container is empty.
    _GLIBCXX_NODISCARD virtual bool
    empty() const = 0;

    /// Returns the iterator associated with the trie's first element.
    virtual iterator
    begin() = 0;

    /// Returns the iterator associated with the trie's
    /// just-after-last element.
    virtual iterator
    end() = 0;

    /// Returns the node_const_iterator associated with the trie's root node.
    virtual node_const_iterator
    node_begin() const = 0;

    /// Returns the node_iterator associated with the trie's root node.
    virtual node_iterator
    node_begin() = 0;

    /// Returns the node_const_iterator associated with a just-after
    /// leaf node.
    virtual node_const_iterator
    node_end() const = 0;

    /// Returns the node_iterator associated with a just-after leaf node.
    virtual node_iterator
    node_end() = 0;

    /// Access to the cmp_fn object.
    virtual access_traits&
    get_access_traits() = 0;
  };

#include <ext/pb_ds/detail/trie_policy/order_statistics_imp.hpp>

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_TRIE_POLICY_BASE

} // namespace __gnu_pbds

#endif
