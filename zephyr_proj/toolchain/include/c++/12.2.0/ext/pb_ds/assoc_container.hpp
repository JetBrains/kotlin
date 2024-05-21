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
 * @file assoc_container.hpp
 * Contains associative containers.
 */

#ifndef PB_DS_ASSOC_CNTNR_HPP
#define PB_DS_ASSOC_CNTNR_HPP

#include <bits/c++config.h>
#include <ext/typelist.h>
#include <ext/pb_ds/tag_and_trait.hpp>
#include <ext/pb_ds/detail/standard_policies.hpp>
#include <ext/pb_ds/detail/container_base_dispatch.hpp>
#include <ext/pb_ds/detail/branch_policy/traits.hpp>

namespace __gnu_pbds
{
  /**
   *  @defgroup containers-pbds Containers
   *  @ingroup pbds
   *  @{
   */

  /**
   *  @defgroup hash-based Hash-Based
   *  @ingroup containers-pbds
   *  @{
   */
#define PB_DS_HASH_BASE \
  detail::container_base_dispatch<Key, Mapped, _Alloc, Tag, \
    typename __gnu_cxx::typelist::append< \
    typename __gnu_cxx::typelist::create4<Hash_Fn, Eq_Fn, Resize_Policy, \
    detail::integral_constant<int, Store_Hash> >::type, Policy_Tl>::type>::type

  /**
   *  @defgroup hash-detail Base and Policy Classes
   *  @ingroup hash-based
   */

  /**
   *  A hashed container abstraction.
   *
   *  @tparam Key 	    	Key type.
   *  @tparam Mapped 	    	Map type.
   *  @tparam Hash_Fn	    	Hashing functor.
   *  @tparam Eq_Fn	    	Equal functor.
   *  @tparam Resize_Policy 	Resizes hash.
   *  @tparam Store_Hash    	Indicates whether the hash value
   *                            will be stored along with each key.
   *  @tparam Tag 	    	Instantiating data structure type,
   *			    	see container_tag.
   *  @tparam Policy_TL	    	Policy typelist.
   *  @tparam _Alloc 	    	Allocator type.
   *
   *  Base is dispatched at compile time via Tag, from the following
   *  choices: cc_hash_tag, gp_hash_tag, and descendants of basic_hash_tag.
   *
   *  Base choices are: detail::cc_ht_map, detail::gp_ht_map
   */
  template<typename Key,
	   typename Mapped,
	   typename Hash_Fn,
	   typename Eq_Fn,
	   typename Resize_Policy,
	   bool Store_Hash,
	   typename Tag,
	   typename Policy_Tl,
	   typename _Alloc>
  class basic_hash_table : public PB_DS_HASH_BASE
  {
  private:
    typedef typename PB_DS_HASH_BASE 		base_type;

  public:
    virtual
    ~basic_hash_table() { }

  protected:
    basic_hash_table() { }

    basic_hash_table(const basic_hash_table& other)
    : base_type((const base_type&)other) { }

    template<typename T0>
      basic_hash_table(T0 t0) : base_type(t0) { }

    template<typename T0, typename T1>
      basic_hash_table(T0 t0, T1 t1) : base_type(t0, t1) { }

    template<typename T0, typename T1, typename T2>
      basic_hash_table(T0 t0, T1 t1, T2 t2) : base_type(t0, t1, t2) { }

    template<typename T0, typename T1, typename T2, typename T3>
      basic_hash_table(T0 t0, T1 t1, T2 t2, T3 t3)
      : base_type(t0, t1, t2, t3) { }

    template<typename T0, typename T1, typename T2, typename T3, typename T4>
      basic_hash_table(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4)
      : base_type(t0, t1, t2, t3, t4) { }

    template<typename T0, typename T1, typename T2, typename T3, typename T4,
	     typename T5>
      basic_hash_table(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5)
      : base_type(t0, t1, t2, t3, t4, t5) { }

    template<typename T0, typename T1, typename T2, typename T3, typename T4,
	     typename T5, typename T6>
      basic_hash_table(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6)
      : base_type(t0, t1, t2, t3, t4, t5, t6) { }

