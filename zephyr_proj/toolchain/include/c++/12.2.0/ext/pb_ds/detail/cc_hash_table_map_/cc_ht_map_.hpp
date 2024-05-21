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
 * @file cc_hash_table_map_/cc_ht_map_.hpp
 * Contains an implementation class for cc_ht_map_.
 */

#include <utility>
#include <iterator>
#include <memory>
#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/tag_and_trait.hpp>
#include <ext/pb_ds/detail/hash_fn/ranged_hash_fn.hpp>
#include <ext/pb_ds/detail/types_traits.hpp>
#include <ext/pb_ds/exception.hpp>
#include <ext/pb_ds/detail/eq_fn/hash_eq_fn.hpp>
#ifdef _GLIBCXX_DEBUG
#include <ext/pb_ds/detail/debug_map_base.hpp>
#endif
#ifdef PB_DS_HT_MAP_TRACE_
#include <iostream>
#endif
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#ifdef PB_DS_DATA_TRUE_INDICATOR
#define PB_DS_CC_HASH_NAME cc_ht_map
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
#define PB_DS_CC_HASH_NAME cc_ht_set
#endif

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Mapped, typename Hash_Fn, \
	     typename Eq_Fn, typename _Alloc, bool Store_Hash, \
	     typename Comb_Hash_Fn, typename Resize_Policy>

#define PB_DS_CLASS_C_DEC \
    PB_DS_CC_HASH_NAME<Key, Mapped, Hash_Fn, Eq_Fn, _Alloc,	\
		       Store_Hash, Comb_Hash_Fn, Resize_Policy>

#define PB_DS_HASH_EQ_FN_C_DEC \
    hash_eq_fn<Key, Eq_Fn, _Alloc, Store_Hash>

#define PB_DS_RANGED_HASH_FN_C_DEC \
    ranged_hash_fn<Key,	Hash_Fn, _Alloc, Comb_Hash_Fn, Store_Hash>

#define PB_DS_CC_HASH_TRAITS_BASE \
    types_traits<Key, Mapped, _Alloc, Store_Hash>

#ifdef _GLIBCXX_DEBUG
#define PB_DS_DEBUG_MAP_BASE_C_DEC \
    debug_map_base<Key,	Eq_Fn, \
		   typename rebind_traits<_Alloc, Key>::const_reference>
#endif


    /**
     *  A collision-chaining hash-based container.
     *
     *
     *  @ingroup hash-detail
     *
     *  @tparam Key 	    	Key type.
     *
     *  @tparam Mapped 	    	Map type.
     *
     *  @tparam Hash_Fn	      	Hashing functor.
     *                          Default is __gnu_cxx::hash.
     *
     *  @tparam Eq_Fn	      	Equal functor.
     *                          Default std::equal_to<Key>
     *
     *  @tparam _Alloc 	    	Allocator type.
     *
     *  @tparam Store_Hash    	If key type stores extra metadata.
     *                          Defaults to false.
     *
     *  @tparam Comb_Hash_Fn	Combining hash functor.
     *                          If Hash_Fn is not null_type, then this
     *                          is the ranged-hash functor; otherwise,
     *                          this is the range-hashing functor.
     *                    XXX(See Design::Hash-Based Containers::Hash Policies.)
     *                          Default direct_mask_range_hashing.
     *
     *  @tparam Resize_Policy 	Resizes hash.
     *                          Defaults to hash_standard_resize_policy,
     *                          using hash_exponential_size_policy and
     *                          hash_load_check_resize_trigger.
     *
     *
     *  Bases are: detail::hash_eq_fn, Resize_Policy, detail::ranged_hash_fn,
     *             detail::types_traits. (Optional: detail::debug_map_base.)
     */
    template<typename Key,
	     typename Mapped,
	     typename Hash_Fn,
	     typename Eq_Fn,
	     typename _Alloc,
	     bool Store_Hash,
	     typename Comb_Hash_Fn,
	     typename Resize_Policy>
    class PB_DS_CC_HASH_NAME:
#ifdef _GLIBCXX_DEBUG
      protected PB_DS_DEBUG_MAP_BASE_C_DEC,
