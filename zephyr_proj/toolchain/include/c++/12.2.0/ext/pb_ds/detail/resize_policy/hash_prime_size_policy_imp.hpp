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
 * @file hash_prime_size_policy_imp.hpp
 * Contains a resize size policy implementation.
 */

#ifdef PB_DS_CLASS_C_DEC

#pragma GCC system_header

namespace detail
{
  enum
    {
      num_distinct_sizes_16_bit = 14,
      num_distinct_sizes_32_bit = 30,
      num_distinct_sizes_64_bit = 62,
      // The number of values is limited by the width of size_t.
      // Maybe we could just use (__SIZE_WIDTH__ - 2) here.
#if __SIZE_WIDTH__ >= 64
      num_distinct_sizes =  num_distinct_sizes_64_bit
#elif __SIZE_WIDTH__ >= 32
      num_distinct_sizes =  num_distinct_sizes_32_bit
#else
      num_distinct_sizes =  num_distinct_sizes_16_bit
#endif
    };

  // Originally taken from the SGI implementation; acknowledged in the docs.
  // Further modified (for 64 bits) from tr1's hashtable.
  static const std::size_t g_a_sizes[num_distinct_sizes] =
    {
      /* 0     */              5ul,
      /* 1     */              11ul, 
      /* 2     */              23ul, 
      /* 3     */              47ul, 
      /* 4     */              97ul, 
      /* 5     */              199ul, 
      /* 6     */              409ul, 
      /* 7     */              823ul, 
      /* 8     */              1741ul, 
      /* 9     */              3469ul, 
      /* 10    */              6949ul, 
      /* 11    */              14033ul, 
      /* 12    */              28411ul, 
      /* 13    */              57557ul, 
#if __SIZE_WIDTH__ >= 32
      /* 14    */              116731ul, 
      /* 15    */              236897ul,
      /* 16    */              480881ul, 
      /* 17    */              976369ul,
      /* 18    */              1982627ul, 
      /* 19    */              4026031ul,
      /* 20    */              8175383ul, 
      /* 21    */              16601593ul, 
      /* 22    */              33712729ul,
      /* 23    */              68460391ul, 
      /* 24    */              139022417ul, 
      /* 25    */              282312799ul, 
      /* 26    */              573292817ul, 
      /* 27    */              1164186217ul,
      /* 28    */              2364114217ul, 
      /* 29    */              4294967291ul,
#if __SIZE_WIDTH__ >= 64
      /* 30    */ (std::size_t)8589934583ull,
      /* 31    */ (std::size_t)17179869143ull,
      /* 32    */ (std::size_t)34359738337ull,
      /* 33    */ (std::size_t)68719476731ull,
      /* 34    */ (std::size_t)137438953447ull,
      /* 35    */ (std::size_t)274877906899ull,
      /* 36    */ (std::size_t)549755813881ull,
      /* 37    */ (std::size_t)1099511627689ull,
      /* 38    */ (std::size_t)2199023255531ull,
      /* 39    */ (std::size_t)4398046511093ull,
      /* 40    */ (std::size_t)8796093022151ull,
      /* 41    */ (std::size_t)17592186044399ull,
      /* 42    */ (std::size_t)35184372088777ull,
      /* 43    */ (std::size_t)70368744177643ull,
      /* 44    */ (std::size_t)140737488355213ull,
      /* 45    */ (std::size_t)281474976710597ull,
      /* 46    */ (std::size_t)562949953421231ull, 
      /* 47    */ (std::size_t)1125899906842597ull,
      /* 48    */ (std::size_t)2251799813685119ull, 
      /* 49    */ (std::size_t)4503599627370449ull,
      /* 50    */ (std::size_t)9007199254740881ull, 
      /* 51    */ (std::size_t)18014398509481951ull,
      /* 52    */ (std::size_t)36028797018963913ull, 
      /* 53    */ (std::size_t)72057594037927931ull,
      /* 54    */ (std::size_t)144115188075855859ull,
      /* 55    */ (std::size_t)288230376151711717ull,
      /* 56    */ (std::size_t)576460752303423433ull,
      /* 57    */ (std::size_t)1152921504606846883ull,
      /* 58    */ (std::size_t)2305843009213693951ull,
      /* 59    */ (std::size_t)4611686018427387847ull,
      /* 60    */ (std::size_t)9223372036854775783ull,
      /* 61    */ (std::size_t)18446744073709551557ull,
#endif
#endif
    };

} // namespace detail

PB_DS_CLASS_T_DEC
inline
PB_DS_CLASS_C_DEC::
hash_prime_size_policy(size_type n) : m_start_size(n)
{ m_start_size = get_nearest_larger_size(n); }

PB_DS_CLASS_T_DEC
inline void
PB_DS_CLASS_C_DEC::
swap(PB_DS_CLASS_C_DEC& other)
{ std::swap(m_start_size, other.m_start_size); }

PB_DS_CLASS_T_DEC
inline PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
get_nearest_larger_size(size_type n) const
{
  const std::size_t* const p_upper = std::upper_bound(detail::g_a_sizes, 
		     detail::g_a_sizes + detail::num_distinct_sizes, n);

  if (p_upper == detail::g_a_sizes + detail::num_distinct_sizes)
    __throw_resize_error();
  return *p_upper;
}

PB_DS_CLASS_T_DEC
inline PB_DS_CLASS_C_DEC::size_type
PB_DS_CLASS_C_DEC::
get_nearest_smaller_size(size_type n) const
{
  const std::size_t* p_lower = std::lower_bound(detail::g_a_sizes, 
		       detail::g_a_sizes + detail::num_distinct_sizes, n);

  if (*p_lower >= n &&  p_lower != detail::g_a_sizes)
    --p_lower;
  if (*p_lower < m_start_size)
    return m_start_size;
  return *p_lower;
}
#endif
