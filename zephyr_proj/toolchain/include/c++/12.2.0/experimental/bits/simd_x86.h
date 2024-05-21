// Simd x86 specific implementations -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_X86_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_X86_H_

#if __cplusplus >= 201703L

#if !_GLIBCXX_SIMD_X86INTRIN
#error                                                                         \
  "simd_x86.h may only be included when MMX or SSE on x86(_64) are available"
#endif

_GLIBCXX_SIMD_BEGIN_NAMESPACE

// __to_masktype {{{
// Given <T, N> return <__int_for_sizeof_t<T>, N>. For _SimdWrapper and
// __vector_type_t.
template <typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper<__int_for_sizeof_t<_Tp>, _Np>
  __to_masktype(_SimdWrapper<_Tp, _Np> __x)
  {
    return reinterpret_cast<__vector_type_t<__int_for_sizeof_t<_Tp>, _Np>>(
      __x._M_data);
  }

template <typename _TV,
	  typename _TVT
	  = enable_if_t<__is_vector_type_v<_TV>, _VectorTraits<_TV>>,
	  typename _Up = __int_for_sizeof_t<typename _TVT::value_type>>
  _GLIBCXX_SIMD_INTRINSIC constexpr __vector_type_t<_Up, _TVT::_S_full_size>
  __to_masktype(_TV __x)
  { return reinterpret_cast<__vector_type_t<_Up, _TVT::_S_full_size>>(__x); }

// }}}
// __interleave128_lo {{{
template <typename _Ap, typename _Bp, typename _Tp = common_type_t<_Ap, _Bp>,
	  typename _Trait = _VectorTraits<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC constexpr _Tp
  __interleave128_lo(const _Ap& __av, const _Bp& __bv)
  {
    const _Tp __a(__av);
    const _Tp __b(__bv);
    if constexpr (sizeof(_Tp) == 16 && _Trait::_S_full_size == 2)
      return _Tp{__a[0], __b[0]};
    else if constexpr (sizeof(_Tp) == 16 && _Trait::_S_full_size == 4)
      return _Tp{__a[0], __b[0], __a[1], __b[1]};
    else if constexpr (sizeof(_Tp) == 16 && _Trait::_S_full_size == 8)
      return _Tp{__a[0], __b[0], __a[1], __b[1],
		 __a[2], __b[2], __a[3], __b[3]};
    else if constexpr (sizeof(_Tp) == 16 && _Trait::_S_full_size == 16)
      return _Tp{__a[0], __b[0], __a[1], __b[1], __a[2], __b[2],
		 __a[3], __b[3], __a[4], __b[4], __a[5], __b[5],
		 __a[6], __b[6], __a[7], __b[7]};
    else if constexpr (sizeof(_Tp) == 32 && _Trait::_S_full_size == 4)
      return _Tp{__a[0], __b[0], __a[2], __b[2]};
    else if constexpr (sizeof(_Tp) == 32 && _Trait::_S_full_size == 8)
      return _Tp{__a[0], __b[0], __a[1], __b[1],
		 __a[4], __b[4], __a[5], __b[5]};
    else if constexpr (sizeof(_Tp) == 32 && _Trait::_S_full_size == 16)
      return _Tp{__a[0],  __b[0],  __a[1],  __b[1], __a[2], __b[2],
		 __a[3],  __b[3],  __a[8],  __b[8], __a[9], __b[9],
		 __a[10], __b[10], __a[11], __b[11]};
    else if constexpr (sizeof(_Tp) == 32 && _Trait::_S_full_size == 32)
      return _Tp{__a[0],  __b[0],  __a[1],  __b[1],  __a[2],  __b[2],  __a[3],
		 __b[3],  __a[4],  __b[4],  __a[5],  __b[5],  __a[6],  __b[6],
		 __a[7],  __b[7],  __a[16], __b[16], __a[17], __b[17], __a[18],
		 __b[18], __a[19], __b[19], __a[20], __b[20], __a[21], __b[21],
		 __a[22], __b[22], __a[23], __b[23]};
    else if constexpr (sizeof(_Tp) == 64 && _Trait::_S_full_size == 8)
      return _Tp{__a[0], __b[0], __a[2], __b[2],
		 __a[4], __b[4], __a[6], __b[6]};
    else if constexpr (sizeof(_Tp) == 64 && _Trait::_S_full_size == 16)
      return _Tp{__a[0],  __b[0],  __a[1],  __b[1], __a[4], __b[4],
		 __a[5],  __b[5],  __a[8],  __b[8], __a[9], __b[9],
		 __a[12], __b[12], __a[13], __b[13]};
    else if constexpr (sizeof(_Tp) == 64 && _Trait::_S_full_size == 32)
      return _Tp{__a[0],  __b[0],  __a[1],  __b[1],  __a[2],  __b[2],  __a[3],
		 __b[3],  __a[8],  __b[8],  __a[9],  __b[9],  __a[10], __b[10],
		 __a[11], __b[11], __a[16], __b[16], __a[17], __b[17], __a[18],
		 __b[18], __a[19], __b[19], __a[24], __b[24], __a[25], __b[25],
		 __a[26], __b[26], __a[27], __b[27]};
    else if constexpr (sizeof(_Tp) == 64 && _Trait::_S_full_size == 64)
      return _Tp{__a[0],  __b[0],  __a[1],  __b[1],  __a[2],  __b[2],  __a[3],
		 __b[3],  __a[4],  __b[4],  __a[5],  __b[5],  __a[6],  __b[6],
		 __a[7],  __b[7],  __a[16], __b[16], __a[17], __b[17], __a[18],
		 __b[18], __a[19], __b[19], __a[20], __b[20], __a[21], __b[21],
		 __a[22], __b[22], __a[23], __b[23], __a[32], __b[32], __a[33],
		 __b[33], __a[34], __b[34], __a[35], __b[35], __a[36], __b[36],
		 __a[37], __b[37], __a[38], __b[38], __a[39], __b[39], __a[48],
		 __b[48], __a[49], __b[49], __a[50], __b[50], __a[51], __b[51],
		 __a[52], __b[52], __a[53], __b[53], __a[54], __b[54], __a[55],
		 __b[55]};
    else
      __assert_unreachable<_Tp>();
  }

// }}}
// __is_zero{{{
template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC constexpr bool
  __is_zero(_Tp __a)
  {
    if (!__builtin_is_constant_evaluated())
      {
	if constexpr (__have_avx)
	  {
	    if constexpr (_TVT::template _S_is<float, 8>)
	      return _mm256_testz_ps(__a, __a);
	    else if constexpr (_TVT::template _S_is<double, 4>)
	      return _mm256_testz_pd(__a, __a);
	    else if constexpr (sizeof(_Tp) == 32)
	      return _mm256_testz_si256(__to_intrin(__a), __to_intrin(__a));
	    else if constexpr (_TVT::template _S_is<float>)
	      return _mm_testz_ps(__to_intrin(__a), __to_intrin(__a));
	    else if constexpr (_TVT::template _S_is<double, 2>)
	      return _mm_testz_pd(__a, __a);
	    else
	      return _mm_testz_si128(__to_intrin(__a), __to_intrin(__a));
	  }
	else if constexpr (__have_sse4_1)
	  return _mm_testz_si128(__intrin_bitcast<__m128i>(__a),
				 __intrin_bitcast<__m128i>(__a));
      }
    else if constexpr (sizeof(_Tp) <= 8)
      return reinterpret_cast<__int_for_sizeof_t<_Tp>>(__a) == 0;
    else
      {
	const auto __b = __vector_bitcast<_LLong>(__a);
	if constexpr (sizeof(__b) == 16)
	  return (__b[0] | __b[1]) == 0;
	else if constexpr (sizeof(__b) == 32)
	  return __is_zero(__lo128(__b) | __hi128(__b));
	else if constexpr (sizeof(__b) == 64)
	  return __is_zero(__lo256(__b) | __hi256(__b));
	else
	  __assert_unreachable<_Tp>();
      }
  }

// }}}
// __movemask{{{
template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_CONST int
  __movemask(_Tp __a)
  {
    if constexpr (sizeof(_Tp) == 32)
      {
	if constexpr (_TVT::template _S_is<float>)
	  return _mm256_movemask_ps(__to_intrin(__a));
	else if constexpr (_TVT::template _S_is<double>)
	  return _mm256_movemask_pd(__to_intrin(__a));
	else
	  return _mm256_movemask_epi8(__to_intrin(__a));
      }
    else if constexpr (_TVT::template _S_is<float>)
      return _mm_movemask_ps(__to_intrin(__a));
    else if constexpr (_TVT::template _S_is<double>)
      return _mm_movemask_pd(__to_intrin(__a));
    else
      return _mm_movemask_epi8(__to_intrin(__a));
  }

// }}}
// __testz{{{
template <typename _TI, typename _TVT = _VectorTraits<_TI>>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_CONST constexpr int
  __testz(_TI __a, _TI __b)
  {
    static_assert(is_same_v<_TI, __intrinsic_type_t<typename _TVT::value_type,
						    _TVT::_S_full_size>>);
    if (!__builtin_is_constant_evaluated())
      {
	if constexpr (sizeof(_TI) == 32)
	  {
	    if constexpr (_TVT::template _S_is<float>)
	      return _mm256_testz_ps(__to_intrin(__a), __to_intrin(__b));
	    else if constexpr (_TVT::template _S_is<double>)
	      return _mm256_testz_pd(__to_intrin(__a), __to_intrin(__b));
	    else
	      return _mm256_testz_si256(__to_intrin(__a), __to_intrin(__b));
	  }
	else if constexpr (_TVT::template _S_is<float> && __have_avx)
	  return _mm_testz_ps(__to_intrin(__a), __to_intrin(__b));
	else if constexpr (_TVT::template _S_is<double> && __have_avx)
	  return _mm_testz_pd(__to_intrin(__a), __to_intrin(__b));
	else if constexpr (__have_sse4_1)
	  return _mm_testz_si128(__intrin_bitcast<__m128i>(__to_intrin(__a)),
				 __intrin_bitcast<__m128i>(__to_intrin(__b)));
	else
	  return __movemask(0 == __and(__a, __b)) != 0;
      }
    else
      return __is_zero(__and(__a, __b));
  }

// }}}
// __testc{{{
// requires SSE4.1 or above
template <typename _TI, typename _TVT = _VectorTraits<_TI>>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_CONST constexpr int
  __testc(_TI __a, _TI __b)
  {
    static_assert(is_same_v<_TI, __intrinsic_type_t<typename _TVT::value_type,
						    _TVT::_S_full_size>>);
    if (__builtin_is_constant_evaluated())
      return __is_zero(__andnot(__a, __b));

    if constexpr (sizeof(_TI) == 32)
      {
	if constexpr (_TVT::template _S_is<float>)
	  return _mm256_testc_ps(__a, __b);
	else if constexpr (_TVT::template _S_is<double>)
	  return _mm256_testc_pd(__a, __b);
	else
	  return _mm256_testc_si256(__to_intrin(__a), __to_intrin(__b));
      }
    else if constexpr (_TVT::template _S_is<float> && __have_avx)
      return _mm_testc_ps(__to_intrin(__a), __to_intrin(__b));
    else if constexpr (_TVT::template _S_is<double> && __have_avx)
      return _mm_testc_pd(__to_intrin(__a), __to_intrin(__b));
    else
      {
	static_assert(is_same_v<_TI, _TI> && __have_sse4_1);
	return _mm_testc_si128(__intrin_bitcast<__m128i>(__to_intrin(__a)),
			       __intrin_bitcast<__m128i>(__to_intrin(__b)));
      }
  }

// }}}
// __testnzc{{{
template <typename _TI, typename _TVT = _VectorTraits<_TI>>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_CONST constexpr int
  __testnzc(_TI __a, _TI __b)
  {
    static_assert(is_same_v<_TI, __intrinsic_type_t<typename _TVT::value_type,
						    _TVT::_S_full_size>>);
    if (!__builtin_is_constant_evaluated())
      {
	if constexpr (sizeof(_TI) == 32)
	  {
	    if constexpr (_TVT::template _S_is<float>)
	      return _mm256_testnzc_ps(__a, __b);
	    else if constexpr (_TVT::template _S_is<double>)
	      return _mm256_testnzc_pd(__a, __b);
	    else
	      return _mm256_testnzc_si256(__to_intrin(__a), __to_intrin(__b));
	  }
	else if constexpr (_TVT::template _S_is<float> && __have_avx)
	  return _mm_testnzc_ps(__to_intrin(__a), __to_intrin(__b));
	else if constexpr (_TVT::template _S_is<double> && __have_avx)
	  return _mm_testnzc_pd(__to_intrin(__a), __to_intrin(__b));
	else if constexpr (__have_sse4_1)
	  return _mm_testnzc_si128(__intrin_bitcast<__m128i>(__to_intrin(__a)),
				   __intrin_bitcast<__m128i>(__to_intrin(__b)));
	else
	  return __movemask(0 == __and(__a, __b)) == 0
		 && __movemask(0 == __andnot(__a, __b)) == 0;
      }
    else
      return !(__is_zero(__and(__a, __b)) || __is_zero(__andnot(__a, __b)));
  }

// }}}
// __xzyw{{{
// shuffles the complete vector, swapping the inner two quarters. Often useful
// for AVX for fixing up a shuffle result.
template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC _Tp
  __xzyw(_Tp __a)
  {
    if constexpr (sizeof(_Tp) == 16)
      {
	const auto __x = __vector_bitcast<conditional_t<
	  is_floating_point_v<typename _TVT::value_type>, float, int>>(__a);
	return reinterpret_cast<_Tp>(
	  decltype(__x){__x[0], __x[2], __x[1], __x[3]});
      }
    else if constexpr (sizeof(_Tp) == 32)
      {
	const auto __x = __vector_bitcast<conditional_t<
	  is_floating_point_v<typename _TVT::value_type>, double, _LLong>>(__a);
	return reinterpret_cast<_Tp>(
	  decltype(__x){__x[0], __x[2], __x[1], __x[3]});
      }
    else if constexpr (sizeof(_Tp) == 64)
      {
	const auto __x = __vector_bitcast<conditional_t<
	  is_floating_point_v<typename _TVT::value_type>, double, _LLong>>(__a);
	return reinterpret_cast<_Tp>(decltype(__x){__x[0], __x[1], __x[4],
						   __x[5], __x[2], __x[3],
						   __x[6], __x[7]});
      }
    else
      __assert_unreachable<_Tp>();
  }

// }}}
// __maskload_epi32{{{
template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC auto
  __maskload_epi32(const int* __ptr, _Tp __k)
  {
    if constexpr (sizeof(__k) == 16)
      return _mm_maskload_epi32(__ptr, __k);
    else
      return _mm256_maskload_epi32(__ptr, __k);
  }

// }}}
// __maskload_epi64{{{
template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC auto
  __maskload_epi64(const _LLong* __ptr, _Tp __k)
  {
    if constexpr (sizeof(__k) == 16)
      return _mm_maskload_epi64(__ptr, __k);
    else
      return _mm256_maskload_epi64(__ptr, __k);
  }

// }}}
// __maskload_ps{{{
template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC auto
  __maskload_ps(const float* __ptr, _Tp __k)
  {
    if constexpr (sizeof(__k) == 16)
      return _mm_maskload_ps(__ptr, __k);
    else
      return _mm256_maskload_ps(__ptr, __k);
  }

// }}}
// __maskload_pd{{{
template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC auto
  __maskload_pd(const double* __ptr, _Tp __k)
  {
    if constexpr (sizeof(__k) == 16)
      return _mm_maskload_pd(__ptr, __k);
    else
      return _mm256_maskload_pd(__ptr, __k);
  }

// }}}

#ifdef _GLIBCXX_SIMD_WORKAROUND_PR85048
#include "simd_x86_conversions.h"
#endif

// ISA & type detection {{{
template <typename _Tp, size_t _Np>
  constexpr bool
  __is_sse_ps()
  {
    return __have_sse
	   && is_same_v<_Tp,
			float> && sizeof(__intrinsic_type_t<_Tp, _Np>) == 16;
  }

template <typename _Tp, size_t _Np>
  constexpr bool
  __is_sse_pd()
  {
    return __have_sse2
	   && is_same_v<_Tp,
			double> && sizeof(__intrinsic_type_t<_Tp, _Np>) == 16;
  }

template <typename _Tp, size_t _Np>
  constexpr bool
  __is_avx_ps()
  {
    return __have_avx
	   && is_same_v<_Tp,
			float> && sizeof(__intrinsic_type_t<_Tp, _Np>) == 32;
  }

template <typename _Tp, size_t _Np>
  constexpr bool
  __is_avx_pd()
  {
    return __have_avx
	   && is_same_v<_Tp,
			double> && sizeof(__intrinsic_type_t<_Tp, _Np>) == 32;
  }

template <typename _Tp, size_t _Np>
  constexpr bool
  __is_avx512_ps()
  {
    return __have_avx512f
	   && is_same_v<_Tp,
			float> && sizeof(__intrinsic_type_t<_Tp, _Np>) == 64;
  }

template <typename _Tp, size_t _Np>
  constexpr bool
  __is_avx512_pd()
  {
    return __have_avx512f
	   && is_same_v<_Tp,
			double> && sizeof(__intrinsic_type_t<_Tp, _Np>) == 64;
  }

// }}}
struct _MaskImplX86Mixin;

// _CommonImplX86 {{{
struct _CommonImplX86 : _CommonImplBuiltin
{
#ifdef _GLIBCXX_SIMD_WORKAROUND_PR85048
  // _S_converts_via_decomposition {{{
  template <typename _From, typename _To, size_t _ToSize>
    static constexpr bool _S_converts_via_decomposition()
    {
      if constexpr (is_integral_v<
		      _From> && is_integral_v<_To> && sizeof(_From) == 8
		    && _ToSize == 16)
	return (sizeof(_To) == 2 && !__have_ssse3)
	       || (sizeof(_To) == 1 && !__have_avx512f);
      else if constexpr (is_floating_point_v<_From> && is_integral_v<_To>)
	return ((sizeof(_From) == 4 || sizeof(_From) == 8) && sizeof(_To) == 8
		&& !__have_avx512dq)
	       || (sizeof(_From) == 8 && sizeof(_To) == 4 && !__have_sse4_1
		   && _ToSize == 16);
      else if constexpr (
	is_integral_v<_From> && is_floating_point_v<_To> && sizeof(_From) == 8
	&& !__have_avx512dq)
	return (sizeof(_To) == 4 && _ToSize == 16)
	       || (sizeof(_To) == 8 && _ToSize < 64);
      else
	return false;
    }

  template <typename _From, typename _To, size_t _ToSize>
    static inline constexpr bool __converts_via_decomposition_v
      = _S_converts_via_decomposition<_From, _To, _ToSize>();

  // }}}
#endif
  // _S_store {{{
  using _CommonImplBuiltin::_S_store;

  template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static void _S_store(_SimdWrapper<_Tp, _Np> __x,
						 void* __addr)
    {
      constexpr size_t _Bytes = _Np * sizeof(_Tp);

      if constexpr ((_Bytes & (_Bytes - 1)) != 0 && __have_avx512bw_vl)
	{
	  const auto __v = __to_intrin(__x);

	  if constexpr (_Bytes & 1)
	    {
	      if constexpr (_Bytes < 16)
		_mm_mask_storeu_epi8(__addr, 0xffffu >> (16 - _Bytes),
				     __intrin_bitcast<__m128i>(__v));
	      else if constexpr (_Bytes < 32)
		_mm256_mask_storeu_epi8(__addr, 0xffffffffu >> (32 - _Bytes),
					__intrin_bitcast<__m256i>(__v));
	      else
		_mm512_mask_storeu_epi8(__addr,
					0xffffffffffffffffull >> (64 - _Bytes),
					__intrin_bitcast<__m512i>(__v));
	    }
	  else if constexpr (_Bytes & 2)
	    {
	      if constexpr (_Bytes < 16)
		_mm_mask_storeu_epi16(__addr, 0xffu >> (8 - _Bytes / 2),
				      __intrin_bitcast<__m128i>(__v));
	      else if constexpr (_Bytes < 32)
		_mm256_mask_storeu_epi16(__addr, 0xffffu >> (16 - _Bytes / 2),
					 __intrin_bitcast<__m256i>(__v));
	      else
		_mm512_mask_storeu_epi16(__addr,
					 0xffffffffull >> (32 - _Bytes / 2),
					 __intrin_bitcast<__m512i>(__v));
	    }
	  else if constexpr (_Bytes & 4)
	    {
	      if constexpr (_Bytes < 16)
		_mm_mask_storeu_epi32(__addr, 0xfu >> (4 - _Bytes / 4),
				      __intrin_bitcast<__m128i>(__v));
	      else if constexpr (_Bytes < 32)
		_mm256_mask_storeu_epi32(__addr, 0xffu >> (8 - _Bytes / 4),
					 __intrin_bitcast<__m256i>(__v));
	      else
		_mm512_mask_storeu_epi32(__addr, 0xffffull >> (16 - _Bytes / 4),
					 __intrin_bitcast<__m512i>(__v));
	    }
	  else
	    {
	      static_assert(
		_Bytes > 16,
		"_Bytes < 16 && (_Bytes & 7) == 0 && (_Bytes & (_Bytes "
		"- 1)) != 0 is impossible");
	      if constexpr (_Bytes < 32)
		_mm256_mask_storeu_epi64(__addr, 0xfu >> (4 - _Bytes / 8),
					 __intrin_bitcast<__m256i>(__v));
	      else
		_mm512_mask_storeu_epi64(__addr, 0xffull >> (8 - _Bytes / 8),
					 __intrin_bitcast<__m512i>(__v));
	    }
	}
      else
	_CommonImplBuiltin::_S_store(__x, __addr);
    }

  // }}}
  // _S_store_bool_array(_BitMask) {{{
  template <size_t _Np, bool _Sanitized>
    _GLIBCXX_SIMD_INTRINSIC static constexpr void
    _S_store_bool_array(const _BitMask<_Np, _Sanitized> __x, bool* __mem)
    {
      if constexpr (__have_avx512bw_vl) // don't care for BW w/o VL
	_S_store<_Np>(1 & __vector_bitcast<_UChar, _Np>([=]() constexpr {
			if constexpr (_Np <= 16)
			  return _mm_movm_epi8(__x._M_to_bits());
			else if constexpr (_Np <= 32)
			  return _mm256_movm_epi8(__x._M_to_bits());
			else if constexpr (_Np <= 64)
			  return _mm512_movm_epi8(__x._M_to_bits());
			else
			  __assert_unreachable<_SizeConstant<_Np>>();
		      }()),
		      __mem);
      else if constexpr (__have_bmi2)
	{
	  if constexpr (_Np <= 4)
	    _S_store<_Np>(_pdep_u32(__x._M_to_bits(), 0x01010101U), __mem);
	  else
	    __execute_n_times<__div_roundup(_Np, sizeof(size_t))>(
	      [&](auto __i) {
		constexpr size_t __offset = __i * sizeof(size_t);
		constexpr int __todo = std::min(sizeof(size_t), _Np - __offset);
		if constexpr (__todo == 1)
		  __mem[__offset] = __x[__offset];
		else
		  {
		    const auto __bools =
#ifdef __x86_64__
		      _pdep_u64(__x.template _M_extract<__offset>().to_ullong(),
				0x0101010101010101ULL);
#else // __x86_64__
		      _pdep_u32(
			__x.template _M_extract<__offset>()._M_to_bits(),
			0x01010101U);
#endif // __x86_64__
		    _S_store<__todo>(__bools, __mem + __offset);
		  }
	      });
	}
      else if constexpr (__have_sse2 && _Np > 7)
	__execute_n_times<__div_roundup(_Np, 16)>([&](auto __i) {
	  constexpr int __offset = __i * 16;
	  constexpr int __todo = std::min(16, int(_Np) - __offset);
	  const int __bits = __x.template _M_extract<__offset>()._M_to_bits();
	  __vector_type16_t<_UChar> __bools;
	  if constexpr (__have_avx512f)
	    {
	      auto __as32bits
		= _mm512_maskz_mov_epi32(__bits, __to_intrin(
						   __vector_broadcast<16>(1)));
	      auto __as16bits
		= __xzyw(_mm256_packs_epi32(__lo256(__as32bits),
					    __todo > 8 ? __hi256(__as32bits)
						       : __m256i()));
	      __bools = __vector_bitcast<_UChar>(
		_mm_packs_epi16(__lo128(__as16bits), __hi128(__as16bits)));
	    }
	  else
	    {
	      using _V = __vector_type_t<_UChar, 16>;
	      auto __tmp = _mm_cvtsi32_si128(__bits);
	      __tmp = _mm_unpacklo_epi8(__tmp, __tmp);
	      __tmp = _mm_unpacklo_epi16(__tmp, __tmp);
	      __tmp = _mm_unpacklo_epi32(__tmp, __tmp);
	      _V __tmp2 = reinterpret_cast<_V>(__tmp);
	      __tmp2 &= _V{1, 2, 4, 8, 16, 32, 64, 128,
			   1, 2, 4, 8, 16, 32, 64, 128}; // mask bit index
	      __bools = (__tmp2 == 0) + 1; // 0xff -> 0x00 | 0x00 -> 0x01
	    }
	  _S_store<__todo>(__bools, __mem + __offset);
	});
      else
	_CommonImplBuiltin::_S_store_bool_array(__x, __mem);
    }

  // }}}
  // _S_blend_avx512 {{{
  // Returns: __k ? __b : __a
  // TODO: reverse __a and __b to match COND_EXPR
  // Requires: _TV to be a __vector_type_t matching valuetype for the bitmask
  //           __k
  template <typename _Kp, typename _TV>
    _GLIBCXX_SIMD_INTRINSIC static _TV
    _S_blend_avx512(const _Kp __k, const _TV __a, const _TV __b) noexcept
    {
#ifdef __clang__
      // FIXME: this does a boolean choice, not a blend
      return __k ? __a : __b;
#else
      static_assert(__is_vector_type_v<_TV>);
      using _Tp = typename _VectorTraits<_TV>::value_type;
      static_assert(sizeof(_TV) >= 16);
      static_assert(sizeof(_Tp) <= 8);
      using _IntT
	= conditional_t<(sizeof(_Tp) > 2),
			conditional_t<sizeof(_Tp) == 4, int, long long>,
			conditional_t<sizeof(_Tp) == 1, char, short>>;
      [[maybe_unused]] const auto __aa = __vector_bitcast<_IntT>(__a);
      [[maybe_unused]] const auto __bb = __vector_bitcast<_IntT>(__b);
      if constexpr (sizeof(_TV) == 64)
	{
	  if constexpr (sizeof(_Tp) == 1)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmb_512_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 2)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmw_512_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 4 && is_floating_point_v<_Tp>)
	    return __builtin_ia32_blendmps_512_mask(__a, __b, __k);
	  else if constexpr (sizeof(_Tp) == 4)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmd_512_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 8 && is_floating_point_v<_Tp>)
	    return __builtin_ia32_blendmpd_512_mask(__a, __b, __k);
	  else if constexpr (sizeof(_Tp) == 8)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmq_512_mask(__aa, __bb, __k));
	}
      else if constexpr (sizeof(_TV) == 32)
	{
	  if constexpr (sizeof(_Tp) == 1)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmb_256_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 2)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmw_256_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 4 && is_floating_point_v<_Tp>)
	    return __builtin_ia32_blendmps_256_mask(__a, __b, __k);
	  else if constexpr (sizeof(_Tp) == 4)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmd_256_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 8 && is_floating_point_v<_Tp>)
	    return __builtin_ia32_blendmpd_256_mask(__a, __b, __k);
	  else if constexpr (sizeof(_Tp) == 8)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmq_256_mask(__aa, __bb, __k));
	}
      else if constexpr (sizeof(_TV) == 16)
	{
	  if constexpr (sizeof(_Tp) == 1)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmb_128_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 2)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmw_128_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 4 && is_floating_point_v<_Tp>)
	    return __builtin_ia32_blendmps_128_mask(__a, __b, __k);
	  else if constexpr (sizeof(_Tp) == 4)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmd_128_mask(__aa, __bb, __k));
	  else if constexpr (sizeof(_Tp) == 8 && is_floating_point_v<_Tp>)
	    return __builtin_ia32_blendmpd_128_mask(__a, __b, __k);
	  else if constexpr (sizeof(_Tp) == 8)
	    return reinterpret_cast<_TV>(
	      __builtin_ia32_blendmq_128_mask(__aa, __bb, __k));
	}
