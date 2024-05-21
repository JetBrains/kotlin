// Simd NEON specific implementations -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_NEON_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_NEON_H_

#if __cplusplus >= 201703L

#if !_GLIBCXX_SIMD_HAVE_NEON
#error "simd_neon.h may only be included when NEON on ARM is available"
#endif

_GLIBCXX_SIMD_BEGIN_NAMESPACE

// _CommonImplNeon {{{
struct _CommonImplNeon : _CommonImplBuiltin
{
  // _S_store {{{
  using _CommonImplBuiltin::_S_store;

  // }}}
};

// }}}
// _SimdImplNeon {{{
template <typename _Abi, typename>
  struct _SimdImplNeon : _SimdImplBuiltin<_Abi>
  {
    using _Base = _SimdImplBuiltin<_Abi>;

    template <typename _Tp>
      using _MaskMember = typename _Base::template _MaskMember<_Tp>;

    template <typename _Tp>
      static constexpr size_t _S_max_store_size = 16;

    // _S_masked_load {{{
    template <typename _Tp, size_t _Np, typename _Up>
      static inline _SimdWrapper<_Tp, _Np>
      _S_masked_load(_SimdWrapper<_Tp, _Np> __merge, _MaskMember<_Tp> __k,
		     const _Up* __mem) noexcept
      {
	__execute_n_times<_Np>([&](auto __i) {
	  if (__k[__i] != 0)
	    __merge._M_set(__i, static_cast<_Tp>(__mem[__i]));
	});
	return __merge;
      }

    // }}}
    // _S_masked_store_nocvt {{{
    template <typename _Tp, size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_store_nocvt(_SimdWrapper<_Tp, _Np> __v, _Tp* __mem,
			    _MaskMember<_Tp> __k)
      {
	__execute_n_times<_Np>([&](auto __i) {
	  if (__k[__i] != 0)
	    __mem[__i] = __v[__i];
	});
      }

    // }}}
    // _S_reduce {{{
    template <typename _Tp, typename _BinaryOperation>
      _GLIBCXX_SIMD_INTRINSIC static _Tp
      _S_reduce(simd<_Tp, _Abi> __x, _BinaryOperation&& __binary_op)
      {
	constexpr size_t _Np = __x.size();
	if constexpr (sizeof(__x) == 16 && _Np >= 4
		      && !_Abi::template _S_is_partial<_Tp>)
	  {
	    const auto __halves = split<simd<_Tp, simd_abi::_Neon<8>>>(__x);
	    const auto __y = __binary_op(__halves[0], __halves[1]);
	    return _SimdImplNeon<simd_abi::_Neon<8>>::_S_reduce(
	      __y, static_cast<_BinaryOperation&&>(__binary_op));
	  }
	else if constexpr (_Np == 8)
	  {
	    __x = __binary_op(__x, _Base::template _M_make_simd<_Tp, _Np>(
				     __vector_permute<1, 0, 3, 2, 5, 4, 7, 6>(
				       __x._M_data)));
	    __x = __binary_op(__x, _Base::template _M_make_simd<_Tp, _Np>(
				     __vector_permute<3, 2, 1, 0, 7, 6, 5, 4>(
				       __x._M_data)));
	    __x = __binary_op(__x, _Base::template _M_make_simd<_Tp, _Np>(
				     __vector_permute<7, 6, 5, 4, 3, 2, 1, 0>(
				       __x._M_data)));
	    return __x[0];
	  }
	else if constexpr (_Np == 4)
	  {
	    __x
	      = __binary_op(__x, _Base::template _M_make_simd<_Tp, _Np>(
				   __vector_permute<1, 0, 3, 2>(__x._M_data)));
	    __x
	      = __binary_op(__x, _Base::template _M_make_simd<_Tp, _Np>(
				   __vector_permute<3, 2, 1, 0>(__x._M_data)));
	    return __x[0];
	  }
	else if constexpr (_Np == 2)
	  {
	    __x = __binary_op(__x, _Base::template _M_make_simd<_Tp, _Np>(
				     __vector_permute<1, 0>(__x._M_data)));
	    return __x[0];
	  }
	else
	  return _Base::_S_reduce(__x,
				  static_cast<_BinaryOperation&&>(__binary_op));
      }

    // }}}
    // math {{{
    // _S_sqrt {{{
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      _GLIBCXX_SIMD_INTRINSIC static _Tp _S_sqrt(_Tp __x)
      {
	if constexpr (__have_neon_a64)
	  {
	    const auto __intrin = __to_intrin(__x);
	    if constexpr (_TVT::template _S_is<float, 2>)
	      return vsqrt_f32(__intrin);
	    else if constexpr (_TVT::template _S_is<float, 4>)
	      return vsqrtq_f32(__intrin);
	    else if constexpr (_TVT::template _S_is<double, 1>)
	      return vsqrt_f64(__intrin);
	    else if constexpr (_TVT::template _S_is<double, 2>)
	      return vsqrtq_f64(__intrin);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_sqrt(__x);
      }

    // }}}
    // _S_trunc {{{
    template <typename _TW, typename _TVT = _VectorTraits<_TW>>
      _GLIBCXX_SIMD_INTRINSIC static _TW _S_trunc(_TW __x)
      {
	using _Tp = typename _TVT::value_type;
	if constexpr (__have_neon_a32)
	  {
	    const auto __intrin = __to_intrin(__x);
	    if constexpr (_TVT::template _S_is<float, 2>)
	      return vrnd_f32(__intrin);
	    else if constexpr (_TVT::template _S_is<float, 4>)
	      return vrndq_f32(__intrin);
	    else if constexpr (_TVT::template _S_is<double, 1>)
	      return vrnd_f64(__intrin);
	    else if constexpr (_TVT::template _S_is<double, 2>)
	      return vrndq_f64(__intrin);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else if constexpr (is_same_v<_Tp, float>)
	  {
	    auto __intrin = __to_intrin(__x);
	    if constexpr (sizeof(__x) == 16)
	      __intrin = vcvtq_f32_s32(vcvtq_s32_f32(__intrin));
	    else
	      __intrin = vcvt_f32_s32(vcvt_s32_f32(__intrin));
	    return _Base::_S_abs(__x)._M_data < 0x1p23f
		     ? __vector_bitcast<float>(__intrin)
		     : __x._M_data;
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
	if constexpr (__have_neon_a32)
	  {
	    const auto __intrin = __to_intrin(__x);
	    if constexpr (sizeof(_Tp) == 4 && sizeof(__x) == 8)
	      return vrnda_f32(__intrin);
	    else if constexpr (sizeof(_Tp) == 4 && sizeof(__x) == 16)
	      return vrndaq_f32(__intrin);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(__x) == 8)
	      return vrnda_f64(__intrin);
	    else if constexpr (sizeof(_Tp) == 8 && sizeof(__x) == 16)
	      return vrndaq_f64(__intrin);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_round(__x);
      }

    // }}}
    // _S_floor {{{
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      _GLIBCXX_SIMD_INTRINSIC static _Tp _S_floor(_Tp __x)
      {
	if constexpr (__have_neon_a32)
	  {
	    const auto __intrin = __to_intrin(__x);
	    if constexpr (_TVT::template _S_is<float, 2>)
	      return vrndm_f32(__intrin);
	    else if constexpr (_TVT::template _S_is<float, 4>)
	      return vrndmq_f32(__intrin);
	    else if constexpr (_TVT::template _S_is<double, 1>)
	      return vrndm_f64(__intrin);
	    else if constexpr (_TVT::template _S_is<double, 2>)
	      return vrndmq_f64(__intrin);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_floor(__x);
      }

    // }}}
    // _S_ceil {{{
    template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
      _GLIBCXX_SIMD_INTRINSIC static _Tp _S_ceil(_Tp __x)
      {
	if constexpr (__have_neon_a32)
	  {
	    const auto __intrin = __to_intrin(__x);
	    if constexpr (_TVT::template _S_is<float, 2>)
	      return vrndp_f32(__intrin);
	    else if constexpr (_TVT::template _S_is<float, 4>)
	      return vrndpq_f32(__intrin);
	    else if constexpr (_TVT::template _S_is<double, 1>)
	      return vrndp_f64(__intrin);
	    else if constexpr (_TVT::template _S_is<double, 2>)
	      return vrndpq_f64(__intrin);
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return _Base::_S_ceil(__x);
      }

    //}}} }}}
  }; // }}}
// _MaskImplNeonMixin {{{
struct _MaskImplNeonMixin
{
  using _Base = _MaskImplBuiltinMixin;

  template <typename _Tp, size_t _Np>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SanitizedBitMask<_Np>
    _S_to_bits(_SimdWrapper<_Tp, _Np> __x)
    {
      if (__builtin_is_constant_evaluated())
	return _Base::_S_to_bits(__x);

      using _I = __int_for_sizeof_t<_Tp>;
      if constexpr (sizeof(__x) == 16)
	{
	  auto __asint = __vector_bitcast<_I>(__x);
#ifdef __aarch64__
	  [[maybe_unused]] constexpr auto __zero = decltype(__asint)();
#else
	  [[maybe_unused]] constexpr auto __zero = decltype(__lo64(__asint))();
#endif
	  if constexpr (sizeof(_Tp) == 1)
	    {
	      constexpr auto __bitsel
		= __generate_from_n_evaluations<16, __vector_type_t<_I, 16>>(
		  [&](auto __i) {
		    return static_cast<_I>(
		      __i < _Np ? (__i < 8 ? 1 << __i : 1 << (__i - 8)) : 0);
		  });
	      __asint &= __bitsel;
#ifdef __aarch64__
	      return __vector_bitcast<_UShort>(
		vpaddq_s8(vpaddq_s8(vpaddq_s8(__asint, __zero), __zero),
			  __zero))[0];
#else
	      return __vector_bitcast<_UShort>(
		vpadd_s8(vpadd_s8(vpadd_s8(__lo64(__asint), __hi64(__asint)),
				  __zero),
			 __zero))[0];
#endif
	    }
	  else if constexpr (sizeof(_Tp) == 2)
	    {
	      constexpr auto __bitsel
		= __generate_from_n_evaluations<8, __vector_type_t<_I, 8>>(
		  [&](auto __i) {
		    return static_cast<_I>(__i < _Np ? 1 << __i : 0);
		  });
	      __asint &= __bitsel;
#ifdef __aarch64__
	      return vaddvq_s16(__asint);
#else
	      return vpadd_s16(
		vpadd_s16(vpadd_s16(__lo64(__asint), __hi64(__asint)), __zero),
		__zero)[0];
#endif
	    }
	  else if constexpr (sizeof(_Tp) == 4)
	    {
	      constexpr auto __bitsel
		= __generate_from_n_evaluations<4, __vector_type_t<_I, 4>>(
		  [&](auto __i) {
		    return static_cast<_I>(__i < _Np ? 1 << __i : 0);
		  });
	      __asint &= __bitsel;
#ifdef __aarch64__
	      return vaddvq_s32(__asint);
#else
	      return vpadd_s32(vpadd_s32(__lo64(__asint), __hi64(__asint)),
			       __zero)[0];
#endif
	    }
	  else if constexpr (sizeof(_Tp) == 8)
	    return (__asint[0] & 1) | (__asint[1] & 2);
	  else
	    __assert_unreachable<_Tp>();
	}
      else if constexpr (sizeof(__x) == 8)
	{
	  auto __asint = __vector_bitcast<_I>(__x);
	  [[maybe_unused]] constexpr auto __zero = decltype(__asint)();
	  if constexpr (sizeof(_Tp) == 1)
	    {
	      constexpr auto __bitsel
		= __generate_from_n_evaluations<8, __vector_type_t<_I, 8>>(
		  [&](auto __i) {
		    return static_cast<_I>(__i < _Np ? 1 << __i : 0);
		  });
	      __asint &= __bitsel;
#ifdef __aarch64__
	      return vaddv_s8(__asint);
#else
	      return vpadd_s8(vpadd_s8(vpadd_s8(__asint, __zero), __zero),
			      __zero)[0];
#endif
	    }
	  else if constexpr (sizeof(_Tp) == 2)
	    {
	      constexpr auto __bitsel
		= __generate_from_n_evaluations<4, __vector_type_t<_I, 4>>(
		  [&](auto __i) {
		    return static_cast<_I>(__i < _Np ? 1 << __i : 0);
		  });
	      __asint &= __bitsel;
#ifdef __aarch64__
	      return vaddv_s16(__asint);
#else
	      return vpadd_s16(vpadd_s16(__asint, __zero), __zero)[0];
#endif
	    }
	  else if constexpr (sizeof(_Tp) == 4)
	    {
	      __asint &= __make_vector<_I>(0x1, 0x2);
#ifdef __aarch64__
	      return vaddv_s32(__asint);
#else
	      return vpadd_s32(__asint, __zero)[0];
#endif
	    }
	  else
	    __assert_unreachable<_Tp>();
	}
      else
	return _Base::_S_to_bits(__x);
    }
};

// }}}
// _MaskImplNeon {{{
template <typename _Abi, typename>
  struct _MaskImplNeon : _MaskImplNeonMixin, _MaskImplBuiltin<_Abi>
  {
    using _MaskImplBuiltinMixin::_S_to_maskvector;
    using _MaskImplNeonMixin::_S_to_bits;
    using _Base = _MaskImplBuiltin<_Abi>;
    using _Base::_S_convert;

    // _S_all_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_all_of(simd_mask<_Tp, _Abi> __k)
      {
	const auto __kk
	  = __vector_bitcast<char>(__k._M_data)
	    | ~__vector_bitcast<char>(_Abi::template _S_implicit_mask<_Tp>());
	if constexpr (sizeof(__k) == 16)
	  {
	    const auto __x = __vector_bitcast<long long>(__kk);
	    return __x[0] + __x[1] == -2;
	  }
	else if constexpr (sizeof(__k) <= 8)
	  return __bit_cast<__int_for_sizeof_t<decltype(__kk)>>(__kk) == -1;
	else
	  __assert_unreachable<_Tp>();
      }

    // }}}
    // _S_any_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_any_of(simd_mask<_Tp, _Abi> __k)
      {
	const auto __kk
	  = __vector_bitcast<char>(__k._M_data)
	    | ~__vector_bitcast<char>(_Abi::template _S_implicit_mask<_Tp>());
	if constexpr (sizeof(__k) == 16)
	  {
	    const auto __x = __vector_bitcast<long long>(__kk);
	    return (__x[0] | __x[1]) != 0;
	  }
	else if constexpr (sizeof(__k) <= 8)
	  return __bit_cast<__int_for_sizeof_t<decltype(__kk)>>(__kk) != 0;
	else
	  __assert_unreachable<_Tp>();
      }

    // }}}
    // _S_none_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_none_of(simd_mask<_Tp, _Abi> __k)
      {
	const auto __kk = _Abi::_S_masked(__k._M_data);
	if constexpr (sizeof(__k) == 16)
	  {
	    const auto __x = __vector_bitcast<long long>(__kk);
	    return (__x[0] | __x[1]) == 0;
	  }
	else if constexpr (sizeof(__k) <= 8)
	  return __bit_cast<__int_for_sizeof_t<decltype(__kk)>>(__kk) == 0;
	else
	  __assert_unreachable<_Tp>();
      }

    // }}}
    // _S_some_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_some_of(simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (sizeof(__k) <= 8)
	  {
	    const auto __kk = __vector_bitcast<char>(__k._M_data)
			      | ~__vector_bitcast<char>(
				_Abi::template _S_implicit_mask<_Tp>());
	    using _Up = make_unsigned_t<__int_for_sizeof_t<decltype(__kk)>>;
	    return __bit_cast<_Up>(__kk) + 1 > 1;
	  }
	else
	  return _Base::_S_some_of(__k);
      }

    // }}}
    // _S_popcount {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int _S_popcount(simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (sizeof(_Tp) == 1)
	  {
	    const auto __s8 = __vector_bitcast<_SChar>(__k._M_data);
	    int8x8_t __tmp = __lo64(__s8) + __hi64z(__s8);
	    return -vpadd_s8(vpadd_s8(vpadd_s8(__tmp, int8x8_t()), int8x8_t()),
			     int8x8_t())[0];
	  }
	else if constexpr (sizeof(_Tp) == 2)
	  {
	    const auto __s16 = __vector_bitcast<short>(__k._M_data);
	    int16x4_t __tmp = __lo64(__s16) + __hi64z(__s16);
	    return -vpadd_s16(vpadd_s16(__tmp, int16x4_t()), int16x4_t())[0];
	  }
	else if constexpr (sizeof(_Tp) == 4)
	  {
	    const auto __s32 = __vector_bitcast<int>(__k._M_data);
	    int32x2_t __tmp = __lo64(__s32) + __hi64z(__s32);
	    return -vpadd_s32(__tmp, int32x2_t())[0];
	  }
	else if constexpr (sizeof(_Tp) == 8)
	  {
	    static_assert(sizeof(__k) == 16);
	    const auto __s64 = __vector_bitcast<long>(__k._M_data);
	    return -(__s64[0] + __s64[1]);
	  }
      }

    // }}}
    // _S_find_first_set {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_find_first_set(simd_mask<_Tp, _Abi> __k)
      {
	// TODO: the _Base implementation is not optimal for NEON
	return _Base::_S_find_first_set(__k);
      }

    // }}}
    // _S_find_last_set {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_find_last_set(simd_mask<_Tp, _Abi> __k)
      {
	// TODO: the _Base implementation is not optimal for NEON
	return _Base::_S_find_last_set(__k);
      }

    // }}}
  }; // }}}

_GLIBCXX_SIMD_END_NAMESPACE
#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_NEON_H_
// vim: foldmethod=marker sw=2 noet ts=8 sts=2 tw=80
