// Simd Abi specific implementations -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_ABIS_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_ABIS_H_

#if __cplusplus >= 201703L

#include <array>
#include <cmath>
#include <cstdlib>

_GLIBCXX_SIMD_BEGIN_NAMESPACE
// _S_allbits{{{
template <typename _V>
  static inline _GLIBCXX_SIMD_USE_CONSTEXPR _V _S_allbits
    = reinterpret_cast<_V>(~__vector_type_t<char, sizeof(_V) / sizeof(char)>());

// }}}
// _S_signmask, _S_absmask{{{
template <typename _V, typename = _VectorTraits<_V>>
  static inline _GLIBCXX_SIMD_USE_CONSTEXPR _V _S_signmask
    = __xor(_V() + 1, _V() - 1);

template <typename _V, typename = _VectorTraits<_V>>
  static inline _GLIBCXX_SIMD_USE_CONSTEXPR _V _S_absmask
    = __andnot(_S_signmask<_V>, _S_allbits<_V>);

//}}}
// __vector_permute<Indices...>{{{
// Index == -1 requests zeroing of the output element
template <int... _Indices, typename _Tp, typename _TVT = _VectorTraits<_Tp>,
	  typename = __detail::__odr_helper>
  _Tp
  __vector_permute(_Tp __x)
  {
    static_assert(sizeof...(_Indices) == _TVT::_S_full_size);
    return __make_vector<typename _TVT::value_type>(
      (_Indices == -1 ? 0 : __x[_Indices == -1 ? 0 : _Indices])...);
  }

// }}}
// __vector_shuffle<Indices...>{{{
// Index == -1 requests zeroing of the output element
template <int... _Indices, typename _Tp, typename _TVT = _VectorTraits<_Tp>,
	  typename = __detail::__odr_helper>
  _Tp
  __vector_shuffle(_Tp __x, _Tp __y)
  {
    return _Tp{(_Indices == -1 ? 0
		: _Indices < _TVT::_S_full_size
		  ? __x[_Indices]
		  : __y[_Indices - _TVT::_S_full_size])...};
  }

// }}}
// __make_wrapper{{{
template <typename _Tp, typename... _Args>
  _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper<_Tp, sizeof...(_Args)>
  __make_wrapper(const _Args&... __args)
  { return __make_vector<_Tp>(__args...); }

// }}}
// __wrapper_bitcast{{{
template <typename _Tp, size_t _ToN = 0, typename _Up, size_t _M,
	  size_t _Np = _ToN != 0 ? _ToN : sizeof(_Up) * _M / sizeof(_Tp)>
  _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper<_Tp, _Np>
  __wrapper_bitcast(_SimdWrapper<_Up, _M> __x)
  {
    static_assert(_Np > 1);
    return __intrin_bitcast<__vector_type_t<_Tp, _Np>>(__x._M_data);
  }

// }}}
// __shift_elements_right{{{
// if (__shift % 2ⁿ == 0) => the low n Bytes are correct
template <unsigned __shift, typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC _Tp
  __shift_elements_right(_Tp __v)
  {
    [[maybe_unused]] const auto __iv = __to_intrin(__v);
    static_assert(__shift <= sizeof(_Tp));
    if constexpr (__shift == 0)
      return __v;
    else if constexpr (__shift == sizeof(_Tp))
      return _Tp();
#if _GLIBCXX_SIMD_X86INTRIN // {{{
    else if constexpr (__have_sse && __shift == 8
		       && _TVT::template _S_is<float, 4>)
      return _mm_movehl_ps(__iv, __iv);
    else if constexpr (__have_sse2 && __shift == 8
		       && _TVT::template _S_is<double, 2>)
      return _mm_unpackhi_pd(__iv, __iv);
    else if constexpr (__have_sse2 && sizeof(_Tp) == 16)
      return reinterpret_cast<typename _TVT::type>(
	_mm_srli_si128(reinterpret_cast<__m128i>(__iv), __shift));
    else if constexpr (__shift == 16 && sizeof(_Tp) == 32)
      {
	/*if constexpr (__have_avx && _TVT::template _S_is<double, 4>)
	  return _mm256_permute2f128_pd(__iv, __iv, 0x81);
	else if constexpr (__have_avx && _TVT::template _S_is<float, 8>)
	  return _mm256_permute2f128_ps(__iv, __iv, 0x81);
	else if constexpr (__have_avx)
	  return reinterpret_cast<typename _TVT::type>(
	    _mm256_permute2f128_si256(__iv, __iv, 0x81));
	else*/
	return __zero_extend(__hi128(__v));
      }
    else if constexpr (__have_avx2 && sizeof(_Tp) == 32 && __shift < 16)
      {
	const auto __vll = __vector_bitcast<_LLong>(__v);
	return reinterpret_cast<typename _TVT::type>(
	  _mm256_alignr_epi8(_mm256_permute2x128_si256(__vll, __vll, 0x81),
			     __vll, __shift));
      }
    else if constexpr (__have_avx && sizeof(_Tp) == 32 && __shift < 16)
      {
	const auto __vll = __vector_bitcast<_LLong>(__v);
	return reinterpret_cast<typename _TVT::type>(
	  __concat(_mm_alignr_epi8(__hi128(__vll), __lo128(__vll), __shift),
		   _mm_srli_si128(__hi128(__vll), __shift)));
      }
    else if constexpr (sizeof(_Tp) == 32 && __shift > 16)
      return __zero_extend(__shift_elements_right<__shift - 16>(__hi128(__v)));
    else if constexpr (sizeof(_Tp) == 64 && __shift == 32)
      return __zero_extend(__hi256(__v));
    else if constexpr (__have_avx512f && sizeof(_Tp) == 64)
      {
	if constexpr (__shift >= 48)
	  return __zero_extend(
	    __shift_elements_right<__shift - 48>(__extract<3, 4>(__v)));
	else if constexpr (__shift >= 32)
	  return __zero_extend(
	    __shift_elements_right<__shift - 32>(__hi256(__v)));
	else if constexpr (__shift % 8 == 0)
	  return reinterpret_cast<typename _TVT::type>(
	    _mm512_alignr_epi64(__m512i(), __intrin_bitcast<__m512i>(__v),
				__shift / 8));
	else if constexpr (__shift % 4 == 0)
	  return reinterpret_cast<typename _TVT::type>(
	    _mm512_alignr_epi32(__m512i(), __intrin_bitcast<__m512i>(__v),
				__shift / 4));
	else if constexpr (__have_avx512bw && __shift < 16)
	  {
	    const auto __vll = __vector_bitcast<_LLong>(__v);
	    return reinterpret_cast<typename _TVT::type>(
	      _mm512_alignr_epi8(_mm512_shuffle_i32x4(__vll, __vll, 0xf9),
				 __vll, __shift));
	  }
	else if constexpr (__have_avx512bw && __shift < 32)
	  {
	    const auto __vll = __vector_bitcast<_LLong>(__v);
	    return reinterpret_cast<typename _TVT::type>(
	      _mm512_alignr_epi8(_mm512_shuffle_i32x4(__vll, __m512i(), 0xee),
				 _mm512_shuffle_i32x4(__vll, __vll, 0xf9),
				 __shift - 16));
	  }
	else
	  __assert_unreachable<_Tp>();
      }
  /*
      } else if constexpr (__shift % 16 == 0 && sizeof(_Tp) == 64)
	  return __auto_bitcast(__extract<__shift / 16, 4>(__v));
  */
#endif // _GLIBCXX_SIMD_X86INTRIN }}}
    else
      {
	constexpr int __chunksize = __shift % 8 == 0   ? 8
				    : __shift % 4 == 0 ? 4
				    : __shift % 2 == 0 ? 2
						       : 1;
	auto __w = __vector_bitcast<__int_with_sizeof_t<__chunksize>>(__v);
	using _Up = decltype(__w);
	return __intrin_bitcast<_Tp>(
	  __call_with_n_evaluations<(sizeof(_Tp) - __shift) / __chunksize>(
	    [](auto... __chunks) { return _Up{__chunks...}; },
	    [&](auto __i) { return __w[__shift / __chunksize + __i]; }));
      }
  }

// }}}
// __extract_part(_SimdWrapper<_Tp, _Np>) {{{
template <int _Index, int _Total, int _Combine, typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_CONST
  _SimdWrapper<_Tp, _Np / _Total * _Combine>
  __extract_part(const _SimdWrapper<_Tp, _Np> __x)
  {
    if constexpr (_Index % 2 == 0 && _Total % 2 == 0 && _Combine % 2 == 0)
      return __extract_part<_Index / 2, _Total / 2, _Combine / 2>(__x);
    else
      {
	constexpr size_t __values_per_part = _Np / _Total;
	constexpr size_t __values_to_skip = _Index * __values_per_part;
	constexpr size_t __return_size = __values_per_part * _Combine;
	using _R = __vector_type_t<_Tp, __return_size>;
	static_assert((_Index + _Combine) * __values_per_part * sizeof(_Tp)
			<= sizeof(__x),
		      "out of bounds __extract_part");
	// the following assertion would ensure no "padding" to be read
	// static_assert(_Total >= _Index + _Combine, "_Total must be greater
	// than _Index");

	// static_assert(__return_size * _Total == _Np, "_Np must be divisible
	// by _Total");
	if (__x._M_is_constprop())
	  return __generate_from_n_evaluations<__return_size, _R>(
	    [&](auto __i) { return __x[__values_to_skip + __i]; });
	if constexpr (_Index == 0 && _Total == 1)
	  return __x;
	else if constexpr (_Index == 0)
	  return __intrin_bitcast<_R>(__as_vector(__x));
#if _GLIBCXX_SIMD_X86INTRIN // {{{
	else if constexpr (sizeof(__x) == 32
			   && __return_size * sizeof(_Tp) <= 16)
	  {
	    constexpr size_t __bytes_to_skip = __values_to_skip * sizeof(_Tp);
	    if constexpr (__bytes_to_skip == 16)
	      return __vector_bitcast<_Tp, __return_size>(
		__hi128(__as_vector(__x)));
	    else
	      return __vector_bitcast<_Tp, __return_size>(
		_mm_alignr_epi8(__hi128(__vector_bitcast<_LLong>(__x)),
				__lo128(__vector_bitcast<_LLong>(__x)),
				__bytes_to_skip));
	  }
#endif // _GLIBCXX_SIMD_X86INTRIN }}}
	else if constexpr (_Index > 0
			   && (__values_to_skip % __return_size != 0
			       || sizeof(_R) >= 8)
			   && (__values_to_skip + __return_size) * sizeof(_Tp)
				<= 64
			   && sizeof(__x) >= 16)
	  return __intrin_bitcast<_R>(
	    __shift_elements_right<__values_to_skip * sizeof(_Tp)>(
	      __as_vector(__x)));
	else
	  {
	    _R __r = {};
	    __builtin_memcpy(&__r,
			     reinterpret_cast<const char*>(&__x)
			       + sizeof(_Tp) * __values_to_skip,
			     __return_size * sizeof(_Tp));
	    return __r;
	  }
      }
  }

// }}}
// __extract_part(_SimdWrapper<bool, _Np>) {{{
template <int _Index, int _Total, int _Combine = 1, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper<bool, _Np / _Total * _Combine>
  __extract_part(const _SimdWrapper<bool, _Np> __x)
  {
    static_assert(_Combine == 1, "_Combine != 1 not implemented");
    static_assert(__have_avx512f && _Np == _Np);
    static_assert(_Total >= 2 && _Index + _Combine <= _Total && _Index >= 0);
    return __x._M_data >> (_Index * _Np / _Total);
  }

// }}}