#endif
    }

  // }}}
  // _S_blend_intrin {{{
  // Returns: __k ? __b : __a
  // TODO: reverse __a and __b to match COND_EXPR
  // Requires: _Tp to be an intrinsic type (integers blend per byte) and 16/32
  //           Bytes wide
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_blend_intrin(_Tp __k, _Tp __a,
						       _Tp __b) noexcept
    {
      static_assert(is_same_v<decltype(__to_intrin(__a)), _Tp>);
      constexpr struct
      {
	_GLIBCXX_SIMD_INTRINSIC __m128 operator()(__m128 __a, __m128 __b,
						  __m128 __k) const noexcept
	{
	  return __builtin_ia32_blendvps(__a, __b, __k);
	}
	_GLIBCXX_SIMD_INTRINSIC __m128d operator()(__m128d __a, __m128d __b,
						   __m128d __k) const noexcept
	{
	  return __builtin_ia32_blendvpd(__a, __b, __k);
	}
	_GLIBCXX_SIMD_INTRINSIC __m128i operator()(__m128i __a, __m128i __b,
						   __m128i __k) const noexcept
	{
	  return reinterpret_cast<__m128i>(
	    __builtin_ia32_pblendvb128(reinterpret_cast<__v16qi>(__a),
				       reinterpret_cast<__v16qi>(__b),
				       reinterpret_cast<__v16qi>(__k)));
	}
	_GLIBCXX_SIMD_INTRINSIC __m256 operator()(__m256 __a, __m256 __b,
						  __m256 __k) const noexcept
	{
	  return __builtin_ia32_blendvps256(__a, __b, __k);
	}
	_GLIBCXX_SIMD_INTRINSIC __m256d operator()(__m256d __a, __m256d __b,
						   __m256d __k) const noexcept
	{
	  return __builtin_ia32_blendvpd256(__a, __b, __k);
	}
	_GLIBCXX_SIMD_INTRINSIC __m256i operator()(__m256i __a, __m256i __b,
						   __m256i __k) const noexcept
	{
	  if constexpr (__have_avx2)
	    return reinterpret_cast<__m256i>(
	      __builtin_ia32_pblendvb256(reinterpret_cast<__v32qi>(__a),
					 reinterpret_cast<__v32qi>(__b),
					 reinterpret_cast<__v32qi>(__k)));
	  else
	    return reinterpret_cast<__m256i>(
	      __builtin_ia32_blendvps256(reinterpret_cast<__v8sf>(__a),
					 reinterpret_cast<__v8sf>(__b),
					 reinterpret_cast<__v8sf>(__k)));
	}
      } __eval;
      return __eval(__a, __b, __k);
    }

  // }}}
  // _S_blend {{{
  // Returns: __k ? __at1 : __at0
  // TODO: reverse __at0 and __at1 to match COND_EXPR
  template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
    _S_blend(_SimdWrapper<bool, _Np> __k, _SimdWrapper<_Tp, _Np> __at0,
	     _SimdWrapper<_Tp, _Np> __at1)
    {
      static_assert(is_same_v<_Tp, _Tp> && __have_avx512f);
      if (__k._M_is_constprop() && __at0._M_is_constprop()
	  && __at1._M_is_constprop())
	return __generate_from_n_evaluations<_Np,
					     __vector_type_t<_Tp, _Np>>([&](
	  auto __i) constexpr { return __k[__i] ? __at1[__i] : __at0[__i]; });
      else if constexpr (sizeof(__at0) == 64
			 || (__have_avx512vl && sizeof(__at0) >= 16))
	return _S_blend_avx512(__k._M_data, __at0._M_data, __at1._M_data);
      else
	{
	  static_assert((__have_avx512vl && sizeof(__at0) < 16)
			|| !__have_avx512vl);
	  constexpr size_t __size = (__have_avx512vl ? 16 : 64) / sizeof(_Tp);
	  return __vector_bitcast<_Tp, _Np>(
	    _S_blend_avx512(__k._M_data, __vector_bitcast<_Tp, __size>(__at0),
			    __vector_bitcast<_Tp, __size>(__at1)));
	}
    }

  template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
    _S_blend(_SimdWrapper<__int_for_sizeof_t<_Tp>, _Np> __k,
	     _SimdWrapper<_Tp, _Np> __at0, _SimdWrapper<_Tp, _Np> __at1)
    {
      const auto __kk = __wrapper_bitcast<_Tp>(__k);
      if (__builtin_is_constant_evaluated()
	  || (__kk._M_is_constprop() && __at0._M_is_constprop()
	      && __at1._M_is_constprop()))
	{
	  auto __r = __or(__andnot(__kk, __at0), __and(__kk, __at1));
	  if (__r._M_is_constprop())
	    return __r;
	}
      if constexpr (((__have_avx512f && sizeof(__at0) == 64) || __have_avx512vl)
		    && (sizeof(_Tp) >= 4 || __have_avx512bw))
	// convert to bitmask and call overload above
	return _S_blend(
	  _SimdWrapper<bool, _Np>(
	    __make_dependent_t<_Tp, _MaskImplX86Mixin>::_S_to_bits(__k)
	      ._M_to_bits()),
	  __at0, __at1);
      else
	{
	  // Since GCC does not assume __k to be a mask, using the builtin
	  // conditional operator introduces an extra compare against 0 before
	  // blending. So we rather call the intrinsic here.
	  if constexpr (__have_sse4_1)
	    return _S_blend_intrin(__to_intrin(__kk), __to_intrin(__at0),
				   __to_intrin(__at1));
	  else
	    return __or(__andnot(__kk, __at0), __and(__kk, __at1));
	}
    }

  // }}}
};

// }}}
// _SimdImplX86 {{{
template <typename _Abi, typename>
  struct _SimdImplX86 : _SimdImplBuiltin<_Abi>
  {
    using _Base = _SimdImplBuiltin<_Abi>;

    template <typename _Tp>
      using _MaskMember = typename _Base::template _MaskMember<_Tp>;

    template <typename _Tp>
      static constexpr size_t _S_full_size = _Abi::template _S_full_size<_Tp>;

    template <typename _Tp>
      static constexpr size_t _S_size = _Abi::template _S_size<_Tp>;

    template <typename _Tp>
      static constexpr size_t _S_max_store_size
	= (sizeof(_Tp) >= 4 && __have_avx512f) || __have_avx512bw  ? 64
	  : (is_floating_point_v<_Tp>&& __have_avx) || __have_avx2 ? 32
								   : 16;
    using _MaskImpl = typename _Abi::_MaskImpl;

    // _S_masked_load {{{
    template <typename _Tp, size_t _Np, typename _Up>
      static inline _SimdWrapper<_Tp, _Np>
      _S_masked_load(_SimdWrapper<_Tp, _Np> __merge, _MaskMember<_Tp> __k,
		     const _Up* __mem) noexcept
      {
	static_assert(_Np == _S_size<_Tp>);
	if constexpr (is_same_v<_Tp, _Up> || // no conversion
		      (sizeof(_Tp) == sizeof(_Up)
		       && is_integral_v<
			    _Tp> == is_integral_v<_Up>) // conversion via bit
							// reinterpretation
	)
	  {
	    [[maybe_unused]] const auto __intrin = __to_intrin(__merge);
	    if constexpr ((__is_avx512_abi<_Abi>() || __have_avx512bw_vl)
			  && sizeof(_Tp) == 1)
	      {
		const auto __kk = _MaskImpl::_S_to_bits(__k)._M_to_bits();
		if constexpr (sizeof(__intrin) == 16)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm_mask_loadu_epi8(__intrin, __kk, __mem));
		else if constexpr (sizeof(__merge) == 32)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm256_mask_loadu_epi8(__intrin, __kk, __mem));
		else if constexpr (sizeof(__merge) == 64)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm512_mask_loadu_epi8(__intrin, __kk, __mem));
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr ((__is_avx512_abi<_Abi>() || __have_avx512bw_vl)
			       && sizeof(_Tp) == 2)
	      {
		const auto __kk = _MaskImpl::_S_to_bits(__k)._M_to_bits();
		if constexpr (sizeof(__intrin) == 16)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm_mask_loadu_epi16(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 32)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm256_mask_loadu_epi16(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 64)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm512_mask_loadu_epi16(__intrin, __kk, __mem));
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr ((__is_avx512_abi<_Abi>() || __have_avx512vl)
			       && sizeof(_Tp) == 4 && is_integral_v<_Up>)
	      {
		const auto __kk = _MaskImpl::_S_to_bits(__k)._M_to_bits();
		if constexpr (sizeof(__intrin) == 16)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm_mask_loadu_epi32(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 32)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm256_mask_loadu_epi32(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 64)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm512_mask_loadu_epi32(__intrin, __kk, __mem));
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr ((__is_avx512_abi<_Abi>() || __have_avx512vl)
			       && sizeof(_Tp) == 4 && is_floating_point_v<_Up>)
	      {
		const auto __kk = _MaskImpl::_S_to_bits(__k)._M_to_bits();
		if constexpr (sizeof(__intrin) == 16)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm_mask_loadu_ps(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 32)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm256_mask_loadu_ps(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 64)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm512_mask_loadu_ps(__intrin, __kk, __mem));
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr (__have_avx2 && sizeof(_Tp) == 4
			       && is_integral_v<_Up>)
	      {
		static_assert(sizeof(__intrin) == 16 || sizeof(__intrin) == 32);
		__merge
		  = __or(__andnot(__vector_bitcast<_Tp>(__k), __merge._M_data),
			 __vector_bitcast<_Tp, _Np>(
			   __maskload_epi32(reinterpret_cast<const int*>(__mem),
					    __to_intrin(__k))));
	      }
	    else if constexpr (__have_avx && sizeof(_Tp) == 4)
	      {
		static_assert(sizeof(__intrin) == 16 || sizeof(__intrin) == 32);
		__merge
		  = __or(__andnot(__vector_bitcast<_Tp>(__k), __merge._M_data),
			 __vector_bitcast<_Tp, _Np>(
			   __maskload_ps(reinterpret_cast<const float*>(__mem),
					 __to_intrin(__k))));
	      }
	    else if constexpr ((__is_avx512_abi<_Abi>() || __have_avx512vl)
			       && sizeof(_Tp) == 8 && is_integral_v<_Up>)
	      {
		const auto __kk = _MaskImpl::_S_to_bits(__k)._M_to_bits();
		if constexpr (sizeof(__intrin) == 16)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm_mask_loadu_epi64(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 32)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm256_mask_loadu_epi64(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 64)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm512_mask_loadu_epi64(__intrin, __kk, __mem));
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr ((__is_avx512_abi<_Abi>() || __have_avx512vl)
			       && sizeof(_Tp) == 8 && is_floating_point_v<_Up>)
	      {
		const auto __kk = _MaskImpl::_S_to_bits(__k)._M_to_bits();
		if constexpr (sizeof(__intrin) == 16)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm_mask_loadu_pd(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 32)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm256_mask_loadu_pd(__intrin, __kk, __mem));
		else if constexpr (sizeof(__intrin) == 64)
		  __merge = __vector_bitcast<_Tp, _Np>(
		    _mm512_mask_loadu_pd(__intrin, __kk, __mem));
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr (__have_avx2 && sizeof(_Tp) == 8
			       && is_integral_v<_Up>)
	      {
		static_assert(sizeof(__intrin) == 16 || sizeof(__intrin) == 32);
		__merge
		  = __or(__andnot(__vector_bitcast<_Tp>(__k), __merge._M_data),
			 __vector_bitcast<_Tp, _Np>(__maskload_epi64(
			   reinterpret_cast<const _LLong*>(__mem),
			   __to_intrin(__k))));
	      }
	    else if constexpr (__have_avx && sizeof(_Tp) == 8)
	      {
		static_assert(sizeof(__intrin) == 16 || sizeof(__intrin) == 32);
		__merge
		  = __or(__andnot(__vector_bitcast<_Tp>(__k), __merge._M_data),
			 __vector_bitcast<_Tp, _Np>(
			   __maskload_pd(reinterpret_cast<const double*>(__mem),
					 __to_intrin(__k))));
	      }
	    else
	      _BitOps::_S_bit_iteration(_MaskImpl::_S_to_bits(__k),
					[&](auto __i) {
					  __merge._M_set(__i, static_cast<_Tp>(
								__mem[__i]));
					});
	  }
	/* Very uncertain, that the following improves anything. Needs
	benchmarking
	 * before it's activated.
	else if constexpr (sizeof(_Up) <= 8 && // no long double
			   !__converts_via_decomposition_v<
			     _Up, _Tp,
			     sizeof(__merge)> // conversion via decomposition
					      // is better handled via the
					      // bit_iteration fallback below
	)
	  {
	    // TODO: copy pattern from _S_masked_store, which doesn't resort to
	    // fixed_size
	    using _Ap       = simd_abi::deduce_t<_Up, _Np>;
	    using _ATraits = _SimdTraits<_Up, _Ap>;
	    using _AImpl   = typename _ATraits::_SimdImpl;
	    typename _ATraits::_SimdMember __uncvted{};
	    typename _ATraits::_MaskMember __kk = _Ap::_MaskImpl::template
	_S_convert<_Up>(__k);
	    __uncvted = _AImpl::_S_masked_load(__uncvted, __kk, __mem);
	    _SimdConverter<_Up, _Ap, _Tp, _Abi> __converter;
	    _Base::_S_masked_assign(__k, __merge, __converter(__uncvted));
	  }
	  */
	else
	  __merge = _Base::_S_masked_load(__merge, __k, __mem);
	return __merge;
      }

    // }}}
    // _S_masked_store_nocvt {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_store_nocvt(_SimdWrapper<_Tp, _Np> __v, _Tp* __mem,
			    _SimdWrapper<bool, _Np> __k)
      {
	[[maybe_unused]] const auto __vi = __to_intrin(__v);
	if constexpr (sizeof(__vi) == 64)
	  {
	    static_assert(sizeof(__v) == 64 && __have_avx512f);
	    if constexpr (__have_avx512bw && sizeof(_Tp) == 1)
	      _mm512_mask_storeu_epi8(__mem, __k, __vi);
	    else if constexpr (__have_avx512bw && sizeof(_Tp) == 2)
	      _mm512_mask_storeu_epi16(__mem, __k, __vi);
	    else if constexpr (__have_avx512f && sizeof(_Tp) == 4)
	      {
		if constexpr (is_integral_v<_Tp>)
		  _mm512_mask_storeu_epi32(__mem, __k, __vi);
		else
		  _mm512_mask_storeu_ps(__mem, __k, __vi);
	      }
	    else if constexpr (__have_avx512f && sizeof(_Tp) == 8)
	      {
		if constexpr (is_integral_v<_Tp>)
		  _mm512_mask_storeu_epi64(__mem, __k, __vi);
		else
		  _mm512_mask_storeu_pd(__mem, __k, __vi);
	      }
#if 0 // with KNL either sizeof(_Tp) >= 4 or sizeof(_vi) <= 32
      // with Skylake-AVX512, __have_avx512bw is true
	  else if constexpr (__have_sse2)
	    {
	      using _M   = __vector_type_t<_Tp, _Np>;
	      using _MVT = _VectorTraits<_M>;
	      _mm_maskmoveu_si128(__auto_bitcast(__extract<0, 4>(__v._M_data)),
				  __auto_bitcast(_MaskImpl::template _S_convert<_Tp, _Np>(__k._M_data)),
				  reinterpret_cast<char*>(__mem));
	      _mm_maskmoveu_si128(__auto_bitcast(__extract<1, 4>(__v._M_data)),
				  __auto_bitcast(_MaskImpl::template _S_convert<_Tp, _Np>(
				    __k._M_data >> 1 * _MVT::_S_full_size)),
				  reinterpret_cast<char*>(__mem) + 1 * 16);
	      _mm_maskmoveu_si128(__auto_bitcast(__extract<2, 4>(__v._M_data)),
				  __auto_bitcast(_MaskImpl::template _S_convert<_Tp, _Np>(
				    __k._M_data >> 2 * _MVT::_S_full_size)),
				  reinterpret_cast<char*>(__mem) + 2 * 16);
	      if constexpr (_Np > 48 / sizeof(_Tp))
		_mm_maskmoveu_si128(
		  __auto_bitcast(__extract<3, 4>(__v._M_data)),
		  __auto_bitcast(_MaskImpl::template _S_convert<_Tp, _Np>(
		    __k._M_data >> 3 * _MVT::_S_full_size)),
		  reinterpret_cast<char*>(__mem) + 3 * 16);
	    }
#endif
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (sizeof(__vi) == 32)
	  {
	    if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 1)
	      _mm256_mask_storeu_epi8(__mem, __k, __vi);
	    else if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 2)
	      _mm256_mask_storeu_epi16(__mem, __k, __vi);
	    else if constexpr (__have_avx512vl && sizeof(_Tp) == 4)
	      {
		if constexpr (is_integral_v<_Tp>)
		  _mm256_mask_storeu_epi32(__mem, __k, __vi);
		else
		  _mm256_mask_storeu_ps(__mem, __k, __vi);
	      }
	    else if constexpr (__have_avx512vl && sizeof(_Tp) == 8)
	      {
		if constexpr (is_integral_v<_Tp>)
		  _mm256_mask_storeu_epi64(__mem, __k, __vi);
		else
		  _mm256_mask_storeu_pd(__mem, __k, __vi);
	      }
	    else if constexpr (__have_avx512f
			       && (sizeof(_Tp) >= 4 || __have_avx512bw))
	      {
		// use a 512-bit maskstore, using zero-extension of the bitmask
		_S_masked_store_nocvt(
		  _SimdWrapper64<_Tp>(
		    __intrin_bitcast<__vector_type64_t<_Tp>>(__v._M_data)),
		  __mem, _SimdWrapper<bool, 64 / sizeof(_Tp)>(__k._M_data));
	      }
	    else
	      _S_masked_store_nocvt(__v, __mem,
				    _MaskImpl::template _S_to_maskvector<
				      __int_for_sizeof_t<_Tp>, _Np>(__k));
	  }
	else if constexpr (sizeof(__vi) == 16)
	  {
	    if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 1)
	      _mm_mask_storeu_epi8(__mem, __k, __vi);
	    else if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 2)
	      _mm_mask_storeu_epi16(__mem, __k, __vi);
	    else if constexpr (__have_avx512vl && sizeof(_Tp) == 4)
	      {
		if constexpr (is_integral_v<_Tp>)
		  _mm_mask_storeu_epi32(__mem, __k, __vi);
		else
		  _mm_mask_storeu_ps(__mem, __k, __vi);
	      }
	    else if constexpr (__have_avx512vl && sizeof(_Tp) == 8)
	      {
		if constexpr (is_integral_v<_Tp>)
		  _mm_mask_storeu_epi64(__mem, __k, __vi);
		else
		  _mm_mask_storeu_pd(__mem, __k, __vi);
	      }
	    else if constexpr (__have_avx512f
			       && (sizeof(_Tp) >= 4 || __have_avx512bw))
	      {
		// use a 512-bit maskstore, using zero-extension of the bitmask
		_S_masked_store_nocvt(
		  _SimdWrapper64<_Tp>(
		    __intrin_bitcast<__intrinsic_type64_t<_Tp>>(__v._M_data)),
		  __mem, _SimdWrapper<bool, 64 / sizeof(_Tp)>(__k._M_data));
	      }
	    else
	      _S_masked_store_nocvt(__v, __mem,
				    _MaskImpl::template _S_to_maskvector<
				      __int_for_sizeof_t<_Tp>, _Np>(__k));
	  }
	else
	  __assert_unreachable<_Tp>();
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_store_nocvt(_SimdWrapper<_Tp, _Np> __v, _Tp* __mem,
			    _SimdWrapper<__int_for_sizeof_t<_Tp>, _Np> __k)
      {
	if constexpr (sizeof(__v) <= 16)
	  {
	    [[maybe_unused]] const auto __vi
	      = __intrin_bitcast<__m128i>(__as_vector(__v));
	    [[maybe_unused]] const auto __ki
	      = __intrin_bitcast<__m128i>(__as_vector(__k));
	    if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 1)
	      _mm_mask_storeu_epi8(__mem, _mm_movepi8_mask(__ki), __vi);
	    else if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 2)
	      _mm_mask_storeu_epi16(__mem, _mm_movepi16_mask(__ki), __vi);
	    else if constexpr (__have_avx2 && sizeof(_Tp) == 4
			       && is_integral_v<_Tp>)
	      _mm_maskstore_epi32(reinterpret_cast<int*>(__mem), __ki, __vi);
	    else if constexpr (__have_avx && sizeof(_Tp) == 4)
	      _mm_maskstore_ps(reinterpret_cast<float*>(__mem), __ki,
			       __vector_bitcast<float>(__vi));
	    else if constexpr (__have_avx2 && sizeof(_Tp) == 8
			       && is_integral_v<_Tp>)
	      _mm_maskstore_epi64(reinterpret_cast<_LLong*>(__mem), __ki, __vi);
	    else if constexpr (__have_avx && sizeof(_Tp) == 8)
	      _mm_maskstore_pd(reinterpret_cast<double*>(__mem), __ki,
			       __vector_bitcast<double>(__vi));
	    else if constexpr (__have_sse2)
	      _mm_maskmoveu_si128(__vi, __ki, reinterpret_cast<char*>(__mem));
	  }
	else if constexpr (sizeof(__v) == 32)
	  {
	    [[maybe_unused]] const auto __vi
	      = __intrin_bitcast<__m256i>(__as_vector(__v));
	    [[maybe_unused]] const auto __ki
	      = __intrin_bitcast<__m256i>(__as_vector(__k));
	    if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 1)
	      _mm256_mask_storeu_epi8(__mem, _mm256_movepi8_mask(__ki), __vi);
	    else if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 2)
	      _mm256_mask_storeu_epi16(__mem, _mm256_movepi16_mask(__ki), __vi);
	    else if constexpr (__have_avx2 && sizeof(_Tp) == 4
			       && is_integral_v<_Tp>)
	      _mm256_maskstore_epi32(reinterpret_cast<int*>(__mem), __ki, __vi);
	    else if constexpr (sizeof(_Tp) == 4)
	      _mm256_maskstore_ps(reinterpret_cast<float*>(__mem), __ki,
				  __vector_bitcast<float>(__v));
	    else if constexpr (__have_avx2 && sizeof(_Tp) == 8
			       && is_integral_v<_Tp>)
	      _mm256_maskstore_epi64(reinterpret_cast<_LLong*>(__mem), __ki,
				     __vi);
	    else if constexpr (__have_avx && sizeof(_Tp) == 8)
	      _mm256_maskstore_pd(reinterpret_cast<double*>(__mem), __ki,
				  __vector_bitcast<double>(__v));
	    else if constexpr (__have_sse2)
	      {
		_mm_maskmoveu_si128(__lo128(__vi), __lo128(__ki),
				    reinterpret_cast<char*>(__mem));
		_mm_maskmoveu_si128(__hi128(__vi), __hi128(__ki),
				    reinterpret_cast<char*>(__mem) + 16);
	      }
	  }
	else
	  __assert_unreachable<_Tp>();
      }

    // }}}
    // _S_masked_store {{{
    template <typename _Tp, size_t _Np, typename _Up>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_store(const _SimdWrapper<_Tp, _Np> __v, _Up* __mem,
		      const _MaskMember<_Tp> __k) noexcept
      {
	if constexpr (is_integral_v<
			_Tp> && is_integral_v<_Up> && sizeof(_Tp) > sizeof(_Up)
		      && __have_avx512f && (sizeof(_Tp) >= 4 || __have_avx512bw)
		      && (sizeof(__v) == 64 || __have_avx512vl))
	  { // truncating store
	    const auto __vi = __to_intrin(__v);
	    const auto __kk = _MaskImpl::_S_to_bits(__k)._M_to_bits();
	    if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 4
			  && sizeof(__vi) == 64)
	      _mm512_mask_cvtepi64_storeu_epi32(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 4
			       && sizeof(__vi) == 32)
	      _mm256_mask_cvtepi64_storeu_epi32(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 4
			       && sizeof(__vi) == 16)
	      _mm_mask_cvtepi64_storeu_epi32(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 2
			       && sizeof(__vi) == 64)
	      _mm512_mask_cvtepi64_storeu_epi16(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 2
			       && sizeof(__vi) == 32)
	      _mm256_mask_cvtepi64_storeu_epi16(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 2
			       && sizeof(__vi) == 16)
	      _mm_mask_cvtepi64_storeu_epi16(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 1
			       && sizeof(__vi) == 64)
	      _mm512_mask_cvtepi64_storeu_epi8(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 1
			       && sizeof(__vi) == 32)
	      _mm256_mask_cvtepi64_storeu_epi8(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 1
			       && sizeof(__vi) == 16)
	      _mm_mask_cvtepi64_storeu_epi8(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 4 && sizeof(_Up) == 2
			       && sizeof(__vi) == 64)
	      _mm512_mask_cvtepi32_storeu_epi16(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 4 && sizeof(_Up) == 2
			       && sizeof(__vi) == 32)
	      _mm256_mask_cvtepi32_storeu_epi16(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 4 && sizeof(_Up) == 2
			       && sizeof(__vi) == 16)
	      _mm_mask_cvtepi32_storeu_epi16(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 4 && sizeof(_Up) == 1
			       && sizeof(__vi) == 64)
	      _mm512_mask_cvtepi32_storeu_epi8(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 4 && sizeof(_Up) == 1
			       && sizeof(__vi) == 32)
	      _mm256_mask_cvtepi32_storeu_epi8(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 4 && sizeof(_Up) == 1
			       && sizeof(__vi) == 16)
	      _mm_mask_cvtepi32_storeu_epi8(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 2 && sizeof(_Up) == 1
			       && sizeof(__vi) == 64)
	      _mm512_mask_cvtepi16_storeu_epi8(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 2 && sizeof(_Up) == 1
			       && sizeof(__vi) == 32)
	      _mm256_mask_cvtepi16_storeu_epi8(__mem, __kk, __vi);
	    else if constexpr (sizeof(_Tp) == 2 && sizeof(_Up) == 1
			       && sizeof(__vi) == 16)
	      _mm_mask_cvtepi16_storeu_epi8(__mem, __kk, __vi);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  _Base::_S_masked_store(__v, __mem, __k);
      }

    // }}}
    // _S_multiplies {{{
    template <typename _V, typename _VVT = _VectorTraits<_V>>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _V _S_multiplies(_V __x, _V __y)
      {
	using _Tp = typename _VVT::value_type;
	if (__builtin_is_constant_evaluated() || __x._M_is_constprop()
	    || __y._M_is_constprop())
	  return __as_vector(__x) * __as_vector(__y);
	else if constexpr (sizeof(_Tp) == 1)
	  {
	    if constexpr (sizeof(_V) == 2)
	      {
		const auto __xs = reinterpret_cast<short>(__x._M_data);
		const auto __ys = reinterpret_cast<short>(__y._M_data);
		return reinterpret_cast<__vector_type_t<_Tp, 2>>(short(
		  ((__xs * __ys) & 0xff) | ((__xs >> 8) * (__ys & 0xff00))));
	      }
	    else if constexpr (sizeof(_V) == 4 && _VVT::_S_partial_width == 3)
	      {
		const auto __xi = reinterpret_cast<int>(__x._M_data);
		const auto __yi = reinterpret_cast<int>(__y._M_data);
		return reinterpret_cast<__vector_type_t<_Tp, 3>>(
		  ((__xi * __yi) & 0xff)
		  | (((__xi >> 8) * (__yi & 0xff00)) & 0xff00)
		  | ((__xi >> 16) * (__yi & 0xff0000)));
	      }
	    else if constexpr (sizeof(_V) == 4)
	      {
		const auto __xi = reinterpret_cast<int>(__x._M_data);
		const auto __yi = reinterpret_cast<int>(__y._M_data);
		return reinterpret_cast<__vector_type_t<_Tp, 4>>(
		  ((__xi * __yi) & 0xff)
		  | (((__xi >> 8) * (__yi & 0xff00)) & 0xff00)
		  | (((__xi >> 16) * (__yi & 0xff0000)) & 0xff0000)
		  | ((__xi >> 24) * (__yi & 0xff000000u)));
	      }
	    else if constexpr (sizeof(_V) == 8 && __have_avx2
			       && is_signed_v<_Tp>)
	      return __convert<typename _VVT::type>(
		__vector_bitcast<short>(_mm_cvtepi8_epi16(__to_intrin(__x)))
		* __vector_bitcast<short>(_mm_cvtepi8_epi16(__to_intrin(__y))));
	    else if constexpr (sizeof(_V) == 8 && __have_avx2
			       && is_unsigned_v<_Tp>)
	      return __convert<typename _VVT::type>(
		__vector_bitcast<short>(_mm_cvtepu8_epi16(__to_intrin(__x)))
		* __vector_bitcast<short>(_mm_cvtepu8_epi16(__to_intrin(__y))));
	    else
	      {
		// codegen of `x*y` is suboptimal (as of GCC 9.0.1)
		constexpr size_t __full_size = _VVT::_S_full_size;
		constexpr int _Np = sizeof(_V) >= 16 ? __full_size / 2 : 8;
		using _ShortW = _SimdWrapper<short, _Np>;
		const _ShortW __even = __vector_bitcast<short, _Np>(__x)
				       * __vector_bitcast<short, _Np>(__y);
		_ShortW __high_byte = _ShortW()._M_data - 256;
		//[&]() { asm("" : "+x"(__high_byte._M_data)); }();
		const _ShortW __odd
		  = (__vector_bitcast<short, _Np>(__x) >> 8)
		    * (__vector_bitcast<short, _Np>(__y) & __high_byte._M_data);
		if constexpr (__have_avx512bw && sizeof(_V) > 2)
		  return _CommonImplX86::_S_blend_avx512(
		    0xaaaa'aaaa'aaaa'aaaaLL, __vector_bitcast<_Tp>(__even),
		    __vector_bitcast<_Tp>(__odd));
		else if constexpr (__have_sse4_1 && sizeof(_V) > 2)
		  return _CommonImplX86::_S_blend_intrin(__to_intrin(
							   __high_byte),
							 __to_intrin(__even),
							 __to_intrin(__odd));
		else
		  return __to_intrin(
		    __or(__andnot(__high_byte, __even), __odd));
	      }
	  }
	else
	  return _Base::_S_multiplies(__x, __y);
      }

    // }}}
    // _S_divides {{{