    template<typename T0, typename T1, typename T2, typename T3, typename T4,
	     typename T5, typename T6, typename T7>
      basic_hash_table(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7)
      : base_type(t0, t1, t2, t3, t4, t5, t6, t7) { }

    template<typename T0, typename T1, typename T2, typename T3, typename T4,
	     typename T5, typename T6, typename T7, typename T8>
      basic_hash_table(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6,
		       T7 t7, T8 t8)
      : base_type(t0, t1, t2, t3, t4, t5, t6, t7, t8)
      { }

  private:
    basic_hash_table&
    operator=(const base_type&);
  };

#undef PB_DS_HASH_BASE


#define PB_DS_CC_HASH_BASE \
  basic_hash_table<Key, Mapped,	Hash_Fn, Eq_Fn, Resize_Policy, Store_Hash, \
		   cc_hash_tag,	\
	  typename __gnu_cxx::typelist::create1<Comb_Hash_Fn>::type, _Alloc>


  /**
   *  A collision-chaining hash-based associative container.
   *
   *  @tparam Key 	    	Key type.
   *  @tparam Mapped 	    	Map type.
   *  @tparam Hash_Fn	    	Hashing functor.
   *  @tparam Eq_Fn	    	Equal functor.
   *  @tparam Comb_Hash_Fn	Combining hash functor.
   *                            If Hash_Fn is not null_type, then this
   *                            is the ranged-hash functor; otherwise,
   *                            this is the range-hashing functor.
   *                    XXX(See Design::Hash-Based Containers::Hash Policies.)
   *  @tparam Resize_Policy 	Resizes hash.
   *  @tparam Store_Hash    	Indicates whether the hash value
   *                            will be stored along with each key.
   *                            If Hash_Fn is null_type, then the
   *                            container will not compile if this
   *                            value is true
   *  @tparam _Alloc 	    	Allocator type.
   *
   *  Base tag choices are: 	cc_hash_tag.
   *
   *  Base is basic_hash_table.
   */
  template<typename Key,
	   typename Mapped,
	   typename Hash_Fn = typename detail::default_hash_fn<Key>::type,
	   typename Eq_Fn = typename detail::default_eq_fn<Key>::type,
	   typename Comb_Hash_Fn = detail::default_comb_hash_fn::type,
	   typename Resize_Policy = typename detail::default_resize_policy<Comb_Hash_Fn>::type,
	   bool Store_Hash = detail::default_store_hash,
	   typename _Alloc = std::allocator<char> >
  class cc_hash_table :  public PB_DS_CC_HASH_BASE
  {
  private:
    typedef PB_DS_CC_HASH_BASE 			base_type;

  public:
    typedef cc_hash_tag	       			container_category;
    typedef Hash_Fn 				hash_fn;
    typedef Eq_Fn 				eq_fn;
    typedef Resize_Policy 			resize_policy;
    typedef Comb_Hash_Fn 			comb_hash_fn;