// __vector_convert {{{
// implementation requires an index sequence
template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d,
		   index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, _From __i,
		   index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...,
	       static_cast<_Tp>(__i[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, _From __i, _From __j,
		   index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...,
	       static_cast<_Tp>(__i[_I])..., static_cast<_Tp>(__j[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, _From __i, _From __j,
		   _From __k, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...,
	       static_cast<_Tp>(__i[_I])..., static_cast<_Tp>(__j[_I])...,
	       static_cast<_Tp>(__k[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, _From __i, _From __j,
		   _From __k, _From __l, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...,
	       static_cast<_Tp>(__i[_I])..., static_cast<_Tp>(__j[_I])...,
	       static_cast<_Tp>(__k[_I])..., static_cast<_Tp>(__l[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, _From __i, _From __j,
		   _From __k, _From __l, _From __m, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...,
	       static_cast<_Tp>(__i[_I])..., static_cast<_Tp>(__j[_I])...,
	       static_cast<_Tp>(__k[_I])..., static_cast<_Tp>(__l[_I])...,
	       static_cast<_Tp>(__m[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, _From __i, _From __j,
		   _From __k, _From __l, _From __m, _From __n,
		   index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...,
	       static_cast<_Tp>(__i[_I])..., static_cast<_Tp>(__j[_I])...,
	       static_cast<_Tp>(__k[_I])..., static_cast<_Tp>(__l[_I])...,
	       static_cast<_Tp>(__m[_I])..., static_cast<_Tp>(__n[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, _From __i, _From __j,
		   _From __k, _From __l, _From __m, _From __n, _From __o,
		   index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...,
	       static_cast<_Tp>(__i[_I])..., static_cast<_Tp>(__j[_I])...,
	       static_cast<_Tp>(__k[_I])..., static_cast<_Tp>(__l[_I])...,
	       static_cast<_Tp>(__m[_I])..., static_cast<_Tp>(__n[_I])...,
	       static_cast<_Tp>(__o[_I])...};
  }

template <typename _To, typename _From, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_From __a, _From __b, _From __c, _From __d, _From __e,
		   _From __f, _From __g, _From __h, _From __i, _From __j,
		   _From __k, _From __l, _From __m, _From __n, _From __o,
		   _From __p, index_sequence<_I...>)
  {
    using _Tp = typename _VectorTraits<_To>::value_type;
    return _To{static_cast<_Tp>(__a[_I])..., static_cast<_Tp>(__b[_I])...,
	       static_cast<_Tp>(__c[_I])..., static_cast<_Tp>(__d[_I])...,
	       static_cast<_Tp>(__e[_I])..., static_cast<_Tp>(__f[_I])...,
	       static_cast<_Tp>(__g[_I])..., static_cast<_Tp>(__h[_I])...,
	       static_cast<_Tp>(__i[_I])..., static_cast<_Tp>(__j[_I])...,
	       static_cast<_Tp>(__k[_I])..., static_cast<_Tp>(__l[_I])...,
	       static_cast<_Tp>(__m[_I])..., static_cast<_Tp>(__n[_I])...,
	       static_cast<_Tp>(__o[_I])..., static_cast<_Tp>(__p[_I])...};
  }

// Defer actual conversion to the overload that takes an index sequence. Note
// that this function adds zeros or drops values off the end if you don't ensure
// matching width.
template <typename _To, typename... _From, size_t _FromSize>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __vector_convert(_SimdWrapper<_From, _FromSize>... __xs)
  {
#ifdef _GLIBCXX_SIMD_WORKAROUND_PR85048
    using _From0 = __first_of_pack_t<_From...>;
    using _FW = _SimdWrapper<_From0, _FromSize>;
    if (!_FW::_S_is_partial && !(... && __xs._M_is_constprop()))
      {
	if constexpr ((sizeof...(_From) & (sizeof...(_From) - 1))
		      == 0) // power-of-two number of arguments
	  return __convert_x86<_To>(__as_vector(__xs)...);
	else // append zeros and recurse until the above branch is taken
	  return __vector_convert<_To>(__xs..., _FW{});
      }
    else
#endif
      return __vector_convert<_To>(
	__as_vector(__xs)...,
	make_index_sequence<(sizeof...(__xs) == 1 ? std::min(
			       _VectorTraits<_To>::_S_full_size, int(_FromSize))
						  : _FromSize)>());
  }

// }}}
// __convert function{{{
template <typename _To, typename _From, typename... _More>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __convert(_From __v0, _More... __vs)
  {
    static_assert((true && ... && is_same_v<_From, _More>) );
    if constexpr (__is_vectorizable_v<_From>)
      {
	using _V = typename _VectorTraits<_To>::type;
	using _Tp = typename _VectorTraits<_To>::value_type;
	return _V{static_cast<_Tp>(__v0), static_cast<_Tp>(__vs)...};
      }
    else if constexpr (__is_vector_type_v<_From>)
      return __convert<_To>(__as_wrapper(__v0), __as_wrapper(__vs)...);
    else // _SimdWrapper arguments
      {
	constexpr size_t __input_size = _From::_S_size * (1 + sizeof...(_More));
	if constexpr (__is_vectorizable_v<_To>)
	  return __convert<__vector_type_t<_To, __input_size>>(__v0, __vs...);
	else if constexpr (!__is_vector_type_v<_To>)
	  return _To(__convert<typename _To::_BuiltinType>(__v0, __vs...));
	else
	  {
	    static_assert(
	      sizeof...(_More) == 0
		|| _VectorTraits<_To>::_S_full_size >= __input_size,
	      "__convert(...) requires the input to fit into the output");
	    return __vector_convert<_To>(__v0, __vs...);
	  }
      }
  }

// }}}
// __convert_all{{{
// Converts __v into array<_To, N>, where N is _NParts if non-zero or
// otherwise deduced from _To such that N * #elements(_To) <= #elements(__v).
// Note: this function may return less than all converted elements
template <typename _To,
	  size_t _NParts = 0, // allows to convert fewer or more (only last
			      // _To, to be partially filled) than all
	  size_t _Offset = 0, // where to start, # of elements (not Bytes or
			      // Parts)
	  typename _From, typename _FromVT = _VectorTraits<_From>>
  _GLIBCXX_SIMD_INTRINSIC auto
  __convert_all(_From __v)
  {
    if constexpr (is_arithmetic_v<_To> && _NParts != 1)
      {
	static_assert(_Offset < _FromVT::_S_full_size);
	constexpr auto _Np
	  = _NParts == 0 ? _FromVT::_S_partial_width - _Offset : _NParts;
	return __generate_from_n_evaluations<_Np, array<_To, _Np>>(
	  [&](auto __i) { return static_cast<_To>(__v[__i + _Offset]); });
      }
    else
      {
	static_assert(__is_vector_type_v<_To>);
	using _ToVT = _VectorTraits<_To>;
	if constexpr (__is_vector_type_v<_From>)
	  return __convert_all<_To, _NParts>(__as_wrapper(__v));
	else if constexpr (_NParts == 1)
	  {
	    static_assert(_Offset % _ToVT::_S_full_size == 0);
	    return array<_To, 1>{__vector_convert<_To>(
	      __extract_part<_Offset / _ToVT::_S_full_size,
			     __div_roundup(_FromVT::_S_partial_width,
					   _ToVT::_S_full_size)>(__v))};
	  }
#if _GLIBCXX_SIMD_X86INTRIN // {{{
	else if constexpr (!__have_sse4_1 && _Offset == 0
	  && is_integral_v<typename _FromVT::value_type>
	  && sizeof(typename _FromVT::value_type)
	      < sizeof(typename _ToVT::value_type)
	  && !(sizeof(typename _FromVT::value_type) == 4
	      && is_same_v<typename _ToVT::value_type, double>))
	  {
	    using _ToT = typename _ToVT::value_type;
	    using _FromT = typename _FromVT::value_type;
	    constexpr size_t _Np
	      = _NParts != 0
		  ? _NParts
		  : (_FromVT::_S_partial_width / _ToVT::_S_full_size);
	    using _R = array<_To, _Np>;
	    // __adjust modifies its input to have _Np (use _SizeConstant)
	    // entries so that no unnecessary intermediate conversions are
	    // requested and, more importantly, no intermediate conversions are
	    // missing
	    [[maybe_unused]] auto __adjust
	      = [](auto __n,
		   auto __vv) -> _SimdWrapper<_FromT, decltype(__n)::value> {
	      return __vector_bitcast<_FromT, decltype(__n)::value>(__vv);
	    };
	    [[maybe_unused]] const auto __vi = __to_intrin(__v);
	    auto&& __make_array = [](auto __x0, [[maybe_unused]] auto __x1) {
	      if constexpr (_Np == 1)
		return _R{__intrin_bitcast<_To>(__x0)};
	      else
		return _R{__intrin_bitcast<_To>(__x0),
			  __intrin_bitcast<_To>(__x1)};
	    };

	    if constexpr (_Np == 0)
	      return _R{};
	    else if constexpr (sizeof(_FromT) == 1 && sizeof(_ToT) == 2)
	      {
		static_assert(is_integral_v<_FromT>);
		static_assert(is_integral_v<_ToT>);
		if constexpr (is_unsigned_v<_FromT>)
		  return __make_array(_mm_unpacklo_epi8(__vi, __m128i()),
				      _mm_unpackhi_epi8(__vi, __m128i()));
		else
		  return __make_array(
		    _mm_srai_epi16(_mm_unpacklo_epi8(__vi, __vi), 8),
		    _mm_srai_epi16(_mm_unpackhi_epi8(__vi, __vi), 8));
	      }
	    else if constexpr (sizeof(_FromT) == 2 && sizeof(_ToT) == 4)
	      {
		static_assert(is_integral_v<_FromT>);
		if constexpr (is_floating_point_v<_ToT>)
		  {
		    const auto __ints
		      = __convert_all<__vector_type16_t<int>, _Np>(
			__adjust(_SizeConstant<_Np * 4>(), __v));
		    return __generate_from_n_evaluations<_Np, _R>(
		      [&](auto __i) {
			return __vector_convert<_To>(__as_wrapper(__ints[__i]));
		      });
		  }
		else if constexpr (is_unsigned_v<_FromT>)
		  return __make_array(_mm_unpacklo_epi16(__vi, __m128i()),
				      _mm_unpackhi_epi16(__vi, __m128i()));
		else
		  return __make_array(
		    _mm_srai_epi32(_mm_unpacklo_epi16(__vi, __vi), 16),
		    _mm_srai_epi32(_mm_unpackhi_epi16(__vi, __vi), 16));
	      }
	    else if constexpr (sizeof(_FromT) == 4 && sizeof(_ToT) == 8
			       && is_integral_v<_FromT> && is_integral_v<_ToT>)
	      {
		if constexpr (is_unsigned_v<_FromT>)
		  return __make_array(_mm_unpacklo_epi32(__vi, __m128i()),
				      _mm_unpackhi_epi32(__vi, __m128i()));
		else
		  return __make_array(
		    _mm_unpacklo_epi32(__vi, _mm_srai_epi32(__vi, 31)),
		    _mm_unpackhi_epi32(__vi, _mm_srai_epi32(__vi, 31)));
	      }
	    else if constexpr (sizeof(_FromT) == 4 && sizeof(_ToT) == 8
			       && is_integral_v<_FromT> && is_integral_v<_ToT>)
	      {
		if constexpr (is_unsigned_v<_FromT>)
		  return __make_array(_mm_unpacklo_epi32(__vi, __m128i()),
				      _mm_unpackhi_epi32(__vi, __m128i()));
		else
		  return __make_array(
		    _mm_unpacklo_epi32(__vi, _mm_srai_epi32(__vi, 31)),
		    _mm_unpackhi_epi32(__vi, _mm_srai_epi32(__vi, 31)));
	      }
	    else if constexpr (sizeof(_FromT) == 1 && sizeof(_ToT) >= 4
			       && is_signed_v<_FromT>)
	      {
		const __m128i __vv[2] = {_mm_unpacklo_epi8(__vi, __vi),
					 _mm_unpackhi_epi8(__vi, __vi)};
		const __vector_type_t<int, 4> __vvvv[4] = {
		  __vector_bitcast<int>(_mm_unpacklo_epi16(__vv[0], __vv[0])),
		  __vector_bitcast<int>(_mm_unpackhi_epi16(__vv[0], __vv[0])),
		  __vector_bitcast<int>(_mm_unpacklo_epi16(__vv[1], __vv[1])),
		  __vector_bitcast<int>(_mm_unpackhi_epi16(__vv[1], __vv[1]))};
		if constexpr (sizeof(_ToT) == 4)
		  return __generate_from_n_evaluations<_Np, _R>([&](auto __i) {
		    return __vector_convert<_To>(
		      _SimdWrapper<int, 4>(__vvvv[__i] >> 24));
		  });
		else if constexpr (is_integral_v<_ToT>)
		  return __generate_from_n_evaluations<_Np, _R>([&](auto __i) {
		    const auto __signbits = __to_intrin(__vvvv[__i / 2] >> 31);
		    const auto __sx32 = __to_intrin(__vvvv[__i / 2] >> 24);
		    return __vector_bitcast<_ToT>(
		      __i % 2 == 0 ? _mm_unpacklo_epi32(__sx32, __signbits)
				   : _mm_unpackhi_epi32(__sx32, __signbits));
		  });
		else
		  return __generate_from_n_evaluations<_Np, _R>([&](auto __i) {
		    const _SimdWrapper<int, 4> __int4 = __vvvv[__i / 2] >> 24;
		    return __vector_convert<_To>(
		      __i % 2 == 0 ? __int4
				   : _SimdWrapper<int, 4>(
				     _mm_unpackhi_epi64(__to_intrin(__int4),
							__to_intrin(__int4))));
		  });
	      }
	    else if constexpr (sizeof(_FromT) == 1 && sizeof(_ToT) == 4)
	      {
		const auto __shorts = __convert_all<__vector_type16_t<
		  conditional_t<is_signed_v<_FromT>, short, unsigned short>>>(
		  __adjust(_SizeConstant<(_Np + 1) / 2 * 8>(), __v));
		return __generate_from_n_evaluations<_Np, _R>([&](auto __i) {
		  return __convert_all<_To>(__shorts[__i / 2])[__i % 2];
		});
	      }
	    else if constexpr (sizeof(_FromT) == 2 && sizeof(_ToT) == 8
			       && is_signed_v<_FromT> && is_integral_v<_ToT>)
	      {
		const __m128i __vv[2] = {_mm_unpacklo_epi16(__vi, __vi),
					 _mm_unpackhi_epi16(__vi, __vi)};
		const __vector_type16_t<int> __vvvv[4]
		  = {__vector_bitcast<int>(
		       _mm_unpacklo_epi32(_mm_srai_epi32(__vv[0], 16),
					  _mm_srai_epi32(__vv[0], 31))),
		     __vector_bitcast<int>(
		       _mm_unpackhi_epi32(_mm_srai_epi32(__vv[0], 16),
					  _mm_srai_epi32(__vv[0], 31))),
		     __vector_bitcast<int>(
		       _mm_unpacklo_epi32(_mm_srai_epi32(__vv[1], 16),
					  _mm_srai_epi32(__vv[1], 31))),
		     __vector_bitcast<int>(
		       _mm_unpackhi_epi32(_mm_srai_epi32(__vv[1], 16),
					  _mm_srai_epi32(__vv[1], 31)))};
		return __generate_from_n_evaluations<_Np, _R>([&](auto __i) {
		  return __vector_bitcast<_ToT>(__vvvv[__i]);
		});
	      }
	    else if constexpr (sizeof(_FromT) <= 2 && sizeof(_ToT) == 8)
	      {
		const auto __ints
		  = __convert_all<__vector_type16_t<conditional_t<
		    is_signed_v<_FromT> || is_floating_point_v<_ToT>, int,
		    unsigned int>>>(
		    __adjust(_SizeConstant<(_Np + 1) / 2 * 4>(), __v));
		return __generate_from_n_evaluations<_Np, _R>([&](auto __i) {
		  return __convert_all<_To>(__ints[__i / 2])[__i % 2];
		});
	      }
	    else
	      __assert_unreachable<_To>();
	  }
#endif // _GLIBCXX_SIMD_X86INTRIN }}}
	else if constexpr ((_FromVT::_S_partial_width - _Offset)
			   > _ToVT::_S_full_size)
	  {
	    /*
	    static_assert(
	      (_FromVT::_S_partial_width & (_FromVT::_S_partial_width - 1)) ==
	    0,
	      "__convert_all only supports power-of-2 number of elements.
	    Otherwise " "the return type cannot be array<_To, N>.");
	      */
	    constexpr size_t _NTotal
	      = (_FromVT::_S_partial_width - _Offset) / _ToVT::_S_full_size;
	    constexpr size_t _Np = _NParts == 0 ? _NTotal : _NParts;
	    static_assert(
	      _Np <= _NTotal
	      || (_Np == _NTotal + 1
		  && (_FromVT::_S_partial_width - _Offset) % _ToVT::_S_full_size
		       > 0));
	    using _R = array<_To, _Np>;
	    if constexpr (_Np == 1)
	      return _R{__vector_convert<_To>(
		__extract_part<_Offset, _FromVT::_S_partial_width,
			       _ToVT::_S_full_size>(__v))};
	    else
	      return __generate_from_n_evaluations<_Np, _R>([&](
		auto __i) constexpr {
		auto __part
		  = __extract_part<__i * _ToVT::_S_full_size + _Offset,
				   _FromVT::_S_partial_width,
				   _ToVT::_S_full_size>(__v);
		return __vector_convert<_To>(__part);
	      });
	  }
	else if constexpr (_Offset == 0)
	  return array<_To, 1>{__vector_convert<_To>(__v)};
	else
	  return array<_To, 1>{__vector_convert<_To>(
	    __extract_part<_Offset, _FromVT::_S_partial_width,
			   _FromVT::_S_partial_width - _Offset>(__v))};
      }
  }

// }}}

// _GnuTraits {{{
template <typename _Tp, typename _Mp, typename _Abi, size_t _Np>
  struct _GnuTraits
  {
    using _IsValid = true_type;
    using _SimdImpl = typename _Abi::_SimdImpl;
    using _MaskImpl = typename _Abi::_MaskImpl;

    // simd and simd_mask member types {{{
    using _SimdMember = _SimdWrapper<_Tp, _Np>;
    using _MaskMember = _SimdWrapper<_Mp, _Np>;
    static constexpr size_t _S_simd_align = alignof(_SimdMember);
    static constexpr size_t _S_mask_align = alignof(_MaskMember);

    // }}}
    // size metadata {{{
    static constexpr size_t _S_full_size = _SimdMember::_S_full_size;
    static constexpr bool _S_is_partial = _SimdMember::_S_is_partial;

    // }}}
    // _SimdBase / base class for simd, providing extra conversions {{{
    struct _SimdBase2
    {
      _GLIBCXX_SIMD_ALWAYS_INLINE
      explicit operator __intrinsic_type_t<_Tp, _Np>() const
      {
	return __to_intrin(static_cast<const simd<_Tp, _Abi>*>(this)->_M_data);
      }
      _GLIBCXX_SIMD_ALWAYS_INLINE
      explicit operator __vector_type_t<_Tp, _Np>() const
      {
	return static_cast<const simd<_Tp, _Abi>*>(this)->_M_data.__builtin();
      }
    };

    struct _SimdBase1
    {
      _GLIBCXX_SIMD_ALWAYS_INLINE
      explicit operator __intrinsic_type_t<_Tp, _Np>() const
      { return __data(*static_cast<const simd<_Tp, _Abi>*>(this)); }
    };

    using _SimdBase = conditional_t<
      is_same<__intrinsic_type_t<_Tp, _Np>, __vector_type_t<_Tp, _Np>>::value,
      _SimdBase1, _SimdBase2>;

    // }}}
    // _MaskBase {{{
    struct _MaskBase2
    {
      _GLIBCXX_SIMD_ALWAYS_INLINE
      explicit operator __intrinsic_type_t<_Tp, _Np>() const
      {
	return static_cast<const simd_mask<_Tp, _Abi>*>(this)
	  ->_M_data.__intrin();
      }
      _GLIBCXX_SIMD_ALWAYS_INLINE
      explicit operator __vector_type_t<_Tp, _Np>() const
      {
	return static_cast<const simd_mask<_Tp, _Abi>*>(this)->_M_data._M_data;
      }
    };

    struct _MaskBase1
    {
      _GLIBCXX_SIMD_ALWAYS_INLINE
      explicit operator __intrinsic_type_t<_Tp, _Np>() const
      { return __data(*static_cast<const simd_mask<_Tp, _Abi>*>(this)); }
    };

    using _MaskBase = conditional_t<
      is_same<__intrinsic_type_t<_Tp, _Np>, __vector_type_t<_Tp, _Np>>::value,
      _MaskBase1, _MaskBase2>;

    // }}}
    // _MaskCastType {{{
    // parameter type of one explicit simd_mask constructor
    class _MaskCastType
    {
      using _Up = __intrinsic_type_t<_Tp, _Np>;
      _Up _M_data;

    public:
      _GLIBCXX_SIMD_ALWAYS_INLINE
      _MaskCastType(_Up __x) : _M_data(__x) {}
      _GLIBCXX_SIMD_ALWAYS_INLINE
      operator _MaskMember() const { return _M_data; }
    };

    // }}}
    // _SimdCastType {{{
    // parameter type of one explicit simd constructor
    class _SimdCastType1
    {
      using _Ap = __intrinsic_type_t<_Tp, _Np>;
      _SimdMember _M_data;

    public:
      _GLIBCXX_SIMD_ALWAYS_INLINE
      _SimdCastType1(_Ap __a) : _M_data(__vector_bitcast<_Tp>(__a)) {}
      _GLIBCXX_SIMD_ALWAYS_INLINE
      operator _SimdMember() const { return _M_data; }
    };

    class _SimdCastType2
    {
      using _Ap = __intrinsic_type_t<_Tp, _Np>;
      using _Bp = __vector_type_t<_Tp, _Np>;
      _SimdMember _M_data;

    public:
      _GLIBCXX_SIMD_ALWAYS_INLINE
      _SimdCastType2(_Ap __a) : _M_data(__vector_bitcast<_Tp>(__a)) {}
      _GLIBCXX_SIMD_ALWAYS_INLINE
      _SimdCastType2(_Bp __b) : _M_data(__b) {}
      _GLIBCXX_SIMD_ALWAYS_INLINE
      operator _SimdMember() const { return _M_data; }
    };

    using _SimdCastType = conditional_t<
      is_same<__intrinsic_type_t<_Tp, _Np>, __vector_type_t<_Tp, _Np>>::value,
      _SimdCastType1, _SimdCastType2>;
    //}}}
  };

// }}}
struct _CommonImplX86;
struct _CommonImplNeon;
struct _CommonImplBuiltin;
template <typename _Abi, typename = __detail::__odr_helper> struct _SimdImplBuiltin;
template <typename _Abi, typename = __detail::__odr_helper> struct _MaskImplBuiltin;
template <typename _Abi, typename = __detail::__odr_helper> struct _SimdImplX86;
template <typename _Abi, typename = __detail::__odr_helper> struct _MaskImplX86;
template <typename _Abi, typename = __detail::__odr_helper> struct _SimdImplNeon;
template <typename _Abi, typename = __detail::__odr_helper> struct _MaskImplNeon;
template <typename _Abi, typename = __detail::__odr_helper> struct _SimdImplPpc;
template <typename _Abi, typename = __detail::__odr_helper> struct _MaskImplPpc;

// simd_abi::_VecBuiltin {{{
template <int _UsedBytes>
  struct simd_abi::_VecBuiltin
  {
    template <typename _Tp>
      static constexpr size_t _S_size = _UsedBytes / sizeof(_Tp);

    // validity traits {{{
    struct _IsValidAbiTag : __bool_constant<(_UsedBytes > 1)> {};

    template <typename _Tp>
      struct _IsValidSizeFor
	: __bool_constant<(_UsedBytes / sizeof(_Tp) > 1
			   && _UsedBytes % sizeof(_Tp) == 0
			   && _UsedBytes <= __vectorized_sizeof<_Tp>()
			   && (!__have_avx512f || _UsedBytes <= 32))> {};

    template <typename _Tp>
      struct _IsValid : conjunction<_IsValidAbiTag, __is_vectorizable<_Tp>,
				    _IsValidSizeFor<_Tp>> {};

    template <typename _Tp>
      static constexpr bool _S_is_valid_v = _IsValid<_Tp>::value;

    // }}}
    // _SimdImpl/_MaskImpl {{{
#if _GLIBCXX_SIMD_X86INTRIN
    using _CommonImpl = _CommonImplX86;
    using _SimdImpl = _SimdImplX86<_VecBuiltin<_UsedBytes>>;
    using _MaskImpl = _MaskImplX86<_VecBuiltin<_UsedBytes>>;
#elif _GLIBCXX_SIMD_HAVE_NEON
    using _CommonImpl = _CommonImplNeon;
    using _SimdImpl = _SimdImplNeon<_VecBuiltin<_UsedBytes>>;
    using _MaskImpl = _MaskImplNeon<_VecBuiltin<_UsedBytes>>;
#else
    using _CommonImpl = _CommonImplBuiltin;
#ifdef __ALTIVEC__
    using _SimdImpl = _SimdImplPpc<_VecBuiltin<_UsedBytes>>;
    using _MaskImpl = _MaskImplPpc<_VecBuiltin<_UsedBytes>>;
#else
    using _SimdImpl = _SimdImplBuiltin<_VecBuiltin<_UsedBytes>>;
    using _MaskImpl = _MaskImplBuiltin<_VecBuiltin<_UsedBytes>>;
#endif
#endif

    // }}}
    // __traits {{{
    template <typename _Tp>
      using _MaskValueType = __int_for_sizeof_t<_Tp>;

    template <typename _Tp>
      using __traits
	= conditional_t<_S_is_valid_v<_Tp>,
			_GnuTraits<_Tp, _MaskValueType<_Tp>,
				   _VecBuiltin<_UsedBytes>, _S_size<_Tp>>,
			_InvalidTraits>;

    //}}}
    // size metadata {{{
    template <typename _Tp>
      static constexpr size_t _S_full_size = __traits<_Tp>::_S_full_size;

    template <typename _Tp>
      static constexpr bool _S_is_partial = __traits<_Tp>::_S_is_partial;

    // }}}
    // implicit masks {{{
    template <typename _Tp>
      using _MaskMember = _SimdWrapper<_MaskValueType<_Tp>, _S_size<_Tp>>;

    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_implicit_mask()
      {
	using _UV = typename _MaskMember<_Tp>::_BuiltinType;
	if constexpr (!_MaskMember<_Tp>::_S_is_partial)
	  return ~_UV();
	else
	  {
	    constexpr auto __size = _S_size<_Tp>;
	    _GLIBCXX_SIMD_USE_CONSTEXPR auto __r = __generate_vector<_UV>(
	      [](auto __i) constexpr { return __i < __size ? -1 : 0; });
	    return __r;
	  }
      }

    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr __intrinsic_type_t<_Tp,
								  _S_size<_Tp>>
      _S_implicit_mask_intrin()
      {
	return __to_intrin(
	  __vector_bitcast<_Tp>(_S_implicit_mask<_Tp>()._M_data));
      }

    template <typename _TW, typename _TVT = _VectorTraits<_TW>>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _TW _S_masked(_TW __x)
      {
	using _Tp = typename _TVT::value_type;
	if constexpr (!_MaskMember<_Tp>::_S_is_partial)
	  return __x;
	else
	  return __and(__as_vector(__x),
		       __vector_bitcast<_Tp>(_S_implicit_mask<_Tp>()));
      }

    template <typename _TW, typename _TVT = _VectorTraits<_TW>>
      _GLIBCXX_SIMD_INTRINSIC static constexpr auto
      __make_padding_nonzero(_TW __x)
      {
	using _Tp = typename _TVT::value_type;
	if constexpr (!_S_is_partial<_Tp>)
	  return __x;
	else
	  {
	    _GLIBCXX_SIMD_USE_CONSTEXPR auto __implicit_mask
	      = __vector_bitcast<_Tp>(_S_implicit_mask<_Tp>());
	    if constexpr (is_integral_v<_Tp>)
	      return __or(__x, ~__implicit_mask);
	    else
	      {
		_GLIBCXX_SIMD_USE_CONSTEXPR auto __one
		  = __andnot(__implicit_mask,
			     __vector_broadcast<_S_full_size<_Tp>>(_Tp(1)));
		// it's not enough to return `x | 1_in_padding` because the
		// padding in x might be inf or nan (independent of
		// __FINITE_MATH_ONLY__, because it's about padding bits)
		return __or(__and(__x, __implicit_mask), __one);
	      }
	  }
      }
    // }}}
  };

// }}}
// simd_abi::_VecBltnBtmsk {{{
template <int _UsedBytes>
  struct simd_abi::_VecBltnBtmsk
  {
    template <typename _Tp>
      static constexpr size_t _S_size = _UsedBytes / sizeof(_Tp);

    // validity traits {{{
    struct _IsValidAbiTag : __bool_constant<(_UsedBytes > 1)> {};

    template <typename _Tp>
      struct _IsValidSizeFor
	: __bool_constant<(_UsedBytes / sizeof(_Tp) > 1
			   && _UsedBytes % sizeof(_Tp) == 0 && _UsedBytes <= 64
			   && (_UsedBytes > 32 || __have_avx512vl))> {};

    // Bitmasks require at least AVX512F. If sizeof(_Tp) < 4 the AVX512BW is also
    // required.
    template <typename _Tp>
      struct _IsValid
	: conjunction<
	    _IsValidAbiTag, __bool_constant<__have_avx512f>,
	    __bool_constant<__have_avx512bw || (sizeof(_Tp) >= 4)>,
	    __bool_constant<(__vectorized_sizeof<_Tp>() > sizeof(_Tp))>,
	    _IsValidSizeFor<_Tp>> {};

    template <typename _Tp>
      static constexpr bool _S_is_valid_v = _IsValid<_Tp>::value;

    // }}}
    // simd/_MaskImpl {{{
  #if _GLIBCXX_SIMD_X86INTRIN
    using _CommonImpl = _CommonImplX86;
    using _SimdImpl = _SimdImplX86<_VecBltnBtmsk<_UsedBytes>>;
    using _MaskImpl = _MaskImplX86<_VecBltnBtmsk<_UsedBytes>>;
  #else
    template <int>
      struct _MissingImpl;

    using _CommonImpl = _MissingImpl<_UsedBytes>;
    using _SimdImpl = _MissingImpl<_UsedBytes>;
    using _MaskImpl = _MissingImpl<_UsedBytes>;
  #endif

    // }}}
    // __traits {{{
    template <typename _Tp>
      using _MaskMember = _SimdWrapper<bool, _S_size<_Tp>>;

    template <typename _Tp>
      using __traits = conditional_t<
	_S_is_valid_v<_Tp>,
	_GnuTraits<_Tp, bool, _VecBltnBtmsk<_UsedBytes>, _S_size<_Tp>>,
	_InvalidTraits>;

    //}}}
    // size metadata {{{
    template <typename _Tp>
      static constexpr size_t _S_full_size = __traits<_Tp>::_S_full_size;
    template <typename _Tp>
      static constexpr bool _S_is_partial = __traits<_Tp>::_S_is_partial;

    // }}}
    // implicit mask {{{
  private:
    template <typename _Tp>
      using _ImplicitMask = _SimdWrapper<bool, _S_size<_Tp>>;

  public:
    template <size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr __bool_storage_member_type_t<_Np>
      __implicit_mask_n()
      {
	using _Tp = __bool_storage_member_type_t<_Np>;
	return _Np < sizeof(_Tp) * __CHAR_BIT__ ? _Tp((1ULL << _Np) - 1) : ~_Tp();
      }

    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _ImplicitMask<_Tp>
      _S_implicit_mask()
      { return __implicit_mask_n<_S_size<_Tp>>(); }

    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr __bool_storage_member_type_t<
	_S_size<_Tp>>
      _S_implicit_mask_intrin()
      { return __implicit_mask_n<_S_size<_Tp>>(); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_masked(_SimdWrapper<_Tp, _Np> __x)
      {
	if constexpr (is_same_v<_Tp, bool>)
	  if constexpr (_Np < 8 || (_Np & (_Np - 1)) != 0)
	    return _MaskImpl::_S_bit_and(
	      __x, _SimdWrapper<_Tp, _Np>(
		     __bool_storage_member_type_t<_Np>((1ULL << _Np) - 1)));
	  else
	    return __x;
	else
	  return _S_masked(__x._M_data);
      }

    template <typename _TV>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _TV
      _S_masked(_TV __x)
      {
	using _Tp = typename _VectorTraits<_TV>::value_type;
	static_assert(
	  !__is_bitmask_v<_TV>,
	  "_VecBltnBtmsk::_S_masked cannot work on bitmasks, since it doesn't "
	  "know the number of elements. Use _SimdWrapper<bool, N> instead.");
	if constexpr (_S_is_partial<_Tp>)
	  {
	    constexpr size_t _Np = _S_size<_Tp>;
	    return __make_dependent_t<_TV, _CommonImpl>::_S_blend(
	      _S_implicit_mask<_Tp>(), _SimdWrapper<_Tp, _Np>(),
	      _SimdWrapper<_Tp, _Np>(__x));
	  }
	else
	  return __x;
      }

    template <typename _TV, typename _TVT = _VectorTraits<_TV>>
      _GLIBCXX_SIMD_INTRINSIC static constexpr auto
      __make_padding_nonzero(_TV __x)
      {
	using _Tp = typename _TVT::value_type;
	if constexpr (!_S_is_partial<_Tp>)
	  return __x;
	else
	  {
	    constexpr size_t _Np = _S_size<_Tp>;
	    if constexpr (is_integral_v<typename _TVT::value_type>)
	      return __x
		     | __generate_vector<_Tp, _S_full_size<_Tp>>(
		       [](auto __i) -> _Tp {
			 if (__i < _Np)
			   return 0;
			 else
			   return 1;
		       });
	    else
	      return __make_dependent_t<_TV, _CommonImpl>::_S_blend(
		       _S_implicit_mask<_Tp>(),
		       _SimdWrapper<_Tp, _Np>(
			 __vector_broadcast<_S_full_size<_Tp>>(_Tp(1))),
		       _SimdWrapper<_Tp, _Np>(__x))
		._M_data;
	  }
      }

    // }}}
  };

//}}}
// _CommonImplBuiltin {{{
struct _CommonImplBuiltin
{
  // _S_converts_via_decomposition{{{
  // This lists all cases where a __vector_convert needs to fall back to
  // conversion of individual scalars (i.e. decompose the input vector into
  // scalars, convert, compose output vector). In those cases, _S_masked_load &
  // _S_masked_store prefer to use the _S_bit_iteration implementation.
  template <typename _From, typename _To, size_t _ToSize>
    static inline constexpr bool __converts_via_decomposition_v
      = sizeof(_From) != sizeof(_To);

  // }}}
  // _S_load{{{
  template <typename _Tp, size_t _Np, size_t _Bytes = _Np * sizeof(_Tp)>
    _GLIBCXX_SIMD_INTRINSIC static __vector_type_t<_Tp, _Np>
    _S_load(const void* __p)
    {
      static_assert(_Np > 1);
      static_assert(_Bytes % sizeof(_Tp) == 0);
      using _Rp = __vector_type_t<_Tp, _Np>;
      if constexpr (sizeof(_Rp) == _Bytes)
	{
	  _Rp __r;
	  __builtin_memcpy(&__r, __p, _Bytes);
	  return __r;
	}
      else
	{
#ifdef _GLIBCXX_SIMD_WORKAROUND_PR90424
	  using _Up = conditional_t<
	    is_integral_v<_Tp>,
	    conditional_t<_Bytes % 4 == 0,
			  conditional_t<_Bytes % 8 == 0, long long, int>,
			  conditional_t<_Bytes % 2 == 0, short, signed char>>,
	    conditional_t<(_Bytes < 8 || _Np % 2 == 1 || _Np == 2), _Tp,
			  double>>;
	  using _V = __vector_type_t<_Up, _Np * sizeof(_Tp) / sizeof(_Up)>;
	  if constexpr (sizeof(_V) != sizeof(_Rp))
	    { // on i386 with 4 < _Bytes <= 8
	      _Rp __r{};
	      __builtin_memcpy(&__r, __p, _Bytes);
	      return __r;
	    }
	  else
#else // _GLIBCXX_SIMD_WORKAROUND_PR90424
	  using _V = _Rp;
#endif // _GLIBCXX_SIMD_WORKAROUND_PR90424
	    {
	      _V __r{};
	      static_assert(_Bytes <= sizeof(_V));
	      __builtin_memcpy(&__r, __p, _Bytes);
	      return reinterpret_cast<_Rp>(__r);
	    }
	}
    }

  // }}}
  // _S_store {{{
  template <size_t _ReqBytes = 0, typename _TV>
    _GLIBCXX_SIMD_INTRINSIC static void _S_store(_TV __x, void* __addr)
    {
      constexpr size_t _Bytes = _ReqBytes == 0 ? sizeof(__x) : _ReqBytes;
      static_assert(sizeof(__x) >= _Bytes);

      if constexpr (__is_vector_type_v<_TV>)
	{
	  using _Tp = typename _VectorTraits<_TV>::value_type;
	  constexpr size_t _Np = _Bytes / sizeof(_Tp);
	  static_assert(_Np * sizeof(_Tp) == _Bytes);

#ifdef _GLIBCXX_SIMD_WORKAROUND_PR90424
	  using _Up = conditional_t<
	    (is_integral_v<_Tp> || _Bytes < 4),
	    conditional_t<(sizeof(__x) > sizeof(long long)), long long, _Tp>,
	    float>;
	  const auto __v = __vector_bitcast<_Up>(__x);
#else // _GLIBCXX_SIMD_WORKAROUND_PR90424
	  const __vector_type_t<_Tp, _Np> __v = __x;
#endif // _GLIBCXX_SIMD_WORKAROUND_PR90424

	  if constexpr ((_Bytes & (_Bytes - 1)) != 0)
	    {
	      constexpr size_t _MoreBytes = std::__bit_ceil(_Bytes);
	      alignas(decltype(__v)) char __tmp[_MoreBytes];
	      __builtin_memcpy(__tmp, &__v, _MoreBytes);
	      __builtin_memcpy(__addr, __tmp, _Bytes);
	    }
	  else
	    __builtin_memcpy(__addr, &__v, _Bytes);
	}
      else
	__builtin_memcpy(__addr, &__x, _Bytes);
    }

  template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static void _S_store(_SimdWrapper<_Tp, _Np> __x,
						 void* __addr)
    { _S_store<_Np * sizeof(_Tp)>(__x._M_data, __addr); }

  // }}}
  // _S_store_bool_array(_BitMask) {{{
  template <size_t _Np, bool _Sanitized>
    _GLIBCXX_SIMD_INTRINSIC static constexpr void
    _S_store_bool_array(_BitMask<_Np, _Sanitized> __x, bool* __mem)
    {
      if constexpr (_Np == 1)
	__mem[0] = __x[0];
      else if constexpr (_Np == 2)
	{
	  short __bool2 = (__x._M_to_bits() * 0x81) & 0x0101;
	  _S_store<_Np>(__bool2, __mem);
	}
      else if constexpr (_Np == 3)
	{
	  int __bool3 = (__x._M_to_bits() * 0x4081) & 0x010101;
	  _S_store<_Np>(__bool3, __mem);
	}
      else
	{
	  __execute_n_times<__div_roundup(_Np, 4)>([&](auto __i) {
	    constexpr int __offset = __i * 4;
	    constexpr int __remaining = _Np - __offset;
	    if constexpr (__remaining > 4 && __remaining <= 7)
	      {
		const _ULLong __bool7
		  = (__x.template _M_extract<__offset>()._M_to_bits()
		     * 0x40810204081ULL)
		    & 0x0101010101010101ULL;
		_S_store<__remaining>(__bool7, __mem + __offset);
	      }
	    else if constexpr (__remaining >= 4)
	      {
		int __bits = __x.template _M_extract<__offset>()._M_to_bits();
		if constexpr (__remaining > 7)
		  __bits &= 0xf;
		const int __bool4 = (__bits * 0x204081) & 0x01010101;
		_S_store<4>(__bool4, __mem + __offset);
	      }
	  });
	}
    }

  // }}}
  // _S_blend{{{
  template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static constexpr auto
    _S_blend(_SimdWrapper<__int_for_sizeof_t<_Tp>, _Np> __k,
	     _SimdWrapper<_Tp, _Np> __at0, _SimdWrapper<_Tp, _Np> __at1)
    { return __k._M_data ? __at1._M_data : __at0._M_data; }

  // }}}
};

// }}}
// _SimdImplBuiltin {{{1
template <typename _Abi, typename>
  struct _SimdImplBuiltin
  {
    // member types {{{2
    template <typename _Tp>
      static constexpr size_t _S_max_store_size = 16;

    using abi_type = _Abi;

    template <typename _Tp>
      using _TypeTag = _Tp*;

    template <typename _Tp>
      using _SimdMember = typename _Abi::template __traits<_Tp>::_SimdMember;

    template <typename _Tp>
      using _MaskMember = typename _Abi::template _MaskMember<_Tp>;

    template <typename _Tp>
      static constexpr size_t _S_size = _Abi::template _S_size<_Tp>;

    template <typename _Tp>
      static constexpr size_t _S_full_size = _Abi::template _S_full_size<_Tp>;

    using _CommonImpl = typename _Abi::_CommonImpl;
    using _SuperImpl = typename _Abi::_SimdImpl;
    using _MaskImpl = typename _Abi::_MaskImpl;

    // _M_make_simd(_SimdWrapper/__intrinsic_type_t) {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static simd<_Tp, _Abi>
      _M_make_simd(_SimdWrapper<_Tp, _Np> __x)
      { return {__private_init, __x}; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static simd<_Tp, _Abi>
      _M_make_simd(__intrinsic_type_t<_Tp, _Np> __x)
      { return {__private_init, __vector_bitcast<_Tp>(__x)}; }

    // _S_broadcast {{{2
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdMember<_Tp>
      _S_broadcast(_Tp __x) noexcept
      { return __vector_broadcast<_S_full_size<_Tp>>(__x); }

    // _S_generator {{{2
    template <typename _Fp, typename _Tp>
      inline static constexpr _SimdMember<_Tp> _S_generator(_Fp&& __gen,
							    _TypeTag<_Tp>)
      {
	return __generate_vector<_Tp, _S_full_size<_Tp>>([&](
	  auto __i) constexpr {
	  if constexpr (__i < _S_size<_Tp>)
	    return __gen(__i);
	  else
	    return 0;
	});
      }

    // _S_load {{{2
    template <typename _Tp, typename _Up>
      _GLIBCXX_SIMD_INTRINSIC static _SimdMember<_Tp>
      _S_load(const _Up* __mem, _TypeTag<_Tp>) noexcept
      {
	constexpr size_t _Np = _S_size<_Tp>;
	constexpr size_t __max_load_size
	  = (sizeof(_Up) >= 4 && __have_avx512f) || __have_avx512bw   ? 64
	    : (is_floating_point_v<_Up> && __have_avx) || __have_avx2 ? 32
								      : 16;
	constexpr size_t __bytes_to_load = sizeof(_Up) * _Np;
	if constexpr (sizeof(_Up) > 8)
	  return __generate_vector<_Tp, _SimdMember<_Tp>::_S_full_size>([&](
	    auto __i) constexpr {
	    return static_cast<_Tp>(__i < _Np ? __mem[__i] : 0);
	  });
	else if constexpr (is_same_v<_Up, _Tp>)
	  return _CommonImpl::template _S_load<_Tp, _S_full_size<_Tp>,
					       _Np * sizeof(_Tp)>(__mem);
	else if constexpr (__bytes_to_load <= __max_load_size)
	  return __convert<_SimdMember<_Tp>>(
	    _CommonImpl::template _S_load<_Up, _Np>(__mem));
	else if constexpr (__bytes_to_load % __max_load_size == 0)
	  {
	    constexpr size_t __n_loads = __bytes_to_load / __max_load_size;
	    constexpr size_t __elements_per_load = _Np / __n_loads;
	    return __call_with_n_evaluations<__n_loads>(
	      [](auto... __uncvted) {
		return __convert<_SimdMember<_Tp>>(__uncvted...);
	      },
	      [&](auto __i) {
		return _CommonImpl::template _S_load<_Up, __elements_per_load>(
		  __mem + __i * __elements_per_load);
	      });
	  }
	else if constexpr (__bytes_to_load % (__max_load_size / 2) == 0
			   && __max_load_size > 16)
	  { // e.g. int[] -> <char, 12> with AVX2
	    constexpr size_t __n_loads
	      = __bytes_to_load / (__max_load_size / 2);
	    constexpr size_t __elements_per_load = _Np / __n_loads;
	    return __call_with_n_evaluations<__n_loads>(
	      [](auto... __uncvted) {
		return __convert<_SimdMember<_Tp>>(__uncvted...);
	      },
	      [&](auto __i) {
		return _CommonImpl::template _S_load<_Up, __elements_per_load>(
		  __mem + __i * __elements_per_load);
	      });
	  }
	else // e.g. int[] -> <char, 9>
	  return __call_with_subscripts(
	    __mem, make_index_sequence<_Np>(), [](auto... __args) {
	      return __vector_type_t<_Tp, _S_full_size<_Tp>>{
		static_cast<_Tp>(__args)...};
	    });
      }

    // _S_masked_load {{{2
    template <typename _Tp, size_t _Np, typename _Up>
      static inline _SimdWrapper<_Tp, _Np>
      _S_masked_load(_SimdWrapper<_Tp, _Np> __merge, _MaskMember<_Tp> __k,
		     const _Up* __mem) noexcept
      {
	_BitOps::_S_bit_iteration(_MaskImpl::_S_to_bits(__k), [&](auto __i) {
	  __merge._M_set(__i, static_cast<_Tp>(__mem[__i]));
	});
	return __merge;
      }

    // _S_store {{{2
    template <typename _Tp, typename _Up>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_store(_SimdMember<_Tp> __v, _Up* __mem, _TypeTag<_Tp>) noexcept
      {
	// TODO: converting int -> "smaller int" can be optimized with AVX512
	constexpr size_t _Np = _S_size<_Tp>;
	constexpr size_t __max_store_size
	  = _SuperImpl::template _S_max_store_size<_Up>;
	if constexpr (sizeof(_Up) > 8)
	  __execute_n_times<_Np>([&](auto __i) constexpr {
	    __mem[__i] = __v[__i];
	  });
	else if constexpr (is_same_v<_Up, _Tp>)
	  _CommonImpl::_S_store(__v, __mem);
	else if constexpr (sizeof(_Up) * _Np <= __max_store_size)
	  _CommonImpl::_S_store(_SimdWrapper<_Up, _Np>(__convert<_Up>(__v)),
				__mem);
	else
	  {
	    constexpr size_t __vsize = __max_store_size / sizeof(_Up);
	    // round up to convert the last partial vector as well:
	    constexpr size_t __stores = __div_roundup(_Np, __vsize);
	    constexpr size_t __full_stores = _Np / __vsize;
	    using _V = __vector_type_t<_Up, __vsize>;
	    const array<_V, __stores> __converted
	      = __convert_all<_V, __stores>(__v);
	    __execute_n_times<__full_stores>([&](auto __i) constexpr {
	      _CommonImpl::_S_store(__converted[__i], __mem + __i * __vsize);
	    });
	    if constexpr (__full_stores < __stores)
	      _CommonImpl::template _S_store<(_Np - __full_stores * __vsize)
					     * sizeof(_Up)>(
		__converted[__full_stores], __mem + __full_stores * __vsize);
	  }
      }

    // _S_masked_store_nocvt {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_store_nocvt(_SimdWrapper<_Tp, _Np> __v, _Tp* __mem,
			    _MaskMember<_Tp> __k)
      {
	_BitOps::_S_bit_iteration(
	  _MaskImpl::_S_to_bits(__k), [&](auto __i) constexpr {
	    __mem[__i] = __v[__i];
	  });
      }

    // _S_masked_store {{{2
    template <typename _TW, typename _TVT = _VectorTraits<_TW>,
	      typename _Tp = typename _TVT::value_type, typename _Up>
      static inline void
      _S_masked_store(const _TW __v, _Up* __mem, const _MaskMember<_Tp> __k)
	noexcept
      {
	constexpr size_t _TV_size = _S_size<_Tp>;
	[[maybe_unused]] const auto __vi = __to_intrin(__v);
	constexpr size_t __max_store_size
	  = _SuperImpl::template _S_max_store_size<_Up>;
	if constexpr (
	  is_same_v<
	    _Tp,
	    _Up> || (is_integral_v<_Tp> && is_integral_v<_Up> && sizeof(_Tp) == sizeof(_Up)))
	  {
	    // bitwise or no conversion, reinterpret:
	    const _MaskMember<_Up> __kk = [&]() {
	      if constexpr (__is_bitmask_v<decltype(__k)>)
		return _MaskMember<_Up>(__k._M_data);
	      else
		return __wrapper_bitcast<__int_for_sizeof_t<_Up>>(__k);
	    }();
	    _SuperImpl::_S_masked_store_nocvt(__wrapper_bitcast<_Up>(__v),
					      __mem, __kk);
	  }
	else if constexpr (__vectorized_sizeof<_Up>() > sizeof(_Up)
			   && !_CommonImpl::
				template __converts_via_decomposition_v<
				  _Tp, _Up, __max_store_size>)
	  { // conversion via decomposition is better handled via the
	    // bit_iteration
	    // fallback below
	    constexpr size_t _UW_size
	      = std::min(_TV_size, __max_store_size / sizeof(_Up));
	    static_assert(_UW_size <= _TV_size);
	    using _UW = _SimdWrapper<_Up, _UW_size>;
	    using _UV = __vector_type_t<_Up, _UW_size>;
	    using _UAbi = simd_abi::deduce_t<_Up, _UW_size>;
	    if constexpr (_UW_size == _TV_size) // one convert+store
	      {
		const _UW __converted = __convert<_UW>(__v);
		_SuperImpl::_S_masked_store_nocvt(
		  __converted, __mem,
		  _UAbi::_MaskImpl::template _S_convert<
		    __int_for_sizeof_t<_Up>>(__k));
	      }
	    else
	      {
		static_assert(_UW_size * sizeof(_Up) == __max_store_size);
		constexpr size_t _NFullStores = _TV_size / _UW_size;
		constexpr size_t _NAllStores
		  = __div_roundup(_TV_size, _UW_size);
		constexpr size_t _NParts = _S_full_size<_Tp> / _UW_size;
		const array<_UV, _NAllStores> __converted
		  = __convert_all<_UV, _NAllStores>(__v);
		__execute_n_times<_NFullStores>([&](auto __i) {
		  _SuperImpl::_S_masked_store_nocvt(
		    _UW(__converted[__i]), __mem + __i * _UW_size,
		    _UAbi::_MaskImpl::template _S_convert<
		      __int_for_sizeof_t<_Up>>(
		      __extract_part<__i, _NParts>(__k.__as_full_vector())));
		});
		if constexpr (_NAllStores
			      > _NFullStores) // one partial at the end
		  _SuperImpl::_S_masked_store_nocvt(
		    _UW(__converted[_NFullStores]),
		    __mem + _NFullStores * _UW_size,
		    _UAbi::_MaskImpl::template _S_convert<
		      __int_for_sizeof_t<_Up>>(
		      __extract_part<_NFullStores, _NParts>(
			__k.__as_full_vector())));
	      }
	  }
	else
	  _BitOps::_S_bit_iteration(
	    _MaskImpl::_S_to_bits(__k), [&](auto __i) constexpr {
	      __mem[__i] = static_cast<_Up>(__v[__i]);
	    });
      }

    // _S_complement {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_complement(_SimdWrapper<_Tp, _Np> __x) noexcept
      {
	if constexpr (is_floating_point_v<_Tp>)
	  return __vector_bitcast<_Tp>(~__vector_bitcast<__int_for_sizeof_t<_Tp>>(__x));
	else
	  return ~__x._M_data;
      }

    // _S_unary_minus {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_unary_minus(_SimdWrapper<_Tp, _Np> __x) noexcept
      {
	// GCC doesn't use the psign instructions, but pxor & psub seem to be
	// just as good a choice as pcmpeqd & psign. So meh.
	return -__x._M_data;
      }

    // arithmetic operators {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_plus(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data + __y._M_data; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_minus(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data - __y._M_data; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_multiplies(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data * __y._M_data; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_divides(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	// Note that division by 0 is always UB, so we must ensure we avoid the
	// case for partial registers
	if constexpr (!_Abi::template _S_is_partial<_Tp>)
	  return __x._M_data / __y._M_data;
	else
	  return __x._M_data / _Abi::__make_padding_nonzero(__y._M_data);
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_modulus(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      {
	if constexpr (!_Abi::template _S_is_partial<_Tp>)
	  return __x._M_data % __y._M_data;
	else
	  return __as_vector(__x)
		 % _Abi::__make_padding_nonzero(__as_vector(__y));
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_and(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __and(__x, __y); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_or(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __or(__x, __y); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_xor(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __xor(__x, __y); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_bit_shift_left(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data << __y._M_data; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_bit_shift_right(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data >> __y._M_data; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_shift_left(_SimdWrapper<_Tp, _Np> __x, int __y)
      { return __x._M_data << __y; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_shift_right(_SimdWrapper<_Tp, _Np> __x, int __y)
      { return __x._M_data >> __y; }

    // compares {{{2
    // _S_equal_to {{{3
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_equal_to(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data == __y._M_data; }

    // _S_not_equal_to {{{3
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_not_equal_to(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data != __y._M_data; }

    // _S_less {{{3
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_less(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data < __y._M_data; }

    // _S_less_equal {{{3
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_less_equal(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
      { return __x._M_data <= __y._M_data; }

    // _S_negate {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_negate(_SimdWrapper<_Tp, _Np> __x) noexcept
      { return !__x._M_data; }

    // _S_min, _S_max, _S_minmax {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_NORMAL_MATH _GLIBCXX_SIMD_INTRINSIC static constexpr
      _SimdWrapper<_Tp, _Np>
      _S_min(_SimdWrapper<_Tp, _Np> __a, _SimdWrapper<_Tp, _Np> __b)
      { return __a._M_data < __b._M_data ? __a._M_data : __b._M_data; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_NORMAL_MATH _GLIBCXX_SIMD_INTRINSIC static constexpr
      _SimdWrapper<_Tp, _Np>
      _S_max(_SimdWrapper<_Tp, _Np> __a, _SimdWrapper<_Tp, _Np> __b)
      { return __a._M_data > __b._M_data ? __a._M_data : __b._M_data; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_NORMAL_MATH _GLIBCXX_SIMD_INTRINSIC static constexpr
      pair<_SimdWrapper<_Tp, _Np>, _SimdWrapper<_Tp, _Np>>
      _S_minmax(_SimdWrapper<_Tp, _Np> __a, _SimdWrapper<_Tp, _Np> __b)
      {
	return {__a._M_data < __b._M_data ? __a._M_data : __b._M_data,
		__a._M_data < __b._M_data ? __b._M_data : __a._M_data};
      }

    // reductions {{{2
    template <size_t _Np, size_t... _Is, size_t... _Zeros, typename _Tp,
	      typename _BinaryOperation>
      _GLIBCXX_SIMD_INTRINSIC static _Tp
      _S_reduce_partial(index_sequence<_Is...>, index_sequence<_Zeros...>,
			simd<_Tp, _Abi> __x, _BinaryOperation&& __binary_op)
      {
	using _V = __vector_type_t<_Tp, _Np / 2>;
	static_assert(sizeof(_V) <= sizeof(__x));
	// _S_full_size is the size of the smallest native SIMD register that
	// can store _Np/2 elements:
	using _FullSimd = __deduced_simd<_Tp, _VectorTraits<_V>::_S_full_size>;
	using _HalfSimd = __deduced_simd<_Tp, _Np / 2>;
	const auto __xx = __as_vector(__x);
	return _HalfSimd::abi_type::_SimdImpl::_S_reduce(
	  static_cast<_HalfSimd>(__as_vector(__binary_op(
	    static_cast<_FullSimd>(__intrin_bitcast<_V>(__xx)),
	    static_cast<_FullSimd>(__intrin_bitcast<_V>(
	      __vector_permute<(_Np / 2 + _Is)..., (int(_Zeros * 0) - 1)...>(
		__xx)))))),
	  __binary_op);
      }

    template <typename _Tp, typename _BinaryOperation>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _Tp
      _S_reduce(simd<_Tp, _Abi> __x, _BinaryOperation&& __binary_op)
      {
	constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	if constexpr (_Np == 1)
	  return __x[0];
	else if constexpr (_Np == 2)
	  return __binary_op(simd<_Tp, simd_abi::scalar>(__x[0]),
			     simd<_Tp, simd_abi::scalar>(__x[1]))[0];
	else if constexpr (_Abi::template _S_is_partial<_Tp>) //{{{
	  {
	    [[maybe_unused]] constexpr auto __full_size
	      = _Abi::template _S_full_size<_Tp>;
	    if constexpr (_Np == 3)
	      return __binary_op(
		__binary_op(simd<_Tp, simd_abi::scalar>(__x[0]),
			    simd<_Tp, simd_abi::scalar>(__x[1])),
		simd<_Tp, simd_abi::scalar>(__x[2]))[0];
	    else if constexpr (is_same_v<__remove_cvref_t<_BinaryOperation>,
					 plus<>>)
	      {
		using _Ap = simd_abi::deduce_t<_Tp, __full_size>;
		return _Ap::_SimdImpl::_S_reduce(
		  simd<_Tp, _Ap>(__private_init,
				 _Abi::_S_masked(__as_vector(__x))),
		  __binary_op);
	      }
	    else if constexpr (is_same_v<__remove_cvref_t<_BinaryOperation>,
					 multiplies<>>)
	      {
		using _Ap = simd_abi::deduce_t<_Tp, __full_size>;
		using _TW = _SimdWrapper<_Tp, __full_size>;
		_GLIBCXX_SIMD_USE_CONSTEXPR auto __implicit_mask_full
		  = _Abi::template _S_implicit_mask<_Tp>().__as_full_vector();
		_GLIBCXX_SIMD_USE_CONSTEXPR _TW __one
		  = __vector_broadcast<__full_size>(_Tp(1));
		const _TW __x_full = __data(__x).__as_full_vector();
		const _TW __x_padded_with_ones
		  = _Ap::_CommonImpl::_S_blend(__implicit_mask_full, __one,
					       __x_full);
		return _Ap::_SimdImpl::_S_reduce(
		  simd<_Tp, _Ap>(__private_init, __x_padded_with_ones),
		  __binary_op);
	      }
	    else if constexpr (_Np & 1)
	      {
		using _Ap = simd_abi::deduce_t<_Tp, _Np - 1>;
		return __binary_op(
		  simd<_Tp, simd_abi::scalar>(_Ap::_SimdImpl::_S_reduce(
		    simd<_Tp, _Ap>(
		      __intrin_bitcast<__vector_type_t<_Tp, _Np - 1>>(
			__as_vector(__x))),
		    __binary_op)),
		  simd<_Tp, simd_abi::scalar>(__x[_Np - 1]))[0];
	      }
	    else
	      return _S_reduce_partial<_Np>(
		make_index_sequence<_Np / 2>(),
		make_index_sequence<__full_size - _Np / 2>(), __x, __binary_op);
	  }                                   //}}}
	else if constexpr (sizeof(__x) == 16) //{{{
	  {
	    if constexpr (_Np == 16)
	      {
		const auto __y = __data(__x);
		__x = __binary_op(
		  _M_make_simd<_Tp, _Np>(
		    __vector_permute<0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
				     7, 7>(__y)),
		  _M_make_simd<_Tp, _Np>(
		    __vector_permute<8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13,
				     14, 14, 15, 15>(__y)));
	      }
	    if constexpr (_Np >= 8)
	      {
		const auto __y = __vector_bitcast<short>(__data(__x));
		__x = __binary_op(
		  _M_make_simd<_Tp, _Np>(__vector_bitcast<_Tp>(
		    __vector_permute<0, 0, 1, 1, 2, 2, 3, 3>(__y))),
		  _M_make_simd<_Tp, _Np>(__vector_bitcast<_Tp>(
		    __vector_permute<4, 4, 5, 5, 6, 6, 7, 7>(__y))));
	      }
	    if constexpr (_Np >= 4)
	      {
		using _Up = conditional_t<is_floating_point_v<_Tp>, float, int>;
		const auto __y = __vector_bitcast<_Up>(__data(__x));
		__x = __binary_op(__x,
				  _M_make_simd<_Tp, _Np>(__vector_bitcast<_Tp>(
				    __vector_permute<3, 2, 1, 0>(__y))));
	      }
	    using _Up = conditional_t<is_floating_point_v<_Tp>, double, _LLong>;
	    const auto __y = __vector_bitcast<_Up>(__data(__x));
	    __x = __binary_op(__x, _M_make_simd<_Tp, _Np>(__vector_bitcast<_Tp>(
				     __vector_permute<1, 1>(__y))));
	    return __x[0];
	  } //}}}
	else
	  {
	    static_assert(sizeof(__x) > __min_vector_size<_Tp>);
	    static_assert((_Np & (_Np - 1)) == 0); // _Np must be a power of 2
	    using _Ap = simd_abi::deduce_t<_Tp, _Np / 2>;
	    using _V = simd<_Tp, _Ap>;
	    return _Ap::_SimdImpl::_S_reduce(
	      __binary_op(_V(__private_init, __extract<0, 2>(__as_vector(__x))),
			  _V(__private_init,
			     __extract<1, 2>(__as_vector(__x)))),
	      static_cast<_BinaryOperation&&>(__binary_op));
	  }
      }

    // math {{{2
    // frexp, modf and copysign implemented in simd_math.h
#define _GLIBCXX_SIMD_MATH_FALLBACK(__name)                                    \
    template <typename _Tp, typename... _More>                                 \
      static _Tp _S_##__name(const _Tp& __x, const _More&... __more)           \
      {                                                                        \
	return __generate_vector<_Tp>(                                         \
	  [&](auto __i) { return __name(__x[__i], __more[__i]...); });         \
      }

#define _GLIBCXX_SIMD_MATH_FALLBACK_MASKRET(__name)                            \
    template <typename _Tp, typename... _More>                                 \
      static typename _Tp::mask_type _S_##__name(const _Tp& __x,               \
						 const _More&... __more)       \
      {                                                                        \
	return __generate_vector<_Tp>(                                         \
	  [&](auto __i) { return __name(__x[__i], __more[__i]...); });         \
      }

#define _GLIBCXX_SIMD_MATH_FALLBACK_FIXEDRET(_RetTp, __name)                   \
    template <typename _Tp, typename... _More>                                 \
      static auto _S_##__name(const _Tp& __x, const _More&... __more)          \
      {                                                                        \
	return __fixed_size_storage_t<_RetTp,                                  \
				      _VectorTraits<_Tp>::_S_partial_width>::  \
	  _S_generate([&](auto __meta) constexpr {                             \
	    return __meta._S_generator(                                        \
	      [&](auto __i) {                                                  \
		return __name(__x[__meta._S_offset + __i],                     \
			      __more[__meta._S_offset + __i]...);              \
	      },                                                               \
	      static_cast<_RetTp*>(nullptr));                                  \
	  });                                                                  \
      }

    _GLIBCXX_SIMD_MATH_FALLBACK(acos)
    _GLIBCXX_SIMD_MATH_FALLBACK(asin)
    _GLIBCXX_SIMD_MATH_FALLBACK(atan)
    _GLIBCXX_SIMD_MATH_FALLBACK(atan2)
    _GLIBCXX_SIMD_MATH_FALLBACK(cos)
    _GLIBCXX_SIMD_MATH_FALLBACK(sin)
    _GLIBCXX_SIMD_MATH_FALLBACK(tan)
    _GLIBCXX_SIMD_MATH_FALLBACK(acosh)
    _GLIBCXX_SIMD_MATH_FALLBACK(asinh)
    _GLIBCXX_SIMD_MATH_FALLBACK(atanh)
    _GLIBCXX_SIMD_MATH_FALLBACK(cosh)
    _GLIBCXX_SIMD_MATH_FALLBACK(sinh)
    _GLIBCXX_SIMD_MATH_FALLBACK(tanh)
    _GLIBCXX_SIMD_MATH_FALLBACK(exp)
    _GLIBCXX_SIMD_MATH_FALLBACK(exp2)
    _GLIBCXX_SIMD_MATH_FALLBACK(expm1)
    _GLIBCXX_SIMD_MATH_FALLBACK(ldexp)
    _GLIBCXX_SIMD_MATH_FALLBACK_FIXEDRET(int, ilogb)
    _GLIBCXX_SIMD_MATH_FALLBACK(log)
    _GLIBCXX_SIMD_MATH_FALLBACK(log10)
    _GLIBCXX_SIMD_MATH_FALLBACK(log1p)
    _GLIBCXX_SIMD_MATH_FALLBACK(log2)
    _GLIBCXX_SIMD_MATH_FALLBACK(logb)

    // modf implemented in simd_math.h
    _GLIBCXX_SIMD_MATH_FALLBACK(scalbn)
    _GLIBCXX_SIMD_MATH_FALLBACK(scalbln)
    _GLIBCXX_SIMD_MATH_FALLBACK(cbrt)
    _GLIBCXX_SIMD_MATH_FALLBACK(fabs)
    _GLIBCXX_SIMD_MATH_FALLBACK(pow)
    _GLIBCXX_SIMD_MATH_FALLBACK(sqrt)
    _GLIBCXX_SIMD_MATH_FALLBACK(erf)
    _GLIBCXX_SIMD_MATH_FALLBACK(erfc)
    _GLIBCXX_SIMD_MATH_FALLBACK(lgamma)
    _GLIBCXX_SIMD_MATH_FALLBACK(tgamma)

    _GLIBCXX_SIMD_MATH_FALLBACK_FIXEDRET(long, lrint)
    _GLIBCXX_SIMD_MATH_FALLBACK_FIXEDRET(long long, llrint)

    _GLIBCXX_SIMD_MATH_FALLBACK_FIXEDRET(long, lround)
    _GLIBCXX_SIMD_MATH_FALLBACK_FIXEDRET(long long, llround)

    _GLIBCXX_SIMD_MATH_FALLBACK(fmod)
    _GLIBCXX_SIMD_MATH_FALLBACK(remainder)

    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      static _Tp
      _S_remquo(const _Tp __x, const _Tp __y,
		__fixed_size_storage_t<int, _TVT::_S_partial_width>* __z)
      {
	return __generate_vector<_Tp>([&](auto __i) {
	  int __tmp;
	  auto __r = remquo(__x[__i], __y[__i], &__tmp);
	  __z->_M_set(__i, __tmp);
	  return __r;
	});
      }

    // copysign in simd_math.h
    _GLIBCXX_SIMD_MATH_FALLBACK(nextafter)
    _GLIBCXX_SIMD_MATH_FALLBACK(fdim)
    _GLIBCXX_SIMD_MATH_FALLBACK(fmax)
    _GLIBCXX_SIMD_MATH_FALLBACK(fmin)
    _GLIBCXX_SIMD_MATH_FALLBACK(fma)

    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp>
      _S_isgreater(_SimdWrapper<_Tp, _Np> __x,
		   _SimdWrapper<_Tp, _Np> __y) noexcept
      {
	using _Ip = __int_for_sizeof_t<_Tp>;
	const auto __xn = __vector_bitcast<_Ip>(__x);
	const auto __yn = __vector_bitcast<_Ip>(__y);
	const auto __xp = __xn < 0 ? -(__xn & __finite_max_v<_Ip>) : __xn;
	const auto __yp = __yn < 0 ? -(__yn & __finite_max_v<_Ip>) : __yn;
	return __andnot(_SuperImpl::_S_isunordered(__x, __y)._M_data,
			__xp > __yp);
      }

    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp>
      _S_isgreaterequal(_SimdWrapper<_Tp, _Np> __x,
			_SimdWrapper<_Tp, _Np> __y) noexcept
      {
	using _Ip = __int_for_sizeof_t<_Tp>;
	const auto __xn = __vector_bitcast<_Ip>(__x);
	const auto __yn = __vector_bitcast<_Ip>(__y);
	const auto __xp = __xn < 0 ? -(__xn & __finite_max_v<_Ip>) : __xn;
	const auto __yp = __yn < 0 ? -(__yn & __finite_max_v<_Ip>) : __yn;
	return __andnot(_SuperImpl::_S_isunordered(__x, __y)._M_data,
			__xp >= __yp);
      }

    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp>
      _S_isless(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y) noexcept
      {
	using _Ip = __int_for_sizeof_t<_Tp>;
	const auto __xn = __vector_bitcast<_Ip>(__x);
	const auto __yn = __vector_bitcast<_Ip>(__y);
	const auto __xp = __xn < 0 ? -(__xn & __finite_max_v<_Ip>) : __xn;
	const auto __yp = __yn < 0 ? -(__yn & __finite_max_v<_Ip>) : __yn;
	return __andnot(_SuperImpl::_S_isunordered(__x, __y)._M_data,
			__xp < __yp);
      }

    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp>
      _S_islessequal(_SimdWrapper<_Tp, _Np> __x,
		     _SimdWrapper<_Tp, _Np> __y) noexcept
      {
	using _Ip = __int_for_sizeof_t<_Tp>;
	const auto __xn = __vector_bitcast<_Ip>(__x);
	const auto __yn = __vector_bitcast<_Ip>(__y);
	const auto __xp = __xn < 0 ? -(__xn & __finite_max_v<_Ip>) : __xn;
	const auto __yp = __yn < 0 ? -(__yn & __finite_max_v<_Ip>) : __yn;
	return __andnot(_SuperImpl::_S_isunordered(__x, __y)._M_data,
			__xp <= __yp);
      }

    template <typename _Tp, size_t _Np>
      static constexpr _MaskMember<_Tp>
      _S_islessgreater(_SimdWrapper<_Tp, _Np> __x,
		       _SimdWrapper<_Tp, _Np> __y) noexcept
      {
	return __andnot(_SuperImpl::_S_isunordered(__x, __y),
			_SuperImpl::_S_not_equal_to(__x, __y));
      }

#undef _GLIBCXX_SIMD_MATH_FALLBACK
#undef _GLIBCXX_SIMD_MATH_FALLBACK_MASKRET
#undef _GLIBCXX_SIMD_MATH_FALLBACK_FIXEDRET
    // _S_abs {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
    _S_abs(_SimdWrapper<_Tp, _Np> __x) noexcept
    {
      // if (__builtin_is_constant_evaluated())
      //  {
      //    return __x._M_data < 0 ? -__x._M_data : __x._M_data;
      //  }
      if constexpr (is_floating_point_v<_Tp>)
	// `v < 0 ? -v : v` cannot compile to the efficient implementation of
	// masking the signbit off because it must consider v == -0

	// ~(-0.) & v would be easy, but breaks with fno-signed-zeros
	return __and(_S_absmask<__vector_type_t<_Tp, _Np>>, __x._M_data);
      else
	return __x._M_data < 0 ? -__x._M_data : __x._M_data;
    }

    // }}}3
    // _S_plus_minus {{{
    // Returns __x + __y - __y without -fassociative-math optimizing to __x.
    // - _TV must be __vector_type_t<floating-point type, N>.
    // - _UV must be _TV or floating-point type.
    template <typename _TV, typename _UV>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _TV _S_plus_minus(_TV __x,
							       _UV __y) noexcept
    {
  #if defined __i386__ && !defined __SSE_MATH__
      if constexpr (sizeof(__x) == 8)
	{ // operations on __x would use the FPU
	  static_assert(is_same_v<_TV, __vector_type_t<float, 2>>);
	  const auto __x4 = __vector_bitcast<float, 4>(__x);
	  if constexpr (is_same_v<_TV, _UV>)
	    return __vector_bitcast<float, 2>(
	      _S_plus_minus(__x4, __vector_bitcast<float, 4>(__y)));
	  else
	    return __vector_bitcast<float, 2>(_S_plus_minus(__x4, __y));
	}
  #endif
  #if !defined __clang__ && __GCC_IEC_559 == 0
      if (__builtin_is_constant_evaluated()
	  || (__builtin_constant_p(__x) && __builtin_constant_p(__y)))
	return (__x + __y) - __y;
      else
	return [&] {
	  __x += __y;
	  if constexpr(__have_sse)
	    {
	      if constexpr (sizeof(__x) >= 16)
		asm("" : "+x"(__x));
	      else if constexpr (is_same_v<__vector_type_t<float, 2>, _TV>)
		asm("" : "+x"(__x[0]), "+x"(__x[1]));
	      else
		__assert_unreachable<_TV>();
	    }
	  else if constexpr(__have_neon)
	    asm("" : "+w"(__x));
	  else if constexpr (__have_power_vmx)
	    {
	      if constexpr (is_same_v<__vector_type_t<float, 2>, _TV>)
		asm("" : "+fgr"(__x[0]), "+fgr"(__x[1]));
	      else
		asm("" : "+v"(__x));
	    }
	  else
	    asm("" : "+g"(__x));
	  return __x - __y;
	}();
  #else
      return (__x + __y) - __y;
  #endif
    }

    // }}}
    // _S_nearbyint {{{3
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_nearbyint(_Tp __x_) noexcept
    {
      using value_type = typename _TVT::value_type;
      using _V = typename _TVT::type;
      const _V __x = __x_;
      const _V __absx = __and(__x, _S_absmask<_V>);
      static_assert(__CHAR_BIT__ * sizeof(1ull) >= __digits_v<value_type>);
      _GLIBCXX_SIMD_USE_CONSTEXPR _V __shifter_abs
	= _V() + (1ull << (__digits_v<value_type> - 1));
      const _V __shifter = __or(__and(_S_signmask<_V>, __x), __shifter_abs);
      const _V __shifted = _S_plus_minus(__x, __shifter);
      return __absx < __shifter_abs ? __shifted : __x;
    }

    // _S_rint {{{3
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_rint(_Tp __x) noexcept
    {
      return _SuperImpl::_S_nearbyint(__x);
    }

    // _S_trunc {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
    _S_trunc(_SimdWrapper<_Tp, _Np> __x)
    {
      using _V = __vector_type_t<_Tp, _Np>;
      const _V __absx = __and(__x._M_data, _S_absmask<_V>);
      static_assert(__CHAR_BIT__ * sizeof(1ull) >= __digits_v<_Tp>);
      constexpr _Tp __shifter = 1ull << (__digits_v<_Tp> - 1);
      _V __truncated = _S_plus_minus(__absx, __shifter);
      __truncated -= __truncated > __absx ? _V() + 1 : _V();
      return __absx < __shifter ? __or(__xor(__absx, __x._M_data), __truncated)
				: __x._M_data;
    }

    // _S_round {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
    _S_round(_SimdWrapper<_Tp, _Np> __x)
    {
      const auto __abs_x = _SuperImpl::_S_abs(__x);
      const auto __t_abs = _SuperImpl::_S_trunc(__abs_x)._M_data;
      const auto __r_abs // round(abs(x)) =
	= __t_abs + (__abs_x._M_data - __t_abs >= _Tp(.5) ? _Tp(1) : 0);
      return __or(__xor(__abs_x._M_data, __x._M_data), __r_abs);
    }

    // _S_floor {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
    _S_floor(_SimdWrapper<_Tp, _Np> __x)
    {
      const auto __y = _SuperImpl::_S_trunc(__x)._M_data;
      const auto __negative_input
	= __vector_bitcast<_Tp>(__x._M_data < __vector_broadcast<_Np, _Tp>(0));
      const auto __mask
	= __andnot(__vector_bitcast<_Tp>(__y == __x._M_data), __negative_input);
      return __or(__andnot(__mask, __y),
		  __and(__mask, __y - __vector_broadcast<_Np, _Tp>(1)));
    }

    // _S_ceil {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
    _S_ceil(_SimdWrapper<_Tp, _Np> __x)
    {
      const auto __y = _SuperImpl::_S_trunc(__x)._M_data;
      const auto __negative_input
	= __vector_bitcast<_Tp>(__x._M_data < __vector_broadcast<_Np, _Tp>(0));
      const auto __inv_mask
	= __or(__vector_bitcast<_Tp>(__y == __x._M_data), __negative_input);
      return __or(__and(__inv_mask, __y),
		  __andnot(__inv_mask, __y + __vector_broadcast<_Np, _Tp>(1)));
    }

    // _S_isnan {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
    _S_isnan([[maybe_unused]] _SimdWrapper<_Tp, _Np> __x)
    {
  #if __FINITE_MATH_ONLY__
      return {}; // false
  #elif !defined __SUPPORT_SNAN__
      return ~(__x._M_data == __x._M_data);
  #elif defined __STDC_IEC_559__
      using _Ip = __int_for_sizeof_t<_Tp>;
      const auto __absn = __vector_bitcast<_Ip>(_SuperImpl::_S_abs(__x));
      const auto __infn
	= __vector_bitcast<_Ip>(__vector_broadcast<_Np>(__infinity_v<_Tp>));
      return __infn < __absn;
  #else
  #error "Not implemented: how to support SNaN but non-IEC559 floating-point?"
  #endif
    }

    // _S_isfinite {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
    _S_isfinite([[maybe_unused]] _SimdWrapper<_Tp, _Np> __x)
    {
  #if __FINITE_MATH_ONLY__
      using _UV = typename _MaskMember<_Tp>::_BuiltinType;
      _GLIBCXX_SIMD_USE_CONSTEXPR _UV __alltrue = ~_UV();
      return __alltrue;
  #else
      // if all exponent bits are set, __x is either inf or NaN
      using _Ip = __int_for_sizeof_t<_Tp>;
      const auto __absn = __vector_bitcast<_Ip>(_SuperImpl::_S_abs(__x));
      const auto __maxn
	= __vector_bitcast<_Ip>(__vector_broadcast<_Np>(__finite_max_v<_Tp>));
      return __absn <= __maxn;
  #endif
    }

    // _S_isunordered {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
    _S_isunordered(_SimdWrapper<_Tp, _Np> __x, _SimdWrapper<_Tp, _Np> __y)
    {
      return __or(_S_isnan(__x), _S_isnan(__y));
    }

    // _S_signbit {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
    _S_signbit(_SimdWrapper<_Tp, _Np> __x)
    {
      using _Ip = __int_for_sizeof_t<_Tp>;
      return __vector_bitcast<_Ip>(__x) < 0;
      // Arithmetic right shift (SRA) would also work (instead of compare), but
      // 64-bit SRA isn't available on x86 before AVX512. And in general,
      // compares are more likely to be efficient than SRA.
    }

    // _S_isinf {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
    _S_isinf([[maybe_unused]] _SimdWrapper<_Tp, _Np> __x)
    {
  #if __FINITE_MATH_ONLY__
      return {}; // false
  #else
      return _SuperImpl::template _S_equal_to<_Tp, _Np>(_SuperImpl::_S_abs(__x),
							__vector_broadcast<_Np>(
							  __infinity_v<_Tp>));
      // alternative:
      // compare to inf using the corresponding integer type
      /*
	 return
	 __vector_bitcast<_Tp>(__vector_bitcast<__int_for_sizeof_t<_Tp>>(
			       _S_abs(__x)._M_data)
	 ==
	 __vector_bitcast<__int_for_sizeof_t<_Tp>>(__vector_broadcast<_Np>(
	 __infinity_v<_Tp>)));
	 */
  #endif
    }

    // _S_isnormal {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
    _S_isnormal(_SimdWrapper<_Tp, _Np> __x)
    {
      using _Ip = __int_for_sizeof_t<_Tp>;
      const auto __absn = __vector_bitcast<_Ip>(_SuperImpl::_S_abs(__x));
      const auto __minn
	= __vector_bitcast<_Ip>(__vector_broadcast<_Np>(__norm_min_v<_Tp>));
  #if __FINITE_MATH_ONLY__
      return __absn >= __minn;
  #else
      const auto __maxn
	= __vector_bitcast<_Ip>(__vector_broadcast<_Np>(__finite_max_v<_Tp>));
      return __minn <= __absn && __absn <= __maxn;
  #endif
    }

    // _S_fpclassify {{{3
    template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static __fixed_size_storage_t<int, _Np>
    _S_fpclassify(_SimdWrapper<_Tp, _Np> __x)
    {
      using _I = __int_for_sizeof_t<_Tp>;
      const auto __xn
	= __vector_bitcast<_I>(__to_intrin(_SuperImpl::_S_abs(__x)));
      constexpr size_t _NI = sizeof(__xn) / sizeof(_I);
      _GLIBCXX_SIMD_USE_CONSTEXPR auto __minn
	= __vector_bitcast<_I>(__vector_broadcast<_NI>(__norm_min_v<_Tp>));
      _GLIBCXX_SIMD_USE_CONSTEXPR auto __infn
	= __vector_bitcast<_I>(__vector_broadcast<_NI>(__infinity_v<_Tp>));

      _GLIBCXX_SIMD_USE_CONSTEXPR auto __fp_normal
	= __vector_broadcast<_NI, _I>(FP_NORMAL);
  #if !__FINITE_MATH_ONLY__
      _GLIBCXX_SIMD_USE_CONSTEXPR auto __fp_nan
	= __vector_broadcast<_NI, _I>(FP_NAN);
      _GLIBCXX_SIMD_USE_CONSTEXPR auto __fp_infinite
	= __vector_broadcast<_NI, _I>(FP_INFINITE);
  #endif
  #ifndef __FAST_MATH__
      _GLIBCXX_SIMD_USE_CONSTEXPR auto __fp_subnormal
	= __vector_broadcast<_NI, _I>(FP_SUBNORMAL);
  #endif
      _GLIBCXX_SIMD_USE_CONSTEXPR auto __fp_zero
	= __vector_broadcast<_NI, _I>(FP_ZERO);

      __vector_type_t<_I, _NI>
	__tmp = __xn < __minn
  #ifdef __FAST_MATH__
		  ? __fp_zero
  #else
		  ? (__xn == 0 ? __fp_zero : __fp_subnormal)
  #endif
  #if __FINITE_MATH_ONLY__
		  : __fp_normal;
  #else
		  : (__xn < __infn ? __fp_normal
				   : (__xn == __infn ? __fp_infinite : __fp_nan));
  #endif

      if constexpr (sizeof(_I) == sizeof(int))
	{
	  using _FixedInt = __fixed_size_storage_t<int, _Np>;
	  const auto __as_int = __vector_bitcast<int, _Np>(__tmp);
	  if constexpr (_FixedInt::_S_tuple_size == 1)
	    return {__as_int};
	  else if constexpr (_FixedInt::_S_tuple_size == 2
			     && is_same_v<
			       typename _FixedInt::_SecondType::_FirstAbi,
			       simd_abi::scalar>)
	    return {__extract<0, 2>(__as_int), __as_int[_Np - 1]};
	  else if constexpr (_FixedInt::_S_tuple_size == 2)
	    return {__extract<0, 2>(__as_int),
		    __auto_bitcast(__extract<1, 2>(__as_int))};
	  else
	    __assert_unreachable<_Tp>();
	}
      else if constexpr (_Np == 2 && sizeof(_I) == 8
			 && __fixed_size_storage_t<int, _Np>::_S_tuple_size == 2)
	{
	  const auto __aslong = __vector_bitcast<_LLong>(__tmp);
	  return {int(__aslong[0]), {int(__aslong[1])}};
	}
  #if _GLIBCXX_SIMD_X86INTRIN
      else if constexpr (sizeof(_Tp) == 8 && sizeof(__tmp) == 32
			 && __fixed_size_storage_t<int, _Np>::_S_tuple_size == 1)
	return {_mm_packs_epi32(__to_intrin(__lo128(__tmp)),
				__to_intrin(__hi128(__tmp)))};
      else if constexpr (sizeof(_Tp) == 8 && sizeof(__tmp) == 64
			 && __fixed_size_storage_t<int, _Np>::_S_tuple_size == 1)
	return {_mm512_cvtepi64_epi32(__to_intrin(__tmp))};
  #endif // _GLIBCXX_SIMD_X86INTRIN
      else if constexpr (__fixed_size_storage_t<int, _Np>::_S_tuple_size == 1)
	return {__call_with_subscripts<_Np>(__vector_bitcast<_LLong>(__tmp),
					    [](auto... __l) {
					      return __make_wrapper<int>(__l...);
					    })};
      else
	__assert_unreachable<_Tp>();
    }

    // _S_increment & _S_decrement{{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_increment(_SimdWrapper<_Tp, _Np>& __x)
      { __x = __x._M_data + 1; }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_decrement(_SimdWrapper<_Tp, _Np>& __x)
      { __x = __x._M_data - 1; }

    // smart_reference access {{{2
    template <typename _Tp, size_t _Np, typename _Up>
      _GLIBCXX_SIMD_INTRINSIC constexpr static void
      _S_set(_SimdWrapper<_Tp, _Np>& __v, int __i, _Up&& __x) noexcept
      { __v._M_set(__i, static_cast<_Up&&>(__x)); }

    // _S_masked_assign{{{2
    template <typename _Tp, typename _K, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_assign(_SimdWrapper<_K, _Np> __k, _SimdWrapper<_Tp, _Np>& __lhs,
		       __type_identity_t<_SimdWrapper<_Tp, _Np>> __rhs)
      {
	if (__k._M_is_constprop_none_of())
	  return;
	else if (__k._M_is_constprop_all_of())
	  __lhs = __rhs;
	else
	  __lhs = _CommonImpl::_S_blend(__k, __lhs, __rhs);
      }

    template <typename _Tp, typename _K, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_assign(_SimdWrapper<_K, _Np> __k, _SimdWrapper<_Tp, _Np>& __lhs,
		       __type_identity_t<_Tp> __rhs)
      {
	if (__k._M_is_constprop_none_of())
	  return;
	else if (__k._M_is_constprop_all_of())
	  __lhs = __vector_broadcast<_Np>(__rhs);
	else if (__builtin_constant_p(__rhs) && __rhs == 0)
	  {
	    if constexpr (!is_same_v<bool, _K>)
	      // the __andnot optimization only makes sense if __k._M_data is a
	      // vector register
	      __lhs._M_data
		= __andnot(__vector_bitcast<_Tp>(__k), __lhs._M_data);
	    else
	      // for AVX512/__mmask, a _mm512_maskz_mov is best
	      __lhs
		= _CommonImpl::_S_blend(__k, __lhs, _SimdWrapper<_Tp, _Np>());
	  }
	else
	  __lhs = _CommonImpl::_S_blend(__k, __lhs,
					_SimdWrapper<_Tp, _Np>(
					  __vector_broadcast<_Np>(__rhs)));
      }

    // _S_masked_cassign {{{2
    template <typename _Op, typename _Tp, typename _K, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_cassign(const _SimdWrapper<_K, _Np> __k,
			_SimdWrapper<_Tp, _Np>& __lhs,
			const __type_identity_t<_SimdWrapper<_Tp, _Np>> __rhs,
			_Op __op)
      {
	if (__k._M_is_constprop_none_of())
	  return;
	else if (__k._M_is_constprop_all_of())
	  __lhs = __op(_SuperImpl{}, __lhs, __rhs);
	else
	  __lhs = _CommonImpl::_S_blend(__k, __lhs,
					__op(_SuperImpl{}, __lhs, __rhs));
      }

    template <typename _Op, typename _Tp, typename _K, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_cassign(const _SimdWrapper<_K, _Np> __k,
			_SimdWrapper<_Tp, _Np>& __lhs,
			const __type_identity_t<_Tp> __rhs, _Op __op)
      { _S_masked_cassign(__k, __lhs, __vector_broadcast<_Np>(__rhs), __op); }

    // _S_masked_unary {{{2
    template <template <typename> class _Op, typename _Tp, typename _K,
	      size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static _SimdWrapper<_Tp, _Np>
      _S_masked_unary(const _SimdWrapper<_K, _Np> __k,
		      const _SimdWrapper<_Tp, _Np> __v)
      {
	if (__k._M_is_constprop_none_of())
	  return __v;
	auto __vv = _M_make_simd(__v);
	_Op<decltype(__vv)> __op;
	if (__k._M_is_constprop_all_of())
	  return __data(__op(__vv));
	else
	  return _CommonImpl::_S_blend(__k, __v, __data(__op(__vv)));
      }

    //}}}2
  };

// _MaskImplBuiltinMixin {{{1
struct _MaskImplBuiltinMixin
{
  template <typename _Tp>
    using _TypeTag = _Tp*;

  // _S_to_maskvector {{{
  template <typename _Up, size_t _ToN = 1>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Up, _ToN>
    _S_to_maskvector(bool __x)
    {
      static_assert(is_same_v<_Up, __int_for_sizeof_t<_Up>>);
      return __x ? __vector_type_t<_Up, _ToN>{~_Up()}
		 : __vector_type_t<_Up, _ToN>{};
    }

  template <typename _Up, size_t _UpN = 0, size_t _Np, bool _Sanitized,
	    size_t _ToN = _UpN == 0 ? _Np : _UpN>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Up, _ToN>
    _S_to_maskvector(_BitMask<_Np, _Sanitized> __x)
    {
      static_assert(is_same_v<_Up, __int_for_sizeof_t<_Up>>);
      return __generate_vector<__vector_type_t<_Up, _ToN>>([&](
	auto __i) constexpr {
	if constexpr (__i < _Np)
	  return __x[__i] ? ~_Up() : _Up();
	else
	  return _Up();
      });
    }

  template <typename _Up, size_t _UpN = 0, typename _Tp, size_t _Np,
	    size_t _ToN = _UpN == 0 ? _Np : _UpN>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Up, _ToN>
    _S_to_maskvector(_SimdWrapper<_Tp, _Np> __x)
    {
      static_assert(is_same_v<_Up, __int_for_sizeof_t<_Up>>);
      using _TW = _SimdWrapper<_Tp, _Np>;
      using _UW = _SimdWrapper<_Up, _ToN>;
      if constexpr (sizeof(_Up) == sizeof(_Tp) && sizeof(_TW) == sizeof(_UW))
	return __wrapper_bitcast<_Up, _ToN>(__x);
      else if constexpr (is_same_v<_Tp, bool>) // bits -> vector
	return _S_to_maskvector<_Up, _ToN>(_BitMask<_Np>(__x._M_data));
      else
	{ // vector -> vector
	  /*
	  [[maybe_unused]] const auto __y = __vector_bitcast<_Up>(__x._M_data);
	  if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 4 && sizeof(__y) ==
	  16) return __vector_permute<1, 3, -1, -1>(__y); else if constexpr
	  (sizeof(_Tp) == 4 && sizeof(_Up) == 2
			     && sizeof(__y) == 16)
	    return __vector_permute<1, 3, 5, 7, -1, -1, -1, -1>(__y);
	  else if constexpr (sizeof(_Tp) == 8 && sizeof(_Up) == 2
			     && sizeof(__y) == 16)
	    return __vector_permute<3, 7, -1, -1, -1, -1, -1, -1>(__y);
	  else if constexpr (sizeof(_Tp) == 2 && sizeof(_Up) == 1
			     && sizeof(__y) == 16)
	    return __vector_permute<1, 3, 5, 7, 9, 11, 13, 15, -1, -1, -1, -1,
	  -1, -1, -1, -1>(__y); else if constexpr (sizeof(_Tp) == 4 &&
	  sizeof(_Up) == 1
			     && sizeof(__y) == 16)
	    return __vector_permute<3, 7, 11, 15, -1, -1, -1, -1, -1, -1, -1,
	  -1, -1, -1, -1, -1>(__y); else if constexpr (sizeof(_Tp) == 8 &&
	  sizeof(_Up) == 1
			     && sizeof(__y) == 16)
	    return __vector_permute<7, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
	  -1, -1, -1, -1, -1>(__y); else
	  */
	  {
	    return __generate_vector<__vector_type_t<_Up, _ToN>>([&](
	      auto __i) constexpr {
	      if constexpr (__i < _Np)
		return _Up(__x[__i.value]);
	      else
		return _Up();
	    });
	  }
	}
    }

  // }}}
  // _S_to_bits {{{
  template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SanitizedBitMask<_Np>
    _S_to_bits(_SimdWrapper<_Tp, _Np> __x)
    {
      static_assert(!is_same_v<_Tp, bool>);
      static_assert(_Np <= __CHAR_BIT__ * sizeof(_ULLong));
      using _Up = make_unsigned_t<__int_for_sizeof_t<_Tp>>;
      const auto __bools
	= __vector_bitcast<_Up>(__x) >> (sizeof(_Up) * __CHAR_BIT__ - 1);
      _ULLong __r = 0;
      __execute_n_times<_Np>(
	[&](auto __i) { __r |= _ULLong(__bools[__i.value]) << __i; });
      return __r;
    }

  // }}}
};

// _MaskImplBuiltin {{{1
template <typename _Abi, typename>
  struct _MaskImplBuiltin : _MaskImplBuiltinMixin
  {
    using _MaskImplBuiltinMixin::_S_to_bits;
    using _MaskImplBuiltinMixin::_S_to_maskvector;

    // member types {{{
    template <typename _Tp>
      using _SimdMember = typename _Abi::template __traits<_Tp>::_SimdMember;

    template <typename _Tp>
      using _MaskMember = typename _Abi::template _MaskMember<_Tp>;

    using _SuperImpl = typename _Abi::_MaskImpl;
    using _CommonImpl = typename _Abi::_CommonImpl;

    template <typename _Tp>
      static constexpr size_t _S_size = simd_size_v<_Tp, _Abi>;

    // }}}
    // _S_broadcast {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_broadcast(bool __x)
      {
	return __x ? _Abi::template _S_implicit_mask<_Tp>()
		   : _MaskMember<_Tp>();
      }

    // }}}
    // _S_load {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember<_Tp>
      _S_load(const bool* __mem)
      {
	using _I = __int_for_sizeof_t<_Tp>;
	if constexpr (sizeof(_Tp) == sizeof(bool))
	  {
	    const auto __bools
	      = _CommonImpl::template _S_load<_I, _S_size<_Tp>>(__mem);
	    // bool is {0, 1}, everything else is UB
	    return __bools > 0;
	  }
	else
	  return __generate_vector<_I, _S_size<_Tp>>([&](auto __i) constexpr {
	    return __mem[__i] ? ~_I() : _I();
	  });
      }

    // }}}
    // _S_convert {{{
    template <typename _Tp, size_t _Np, bool _Sanitized>
      _GLIBCXX_SIMD_INTRINSIC static constexpr auto
      _S_convert(_BitMask<_Np, _Sanitized> __x)
      {
	if constexpr (__is_builtin_bitmask_abi<_Abi>())
	  return _SimdWrapper<bool, simd_size_v<_Tp, _Abi>>(__x._M_to_bits());
	else
	  return _SuperImpl::template _S_to_maskvector<__int_for_sizeof_t<_Tp>,
						       _S_size<_Tp>>(
	    __x._M_sanitized());
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr auto
      _S_convert(_SimdWrapper<bool, _Np> __x)
      {
	if constexpr (__is_builtin_bitmask_abi<_Abi>())
	  return _SimdWrapper<bool, simd_size_v<_Tp, _Abi>>(__x._M_data);
	else
	  return _SuperImpl::template _S_to_maskvector<__int_for_sizeof_t<_Tp>,
						       _S_size<_Tp>>(
	    _BitMask<_Np>(__x._M_data)._M_sanitized());
      }

    template <typename _Tp, typename _Up, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr auto
      _S_convert(_SimdWrapper<_Up, _Np> __x)
      {
	if constexpr (__is_builtin_bitmask_abi<_Abi>())
	  return _SimdWrapper<bool, simd_size_v<_Tp, _Abi>>(
	    _SuperImpl::_S_to_bits(__x));
	else
	  return _SuperImpl::template _S_to_maskvector<__int_for_sizeof_t<_Tp>,
						       _S_size<_Tp>>(__x);
      }

    template <typename _Tp, typename _Up, typename _UAbi>
      _GLIBCXX_SIMD_INTRINSIC static constexpr auto
      _S_convert(simd_mask<_Up, _UAbi> __x)
      {
	if constexpr (__is_builtin_bitmask_abi<_Abi>())
	  {
	    using _R = _SimdWrapper<bool, simd_size_v<_Tp, _Abi>>;
	    if constexpr (__is_builtin_bitmask_abi<_UAbi>()) // bits -> bits
	      return _R(__data(__x));
	    else if constexpr (__is_scalar_abi<_UAbi>()) // bool -> bits
	      return _R(__data(__x));
	    else if constexpr (__is_fixed_size_abi_v<_UAbi>) // bitset -> bits
	      return _R(__data(__x)._M_to_bits());
	    else // vector -> bits
	      return _R(_UAbi::_MaskImpl::_S_to_bits(__data(__x))._M_to_bits());
	  }
	else
	  return _SuperImpl::template _S_to_maskvector<__int_for_sizeof_t<_Tp>,
						       _S_size<_Tp>>(
	    __data(__x));
      }

    // }}}
    // _S_masked_load {{{2
    template <typename _Tp, size_t _Np>
      static inline _SimdWrapper<_Tp, _Np>
      _S_masked_load(_SimdWrapper<_Tp, _Np> __merge,
		     _SimdWrapper<_Tp, _Np> __mask, const bool* __mem) noexcept
      {
	// AVX(2) has 32/64 bit maskload, but nothing at 8 bit granularity
	auto __tmp = __wrapper_bitcast<__int_for_sizeof_t<_Tp>>(__merge);
	_BitOps::_S_bit_iteration(_SuperImpl::_S_to_bits(__mask),
				  [&](auto __i) {
				    __tmp._M_set(__i, -__mem[__i]);
				  });
	__merge = __wrapper_bitcast<_Tp>(__tmp);
	return __merge;
      }

    // _S_store {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void _S_store(_SimdWrapper<_Tp, _Np> __v,
						   bool* __mem) noexcept
      {
	__execute_n_times<_Np>([&](auto __i) constexpr {
	  __mem[__i] = __v[__i];
	});
      }

    // _S_masked_store {{{2
    template <typename _Tp, size_t _Np>
      static inline void
      _S_masked_store(const _SimdWrapper<_Tp, _Np> __v, bool* __mem,
		      const _SimdWrapper<_Tp, _Np> __k) noexcept
      {
	_BitOps::_S_bit_iteration(
	  _SuperImpl::_S_to_bits(__k), [&](auto __i) constexpr {
	    __mem[__i] = __v[__i];
	  });
      }

    // _S_from_bitmask{{{2
    template <size_t _Np, typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember<_Tp>
      _S_from_bitmask(_SanitizedBitMask<_Np> __bits, _TypeTag<_Tp>)
      {
	return _SuperImpl::template _S_to_maskvector<_Tp, _S_size<_Tp>>(__bits);
      }

    // logical and bitwise operators {{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_logical_and(const _SimdWrapper<_Tp, _Np>& __x,
		     const _SimdWrapper<_Tp, _Np>& __y)
      { return __and(__x._M_data, __y._M_data); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_logical_or(const _SimdWrapper<_Tp, _Np>& __x,
		    const _SimdWrapper<_Tp, _Np>& __y)
      { return __or(__x._M_data, __y._M_data); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_not(const _SimdWrapper<_Tp, _Np>& __x)
      {
	if constexpr (_Abi::template _S_is_partial<_Tp>)
	  return __andnot(__x, __wrapper_bitcast<_Tp>(
				 _Abi::template _S_implicit_mask<_Tp>()));
	else
	  return __not(__x._M_data);
      }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_and(const _SimdWrapper<_Tp, _Np>& __x,
		 const _SimdWrapper<_Tp, _Np>& __y)
      { return __and(__x._M_data, __y._M_data); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_or(const _SimdWrapper<_Tp, _Np>& __x,
		const _SimdWrapper<_Tp, _Np>& __y)
      { return __or(__x._M_data, __y._M_data); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdWrapper<_Tp, _Np>
      _S_bit_xor(const _SimdWrapper<_Tp, _Np>& __x,
		 const _SimdWrapper<_Tp, _Np>& __y)
      { return __xor(__x._M_data, __y._M_data); }

    // smart_reference access {{{2
    template <typename _Tp, size_t _Np>
      static constexpr void _S_set(_SimdWrapper<_Tp, _Np>& __k, int __i,
				   bool __x) noexcept
      {
	if constexpr (is_same_v<_Tp, bool>)
	  __k._M_set(__i, __x);
	else
	  {
	    static_assert(is_same_v<_Tp, __int_for_sizeof_t<_Tp>>);
	    if (__builtin_is_constant_evaluated())
	      {
		__k = __generate_from_n_evaluations<_Np,
						    __vector_type_t<_Tp, _Np>>(
		  [&](auto __j) {
		    if (__i == __j)
		      return _Tp(-__x);
		    else
		      return __k[+__j];
		  });
	      }
	    else
	      __k._M_data[__i] = -__x;
	  }
      }

    // _S_masked_assign{{{2
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_assign(_SimdWrapper<_Tp, _Np> __k,
		       _SimdWrapper<_Tp, _Np>& __lhs,
		       __type_identity_t<_SimdWrapper<_Tp, _Np>> __rhs)
      { __lhs = _CommonImpl::_S_blend(__k, __lhs, __rhs); }

    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_assign(_SimdWrapper<_Tp, _Np> __k,
		       _SimdWrapper<_Tp, _Np>& __lhs, bool __rhs)
      {
	if (__builtin_constant_p(__rhs))
	  {
	    if (__rhs == false)
	      __lhs = __andnot(__k, __lhs);
	    else
	      __lhs = __or(__k, __lhs);
	    return;
	  }
	__lhs = _CommonImpl::_S_blend(__k, __lhs,
				      __data(simd_mask<_Tp, _Abi>(__rhs)));
      }

    //}}}2
    // _S_all_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool
      _S_all_of(simd_mask<_Tp, _Abi> __k)
      {
	return __call_with_subscripts(
	  __data(__k), make_index_sequence<_S_size<_Tp>>(),
	  [](const auto... __ent) constexpr { return (... && !(__ent == 0)); });
      }

    // }}}
    // _S_any_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool
      _S_any_of(simd_mask<_Tp, _Abi> __k)
      {
	return __call_with_subscripts(
	  __data(__k), make_index_sequence<_S_size<_Tp>>(),
	  [](const auto... __ent) constexpr { return (... || !(__ent == 0)); });
      }

    // }}}
    // _S_none_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool
      _S_none_of(simd_mask<_Tp, _Abi> __k)
      {
	return __call_with_subscripts(
	  __data(__k), make_index_sequence<_S_size<_Tp>>(),
	  [](const auto... __ent) constexpr { return (... && (__ent == 0)); });
      }

    // }}}
    // _S_some_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool
      _S_some_of(simd_mask<_Tp, _Abi> __k)
      {
	const int __n_true = _SuperImpl::_S_popcount(__k);
	return __n_true > 0 && __n_true < int(_S_size<_Tp>);
      }

    // }}}
    // _S_popcount {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_popcount(simd_mask<_Tp, _Abi> __k)
      {
	using _I = __int_for_sizeof_t<_Tp>;
	if constexpr (is_default_constructible_v<simd<_I, _Abi>>)
	  return -reduce(
	    simd<_I, _Abi>(__private_init, __wrapper_bitcast<_I>(__data(__k))));
	else
	  return -reduce(__bit_cast<rebind_simd_t<_I, simd<_Tp, _Abi>>>(
	    simd<_Tp, _Abi>(__private_init, __data(__k))));
      }

    // }}}
    // _S_find_first_set {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_find_first_set(simd_mask<_Tp, _Abi> __k)
      {
	return std::__countr_zero(
	  _SuperImpl::_S_to_bits(__data(__k))._M_to_bits());
      }

    // }}}
    // _S_find_last_set {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_find_last_set(simd_mask<_Tp, _Abi> __k)
      {
	return std::__bit_width(
	  _SuperImpl::_S_to_bits(__data(__k))._M_to_bits()) - 1;
      }

    // }}}
  };

//}}}1
_GLIBCXX_SIMD_END_NAMESPACE
#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_ABIS_H_

// vim: foldmethod=marker foldmarker={{{,}}} sw=2 noet ts=8 sts=2 tw=100