#ifdef _GLIBCXX_SIMD_WORKAROUND_PR90993
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_divides(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	if (!__builtin_is_constant_evaluated()
	    && !__builtin_constant_p(__y._M_data))
	  if constexpr (is_integral_v<_Tp> && sizeof(_Tp) <= 4)
	    { // use divps - codegen of `x/y` is suboptimal (as of GCC 9.0.1)
	      // Note that using floating-point division is likely to raise the
	      // *Inexact* exception flag and thus appears like an invalid
	      // "as-if" transformation. However, C++ doesn't specify how the
	      // fpenv can be observed and points to C. C says that function
	      // calls are assumed to potentially raise fp exceptions, unless
	      // documented otherwise. Consequently, operator/, which is a
	      // function call, may raise fp exceptions.
	      /*const struct _CsrGuard
	      {
		const unsigned _M_data = _mm_getcsr();
		_CsrGuard()
		{
		  _mm_setcsr(0x9f80); // turn off FP exceptions and
	      flush-to-zero
		}
		~_CsrGuard() { _mm_setcsr(_M_data); }
	      } __csr;*/
	      using _Float = conditional_t<sizeof(_Tp) == 4, double, float>;
	      constexpr size_t __n_intermediate
		= std::min(_Np, (__have_avx512f ? 64
				 : __have_avx   ? 32
						: 16)
				  / sizeof(_Float));
	      using _FloatV = __vector_type_t<_Float, __n_intermediate>;
	      constexpr size_t __n_floatv
		= __div_roundup(_Np, __n_intermediate);
	      using _R = __vector_type_t<_Tp, _Np>;
	      const auto __xf = __convert_all<_FloatV, __n_floatv>(__x);
	      const auto __yf = __convert_all<_FloatV, __n_floatv>(
		_Abi::__make_padding_nonzero(__as_vector(__y)));
	      return __call_with_n_evaluations<__n_floatv>(
		[](auto... __quotients) {
		  return __vector_convert<_R>(__quotients...);
		},
		[&__xf,
		 &__yf](auto __i) -> _SimdWrapper<_Float, __n_intermediate> {
#if !defined __clang__ && __GCC_IEC_559 == 0
		  // If -freciprocal-math is active, using the `/` operator is
		  // incorrect because it may be translated to an imprecise
		  // multiplication with reciprocal. We need to use inline
		  // assembly to force a real division.
		  _FloatV __r;
		  if constexpr (__have_avx) // -mno-sse2avx is irrelevant
					    // because once -mavx is given, GCC
					    // emits VEX encoded vdivp[sd]
		    {
		      if constexpr (sizeof(_Tp) == 4)
			asm("vdivpd\t{%2, %1, %0|%0, %1, %2}"
			    : "=x"(__r)
			    : "x"(__xf[__i]), "x"(__yf[__i]));
		      else
			asm("vdivps\t{%2, %1, %0|%0, %1, %2}"
			    : "=x"(__r)
			    : "x"(__xf[__i]), "x"(__yf[__i]));
		    }
		  else
		    {
		      __r = __xf[__i];
		      if constexpr (sizeof(_Tp) == 4)
			asm("divpd\t{%1, %0|%0, %1}"
			    : "=x"(__r)
			    : "x"(__yf[__i]));
		      else
			asm("divps\t{%1, %0|%0, %1}"
			    : "=x"(__r)
			    : "x"(__yf[__i]));
		    }
		  return __r;
#else
		  return __xf[__i] / __yf[__i];
#endif
		});
	    }
	/* 64-bit int division is potentially optimizable via double division if
	 * the value in __x is small enough and the conversion between
	 * int<->double is efficient enough:
	else if constexpr (is_integral_v<_Tp> && is_unsigned_v<_Tp> &&
			   sizeof(_Tp) == 8)
	  {
	    if constexpr (__have_sse4_1 && sizeof(__x) == 16)
	      {
		if (_mm_test_all_zeros(__x, __m128i{0xffe0'0000'0000'0000ull,
						    0xffe0'0000'0000'0000ull}))
		  {
		    __x._M_data | 0x __vector_convert<__m128d>(__x._M_data)
		  }
	      }
	  }
	  */
	return _Base::_S_divides(__x, __y);
      }
  #endif // _GLIBCXX_SIMD_WORKAROUND_PR90993

    // }}}
    // _S_modulus {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_modulus(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	if (__builtin_is_constant_evaluated()
	    || __builtin_constant_p(__y._M_data) || sizeof(_Tp) >= 8)
	  return _Base::_S_modulus(__x, __y);
	else
	  return _Base::_S_minus(__x, _S_multiplies(__y, _S_divides(__x, __y)));
      }

    // }}}
    // _S_bit_shift_left {{{
    // Notes on UB. C++2a [expr.shift] says:
    // -1- [...] The operands shall be of integral or unscoped enumeration type
    //     and integral promotions are performed. The type of the result is that
    //     of the promoted left operand. The behavior is undefined if the right
    //     operand is negative, or greater than or equal to the width of the
    //     promoted left operand.
    // -2- The value of E1 << E2 is the unique value congruent to E1×2^E2 modulo
    //     2^N, where N is the width of the type of the result.
    //
    // C++17 [expr.shift] says:
    // -2- The value of E1 << E2 is E1 left-shifted E2 bit positions; vacated
    //     bits are zero-filled. If E1 has an unsigned type, the value of the
    //     result is E1 × 2^E2 , reduced modulo one more than the maximum value
    //     representable in the result type. Otherwise, if E1 has a signed type
    //     and non-negative value, and E1 × 2^E2 is representable in the
    //     corresponding unsigned type of the result type, then that value,
    //     converted to the result type, is the resulting value; otherwise, the
    //     behavior is undefined.
    //
    // Consequences:
    // With C++2a signed and unsigned types have the same UB
    // characteristics:
    // - left shift is not UB for 0 <= RHS < max(32, #bits(T))
    //
    // With C++17 there's little room for optimizations because the standard
    // requires all shifts to happen on promoted integrals (i.e. int). Thus,
    // short and char shifts must assume shifts affect bits of neighboring
    // values.
  #ifndef _GLIBCXX_SIMD_NO_SHIFT_OPT
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      inline _GLIBCXX_CONST static typename _TVT::type
      _S_bit_shift_left(_Tp __xx, int __y)
      {
	using _V = typename _TVT::type;
	using _Up = typename _TVT::value_type;
	_V __x = __xx;
	[[maybe_unused]] const auto __ix = __to_intrin(__x);
	if (__builtin_is_constant_evaluated())
	  return __x << __y;
#if __cplusplus > 201703
	// after C++17, signed shifts have no UB, and behave just like unsigned
	// shifts
	else if constexpr (sizeof(_Up) == 1 && is_signed_v<_Up>)
	  return __vector_bitcast<_Up>(
	    _S_bit_shift_left(__vector_bitcast<make_unsigned_t<_Up>>(__x),
			      __y));
#endif
	else if constexpr (sizeof(_Up) == 1)
	  {
	    // (cf. https://gcc.gnu.org/bugzilla/show_bug.cgi?id=83894)
	    if (__builtin_constant_p(__y))
	      {
		if (__y == 0)
		  return __x;
		else if (__y == 1)
		  return __x + __x;
		else if (__y == 2)
		  {
		    __x = __x + __x;
		    return __x + __x;
		  }
		else if (__y > 2 && __y < 8)
		  {
		    if constexpr (sizeof(__x) > sizeof(unsigned))
		      {
			const _UChar __mask = 0xff << __y; // precomputed vector
			return __vector_bitcast<_Up>(
			  __vector_bitcast<_UChar>(
			    __vector_bitcast<unsigned>(__x) << __y)
			  & __mask);
		      }
		    else
		      {
			const unsigned __mask
			  = (0xff & (0xff << __y)) * 0x01010101u;
			return reinterpret_cast<_V>(
			  static_cast<__int_for_sizeof_t<_V>>(
			    unsigned(
			      reinterpret_cast<__int_for_sizeof_t<_V>>(__x)
			      << __y)
			    & __mask));
		      }
		  }
		else if (__y >= 8 && __y < 32)
		  return _V();
		else
		  __builtin_unreachable();
	      }
	    // general strategy in the following: use an sllv instead of sll
	    // instruction, because it's 2 to 4 times faster:
	    else if constexpr (__have_avx512bw_vl && sizeof(__x) == 16)
	      return __vector_bitcast<_Up>(_mm256_cvtepi16_epi8(
		_mm256_sllv_epi16(_mm256_cvtepi8_epi16(__ix),
				  _mm256_set1_epi16(__y))));
	    else if constexpr (__have_avx512bw && sizeof(__x) == 32)
	      return __vector_bitcast<_Up>(_mm512_cvtepi16_epi8(
		_mm512_sllv_epi16(_mm512_cvtepi8_epi16(__ix),
				  _mm512_set1_epi16(__y))));
	    else if constexpr (__have_avx512bw && sizeof(__x) == 64)
	      {
		const auto __shift = _mm512_set1_epi16(__y);
		return __vector_bitcast<_Up>(
		  __concat(_mm512_cvtepi16_epi8(_mm512_sllv_epi16(
			     _mm512_cvtepi8_epi16(__lo256(__ix)), __shift)),
			   _mm512_cvtepi16_epi8(_mm512_sllv_epi16(
			     _mm512_cvtepi8_epi16(__hi256(__ix)), __shift))));
	      }
	    else if constexpr (__have_avx2 && sizeof(__x) == 32)
	      {
#if 1
		const auto __shift = _mm_cvtsi32_si128(__y);
		auto __k
		  = _mm256_sll_epi16(_mm256_slli_epi16(~__m256i(), 8), __shift);
		__k |= _mm256_srli_epi16(__k, 8);
		return __vector_bitcast<_Up>(_mm256_sll_epi32(__ix, __shift)
					     & __k);
#else
		const _Up __k = 0xff << __y;
		return __vector_bitcast<_Up>(__vector_bitcast<int>(__x) << __y)
		       & __k;
#endif
	      }
	    else
	      {
		const auto __shift = _mm_cvtsi32_si128(__y);
		auto __k
		  = _mm_sll_epi16(_mm_slli_epi16(~__m128i(), 8), __shift);
		__k |= _mm_srli_epi16(__k, 8);
		return __intrin_bitcast<_V>(_mm_sll_epi16(__ix, __shift) & __k);
	      }
	  }
	return __x << __y;
      }

    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      inline _GLIBCXX_CONST static typename _TVT::type
      _S_bit_shift_left(_Tp __xx, typename _TVT::type __y)
      {
	using _V = typename _TVT::type;
	using _Up = typename _TVT::value_type;
	_V __x = __xx;
	[[maybe_unused]] const auto __ix = __to_intrin(__x);
	[[maybe_unused]] const auto __iy = __to_intrin(__y);
	if (__builtin_is_constant_evaluated())
	  return __x << __y;
#if __cplusplus > 201703
	// after C++17, signed shifts have no UB, and behave just like unsigned
	// shifts
	else if constexpr (is_signed_v<_Up>)
	  return __vector_bitcast<_Up>(
	    _S_bit_shift_left(__vector_bitcast<make_unsigned_t<_Up>>(__x),
			      __vector_bitcast<make_unsigned_t<_Up>>(__y)));
#endif
	else if constexpr (sizeof(_Up) == 1)
	  {
	    if constexpr (sizeof __ix == 64 && __have_avx512bw)
	      return __vector_bitcast<_Up>(__concat(
		_mm512_cvtepi16_epi8(
		  _mm512_sllv_epi16(_mm512_cvtepu8_epi16(__lo256(__ix)),
				    _mm512_cvtepu8_epi16(__lo256(__iy)))),
		_mm512_cvtepi16_epi8(
		  _mm512_sllv_epi16(_mm512_cvtepu8_epi16(__hi256(__ix)),
				    _mm512_cvtepu8_epi16(__hi256(__iy))))));
	    else if constexpr (sizeof __ix == 32 && __have_avx512bw)
	      return __vector_bitcast<_Up>(_mm512_cvtepi16_epi8(
		_mm512_sllv_epi16(_mm512_cvtepu8_epi16(__ix),
				  _mm512_cvtepu8_epi16(__iy))));
	    else if constexpr (sizeof __x <= 8 && __have_avx512bw_vl)
	      return __intrin_bitcast<_V>(
		_mm_cvtepi16_epi8(_mm_sllv_epi16(_mm_cvtepu8_epi16(__ix),
						 _mm_cvtepu8_epi16(__iy))));
	    else if constexpr (sizeof __ix == 16 && __have_avx512bw_vl)
	      return __intrin_bitcast<_V>(_mm256_cvtepi16_epi8(
		_mm256_sllv_epi16(_mm256_cvtepu8_epi16(__ix),
				  _mm256_cvtepu8_epi16(__iy))));
	    else if constexpr (sizeof __ix == 16 && __have_avx512bw)
	      return __intrin_bitcast<_V>(
		__lo128(_mm512_cvtepi16_epi8(_mm512_sllv_epi16(
		  _mm512_cvtepu8_epi16(_mm256_castsi128_si256(__ix)),
		  _mm512_cvtepu8_epi16(_mm256_castsi128_si256(__iy))))));
	    else if constexpr (__have_sse4_1 && sizeof(__x) == 16)
	      {
		auto __mask
		  = __vector_bitcast<_Up>(__vector_bitcast<short>(__y) << 5);
		auto __x4
		  = __vector_bitcast<_Up>(__vector_bitcast<short>(__x) << 4);
		__x4 &= char(0xf0);
		__x = reinterpret_cast<_V>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__x), __to_intrin(__x4)));
		__mask += __mask;
		auto __x2
		  = __vector_bitcast<_Up>(__vector_bitcast<short>(__x) << 2);
		__x2 &= char(0xfc);
		__x = reinterpret_cast<_V>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__x), __to_intrin(__x2)));
		__mask += __mask;
		auto __x1 = __x + __x;
		__x = reinterpret_cast<_V>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__x), __to_intrin(__x1)));
		return __x
		       & ((__y & char(0xf8)) == 0); // y > 7 nulls the result
	      }
	    else if constexpr (sizeof(__x) == 16)
	      {
		auto __mask
		  = __vector_bitcast<_UChar>(__vector_bitcast<short>(__y) << 5);
		auto __x4
		  = __vector_bitcast<_Up>(__vector_bitcast<short>(__x) << 4);
		__x4 &= char(0xf0);
		__x = __vector_bitcast<_SChar>(__mask) < 0 ? __x4 : __x;
		__mask += __mask;
		auto __x2
		  = __vector_bitcast<_Up>(__vector_bitcast<short>(__x) << 2);
		__x2 &= char(0xfc);
		__x = __vector_bitcast<_SChar>(__mask) < 0 ? __x2 : __x;
		__mask += __mask;
		auto __x1 = __x + __x;
		__x = __vector_bitcast<_SChar>(__mask) < 0 ? __x1 : __x;
		return __x
		       & ((__y & char(0xf8)) == 0); // y > 7 nulls the result
	      }
	    else
	      return __x << __y;
	  }
	else if constexpr (sizeof(_Up) == 2)
	  {
	    if constexpr (sizeof __ix == 64 && __have_avx512bw)
	      return __vector_bitcast<_Up>(_mm512_sllv_epi16(__ix, __iy));
	    else if constexpr (sizeof __ix == 32 && __have_avx512bw_vl)
	      return __vector_bitcast<_Up>(_mm256_sllv_epi16(__ix, __iy));
	    else if constexpr (sizeof __ix == 32 && __have_avx512bw)
	      return __vector_bitcast<_Up>(
		__lo256(_mm512_sllv_epi16(_mm512_castsi256_si512(__ix),
					  _mm512_castsi256_si512(__iy))));
	    else if constexpr (sizeof __ix == 32 && __have_avx2)
	      {
		const auto __ux = __vector_bitcast<unsigned>(__x);
		const auto __uy = __vector_bitcast<unsigned>(__y);
		return __vector_bitcast<_Up>(_mm256_blend_epi16(
		  __auto_bitcast(__ux << (__uy & 0x0000ffffu)),
		  __auto_bitcast((__ux & 0xffff0000u) << (__uy >> 16)), 0xaa));
	      }
	    else if constexpr (sizeof __ix == 16 && __have_avx512bw_vl)
	      return __intrin_bitcast<_V>(_mm_sllv_epi16(__ix, __iy));
	    else if constexpr (sizeof __ix == 16 && __have_avx512bw)
	      return __intrin_bitcast<_V>(
		__lo128(_mm512_sllv_epi16(_mm512_castsi128_si512(__ix),
					  _mm512_castsi128_si512(__iy))));
	    else if constexpr (sizeof __ix == 16 && __have_avx2)
	      {
		const auto __ux = __vector_bitcast<unsigned>(__ix);
		const auto __uy = __vector_bitcast<unsigned>(__iy);
		return __intrin_bitcast<_V>(_mm_blend_epi16(
		  __auto_bitcast(__ux << (__uy & 0x0000ffffu)),
		  __auto_bitcast((__ux & 0xffff0000u) << (__uy >> 16)), 0xaa));
	      }
	    else if constexpr (sizeof __ix == 16)
	      {
		using _Float4 = __vector_type_t<float, 4>;
		using _Int4 = __vector_type_t<int, 4>;
		using _UInt4 = __vector_type_t<unsigned, 4>;
		const _UInt4 __yu
		  = reinterpret_cast<_UInt4>(__to_intrin(__y + (0x3f8 >> 3)));
		return __x
		       * __intrin_bitcast<_V>(
			 __vector_convert<_Int4>(_SimdWrapper<float, 4>(
			   reinterpret_cast<_Float4>(__yu << 23)))
			 | (__vector_convert<_Int4>(_SimdWrapper<float, 4>(
			      reinterpret_cast<_Float4>((__yu >> 16) << 23)))
			    << 16));
	      }
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (sizeof(_Up) == 4 && sizeof __ix == 16
			   && !__have_avx2)
	  // latency is suboptimal, but throughput is at full speedup
	  return __intrin_bitcast<_V>(
	    __vector_bitcast<unsigned>(__ix)
	    * __vector_convert<__vector_type16_t<int>>(
	      _SimdWrapper<float, 4>(__vector_bitcast<float>(
		(__vector_bitcast<unsigned, 4>(__y) << 23) + 0x3f80'0000))));
	else if constexpr (sizeof(_Up) == 8 && sizeof __ix == 16
			   && !__have_avx2)
	  {
	    const auto __lo = _mm_sll_epi64(__ix, __iy);
	    const auto __hi
	      = _mm_sll_epi64(__ix, _mm_unpackhi_epi64(__iy, __iy));
	    if constexpr (__have_sse4_1)
	      return __vector_bitcast<_Up>(_mm_blend_epi16(__lo, __hi, 0xf0));
	    else
	      return __vector_bitcast<_Up>(
		_mm_move_sd(__vector_bitcast<double>(__hi),
			    __vector_bitcast<double>(__lo)));
	  }
	else
	  return __x << __y;
      }
#endif // _GLIBCXX_SIMD_NO_SHIFT_OPT

    // }}}
    // _S_bit_shift_right {{{
#ifndef _GLIBCXX_SIMD_NO_SHIFT_OPT
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      inline _GLIBCXX_CONST static typename _TVT::type
      _S_bit_shift_right(_Tp __xx, int __y)
      {
	using _V = typename _TVT::type;
	using _Up = typename _TVT::value_type;
	_V __x = __xx;
	[[maybe_unused]] const auto __ix = __to_intrin(__x);
	if (__builtin_is_constant_evaluated())
	  return __x >> __y;
	else if (__builtin_constant_p(__y)
		 && is_unsigned_v<
		   _Up> && __y >= int(sizeof(_Up) * __CHAR_BIT__))
	  return _V();
	else if constexpr (sizeof(_Up) == 1 && is_unsigned_v<_Up>) //{{{
	  return __intrin_bitcast<_V>(__vector_bitcast<_UShort>(__ix) >> __y)
		 & _Up(0xff >> __y);
	//}}}
	else if constexpr (sizeof(_Up) == 1 && is_signed_v<_Up>) //{{{
	  return __intrin_bitcast<_V>(
	    (__vector_bitcast<_UShort>(__vector_bitcast<short>(__ix)
				       >> (__y + 8))
	     << 8)
	    | (__vector_bitcast<_UShort>(
		 __vector_bitcast<short>(__vector_bitcast<_UShort>(__ix) << 8)
		 >> __y)
	       >> 8));
	//}}}
	// GCC optimizes sizeof == 2, 4, and unsigned 8 as expected
	else if constexpr (sizeof(_Up) == 8 && is_signed_v<_Up>) //{{{
	  {
	    if (__y > 32)
	      return (__intrin_bitcast<_V>(__vector_bitcast<int>(__ix) >> 32)
		      & _Up(0xffff'ffff'0000'0000ull))
		     | __vector_bitcast<_Up>(
		       __vector_bitcast<int>(__vector_bitcast<_ULLong>(__ix)
					     >> 32)
		       >> (__y - 32));
	    else
	      return __intrin_bitcast<_V>(__vector_bitcast<_ULLong>(__ix)
					  >> __y)
		     | __vector_bitcast<_Up>(
		       __vector_bitcast<int>(__ix & -0x8000'0000'0000'0000ll)
		       >> __y);
	  }
	//}}}
	else
	  return __x >> __y;
      }

    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      inline _GLIBCXX_CONST static typename _TVT::type
      _S_bit_shift_right(_Tp __xx, typename _TVT::type __y)
      {
	using _V = typename _TVT::type;
	using _Up = typename _TVT::value_type;
	_V __x = __xx;
	[[maybe_unused]] const auto __ix = __to_intrin(__x);
	[[maybe_unused]] const auto __iy = __to_intrin(__y);
	if (__builtin_is_constant_evaluated()
	    || (__builtin_constant_p(__x) && __builtin_constant_p(__y)))
	  return __x >> __y;
	else if constexpr (sizeof(_Up) == 1) //{{{
	  {
	    if constexpr (sizeof(__x) <= 8 && __have_avx512bw_vl)
	      return __intrin_bitcast<_V>(_mm_cvtepi16_epi8(
		is_signed_v<_Up> ? _mm_srav_epi16(_mm_cvtepi8_epi16(__ix),
						  _mm_cvtepi8_epi16(__iy))
				 : _mm_srlv_epi16(_mm_cvtepu8_epi16(__ix),
						  _mm_cvtepu8_epi16(__iy))));
	    if constexpr (sizeof(__x) == 16 && __have_avx512bw_vl)
	      return __intrin_bitcast<_V>(_mm256_cvtepi16_epi8(
		is_signed_v<_Up>
		  ? _mm256_srav_epi16(_mm256_cvtepi8_epi16(__ix),
				      _mm256_cvtepi8_epi16(__iy))
		  : _mm256_srlv_epi16(_mm256_cvtepu8_epi16(__ix),
				      _mm256_cvtepu8_epi16(__iy))));
	    else if constexpr (sizeof(__x) == 32 && __have_avx512bw)
	      return __vector_bitcast<_Up>(_mm512_cvtepi16_epi8(
		is_signed_v<_Up>
		  ? _mm512_srav_epi16(_mm512_cvtepi8_epi16(__ix),
				      _mm512_cvtepi8_epi16(__iy))
		  : _mm512_srlv_epi16(_mm512_cvtepu8_epi16(__ix),
				      _mm512_cvtepu8_epi16(__iy))));
	    else if constexpr (sizeof(__x) == 64 && is_signed_v<_Up>)
	      return __vector_bitcast<_Up>(_mm512_mask_mov_epi8(
		_mm512_srav_epi16(__ix, _mm512_srli_epi16(__iy, 8)),
		0x5555'5555'5555'5555ull,
		_mm512_srav_epi16(
		  _mm512_slli_epi16(__ix, 8),
		  _mm512_maskz_add_epi8(0x5555'5555'5555'5555ull, __iy,
					_mm512_set1_epi16(8)))));
	    else if constexpr (sizeof(__x) == 64 && is_unsigned_v<_Up>)
	      return __vector_bitcast<_Up>(_mm512_mask_mov_epi8(
		_mm512_srlv_epi16(__ix, _mm512_srli_epi16(__iy, 8)),
		0x5555'5555'5555'5555ull,
		_mm512_srlv_epi16(
		  _mm512_maskz_mov_epi8(0x5555'5555'5555'5555ull, __ix),
		  _mm512_maskz_mov_epi8(0x5555'5555'5555'5555ull, __iy))));
	    /* This has better throughput but higher latency than the impl below
	    else if constexpr (__have_avx2 && sizeof(__x) == 16 &&
			       is_unsigned_v<_Up>)
	      {
		const auto __shorts = __to_intrin(_S_bit_shift_right(
		  __vector_bitcast<_UShort>(_mm256_cvtepu8_epi16(__ix)),
		  __vector_bitcast<_UShort>(_mm256_cvtepu8_epi16(__iy))));
		return __vector_bitcast<_Up>(
		  _mm_packus_epi16(__lo128(__shorts), __hi128(__shorts)));
	      }
	      */
	    else if constexpr (__have_avx2 && sizeof(__x) > 8)
	      // the following uses vpsr[al]vd, which requires AVX2
	      if constexpr (is_signed_v<_Up>)
		{
		  const auto r3 = __vector_bitcast<_UInt>(
				    (__vector_bitcast<int>(__x)
				     >> (__vector_bitcast<_UInt>(__y) >> 24)))
				  & 0xff000000u;
		  const auto r2
		    = __vector_bitcast<_UInt>(
			((__vector_bitcast<int>(__x) << 8)
			 >> ((__vector_bitcast<_UInt>(__y) << 8) >> 24)))
		      & 0xff000000u;
		  const auto r1
		    = __vector_bitcast<_UInt>(
			((__vector_bitcast<int>(__x) << 16)
			 >> ((__vector_bitcast<_UInt>(__y) << 16) >> 24)))
		      & 0xff000000u;
		  const auto r0 = __vector_bitcast<_UInt>(
		    (__vector_bitcast<int>(__x) << 24)
		    >> ((__vector_bitcast<_UInt>(__y) << 24) >> 24));
		  return __vector_bitcast<_Up>(r3 | (r2 >> 8) | (r1 >> 16)
					       | (r0 >> 24));
		}
	      else
		{
		  const auto r3 = (__vector_bitcast<_UInt>(__x)
				   >> (__vector_bitcast<_UInt>(__y) >> 24))
				  & 0xff000000u;
		  const auto r2
		    = ((__vector_bitcast<_UInt>(__x) << 8)
		       >> ((__vector_bitcast<_UInt>(__y) << 8) >> 24))
		      & 0xff000000u;
		  const auto r1
		    = ((__vector_bitcast<_UInt>(__x) << 16)
		       >> ((__vector_bitcast<_UInt>(__y) << 16) >> 24))
		      & 0xff000000u;
		  const auto r0
		    = (__vector_bitcast<_UInt>(__x) << 24)
		      >> ((__vector_bitcast<_UInt>(__y) << 24) >> 24);
		  return __vector_bitcast<_Up>(r3 | (r2 >> 8) | (r1 >> 16)
					       | (r0 >> 24));
		}
	    else if constexpr (__have_sse4_1
			       && is_unsigned_v<_Up> && sizeof(__x) > 2)
	      {
		auto __x128 = __vector_bitcast<_Up>(__ix);
		auto __mask
		  = __vector_bitcast<_Up>(__vector_bitcast<_UShort>(__iy) << 5);
		auto __x4 = __vector_bitcast<_Up>(
		  (__vector_bitcast<_UShort>(__x128) >> 4) & _UShort(0xff0f));
		__x128 = __vector_bitcast<_Up>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__x128), __to_intrin(__x4)));
		__mask += __mask;
		auto __x2 = __vector_bitcast<_Up>(
		  (__vector_bitcast<_UShort>(__x128) >> 2) & _UShort(0xff3f));
		__x128 = __vector_bitcast<_Up>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__x128), __to_intrin(__x2)));
		__mask += __mask;
		auto __x1 = __vector_bitcast<_Up>(
		  (__vector_bitcast<_UShort>(__x128) >> 1) & _UShort(0xff7f));
		__x128 = __vector_bitcast<_Up>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__x128), __to_intrin(__x1)));
		return __intrin_bitcast<_V>(
		  __x128
		  & ((__vector_bitcast<_Up>(__iy) & char(0xf8))
		     == 0)); // y > 7 nulls the result
	      }
	    else if constexpr (__have_sse4_1
			       && is_signed_v<_Up> && sizeof(__x) > 2)
	      {
		auto __mask = __vector_bitcast<_UChar>(
		  __vector_bitcast<_UShort>(__iy) << 5);
		auto __maskl = [&]() {
		  return __to_intrin(__vector_bitcast<_UShort>(__mask) << 8);
		};
		auto __xh = __vector_bitcast<short>(__ix);
		auto __xl = __vector_bitcast<short>(__ix) << 8;
		auto __xh4 = __xh >> 4;
		auto __xl4 = __xl >> 4;
		__xh = __vector_bitcast<short>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__xh), __to_intrin(__xh4)));
		__xl = __vector_bitcast<short>(
		  _CommonImplX86::_S_blend_intrin(__maskl(), __to_intrin(__xl),
						  __to_intrin(__xl4)));
		__mask += __mask;
		auto __xh2 = __xh >> 2;
		auto __xl2 = __xl >> 2;
		__xh = __vector_bitcast<short>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__xh), __to_intrin(__xh2)));
		__xl = __vector_bitcast<short>(
		  _CommonImplX86::_S_blend_intrin(__maskl(), __to_intrin(__xl),
						  __to_intrin(__xl2)));
		__mask += __mask;
		auto __xh1 = __xh >> 1;
		auto __xl1 = __xl >> 1;
		__xh = __vector_bitcast<short>(_CommonImplX86::_S_blend_intrin(
		  __to_intrin(__mask), __to_intrin(__xh), __to_intrin(__xh1)));
		__xl = __vector_bitcast<short>(
		  _CommonImplX86::_S_blend_intrin(__maskl(), __to_intrin(__xl),
						  __to_intrin(__xl1)));
		return __intrin_bitcast<_V>(
		  (__vector_bitcast<_Up>((__xh & short(0xff00)))
		   | __vector_bitcast<_Up>(__vector_bitcast<_UShort>(__xl)
					   >> 8))
		  & ((__vector_bitcast<_Up>(__iy) & char(0xf8))
		     == 0)); // y > 7 nulls the result
	      }
	    else if constexpr (is_unsigned_v<_Up> && sizeof(__x) > 2) // SSE2
	      {
		auto __mask
		  = __vector_bitcast<_Up>(__vector_bitcast<_UShort>(__y) << 5);
		auto __x4 = __vector_bitcast<_Up>(
		  (__vector_bitcast<_UShort>(__x) >> 4) & _UShort(0xff0f));
		__x = __mask > 0x7f ? __x4 : __x;
		__mask += __mask;
		auto __x2 = __vector_bitcast<_Up>(
		  (__vector_bitcast<_UShort>(__x) >> 2) & _UShort(0xff3f));
		__x = __mask > 0x7f ? __x2 : __x;
		__mask += __mask;
		auto __x1 = __vector_bitcast<_Up>(
		  (__vector_bitcast<_UShort>(__x) >> 1) & _UShort(0xff7f));
		__x = __mask > 0x7f ? __x1 : __x;
		return __x
		       & ((__y & char(0xf8)) == 0); // y > 7 nulls the result
	      }
	    else if constexpr (sizeof(__x) > 2) // signed SSE2
	      {
		static_assert(is_signed_v<_Up>);
		auto __maskh = __vector_bitcast<_UShort>(__y) << 5;
		auto __maskl = __vector_bitcast<_UShort>(__y) << (5 + 8);
		auto __xh = __vector_bitcast<short>(__x);
		auto __xl = __vector_bitcast<short>(__x) << 8;
		auto __xh4 = __xh >> 4;
		auto __xl4 = __xl >> 4;
		__xh = __maskh > 0x7fff ? __xh4 : __xh;
		__xl = __maskl > 0x7fff ? __xl4 : __xl;
		__maskh += __maskh;
		__maskl += __maskl;
		auto __xh2 = __xh >> 2;
		auto __xl2 = __xl >> 2;
		__xh = __maskh > 0x7fff ? __xh2 : __xh;
		__xl = __maskl > 0x7fff ? __xl2 : __xl;
		__maskh += __maskh;
		__maskl += __maskl;
		auto __xh1 = __xh >> 1;
		auto __xl1 = __xl >> 1;
		__xh = __maskh > 0x7fff ? __xh1 : __xh;
		__xl = __maskl > 0x7fff ? __xl1 : __xl;
		__x = __vector_bitcast<_Up>((__xh & short(0xff00)))
		      | __vector_bitcast<_Up>(__vector_bitcast<_UShort>(__xl)
					      >> 8);
		return __x
		       & ((__y & char(0xf8)) == 0); // y > 7 nulls the result
	      }
	    else
	      return __x >> __y;
	  }                                                      //}}}
	else if constexpr (sizeof(_Up) == 2 && sizeof(__x) >= 4) //{{{
	  {
	    [[maybe_unused]] auto __blend_0xaa = [](auto __a, auto __b) {
	      if constexpr (sizeof(__a) == 16)
		return _mm_blend_epi16(__to_intrin(__a), __to_intrin(__b),
				       0xaa);
	      else if constexpr (sizeof(__a) == 32)
		return _mm256_blend_epi16(__to_intrin(__a), __to_intrin(__b),
					  0xaa);
	      else if constexpr (sizeof(__a) == 64)
		return _mm512_mask_blend_epi16(0xaaaa'aaaaU, __to_intrin(__a),
					       __to_intrin(__b));
	      else
		__assert_unreachable<decltype(__a)>();
	    };
	    if constexpr (__have_avx512bw_vl && sizeof(_Tp) <= 16)
	      return __intrin_bitcast<_V>(is_signed_v<_Up>
					    ? _mm_srav_epi16(__ix, __iy)
					    : _mm_srlv_epi16(__ix, __iy));
	    else if constexpr (__have_avx512bw_vl && sizeof(_Tp) == 32)
	      return __vector_bitcast<_Up>(is_signed_v<_Up>
					     ? _mm256_srav_epi16(__ix, __iy)
					     : _mm256_srlv_epi16(__ix, __iy));
	    else if constexpr (__have_avx512bw && sizeof(_Tp) == 64)
	      return __vector_bitcast<_Up>(is_signed_v<_Up>
					     ? _mm512_srav_epi16(__ix, __iy)
					     : _mm512_srlv_epi16(__ix, __iy));
	    else if constexpr (__have_avx2 && is_signed_v<_Up>)
	      return __intrin_bitcast<_V>(
		__blend_0xaa(((__vector_bitcast<int>(__ix) << 16)
			      >> (__vector_bitcast<int>(__iy) & 0xffffu))
			       >> 16,
			     __vector_bitcast<int>(__ix)
			       >> (__vector_bitcast<int>(__iy) >> 16)));
	    else if constexpr (__have_avx2 && is_unsigned_v<_Up>)
	      return __intrin_bitcast<_V>(
		__blend_0xaa((__vector_bitcast<_UInt>(__ix) & 0xffffu)
			       >> (__vector_bitcast<_UInt>(__iy) & 0xffffu),
			     __vector_bitcast<_UInt>(__ix)
			       >> (__vector_bitcast<_UInt>(__iy) >> 16)));
	    else if constexpr (__have_sse4_1)
	      {
		auto __mask = __vector_bitcast<_UShort>(__iy);
		auto __x128 = __vector_bitcast<_Up>(__ix);
		//__mask *= 0x0808;
		__mask = (__mask << 3) | (__mask << 11);
		// do __x128 = 0 where __y[4] is set
		__x128 = __vector_bitcast<_Up>(
		  _mm_blendv_epi8(__to_intrin(__x128), __m128i(),
				  __to_intrin(__mask)));
		// do __x128 =>> 8 where __y[3] is set
		__x128 = __vector_bitcast<_Up>(
		  _mm_blendv_epi8(__to_intrin(__x128), __to_intrin(__x128 >> 8),
				  __to_intrin(__mask += __mask)));
		// do __x128 =>> 4 where __y[2] is set
		__x128 = __vector_bitcast<_Up>(
		  _mm_blendv_epi8(__to_intrin(__x128), __to_intrin(__x128 >> 4),
				  __to_intrin(__mask += __mask)));
		// do __x128 =>> 2 where __y[1] is set
		__x128 = __vector_bitcast<_Up>(
		  _mm_blendv_epi8(__to_intrin(__x128), __to_intrin(__x128 >> 2),
				  __to_intrin(__mask += __mask)));
		// do __x128 =>> 1 where __y[0] is set
		return __intrin_bitcast<_V>(
		  _mm_blendv_epi8(__to_intrin(__x128), __to_intrin(__x128 >> 1),
				  __to_intrin(__mask + __mask)));
	      }
	    else
	      {
		auto __k = __vector_bitcast<_UShort>(__iy) << 11;
		auto __x128 = __vector_bitcast<_Up>(__ix);
		auto __mask = [](__vector_type16_t<_UShort> __kk) {
		  return __vector_bitcast<short>(__kk) < 0;
		};
		// do __x128 = 0 where __y[4] is set
		__x128 = __mask(__k) ? decltype(__x128)() : __x128;
		// do __x128 =>> 8 where __y[3] is set
		__x128 = __mask(__k += __k) ? __x128 >> 8 : __x128;
		// do __x128 =>> 4 where __y[2] is set
		__x128 = __mask(__k += __k) ? __x128 >> 4 : __x128;
		// do __x128 =>> 2 where __y[1] is set
		__x128 = __mask(__k += __k) ? __x128 >> 2 : __x128;
		// do __x128 =>> 1 where __y[0] is set
		return __intrin_bitcast<_V>(__mask(__k + __k) ? __x128 >> 1
							      : __x128);
	      }
	  }                                                  //}}}
	else if constexpr (sizeof(_Up) == 4 && !__have_avx2) //{{{
	  {
	    if constexpr (is_unsigned_v<_Up>)
	      {
		// x >> y == x * 2^-y == (x * 2^(31-y)) >> 31
		const __m128 __factor_f = reinterpret_cast<__m128>(
		  0x4f00'0000u - (__vector_bitcast<unsigned, 4>(__y) << 23));
		const __m128i __factor
		  = __builtin_constant_p(__factor_f)
		      ? __to_intrin(
			__make_vector<unsigned>(__factor_f[0], __factor_f[1],
						__factor_f[2], __factor_f[3]))
		      : _mm_cvttps_epi32(__factor_f);
		const auto __r02
		  = _mm_srli_epi64(_mm_mul_epu32(__ix, __factor), 31);
		const auto __r13 = _mm_mul_epu32(_mm_srli_si128(__ix, 4),
						 _mm_srli_si128(__factor, 4));
		if constexpr (__have_sse4_1)
		  return __intrin_bitcast<_V>(
		    _mm_blend_epi16(_mm_slli_epi64(__r13, 1), __r02, 0x33));
		else
		  return __intrin_bitcast<_V>(
		    __r02 | _mm_slli_si128(_mm_srli_epi64(__r13, 31), 4));
	      }
	    else
	      {
		auto __shift = [](auto __a, auto __b) {
		  if constexpr (is_signed_v<_Up>)
		    return _mm_sra_epi32(__a, __b);
		  else
		    return _mm_srl_epi32(__a, __b);
		};
		const auto __r0
		  = __shift(__ix, _mm_unpacklo_epi32(__iy, __m128i()));
		const auto __r1 = __shift(__ix, _mm_srli_epi64(__iy, 32));
		const auto __r2
		  = __shift(__ix, _mm_unpackhi_epi32(__iy, __m128i()));
		const auto __r3 = __shift(__ix, _mm_srli_si128(__iy, 12));
		if constexpr (__have_sse4_1)
		  return __intrin_bitcast<_V>(
		    _mm_blend_epi16(_mm_blend_epi16(__r1, __r0, 0x3),
				    _mm_blend_epi16(__r3, __r2, 0x30), 0xf0));
		else
		  return __intrin_bitcast<_V>(_mm_unpacklo_epi64(
		    _mm_unpacklo_epi32(__r0, _mm_srli_si128(__r1, 4)),
		    _mm_unpackhi_epi32(__r2, _mm_srli_si128(__r3, 4))));
	      }
	  } //}}}
	else
	  return __x >> __y;
      }
