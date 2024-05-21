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
 * @file list_update_map_/lu_map_.hpp
 * Contains a list update map.
 */

#include <utility>
#include <iterator>
#include <ext/pb_ds/detail/cond_dealtor.hpp>
#include <ext/pb_ds/tag_and_trait.hpp>
#include <ext/pb_ds/detail/types_traits.hpp>
#include <ext/pb_ds/detail/list_update_map_/entry_metadata_base.hpp>
#include <ext/pb_ds/exception.hpp>
#ifdef _GLIBCXX_DEBUG
#include <ext/pb_ds/detail/debug_map_base.hpp>
#endif
#ifdef PB_DS_LU_MAP_TRACE_
#include <iostream>
#endif
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
#ifdef PB_DS_DATA_TRUE_INDICATOR
#define PB_DS_LU_NAME lu_map
#endif

#ifdef PB_DS_DATA_FALSE_INDICATOR
#define PB_DS_LU_NAME lu_set
#endif

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Mapped, typename Eq_Fn, \
	     typename _Alloc, typename Update_Policy>

#define PB_DS_CLASS_C_DEC \
    PB_DS_LU_NAME<Key, Mapped, Eq_Fn, _Alloc, Update_Policy>

#define PB_DS_LU_TRAITS_BASE \
    types_traits<Key, Mapped, _Alloc, false>

#ifdef _GLIBCXX_DEBUG
#define PB_DS_DEBUG_MAP_BASE_C_DEC \
    debug_map_base<Key, Eq_Fn, \
		   typename rebind_traits<_Alloc, Key>::const_reference>
#endif

    /// list-based (with updates) associative container.
    /// Skip to the lu, my darling.
    template<typename Key,
	     typename Mapped,
	     typename Eq_Fn,
	     typename _Alloc,
	     typename Update_Policy>
    class PB_DS_LU_NAME :
#ifdef _GLIBCXX_DEBUG
      protected PB_DS_DEBUG_MAP_BASE_C_DEC,
