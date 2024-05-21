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
 * @file pat_trie_/synth_access_traits.hpp
 * Contains an implementation class for a patricia tree.
 */

#ifndef PB_DS_SYNTH_E_ACCESS_TRAITS_HPP
#define PB_DS_SYNTH_E_ACCESS_TRAITS_HPP

#include <ext/pb_ds/detail/type_utils.hpp>

namespace __gnu_pbds
{
  namespace detail
  {

#define PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC \
    template<typename Type_Traits, bool Set, typename _ATraits>

#define PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC \
    synth_access_traits<Type_Traits, Set, _ATraits>

    /// Synthetic element access traits.
    template<typename Type_Traits, bool Set, typename _ATraits>
    struct synth_access_traits : public _ATraits
    {
      typedef _ATraits 					base_type;
      typedef typename base_type::const_iterator	const_iterator;
      typedef Type_Traits 				type_traits;
      typedef typename type_traits::const_reference 	const_reference;
      typedef typename type_traits::key_const_reference key_const_reference;

      synth_access_traits();

      synth_access_traits(const base_type&);

      inline bool
      equal_prefixes(const_iterator, const_iterator, const_iterator,
		     const_iterator, bool compare_after = true) const;

      bool
      equal_keys(key_const_reference, key_const_reference) const;

      bool
      cmp_prefixes(const_iterator, const_iterator, const_iterator,
		   const_iterator, bool compare_after = false) const;

      bool
      cmp_keys(key_const_reference, key_const_reference) const;

      inline static key_const_reference
      extract_key(const_reference);

#ifdef _GLIBCXX_DEBUG
      bool
      operator()(key_const_reference, key_const_reference);
#endif

    private:
      inline static key_const_reference
      extract_key(const_reference, true_type);

      inline static key_const_reference
      extract_key(const_reference, false_type);

      static integral_constant<int, Set> s_set_ind;
    };

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    integral_constant<int,Set>
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::s_set_ind;

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    synth_access_traits()
    { }

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    synth_access_traits(const _ATraits& r_traits) 
    : _ATraits(r_traits)
    { }

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    inline bool
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    equal_prefixes(const_iterator b_l, const_iterator e_l, const_iterator b_r,
		   const_iterator e_r, bool compare_after /*= false */) const
    {
      while (b_l != e_l)
	{
	  if (b_r == e_r)
	    return false;
	  if (base_type::e_pos(*b_l) != base_type::e_pos(*b_r))
	    return false;
	  ++b_l;
	  ++b_r;
	}
      return (!compare_after || b_r == e_r);
    }

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    bool
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    equal_keys(key_const_reference r_lhs_key,
	       key_const_reference r_rhs_key) const
    {
      return equal_prefixes(base_type::begin(r_lhs_key),
			    base_type::end(r_lhs_key),
			    base_type::begin(r_rhs_key),
			    base_type::end(r_rhs_key),
			    true);
    }

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    bool
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    cmp_prefixes(const_iterator b_l, const_iterator e_l, const_iterator b_r,
		 const_iterator e_r, bool compare_after /* = false*/) const
    {
      while (b_l != e_l)
	{
	  if (b_r == e_r)
	    return false;

	  const typename base_type::size_type l_pos = base_type::e_pos(*b_l);
	  const typename base_type::size_type r_pos = base_type::e_pos(*b_r);
	  if (l_pos != r_pos)
	    return l_pos < r_pos;
	  ++b_l;
	  ++b_r;
	}

      if (!compare_after)
	return false;
      return b_r != e_r;
    }

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    bool
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    cmp_keys(key_const_reference r_lhs_key,
	     key_const_reference r_rhs_key) const
    {
      return cmp_prefixes(base_type::begin(r_lhs_key),
			  base_type::end(r_lhs_key),
			  base_type::begin(r_rhs_key),
			  base_type::end(r_rhs_key),
			  true);
    }

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    inline typename PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::key_const_reference
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    extract_key(const_reference r_val)
    { return extract_key(r_val, s_set_ind); }

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    inline typename PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::key_const_reference
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    extract_key(const_reference r_val, true_type)
    { return r_val; }

    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    inline typename PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::key_const_reference
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    extract_key(const_reference r_val, false_type)
    { return r_val.first; }

#ifdef _GLIBCXX_DEBUG
    PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
    bool
    PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC::
    operator()(key_const_reference r_lhs, key_const_reference r_rhs)
    { return cmp_keys(r_lhs, r_rhs); }
#endif

#undef PB_DS_SYNTH_E_ACCESS_TRAITS_T_DEC
#undef PB_DS_SYNTH_E_ACCESS_TRAITS_C_DEC

  } // namespace detail
} // namespace __gnu_pbds

#endif