#endif // _GLIBCXX_SIMD_NO_SHIFT_OPT

    // }}}
    // compares {{{
    // _S_equal_to {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_equal_to(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	if constexpr (__is_avx512_abi<_Abi>()) // {{{
	  {
	    if (__builtin_is_constant_evaluated()
		|| (__x._M_is_constprop() && __y._M_is_constprop()))
	      return _MaskImpl::_S_to_bits(
		__as_wrapper<_Np>(__x._M_data == __y._M_data));

	    constexpr auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    [[maybe_unused]] const auto __xi = __to_intrin(__x);
	    [[maybe_unused]] const auto __yi = __to_intrin(__y);
	    if constexpr (is_floating_point_v<_Tp>)
	      {
		if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
		  return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_EQ_OQ);
		else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
		  return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_EQ_OQ);
		else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
		  return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_EQ_OQ);
		else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
		  return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_EQ_OQ);
		else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
		  return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_EQ_OQ);
		else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
		  return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_EQ_OQ);
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_mask_cmpeq_epi64_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_mask_cmpeq_epi32_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 2)
	      return _mm512_mask_cmpeq_epi16_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 1)
	      return _mm512_mask_cmpeq_epi8_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_mask_cmpeq_epi64_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_mask_cmpeq_epi32_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 2)
	      return _mm256_mask_cmpeq_epi16_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 1)
	      return _mm256_mask_cmpeq_epi8_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_mask_cmpeq_epi64_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_mask_cmpeq_epi32_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 2)
	      return _mm_mask_cmpeq_epi16_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 1)
	      return _mm_mask_cmpeq_epi8_mask(__k1, __xi, __yi);
	    else
	      __assert_unreachable<_Tp>();
	  } // }}}
	else if (__builtin_is_constant_evaluated())
	  return _Base::_S_equal_to(__x, __y);
	else if constexpr (sizeof(__x) == 8) // {{{
	  {
	    const auto __r128 = __vector_bitcast<_Tp, 16 / sizeof(_Tp)>(__x)
				== __vector_bitcast<_Tp, 16 / sizeof(_Tp)>(__y);
	    _MaskMember<_Tp> __r64;
	    __builtin_memcpy(&__r64._M_data, &__r128, sizeof(__r64));
	    return __r64;
	  } // }}}
	else
	  return _Base::_S_equal_to(__x, __y);
      }

    // }}}
    // _S_not_equal_to {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_not_equal_to(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	if constexpr (__is_avx512_abi<_Abi>()) // {{{
	  {
	    if (__builtin_is_constant_evaluated()
		|| (__x._M_is_constprop() && __y._M_is_constprop()))
	      return _MaskImpl::_S_to_bits(
		__as_wrapper<_Np>(__x._M_data != __y._M_data));

	    constexpr auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    [[maybe_unused]] const auto __xi = __to_intrin(__x);
	    [[maybe_unused]] const auto __yi = __to_intrin(__y);
	    if constexpr (is_floating_point_v<_Tp>)
	      {
		if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
		  return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_NEQ_UQ);
		else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
		  return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_NEQ_UQ);
		else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
		  return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_NEQ_UQ);
		else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
		  return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_NEQ_UQ);
		else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
		  return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_NEQ_UQ);
		else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
		  return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_NEQ_UQ);
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return ~_mm512_mask_cmpeq_epi64_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return ~_mm512_mask_cmpeq_epi32_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 2)
	      return ~_mm512_mask_cmpeq_epi16_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 1)
	      return ~_mm512_mask_cmpeq_epi8_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return ~_mm256_mask_cmpeq_epi64_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return ~_mm256_mask_cmpeq_epi32_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 2)
	      return ~_mm256_mask_cmpeq_epi16_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 1)
	      return ~_mm256_mask_cmpeq_epi8_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return ~_mm_mask_cmpeq_epi64_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return ~_mm_mask_cmpeq_epi32_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 2)
	      return ~_mm_mask_cmpeq_epi16_mask(__k1, __xi, __yi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 1)
	      return ~_mm_mask_cmpeq_epi8_mask(__k1, __xi, __yi);
	    else
	      __assert_unreachable<_Tp>();
	  }                                                   // }}}
	else if constexpr (!__builtin_is_constant_evaluated() // {{{
			   && sizeof(__x) == 8)
	  {
	    const auto __r128 = __vector_bitcast<_Tp, 16 / sizeof(_Tp)>(__x)
				!= __vector_bitcast<_Tp, 16 / sizeof(_Tp)>(__y);
	    _MaskMember<_Tp> __r64;
	    __builtin_memcpy(&__r64._M_data, &__r128, sizeof(__r64));
	    return __r64;
	  } // }}}
	else
	  return _Base::_S_not_equal_to(__x, __y);
      }

    // }}}
    // _S_less {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_less(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	if constexpr (__is_avx512_abi<_Abi>()) // {{{
	  {
	    if (__builtin_is_constant_evaluated()
		|| (__x._M_is_constprop() && __y._M_is_constprop()))
	      return _MaskImpl::_S_to_bits(
		__as_wrapper<_Np>(__x._M_data < __y._M_data));

	    constexpr auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    [[maybe_unused]] const auto __xi = __to_intrin(__x);
	    [[maybe_unused]] const auto __yi = __to_intrin(__y);
	    if constexpr (sizeof(__xi) == 64)
	      {
		if constexpr (is_same_v<_Tp, float>)
		  return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LT_OS);
		else if constexpr (is_same_v<_Tp, double>)
		  return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LT_OS);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm512_mask_cmplt_epi8_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm512_mask_cmplt_epi16_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm512_mask_cmplt_epi32_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm512_mask_cmplt_epi64_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm512_mask_cmplt_epu8_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm512_mask_cmplt_epu16_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm512_mask_cmplt_epu32_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm512_mask_cmplt_epu64_mask(__k1, __xi, __yi);
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr (sizeof(__xi) == 32)
	      {
		if constexpr (is_same_v<_Tp, float>)
		  return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LT_OS);
		else if constexpr (is_same_v<_Tp, double>)
		  return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LT_OS);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm256_mask_cmplt_epi8_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm256_mask_cmplt_epi16_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm256_mask_cmplt_epi32_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm256_mask_cmplt_epi64_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm256_mask_cmplt_epu8_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm256_mask_cmplt_epu16_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm256_mask_cmplt_epu32_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm256_mask_cmplt_epu64_mask(__k1, __xi, __yi);
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr (sizeof(__xi) == 16)
	      {
		if constexpr (is_same_v<_Tp, float>)
		  return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LT_OS);
		else if constexpr (is_same_v<_Tp, double>)
		  return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LT_OS);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm_mask_cmplt_epi8_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm_mask_cmplt_epi16_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm_mask_cmplt_epi32_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm_mask_cmplt_epi64_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm_mask_cmplt_epu8_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm_mask_cmplt_epu16_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm_mask_cmplt_epu32_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm_mask_cmplt_epu64_mask(__k1, __xi, __yi);
		else
		  __assert_unreachable<_Tp>();
	      }
	    else
	      __assert_unreachable<_Tp>();
	  }                                                   // }}}
	else if constexpr (!__builtin_is_constant_evaluated() // {{{
			   && sizeof(__x) == 8)
	  {
	    const auto __r128 = __vector_bitcast<_Tp, 16 / sizeof(_Tp)>(__x)
				< __vector_bitcast<_Tp, 16 / sizeof(_Tp)>(__y);
	    _MaskMember<_Tp> __r64;
	    __builtin_memcpy(&__r64._M_data, &__r128, sizeof(__r64));
	    return __r64;
	  } // }}}
	else
	  return _Base::_S_less(__x, __y);
      }

    // }}}
    // _S_less_equal {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_less_equal(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	if constexpr (__is_avx512_abi<_Abi>()) // {{{
	  {
	    if (__builtin_is_constant_evaluated()
		|| (__x._M_is_constprop() && __y._M_is_constprop()))
	      return _MaskImpl::_S_to_bits(
		__as_wrapper<_Np>(__x._M_data <= __y._M_data));

	    constexpr auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    [[maybe_unused]] const auto __xi = __to_intrin(__x);
	    [[maybe_unused]] const auto __yi = __to_intrin(__y);
	    if constexpr (sizeof(__xi) == 64)
	      {
		if constexpr (is_same_v<_Tp, float>)
		  return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LE_OS);
		else if constexpr (is_same_v<_Tp, double>)
		  return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LE_OS);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm512_mask_cmple_epi8_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm512_mask_cmple_epi16_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm512_mask_cmple_epi32_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm512_mask_cmple_epi64_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm512_mask_cmple_epu8_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm512_mask_cmple_epu16_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm512_mask_cmple_epu32_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm512_mask_cmple_epu64_mask(__k1, __xi, __yi);
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr (sizeof(__xi) == 32)
	      {
		if constexpr (is_same_v<_Tp, float>)
		  return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LE_OS);
		else if constexpr (is_same_v<_Tp, double>)
		  return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LE_OS);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm256_mask_cmple_epi8_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm256_mask_cmple_epi16_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm256_mask_cmple_epi32_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm256_mask_cmple_epi64_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm256_mask_cmple_epu8_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm256_mask_cmple_epu16_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm256_mask_cmple_epu32_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm256_mask_cmple_epu64_mask(__k1, __xi, __yi);
		else
		  __assert_unreachable<_Tp>();
	      }
	    else if constexpr (sizeof(__xi) == 16)
	      {
		if constexpr (is_same_v<_Tp, float>)
		  return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LE_OS);
		else if constexpr (is_same_v<_Tp, double>)
		  return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LE_OS);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm_mask_cmple_epi8_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm_mask_cmple_epi16_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm_mask_cmple_epi32_mask(__k1, __xi, __yi);
		else if constexpr (is_signed_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm_mask_cmple_epi64_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 1)
		  return _mm_mask_cmple_epu8_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 2)
		  return _mm_mask_cmple_epu16_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 4)
		  return _mm_mask_cmple_epu32_mask(__k1, __xi, __yi);
		else if constexpr (is_unsigned_v<_Tp> && sizeof(_Tp) == 8)
		  return _mm_mask_cmple_epu64_mask(__k1, __xi, __yi);
		else
		  __assert_unreachable<_Tp>();
	      }
	    else
	      __assert_unreachable<_Tp>();
	  }                                                   // }}}
	else if constexpr (!__builtin_is_constant_evaluated() // {{{
			   && sizeof(__x) == 8)
	  {
	    const auto __r128 = __vector_bitcast<_Tp, 16 / sizeof(_Tp)>(__x)
				<= __vector_bitcast<_Tp, 16 / sizeof(_Tp)>(__y);
	    _MaskMember<_Tp> __r64;
	    __builtin_memcpy(&__r64._M_data, &__r128, sizeof(__r64));
	    return __r64;
	  } // }}}
	else
	  return _Base::_S_less_equal(__x, __y);
      }

    // }}} }}}
    // negation {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_negate(_SimdWrapper<_Tp, _Np> __x) noexcept
      {
	if constexpr (__is_avx512_abi<_Abi>())
	  return _S_equal_to(__x, _SimdWrapper<_Tp, _Np>());
	else
	  return _Base::_S_negate(__x);
      }

    // }}}
    // math {{{
    using _Base::_S_abs;

    // _S_sqrt {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_sqrt(_SimdWrapper<_Tp, _Np> __x)
      {
	if constexpr (__is_sse_ps<_Tp, _Np>())
	  return __auto_bitcast(_mm_sqrt_ps(__to_intrin(__x)));
	else if constexpr (__is_sse_pd<_Tp, _Np>())
	  return _mm_sqrt_pd(__x);
	else if constexpr (__is_avx_ps<_Tp, _Np>())
	  return _mm256_sqrt_ps(__x);
	else if constexpr (__is_avx_pd<_Tp, _Np>())
	  return _mm256_sqrt_pd(__x);
	else if constexpr (__is_avx512_ps<_Tp, _Np>())
	  return _mm512_sqrt_ps(__x);
	else if constexpr (__is_avx512_pd<_Tp, _Np>())
	  return _mm512_sqrt_pd(__x);
	else
	  __assert_unreachable<_Tp>();
      }

    // }}}
    // _S_ldexp {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_ldexp(_SimdWrapper<_Tp, _Np> __x,
	       __fixed_size_storage_t<int, _Np> __exp)
      {
	if constexpr (sizeof(__x) == 64 || __have_avx512vl)
	  {
	    const auto __xi = __to_intrin(__x);
	    constexpr _SimdConverter<int, simd_abi::fixed_size<_Np>, _Tp, _Abi>
	      __cvt;
	    const auto __expi = __to_intrin(__cvt(__exp));
	    using _Up = __bool_storage_member_type_t<_Np>;
	    constexpr _Up __k1 = _Np < sizeof(_Up) * __CHAR_BIT__ ? _Up((1ULL << _Np) - 1) : ~_Up();
	    if constexpr (sizeof(__xi) == 16)
	      {
		if constexpr (sizeof(_Tp) == 8)
		  return _mm_maskz_scalef_pd(__k1, __xi, __expi);
		else
		  return _mm_maskz_scalef_ps(__k1, __xi, __expi);
	      }
	    else if constexpr (sizeof(__xi) == 32)
	      {
		if constexpr (sizeof(_Tp) == 8)
		  return _mm256_maskz_scalef_pd(__k1, __xi, __expi);
		else
		  return _mm256_maskz_scalef_ps(__k1, __xi, __expi);
	      }
	    else
	      {
		static_assert(sizeof(__xi) == 64);
		if constexpr (sizeof(_Tp) == 8)
		  return _mm512_maskz_scalef_pd(__k1, __xi, __expi);
		else
		  return _mm512_maskz_scalef_ps(__k1, __xi, __expi);
	      }
	  }
	else
	  return _Base::_S_ldexp(__x, __exp);
      }

    // }}}
    // _S_trunc {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_trunc(_SimdWrapper<_Tp, _Np> __x)
      {
	if constexpr (__is_avx512_ps<_Tp, _Np>())
	  return _mm512_roundscale_ps(__x, 0x0b);
	else if constexpr (__is_avx512_pd<_Tp, _Np>())
	  return _mm512_roundscale_pd(__x, 0x0b);
	else if constexpr (__is_avx_ps<_Tp, _Np>())
	  return _mm256_round_ps(__x, 0xb);
	else if constexpr (__is_avx_pd<_Tp, _Np>())
	  return _mm256_round_pd(__x, 0xb);
	else if constexpr (__have_sse4_1 && __is_sse_ps<_Tp, _Np>())
	  return __auto_bitcast(_mm_round_ps(__to_intrin(__x), 0xb));
	else if constexpr (__have_sse4_1 && __is_sse_pd<_Tp, _Np>())
	  return _mm_round_pd(__x, 0xb);
	else if constexpr (__is_sse_ps<_Tp, _Np>())
	  {
	    auto __truncated
	      = _mm_cvtepi32_ps(_mm_cvttps_epi32(__to_intrin(__x)));
	    const auto __no_fractional_values
	      = __vector_bitcast<int>(__vector_bitcast<_UInt>(__to_intrin(__x))
				      & 0x7f800000u)
		< 0x4b000000; // the exponent is so large that no mantissa bits
			      // signify fractional values (0x3f8 + 23*8 =
			      // 0x4b0)
	    return __no_fractional_values ? __truncated : __to_intrin(__x);
	  }
	else
	  return _Base::_S_trunc(__x);
      }

    // }}}
    // _S_round {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_round(_SimdWrapper<_Tp, _Np> __x)
      {
	// Note that _MM_FROUND_TO_NEAREST_INT rounds ties to even, not away
	// from zero as required by std::round. Therefore this function is more
	// complicated.
	using _V = __vector_type_t<_Tp, _Np>;
	_V __truncated;
	if constexpr (__is_avx512_ps<_Tp, _Np>())
	  __truncated = _mm512_roundscale_ps(__x._M_data, 0x0b);
	else if constexpr (__is_avx512_pd<_Tp, _Np>())
	  __truncated = _mm512_roundscale_pd(__x._M_data, 0x0b);
	else if constexpr (__is_avx_ps<_Tp, _Np>())
	  __truncated = _mm256_round_ps(__x._M_data,
					_MM_FROUND_TO_ZERO | _MM_FROUND_NO_EXC);
	else if constexpr (__is_avx_pd<_Tp, _Np>())
	  __truncated = _mm256_round_pd(__x._M_data,
					_MM_FROUND_TO_ZERO | _MM_FROUND_NO_EXC);
	else if constexpr (__have_sse4_1 && __is_sse_ps<_Tp, _Np>())
	  __truncated = __auto_bitcast(
	    _mm_round_ps(__to_intrin(__x),
			 _MM_FROUND_TO_ZERO | _MM_FROUND_NO_EXC));
	else if constexpr (__have_sse4_1 && __is_sse_pd<_Tp, _Np>())
	  __truncated
	    = _mm_round_pd(__x._M_data, _MM_FROUND_TO_ZERO | _MM_FROUND_NO_EXC);
	else if constexpr (__is_sse_ps<_Tp, _Np>())
	  __truncated = __auto_bitcast(
	    _mm_cvtepi32_ps(_mm_cvttps_epi32(__to_intrin(__x))));
	else
	  return _Base::_S_round(__x);

	// x < 0 => truncated <= 0 && truncated >= x => x - truncated <= 0
	// x > 0 => truncated >= 0 && truncated <= x => x - truncated >= 0

	const _V __rounded
	  = __truncated
	    + (__and(_S_absmask<_V>, __x._M_data - __truncated) >= _Tp(.5)
		 ? __or(__and(_S_signmask<_V>, __x._M_data), _V() + 1)
		 : _V());
	if constexpr (__have_sse4_1)
	  return __rounded;
	else // adjust for missing range in cvttps_epi32
	  return __and(_S_absmask<_V>, __x._M_data) < 0x1p23f ? __rounded
							      : __x._M_data;
      }

    // }}}
    // _S_nearbyint {{{
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      _GLIBCXX_SIMD_INTRINSIC static _Tp _S_nearbyint(_Tp __x) noexcept
      {
	if constexpr (_TVT::template _S_is<float, 16>)
	  return _mm512_roundscale_ps(__x, 0x0c);
	else if constexpr (_TVT::template _S_is<double, 8>)
	  return _mm512_roundscale_pd(__x, 0x0c);
	else if constexpr (_TVT::template _S_is<float, 8>)
	  return _mm256_round_ps(__x,
				 _MM_FROUND_CUR_DIRECTION | _MM_FROUND_NO_EXC);
	else if constexpr (_TVT::template _S_is<double, 4>)
	  return _mm256_round_pd(__x,
				 _MM_FROUND_CUR_DIRECTION | _MM_FROUND_NO_EXC);
	else if constexpr (__have_sse4_1 && _TVT::template _S_is<float, 4>)
	  return _mm_round_ps(__x,
			      _MM_FROUND_CUR_DIRECTION | _MM_FROUND_NO_EXC);
	else if constexpr (__have_sse4_1 && _TVT::template _S_is<double, 2>)
	  return _mm_round_pd(__x,
			      _MM_FROUND_CUR_DIRECTION | _MM_FROUND_NO_EXC);
	else
	  return _Base::_S_nearbyint(__x);
      }

    // }}}
    // _S_rint {{{
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      _GLIBCXX_SIMD_INTRINSIC static _Tp _S_rint(_Tp __x) noexcept
      {
	if constexpr (_TVT::template _S_is<float, 16>)
	  return _mm512_roundscale_ps(__x, 0x04);
	else if constexpr (_TVT::template _S_is<double, 8>)
	  return _mm512_roundscale_pd(__x, 0x04);
	else if constexpr (_TVT::template _S_is<float, 8>)
	  return _mm256_round_ps(__x, _MM_FROUND_CUR_DIRECTION);
	else if constexpr (_TVT::template _S_is<double, 4>)
	  return _mm256_round_pd(__x, _MM_FROUND_CUR_DIRECTION);
	else if constexpr (__have_sse4_1 && _TVT::template _S_is<float, 4>)
	  return _mm_round_ps(__x, _MM_FROUND_CUR_DIRECTION);
	else if constexpr (__have_sse4_1 && _TVT::template _S_is<double, 2>)
	  return _mm_round_pd(__x, _MM_FROUND_CUR_DIRECTION);
	else
	  return _Base::_S_rint(__x);
      }

    // }}}
    // _S_floor {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_floor(_SimdWrapper<_Tp, _Np> __x)
      {
	if constexpr (__is_avx512_ps<_Tp, _Np>())
	  return _mm512_roundscale_ps(__x, 0x09);
	else if constexpr (__is_avx512_pd<_Tp, _Np>())
	  return _mm512_roundscale_pd(__x, 0x09);
	else if constexpr (__is_avx_ps<_Tp, _Np>())
	  return _mm256_round_ps(__x, 0x9);
	else if constexpr (__is_avx_pd<_Tp, _Np>())
	  return _mm256_round_pd(__x, 0x9);
	else if constexpr (__have_sse4_1 && __is_sse_ps<_Tp, _Np>())
	  return __auto_bitcast(_mm_round_ps(__to_intrin(__x), 0x9));
	else if constexpr (__have_sse4_1 && __is_sse_pd<_Tp, _Np>())
	  return _mm_round_pd(__x, 0x9);
	else
	  return _Base::_S_floor(__x);
      }

    // }}}
    // _S_ceil {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_ceil(_SimdWrapper<_Tp, _Np> __x)
      {
	if constexpr (__is_avx512_ps<_Tp, _Np>())
	  return _mm512_roundscale_ps(__x, 0x0a);
	else if constexpr (__is_avx512_pd<_Tp, _Np>())
	  return _mm512_roundscale_pd(__x, 0x0a);
	else if constexpr (__is_avx_ps<_Tp, _Np>())
	  return _mm256_round_ps(__x, 0xa);
	else if constexpr (__is_avx_pd<_Tp, _Np>())
	  return _mm256_round_pd(__x, 0xa);
	else if constexpr (__have_sse4_1 && __is_sse_ps<_Tp, _Np>())
	  return __auto_bitcast(_mm_round_ps(__to_intrin(__x), 0xa));
	else if constexpr (__have_sse4_1 && __is_sse_pd<_Tp, _Np>())
	  return _mm_round_pd(__x, 0xa);
	else
	  return _Base::_S_ceil(__x);
      }

    // }}}
    // _S_signbit {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
      _S_signbit(_SimdWrapper<_Tp, _Np> __x)
      {
	if constexpr (__is_avx512_abi<_Abi>() && __have_avx512dq)
	  {
	    if constexpr (sizeof(__x) == 64 && sizeof(_Tp) == 4)
	      return _mm512_movepi32_mask(
		__intrin_bitcast<__m512i>(__x._M_data));
	    else if constexpr (sizeof(__x) == 64 && sizeof(_Tp) == 8)
	      return _mm512_movepi64_mask(
		__intrin_bitcast<__m512i>(__x._M_data));
	    else if constexpr (sizeof(__x) == 32 && sizeof(_Tp) == 4)
	      return _mm256_movepi32_mask(
		__intrin_bitcast<__m256i>(__x._M_data));
	    else if constexpr (sizeof(__x) == 32 && sizeof(_Tp) == 8)
	      return _mm256_movepi64_mask(
		__intrin_bitcast<__m256i>(__x._M_data));
	    else if constexpr (sizeof(__x) <= 16 && sizeof(_Tp) == 4)
	      return _mm_movepi32_mask(__intrin_bitcast<__m128i>(__x._M_data));
	    else if constexpr (sizeof(__x) <= 16 && sizeof(_Tp) == 8)
	      return _mm_movepi64_mask(__intrin_bitcast<__m128i>(__x._M_data));
	  }
	else if constexpr (__is_avx512_abi<_Abi>())
	  {
	    const auto __xi = __to_intrin(__x);
	    [[maybe_unused]] constexpr auto __k1
	      = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_movemask_ps(__xi);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_movemask_pd(__xi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_movemask_ps(__xi);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_movemask_pd(__xi);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_mask_cmplt_epi32_mask(
		__k1, __intrin_bitcast<__m512i>(__xi), __m512i());
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_mask_cmplt_epi64_mask(
		__k1, __intrin_bitcast<__m512i>(__xi), __m512i());
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_signbit(__x);
	/*{
	  using _I = __int_for_sizeof_t<_Tp>;
	  if constexpr (sizeof(__x) == 64)
	    return _S_less(__vector_bitcast<_I>(__x), _I());
	  else
	    {
	      const auto __xx = __vector_bitcast<_I>(__x._M_data);
	      [[maybe_unused]] constexpr _I __signmask = __finite_min_v<_I>;
	      if constexpr ((sizeof(_Tp) == 4 &&
			     (__have_avx2 || sizeof(__x) == 16)) ||
			    __have_avx512vl)
		{
		  return __vector_bitcast<_Tp>(__xx >> __digits_v<_I>);
		}
	      else if constexpr ((__have_avx2 ||
				  (__have_ssse3 && sizeof(__x) == 16)))
		{
		  return __vector_bitcast<_Tp>((__xx & __signmask) ==
					       __signmask);
		}
	      else
		{ // SSE2/3 or AVX (w/o AVX2)
		  constexpr auto __one = __vector_broadcast<_Np, _Tp>(1);
		  return __vector_bitcast<_Tp>(
		    __vector_bitcast<_Tp>(
		      (__xx & __signmask) |
		      __vector_bitcast<_I>(__one)) // -1 or 1
		    != __one);
		}
	    }
	}*/
      }

    // }}}
    // _S_isnonzerovalue_mask {{{
    // (isnormal | is subnormal == !isinf & !isnan & !is zero)
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static auto _S_isnonzerovalue_mask(_Tp __x)
      {
	using _Traits = _VectorTraits<_Tp>;
	if constexpr (__have_avx512dq_vl)
	  {
	    if constexpr (_Traits::template _S_is<
			    float, 2> || _Traits::template _S_is<float, 4>)
	      return _knot_mask8(_mm_fpclass_ps_mask(__to_intrin(__x), 0x9f));
	    else if constexpr (_Traits::template _S_is<float, 8>)
	      return _knot_mask8(_mm256_fpclass_ps_mask(__x, 0x9f));
	    else if constexpr (_Traits::template _S_is<float, 16>)
	      return _knot_mask16(_mm512_fpclass_ps_mask(__x, 0x9f));
	    else if constexpr (_Traits::template _S_is<double, 2>)
	      return _knot_mask8(_mm_fpclass_pd_mask(__x, 0x9f));
	    else if constexpr (_Traits::template _S_is<double, 4>)
	      return _knot_mask8(_mm256_fpclass_pd_mask(__x, 0x9f));
	    else if constexpr (_Traits::template _S_is<double, 8>)
	      return _knot_mask8(_mm512_fpclass_pd_mask(__x, 0x9f));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  {
	    using _Up = typename _Traits::value_type;
	    constexpr size_t _Np = _Traits::_S_full_size;
	    const auto __a = __x * __infinity_v<_Up>; // NaN if __x == 0
	    const auto __b = __x * _Up();             // NaN if __x == inf
	    if constexpr (__have_avx512vl && __is_sse_ps<_Up, _Np>())
	      return _mm_cmp_ps_mask(__to_intrin(__a), __to_intrin(__b),
				     _CMP_ORD_Q);
	    else if constexpr (__have_avx512f && __is_sse_ps<_Up, _Np>())
	      return __mmask8(0xf
			      & _mm512_cmp_ps_mask(__auto_bitcast(__a),
						   __auto_bitcast(__b),
						   _CMP_ORD_Q));
	    else if constexpr (__have_avx512vl && __is_sse_pd<_Up, _Np>())
	      return _mm_cmp_pd_mask(__a, __b, _CMP_ORD_Q);
	    else if constexpr (__have_avx512f && __is_sse_pd<_Up, _Np>())
	      return __mmask8(0x3
			      & _mm512_cmp_pd_mask(__auto_bitcast(__a),
						   __auto_bitcast(__b),
						   _CMP_ORD_Q));
	    else if constexpr (__have_avx512vl && __is_avx_ps<_Up, _Np>())
	      return _mm256_cmp_ps_mask(__a, __b, _CMP_ORD_Q);
	    else if constexpr (__have_avx512f && __is_avx_ps<_Up, _Np>())
	      return __mmask8(_mm512_cmp_ps_mask(__auto_bitcast(__a),
						 __auto_bitcast(__b),
						 _CMP_ORD_Q));
	    else if constexpr (__have_avx512vl && __is_avx_pd<_Up, _Np>())
	      return _mm256_cmp_pd_mask(__a, __b, _CMP_ORD_Q);
	    else if constexpr (__have_avx512f && __is_avx_pd<_Up, _Np>())
	      return __mmask8(0xf
			      & _mm512_cmp_pd_mask(__auto_bitcast(__a),
						   __auto_bitcast(__b),
						   _CMP_ORD_Q));
	    else if constexpr (__is_avx512_ps<_Up, _Np>())
	      return _mm512_cmp_ps_mask(__a, __b, _CMP_ORD_Q);
	    else if constexpr (__is_avx512_pd<_Up, _Np>())
	      return _mm512_cmp_pd_mask(__a, __b, _CMP_ORD_Q);
	    else
	      __assert_unreachable<_Tp>();
	  }
      }

    // }}}
    // _S_isfinite {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
      _S_isfinite(_SimdWrapper<_Tp, _Np> __x)
      {
	static_assert(is_floating_point_v<_Tp>);
#if !__FINITE_MATH_ONLY__
	if constexpr (__is_avx512_abi<_Abi>() && __have_avx512dq)
	  {
	    const auto __xi = __to_intrin(__x);
	    constexpr auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return __k1 ^ _mm512_mask_fpclass_ps_mask(__k1, __xi, 0x99);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return __k1 ^ _mm512_mask_fpclass_pd_mask(__k1, __xi, 0x99);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return __k1 ^ _mm256_mask_fpclass_ps_mask(__k1, __xi, 0x99);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return __k1 ^ _mm256_mask_fpclass_pd_mask(__k1, __xi, 0x99);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return __k1 ^ _mm_mask_fpclass_ps_mask(__k1, __xi, 0x99);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return __k1 ^ _mm_mask_fpclass_pd_mask(__k1, __xi, 0x99);
	  }
	else if constexpr (__is_avx512_abi<_Abi>())
	  {
	    // if all exponent bits are set, __x is either inf or NaN
	    using _I = __int_for_sizeof_t<_Tp>;
	    const auto __inf = __vector_bitcast<_I>(
	      __vector_broadcast<_Np>(__infinity_v<_Tp>));
	    return _S_less<_I, _Np>(__vector_bitcast<_I>(__x) & __inf, __inf);
	  }
	else
#endif
	  return _Base::_S_isfinite(__x);
      }

    // }}}
    // _S_isinf {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
      _S_isinf(_SimdWrapper<_Tp, _Np> __x)
      {
#if !__FINITE_MATH_ONLY__
	if constexpr (__is_avx512_abi<_Abi>() && __have_avx512dq)
	  {
	    const auto __xi = __to_intrin(__x);
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_fpclass_ps_mask(__xi, 0x18);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_fpclass_pd_mask(__xi, 0x18);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_fpclass_ps_mask(__xi, 0x18);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_fpclass_pd_mask(__xi, 0x18);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_fpclass_ps_mask(__xi, 0x18);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_fpclass_pd_mask(__xi, 0x18);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_avx512dq_vl)
	  {
	    if constexpr (__is_sse_pd<_Tp, _Np>())
	      return _mm_movm_epi64(_mm_fpclass_pd_mask(__x, 0x18));
	    else if constexpr (__is_avx_pd<_Tp, _Np>())
	      return _mm256_movm_epi64(_mm256_fpclass_pd_mask(__x, 0x18));
	    else if constexpr (__is_sse_ps<_Tp, _Np>())
	      return _mm_movm_epi32(
		_mm_fpclass_ps_mask(__to_intrin(__x), 0x18));
	    else if constexpr (__is_avx_ps<_Tp, _Np>())
	      return _mm256_movm_epi32(_mm256_fpclass_ps_mask(__x, 0x18));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
#endif
	  return _Base::_S_isinf(__x);
      }

    // }}}
    // _S_isnormal {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
      _S_isnormal(_SimdWrapper<_Tp, _Np> __x)
      {
#if __FINITE_MATH_ONLY__
	[[maybe_unused]] constexpr int __mode = 0x26;
#else
      [[maybe_unused]] constexpr int __mode = 0xbf;
#endif
	if constexpr (__is_avx512_abi<_Abi>() && __have_avx512dq)
	  {
	    const auto __xi = __to_intrin(__x);
	    const auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return __k1 ^ _mm512_mask_fpclass_ps_mask(__k1, __xi, __mode);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return __k1 ^ _mm512_mask_fpclass_pd_mask(__k1, __xi, __mode);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return __k1 ^ _mm256_mask_fpclass_ps_mask(__k1, __xi, __mode);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return __k1 ^ _mm256_mask_fpclass_pd_mask(__k1, __xi, __mode);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return __k1 ^ _mm_mask_fpclass_ps_mask(__k1, __xi, __mode);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return __k1 ^ _mm_mask_fpclass_pd_mask(__k1, __xi, __mode);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_avx512dq)
	  {
	    if constexpr (__have_avx512vl && __is_sse_ps<_Tp, _Np>())
	      return _mm_movm_epi32(
		_knot_mask8(_mm_fpclass_ps_mask(__to_intrin(__x), __mode)));
	    else if constexpr (__have_avx512vl && __is_avx_ps<_Tp, _Np>())
	      return _mm256_movm_epi32(
		_knot_mask8(_mm256_fpclass_ps_mask(__x, __mode)));
	    else if constexpr (__is_avx512_ps<_Tp, _Np>())
	      return _knot_mask16(_mm512_fpclass_ps_mask(__x, __mode));
	    else if constexpr (__have_avx512vl && __is_sse_pd<_Tp, _Np>())
	      return _mm_movm_epi64(
		_knot_mask8(_mm_fpclass_pd_mask(__x, __mode)));
	    else if constexpr (__have_avx512vl && __is_avx_pd<_Tp, _Np>())
	      return _mm256_movm_epi64(
		_knot_mask8(_mm256_fpclass_pd_mask(__x, __mode)));
	    else if constexpr (__is_avx512_pd<_Tp, _Np>())
	      return _knot_mask8(_mm512_fpclass_pd_mask(__x, __mode));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__is_avx512_abi<_Abi>())
	  {
	    using _I = __int_for_sizeof_t<_Tp>;
	    const auto absn = __vector_bitcast<_I>(_S_abs(__x));
	    const auto minn = __vector_bitcast<_I>(
	      __vector_broadcast<_Np>(__norm_min_v<_Tp>));
#if __FINITE_MATH_ONLY__
	    return _S_less_equal<_I, _Np>(minn, absn);
#else
	  const auto infn
	    = __vector_bitcast<_I>(__vector_broadcast<_Np>(__infinity_v<_Tp>));
	  return __and(_S_less_equal<_I, _Np>(minn, absn),
		       _S_less<_I, _Np>(absn, infn));
#endif
	  }
	else
	  return _Base::_S_isnormal(__x);
      }

    // }}}
    // _S_isnan {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
      _S_isnan(_SimdWrapper<_Tp, _Np> __x)
      { return _S_isunordered(__x, __x); }

    // }}}
    // _S_isunordered {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
      _S_isunordered([[maybe_unused]] _SimdWrapper<_Tp, _Np> __x,
		     [[maybe_unused]] _SimdWrapper<_Tp, _Np> __y)
      {
#if __FINITE_MATH_ONLY__
	return {}; // false
#else
	const auto __xi = __to_intrin(__x);
	const auto __yi = __to_intrin(__y);
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    constexpr auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_UNORD_Q);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_UNORD_Q);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_UNORD_Q);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_UNORD_Q);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_UNORD_Q);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_UNORD_Q);
	  }
      else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	return __to_masktype(_mm256_cmp_ps(__xi, __yi, _CMP_UNORD_Q));
      else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	return __to_masktype(_mm256_cmp_pd(__xi, __yi, _CMP_UNORD_Q));
      else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	return __auto_bitcast(_mm_cmpunord_ps(__xi, __yi));
      else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	return __to_masktype(_mm_cmpunord_pd(__xi, __yi));
      else
	__assert_unreachable<_Tp>();
