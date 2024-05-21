// Simd PowerPC specific implementations -*- C++ -*-

// Copyright (C) 2020-2022 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the
// Free Software Foundation; either version 3, or (at your option)
// any later version.

// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_PPC_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_PPC_H_

#if __cplusplus >= 201703L

#ifndef __ALTIVEC__
#error "simd_ppc.h may only be included when AltiVec/VMX is available"
#endif
#include <altivec.h>

_GLIBCXX_SIMD_BEGIN_NAMESPACE

// _SimdImplPpc {{{
template <typename _Abi, typename>
  struct _SimdImplPpc : _SimdImplBuiltin<_Abi>
  {
    using _Base = _SimdImplBuiltin<_Abi>;

    // Byte and halfword shift instructions on PPC only consider the low 3 or 4
    // bits of the RHS. Consequently, shifting by sizeof(_Tp)*CHAR_BIT (or more)
    // is UB without extra measures. To match scalar behavior, byte and halfword
    // shifts need an extra fixup step.

    // _S_bit_shift_left {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_shift_left(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	__x = _Base::_S_bit_shift_left(__x, __y);
	if constexpr (sizeof(_Tp) < sizeof(int))
	  __x._M_data
	    = (__y._M_data < sizeof(_Tp) * __CHAR_BIT__) & __x._M_data;
	return __x;
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_shift_left(_SimdWrapper<_Tp, _Np> __x, int __y)
      {
	__x = _Base::_S_bit_shift_left(__x, __y);
	if constexpr (sizeof(_Tp) < sizeof(int))
	  {
	    if (__y >= sizeof(_Tp) * __CHAR_BIT__)
	      return {};
	  }
	return __x;
      }

    // }}}
    // _S_bit_shift_right {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_shift_right(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	if constexpr (sizeof(_Tp) < sizeof(int))
	  {
	    constexpr int __nbits = sizeof(_Tp) * __CHAR_BIT__;
	    if constexpr (is_unsigned_v<_Tp>)
	      return (__y._M_data < __nbits)
		     & _Base::_S_bit_shift_right(__x, __y)._M_data;
	    else
	      {
		_Base::_S_masked_assign(_SimdWrapper<_Tp, _Np>(__y._M_data
							       >= __nbits),
					__y, __nbits - 1);
		return _Base::_S_bit_shift_right(__x, __y);
	      }
	  }
	else
	  return _Base::_S_bit_shift_right(__x, __y);
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_shift_right(_SimdWrapper<_Tp, _Np> __x, int __y)
      {
	if constexpr (sizeof(_Tp) < sizeof(int))
	  {
	    constexpr int __nbits = sizeof(_Tp) * __CHAR_BIT__;
	    if (__y >= __nbits)
	      {
		if constexpr (is_unsigned_v<_Tp>)
		  return {};
		else
		  return _Base::_S_bit_shift_right(__x, __nbits - 1);
	      }
	  }
	return _Base::_S_bit_shift_right(__x, __y);
      }

    // }}}
  };

// }}}
// _MaskImplPpc {{{
template <typename _Abi, typename>
  struct _MaskImplPpc : _MaskImplBuiltin<_Abi>
  {
    using _Base = _MaskImplBuiltin<_Abi>;

    // _S_popcount {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int _S_popcount(simd_mask<_Tp, _Abi> __k)
      {
	const auto __kv = __as_vector(__k);
	if constexpr (__have_power10vec)
	  {
	    return vec_cntm(__to_intrin(__kv), 1);
	  }
	else if constexpr (sizeof(_Tp) >= sizeof(int))
	  {
	    using _Intrin = __intrinsic_type16_t<int>;
	    const int __sum = -vec_sums(__intrin_bitcast<_Intrin>(__kv), _Intrin())[3];
	    return __sum / (sizeof(_Tp) / sizeof(int));
	  }
	else
	  {
	    const auto __summed_to_int = vec_sum4s(__to_intrin(__kv), __intrinsic_type16_t<int>());
	    return -vec_sums(__summed_to_int, __intrinsic_type16_t<int>())[3];
	  }
      }

    // }}}
  };

// }}}

_GLIBCXX_SIMD_END_NAMESPACE
#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_PPC_H_

// vim: foldmethod=marker foldmarker={{{,}}} sw=2 noet ts=8 sts=2 tw=100
