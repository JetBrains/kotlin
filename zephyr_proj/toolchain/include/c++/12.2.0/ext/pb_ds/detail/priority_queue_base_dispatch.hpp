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
 * @file detail/priority_queue_base_dispatch.hpp
 * Contains an pqiative container dispatching base.
 */

#ifndef PB_DS_PRIORITY_QUEUE_BASE_DS_DISPATCHER_HPP
#define PB_DS_PRIORITY_QUEUE_BASE_DS_DISPATCHER_HPP

#define PB_DS_ASSERT_VALID(X)						\
  _GLIBCXX_DEBUG_ONLY(X.assert_valid(__FILE__, __LINE__);)

#define PB_DS_DEBUG_VERIFY(_Cond)					\
  _GLIBCXX_DEBUG_VERIFY_AT(_Cond,					\
			   _M_message(#_Cond" assertion from %1;:%2;")	\
			   ._M_string(__FILE__)._M_integer(__LINE__)	\
			   ,__file,__line)

#include <ext/pb_ds/detail/pairing_heap_/pairing_heap_.hpp>
#include <ext/pb_ds/detail/binomial_heap_/binomial_heap_.hpp>
#include <ext/pb_ds/detail/rc_binomial_heap_/rc_binomial_heap_.hpp>
#include <ext/pb_ds/detail/binary_heap_/binary_heap_.hpp>
#include <ext/pb_ds/detail/thin_heap_/thin_heap_.hpp>

#undef PB_DS_DEBUG_VERIFY
#undef PB_DS_ASSERT_VALID

namespace __gnu_pbds
{
  namespace detail
  {
    /**
     *  @ingroup pbds
     *
     *  @{
     */

    /// Specialization for pairing_heap.
    template<typename _VTp, typename Cmp_Fn, typename _Alloc>
      struct container_base_dispatch<_VTp, Cmp_Fn, _Alloc, pairing_heap_tag,
				     null_type>
      {
	/// Dispatched type.
	typedef pairing_heap<_VTp, Cmp_Fn, _Alloc> 		type;
      };

    /// Specialization for binomial_heap.
    template<typename _VTp, typename Cmp_Fn, typename _Alloc>
      struct container_base_dispatch<_VTp, Cmp_Fn, _Alloc, binomial_heap_tag,
				     null_type>
      {
	/// Dispatched type.
	typedef binomial_heap<_VTp, Cmp_Fn, _Alloc> 		type;
      };

    /// Specialization for rc_binary_heap.
    template<typename _VTp, typename Cmp_Fn, typename _Alloc>
      struct container_base_dispatch<_VTp, Cmp_Fn, _Alloc, rc_binomial_heap_tag,
  				     null_type>
      {
	/// Dispatched type.
	typedef rc_binomial_heap<_VTp, Cmp_Fn, _Alloc>	       	type;
      };

    /// Specialization for binary_heap.
    template<typename _VTp, typename Cmp_Fn, typename _Alloc>
      struct container_base_dispatch<_VTp, Cmp_Fn, _Alloc, binary_heap_tag,
  				     null_type>
      {
	/// Dispatched type.
	typedef binary_heap<_VTp, Cmp_Fn, _Alloc> 		type;
      };

    /// Specialization for thin_heap.
    template<typename _VTp, typename Cmp_Fn, typename _Alloc>
      struct container_base_dispatch<_VTp, Cmp_Fn, _Alloc, thin_heap_tag,
  				     null_type>    
      {
	/// Dispatched type.
	typedef thin_heap<_VTp, Cmp_Fn, _Alloc> 		type;
      };
    ///@} group pbds
  } // namespace detail
} // namespace __gnu_pbds

#endif // #ifndef PB_DS_PRIORITY_QUEUE_BASE_DS_DISPATCHER_HPP