#endif
      }

    // }}}
    // _S_isgreater {{{
    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp> _S_isgreater(_SimdWrapper<_Tp, _Np> __x,
						     _SimdWrapper<_Tp, _Np> __y)
      {
	const auto __xi = __to_intrin(__x);
	const auto __yi = __to_intrin(__y);
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    const auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_GT_OQ);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_GT_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_GT_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_GT_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_GT_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_GT_OQ);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_avx)
	  {
	    if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return __to_masktype(_mm256_cmp_ps(__xi, __yi, _CMP_GT_OQ));
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm256_cmp_pd(__xi, __yi, _CMP_GT_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return __auto_bitcast(_mm_cmp_ps(__xi, __yi, _CMP_GT_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm_cmp_pd(__xi, __yi, _CMP_GT_OQ));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_sse2 && sizeof(__xi) == 16
			   && sizeof(_Tp) == 4)
	  {
	    const auto __xn = __vector_bitcast<int>(__xi);
	    const auto __yn = __vector_bitcast<int>(__yi);
	    const auto __xp = __xn < 0 ? -(__xn & 0x7fff'ffff) : __xn;
	    const auto __yp = __yn < 0 ? -(__yn & 0x7fff'ffff) : __yn;
	    return __auto_bitcast(
	      __and(__to_masktype(_mm_cmpord_ps(__xi, __yi)), __xp > __yp));
	  }
	else if constexpr (__have_sse2 && sizeof(__xi) == 16
			   && sizeof(_Tp) == 8)
	  return __vector_type_t<__int_with_sizeof_t<8>, 2>{
	    -_mm_ucomigt_sd(__xi, __yi),
	    -_mm_ucomigt_sd(_mm_unpackhi_pd(__xi, __xi),
			    _mm_unpackhi_pd(__yi, __yi))};
	else
	  return _Base::_S_isgreater(__x, __y);
      }

    // }}}
    // _S_isgreaterequal {{{
    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp>
      _S_isgreaterequal(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	const auto __xi = __to_intrin(__x);
	const auto __yi = __to_intrin(__y);
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    const auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_GE_OQ);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_GE_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_GE_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_GE_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_GE_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_GE_OQ);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_avx)
	  {
	    if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return __to_masktype(_mm256_cmp_ps(__xi, __yi, _CMP_GE_OQ));
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm256_cmp_pd(__xi, __yi, _CMP_GE_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return __auto_bitcast(_mm_cmp_ps(__xi, __yi, _CMP_GE_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm_cmp_pd(__xi, __yi, _CMP_GE_OQ));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_sse2 && sizeof(__xi) == 16
			   && sizeof(_Tp) == 4)
	  {
	    const auto __xn = __vector_bitcast<int>(__xi);
	    const auto __yn = __vector_bitcast<int>(__yi);
	    const auto __xp = __xn < 0 ? -(__xn & 0x7fff'ffff) : __xn;
	    const auto __yp = __yn < 0 ? -(__yn & 0x7fff'ffff) : __yn;
	    return __auto_bitcast(
	      __and(__to_masktype(_mm_cmpord_ps(__xi, __yi)), __xp >= __yp));
	  }
	else if constexpr (__have_sse2 && sizeof(__xi) == 16
			   && sizeof(_Tp) == 8)
	  return __vector_type_t<__int_with_sizeof_t<8>, 2>{
	    -_mm_ucomige_sd(__xi, __yi),
	    -_mm_ucomige_sd(_mm_unpackhi_pd(__xi, __xi),
			    _mm_unpackhi_pd(__yi, __yi))};
	else
	  return _Base::_S_isgreaterequal(__x, __y);
      }

    // }}}
    // _S_isless {{{
    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp> _S_isless(_SimdWrapper<_Tp, _Np> __x,
						  _SimdWrapper<_Tp, _Np> __y)
      {
	const auto __xi = __to_intrin(__x);
	const auto __yi = __to_intrin(__y);
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    const auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LT_OQ);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LT_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LT_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LT_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LT_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LT_OQ);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_avx)
	  {
	    if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return __to_masktype(_mm256_cmp_ps(__xi, __yi, _CMP_LT_OQ));
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm256_cmp_pd(__xi, __yi, _CMP_LT_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return __auto_bitcast(_mm_cmp_ps(__xi, __yi, _CMP_LT_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm_cmp_pd(__xi, __yi, _CMP_LT_OQ));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_sse2 && sizeof(__xi) == 16
			   && sizeof(_Tp) == 4)
	  {
	    const auto __xn = __vector_bitcast<int>(__xi);
	    const auto __yn = __vector_bitcast<int>(__yi);
	    const auto __xp = __xn < 0 ? -(__xn & 0x7fff'ffff) : __xn;
	    const auto __yp = __yn < 0 ? -(__yn & 0x7fff'ffff) : __yn;
	    return __auto_bitcast(
	      __and(__to_masktype(_mm_cmpord_ps(__xi, __yi)), __xp < __yp));
	  }
	else if constexpr (__have_sse2 && sizeof(__xi) == 16
			   && sizeof(_Tp) == 8)
	  return __vector_type_t<__int_with_sizeof_t<8>, 2>{
	    -_mm_ucomigt_sd(__yi, __xi),
	    -_mm_ucomigt_sd(_mm_unpackhi_pd(__yi, __yi),
			    _mm_unpackhi_pd(__xi, __xi))};
	else
	  return _Base::_S_isless(__x, __y);
      }

    // }}}
    // _S_islessequal {{{
    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp>
      _S_islessequal(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	const auto __xi = __to_intrin(__x);
	const auto __yi = __to_intrin(__y);
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    const auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LE_OQ);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LE_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LE_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LE_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_LE_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_LE_OQ);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_avx)
	  {
	    if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return __to_masktype(_mm256_cmp_ps(__xi, __yi, _CMP_LE_OQ));
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm256_cmp_pd(__xi, __yi, _CMP_LE_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return __auto_bitcast(_mm_cmp_ps(__xi, __yi, _CMP_LE_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm_cmp_pd(__xi, __yi, _CMP_LE_OQ));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_sse2 && sizeof(__xi) == 16
			   && sizeof(_Tp) == 4)
	  {
	    const auto __xn = __vector_bitcast<int>(__xi);
	    const auto __yn = __vector_bitcast<int>(__yi);
	    const auto __xp = __xn < 0 ? -(__xn & 0x7fff'ffff) : __xn;
	    const auto __yp = __yn < 0 ? -(__yn & 0x7fff'ffff) : __yn;
	    return __auto_bitcast(
	      __and(__to_masktype(_mm_cmpord_ps(__xi, __yi)), __xp <= __yp));
	  }
	else if constexpr (__have_sse2 && sizeof(__xi) == 16
			   && sizeof(_Tp) == 8)
	  return __vector_type_t<__int_with_sizeof_t<8>, 2>{
	    -_mm_ucomige_sd(__yi, __xi),
	    -_mm_ucomige_sd(_mm_unpackhi_pd(__yi, __yi),
			    _mm_unpackhi_pd(__xi, __xi))};
	else
	  return _Base::_S_islessequal(__x, __y);
      }

    // }}}
    // _S_islessgreater {{{
    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp>
      _S_islessgreater(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	const auto __xi = __to_intrin(__x);
	const auto __yi = __to_intrin(__y);
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    const auto __k1 = _Abi::template _S_implicit_mask_intrin<_Tp>();
	    if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 4)
	      return _mm512_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_NEQ_OQ);
	    else if constexpr (sizeof(__xi) == 64 && sizeof(_Tp) == 8)
	      return _mm512_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_NEQ_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return _mm256_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_NEQ_OQ);
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return _mm256_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_NEQ_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return _mm_mask_cmp_ps_mask(__k1, __xi, __yi, _CMP_NEQ_OQ);
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return _mm_mask_cmp_pd_mask(__k1, __xi, __yi, _CMP_NEQ_OQ);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__have_avx)
	  {
	    if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 4)
	      return __to_masktype(_mm256_cmp_ps(__xi, __yi, _CMP_NEQ_OQ));
	    else if constexpr (sizeof(__xi) == 32 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm256_cmp_pd(__xi, __yi, _CMP_NEQ_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	      return __auto_bitcast(_mm_cmp_ps(__xi, __yi, _CMP_NEQ_OQ));
	    else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	      return __to_masktype(_mm_cmp_pd(__xi, __yi, _CMP_NEQ_OQ));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 4)
	  return __auto_bitcast(
	    __and(_mm_cmpord_ps(__xi, __yi), _mm_cmpneq_ps(__xi, __yi)));
	else if constexpr (sizeof(__xi) == 16 && sizeof(_Tp) == 8)
	  return __to_masktype(
	    __and(_mm_cmpord_pd(__xi, __yi), _mm_cmpneq_pd(__xi, __yi)));
	else
	  __assert_unreachable<_Tp>();
      }

    //}}} }}}
  };

// }}}
// _MaskImplX86Mixin {{{
struct _MaskImplX86Mixin
{
  template <typename _Tp>
    using _TypeTag = _Tp*;

  using _Base = _MaskImplBuiltinMixin;

  // _S_to_maskvector(bool) {{{
  template <typename _Up, size_t _ToN = 1, typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static constexpr enable_if_t<
      is_same_v<_Tp, bool>, _SimdWrapper<_Up, _ToN>>
    _S_to_maskvector(_Tp __x)
    {
      static_assert(is_same_v<_Up, __int_for_sizeof_t<_Up>>);
      return __x ? __vector_type_t<_Up, _ToN>{~_Up()}
		 : __vector_type_t<_Up, _ToN>();
    }

  // }}}
  // _S_to_maskvector(_SanitizedBitMask) {{{
  template <typename _Up, size_t _UpN = 0, size_t _Np,
	    size_t _ToN = _UpN == 0 ? _Np : _UpN>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Up, _ToN>
    _S_to_maskvector(_SanitizedBitMask<_Np> __x)
    {
      static_assert(is_same_v<_Up, __int_for_sizeof_t<_Up>>);
      using _UV = __vector_type_t<_Up, _ToN>;
      using _UI = __intrinsic_type_t<_Up, _ToN>;
      [[maybe_unused]] const auto __k = __x._M_to_bits();
      if constexpr (_Np == 1)
	return _S_to_maskvector<_Up, _ToN>(__k);
      else if (__x._M_is_constprop() || __builtin_is_constant_evaluated())
	return __generate_from_n_evaluations<std::min(_ToN, _Np), _UV>(
	  [&](auto __i) -> _Up { return -__x[__i.value]; });
      else if constexpr (sizeof(_Up) == 1)
	{
	  if constexpr (sizeof(_UI) == 16)
	    {
	      if constexpr (__have_avx512bw_vl)
		return __intrin_bitcast<_UV>(_mm_movm_epi8(__k));
	      else if constexpr (__have_avx512bw)
		return __intrin_bitcast<_UV>(__lo128(_mm512_movm_epi8(__k)));
	      else if constexpr (__have_avx512f)
		{
		  auto __as32bits = _mm512_maskz_mov_epi32(__k, ~__m512i());
		  auto __as16bits
		    = __xzyw(_mm256_packs_epi32(__lo256(__as32bits),
						__hi256(__as32bits)));
		  return __intrin_bitcast<_UV>(
		    _mm_packs_epi16(__lo128(__as16bits), __hi128(__as16bits)));
		}
	      else if constexpr (__have_ssse3)
		{
		  const auto __bitmask = __to_intrin(
		    __make_vector<_UChar>(1, 2, 4, 8, 16, 32, 64, 128, 1, 2, 4,
					  8, 16, 32, 64, 128));
		  return __intrin_bitcast<_UV>(
		    __vector_bitcast<_Up>(
		      _mm_shuffle_epi8(__to_intrin(
					 __vector_type_t<_ULLong, 2>{__k}),
				       _mm_setr_epi8(0, 0, 0, 0, 0, 0, 0, 0, 1,
						     1, 1, 1, 1, 1, 1, 1))
		      & __bitmask)
		    != 0);
		}
	      // else fall through
	    }
	  else if constexpr (sizeof(_UI) == 32)
	    {
	      if constexpr (__have_avx512bw_vl)
		return __vector_bitcast<_Up>(_mm256_movm_epi8(__k));
	      else if constexpr (__have_avx512bw)
		return __vector_bitcast<_Up>(__lo256(_mm512_movm_epi8(__k)));
	      else if constexpr (__have_avx512f)
		{
		  auto __as16bits = // 0 16 1 17 ... 15 31
		    _mm512_srli_epi32(_mm512_maskz_mov_epi32(__k, ~__m512i()),
				      16)
		    | _mm512_slli_epi32(_mm512_maskz_mov_epi32(__k >> 16,
							       ~__m512i()),
					16);
		  auto __0_16_1_17 = __xzyw(_mm256_packs_epi16(
		    __lo256(__as16bits),
		    __hi256(__as16bits)) // 0 16 1 17 2 18 3 19 8 24 9 25 ...
		  );
		  // deinterleave:
		  return __vector_bitcast<_Up>(__xzyw(_mm256_shuffle_epi8(
		    __0_16_1_17, // 0 16 1 17 2 ...
		    _mm256_setr_epi8(0, 2, 4, 6, 8, 10, 12, 14, 1, 3, 5, 7, 9,
				     11, 13, 15, 0, 2, 4, 6, 8, 10, 12, 14, 1,
				     3, 5, 7, 9, 11, 13,
				     15)))); // 0-7 16-23 8-15 24-31 -> xzyw
					     // 0-3  8-11 16-19 24-27
					     // 4-7 12-15 20-23 28-31
		}
	      else if constexpr (__have_avx2)
		{
		  const auto __bitmask
		    = _mm256_broadcastsi128_si256(__to_intrin(
		      __make_vector<_UChar>(1, 2, 4, 8, 16, 32, 64, 128, 1, 2,
					    4, 8, 16, 32, 64, 128)));
		  return __vector_bitcast<_Up>(
		    __vector_bitcast<_Up>(
		      _mm256_shuffle_epi8(
			_mm256_broadcastsi128_si256(
			  __to_intrin(__vector_type_t<_ULLong, 2>{__k})),
			_mm256_setr_epi8(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1,
					 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3,
					 3, 3, 3, 3, 3, 3))
		      & __bitmask)
		    != 0);
		}
	      // else fall through
	    }
	  else if constexpr (sizeof(_UI) == 64)
	    return reinterpret_cast<_UV>(_mm512_movm_epi8(__k));
	  if constexpr (std::min(_ToN, _Np) <= 4)
	    {
	      if constexpr (_Np > 7) // avoid overflow
		__x &= _SanitizedBitMask<_Np>(0x0f);
	      const _UInt __char_mask
		= ((_UInt(__x.to_ulong()) * 0x00204081U) & 0x01010101ULL)
		  * 0xff;
	      _UV __r = {};
	      __builtin_memcpy(&__r, &__char_mask,
			       std::min(sizeof(__r), sizeof(__char_mask)));
	      return __r;
	    }
	  else if constexpr (std::min(_ToN, _Np) <= 7)
	    {
	      if constexpr (_Np > 7) // avoid overflow
		__x &= _SanitizedBitMask<_Np>(0x7f);
	      const _ULLong __char_mask
		= ((__x.to_ulong() * 0x40810204081ULL) & 0x0101010101010101ULL)
		  * 0xff;
	      _UV __r = {};
	      __builtin_memcpy(&__r, &__char_mask,
			       std::min(sizeof(__r), sizeof(__char_mask)));
	      return __r;
	    }
	}
      else if constexpr (sizeof(_Up) == 2)
	{
	  if constexpr (sizeof(_UI) == 16)
	    {
	      if constexpr (__have_avx512bw_vl)
		return __intrin_bitcast<_UV>(_mm_movm_epi16(__k));
	      else if constexpr (__have_avx512bw)
		return __intrin_bitcast<_UV>(__lo128(_mm512_movm_epi16(__k)));
	      else if constexpr (__have_avx512f)
		{
		  __m256i __as32bits = {};
		  if constexpr (__have_avx512vl)
		    __as32bits = _mm256_maskz_mov_epi32(__k, ~__m256i());
		  else
		    __as32bits
		      = __lo256(_mm512_maskz_mov_epi32(__k, ~__m512i()));
		  return __intrin_bitcast<_UV>(
		    _mm_packs_epi32(__lo128(__as32bits), __hi128(__as32bits)));
		}
	      // else fall through
	    }
	  else if constexpr (sizeof(_UI) == 32)
	    {
	      if constexpr (__have_avx512bw_vl)
		return __vector_bitcast<_Up>(_mm256_movm_epi16(__k));
	      else if constexpr (__have_avx512bw)
		return __vector_bitcast<_Up>(__lo256(_mm512_movm_epi16(__k)));
	      else if constexpr (__have_avx512f)
		{
		  auto __as32bits = _mm512_maskz_mov_epi32(__k, ~__m512i());
		  return __vector_bitcast<_Up>(
		    __xzyw(_mm256_packs_epi32(__lo256(__as32bits),
					      __hi256(__as32bits))));
		}
	      // else fall through
	    }
	  else if constexpr (sizeof(_UI) == 64)
	    return __vector_bitcast<_Up>(_mm512_movm_epi16(__k));
	}
      else if constexpr (sizeof(_Up) == 4)
	{
	  if constexpr (sizeof(_UI) == 16)
	    {
	      if constexpr (__have_avx512dq_vl)
		return __intrin_bitcast<_UV>(_mm_movm_epi32(__k));
	      else if constexpr (__have_avx512dq)
		return __intrin_bitcast<_UV>(__lo128(_mm512_movm_epi32(__k)));
	      else if constexpr (__have_avx512vl)
		return __intrin_bitcast<_UV>(
		  _mm_maskz_mov_epi32(__k, ~__m128i()));
	      else if constexpr (__have_avx512f)
		return __intrin_bitcast<_UV>(
		  __lo128(_mm512_maskz_mov_epi32(__k, ~__m512i())));
	      // else fall through
	    }
	  else if constexpr (sizeof(_UI) == 32)
	    {
	      if constexpr (__have_avx512dq_vl)
		return __vector_bitcast<_Up>(_mm256_movm_epi32(__k));
	      else if constexpr (__have_avx512dq)
		return __vector_bitcast<_Up>(__lo256(_mm512_movm_epi32(__k)));
	      else if constexpr (__have_avx512vl)
		return __vector_bitcast<_Up>(
		  _mm256_maskz_mov_epi32(__k, ~__m256i()));
	      else if constexpr (__have_avx512f)
		return __vector_bitcast<_Up>(
		  __lo256(_mm512_maskz_mov_epi32(__k, ~__m512i())));
	      // else fall through
	    }
	  else if constexpr (sizeof(_UI) == 64)
	    return __vector_bitcast<_Up>(
	      __have_avx512dq ? _mm512_movm_epi32(__k)
			      : _mm512_maskz_mov_epi32(__k, ~__m512i()));
	}
      else if constexpr (sizeof(_Up) == 8)
	{
	  if constexpr (sizeof(_UI) == 16)
	    {
	      if constexpr (__have_avx512dq_vl)
		return __vector_bitcast<_Up>(_mm_movm_epi64(__k));
	      else if constexpr (__have_avx512dq)
		return __vector_bitcast<_Up>(__lo128(_mm512_movm_epi64(__k)));
	      else if constexpr (__have_avx512vl)
		return __vector_bitcast<_Up>(
		  _mm_maskz_mov_epi64(__k, ~__m128i()));
	      else if constexpr (__have_avx512f)
		return __vector_bitcast<_Up>(
		  __lo128(_mm512_maskz_mov_epi64(__k, ~__m512i())));
	      // else fall through
	    }
	  else if constexpr (sizeof(_UI) == 32)
	    {
	      if constexpr (__have_avx512dq_vl)
		return __vector_bitcast<_Up>(_mm256_movm_epi64(__k));
	      else if constexpr (__have_avx512dq)
		return __vector_bitcast<_Up>(__lo256(_mm512_movm_epi64(__k)));
	      else if constexpr (__have_avx512vl)
		return __vector_bitcast<_Up>(
		  _mm256_maskz_mov_epi64(__k, ~__m256i()));
	      else if constexpr (__have_avx512f)
		return __vector_bitcast<_Up>(
		  __lo256(_mm512_maskz_mov_epi64(__k, ~__m512i())));
	      // else fall through
	    }
	  else if constexpr (sizeof(_UI) == 64)
	    return __vector_bitcast<_Up>(
	      __have_avx512dq ? _mm512_movm_epi64(__k)
			      : _mm512_maskz_mov_epi64(__k, ~__m512i()));
	}

      using _UpUInt = make_unsigned_t<_Up>;
      using _V = __vector_type_t<_UpUInt, _ToN>;
      constexpr size_t __bits_per_element = sizeof(_Up) * __CHAR_BIT__;
      if constexpr (_ToN == 2)
	{
	  return __vector_bitcast<_Up>(_V{_UpUInt(-__x[0]), _UpUInt(-__x[1])});
	}
      else if constexpr (!__have_avx2 && __have_avx && sizeof(_V) == 32)
	{
	  if constexpr (sizeof(_Up) == 4)
	    return __vector_bitcast<_Up>(_mm256_cmp_ps(
	      _mm256_and_ps(_mm256_castsi256_ps(_mm256_set1_epi32(__k)),
			    _mm256_castsi256_ps(_mm256_setr_epi32(
			      0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80))),
	      _mm256_setzero_ps(), _CMP_NEQ_UQ));
	  else if constexpr (sizeof(_Up) == 8)
	    return __vector_bitcast<_Up>(_mm256_cmp_pd(
	      _mm256_and_pd(_mm256_castsi256_pd(_mm256_set1_epi64x(__k)),
			    _mm256_castsi256_pd(
			      _mm256_setr_epi64x(0x01, 0x02, 0x04, 0x08))),
	      _mm256_setzero_pd(), _CMP_NEQ_UQ));
	  else
	    __assert_unreachable<_Up>();
	}
      else if constexpr (__bits_per_element >= _ToN)
	{
	  constexpr auto __bitmask
	    = __generate_vector<_V>([](auto __i) constexpr->_UpUInt {
		return __i < _ToN ? 1ull << __i : 0;
	      });
	  const auto __bits
	    = __vector_broadcast<_ToN, _UpUInt>(__k) & __bitmask;
	  if constexpr (__bits_per_element > _ToN)
	    return __vector_bitcast<_Up>(__bits) > 0;
	  else
	    return __vector_bitcast<_Up>(__bits != 0);
	}
      else
	{
	  const _V __tmp
	    = __generate_vector<_V>([&](auto __i) constexpr {
		return static_cast<_UpUInt>(
		  __k >> (__bits_per_element * (__i / __bits_per_element)));
	      })
	      & __generate_vector<_V>([](auto __i) constexpr {
		  return static_cast<_UpUInt>(1ull
					      << (__i % __bits_per_element));
		}); // mask bit index
	  return __intrin_bitcast<_UV>(__tmp != _V());
	}
    }

  // }}}
  // _S_to_maskvector(_SimdWrapper) {{{
  template <typename _Up, size_t _UpN = 0, typename _Tp, size_t _Np,
	    size_t _ToN = _UpN == 0 ? _Np : _UpN>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Up, _ToN>
    _S_to_maskvector(_SimdWrapper<_Tp, _Np> __x)
    {
      static_assert(is_same_v<_Up, __int_for_sizeof_t<_Up>>);
      using _TW = _SimdWrapper<_Tp, _Np>;
      using _UW = _SimdWrapper<_Up, _ToN>;
      using _UI = __intrinsic_type_t<_Up, _ToN>;
      if constexpr (is_same_v<_Tp, bool>) // bits -> vector
	return _S_to_maskvector<_Up, _ToN>(
	  _BitMask<_Np>(__x._M_data)._M_sanitized());
      // vector -> vector bitcast
      else if constexpr (sizeof(_Up) == sizeof(_Tp)
			 && sizeof(_TW) == sizeof(_UW))
	return __wrapper_bitcast<_Up, _ToN>(
	  _ToN <= _Np
	    ? __x
	    : simd_abi::_VecBuiltin<sizeof(_Tp) * _Np>::_S_masked(__x));
      else // vector -> vector {{{
	{
	  if (__x._M_is_constprop() || __builtin_is_constant_evaluated())
	    {
	      const auto __y = __vector_bitcast<__int_for_sizeof_t<_Tp>>(__x);
	      return __generate_from_n_evaluations<std::min(_ToN, _Np),
						   __vector_type_t<_Up, _ToN>>(
		[&](auto __i) -> _Up { return __y[__i.value]; });
	    }
	  using _To = __vector_type_t<_Up, _ToN>;
	  [[maybe_unused]] constexpr size_t _FromN = _Np;
	  constexpr int _FromBytes = sizeof(_Tp);
	  constexpr int _ToBytes = sizeof(_Up);
	  const auto __k = __x._M_data;

	  if constexpr (_FromBytes == _ToBytes)
	    return __intrin_bitcast<_To>(__k);
	  else if constexpr (sizeof(_UI) == 16 && sizeof(__k) == 16)
	    { // SSE -> SSE {{{
	      if constexpr (_FromBytes == 4 && _ToBytes == 8)
		return __intrin_bitcast<_To>(__interleave128_lo(__k, __k));
	      else if constexpr (_FromBytes == 2 && _ToBytes == 8)
		{
		  const auto __y
		    = __vector_bitcast<int>(__interleave128_lo(__k, __k));
		  return __intrin_bitcast<_To>(__interleave128_lo(__y, __y));
		}
	      else if constexpr (_FromBytes == 1 && _ToBytes == 8)
		{
		  auto __y
		    = __vector_bitcast<short>(__interleave128_lo(__k, __k));
		  auto __z
		    = __vector_bitcast<int>(__interleave128_lo(__y, __y));
		  return __intrin_bitcast<_To>(__interleave128_lo(__z, __z));
		}
	      else if constexpr (_FromBytes == 8 && _ToBytes == 4
				 && __have_sse2)
		return __intrin_bitcast<_To>(
		  _mm_packs_epi32(__vector_bitcast<_LLong>(__k), __m128i()));
	      else if constexpr (_FromBytes == 8 && _ToBytes == 4)
		return __vector_shuffle<1, 3, 6, 7>(__vector_bitcast<_Up>(__k),
						    _UI());
	      else if constexpr (_FromBytes == 2 && _ToBytes == 4)
		return __intrin_bitcast<_To>(__interleave128_lo(__k, __k));
	      else if constexpr (_FromBytes == 1 && _ToBytes == 4)
		{
		  const auto __y
		    = __vector_bitcast<short>(__interleave128_lo(__k, __k));
		  return __intrin_bitcast<_To>(__interleave128_lo(__y, __y));
		}
	      else if constexpr (_FromBytes == 8 && _ToBytes == 2)
		{
		  if constexpr (__have_sse2 && !__have_ssse3)
		    return __intrin_bitcast<_To>(_mm_packs_epi32(
		      _mm_packs_epi32(__vector_bitcast<_LLong>(__k), __m128i()),
		      __m128i()));
		  else
		    return __intrin_bitcast<_To>(
		      __vector_permute<3, 7, -1, -1, -1, -1, -1, -1>(
			__vector_bitcast<_Up>(__k)));
		}
	      else if constexpr (_FromBytes == 4 && _ToBytes == 2)
		return __intrin_bitcast<_To>(
		  _mm_packs_epi32(__vector_bitcast<_LLong>(__k), __m128i()));
	      else if constexpr (_FromBytes == 1 && _ToBytes == 2)
		return __intrin_bitcast<_To>(__interleave128_lo(__k, __k));
	      else if constexpr (_FromBytes == 8 && _ToBytes == 1
				 && __have_ssse3)
		return __intrin_bitcast<_To>(
		  _mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
				   _mm_setr_epi8(7, 15, -1, -1, -1, -1, -1, -1,
						 -1, -1, -1, -1, -1, -1, -1,
						 -1)));
	      else if constexpr (_FromBytes == 8 && _ToBytes == 1)
		{
		  auto __y
		    = _mm_packs_epi32(__vector_bitcast<_LLong>(__k), __m128i());
		  __y = _mm_packs_epi32(__y, __m128i());
		  return __intrin_bitcast<_To>(_mm_packs_epi16(__y, __m128i()));
		}
	      else if constexpr (_FromBytes == 4 && _ToBytes == 1
				 && __have_ssse3)
		return __intrin_bitcast<_To>(
		  _mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
				   _mm_setr_epi8(3, 7, 11, 15, -1, -1, -1, -1,
						 -1, -1, -1, -1, -1, -1, -1,
						 -1)));
	      else if constexpr (_FromBytes == 4 && _ToBytes == 1)
		{
		  const auto __y
		    = _mm_packs_epi32(__vector_bitcast<_LLong>(__k), __m128i());
		  return __intrin_bitcast<_To>(_mm_packs_epi16(__y, __m128i()));
		}
	      else if constexpr (_FromBytes == 2 && _ToBytes == 1)
		return __intrin_bitcast<_To>(
		  _mm_packs_epi16(__vector_bitcast<_LLong>(__k), __m128i()));
	      else
		__assert_unreachable<_Tp>();
	    } // }}}
	  else if constexpr (sizeof(_UI) == 32 && sizeof(__k) == 32)
	    { // AVX -> AVX {{{
	      if constexpr (_FromBytes == _ToBytes)
		__assert_unreachable<_Tp>();
	      else if constexpr (_FromBytes == _ToBytes * 2)
		{
		  const auto __y = __vector_bitcast<_LLong>(__k);
		  return __intrin_bitcast<_To>(_mm256_castsi128_si256(
		    _mm_packs_epi16(__lo128(__y), __hi128(__y))));
		}
	      else if constexpr (_FromBytes == _ToBytes * 4)
		{
		  const auto __y = __vector_bitcast<_LLong>(__k);
		  return __intrin_bitcast<_To>(_mm256_castsi128_si256(
		    _mm_packs_epi16(_mm_packs_epi16(__lo128(__y), __hi128(__y)),
				    __m128i())));
		}
	      else if constexpr (_FromBytes == _ToBytes * 8)
		{
		  const auto __y = __vector_bitcast<_LLong>(__k);
		  return __intrin_bitcast<_To>(
		    _mm256_castsi128_si256(_mm_shuffle_epi8(
		      _mm_packs_epi16(__lo128(__y), __hi128(__y)),
		      _mm_setr_epi8(3, 7, 11, 15, -1, -1, -1, -1, -1, -1, -1,
				    -1, -1, -1, -1, -1))));
		}
	      else if constexpr (_FromBytes * 2 == _ToBytes)
		{
		  auto __y = __xzyw(__to_intrin(__k));
		  if constexpr (is_floating_point_v<
				  _Tp> || (!__have_avx2 && _FromBytes == 4))
		    {
		      const auto __yy = __vector_bitcast<float>(__y);
		      return __intrin_bitcast<_To>(
			_mm256_unpacklo_ps(__yy, __yy));
		    }
		  else
		    return __intrin_bitcast<_To>(
		      _mm256_unpacklo_epi8(__y, __y));
		}
	      else if constexpr (_FromBytes * 4 == _ToBytes)
		{
		  auto __y
		    = _mm_unpacklo_epi8(__lo128(__vector_bitcast<_LLong>(__k)),
					__lo128(__vector_bitcast<_LLong>(
					  __k))); // drops 3/4 of input
		  return __intrin_bitcast<_To>(
		    __concat(_mm_unpacklo_epi16(__y, __y),
			     _mm_unpackhi_epi16(__y, __y)));
		}
	      else if constexpr (_FromBytes == 1 && _ToBytes == 8)
		{
		  auto __y
		    = _mm_unpacklo_epi8(__lo128(__vector_bitcast<_LLong>(__k)),
					__lo128(__vector_bitcast<_LLong>(
					  __k))); // drops 3/4 of input
		  __y
		    = _mm_unpacklo_epi16(__y,
					 __y); // drops another 1/2 => 7/8 total
		  return __intrin_bitcast<_To>(
		    __concat(_mm_unpacklo_epi32(__y, __y),
			     _mm_unpackhi_epi32(__y, __y)));
		}
	      else
		__assert_unreachable<_Tp>();
	    } // }}}
	  else if constexpr (sizeof(_UI) == 32 && sizeof(__k) == 16)
	    { // SSE -> AVX {{{
	      if constexpr (_FromBytes == _ToBytes)
		return __intrin_bitcast<_To>(
		  __intrinsic_type_t<_Tp, 32 / sizeof(_Tp)>(
		    __zero_extend(__to_intrin(__k))));
	      else if constexpr (_FromBytes * 2 == _ToBytes)
		{ // keep all
		  return __intrin_bitcast<_To>(
		    __concat(_mm_unpacklo_epi8(__vector_bitcast<_LLong>(__k),
					       __vector_bitcast<_LLong>(__k)),
			     _mm_unpackhi_epi8(__vector_bitcast<_LLong>(__k),
					       __vector_bitcast<_LLong>(__k))));
		}
	      else if constexpr (_FromBytes * 4 == _ToBytes)
		{
		  if constexpr (__have_avx2)
		    {
		      return __intrin_bitcast<_To>(_mm256_shuffle_epi8(
			__concat(__vector_bitcast<_LLong>(__k),
				 __vector_bitcast<_LLong>(__k)),
			_mm256_setr_epi8(0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3,
					 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6,
					 6, 6, 7, 7, 7, 7)));
		    }
		  else
		    {
		      return __intrin_bitcast<_To>(__concat(
			_mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
					 _mm_setr_epi8(0, 0, 0, 0, 1, 1, 1, 1,
						       2, 2, 2, 2, 3, 3, 3, 3)),
			_mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
					 _mm_setr_epi8(4, 4, 4, 4, 5, 5, 5, 5,
						       6, 6, 6, 6, 7, 7, 7,
						       7))));
		    }
		}
	      else if constexpr (_FromBytes * 8 == _ToBytes)
		{
		  if constexpr (__have_avx2)
		    {
		      return __intrin_bitcast<_To>(_mm256_shuffle_epi8(
			__concat(__vector_bitcast<_LLong>(__k),
				 __vector_bitcast<_LLong>(__k)),
			_mm256_setr_epi8(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1,
					 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3,
					 3, 3, 3, 3, 3, 3)));
		    }
		  else
		    {
		      return __intrin_bitcast<_To>(__concat(
			_mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
					 _mm_setr_epi8(0, 0, 0, 0, 0, 0, 0, 0,
						       1, 1, 1, 1, 1, 1, 1, 1)),
			_mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
					 _mm_setr_epi8(2, 2, 2, 2, 2, 2, 2, 2,
						       3, 3, 3, 3, 3, 3, 3,
						       3))));
		    }
		}
	      else if constexpr (_FromBytes == _ToBytes * 2)
		return __intrin_bitcast<_To>(__m256i(__zero_extend(
		  _mm_packs_epi16(__vector_bitcast<_LLong>(__k), __m128i()))));
	      else if constexpr (_FromBytes == 8 && _ToBytes == 2)
		{
		  return __intrin_bitcast<_To>(__m256i(__zero_extend(
		    _mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
				     _mm_setr_epi8(6, 7, 14, 15, -1, -1, -1, -1,
						   -1, -1, -1, -1, -1, -1, -1,
						   -1)))));
		}
	      else if constexpr (_FromBytes == 4 && _ToBytes == 1)
		{
		  return __intrin_bitcast<_To>(__m256i(__zero_extend(
		    _mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
				     _mm_setr_epi8(3, 7, 11, 15, -1, -1, -1, -1,
						   -1, -1, -1, -1, -1, -1, -1,
						   -1)))));
		}
	      else if constexpr (_FromBytes == 8 && _ToBytes == 1)
		{
		  return __intrin_bitcast<_To>(__m256i(__zero_extend(
		    _mm_shuffle_epi8(__vector_bitcast<_LLong>(__k),
				     _mm_setr_epi8(7, 15, -1, -1, -1, -1, -1,
						   -1, -1, -1, -1, -1, -1, -1,
						   -1, -1)))));
		}
	      else
		static_assert(!is_same_v<_Tp, _Tp>, "should be unreachable");
	    } // }}}
	  else if constexpr (sizeof(_UI) == 16 && sizeof(__k) == 32)
	    { // AVX -> SSE {{{
	      if constexpr (_FromBytes == _ToBytes)
		{ // keep low 1/2
		  return __intrin_bitcast<_To>(__lo128(__k));
		}
	      else if constexpr (_FromBytes == _ToBytes * 2)
		{ // keep all
		  auto __y = __vector_bitcast<_LLong>(__k);
		  return __intrin_bitcast<_To>(
		    _mm_packs_epi16(__lo128(__y), __hi128(__y)));
		}
	      else if constexpr (_FromBytes == _ToBytes * 4)
		{ // add 1/2 undef
		  auto __y = __vector_bitcast<_LLong>(__k);
		  return __intrin_bitcast<_To>(
		    _mm_packs_epi16(_mm_packs_epi16(__lo128(__y), __hi128(__y)),
				    __m128i()));
		}
	      else if constexpr (_FromBytes == 8 && _ToBytes == 1)
		{ // add 3/4 undef
		  auto __y = __vector_bitcast<_LLong>(__k);
		  return __intrin_bitcast<_To>(_mm_shuffle_epi8(
		    _mm_packs_epi16(__lo128(__y), __hi128(__y)),
		    _mm_setr_epi8(3, 7, 11, 15, -1, -1, -1, -1, -1, -1, -1, -1,
				  -1, -1, -1, -1)));
		}
	      else if constexpr (_FromBytes * 2 == _ToBytes)
		{ // keep low 1/4
		  auto __y = __lo128(__vector_bitcast<_LLong>(__k));
		  return __intrin_bitcast<_To>(_mm_unpacklo_epi8(__y, __y));
		}
	      else if constexpr (_FromBytes * 4 == _ToBytes)
		{ // keep low 1/8
		  auto __y = __lo128(__vector_bitcast<_LLong>(__k));
		  __y = _mm_unpacklo_epi8(__y, __y);
		  return __intrin_bitcast<_To>(_mm_unpacklo_epi8(__y, __y));
		}
	      else if constexpr (_FromBytes * 8 == _ToBytes)
		{ // keep low 1/16
		  auto __y = __lo128(__vector_bitcast<_LLong>(__k));
		  __y = _mm_unpacklo_epi8(__y, __y);
		  __y = _mm_unpacklo_epi8(__y, __y);
		  return __intrin_bitcast<_To>(_mm_unpacklo_epi8(__y, __y));
		}
	      else
		static_assert(!is_same_v<_Tp, _Tp>, "should be unreachable");
	    } // }}}
	  else
	    return _Base::template _S_to_maskvector<_Up, _ToN>(__x);
	  /*
	  if constexpr (_FromBytes > _ToBytes) {
	      const _To     __y      = __vector_bitcast<_Up>(__k);
	      return [&] <size_t... _Is> (index_sequence<_Is...>) {
		constexpr int _Stride = _FromBytes / _ToBytes;
		return _To{__y[(_Is + 1) * _Stride - 1]...};
	      }(make_index_sequence<std::min(_ToN, _FromN)>());
	  } else {
	      // {0, 0, 1, 1} (_Dups = 2, _Is<4>)
	      // {0, 0, 0, 0, 1, 1, 1, 1} (_Dups = 4, _Is<8>)
	      // {0, 0, 1, 1, 2, 2, 3, 3} (_Dups = 2, _Is<8>)
	      // ...
	      return [&] <size_t... _Is> (index_sequence<_Is...>) {
		constexpr int __dup = _ToBytes / _FromBytes;
		return __intrin_bitcast<_To>(_From{__k[_Is / __dup]...});
	      }(make_index_sequence<_FromN>());
	  }
	  */
	} // }}}
    }

  // }}}
  // _S_to_bits {{{
  template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SanitizedBitMask<_Np>
    _S_to_bits(_SimdWrapper<_Tp, _Np> __x)
    {
      if constexpr (is_same_v<_Tp, bool>)
	return _BitMask<_Np>(__x._M_data)._M_sanitized();
      else
	{
	  static_assert(is_same_v<_Tp, __int_for_sizeof_t<_Tp>>);
	  if (__builtin_is_constant_evaluated()
	      || __builtin_constant_p(__x._M_data))
	    {
	      const auto __bools = -__x._M_data;
	      const _ULLong __k = __call_with_n_evaluations<_Np>(
		[](auto... __bits) { return (__bits | ...); },
		[&](auto __i) { return _ULLong(__bools[+__i]) << __i; });
	      if (__builtin_is_constant_evaluated()
		  || __builtin_constant_p(__k))
		return __k;
	    }
	  const auto __xi = __to_intrin(__x);
	  if constexpr (sizeof(_Tp) == 1)
	    if constexpr (sizeof(__xi) == 16)
	      if constexpr (__have_avx512bw_vl)
		return _BitMask<_Np>(_mm_movepi8_mask(__xi));
	      else // implies SSE2
		return _BitMask<_Np>(_mm_movemask_epi8(__xi));
	    else if constexpr (sizeof(__xi) == 32)
	      if constexpr (__have_avx512bw_vl)
		return _BitMask<_Np>(_mm256_movepi8_mask(__xi));
	      else // implies AVX2
		return _BitMask<_Np>(_mm256_movemask_epi8(__xi));
	    else // implies AVX512BW
	      return _BitMask<_Np>(_mm512_movepi8_mask(__xi));

	  else if constexpr (sizeof(_Tp) == 2)
	    if constexpr (sizeof(__xi) == 16)
	      if constexpr (__have_avx512bw_vl)
		return _BitMask<_Np>(_mm_movepi16_mask(__xi));
	      else if constexpr (__have_avx512bw)
		return _BitMask<_Np>(_mm512_movepi16_mask(__zero_extend(__xi)));
	      else // implies SSE2
		return _BitMask<_Np>(
		  _mm_movemask_epi8(_mm_packs_epi16(__xi, __m128i())));
	    else if constexpr (sizeof(__xi) == 32)
	      if constexpr (__have_avx512bw_vl)
		return _BitMask<_Np>(_mm256_movepi16_mask(__xi));
	      else if constexpr (__have_avx512bw)
		return _BitMask<_Np>(_mm512_movepi16_mask(__zero_extend(__xi)));
	      else // implies SSE2
		return _BitMask<_Np>(_mm_movemask_epi8(
		  _mm_packs_epi16(__lo128(__xi), __hi128(__xi))));
	    else // implies AVX512BW
	      return _BitMask<_Np>(_mm512_movepi16_mask(__xi));

	  else if constexpr (sizeof(_Tp) == 4)
	    if constexpr (sizeof(__xi) == 16)
	      if constexpr (__have_avx512dq_vl)
		return _BitMask<_Np>(_mm_movepi32_mask(__xi));
	      else if constexpr (__have_avx512vl)
		return _BitMask<_Np>(_mm_cmplt_epi32_mask(__xi, __m128i()));
	      else if constexpr (__have_avx512dq)
		return _BitMask<_Np>(_mm512_movepi32_mask(__zero_extend(__xi)));
	      else if constexpr (__have_avx512f)
		return _BitMask<_Np>(
		  _mm512_cmplt_epi32_mask(__zero_extend(__xi), __m512i()));
	      else // implies SSE
		return _BitMask<_Np>(
		  _mm_movemask_ps(reinterpret_cast<__m128>(__xi)));
	    else if constexpr (sizeof(__xi) == 32)
	      if constexpr (__have_avx512dq_vl)
		return _BitMask<_Np>(_mm256_movepi32_mask(__xi));
	      else if constexpr (__have_avx512dq)
		return _BitMask<_Np>(_mm512_movepi32_mask(__zero_extend(__xi)));
	      else if constexpr (__have_avx512vl)
		return _BitMask<_Np>(_mm256_cmplt_epi32_mask(__xi, __m256i()));
	      else if constexpr (__have_avx512f)
		return _BitMask<_Np>(
		  _mm512_cmplt_epi32_mask(__zero_extend(__xi), __m512i()));
	      else // implies AVX
		return _BitMask<_Np>(
		  _mm256_movemask_ps(reinterpret_cast<__m256>(__xi)));
	    else // implies AVX512??
	      if constexpr (__have_avx512dq)
	      return _BitMask<_Np>(_mm512_movepi32_mask(__xi));
	    else // implies AVX512F
	      return _BitMask<_Np>(_mm512_cmplt_epi32_mask(__xi, __m512i()));

	  else if constexpr (sizeof(_Tp) == 8)
	    if constexpr (sizeof(__xi) == 16)
	      if constexpr (__have_avx512dq_vl)
		return _BitMask<_Np>(_mm_movepi64_mask(__xi));
	      else if constexpr (__have_avx512dq)
		return _BitMask<_Np>(_mm512_movepi64_mask(__zero_extend(__xi)));
	      else if constexpr (__have_avx512vl)
		return _BitMask<_Np>(_mm_cmplt_epi64_mask(__xi, __m128i()));
	      else if constexpr (__have_avx512f)
		return _BitMask<_Np>(
		  _mm512_cmplt_epi64_mask(__zero_extend(__xi), __m512i()));
	      else // implies SSE2
		return _BitMask<_Np>(
		  _mm_movemask_pd(reinterpret_cast<__m128d>(__xi)));
	    else if constexpr (sizeof(__xi) == 32)
	      if constexpr (__have_avx512dq_vl)
		return _BitMask<_Np>(_mm256_movepi64_mask(__xi));
	      else if constexpr (__have_avx512dq)
		return _BitMask<_Np>(_mm512_movepi64_mask(__zero_extend(__xi)));
	      else if constexpr (__have_avx512vl)
		return _BitMask<_Np>(_mm256_cmplt_epi64_mask(__xi, __m256i()));
	      else if constexpr (__have_avx512f)
		return _BitMask<_Np>(
		  _mm512_cmplt_epi64_mask(__zero_extend(__xi), __m512i()));
	      else // implies AVX
		return _BitMask<_Np>(
		  _mm256_movemask_pd(reinterpret_cast<__m256d>(__xi)));
	    else // implies AVX512??
	      if constexpr (__have_avx512dq)
	      return _BitMask<_Np>(_mm512_movepi64_mask(__xi));
	    else // implies AVX512F
	      return _BitMask<_Np>(_mm512_cmplt_epi64_mask(__xi, __m512i()));

	  else
	    __assert_unreachable<_Tp>();
	}
    }
  // }}}
};

