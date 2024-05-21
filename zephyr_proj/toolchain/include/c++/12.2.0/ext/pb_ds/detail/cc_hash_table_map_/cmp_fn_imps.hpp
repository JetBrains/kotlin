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
 * @file cc_hash_table_map_/cmp_fn_imps.hpp
 * Contains implementations of cc_ht_map_'s entire container comparison related
 *    functions.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
template<typename Other_HT_Map_Type>
bool
PB_DS_CLASS_C_DEC::
operator==(const Other_HT_Map_Type& other) const
{ return cmp_with_other(other); }

PB_DS_CLASS_T_DEC
template<typename Other_Map_Type>
bool
PB_DS_CLASS_C_DEC::
cmp_with_other(const Other_Map_Type& other) const
{
  if (size() != other.size())
    return false;

  for (typename Other_Map_Type::const_iterator it = other.begin();
       it != other.end(); ++it)
    {
      key_const_reference r_key = key_const_reference(PB_DS_V2F(*it));

      mapped_const_pointer p_mapped_value =
	const_cast<PB_DS_CLASS_C_DEC& >(*this).
	find_key_pointer(r_key, traits_base::m_store_extra_indicator);

      if (p_mapped_value == 0)
	return false;

#ifdef PB_DS_DATA_TRUE_INDICATOR
      if (p_mapped_value->second != it->second)
	return false;
#endif
    }
  return true;
}

PB_DS_CLASS_T_DEC
template<typename Other_HT_Map_Type>
bool
PB_DS_CLASS_C_DEC::
operator!=(const Other_HT_Map_Type& other) const
{ return !operator==(other); }
#endif
