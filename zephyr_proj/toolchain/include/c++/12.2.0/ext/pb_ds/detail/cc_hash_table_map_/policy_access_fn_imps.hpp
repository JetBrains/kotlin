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
 * @file cc_hash_table_map_/policy_access_fn_imps.hpp
 * Contains implementations of cc_ht_map_'s policy access
 *    functions.
 */

#ifdef PB_DS_CLASS_C_DEC

PB_DS_CLASS_T_DEC
Hash_Fn& 
PB_DS_CLASS_C_DEC::
get_hash_fn()
{ return *this; }

PB_DS_CLASS_T_DEC
const Hash_Fn& 
PB_DS_CLASS_C_DEC::
get_hash_fn() const
{ return *this; }

PB_DS_CLASS_T_DEC
Eq_Fn& 
PB_DS_CLASS_C_DEC::
get_eq_fn()
{ return *this; }

PB_DS_CLASS_T_DEC
const Eq_Fn& 
PB_DS_CLASS_C_DEC::
get_eq_fn() const
{ return *this; }

PB_DS_CLASS_T_DEC
Comb_Hash_Fn& 
PB_DS_CLASS_C_DEC::
get_comb_hash_fn()
{ return *this; }

PB_DS_CLASS_T_DEC
const Comb_Hash_Fn& 
PB_DS_CLASS_C_DEC::
get_comb_hash_fn() const
{ return *this; }

PB_DS_CLASS_T_DEC
Resize_Policy& 
PB_DS_CLASS_C_DEC::
get_resize_policy()
{ return *this; }

PB_DS_CLASS_T_DEC
const Resize_Policy& 
PB_DS_CLASS_C_DEC::
get_resize_policy() const
{ return *this; }
#endif