#endif
      public PB_DS_LU_TRAITS_BASE
    {
    private:
      typedef PB_DS_LU_TRAITS_BASE 	       	traits_base;

      struct entry
     : public lu_map_entry_metadata_base<typename Update_Policy::metadata_type>
      {
	typename traits_base::value_type m_value;
	typename rebind_traits<_Alloc, entry>::pointer m_p_next;
      };

      typedef rebind_traits<_Alloc, entry>		  entry_alloc_traits;
      typedef typename entry_alloc_traits::allocator_type entry_allocator;
      typedef typename entry_alloc_traits::pointer	  entry_pointer;
      typedef typename entry_alloc_traits::const_pointer  const_entry_pointer;
      typedef typename entry_alloc_traits::reference	  entry_reference;
      typedef typename entry_alloc_traits::const_reference const_entry_reference;

      typedef rebind_traits<_Alloc, entry_pointer>	entry_pointer_alloc_traits;
      typedef typename entry_pointer_alloc_traits::allocator_type entry_pointer_allocator;
      typedef typename entry_pointer_alloc_traits::pointer entry_pointer_array;

      typedef typename traits_base::value_type value_type_;
      typedef typename traits_base::pointer pointer_;
      typedef typename traits_base::const_pointer const_pointer_;
      typedef typename traits_base::reference reference_;
      typedef typename traits_base::const_reference const_reference_;

#define PB_DS_GEN_POS entry_pointer

#include <ext/pb_ds/detail/unordered_iterator/point_const_iterator.hpp>
#include <ext/pb_ds/detail/unordered_iterator/point_iterator.hpp>
#include <ext/pb_ds/detail/unordered_iterator/const_iterator.hpp>
#include <ext/pb_ds/detail/unordered_iterator/iterator.hpp>

#undef PB_DS_GEN_POS


#ifdef _GLIBCXX_DEBUG
      typedef PB_DS_DEBUG_MAP_BASE_C_DEC debug_base;
#endif

      typedef cond_dealtor<entry, _Alloc> cond_dealtor_t;

    public:
      typedef _Alloc allocator_type;
      typedef typename _Alloc::size_type size_type;
      typedef typename _Alloc::difference_type difference_type;
      typedef Eq_Fn eq_fn;
      typedef Update_Policy update_policy;
      typedef typename Update_Policy::metadata_type update_metadata;
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
      typedef typename traits_base::value_type value_type;
      typedef typename traits_base::pointer pointer;
      typedef typename traits_base::const_pointer const_pointer;
      typedef typename traits_base::reference reference;
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

    public:
      PB_DS_LU_NAME();

      PB_DS_LU_NAME(const PB_DS_CLASS_C_DEC&);

      virtual
      ~PB_DS_LU_NAME();

      template<typename It>
      PB_DS_LU_NAME(It, It);

      void
      swap(PB_DS_CLASS_C_DEC&);

      inline size_type
      size() const;

      inline size_type
      max_size() const;

      _GLIBCXX_NODISCARD inline bool
      empty() const;

      inline mapped_reference
      operator[](key_const_reference r_key)
      {
#ifdef PB_DS_DATA_TRUE_INDICATOR
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	return insert(std::make_pair(r_key, mapped_type())).first->second;
#else
	insert(r_key);
	return traits_base::s_null_type;
#endif
      }

      inline std::pair<point_iterator, bool>
      insert(const_reference);

      inline point_iterator
      find(key_const_reference r_key)
      {
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	entry_pointer p_e = find_imp(r_key);
	return point_iterator(p_e == 0 ? 0: &p_e->m_value);
      }

      inline point_const_iterator
      find(key_const_reference r_key) const
      {
	_GLIBCXX_DEBUG_ONLY(assert_valid(__FILE__, __LINE__);)
	entry_pointer p_e = find_imp(r_key);
	return point_const_iterator(p_e == 0 ? 0: &p_e->m_value);
      }

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
      assert_valid(const char* file, int line) const;
#endif

#ifdef PB_DS_LU_MAP_TRACE_
      void
      trace() const;
#endif

    protected:

      template<typename It>
      void
      copy_from_range(It, It);

    private:
#ifdef PB_DS_DATA_TRUE_INDICATOR
      friend class iterator_;
#endif

      friend class const_iterator_;

      inline entry_pointer
      allocate_new_entry(const_reference, false_type);

      inline entry_pointer
      allocate_new_entry(const_reference, true_type);

      template<typename Metadata>
      inline static void
      init_entry_metadata(entry_pointer, type_to_type<Metadata>);

      inline static void
      init_entry_metadata(entry_pointer, type_to_type<null_type>);

      void
      deallocate_all();

      void
      erase_next(entry_pointer);

      void
      actual_erase_entry(entry_pointer);

      void
      inc_it_state(const_pointer& r_p_value, entry_pointer& r_pos) const
      {
	r_pos = r_pos->m_p_next;
	r_p_value = (r_pos == 0) ? 0 : &r_pos->m_value;
      }

      template<typename Metadata>
      inline static bool
      apply_update(entry_pointer, type_to_type<Metadata>);

      inline static bool
      apply_update(entry_pointer, type_to_type<null_type>);

      inline entry_pointer
      find_imp(key_const_reference) const;

      static entry_allocator 			s_entry_allocator;
      static Eq_Fn 				s_eq_fn;
      static Update_Policy 			s_update_policy;
      static type_to_type<update_metadata> 	s_metadata_type_indicator;
      static null_type 				s_null_type;

      mutable entry_pointer 			m_p_l;
    };

#include <ext/pb_ds/detail/list_update_map_/constructor_destructor_fn_imps.hpp>
#include <ext/pb_ds/detail/list_update_map_/info_fn_imps.hpp>
#include <ext/pb_ds/detail/list_update_map_/debug_fn_imps.hpp>
#include <ext/pb_ds/detail/list_update_map_/iterators_fn_imps.hpp>
#include <ext/pb_ds/detail/list_update_map_/erase_fn_imps.hpp>
#include <ext/pb_ds/detail/list_update_map_/find_fn_imps.hpp>
#include <ext/pb_ds/detail/list_update_map_/insert_fn_imps.hpp>
#include <ext/pb_ds/detail/list_update_map_/trace_fn_imps.hpp>

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC
#undef PB_DS_LU_TRAITS_BASE
#undef PB_DS_DEBUG_MAP_BASE_C_DEC
#undef PB_DS_LU_NAME
  } // namespace detail
} // namespace __gnu_pbds