#endif
      public PB_DS_HASH_EQ_FN_C_DEC,
      public Resize_Policy,
      public PB_DS_RANGED_HASH_FN_C_DEC,
      public PB_DS_CC_HASH_TRAITS_BASE
    {
    private:
      typedef PB_DS_CC_HASH_TRAITS_BASE	       	traits_base;
      typedef typename traits_base::comp_hash 	comp_hash;
      typedef typename traits_base::value_type 	value_type_;
      typedef typename traits_base::pointer 	pointer_;
      typedef typename traits_base::const_pointer const_pointer_;
      typedef typename traits_base::reference 	reference_;
      typedef typename traits_base::const_reference const_reference_;

      struct entry : public traits_base::stored_data_type
      {
	typename rebind_traits<_Alloc, entry>::pointer m_p_next;
      };

      typedef cond_dealtor<entry, _Alloc> 	cond_dealtor_t;

      typedef rebind_traits<_Alloc, entry> entry_traits;
      typedef typename entry_traits::allocator_type entry_allocator;
      typedef typename entry_traits::pointer entry_pointer;
      typedef typename entry_traits::const_pointer const_entry_pointer;
      typedef typename entry_traits::reference entry_reference;
      typedef typename entry_traits::const_reference const_entry_reference;

      typedef rebind_traits<_Alloc, entry_pointer> entry_pointer_traits;
      typedef typename entry_pointer_traits::allocator_type entry_pointer_allocator;
      typedef typename entry_pointer_traits::pointer entry_pointer_array;

      typedef PB_DS_RANGED_HASH_FN_C_DEC ranged_hash_fn_base;
      typedef PB_DS_HASH_EQ_FN_C_DEC hash_eq_fn_base;
      typedef Resize_Policy resize_base;

#ifdef _GLIBCXX_DEBUG
      typedef PB_DS_DEBUG_MAP_BASE_C_DEC 	debug_base;
#endif

#define PB_DS_GEN_POS std::pair<entry_pointer, typename _Alloc::size_type>

#include <ext/pb_ds/detail/unordered_iterator/point_const_iterator.hpp>
#include <ext/pb_ds/detail/unordered_iterator/point_iterator.hpp>
#include <ext/pb_ds/detail/unordered_iterator/const_iterator.hpp>
#include <ext/pb_ds/detail/unordered_iterator/iterator.hpp>

#undef PB_DS_GEN_POS

    public:
      typedef _Alloc 				allocator_type;
      typedef typename _Alloc::size_type 	size_type;
      typedef typename _Alloc::difference_type 	difference_type;
      typedef Hash_Fn 				hash_fn;
      typedef Eq_Fn 				eq_fn;
      typedef Comb_Hash_Fn 			comb_hash_fn;
      typedef Resize_Policy 			resize_policy;

      /// Value stores hash, true or false.
      enum
	{
	  store_hash = Store_Hash
	};

      typedef typename traits_base::key_type key_type;
      typedef typename traits_base::key_pointer key_pointer;
      typedef typename traits_base::key_const_pointer key_const_pointer;
      typedef typename traits_base::key_reference key_reference;
      typedef typename traits_base::key_const_reference key_const_reference;
      typedef typename traits_base::mapped_type mapped_type;
      typedef typename traits_base::mapped_pointer mapped_pointer;
      typedef typename traits_base::mapped_const_pointer mapped_const_pointer;
      typedef typename traits_base::mapped_reference mapped_reference;
      typedef typename traits_base::mapped_const_reference mapped_const_reference;
      typedef typename traits_base::value_type 	value_type;
      typedef typename traits_base::pointer 	pointer;
      typedef typename traits_base::const_pointer const_pointer;
      typedef typename traits_base::reference 	reference;
      typedef typename traits_base::const_reference const_reference;

#ifdef PB_DS_DATA_TRUE_INDICATOR
      typedef point_iterator_ 			point_iterator;
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
      typedef point_const_iterator_ 		point_iterator;
#endif

      typedef point_const_iterator_ 		point_const_iterator;

#ifdef PB_DS_DATA_TRUE_INDICATOR
      typedef iterator_ 			iterator;
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
      typedef const_iterator_ 			iterator;
#endif

      typedef const_iterator_ 			const_iterator;

      PB_DS_CC_HASH_NAME();

      PB_DS_CC_HASH_NAME(const Hash_Fn&);

      PB_DS_CC_HASH_NAME(const Hash_Fn&, const Eq_Fn&);

      PB_DS_CC_HASH_NAME(const Hash_Fn&, const Eq_Fn&, const Comb_Hash_Fn&);

      PB_DS_CC_HASH_NAME(const Hash_Fn&, const Eq_Fn&, const Comb_Hash_Fn&,
		       const Resize_Policy&);

      PB_DS_CC_HASH_NAME(const PB_DS_CLASS_C_DEC&);

      virtual
      ~PB_DS_CC_HASH_NAME();

      void
      swap(PB_DS_CLASS_C_DEC&);

      template<typename It>
      void
      copy_from_range(It, It);

      void
      initialize();

      inline size_type
      size() const;

      inline size_type
      max_size() const;

      /// True if size() == 0.
      _GLIBCXX_NODISCARD inline bool
      empty() const;

      /// Return current hash_fn.
      Hash_Fn&
      get_hash_fn();

      /// Return current const hash_fn.
      const Hash_Fn&
      get_hash_fn() const;

      /// Return current eq_fn.
      Eq_Fn&
      get_eq_fn();

      /// Return current const eq_fn.
      const Eq_Fn&
      get_eq_fn() const;

      /// Return current comb_hash_fn.
      Comb_Hash_Fn&
      get_comb_hash_fn();

      /// Return current const comb_hash_fn.
      const Comb_Hash_Fn&
      get_comb_hash_fn() const;

      /// Return current resize_policy.
      Resize_Policy&
      get_resize_policy();

      /// Return current const resize_policy.
      const Resize_Policy&
      get_resize_policy() const;

      inline std::pair<point_iterator, bool>
      insert(const_reference r_val)
      { return insert_imp(r_val, traits_base::m_store_extra_indicator); }

      inline mapped_reference
      operator[](key_const_reference r_key)
      {
#ifdef PB_DS_DATA_TRUE_INDICATOR
	return (subscript_imp(r_key, traits_base::m_store_extra_indicator));
#else
	insert(r_key);
	return traits_base::s_null_type;
#endif
      }

      inline point_iterator
      find(key_const_reference);

      inline point_const_iterator
      find(key_const_reference) const;

      inline point_iterator
      find_end();

      inline point_const_iterator
      find_end() const;

      inline bool
      erase(key_const_reference);

      template<typename Pred>
      inline size_type
      erase_if(Pred);

      void
      clear();

      inline iterator
      begin();

      inline const_iterator
      begin() const;

      inline iterator
      end();

      inline const_iterator
      end() const;

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;
#endif

#ifdef PB_DS_HT_MAP_TRACE_
      void
      trace() const;
#endif

    private:
      void
      deallocate_all();

      inline bool
      do_resize_if_needed();

      inline void
      do_resize_if_needed_no_throw();

      void
      resize_imp(size_type);

      void
      do_resize(size_type);

      void
      resize_imp_no_exceptions(size_type, entry_pointer_array, size_type);

      inline entry_pointer
      resize_imp_no_exceptions_reassign_pointer(entry_pointer,
						entry_pointer_array,
						false_type);

      inline entry_pointer
      resize_imp_no_exceptions_reassign_pointer(entry_pointer,
						entry_pointer_array,
						true_type);

      void
      deallocate_links_in_list(entry_pointer);

      inline entry_pointer
      get_entry(const_reference, false_type);

      inline entry_pointer
      get_entry(const_reference, true_type);

      inline void
      rels_entry(entry_pointer);

#ifdef PB_DS_DATA_TRUE_INDICATOR
      inline mapped_reference
      subscript_imp(key_const_reference r_key, false_type)
      {
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	const size_type pos = ranged_hash_fn_base::operator()(r_key);
	entry_pointer p_e = m_entries[pos];
	resize_base::notify_insert_search_start();

	while (p_e != 0
	       && !hash_eq_fn_base::operator()(p_e->m_value.first, r_key))
	  {
	    resize_base::notify_insert_search_collision();
	    p_e = p_e->m_p_next;
	  }

	resize_base::notify_insert_search_end();
	if (p_e != 0)
	  {
	    PB_DS_CHECK_KEY_EXISTS(r_key)
	    return (p_e->m_value.second);
	  }

	PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
	return insert_new_imp(value_type(r_key, mapped_type()), pos)->second;
      }

      inline mapped_reference
      subscript_imp(key_const_reference r_key, true_type)
      {
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	comp_hash pos_hash_pair = ranged_hash_fn_base::operator()(r_key);
	entry_pointer p_e = m_entries[pos_hash_pair.first];
	resize_base::notify_insert_search_start();
	while (p_e != 0 &&
	       !hash_eq_fn_base::operator()(p_e->m_value.first, p_e->m_hash,
					    r_key, pos_hash_pair.second))
	  {
	    resize_base::notify_insert_search_collision();
	    p_e = p_e->m_p_next;
	  }

	resize_base::notify_insert_search_end();
	if (p_e != 0)
	  {
	    PB_DS_CHECK_KEY_EXISTS(r_key)
	    return p_e->m_value.second;
	  }

	PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
	return insert_new_imp(value_type(r_key, mapped_type()),
			      pos_hash_pair)->second;
      }
#endif

      inline std::pair<point_iterator, bool>
      insert_imp(const_reference, false_type);

      inline std::pair<point_iterator, bool>
      insert_imp(const_reference, true_type);

      inline pointer
      insert_new_imp(const_reference r_val, size_type pos)
      {
	if (do_resize_if_needed())
	  pos = ranged_hash_fn_base::operator()(PB_DS_V2F(r_val));

	// Following lines might throw an exception.
	entry_pointer p_e = get_entry(r_val,
				      traits_base::m_no_throw_copies_indicator);

	// At this point no exceptions can be thrown.
	p_e->m_p_next = m_entries[pos];
	m_entries[pos] = p_e;
	resize_base::notify_inserted(++m_num_used_e);

	_GLIBCXX_DEBUG_ONLY(debug_base::insert_new(PB_DS_V2F(r_val));)
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	return &p_e->m_value;
      }

      inline pointer
      insert_new_imp(const_reference r_val, comp_hash& r_pos_hash_pair)
      {
	// Following lines might throw an exception.
	if (do_resize_if_needed())
	  r_pos_hash_pair = ranged_hash_fn_base::operator()(PB_DS_V2F(r_val));

	entry_pointer p_e = get_entry(r_val,
				      traits_base::m_no_throw_copies_indicator);

	// At this point no exceptions can be thrown.
	p_e->m_hash = r_pos_hash_pair.second;
	p_e->m_p_next = m_entries[r_pos_hash_pair.first];
	m_entries[r_pos_hash_pair.first] = p_e;
	resize_base::notify_inserted(++m_num_used_e);
	_GLIBCXX_DEBUG_ONLY(debug_base::insert_new(PB_DS_V2F(r_val));)
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	return &p_e->m_value;
      }

      inline pointer
      find_key_pointer(key_const_reference r_key, false_type)
      {
	entry_pointer p_e = m_entries[ranged_hash_fn_base::operator()(r_key)];
	resize_base::notify_find_search_start();
	while (p_e != 0 &&
	       !hash_eq_fn_base::operator()(PB_DS_V2F(p_e->m_value), r_key))
	  {
	    resize_base::notify_find_search_collision();
	    p_e = p_e->m_p_next;
	  }

	resize_base::notify_find_search_end();

	if (p_e == 0)
	  {
	    PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
	    return 0;
	  }
	else
	  {
	    PB_DS_CHECK_KEY_EXISTS(r_key)
	    return &p_e->m_value;
	  }
      }

      inline pointer
      find_key_pointer(key_const_reference r_key, true_type)
      {
	comp_hash pos_hash_pair = ranged_hash_fn_base::operator()(r_key);
	entry_pointer p_e = m_entries[pos_hash_pair.first];
	resize_base::notify_find_search_start();
	while (p_e != 0 &&
	       !hash_eq_fn_base::operator()(PB_DS_V2F(p_e->m_value),
					    p_e->m_hash,
					    r_key, pos_hash_pair.second))
	  {
	    resize_base::notify_find_search_collision();
	    p_e = p_e->m_p_next;
	  }

	resize_base::notify_find_search_end();

	if (p_e == 0)
	  {
	    PB_DS_CHECK_KEY_DOES_NOT_EXIST(r_key)
	    return 0;
	  }
	else
	  {
	    PB_DS_CHECK_KEY_EXISTS(r_key)
	    return &p_e->m_value;
	  }
      }

      inline bool
      erase_in_pos_imp(key_const_reference, size_type);

      inline bool
      erase_in_pos_imp(key_const_reference, const comp_hash&);

      inline void
      erase_entry_pointer(entry_pointer&);

#ifdef PB_DS_DATA_TRUE_INDICATOR
      void
      inc_it_state(pointer& r_p_value,
		   std::pair<entry_pointer, size_type>& r_pos) const
      {
	inc_it_state((mapped_const_pointer& )r_p_value, r_pos);
      }
#endif

      void
      inc_it_state(const_pointer& r_p_value,
		   std::pair<entry_pointer, size_type>& r_pos) const
      {
	_GLIBCXX_DEBUG_ASSERT(r_p_value != 0);
	r_pos.first = r_pos.first->m_p_next;
	if (r_pos.first != 0)
	  {
	    r_p_value = &r_pos.first->m_value;
	    return;
	  }

	for (++r_pos.second; r_pos.second < m_num_e; ++r_pos.second)
	  if (m_entries[r_pos.second] != 0)
	    {
	      r_pos.first = m_entries[r_pos.second];
	      r_p_value = &r_pos.first->m_value;
	      return;
	    }
	r_p_value = 0;
      }

      void
      get_start_it_state(pointer& r_p_value,
			 std::pair<entry_pointer, size_type>& r_pos) const
      {
	for (r_pos.second = 0; r_pos.second < m_num_e; ++r_pos.second)
	  if (m_entries[r_pos.second] != 0)
	    {
	      r_pos.first = m_entries[r_pos.second];
	      r_p_value = &r_pos.first->m_value;
	      return;
	    }
	r_p_value = 0;
      }

#ifdef _GLIBCXX_DEBUG
      void
      assert_entry_pointer_array_valid(const entry_pointer_array,
				       const char*, int) const;

      void
      assert_entry_pointer_valid(const entry_pointer, true_type,
				 const char*, int) const;

      void
      assert_entry_pointer_valid(const entry_pointer, false_type,
				 const char*, int) const;
#endif

#ifdef PB_DS_HT_MAP_TRACE_
      void
      trace_list(const_entry_pointer) const;
#endif

    private:
#ifdef PB_DS_DATA_TRUE_INDICATOR
      friend class iterator_;
#endif

      friend class const_iterator_;

      static entry_allocator 		s_entry_allocator;
      static entry_pointer_allocator 	s_entry_pointer_allocator;
      static iterator 			s_end_it;
      static const_iterator 		s_const_end_it;
      static point_iterator 		s_find_end_it;
      static point_const_iterator 	s_const_find_end_it;

      size_type 			m_num_e;
      size_type 			m_num_used_e;
      entry_pointer_array 		m_entries;

      enum
	{
	  store_hash_ok = !Store_Hash
			  || !is_same<Hash_Fn, __gnu_pbds::null_type>::value
	};

      PB_DS_STATIC_ASSERT(sth, store_hash_ok);
    };

#include <ext/pb_ds/detail/cc_hash_table_map_/constructor_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/entry_list_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/find_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/resize_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/size_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/policy_access_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/iterators_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/cc_hash_table_map_/trace_fn_imps.hpp>

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_HASH_EQ_FN_C_DEC
#undef PB_DS_RANGED_HASH_FN_C_DEC
#undef PB_DS_CC_HASH_TRAITS_BASE
#undef PB_DS_DEBUG_MAP_BASE_C_DEC
#undef PB_DS_CC_HASH_NAME
  } // namespace detail
} // namespace __gnu_pbds