    /// Default constructor.
    cc_hash_table() { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the Hash_Fn object of the container object.
    cc_hash_table(const hash_fn& h)
    : base_type(h) { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the hash_fn object of the container object, and
    /// r_eq_fn will be copied by the eq_fn object of the container
    /// object.
    cc_hash_table(const hash_fn& h, const eq_fn& e)
    : base_type(h, e) { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the hash_fn object of the container object, r_eq_fn
    /// will be copied by the eq_fn object of the container object,
    /// and r_comb_hash_fn will be copied by the comb_hash_fn object
    /// of the container object.
    cc_hash_table(const hash_fn& h, const eq_fn& e, const comb_hash_fn& ch)
    : base_type(h, e, ch) { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the hash_fn object of the container object, r_eq_fn
    /// will be copied by the eq_fn object of the container object,
    /// r_comb_hash_fn will be copied by the comb_hash_fn object of
    /// the container object, and r_resize_policy will be copied by
    /// the resize_policy object of the container object.
    cc_hash_table(const hash_fn& h, const eq_fn& e, const comb_hash_fn& ch,
		  const resize_policy& rp)
    : base_type(h, e, ch, rp) { }

    /// Constructor taking __iterators to a range of value_types. The
    /// value_types between first_it and last_it will be inserted into
    /// the container object.
    template<typename It>
    cc_hash_table(It first, It last)
    { base_type::copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects. The value_types between first_it and
    /// last_it will be inserted into the container object.
    template<typename It>
    cc_hash_table(It first, It last, const hash_fn& h)
    : base_type(h)
    { this->copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects The value_types between first_it and
    /// last_it will be inserted into the container object. r_hash_fn
    /// will be copied by the hash_fn object of the container object,
    /// and r_eq_fn will be copied by the eq_fn object of the
    /// container object.
    template<typename It>
    cc_hash_table(It first, It last, const hash_fn& h, const eq_fn& e)
    : base_type(h, e)
    { this->copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects The value_types between first_it and
    /// last_it will be inserted into the container object. r_hash_fn
    /// will be copied by the hash_fn object of the container object,
    /// r_eq_fn will be copied by the eq_fn object of the container
    /// object, and r_comb_hash_fn will be copied by the comb_hash_fn
    /// object of the container object.
    template<typename It>
    cc_hash_table(It first, It last, const hash_fn& h, const eq_fn& e,
		  const comb_hash_fn& ch)
    : base_type(h, e, ch)
    { this->copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects The value_types between first_it and
    /// last_it will be inserted into the container object. r_hash_fn
    /// will be copied by the hash_fn object of the container object,
    /// r_eq_fn will be copied by the eq_fn object of the container
    /// object, r_comb_hash_fn will be copied by the comb_hash_fn
    /// object of the container object, and r_resize_policy will be
    /// copied by the resize_policy object of the container object.
    template<typename It>
    cc_hash_table(It first, It last, const hash_fn& h, const eq_fn& e,
		  const comb_hash_fn& ch, const resize_policy& rp)
    : base_type(h, e, ch, rp)
    { this->copy_from_range(first, last); }

    cc_hash_table(const cc_hash_table& other)
    : base_type((const base_type&)other)
    { }

    virtual
    ~cc_hash_table() { }

    cc_hash_table&
    operator=(const cc_hash_table& other)
    {
      if (this != &other)
	{
	  cc_hash_table tmp(other);
	  swap(tmp);
	}
      return *this;
    }

    void
    swap(cc_hash_table& other)
    { base_type::swap(other); }
  };

#undef PB_DS_CC_HASH_BASE


#define PB_DS_GP_HASH_BASE \
  basic_hash_table<Key, Mapped,	Hash_Fn, Eq_Fn, Resize_Policy, Store_Hash, \
		   gp_hash_tag, \
  typename __gnu_cxx::typelist::create2<Comb_Probe_Fn, Probe_Fn>::type, _Alloc>


  /**
   *  A general-probing hash-based associative container.
   *
   *  @tparam Key 	    	Key type.
   *  @tparam Mapped 	    	Map type.
   *  @tparam Hash_Fn	    	Hashing functor.
   *  @tparam Eq_Fn	    	Equal functor.
   *  @tparam Comb_Probe_Fn	Combining probe functor.
   *                            If Hash_Fn is not null_type, then this
   *                            is the ranged-probe functor; otherwise,
   *                            this is the range-hashing functor.
   *                    XXX See Design::Hash-Based Containers::Hash Policies.
   *  @tparam Probe_Fn		Probe functor.
   *  @tparam Resize_Policy 	Resizes hash.
   *  @tparam Store_Hash    	Indicates whether the hash value
   *                            will be stored along with each key.
   *                            If Hash_Fn is null_type, then the
   *                            container will not compile if this
   *                            value is true
   *  @tparam _Alloc 	    	Allocator type.
   *
   *  Base tag choices are: 	gp_hash_tag.
   *
   *  Base is basic_hash_table.
   */
  template<typename Key,
	   typename Mapped,
	   typename Hash_Fn = typename detail::default_hash_fn<Key>::type,
	   typename Eq_Fn = typename detail::default_eq_fn<Key>::type,
	   typename Comb_Probe_Fn = detail::default_comb_hash_fn::type,
	   typename Probe_Fn = typename detail::default_probe_fn<Comb_Probe_Fn>::type,
	   typename Resize_Policy = typename detail::default_resize_policy<Comb_Probe_Fn>::type,
	   bool Store_Hash = detail::default_store_hash,
	   typename _Alloc = std::allocator<char> >
  class gp_hash_table : public PB_DS_GP_HASH_BASE
  {
  private:
    typedef PB_DS_GP_HASH_BASE 			base_type;

  public:
    typedef gp_hash_tag	       			container_category;
    typedef Hash_Fn 				hash_fn;
    typedef Eq_Fn 				eq_fn;
    typedef Comb_Probe_Fn			comb_probe_fn;
    typedef Probe_Fn 				probe_fn;
    typedef Resize_Policy 			resize_policy;

    /// Default constructor.
    gp_hash_table() { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the hash_fn object of the container object.
    gp_hash_table(const hash_fn& h)
    : base_type(h) { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the hash_fn object of the container object, and
    /// r_eq_fn will be copied by the eq_fn object of the container
    /// object.
    gp_hash_table(const hash_fn& h, const eq_fn& e)
    : base_type(h, e) { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the hash_fn object of the container object, r_eq_fn
    /// will be copied by the eq_fn object of the container object,
    /// and r_comb_probe_fn will be copied by the comb_probe_fn object
    /// of the container object.
    gp_hash_table(const hash_fn& h, const eq_fn& e, const comb_probe_fn& cp)
    : base_type(h, e, cp) { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the hash_fn object of the container object, r_eq_fn
    /// will be copied by the eq_fn object of the container object,
    /// r_comb_probe_fn will be copied by the comb_probe_fn object of
    /// the container object, and r_probe_fn will be copied by the
    /// probe_fn object of the container object.
    gp_hash_table(const hash_fn& h, const eq_fn& e, const comb_probe_fn& cp,
		  const probe_fn& p)
    : base_type(h, e, cp, p) { }

    /// Constructor taking some policy objects. r_hash_fn will be
    /// copied by the hash_fn object of the container object, r_eq_fn
    /// will be copied by the eq_fn object of the container object,
    /// r_comb_probe_fn will be copied by the comb_probe_fn object of
    /// the container object, r_probe_fn will be copied by the
    /// probe_fn object of the container object, and r_resize_policy
    /// will be copied by the Resize_Policy object of the container
    /// object.
    gp_hash_table(const hash_fn& h, const eq_fn& e, const comb_probe_fn& cp,
		  const probe_fn& p, const resize_policy& rp)
    : base_type(h, e, cp, p, rp) { }

    /// Constructor taking __iterators to a range of value_types. The
    /// value_types between first_it and last_it will be inserted into
    /// the container object.
    template<typename It>
    gp_hash_table(It first, It last)
    { base_type::copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects. The value_types between first_it and
    /// last_it will be inserted into the container object. r_hash_fn
    /// will be copied by the hash_fn object of the container object.
    template<typename It>
    gp_hash_table(It first, It last, const hash_fn& h)
    : base_type(h)
    { base_type::copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects. The value_types between first_it and
    /// last_it will be inserted into the container object. r_hash_fn
    /// will be copied by the hash_fn object of the container object,
    /// and r_eq_fn will be copied by the eq_fn object of the
    /// container object.
    template<typename It>
    gp_hash_table(It first, It last, const hash_fn& h, const eq_fn& e)
    : base_type(h, e)
    { base_type::copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects. The value_types between first_it and
    /// last_it will be inserted into the container object. r_hash_fn
    /// will be copied by the hash_fn object of the container object,
    /// r_eq_fn will be copied by the eq_fn object of the container
    /// object, and r_comb_probe_fn will be copied by the
    /// comb_probe_fn object of the container object.
    template<typename It>
    gp_hash_table(It first, It last, const hash_fn& h, const eq_fn& e,
		  const comb_probe_fn& cp)
    : base_type(h, e, cp)
    { base_type::copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects. The value_types between first_it and
    /// last_it will be inserted into the container object. r_hash_fn
    /// will be copied by the hash_fn object of the container object,
    /// r_eq_fn will be copied by the eq_fn object of the container
    /// object, r_comb_probe_fn will be copied by the comb_probe_fn
    /// object of the container object, and r_probe_fn will be copied
    /// by the probe_fn object of the container object.
    template<typename It>
    gp_hash_table(It first, It last, const hash_fn& h, const eq_fn& e,
		  const comb_probe_fn& cp, const probe_fn& p)
    : base_type(h, e, cp, p)
    { base_type::copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects. The value_types between first_it and
    /// last_it will be inserted into the container object. r_hash_fn
    /// will be copied by the hash_fn object of the container object,
    /// r_eq_fn will be copied by the eq_fn object of the container
    /// object, r_comb_probe_fn will be copied by the comb_probe_fn
    /// object of the container object, r_probe_fn will be copied by
    /// the probe_fn object of the container object, and
    /// r_resize_policy will be copied by the resize_policy object of
    /// the container object.
    template<typename It>
    gp_hash_table(It first, It last, const hash_fn& h, const eq_fn& e,
		  const comb_probe_fn& cp, const probe_fn& p,
		  const resize_policy& rp)
    : base_type(h, e, cp, p, rp)
    { base_type::copy_from_range(first, last); }

    gp_hash_table(const gp_hash_table& other)
    : base_type((const base_type&)other)
    { }

    virtual
    ~gp_hash_table() { }

    gp_hash_table&
    operator=(const gp_hash_table& other)
    {
      if (this != &other)
	{
	  gp_hash_table tmp(other);
	  swap(tmp);
	}
      return *this;
    }

    void
    swap(gp_hash_table& other)
    { base_type::swap(other); }
  };
  ///@} hash-based
#undef PB_DS_GP_HASH_BASE


  /**
   *  @defgroup branch-based Branch-Based
   *  @ingroup containers-pbds
   *  @{
   */
#define PB_DS_BRANCH_BASE \
  detail::container_base_dispatch<Key, Mapped, _Alloc, Tag, Policy_Tl>::type

  /**
   *  @defgroup branch-detail Base and Policy Classes
   *  @ingroup branch-based
   */

  /**
   *  A branched, tree-like (tree, trie) container abstraction.
   *
   *  @tparam Key 	  	Key type.
   *  @tparam Mapped 	  	Map type.
   *  @tparam Tag 	  	Instantiating data structure type,
   *                            see container_tag.
   *  @tparam Node_Update 	Updates nodes, restores invariants.
   *  @tparam Policy_TL         Policy typelist.
   *  @tparam _Alloc 	  	Allocator type.
   *
   *  Base is dispatched at compile time via Tag, from the following
   *  choices: tree_tag, trie_tag, and their descendants.
   *
   *  Base choices are: detail::ov_tree_map, detail::rb_tree_map,
   *		       	detail::splay_tree_map, and detail::pat_trie_map.
   */
  template<typename Key, typename Mapped, typename Tag,
	   typename Node_Update, typename Policy_Tl, typename _Alloc>
  class basic_branch : public PB_DS_BRANCH_BASE
  {
  private:
    typedef typename PB_DS_BRANCH_BASE 	       	base_type;

  public:
    typedef Node_Update 			node_update;

    virtual
    ~basic_branch() { }

  protected:
    basic_branch() { }

    basic_branch(const basic_branch& other)
    : base_type((const base_type&)other) { }

    template<typename T0>
      basic_branch(T0 t0) : base_type(t0) { }

    template<typename T0, typename T1>
      basic_branch(T0 t0, T1 t1) : base_type(t0, t1) { }

    template<typename T0, typename T1, typename T2>
      basic_branch(T0 t0, T1 t1, T2 t2) : base_type(t0, t1, t2) { }

    template<typename T0, typename T1, typename T2, typename T3>
      basic_branch(T0 t0, T1 t1, T2 t2, T3 t3)
      : base_type(t0, t1, t2, t3) { }

    template<typename T0, typename T1, typename T2, typename T3, typename T4>
      basic_branch(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4)
      : base_type(t0, t1, t2, t3, t4) { }

    template<typename T0, typename T1, typename T2, typename T3, typename T4,
	     typename T5>
      basic_branch(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5)
      : base_type(t0, t1, t2, t3, t4, t5) { }

    template<typename T0, typename T1, typename T2, typename T3, typename T4,
	     typename T5, typename T6>
      basic_branch(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6)
      : base_type(t0, t1, t2, t3, t4, t5, t6) { }
  };
#undef PB_DS_BRANCH_BASE


#define PB_DS_TREE_NODE_AND_IT_TRAITS \
  detail::tree_traits<Key, Mapped,Cmp_Fn,Node_Update,Tag,_Alloc>

#define PB_DS_TREE_BASE \
  basic_branch<Key,Mapped, Tag, \
	       typename PB_DS_TREE_NODE_AND_IT_TRAITS::node_update, \
	       typename __gnu_cxx::typelist::create2<Cmp_Fn, \
	       PB_DS_TREE_NODE_AND_IT_TRAITS>::type, _Alloc>


  /**
   *  A tree-based container.
   *
   *  @tparam Key 	 	Key type.
   *  @tparam Mapped 	 	Map type.
   *  @tparam Cmp_Fn	 	Comparison functor.
   *  @tparam Tag 	 	Instantiating data structure type,
   *                            see container_tag.
   *  @tparam Node_Update 	Updates tree internal-nodes,
   *                            restores invariants when invalidated.
   *                     XXX See design::tree-based-containers::node invariants.
   *  @tparam _Alloc 	 	Allocator type.
   *
   *  Base tag choices are: ov_tree_tag, rb_tree_tag, splay_tree_tag.
   *
   *  Base is basic_branch.
   */
  template<typename Key, typename Mapped, typename Cmp_Fn = std::less<Key>,
	   typename Tag = rb_tree_tag,
	   template<typename Node_CItr, typename Node_Itr,
		    typename Cmp_Fn_, typename _Alloc_>
	   class Node_Update = null_node_update,
	   typename _Alloc = std::allocator<char> >
  class tree : public PB_DS_TREE_BASE
  {
  private:
    typedef PB_DS_TREE_BASE 			base_type;

  public:
    /// Comparison functor type.
    typedef Cmp_Fn 				cmp_fn;

    tree() { }

    /// Constructor taking some policy objects. r_cmp_fn will be
    /// copied by the Cmp_Fn object of the container object.
    tree(const cmp_fn& c)
    : base_type(c) { }

    /// Constructor taking __iterators to a range of value_types. The
    /// value_types between first_it and last_it will be inserted into
    /// the container object.
    template<typename It>
    tree(It first, It last)
    { base_type::copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects The value_types between first_it and
    /// last_it will be inserted into the container object. r_cmp_fn
    /// will be copied by the cmp_fn object of the container object.
    template<typename It>
    tree(It first, It last, const cmp_fn& c)
    : base_type(c)
    { base_type::copy_from_range(first, last); }

    tree(const tree& other)
    : base_type((const base_type&)other) { }

    virtual
    ~tree() { }

    tree&
    operator=(const tree& other)
    {
      if (this != &other)
	{
	  tree tmp(other);
	  swap(tmp);
	}
      return *this;
    }

    void
    swap(tree& other)
    { base_type::swap(other); }
  };

#undef PB_DS_TREE_BASE
#undef PB_DS_TREE_NODE_AND_IT_TRAITS


#define PB_DS_TRIE_NODE_AND_IT_TRAITS \
  detail::trie_traits<Key,Mapped,_ATraits,Node_Update,Tag,_Alloc>

#define PB_DS_TRIE_BASE \
  basic_branch<Key,Mapped,Tag, \
	       typename PB_DS_TRIE_NODE_AND_IT_TRAITS::node_update, \
	       typename __gnu_cxx::typelist::create2<_ATraits, \
	       PB_DS_TRIE_NODE_AND_IT_TRAITS >::type, _Alloc>


  /**
   *  A trie-based container.
   *
   *  @tparam Key 	  	Key type.
   *  @tparam Mapped 	  	Map type.
   *  @tparam _ATraits	  	Element access traits.
   *  @tparam Tag 	  	Instantiating data structure type,
   *                            see container_tag.
   *  @tparam Node_Update 	Updates trie internal-nodes,
   *                            restores invariants when invalidated.
   *                     XXX See design::tree-based-containers::node invariants.
   *  @tparam _Alloc 	  	Allocator type.
   *
   *  Base tag choice is pat_trie_tag.
   *
   *  Base is basic_branch.
   */
  template<typename Key,
	   typename Mapped,
	   typename _ATraits = \
		    typename detail::default_trie_access_traits<Key>::type,
	   typename Tag = pat_trie_tag,
	   template<typename Node_CItr,
		    typename Node_Itr,
		    typename _ATraits_,
		    typename _Alloc_>
	   class Node_Update = null_node_update,
	   typename _Alloc = std::allocator<char> >
  class trie : public PB_DS_TRIE_BASE
  {
  private:
    typedef PB_DS_TRIE_BASE			base_type;

  public:
    /// Element access traits type.
    typedef _ATraits 				access_traits;

    trie() { }

    /// Constructor taking some policy objects. r_access_traits will
    /// be copied by the _ATraits object of the container object.
    trie(const access_traits& t)
    : base_type(t) { }

    /// Constructor taking __iterators to a range of value_types. The
    /// value_types between first_it and last_it will be inserted into
    /// the container object.
    template<typename It>
    trie(It first, It last)
    { base_type::copy_from_range(first, last); }

    /// Constructor taking __iterators to a range of value_types and
    /// some policy objects. The value_types between first_it and
    /// last_it will be inserted into the container object.
    template<typename It>
    trie(It first, It last, const access_traits& t)
    : base_type(t)
    { base_type::copy_from_range(first, last); }

    trie(const trie& other)
    : base_type((const base_type&)other) { }

    virtual
    ~trie() { }

    trie&
    operator=(const trie& other)
    {
      if (this != &other)
	{
	  trie tmp(other);
	  swap(tmp);
	}
      return *this;
    }

    void
    swap(trie& other)
    { base_type::swap(other); }
  };
  ///@} branch-based
#undef PB_DS_TRIE_BASE
#undef PB_DS_TRIE_NODE_AND_IT_TRAITS


  /**
   *  @defgroup list-based List-Based
   *  @ingroup containers-pbds
   *  @{
   */
#define PB_DS_LU_BASE \
  detail::container_base_dispatch<Key, Mapped, _Alloc, list_update_tag,	\
    typename __gnu_cxx::typelist::create2<Eq_Fn, Update_Policy>::type>::type


  /**
   *  A list-update based associative container.
   *
   *  @tparam Key 	    	Key type.
   *  @tparam Mapped 	    	Map type.
   *  @tparam Eq_Fn	    	Equal functor.
   *  @tparam Update_Policy	Update policy, determines when an element
   *                            will be moved to the front of the list.
   *  @tparam _Alloc 	    	Allocator type.
   *
   *  Base is detail::lu_map.
   */
  template<typename Key,
	   typename Mapped,
	   class Eq_Fn = typename detail::default_eq_fn<Key>::type,
	   class Update_Policy = detail::default_update_policy::type,
	   class _Alloc = std::allocator<char> >
  class list_update : public PB_DS_LU_BASE
  {
  private:
    typedef typename PB_DS_LU_BASE 		base_type;

  public:
    typedef list_update_tag	       		container_category;
    typedef Eq_Fn 				eq_fn;
    typedef Update_Policy 			update_policy;

    list_update() { }

    /// Constructor taking __iterators to a range of value_types. The
    /// value_types between first_it and last_it will be inserted into
    /// the container object.
    template<typename It>
    list_update(It first, It last)
    { base_type::copy_from_range(first, last); }

    list_update(const list_update& other)
    : base_type((const base_type&)other) { }

    virtual
    ~list_update() { }

    list_update&
    operator=(const list_update& other)
    {
      if (this !=& other)
	{
	  list_update tmp(other);
	  swap(tmp);
	}
      return *this;
    }

    void
    swap(list_update& other)
    { base_type::swap(other); }
  };
  ///@} list-based
#undef PB_DS_LU_BASE

  /// @} group containers-pbds
} // namespace __gnu_pbds

#endif