// }}}
// _MaskImplX86 {{{
template <typename _Abi, typename>
  struct _MaskImplX86 : _MaskImplX86Mixin, _MaskImplBuiltin<_Abi>
  {
    using _MaskImplX86Mixin::_S_to_bits;
    using _MaskImplX86Mixin::_S_to_maskvector;
    using _MaskImplBuiltin<_Abi>::_S_convert;

    // member types {{{
    template <typename _Tp>
      using _SimdMember = typename _Abi::template __traits<_Tp>::_SimdMember;

    template <typename _Tp>
      using _MaskMember = typename _Abi::template _MaskMember<_Tp>;

    template <typename _Tp>
      static constexpr size_t _S_size = simd_size_v<_Tp, _Abi>;

    using _Base = _MaskImplBuiltin<_Abi>;

    // }}}
    // _S_broadcast {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_broadcast(bool __x)
      {
	if constexpr (__is_avx512_abi<_Abi>())
	  return __x ? _Abi::_S_masked(_MaskMember<_Tp>(-1))
		     : _MaskMember<_Tp>();
	else
	  return _Base::template _S_broadcast<_Tp>(__x);
      }

    // }}}
    // _S_load {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_load(const bool* __mem)
      {
	static_assert(is_same_v<_Tp, __int_for_sizeof_t<_Tp>>);
	if constexpr (__have_avx512bw)
	  {
	    const auto __to_vec_or_bits = [](auto __bits) -> decltype(auto) {
	      if constexpr (__is_avx512_abi<_Abi>())
		return __bits;
	      else
		return _S_to_maskvector<_Tp>(
		  _BitMask<_S_size<_Tp>>(__bits)._M_sanitized());
	    };

	    if constexpr (_S_size<_Tp> <= 16 && __have_avx512vl)
	      {
		__m128i __a = {};
		__builtin_memcpy(&__a, __mem, _S_size<_Tp>);
		return __to_vec_or_bits(_mm_test_epi8_mask(__a, __a));
	      }
	    else if constexpr (_S_size<_Tp> <= 32 && __have_avx512vl)
	      {
		__m256i __a = {};
		__builtin_memcpy(&__a, __mem, _S_size<_Tp>);
		return __to_vec_or_bits(_mm256_test_epi8_mask(__a, __a));
	      }
	    else if constexpr (_S_size<_Tp> <= 64)
	      {
		__m512i __a = {};
		__builtin_memcpy(&__a, __mem, _S_size<_Tp>);
		return __to_vec_or_bits(_mm512_test_epi8_mask(__a, __a));
	      }
	  }
	else if constexpr (__is_avx512_abi<_Abi>())
	  {
	    if constexpr (_S_size<_Tp> <= 8)
	      {
		__m128i __a = {};
		__builtin_memcpy(&__a, __mem, _S_size<_Tp>);
		const auto __b = _mm512_cvtepi8_epi64(__a);
		return _mm512_test_epi64_mask(__b, __b);
	      }
	    else if constexpr (_S_size<_Tp> <= 16)
	      {
		__m128i __a = {};
		__builtin_memcpy(&__a, __mem, _S_size<_Tp>);
		const auto __b = _mm512_cvtepi8_epi32(__a);
		return _mm512_test_epi32_mask(__b, __b);
	      }
	    else if constexpr (_S_size<_Tp> <= 32)
	      {
		__m128i __a = {};
		__builtin_memcpy(&__a, __mem, 16);
		const auto __b = _mm512_cvtepi8_epi32(__a);
		__builtin_memcpy(&__a, __mem + 16, _S_size<_Tp> - 16);
		const auto __c = _mm512_cvtepi8_epi32(__a);
		return _mm512_test_epi32_mask(__b, __b)
		       | (_mm512_test_epi32_mask(__c, __c) << 16);
	      }
	    else if constexpr (_S_size<_Tp> <= 64)
	      {
		__m128i __a = {};
		__builtin_memcpy(&__a, __mem, 16);
		const auto __b = _mm512_cvtepi8_epi32(__a);
		__builtin_memcpy(&__a, __mem + 16, 16);
		const auto __c = _mm512_cvtepi8_epi32(__a);
		if constexpr (_S_size<_Tp> <= 48)
		  {
		    __builtin_memcpy(&__a, __mem + 32, _S_size<_Tp> - 32);
		    const auto __d = _mm512_cvtepi8_epi32(__a);
		    return _mm512_test_epi32_mask(__b, __b)
			   | (_mm512_test_epi32_mask(__c, __c) << 16)
			   | (_ULLong(_mm512_test_epi32_mask(__d, __d)) << 32);
		  }
		else
		  {
		    __builtin_memcpy(&__a, __mem + 16, 16);
		    const auto __d = _mm512_cvtepi8_epi32(__a);
		    __builtin_memcpy(&__a, __mem + 32, _S_size<_Tp> - 48);
		    const auto __e = _mm512_cvtepi8_epi32(__a);
		    return _mm512_test_epi32_mask(__b, __b)
			   | (_mm512_test_epi32_mask(__c, __c) << 16)
			   | (_ULLong(_mm512_test_epi32_mask(__d, __d)) << 32)
			   | (_ULLong(_mm512_test_epi32_mask(__e, __e)) << 48);
		  }
	      }
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (sizeof(_Tp) == 8 && _S_size<_Tp> == 2)
	  return __vector_bitcast<_Tp>(
	    __vector_type16_t<int>{-int(__mem[0]), -int(__mem[0]),
				   -int(__mem[1]), -int(__mem[1])});
	else if constexpr (sizeof(_Tp) == 8 && _S_size<_Tp> <= 4 && __have_avx)
	  {
	    int __bool4 = 0;
	    __builtin_memcpy(&__bool4, __mem, _S_size<_Tp>);
	    const auto __k = __to_intrin(
	      (__vector_broadcast<4>(__bool4)
	       & __make_vector<int>(0x1, 0x100, 0x10000,
				    _S_size<_Tp> == 4 ? 0x1000000 : 0))
	      != 0);
	    return __vector_bitcast<_Tp>(
	      __concat(_mm_unpacklo_epi32(__k, __k),
		       _mm_unpackhi_epi32(__k, __k)));
	  }
	else if constexpr (sizeof(_Tp) == 4 && _S_size<_Tp> <= 4)
	  {
	    int __bools = 0;
	    __builtin_memcpy(&__bools, __mem, _S_size<_Tp>);
	    if constexpr (__have_sse2)
	      {
		__m128i __k = _mm_cvtsi32_si128(__bools);
		__k = _mm_cmpgt_epi16(_mm_unpacklo_epi8(__k, __k), __m128i());
		return __vector_bitcast<_Tp, _S_size<_Tp>>(
		  _mm_unpacklo_epi16(__k, __k));
	      }
	    else
	      {
		__m128 __k = _mm_cvtpi8_ps(_mm_cvtsi32_si64(__bools));
		_mm_empty();
		return __vector_bitcast<_Tp, _S_size<_Tp>>(
		  _mm_cmpgt_ps(__k, __m128()));
	      }
	  }
	else if constexpr (sizeof(_Tp) == 4 && _S_size<_Tp> <= 8)
	  {
	    __m128i __k = {};
	    __builtin_memcpy(&__k, __mem, _S_size<_Tp>);
	    __k = _mm_cmpgt_epi16(_mm_unpacklo_epi8(__k, __k), __m128i());
	    return __vector_bitcast<_Tp>(
	      __concat(_mm_unpacklo_epi16(__k, __k),
		       _mm_unpackhi_epi16(__k, __k)));
	  }
	else if constexpr (sizeof(_Tp) == 2 && _S_size<_Tp> <= 16)
	  {
	    __m128i __k = {};
	    __builtin_memcpy(&__k, __mem, _S_size<_Tp>);
	    __k = _mm_cmpgt_epi8(__k, __m128i());
	    if constexpr (_S_size<_Tp> <= 8)
	      return __vector_bitcast<_Tp, _S_size<_Tp>>(
		_mm_unpacklo_epi8(__k, __k));
	    else
	      return __concat(_mm_unpacklo_epi8(__k, __k),
			      _mm_unpackhi_epi8(__k, __k));
	  }
	else
	  return _Base::template _S_load<_Tp>(__mem);
      }

    // }}}
    // _S_from_bitmask{{{
    template <size_t _Np, typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
      _S_from_bitmask(_SanitizedBitMask<_Np> __bits, _TypeTag<_Tp>)
      {
	static_assert(is_same_v<_Tp, __int_for_sizeof_t<_Tp>>);
	if constexpr (__is_avx512_abi<_Abi>())
	  return __bits._M_to_bits();
	else
	  return _S_to_maskvector<_Tp, _S_size<_Tp>>(__bits);
      }

    // }}}
    // _S_masked_load {{{2
    template <typename _Tp, size_t _Np>
      static inline _SimdWrapper<_Tp, _Np>
      _S_masked_load(_SimdWrapper<_Tp, _Np> __merge,
		     _SimdWrapper<_Tp, _Np> __mask, const bool* __mem) noexcept
      {
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    if constexpr (__have_avx512bw_vl)
	      {
		if constexpr (_Np <= 16)
		  {
		    const auto __a
		      = _mm_mask_loadu_epi8(__m128i(), __mask, __mem);
		    return (__merge & ~__mask) | _mm_test_epi8_mask(__a, __a);
		  }
		else if constexpr (_Np <= 32)
		  {
		    const auto __a
		      = _mm256_mask_loadu_epi8(__m256i(), __mask, __mem);
		    return (__merge & ~__mask)
			   | _mm256_test_epi8_mask(__a, __a);
		  }
		else if constexpr (_Np <= 64)
		  {
		    const auto __a
		      = _mm512_mask_loadu_epi8(__m512i(), __mask, __mem);
		    return (__merge & ~__mask)
			   | _mm512_test_epi8_mask(__a, __a);
		  }
		else
		  __assert_unreachable<_Tp>();
	      }
	    else
	      {
		_BitOps::_S_bit_iteration(__mask, [&](auto __i) {
		  __merge._M_set(__i, __mem[__i]);
		});
		return __merge;
	      }
	  }
	else if constexpr (__have_avx512bw_vl && _Np == 32 && sizeof(_Tp) == 1)
	  {
	    const auto __k = _S_to_bits(__mask)._M_to_bits();
	    __merge = _mm256_mask_sub_epi8(__to_intrin(__merge), __k, __m256i(),
					   _mm256_mask_loadu_epi8(__m256i(),
								  __k, __mem));
	  }
	else if constexpr (__have_avx512bw_vl && _Np == 16 && sizeof(_Tp) == 1)
	  {
	    const auto __k = _S_to_bits(__mask)._M_to_bits();
	    __merge
	      = _mm_mask_sub_epi8(__vector_bitcast<_LLong>(__merge), __k,
				  __m128i(),
				  _mm_mask_loadu_epi8(__m128i(), __k, __mem));
	  }
	else if constexpr (__have_avx512bw_vl && _Np == 16 && sizeof(_Tp) == 2)
	  {
	    const auto __k = _S_to_bits(__mask)._M_to_bits();
	    __merge = _mm256_mask_sub_epi16(
	      __vector_bitcast<_LLong>(__merge), __k, __m256i(),
	      _mm256_cvtepi8_epi16(_mm_mask_loadu_epi8(__m128i(), __k, __mem)));
	  }
	else if constexpr (__have_avx512bw_vl && _Np == 8 && sizeof(_Tp) == 2)
	  {
	    const auto __k = _S_to_bits(__mask)._M_to_bits();
	    __merge = _mm_mask_sub_epi16(
	      __vector_bitcast<_LLong>(__merge), __k, __m128i(),
	      _mm_cvtepi8_epi16(_mm_mask_loadu_epi8(__m128i(), __k, __mem)));
	  }
	else if constexpr (__have_avx512bw_vl && _Np == 8 && sizeof(_Tp) == 4)
	  {
	    const auto __k = _S_to_bits(__mask)._M_to_bits();
	    __merge = __vector_bitcast<_Tp>(_mm256_mask_sub_epi32(
	      __vector_bitcast<_LLong>(__merge), __k, __m256i(),
	      _mm256_cvtepi8_epi32(
		_mm_mask_loadu_epi8(__m128i(), __k, __mem))));
	  }
	else if constexpr (__have_avx512bw_vl && _Np == 4 && sizeof(_Tp) == 4)
	  {
	    const auto __k = _S_to_bits(__mask)._M_to_bits();
	    __merge = __vector_bitcast<_Tp>(_mm_mask_sub_epi32(
	      __vector_bitcast<_LLong>(__merge), __k, __m128i(),
	      _mm_cvtepi8_epi32(_mm_mask_loadu_epi8(__m128i(), __k, __mem))));
	  }
	else if constexpr (__have_avx512bw_vl && _Np == 4 && sizeof(_Tp) == 8)
	  {
	    const auto __k = _S_to_bits(__mask)._M_to_bits();
	    __merge = __vector_bitcast<_Tp>(_mm256_mask_sub_epi64(
	      __vector_bitcast<_LLong>(__merge), __k, __m256i(),
	      _mm256_cvtepi8_epi64(
		_mm_mask_loadu_epi8(__m128i(), __k, __mem))));
	  }
	else if constexpr (__have_avx512bw_vl && _Np == 2 && sizeof(_Tp) == 8)
	  {
	    const auto __k = _S_to_bits(__mask)._M_to_bits();
	    __merge = __vector_bitcast<_Tp>(_mm_mask_sub_epi64(
	      __vector_bitcast<_LLong>(__merge), __k, __m128i(),
	      _mm_cvtepi8_epi64(_mm_mask_loadu_epi8(__m128i(), __k, __mem))));
	  }
	else
	  return _Base::_S_masked_load(__merge, __mask, __mem);
	return __merge;
      }

    // _S_store {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void _S_store(_SimdWrapper<_Tp, _Np> __v,
						   bool* __mem) noexcept
      {
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    if constexpr (__have_avx512bw_vl)
	      _CommonImplX86::_S_store<_Np>(
		__vector_bitcast<char>([](auto __data) {
		  if constexpr (_Np <= 16)
		    return _mm_maskz_set1_epi8(__data, 1);
		  else if constexpr (_Np <= 32)
		    return _mm256_maskz_set1_epi8(__data, 1);
		  else
		    return _mm512_maskz_set1_epi8(__data, 1);
		}(__v._M_data)),
		__mem);
	    else if constexpr (_Np <= 8)
	      _CommonImplX86::_S_store<_Np>(
		__vector_bitcast<char>(
#if defined __x86_64__
		  __make_wrapper<_ULLong>(
		    _pdep_u64(__v._M_data, 0x0101010101010101ULL), 0ull)
#else
		  __make_wrapper<_UInt>(_pdep_u32(__v._M_data, 0x01010101U),
					_pdep_u32(__v._M_data >> 4,
						  0x01010101U))
#endif
		    ),
		__mem);
	    else if constexpr (_Np <= 16)
	      _mm512_mask_cvtepi32_storeu_epi8(
		__mem, 0xffffu >> (16 - _Np),
		_mm512_maskz_set1_epi32(__v._M_data, 1));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (__is_sse_abi<_Abi>()) //{{{
	  {
	    if constexpr (_Np == 2 && sizeof(_Tp) == 8)
	      {
		const auto __k = __vector_bitcast<int>(__v);
		__mem[0] = -__k[1];
		__mem[1] = -__k[3];
	      }
	    else if constexpr (_Np <= 4 && sizeof(_Tp) == 4)
	      {
		if constexpr (__have_sse2)
		  {
		    const unsigned __bool4
		      = __vector_bitcast<_UInt>(_mm_packs_epi16(
			  _mm_packs_epi32(__intrin_bitcast<__m128i>(
					    __to_intrin(__v)),
					  __m128i()),
			  __m128i()))[0]
			& 0x01010101u;
		    __builtin_memcpy(__mem, &__bool4, _Np);
		  }
		else if constexpr (__have_mmx)
		  {
		    const __m64 __k = _mm_cvtps_pi8(
		      __and(__to_intrin(__v), _mm_set1_ps(1.f)));
		    __builtin_memcpy(__mem, &__k, _Np);
		    _mm_empty();
		  }
		else
		  return _Base::_S_store(__v, __mem);
	      }
	    else if constexpr (_Np <= 8 && sizeof(_Tp) == 2)
	      {
		_CommonImplX86::_S_store<_Np>(
		  __vector_bitcast<char>(_mm_packs_epi16(
		    __to_intrin(__vector_bitcast<_UShort>(__v) >> 15),
		    __m128i())),
		  __mem);
	      }
	    else if constexpr (_Np <= 16 && sizeof(_Tp) == 1)
	      _CommonImplX86::_S_store<_Np>(__v._M_data & 1, __mem);
	    else
	      __assert_unreachable<_Tp>();
	  }                                      // }}}
	else if constexpr (__is_avx_abi<_Abi>()) // {{{
	  {
	    if constexpr (_Np <= 4 && sizeof(_Tp) == 8)
	      {
		auto __k = __intrin_bitcast<__m256i>(__to_intrin(__v));
		int __bool4;
		if constexpr (__have_avx2)
		  __bool4 = _mm256_movemask_epi8(__k);
		else
		  __bool4 = (_mm_movemask_epi8(__lo128(__k))
			     | (_mm_movemask_epi8(__hi128(__k)) << 16));
		__bool4 &= 0x01010101;
		__builtin_memcpy(__mem, &__bool4, _Np);
	      }
	    else if constexpr (_Np <= 8 && sizeof(_Tp) == 4)
	      {
		const auto __k = __intrin_bitcast<__m256i>(__to_intrin(__v));
		const auto __k2
		  = _mm_srli_epi16(_mm_packs_epi16(__lo128(__k), __hi128(__k)),
				   15);
		const auto __k3
		  = __vector_bitcast<char>(_mm_packs_epi16(__k2, __m128i()));
		_CommonImplX86::_S_store<_Np>(__k3, __mem);
	      }
	    else if constexpr (_Np <= 16 && sizeof(_Tp) == 2)
	      {
		if constexpr (__have_avx2)
		  {
		    const auto __x = _mm256_srli_epi16(__to_intrin(__v), 15);
		    const auto __bools = __vector_bitcast<char>(
		      _mm_packs_epi16(__lo128(__x), __hi128(__x)));
		    _CommonImplX86::_S_store<_Np>(__bools, __mem);
		  }
		else
		  {
		    const auto __bools
		      = 1
			& __vector_bitcast<_UChar>(
			  _mm_packs_epi16(__lo128(__to_intrin(__v)),
					  __hi128(__to_intrin(__v))));
		    _CommonImplX86::_S_store<_Np>(__bools, __mem);
		  }
	      }
	    else if constexpr (_Np <= 32 && sizeof(_Tp) == 1)
	      _CommonImplX86::_S_store<_Np>(1 & __v._M_data, __mem);
	    else
	      __assert_unreachable<_Tp>();
	  } // }}}
	else
	  __assert_unreachable<_Tp>();
      }

    // _S_masked_store {{{2
    template <typename _Tp, size_t _Np>
      static inline void
      _S_masked_store(const _SimdWrapper<_Tp, _Np> __v, bool* __mem,
		      const _SimdWrapper<_Tp, _Np> __k) noexcept
      {
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    static_assert(is_same_v<_Tp, bool>);
	    if constexpr (_Np <= 16 && __have_avx512bw_vl)
	      _mm_mask_storeu_epi8(__mem, __k, _mm_maskz_set1_epi8(__v, 1));
	    else if constexpr (_Np <= 16)
	      _mm512_mask_cvtepi32_storeu_epi8(__mem, __k,
					       _mm512_maskz_set1_epi32(__v, 1));
	    else if constexpr (_Np <= 32 && __have_avx512bw_vl)
	      _mm256_mask_storeu_epi8(__mem, __k,
				      _mm256_maskz_set1_epi8(__v, 1));
	    else if constexpr (_Np <= 32 && __have_avx512bw)
	      _mm256_mask_storeu_epi8(__mem, __k,
				      __lo256(_mm512_maskz_set1_epi8(__v, 1)));
	    else if constexpr (_Np <= 64 && __have_avx512bw)
	      _mm512_mask_storeu_epi8(__mem, __k,
				      _mm512_maskz_set1_epi8(__v, 1));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  _Base::_S_masked_store(__v, __mem, __k);
      }

    // logical and bitwise operators {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_logical_and(const _SimdWrapper<_Tp, _Np>& __x,
		     const _SimdWrapper<_Tp, _Np>& __y)
      {
	if constexpr (is_same_v<_Tp, bool>)
	  {
	    if constexpr (__have_avx512dq && _Np <= 8)
	      return _kand_mask8(__x._M_data, __y._M_data);
	    else if constexpr (_Np <= 16)
	      return _kand_mask16(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 32)
	      return _kand_mask32(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 64)
	      return _kand_mask64(__x._M_data, __y._M_data);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_logical_and(__x, __y);
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_logical_or(const _SimdWrapper<_Tp, _Np>& __x,
		    const _SimdWrapper<_Tp, _Np>& __y)
      {
	if constexpr (is_same_v<_Tp, bool>)
	  {
	    if constexpr (__have_avx512dq && _Np <= 8)
	      return _kor_mask8(__x._M_data, __y._M_data);
	    else if constexpr (_Np <= 16)
	      return _kor_mask16(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 32)
	      return _kor_mask32(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 64)
	      return _kor_mask64(__x._M_data, __y._M_data);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_logical_or(__x, __y);
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_not(const _SimdWrapper<_Tp, _Np>& __x)
      {
	if constexpr (is_same_v<_Tp, bool>)
	  {
	    if constexpr (__have_avx512dq && _Np <= 8)
	      return _kandn_mask8(__x._M_data,
				  _Abi::template __implicit_mask_n<_Np>());
	    else if constexpr (_Np <= 16)
	      return _kandn_mask16(__x._M_data,
				   _Abi::template __implicit_mask_n<_Np>());
	    else if constexpr (__have_avx512bw && _Np <= 32)
	      return _kandn_mask32(__x._M_data,
				   _Abi::template __implicit_mask_n<_Np>());
	    else if constexpr (__have_avx512bw && _Np <= 64)
	      return _kandn_mask64(__x._M_data,
				   _Abi::template __implicit_mask_n<_Np>());
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_bit_not(__x);
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_and(const _SimdWrapper<_Tp, _Np>& __x,
		 const _SimdWrapper<_Tp, _Np>& __y)
      {
	if constexpr (is_same_v<_Tp, bool>)
	  {
	    if constexpr (__have_avx512dq && _Np <= 8)
	      return _kand_mask8(__x._M_data, __y._M_data);
	    else if constexpr (_Np <= 16)
	      return _kand_mask16(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 32)
	      return _kand_mask32(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 64)
	      return _kand_mask64(__x._M_data, __y._M_data);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_bit_and(__x, __y);
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_or(const _SimdWrapper<_Tp, _Np>& __x,
		const _SimdWrapper<_Tp, _Np>& __y)
      {
	if constexpr (is_same_v<_Tp, bool>)
	  {
	    if constexpr (__have_avx512dq && _Np <= 8)
	      return _kor_mask8(__x._M_data, __y._M_data);
	    else if constexpr (_Np <= 16)
	      return _kor_mask16(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 32)
	      return _kor_mask32(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 64)
	      return _kor_mask64(__x._M_data, __y._M_data);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_bit_or(__x, __y);
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_xor(const _SimdWrapper<_Tp, _Np>& __x,
		 const _SimdWrapper<_Tp, _Np>& __y)
      {
	if constexpr (is_same_v<_Tp, bool>)
	  {
	    if constexpr (__have_avx512dq && _Np <= 8)
	      return _kxor_mask8(__x._M_data, __y._M_data);
	    else if constexpr (_Np <= 16)
	      return _kxor_mask16(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 32)
	      return _kxor_mask32(__x._M_data, __y._M_data);
	    else if constexpr (__have_avx512bw && _Np <= 64)
	      return _kxor_mask64(__x._M_data, __y._M_data);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_bit_xor(__x, __y);
      }

    //}}}2
    // _S_masked_assign{{{
    template <size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_assign(_SimdWrapper<bool, _Np> __k,
		       _SimdWrapper<bool, _Np>& __lhs,
		       _SimdWrapper<bool, _Np> __rhs)
      {
	__lhs._M_data
	  = (~__k._M_data & __lhs._M_data) | (__k._M_data & __rhs._M_data);
      }

    template <size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_assign(_SimdWrapper<bool, _Np> __k,
		       _SimdWrapper<bool, _Np>& __lhs, bool __rhs)
      {
	if (__rhs)
	  __lhs._M_data = __k._M_data | __lhs._M_data;
	else
	  __lhs._M_data = ~__k._M_data & __lhs._M_data;
      }

    using _MaskImplBuiltin<_Abi>::_S_masked_assign;

    //}}}
    // _S_all_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_all_of(simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (__is_sse_abi<_Abi>() || __is_avx_abi<_Abi>())
	  {
	    constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	    using _TI = __intrinsic_type_t<_Tp, _Np>;
	    const _TI __a = reinterpret_cast<_TI>(__to_intrin(__data(__k)));
	    if constexpr (__have_sse4_1)
	      {
		_GLIBCXX_SIMD_USE_CONSTEXPR _TI __b
		  = _Abi::template _S_implicit_mask_intrin<_Tp>();
		return 0 != __testc(__a, __b);
	      }
	    else if constexpr (is_same_v<_Tp, float>)
	      return (_mm_movemask_ps(__a) & ((1 << _Np) - 1))
		     == (1 << _Np) - 1;
	    else if constexpr (is_same_v<_Tp, double>)
	      return (_mm_movemask_pd(__a) & ((1 << _Np) - 1))
		     == (1 << _Np) - 1;
	    else
	      return (_mm_movemask_epi8(__a) & ((1 << (_Np * sizeof(_Tp))) - 1))
		     == (1 << (_Np * sizeof(_Tp))) - 1;
	  }
	else if constexpr (__is_avx512_abi<_Abi>())
	  {
	    constexpr auto _Mask = _Abi::template _S_implicit_mask<_Tp>();
	    const auto __kk = __k._M_data._M_data;
	    if constexpr (sizeof(__kk) == 1)
	      {
		if constexpr (__have_avx512dq)
		  return _kortestc_mask8_u8(__kk, _Mask == 0xff
						    ? __kk
						    : __mmask8(~_Mask));
		else
		  return _kortestc_mask16_u8(__kk, __mmask16(~_Mask));
	      }
	    else if constexpr (sizeof(__kk) == 2)
	      return _kortestc_mask16_u8(__kk, _Mask == 0xffff
						 ? __kk
						 : __mmask16(~_Mask));
	    else if constexpr (sizeof(__kk) == 4 && __have_avx512bw)
	      return _kortestc_mask32_u8(__kk, _Mask == 0xffffffffU
						 ? __kk
						 : __mmask32(~_Mask));
	    else if constexpr (sizeof(__kk) == 8 && __have_avx512bw)
	      return _kortestc_mask64_u8(__kk, _Mask == 0xffffffffffffffffULL
						 ? __kk
						 : __mmask64(~_Mask));
	    else
	      __assert_unreachable<_Tp>();
	  }
      }

    // }}}
    // _S_any_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_any_of(simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (__is_sse_abi<_Abi>() || __is_avx_abi<_Abi>())
	  {
	    constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	    using _TI = __intrinsic_type_t<_Tp, _Np>;
	    const _TI __a = reinterpret_cast<_TI>(__to_intrin(__data(__k)));
	    if constexpr (__have_sse4_1)
	      {
		if constexpr (_Abi::template _S_is_partial<
				_Tp> || sizeof(__k) < 16)
		  {
		    _GLIBCXX_SIMD_USE_CONSTEXPR _TI __b
		      = _Abi::template _S_implicit_mask_intrin<_Tp>();
		    return 0 == __testz(__a, __b);
		  }
		else
		  return 0 == __testz(__a, __a);
	      }
	    else if constexpr (is_same_v<_Tp, float>)
	      return (_mm_movemask_ps(__a) & ((1 << _Np) - 1)) != 0;
	    else if constexpr (is_same_v<_Tp, double>)
	      return (_mm_movemask_pd(__a) & ((1 << _Np) - 1)) != 0;
	    else
	      return (_mm_movemask_epi8(__a) & ((1 << (_Np * sizeof(_Tp))) - 1))
		     != 0;
	  }
	else if constexpr (__is_avx512_abi<_Abi>())
	  return (__k._M_data._M_data & _Abi::template _S_implicit_mask<_Tp>())
		 != 0;
      }

    // }}}
    // _S_none_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_none_of(simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (__is_sse_abi<_Abi>() || __is_avx_abi<_Abi>())
	  {
	    constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	    using _TI = __intrinsic_type_t<_Tp, _Np>;
	    const _TI __a = reinterpret_cast<_TI>(__to_intrin(__data(__k)));
	    if constexpr (__have_sse4_1)
	      {
		if constexpr (_Abi::template _S_is_partial<
				_Tp> || sizeof(__k) < 16)
		  {
		    _GLIBCXX_SIMD_USE_CONSTEXPR _TI __b
		      = _Abi::template _S_implicit_mask_intrin<_Tp>();
		    return 0 != __testz(__a, __b);
		  }
		else
		  return 0 != __testz(__a, __a);
	      }
	    else if constexpr (is_same_v<_Tp, float>)
	      return (__movemask(__a) & ((1 << _Np) - 1)) == 0;
	    else if constexpr (is_same_v<_Tp, double>)
	      return (__movemask(__a) & ((1 << _Np) - 1)) == 0;
	    else
	      return (__movemask(__a) & int((1ull << (_Np * sizeof(_Tp))) - 1))
		     == 0;
	  }
	else if constexpr (__is_avx512_abi<_Abi>())
	  return (__k._M_data._M_data & _Abi::template _S_implicit_mask<_Tp>())
		 == 0;
      }

    // }}}
    // _S_some_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_some_of(simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (__is_sse_abi<_Abi>() || __is_avx_abi<_Abi>())
	  {
	    constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	    using _TI = __intrinsic_type_t<_Tp, _Np>;
	    const _TI __a = reinterpret_cast<_TI>(__to_intrin(__data(__k)));
	    if constexpr (__have_sse4_1)
	      {
		_GLIBCXX_SIMD_USE_CONSTEXPR _TI __b
		  = _Abi::template _S_implicit_mask_intrin<_Tp>();
		return 0 != __testnzc(__a, __b);
	      }
	    else if constexpr (is_same_v<_Tp, float>)
	      {
		constexpr int __allbits = (1 << _Np) - 1;
		const auto __tmp = _mm_movemask_ps(__a) & __allbits;
		return __tmp > 0 && __tmp < __allbits;
	      }
	    else if constexpr (is_same_v<_Tp, double>)
	      {
		constexpr int __allbits = (1 << _Np) - 1;
		const auto __tmp = _mm_movemask_pd(__a) & __allbits;
		return __tmp > 0 && __tmp < __allbits;
	      }
	    else
	      {
		constexpr int __allbits = (1 << (_Np * sizeof(_Tp))) - 1;
		const auto __tmp = _mm_movemask_epi8(__a) & __allbits;
		return __tmp > 0 && __tmp < __allbits;
	      }
	  }
	else if constexpr (__is_avx512_abi<_Abi>())
	  return _S_any_of(__k) && !_S_all_of(__k);
	else
	  __assert_unreachable<_Tp>();
      }

    // }}}
    // _S_popcount {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int _S_popcount(simd_mask<_Tp, _Abi> __k)
      {
	constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	const auto __kk = _Abi::_S_masked(__k._M_data)._M_data;
	if constexpr (__is_avx512_abi<_Abi>())
	  {
	    if constexpr (_Np > 32)
	      return __builtin_popcountll(__kk);
	    else
	      return __builtin_popcount(__kk);
	  }
	else
	  {
	    if constexpr (__have_popcnt)
	      {
		int __bits
		  = __movemask(__to_intrin(__vector_bitcast<_Tp>(__kk)));
		const int __count = __builtin_popcount(__bits);
		return is_integral_v<_Tp> ? __count / sizeof(_Tp) : __count;
	      }
	    else if constexpr (_Np == 2 && sizeof(_Tp) == 8)
	      {
		const int mask = _mm_movemask_pd(__auto_bitcast(__kk));
		return mask - (mask >> 1);
	      }
	    else if constexpr (_Np <= 4 && sizeof(_Tp) == 8)
	      {
		auto __x = -(__lo128(__kk) + __hi128(__kk));
		return __x[0] + __x[1];
	      }
	    else if constexpr (_Np <= 4 && sizeof(_Tp) == 4)
	      {
		if constexpr (__have_sse2)
		  {
		    __m128i __x = __intrin_bitcast<__m128i>(__to_intrin(__kk));
		    __x = _mm_add_epi32(
		      __x, _mm_shuffle_epi32(__x, _MM_SHUFFLE(0, 1, 2, 3)));
		    __x = _mm_add_epi32(
		      __x, _mm_shufflelo_epi16(__x, _MM_SHUFFLE(1, 0, 3, 2)));
		    return -_mm_cvtsi128_si32(__x);
		  }
		else
		  return __builtin_popcount(
		    _mm_movemask_ps(__auto_bitcast(__kk)));
	      }
	    else if constexpr (_Np <= 8 && sizeof(_Tp) == 2)
	      {
		auto __x = __to_intrin(__kk);
		__x = _mm_add_epi16(__x,
				    _mm_shuffle_epi32(__x,
						      _MM_SHUFFLE(0, 1, 2, 3)));
		__x = _mm_add_epi16(
		  __x, _mm_shufflelo_epi16(__x, _MM_SHUFFLE(0, 1, 2, 3)));
		__x = _mm_add_epi16(
		  __x, _mm_shufflelo_epi16(__x, _MM_SHUFFLE(2, 3, 0, 1)));
		return -short(_mm_extract_epi16(__x, 0));
	      }
	    else if constexpr (_Np <= 16 && sizeof(_Tp) == 1)
	      {
		auto __x = __to_intrin(__kk);
		__x = _mm_add_epi8(__x,
				   _mm_shuffle_epi32(__x,
						     _MM_SHUFFLE(0, 1, 2, 3)));
		__x = _mm_add_epi8(__x,
				   _mm_shufflelo_epi16(__x, _MM_SHUFFLE(0, 1, 2,
									3)));
		__x = _mm_add_epi8(__x,
				   _mm_shufflelo_epi16(__x, _MM_SHUFFLE(2, 3, 0,
									1)));
		auto __y = -__vector_bitcast<_UChar>(__x);
		if constexpr (__have_sse4_1)
		  return __y[0] + __y[1];
		else
		  {
		    unsigned __z = _mm_extract_epi16(__to_intrin(__y), 0);
		    return (__z & 0xff) + (__z >> 8);
		  }
	      }
	    else if constexpr (sizeof(__kk) == 32)
	      {
		// The following works only as long as the implementations above
		// use a summation
		using _I = __int_for_sizeof_t<_Tp>;
		const auto __as_int = __vector_bitcast<_I>(__kk);
		_MaskImplX86<simd_abi::__sse>::_S_popcount(
		  simd_mask<_I, simd_abi::__sse>(__private_init,
						 __lo128(__as_int)
						   + __hi128(__as_int)));
	      }
	    else
	      __assert_unreachable<_Tp>();
	  }
      }

    // }}}
    // _S_find_first_set {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_find_first_set(simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (__is_avx512_abi<_Abi>())
	  return std::__countr_zero(__k._M_data._M_data);
	else
	  return _Base::_S_find_first_set(__k);
      }

    // }}}
    // _S_find_last_set {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_find_last_set(simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (__is_avx512_abi<_Abi>())
	  return std::__bit_width(__k._M_data._M_data) - 1;
	else
	  return _Base::_S_find_last_set(__k);
      }

    // }}}
  };

// }}}

_GLIBCXX_SIMD_END_NAMESPACE
#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_X86_H_

// vim: foldmethod=marker sw=2 noet ts=8 sts=2 tw=80
