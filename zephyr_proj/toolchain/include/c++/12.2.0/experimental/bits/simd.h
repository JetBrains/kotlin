// Definition of the public simd interfaces -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_H
#define _GLIBCXX_EXPERIMENTAL_SIMD_H

#if __cplusplus >= 201703L

#include "simd_detail.h"
#include "numeric_traits.h"
#include <bit>
#include <bitset>
#ifdef _GLIBCXX_DEBUG_UB
#include <cstdio> // for stderr
#endif
#include <cstring>
#include <cmath>
#include <functional>
#include <iosfwd>
#include <utility>

#if _GLIBCXX_SIMD_X86INTRIN
#include <x86intrin.h>
#elif _GLIBCXX_SIMD_HAVE_NEON
#include <arm_neon.h>
#endif

/** @ingroup ts_simd
 * @{
 */
/* There are several closely related types, with the following naming
 * convention:
 * _Tp: vectorizable (arithmetic) type (or any type)
 * _TV: __vector_type_t<_Tp, _Np>
 * _TW: _SimdWrapper<_Tp, _Np>
 * _TI: __intrinsic_type_t<_Tp, _Np>
 * _TVT: _VectorTraits<_TV> or _VectorTraits<_TW>
 * If one additional type is needed use _U instead of _T.
 * Otherwise use _T\d, _TV\d, _TW\d, TI\d, _TVT\d.
 *
 * More naming conventions:
 * _Ap or _Abi: An ABI tag from the simd_abi namespace
 * _Ip: often used for integer types with sizeof(_Ip) == sizeof(_Tp),
 *      _IV, _IW as for _TV, _TW
 * _Np: number of elements (not bytes)
 * _Bytes: number of bytes
 *
 * Variable names:
 * __k: mask object (vector- or bitmask)
 */
_GLIBCXX_SIMD_BEGIN_NAMESPACE

#if !_GLIBCXX_SIMD_X86INTRIN
using __m128  [[__gnu__::__vector_size__(16)]] = float;
using __m128d [[__gnu__::__vector_size__(16)]] = double;
using __m128i [[__gnu__::__vector_size__(16)]] = long long;
using __m256  [[__gnu__::__vector_size__(32)]] = float;
using __m256d [[__gnu__::__vector_size__(32)]] = double;
using __m256i [[__gnu__::__vector_size__(32)]] = long long;
using __m512  [[__gnu__::__vector_size__(64)]] = float;
using __m512d [[__gnu__::__vector_size__(64)]] = double;
using __m512i [[__gnu__::__vector_size__(64)]] = long long;
#endif

namespace simd_abi {
// simd_abi forward declarations {{{
// implementation details:
struct _Scalar;

template <int _Np>
  struct _Fixed;

// There are two major ABIs that appear on different architectures.
// Both have non-boolean values packed into an N Byte register
// -> #elements = N / sizeof(T)
// Masks differ:
// 1. Use value vector registers for masks (all 0 or all 1)
// 2. Use bitmasks (mask registers) with one bit per value in the corresponding
//    value vector
//
// Both can be partially used, masking off the rest when doing horizontal
// operations or operations that can trap (e.g. FP_INVALID or integer division
// by 0). This is encoded as the number of used bytes.
template <int _UsedBytes>
  struct _VecBuiltin;

template <int _UsedBytes>
  struct _VecBltnBtmsk;

template <typename _Tp, int _Np>
  using _VecN = _VecBuiltin<sizeof(_Tp) * _Np>;

template <int _UsedBytes = 16>
  using _Sse = _VecBuiltin<_UsedBytes>;

template <int _UsedBytes = 32>
  using _Avx = _VecBuiltin<_UsedBytes>;

template <int _UsedBytes = 64>
  using _Avx512 = _VecBltnBtmsk<_UsedBytes>;

template <int _UsedBytes = 16>
  using _Neon = _VecBuiltin<_UsedBytes>;

// implementation-defined:
using __sse = _Sse<>;
using __avx = _Avx<>;
using __avx512 = _Avx512<>;
using __neon = _Neon<>;
using __neon128 = _Neon<16>;
using __neon64 = _Neon<8>;

// standard:
template <typename _Tp, size_t _Np, typename...>
  struct deduce;

template <int _Np>
  using fixed_size = _Fixed<_Np>;

using scalar = _Scalar;

// }}}
} // namespace simd_abi
// forward declarations is_simd(_mask), simd(_mask), simd_size {{{
template <typename _Tp>
  struct is_simd;

template <typename _Tp>
  struct is_simd_mask;

template <typename _Tp, typename _Abi>
  class simd;

template <typename _Tp, typename _Abi>
  class simd_mask;

template <typename _Tp, typename _Abi>
  struct simd_size;

// }}}
// load/store flags {{{
struct element_aligned_tag
{
  template <typename _Tp, typename _Up = typename _Tp::value_type>
    static constexpr size_t _S_alignment = alignof(_Up);

  template <typename _Tp, typename _Up>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _Up*
    _S_apply(_Up* __ptr)
    { return __ptr; }
};

struct vector_aligned_tag
{
  template <typename _Tp, typename _Up = typename _Tp::value_type>
    static constexpr size_t _S_alignment
      = std::__bit_ceil(sizeof(_Up) * _Tp::size());

  template <typename _Tp, typename _Up>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _Up*
    _S_apply(_Up* __ptr)
    {
      return static_cast<_Up*>(
	__builtin_assume_aligned(__ptr, _S_alignment<_Tp, _Up>));
    }
};

template <size_t _Np> struct overaligned_tag
{
  template <typename _Tp, typename _Up = typename _Tp::value_type>
    static constexpr size_t _S_alignment = _Np;

  template <typename _Tp, typename _Up>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _Up*
    _S_apply(_Up* __ptr)
    { return static_cast<_Up*>(__builtin_assume_aligned(__ptr, _Np)); }
};

inline constexpr element_aligned_tag element_aligned = {};

inline constexpr vector_aligned_tag vector_aligned = {};

template <size_t _Np>
  inline constexpr overaligned_tag<_Np> overaligned = {};

// }}}
template <size_t _Xp>
  using _SizeConstant = integral_constant<size_t, _Xp>;
// constexpr feature detection{{{
constexpr inline bool __have_mmx = _GLIBCXX_SIMD_HAVE_MMX;
constexpr inline bool __have_sse = _GLIBCXX_SIMD_HAVE_SSE;
constexpr inline bool __have_sse2 = _GLIBCXX_SIMD_HAVE_SSE2;
constexpr inline bool __have_sse3 = _GLIBCXX_SIMD_HAVE_SSE3;
constexpr inline bool __have_ssse3 = _GLIBCXX_SIMD_HAVE_SSSE3;
constexpr inline bool __have_sse4_1 = _GLIBCXX_SIMD_HAVE_SSE4_1;
constexpr inline bool __have_sse4_2 = _GLIBCXX_SIMD_HAVE_SSE4_2;
constexpr inline bool __have_xop = _GLIBCXX_SIMD_HAVE_XOP;
constexpr inline bool __have_avx = _GLIBCXX_SIMD_HAVE_AVX;
constexpr inline bool __have_avx2 = _GLIBCXX_SIMD_HAVE_AVX2;
constexpr inline bool __have_bmi = _GLIBCXX_SIMD_HAVE_BMI1;
constexpr inline bool __have_bmi2 = _GLIBCXX_SIMD_HAVE_BMI2;
constexpr inline bool __have_lzcnt = _GLIBCXX_SIMD_HAVE_LZCNT;
constexpr inline bool __have_sse4a = _GLIBCXX_SIMD_HAVE_SSE4A;
constexpr inline bool __have_fma = _GLIBCXX_SIMD_HAVE_FMA;
constexpr inline bool __have_fma4 = _GLIBCXX_SIMD_HAVE_FMA4;
constexpr inline bool __have_f16c = _GLIBCXX_SIMD_HAVE_F16C;
constexpr inline bool __have_popcnt = _GLIBCXX_SIMD_HAVE_POPCNT;
constexpr inline bool __have_avx512f = _GLIBCXX_SIMD_HAVE_AVX512F;
constexpr inline bool __have_avx512dq = _GLIBCXX_SIMD_HAVE_AVX512DQ;
constexpr inline bool __have_avx512vl = _GLIBCXX_SIMD_HAVE_AVX512VL;
constexpr inline bool __have_avx512bw = _GLIBCXX_SIMD_HAVE_AVX512BW;
constexpr inline bool __have_avx512dq_vl = __have_avx512dq && __have_avx512vl;
constexpr inline bool __have_avx512bw_vl = __have_avx512bw && __have_avx512vl;
constexpr inline bool __have_avx512bitalg = _GLIBCXX_SIMD_HAVE_AVX512BITALG;
constexpr inline bool __have_avx512vbmi2 = _GLIBCXX_SIMD_HAVE_AVX512VBMI2;
constexpr inline bool __have_avx512vbmi = _GLIBCXX_SIMD_HAVE_AVX512VBMI;
constexpr inline bool __have_avx512ifma = _GLIBCXX_SIMD_HAVE_AVX512IFMA;
constexpr inline bool __have_avx512cd = _GLIBCXX_SIMD_HAVE_AVX512CD;
constexpr inline bool __have_avx512vnni = _GLIBCXX_SIMD_HAVE_AVX512VNNI;
constexpr inline bool __have_avx512vpopcntdq = _GLIBCXX_SIMD_HAVE_AVX512VPOPCNTDQ;
constexpr inline bool __have_avx512vp2intersect = _GLIBCXX_SIMD_HAVE_AVX512VP2INTERSECT;

constexpr inline bool __have_neon = _GLIBCXX_SIMD_HAVE_NEON;
constexpr inline bool __have_neon_a32 = _GLIBCXX_SIMD_HAVE_NEON_A32;
constexpr inline bool __have_neon_a64 = _GLIBCXX_SIMD_HAVE_NEON_A64;
constexpr inline bool __support_neon_float =
#if defined __GCC_IEC_559
  __GCC_IEC_559 == 0;
#elif defined __FAST_MATH__
  true;
#else
  false;
#endif

#ifdef _ARCH_PWR10
constexpr inline bool __have_power10vec = true;
#else
constexpr inline bool __have_power10vec = false;
#endif
#ifdef __POWER9_VECTOR__
constexpr inline bool __have_power9vec = true;
#else
constexpr inline bool __have_power9vec = false;
#endif
#if defined __POWER8_VECTOR__
constexpr inline bool __have_power8vec = true;
#else
constexpr inline bool __have_power8vec = __have_power9vec;
#endif
#if defined __VSX__
constexpr inline bool __have_power_vsx = true;
#else
constexpr inline bool __have_power_vsx = __have_power8vec;
#endif
#if defined __ALTIVEC__
constexpr inline bool __have_power_vmx = true;
#else
constexpr inline bool __have_power_vmx = __have_power_vsx;
#endif

// }}}

namespace __detail
{
#ifdef math_errhandling
  // Determines _S_handle_fpexcept from math_errhandling if it is defined and expands to a constant
  // expression. math_errhandling may expand to an extern symbol, in which case a constexpr value
  // must be guessed.
  template <int = math_errhandling>
    constexpr bool __handle_fpexcept_impl(int)
    { return math_errhandling & MATH_ERREXCEPT; }
#endif

  // Fallback if math_errhandling doesn't work: with fast-math assume floating-point exceptions are
  // ignored, otherwise implement correct exception behavior.
  constexpr bool __handle_fpexcept_impl(float)
  {
#if defined __FAST_MATH__
    return false;
#else
    return true;
#endif
  }

  /// True if math functions must raise floating-point exceptions as specified by C17.
  static constexpr bool _S_handle_fpexcept = __handle_fpexcept_impl(0);

  constexpr std::uint_least64_t
  __floating_point_flags()
  {
    std::uint_least64_t __flags = 0;
    if constexpr (_S_handle_fpexcept)
      __flags |= 1;
#ifdef __FAST_MATH__
    __flags |= 1 << 1;
#elif __FINITE_MATH_ONLY__
    __flags |= 2 << 1;
#elif __GCC_IEC_559 < 2
    __flags |= 3 << 1;
#endif
    __flags |= (__FLT_EVAL_METHOD__ + 1) << 3;
    return __flags;
  }

  constexpr std::uint_least64_t
  __machine_flags()
  {
    if constexpr (__have_mmx || __have_sse)
      return __have_mmx
		 | (__have_sse                << 1)
		 | (__have_sse2               << 2)
		 | (__have_sse3               << 3)
		 | (__have_ssse3              << 4)
		 | (__have_sse4_1             << 5)
		 | (__have_sse4_2             << 6)
		 | (__have_xop                << 7)
		 | (__have_avx                << 8)
		 | (__have_avx2               << 9)
		 | (__have_bmi                << 10)
		 | (__have_bmi2               << 11)
		 | (__have_lzcnt              << 12)
		 | (__have_sse4a              << 13)
		 | (__have_fma                << 14)
		 | (__have_fma4               << 15)
		 | (__have_f16c               << 16)
		 | (__have_popcnt             << 17)
		 | (__have_avx512f            << 18)
		 | (__have_avx512dq           << 19)
		 | (__have_avx512vl           << 20)
		 | (__have_avx512bw           << 21)
		 | (__have_avx512bitalg       << 22)
		 | (__have_avx512vbmi2        << 23)
		 | (__have_avx512vbmi         << 24)
		 | (__have_avx512ifma         << 25)
		 | (__have_avx512cd           << 26)
		 | (__have_avx512vnni         << 27)
		 | (__have_avx512vpopcntdq    << 28)
		 | (__have_avx512vp2intersect << 29);
    else if constexpr (__have_neon)
      return __have_neon
	       | (__have_neon_a32 << 1)
	       | (__have_neon_a64 << 2)
	       | (__have_neon_a64 << 2)
	       | (__support_neon_float << 3);
    else if constexpr (__have_power_vmx)
      return __have_power_vmx
	       | (__have_power_vsx  << 1)
	       | (__have_power8vec  << 2)
	       | (__have_power9vec  << 3)
	       | (__have_power10vec << 4);
    else
      return 0;
  }

  namespace
  {
    struct _OdrEnforcer {};
  }

  template <std::uint_least64_t...>
    struct _MachineFlagsTemplate {};

  /**@internal
   * Use this type as default template argument to all function templates that
   * are not declared always_inline. It ensures, that a function
   * specialization, which the compiler decides not to inline, has a unique symbol
   * (_OdrEnforcer) or a symbol matching the machine/architecture flags
   * (_MachineFlagsTemplate). This helps to avoid ODR violations in cases where
   * users link TUs compiled with different flags. This is especially important
   * for using simd in libraries.
   */
  using __odr_helper
    = conditional_t<__machine_flags() == 0, _OdrEnforcer,
		    _MachineFlagsTemplate<__machine_flags(), __floating_point_flags()>>;

  struct _Minimum
  {
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC constexpr
      _Tp
      operator()(_Tp __a, _Tp __b) const
      {
	using std::min;
	return min(__a, __b);
      }
  };

  struct _Maximum
  {
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC constexpr
      _Tp
      operator()(_Tp __a, _Tp __b) const
      {
	using std::max;
	return max(__a, __b);
      }
  };
} // namespace __detail

// unrolled/pack execution helpers
// __execute_n_times{{{
template <typename _Fp, size_t... _I>
  [[__gnu__::__flatten__]] _GLIBCXX_SIMD_INTRINSIC constexpr
  void
  __execute_on_index_sequence(_Fp&& __f, index_sequence<_I...>)
  { ((void)__f(_SizeConstant<_I>()), ...); }

template <typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __execute_on_index_sequence(_Fp&&, index_sequence<>)
  { }

template <size_t _Np, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __execute_n_times(_Fp&& __f)
  {
    __execute_on_index_sequence(static_cast<_Fp&&>(__f),
				make_index_sequence<_Np>{});
  }

// }}}
// __generate_from_n_evaluations{{{
template <typename _R, typename _Fp, size_t... _I>
  [[__gnu__::__flatten__]] _GLIBCXX_SIMD_INTRINSIC constexpr
  _R
  __execute_on_index_sequence_with_return(_Fp&& __f, index_sequence<_I...>)
  { return _R{__f(_SizeConstant<_I>())...}; }

template <size_t _Np, typename _R, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr _R
  __generate_from_n_evaluations(_Fp&& __f)
  {
    return __execute_on_index_sequence_with_return<_R>(
      static_cast<_Fp&&>(__f), make_index_sequence<_Np>{});
  }

// }}}
// __call_with_n_evaluations{{{
template <size_t... _I, typename _F0, typename _FArgs>
  [[__gnu__::__flatten__]] _GLIBCXX_SIMD_INTRINSIC constexpr
  auto
  __call_with_n_evaluations(index_sequence<_I...>, _F0&& __f0, _FArgs&& __fargs)
  { return __f0(__fargs(_SizeConstant<_I>())...); }

template <size_t _Np, typename _F0, typename _FArgs>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __call_with_n_evaluations(_F0&& __f0, _FArgs&& __fargs)
  {
    return __call_with_n_evaluations(make_index_sequence<_Np>{},
				     static_cast<_F0&&>(__f0),
				     static_cast<_FArgs&&>(__fargs));
  }

// }}}
// __call_with_subscripts{{{
template <size_t _First = 0, size_t... _It, typename _Tp, typename _Fp>
  [[__gnu__::__flatten__]] _GLIBCXX_SIMD_INTRINSIC constexpr
  auto
  __call_with_subscripts(_Tp&& __x, index_sequence<_It...>, _Fp&& __fun)
  { return __fun(__x[_First + _It]...); }

template <size_t _Np, size_t _First = 0, typename _Tp, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __call_with_subscripts(_Tp&& __x, _Fp&& __fun)
  {
    return __call_with_subscripts<_First>(static_cast<_Tp&&>(__x),
					  make_index_sequence<_Np>(),
					  static_cast<_Fp&&>(__fun));
  }

// }}}

// vvv ---- type traits ---- vvv
// integer type aliases{{{
using _UChar = unsigned char;
using _SChar = signed char;
using _UShort = unsigned short;
using _UInt = unsigned int;
using _ULong = unsigned long;
using _ULLong = unsigned long long;
using _LLong = long long;

//}}}
// __first_of_pack{{{
template <typename _T0, typename...>
  struct __first_of_pack
  { using type = _T0; };

template <typename... _Ts>
  using __first_of_pack_t = typename __first_of_pack<_Ts...>::type;

//}}}
// __value_type_or_identity_t {{{
template <typename _Tp>
  typename _Tp::value_type
  __value_type_or_identity_impl(int);

template <typename _Tp>
  _Tp
  __value_type_or_identity_impl(float);

template <typename _Tp>
  using __value_type_or_identity_t
    = decltype(__value_type_or_identity_impl<_Tp>(int()));

// }}}
// __is_vectorizable {{{
template <typename _Tp>
  struct __is_vectorizable : public is_arithmetic<_Tp> {};

template <>
  struct __is_vectorizable<bool> : public false_type {};

template <typename _Tp>
  inline constexpr bool __is_vectorizable_v = __is_vectorizable<_Tp>::value;

// Deduces to a vectorizable type
template <typename _Tp, typename = enable_if_t<__is_vectorizable_v<_Tp>>>
  using _Vectorizable = _Tp;

// }}}
// _LoadStorePtr / __is_possible_loadstore_conversion {{{
template <typename _Ptr, typename _ValueType>
  struct __is_possible_loadstore_conversion
  : conjunction<__is_vectorizable<_Ptr>, __is_vectorizable<_ValueType>> {};

template <>
  struct __is_possible_loadstore_conversion<bool, bool> : true_type {};

// Deduces to a type allowed for load/store with the given value type.
template <typename _Ptr, typename _ValueType,
	  typename = enable_if_t<
	    __is_possible_loadstore_conversion<_Ptr, _ValueType>::value>>
  using _LoadStorePtr = _Ptr;

// }}}
// __is_bitmask{{{
template <typename _Tp, typename = void_t<>>
  struct __is_bitmask : false_type {};

template <typename _Tp>
  inline constexpr bool __is_bitmask_v = __is_bitmask<_Tp>::value;

// the __mmaskXX case:
template <typename _Tp>
  struct __is_bitmask<_Tp,
    void_t<decltype(declval<unsigned&>() = declval<_Tp>() & 1u)>>
  : true_type {};

// }}}
// __int_for_sizeof{{{
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wpedantic"
template <size_t _Bytes>
  constexpr auto
  __int_for_sizeof()
  {
    if constexpr (_Bytes == sizeof(int))
      return int();
  #ifdef __clang__
    else if constexpr (_Bytes == sizeof(char))
      return char();
  #else
    else if constexpr (_Bytes == sizeof(_SChar))
      return _SChar();
  #endif
    else if constexpr (_Bytes == sizeof(short))
      return short();
  #ifndef __clang__
    else if constexpr (_Bytes == sizeof(long))
      return long();
  #endif
    else if constexpr (_Bytes == sizeof(_LLong))
      return _LLong();
  #ifdef __SIZEOF_INT128__
    else if constexpr (_Bytes == sizeof(__int128))
      return __int128();
  #endif // __SIZEOF_INT128__
    else if constexpr (_Bytes % sizeof(int) == 0)
      {
	constexpr size_t _Np = _Bytes / sizeof(int);
	struct _Ip
	{
	  int _M_data[_Np];

	  _GLIBCXX_SIMD_INTRINSIC constexpr _Ip
	  operator&(_Ip __rhs) const
	  {
	    return __generate_from_n_evaluations<_Np, _Ip>(
	      [&](auto __i) { return __rhs._M_data[__i] & _M_data[__i]; });
	  }

	  _GLIBCXX_SIMD_INTRINSIC constexpr _Ip
	  operator|(_Ip __rhs) const
	  {
	    return __generate_from_n_evaluations<_Np, _Ip>(
	      [&](auto __i) { return __rhs._M_data[__i] | _M_data[__i]; });
	  }

	  _GLIBCXX_SIMD_INTRINSIC constexpr _Ip
	  operator^(_Ip __rhs) const
	  {
	    return __generate_from_n_evaluations<_Np, _Ip>(
	      [&](auto __i) { return __rhs._M_data[__i] ^ _M_data[__i]; });
	  }

	  _GLIBCXX_SIMD_INTRINSIC constexpr _Ip
	  operator~() const
	  {
	    return __generate_from_n_evaluations<_Np, _Ip>(
	      [&](auto __i) { return ~_M_data[__i]; });
	  }
	};
	return _Ip{};
      }
    else
      static_assert(_Bytes != _Bytes, "this should be unreachable");
  }
#pragma GCC diagnostic pop

template <typename _Tp>
  using __int_for_sizeof_t = decltype(__int_for_sizeof<sizeof(_Tp)>());

template <size_t _Np>
  using __int_with_sizeof_t = decltype(__int_for_sizeof<_Np>());

// }}}
// __is_fixed_size_abi{{{
template <typename _Tp>
  struct __is_fixed_size_abi : false_type {};

template <int _Np>
  struct __is_fixed_size_abi<simd_abi::fixed_size<_Np>> : true_type {};

template <typename _Tp>
  inline constexpr bool __is_fixed_size_abi_v = __is_fixed_size_abi<_Tp>::value;

// }}}
// __is_scalar_abi {{{
template <typename _Abi>
  constexpr bool
  __is_scalar_abi()
  { return is_same_v<simd_abi::scalar, _Abi>; }

// }}}
// __abi_bytes_v {{{
template <template <int> class _Abi, int _Bytes>
  constexpr int
  __abi_bytes_impl(_Abi<_Bytes>*)
  { return _Bytes; }

template <typename _Tp>
  constexpr int
  __abi_bytes_impl(_Tp*)
  { return -1; }

template <typename _Abi>
  inline constexpr int __abi_bytes_v
    = __abi_bytes_impl(static_cast<_Abi*>(nullptr));

// }}}
// __is_builtin_bitmask_abi {{{
template <typename _Abi>
  constexpr bool
  __is_builtin_bitmask_abi()
  { return is_same_v<simd_abi::_VecBltnBtmsk<__abi_bytes_v<_Abi>>, _Abi>; }

// }}}
// __is_sse_abi {{{
template <typename _Abi>
  constexpr bool
  __is_sse_abi()
  {
    constexpr auto _Bytes = __abi_bytes_v<_Abi>;
    return _Bytes <= 16 && is_same_v<simd_abi::_VecBuiltin<_Bytes>, _Abi>;
  }

// }}}
// __is_avx_abi {{{
template <typename _Abi>
  constexpr bool
  __is_avx_abi()
  {
    constexpr auto _Bytes = __abi_bytes_v<_Abi>;
    return _Bytes > 16 && _Bytes <= 32
	   && is_same_v<simd_abi::_VecBuiltin<_Bytes>, _Abi>;
  }

// }}}
// __is_avx512_abi {{{
template <typename _Abi>
  constexpr bool
  __is_avx512_abi()
  {
    constexpr auto _Bytes = __abi_bytes_v<_Abi>;
    return _Bytes <= 64 && is_same_v<simd_abi::_Avx512<_Bytes>, _Abi>;
  }

// }}}
// __is_neon_abi {{{
template <typename _Abi>
  constexpr bool
  __is_neon_abi()
  {
    constexpr auto _Bytes = __abi_bytes_v<_Abi>;
    return _Bytes <= 16 && is_same_v<simd_abi::_VecBuiltin<_Bytes>, _Abi>;
  }

// }}}
// __make_dependent_t {{{
template <typename, typename _Up>
  struct __make_dependent
  { using type = _Up; };

template <typename _Tp, typename _Up>
  using __make_dependent_t = typename __make_dependent<_Tp, _Up>::type;

// }}}
// ^^^ ---- type traits ---- ^^^

// __invoke_ub{{{
template <typename... _Args>
  [[noreturn]] _GLIBCXX_SIMD_ALWAYS_INLINE void
  __invoke_ub([[maybe_unused]] const char* __msg,
	      [[maybe_unused]] const _Args&... __args)
  {
#ifdef _GLIBCXX_DEBUG_UB
    __builtin_fprintf(stderr, __msg, __args...);
    __builtin_trap();
#else
    __builtin_unreachable();
#endif
  }

// }}}
// __assert_unreachable{{{
template <typename _Tp>
  struct __assert_unreachable
  { static_assert(!is_same_v<_Tp, _Tp>, "this should be unreachable"); };

// }}}
// __size_or_zero_v {{{
template <typename _Tp, typename _Ap, size_t _Np = simd_size<_Tp, _Ap>::value>
  constexpr size_t
  __size_or_zero_dispatch(int)
  { return _Np; }

template <typename _Tp, typename _Ap>
  constexpr size_t
  __size_or_zero_dispatch(float)
  { return 0; }

template <typename _Tp, typename _Ap>
  inline constexpr size_t __size_or_zero_v
     = __size_or_zero_dispatch<_Tp, _Ap>(0);

// }}}
// __div_roundup {{{
inline constexpr size_t
__div_roundup(size_t __a, size_t __b)
{ return (__a + __b - 1) / __b; }

// }}}
// _ExactBool{{{
class _ExactBool
{
  const bool _M_data;

public:
  _GLIBCXX_SIMD_INTRINSIC constexpr _ExactBool(bool __b) : _M_data(__b) {}

  _ExactBool(int) = delete;

  _GLIBCXX_SIMD_INTRINSIC constexpr operator bool() const { return _M_data; }
};

// }}}
// __may_alias{{{
/**@internal
 * Helper __may_alias<_Tp> that turns _Tp into the type to be used for an
 * aliasing pointer. This adds the __may_alias attribute to _Tp (with compilers
 * that support it).
 */
template <typename _Tp>
  using __may_alias [[__gnu__::__may_alias__]] = _Tp;

// }}}
// _UnsupportedBase {{{
// simd and simd_mask base for unsupported <_Tp, _Abi>
struct _UnsupportedBase
{
  _UnsupportedBase() = delete;
  _UnsupportedBase(const _UnsupportedBase&) = delete;
  _UnsupportedBase& operator=(const _UnsupportedBase&) = delete;
  ~_UnsupportedBase() = delete;
};

// }}}
// _InvalidTraits {{{
/**
 * @internal
 * Defines the implementation of __a given <_Tp, _Abi>.
 *
 * Implementations must ensure that only valid <_Tp, _Abi> instantiations are
 * possible. Static assertions in the type definition do not suffice. It is
 * important that SFINAE works.
 */
struct _InvalidTraits
{
  using _IsValid = false_type;
  using _SimdBase = _UnsupportedBase;
  using _MaskBase = _UnsupportedBase;

  static constexpr size_t _S_full_size = 0;
  static constexpr bool _S_is_partial = false;

  static constexpr size_t _S_simd_align = 1;
  struct _SimdImpl;
  struct _SimdMember {};
  struct _SimdCastType;

  static constexpr size_t _S_mask_align = 1;
  struct _MaskImpl;
  struct _MaskMember {};
  struct _MaskCastType;
};

// }}}
// _SimdTraits {{{
template <typename _Tp, typename _Abi, typename = void_t<>>
  struct _SimdTraits : _InvalidTraits {};

// }}}
// __private_init, __bitset_init{{{
/**
 * @internal
 * Tag used for private init constructor of simd and simd_mask
 */
inline constexpr struct _PrivateInit {} __private_init = {};

inline constexpr struct _BitsetInit {} __bitset_init = {};

// }}}
// __is_narrowing_conversion<_From, _To>{{{
template <typename _From, typename _To, bool = is_arithmetic_v<_From>,
	  bool = is_arithmetic_v<_To>>
  struct __is_narrowing_conversion;

// ignore "signed/unsigned mismatch" in the following trait.
// The implicit conversions will do the right thing here.
template <typename _From, typename _To>
  struct __is_narrowing_conversion<_From, _To, true, true>
  : public __bool_constant<(
      __digits_v<_From> > __digits_v<_To>
      || __finite_max_v<_From> > __finite_max_v<_To>
      || __finite_min_v<_From> < __finite_min_v<_To>
      || (is_signed_v<_From> && is_unsigned_v<_To>))> {};

template <typename _Tp>
  struct __is_narrowing_conversion<_Tp, bool, true, true>
  : public true_type {};

template <>
  struct __is_narrowing_conversion<bool, bool, true, true>
  : public false_type {};

template <typename _Tp>
  struct __is_narrowing_conversion<_Tp, _Tp, true, true>
  : public false_type {};

template <typename _From, typename _To>
  struct __is_narrowing_conversion<_From, _To, false, true>
  : public negation<is_convertible<_From, _To>> {};

// }}}
// __converts_to_higher_integer_rank{{{
template <typename _From, typename _To, bool = (sizeof(_From) < sizeof(_To))>
  struct __converts_to_higher_integer_rank : public true_type {};

// this may fail for char -> short if sizeof(char) == sizeof(short)
template <typename _From, typename _To>
  struct __converts_to_higher_integer_rank<_From, _To, false>
  : public is_same<decltype(declval<_From>() + declval<_To>()), _To> {};

// }}}
// __data(simd/simd_mask) {{{
template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC constexpr const auto&
  __data(const simd<_Tp, _Ap>& __x);

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto&
  __data(simd<_Tp, _Ap>& __x);

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC constexpr const auto&
  __data(const simd_mask<_Tp, _Ap>& __x);

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto&
  __data(simd_mask<_Tp, _Ap>& __x);

// }}}
// _SimdConverter {{{
template <typename _FromT, typename _FromA, typename _ToT, typename _ToA,
	  typename = void>
  struct _SimdConverter;

template <typename _Tp, typename _Ap>
  struct _SimdConverter<_Tp, _Ap, _Tp, _Ap, void>
  {
    template <typename _Up>
      _GLIBCXX_SIMD_INTRINSIC const _Up&
      operator()(const _Up& __x)
      { return __x; }
  };

// }}}
// __to_value_type_or_member_type {{{
template <typename _V>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __to_value_type_or_member_type(const _V& __x) -> decltype(__data(__x))
  { return __data(__x); }

template <typename _V>
  _GLIBCXX_SIMD_INTRINSIC constexpr const typename _V::value_type&
  __to_value_type_or_member_type(const typename _V::value_type& __x)
  { return __x; }

// }}}
// __bool_storage_member_type{{{
template <size_t _Size>
  struct __bool_storage_member_type;

template <size_t _Size>
  using __bool_storage_member_type_t =
    typename __bool_storage_member_type<_Size>::type;

// }}}
// _SimdTuple {{{
// why not tuple?
// 1. tuple gives no guarantee about the storage order, but I require
// storage
//    equivalent to array<_Tp, _Np>
// 2. direct access to the element type (first template argument)
// 3. enforces equal element type, only different _Abi types are allowed
template <typename _Tp, typename... _Abis>
  struct _SimdTuple;

//}}}
// __fixed_size_storage_t {{{
template <typename _Tp, int _Np>
  struct __fixed_size_storage;

template <typename _Tp, int _Np>
  using __fixed_size_storage_t = typename __fixed_size_storage<_Tp, _Np>::type;

// }}}
// _SimdWrapper fwd decl{{{
template <typename _Tp, size_t _Size, typename = void_t<>>
  struct _SimdWrapper;

template <typename _Tp>
  using _SimdWrapper8 = _SimdWrapper<_Tp, 8 / sizeof(_Tp)>;
template <typename _Tp>
  using _SimdWrapper16 = _SimdWrapper<_Tp, 16 / sizeof(_Tp)>;
template <typename _Tp>
  using _SimdWrapper32 = _SimdWrapper<_Tp, 32 / sizeof(_Tp)>;
template <typename _Tp>
  using _SimdWrapper64 = _SimdWrapper<_Tp, 64 / sizeof(_Tp)>;

// }}}
// __is_simd_wrapper {{{
template <typename _Tp>
  struct __is_simd_wrapper : false_type {};

template <typename _Tp, size_t _Np>
  struct __is_simd_wrapper<_SimdWrapper<_Tp, _Np>> : true_type {};

template <typename _Tp>
  inline constexpr bool __is_simd_wrapper_v = __is_simd_wrapper<_Tp>::value;

// }}}
// _BitOps {{{
struct _BitOps
{
  // _S_bit_iteration {{{
  template <typename _Tp, typename _Fp>
    static void
    _S_bit_iteration(_Tp __mask, _Fp&& __f)
    {
      static_assert(sizeof(_ULLong) >= sizeof(_Tp));
      conditional_t<sizeof(_Tp) <= sizeof(_UInt), _UInt, _ULLong> __k;
      if constexpr (is_convertible_v<_Tp, decltype(__k)>)
	__k = __mask;
      else
	__k = __mask.to_ullong();
      while(__k)
	{
	  __f(std::__countr_zero(__k));
	  __k &= (__k - 1);
	}
    }

  //}}}
};

//}}}
// __increment, __decrement {{{
template <typename _Tp = void>
  struct __increment
  { constexpr _Tp operator()(_Tp __a) const { return ++__a; } };

template <>
  struct __increment<void>
  {
    template <typename _Tp>
      constexpr _Tp
      operator()(_Tp __a) const
      { return ++__a; }
  };

template <typename _Tp = void>
  struct __decrement
  { constexpr _Tp operator()(_Tp __a) const { return --__a; } };

template <>
  struct __decrement<void>
  {
    template <typename _Tp>
      constexpr _Tp
      operator()(_Tp __a) const
      { return --__a; }
  };

// }}}
// _ValuePreserving(OrInt) {{{
template <typename _From, typename _To,
	  typename = enable_if_t<negation<
	    __is_narrowing_conversion<__remove_cvref_t<_From>, _To>>::value>>
  using _ValuePreserving = _From;

template <typename _From, typename _To,
	  typename _DecayedFrom = __remove_cvref_t<_From>,
	  typename = enable_if_t<conjunction<
	    is_convertible<_From, _To>,
	    disjunction<
	      is_same<_DecayedFrom, _To>, is_same<_DecayedFrom, int>,
	      conjunction<is_same<_DecayedFrom, _UInt>, is_unsigned<_To>>,
	      negation<__is_narrowing_conversion<_DecayedFrom, _To>>>>::value>>
  using _ValuePreservingOrInt = _From;

// }}}
// __intrinsic_type {{{
template <typename _Tp, size_t _Bytes, typename = void_t<>>
  struct __intrinsic_type;

template <typename _Tp, size_t _Size>
  using __intrinsic_type_t =
    typename __intrinsic_type<_Tp, _Size * sizeof(_Tp)>::type;

template <typename _Tp>
  using __intrinsic_type2_t = typename __intrinsic_type<_Tp, 2>::type;
template <typename _Tp>
  using __intrinsic_type4_t = typename __intrinsic_type<_Tp, 4>::type;
template <typename _Tp>
  using __intrinsic_type8_t = typename __intrinsic_type<_Tp, 8>::type;
template <typename _Tp>
  using __intrinsic_type16_t = typename __intrinsic_type<_Tp, 16>::type;
template <typename _Tp>
  using __intrinsic_type32_t = typename __intrinsic_type<_Tp, 32>::type;
template <typename _Tp>
  using __intrinsic_type64_t = typename __intrinsic_type<_Tp, 64>::type;

// }}}
// _BitMask {{{
template <size_t _Np, bool _Sanitized = false>
  struct _BitMask;

template <size_t _Np, bool _Sanitized>
  struct __is_bitmask<_BitMask<_Np, _Sanitized>, void> : true_type {};

template <size_t _Np>
  using _SanitizedBitMask = _BitMask<_Np, true>;

template <size_t _Np, bool _Sanitized>
  struct _BitMask
  {
    static_assert(_Np > 0);

    static constexpr size_t _NBytes = __div_roundup(_Np, __CHAR_BIT__);

    using _Tp = conditional_t<_Np == 1, bool,
			      make_unsigned_t<__int_with_sizeof_t<std::min(
				sizeof(_ULLong), std::__bit_ceil(_NBytes))>>>;

    static constexpr int _S_array_size = __div_roundup(_NBytes, sizeof(_Tp));

    _Tp _M_bits[_S_array_size];

    static constexpr int _S_unused_bits
      = _Np == 1 ? 0 : _S_array_size * sizeof(_Tp) * __CHAR_BIT__ - _Np;

    static constexpr _Tp _S_bitmask = +_Tp(~_Tp()) >> _S_unused_bits;

    constexpr _BitMask() noexcept = default;

    constexpr _BitMask(unsigned long long __x) noexcept
      : _M_bits{static_cast<_Tp>(__x)} {}

    _BitMask(bitset<_Np> __x) noexcept : _BitMask(__x.to_ullong()) {}

    constexpr _BitMask(const _BitMask&) noexcept = default;

    template <bool _RhsSanitized, typename = enable_if_t<_RhsSanitized == false
							 && _Sanitized == true>>
      constexpr _BitMask(const _BitMask<_Np, _RhsSanitized>& __rhs) noexcept
	: _BitMask(__rhs._M_sanitized()) {}

    constexpr operator _SimdWrapper<bool, _Np>() const noexcept
    {
      static_assert(_S_array_size == 1);
      return _M_bits[0];
    }

    // precondition: is sanitized
    constexpr _Tp
    _M_to_bits() const noexcept
    {
      static_assert(_S_array_size == 1);
      return _M_bits[0];
    }

    // precondition: is sanitized
    constexpr unsigned long long
    to_ullong() const noexcept
    {
      static_assert(_S_array_size == 1);
      return _M_bits[0];
    }

    // precondition: is sanitized
    constexpr unsigned long
    to_ulong() const noexcept
    {
      static_assert(_S_array_size == 1);
      return _M_bits[0];
    }

    constexpr bitset<_Np>
    _M_to_bitset() const noexcept
    {
      static_assert(_S_array_size == 1);
      return _M_bits[0];
    }

    constexpr decltype(auto)
    _M_sanitized() const noexcept
    {
      if constexpr (_Sanitized)
	return *this;
      else if constexpr (_Np == 1)
	return _SanitizedBitMask<_Np>(_M_bits[0]);
      else
	{
	  _SanitizedBitMask<_Np> __r = {};
	  for (int __i = 0; __i < _S_array_size; ++__i)
	    __r._M_bits[__i] = _M_bits[__i];
	  if constexpr (_S_unused_bits > 0)
	    __r._M_bits[_S_array_size - 1] &= _S_bitmask;
	  return __r;
	}
    }

    template <size_t _Mp, bool _LSanitized>
      constexpr _BitMask<_Np + _Mp, _Sanitized>
      _M_prepend(_BitMask<_Mp, _LSanitized> __lsb) const noexcept
      {
	constexpr size_t _RN = _Np + _Mp;
	using _Rp = _BitMask<_RN, _Sanitized>;
	if constexpr (_Rp::_S_array_size == 1)
	  {
	    _Rp __r{{_M_bits[0]}};
	    __r._M_bits[0] <<= _Mp;
	    __r._M_bits[0] |= __lsb._M_sanitized()._M_bits[0];
	    return __r;
	  }
	else
	  __assert_unreachable<_Rp>();
      }

    // Return a new _BitMask with size _NewSize while dropping _DropLsb least
    // significant bits. If the operation implicitly produces a sanitized bitmask,
    // the result type will have _Sanitized set.
    template <size_t _DropLsb, size_t _NewSize = _Np - _DropLsb>
      constexpr auto
      _M_extract() const noexcept
      {
	static_assert(_Np > _DropLsb);
	static_assert(_DropLsb + _NewSize <= sizeof(_ULLong) * __CHAR_BIT__,
		      "not implemented for bitmasks larger than one ullong");
	if constexpr (_NewSize == 1)
	  // must sanitize because the return _Tp is bool
	  return _SanitizedBitMask<1>(_M_bits[0] & (_Tp(1) << _DropLsb));
	else
	  return _BitMask<_NewSize,
			  ((_NewSize + _DropLsb == sizeof(_Tp) * __CHAR_BIT__
			    && _NewSize + _DropLsb <= _Np)
			   || ((_Sanitized || _Np == sizeof(_Tp) * __CHAR_BIT__)
			       && _NewSize + _DropLsb >= _Np))>(_M_bits[0]
								>> _DropLsb);
      }

    // True if all bits are set. Implicitly sanitizes if _Sanitized == false.
    constexpr bool
    all() const noexcept
    {
      if constexpr (_Np == 1)
	return _M_bits[0];
      else if constexpr (!_Sanitized)
	return _M_sanitized().all();
      else
	{
	  constexpr _Tp __allbits = ~_Tp();
	  for (int __i = 0; __i < _S_array_size - 1; ++__i)
	    if (_M_bits[__i] != __allbits)
	      return false;
	  return _M_bits[_S_array_size - 1] == _S_bitmask;
	}
    }

    // True if at least one bit is set. Implicitly sanitizes if _Sanitized ==
    // false.
    constexpr bool
    any() const noexcept
    {
      if constexpr (_Np == 1)
	return _M_bits[0];
      else if constexpr (!_Sanitized)
	return _M_sanitized().any();
      else
	{
	  for (int __i = 0; __i < _S_array_size - 1; ++__i)
	    if (_M_bits[__i] != 0)
	      return true;
	  return _M_bits[_S_array_size - 1] != 0;
	}
    }

    // True if no bit is set. Implicitly sanitizes if _Sanitized == false.
    constexpr bool
    none() const noexcept
    {
      if constexpr (_Np == 1)
	return !_M_bits[0];
      else if constexpr (!_Sanitized)
	return _M_sanitized().none();
      else
	{
	  for (int __i = 0; __i < _S_array_size - 1; ++__i)
	    if (_M_bits[__i] != 0)
	      return false;
	  return _M_bits[_S_array_size - 1] == 0;
	}
    }

    // Returns the number of set bits. Implicitly sanitizes if _Sanitized ==
    // false.
    constexpr int
    count() const noexcept
    {
      if constexpr (_Np == 1)
	return _M_bits[0];
      else if constexpr (!_Sanitized)
	return _M_sanitized().none();
      else
	{
	  int __result = __builtin_popcountll(_M_bits[0]);
	  for (int __i = 1; __i < _S_array_size; ++__i)
	    __result += __builtin_popcountll(_M_bits[__i]);
	  return __result;
	}
    }

    // Returns the bit at offset __i as bool.
    constexpr bool
    operator[](size_t __i) const noexcept
    {
      if constexpr (_Np == 1)
	return _M_bits[0];
      else if constexpr (_S_array_size == 1)
	return (_M_bits[0] >> __i) & 1;
      else
	{
	  const size_t __j = __i / (sizeof(_Tp) * __CHAR_BIT__);
	  const size_t __shift = __i % (sizeof(_Tp) * __CHAR_BIT__);
	  return (_M_bits[__j] >> __shift) & 1;
	}
    }

    template <size_t __i>
      constexpr bool
      operator[](_SizeConstant<__i>) const noexcept
      {
	static_assert(__i < _Np);
	constexpr size_t __j = __i / (sizeof(_Tp) * __CHAR_BIT__);
	constexpr size_t __shift = __i % (sizeof(_Tp) * __CHAR_BIT__);
	return static_cast<bool>(_M_bits[__j] & (_Tp(1) << __shift));
      }

    // Set the bit at offset __i to __x.
    constexpr void
    set(size_t __i, bool __x) noexcept
    {
      if constexpr (_Np == 1)
	_M_bits[0] = __x;
      else if constexpr (_S_array_size == 1)
	{
	  _M_bits[0] &= ~_Tp(_Tp(1) << __i);
	  _M_bits[0] |= _Tp(_Tp(__x) << __i);
	}
      else
	{
	  const size_t __j = __i / (sizeof(_Tp) * __CHAR_BIT__);
	  const size_t __shift = __i % (sizeof(_Tp) * __CHAR_BIT__);
	  _M_bits[__j] &= ~_Tp(_Tp(1) << __shift);
	  _M_bits[__j] |= _Tp(_Tp(__x) << __shift);
	}
    }

    template <size_t __i>
      constexpr void
      set(_SizeConstant<__i>, bool __x) noexcept
      {
	static_assert(__i < _Np);
	if constexpr (_Np == 1)
	  _M_bits[0] = __x;
	else
	  {
	    constexpr size_t __j = __i / (sizeof(_Tp) * __CHAR_BIT__);
	    constexpr size_t __shift = __i % (sizeof(_Tp) * __CHAR_BIT__);
	    constexpr _Tp __mask = ~_Tp(_Tp(1) << __shift);
	    _M_bits[__j] &= __mask;
	    _M_bits[__j] |= _Tp(_Tp(__x) << __shift);
	  }
      }

    // Inverts all bits. Sanitized input leads to sanitized output.
    constexpr _BitMask
    operator~() const noexcept
    {
      if constexpr (_Np == 1)
	return !_M_bits[0];
      else
	{
	  _BitMask __result{};
	  for (int __i = 0; __i < _S_array_size - 1; ++__i)
	    __result._M_bits[__i] = ~_M_bits[__i];
	  if constexpr (_Sanitized)
	    __result._M_bits[_S_array_size - 1]
	      = _M_bits[_S_array_size - 1] ^ _S_bitmask;
	  else
	    __result._M_bits[_S_array_size - 1] = ~_M_bits[_S_array_size - 1];
	  return __result;
	}
    }

    constexpr _BitMask&
    operator^=(const _BitMask& __b) & noexcept
    {
      __execute_n_times<_S_array_size>(
	[&](auto __i) { _M_bits[__i] ^= __b._M_bits[__i]; });
      return *this;
    }

    constexpr _BitMask&
    operator|=(const _BitMask& __b) & noexcept
    {
      __execute_n_times<_S_array_size>(
	[&](auto __i) { _M_bits[__i] |= __b._M_bits[__i]; });
      return *this;
    }

    constexpr _BitMask&
    operator&=(const _BitMask& __b) & noexcept
    {
      __execute_n_times<_S_array_size>(
	[&](auto __i) { _M_bits[__i] &= __b._M_bits[__i]; });
      return *this;
    }

    friend constexpr _BitMask
    operator^(const _BitMask& __a, const _BitMask& __b) noexcept
    {
      _BitMask __r = __a;
      __r ^= __b;
      return __r;
    }

    friend constexpr _BitMask
    operator|(const _BitMask& __a, const _BitMask& __b) noexcept
    {
      _BitMask __r = __a;
      __r |= __b;
      return __r;
    }

    friend constexpr _BitMask
    operator&(const _BitMask& __a, const _BitMask& __b) noexcept
    {
      _BitMask __r = __a;
      __r &= __b;
      return __r;
    }

    _GLIBCXX_SIMD_INTRINSIC
    constexpr bool
    _M_is_constprop() const
    {
      if constexpr (_S_array_size == 0)
	return __builtin_constant_p(_M_bits[0]);
      else
	{
	  for (int __i = 0; __i < _S_array_size; ++__i)
	    if (!__builtin_constant_p(_M_bits[__i]))
	      return false;
	  return true;
	}
    }
  };

// }}}

// vvv ---- builtin vector types [[gnu::vector_size(N)]] and operations ---- vvv
// __min_vector_size {{{
template <typename _Tp = void>
  static inline constexpr int __min_vector_size = 2 * sizeof(_Tp);

#if _GLIBCXX_SIMD_HAVE_NEON
template <>
  inline constexpr int __min_vector_size<void> = 8;
#else
template <>
  inline constexpr int __min_vector_size<void> = 16;
#endif

// }}}
// __vector_type {{{
template <typename _Tp, size_t _Np, typename = void>
  struct __vector_type_n {};

// substition failure for 0-element case
template <typename _Tp>
  struct __vector_type_n<_Tp, 0, void> {};

// special case 1-element to be _Tp itself
template <typename _Tp>
  struct __vector_type_n<_Tp, 1, enable_if_t<__is_vectorizable_v<_Tp>>>
  { using type = _Tp; };

// else, use GNU-style builtin vector types
template <typename _Tp, size_t _Np>
  struct __vector_type_n<_Tp, _Np,
			 enable_if_t<__is_vectorizable_v<_Tp> && _Np >= 2>>
  {
    static constexpr size_t _S_Np2 = std::__bit_ceil(_Np * sizeof(_Tp));

    static constexpr size_t _S_Bytes =
#ifdef __i386__
      // Using [[gnu::vector_size(8)]] would wreak havoc on the FPU because
      // those objects are passed via MMX registers and nothing ever calls EMMS.
      _S_Np2 == 8 ? 16 :
#endif
      _S_Np2 < __min_vector_size<_Tp> ? __min_vector_size<_Tp>
				      : _S_Np2;

    using type [[__gnu__::__vector_size__(_S_Bytes)]] = _Tp;
  };

template <typename _Tp, size_t _Bytes, size_t = _Bytes % sizeof(_Tp)>
  struct __vector_type;

template <typename _Tp, size_t _Bytes>
  struct __vector_type<_Tp, _Bytes, 0>
  : __vector_type_n<_Tp, _Bytes / sizeof(_Tp)> {};

template <typename _Tp, size_t _Size>
  using __vector_type_t = typename __vector_type_n<_Tp, _Size>::type;

template <typename _Tp>
  using __vector_type2_t = typename __vector_type<_Tp, 2>::type;
template <typename _Tp>
  using __vector_type4_t = typename __vector_type<_Tp, 4>::type;
template <typename _Tp>
  using __vector_type8_t = typename __vector_type<_Tp, 8>::type;
template <typename _Tp>
  using __vector_type16_t = typename __vector_type<_Tp, 16>::type;
template <typename _Tp>
  using __vector_type32_t = typename __vector_type<_Tp, 32>::type;
template <typename _Tp>
  using __vector_type64_t = typename __vector_type<_Tp, 64>::type;

// }}}
// __is_vector_type {{{
template <typename _Tp, typename = void_t<>>
  struct __is_vector_type : false_type {};

template <typename _Tp>
  struct __is_vector_type<
    _Tp, void_t<typename __vector_type<
	   remove_reference_t<decltype(declval<_Tp>()[0])>, sizeof(_Tp)>::type>>
    : is_same<_Tp, typename __vector_type<
		     remove_reference_t<decltype(declval<_Tp>()[0])>,
		     sizeof(_Tp)>::type> {};

template <typename _Tp>
  inline constexpr bool __is_vector_type_v = __is_vector_type<_Tp>::value;

// }}}
// __is_intrinsic_type {{{
#if _GLIBCXX_SIMD_HAVE_SSE_ABI
template <typename _Tp>
  using __is_intrinsic_type = __is_vector_type<_Tp>;
#else // not SSE (x86)
template <typename _Tp, typename = void_t<>>
  struct __is_intrinsic_type : false_type {};

template <typename _Tp>
  struct __is_intrinsic_type<
    _Tp, void_t<typename __intrinsic_type<
	   remove_reference_t<decltype(declval<_Tp>()[0])>, sizeof(_Tp)>::type>>
    : is_same<_Tp, typename __intrinsic_type<
		     remove_reference_t<decltype(declval<_Tp>()[0])>,
		     sizeof(_Tp)>::type> {};
#endif

template <typename _Tp>
  inline constexpr bool __is_intrinsic_type_v = __is_intrinsic_type<_Tp>::value;

// }}}
// _VectorTraits{{{
template <typename _Tp, typename = void_t<>>
  struct _VectorTraitsImpl;

template <typename _Tp>
  struct _VectorTraitsImpl<_Tp, enable_if_t<__is_vector_type_v<_Tp>
					      || __is_intrinsic_type_v<_Tp>>>
  {
    using type = _Tp;
    using value_type = remove_reference_t<decltype(declval<_Tp>()[0])>;
    static constexpr int _S_full_size = sizeof(_Tp) / sizeof(value_type);
    using _Wrapper = _SimdWrapper<value_type, _S_full_size>;
    template <typename _Up, int _W = _S_full_size>
      static constexpr bool _S_is
	= is_same_v<value_type, _Up> && _W == _S_full_size;
  };

template <typename _Tp, size_t _Np>
  struct _VectorTraitsImpl<_SimdWrapper<_Tp, _Np>,
			   void_t<__vector_type_t<_Tp, _Np>>>
  {
    using type = __vector_type_t<_Tp, _Np>;
    using value_type = _Tp;
    static constexpr int _S_full_size = sizeof(type) / sizeof(value_type);
    using _Wrapper = _SimdWrapper<_Tp, _Np>;
    static constexpr bool _S_is_partial = (_Np == _S_full_size);
    static constexpr int _S_partial_width = _Np;
    template <typename _Up, int _W = _S_full_size>
      static constexpr bool _S_is
	= is_same_v<value_type, _Up>&& _W == _S_full_size;
  };

template <typename _Tp, typename = typename _VectorTraitsImpl<_Tp>::type>
  using _VectorTraits = _VectorTraitsImpl<_Tp>;

// }}}
// __as_vector{{{
template <typename _V>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __as_vector(_V __x)
  {
    if constexpr (__is_vector_type_v<_V>)
      return __x;
    else if constexpr (is_simd<_V>::value || is_simd_mask<_V>::value)
      return __data(__x)._M_data;
    else if constexpr (__is_vectorizable_v<_V>)
      return __vector_type_t<_V, 2>{__x};
    else
      return __x._M_data;
  }

// }}}
// __as_wrapper{{{
template <size_t _Np = 0, typename _V>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __as_wrapper(_V __x)
  {
    if constexpr (__is_vector_type_v<_V>)
      return _SimdWrapper<typename _VectorTraits<_V>::value_type,
			  (_Np > 0 ? _Np : _VectorTraits<_V>::_S_full_size)>(__x);
    else if constexpr (is_simd<_V>::value || is_simd_mask<_V>::value)
      {
	static_assert(_V::size() == _Np);
	return __data(__x);
      }
    else
      {
	static_assert(_V::_S_size == _Np);
	return __x;
      }
  }

// }}}
// __intrin_bitcast{{{
template <typename _To, typename _From>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __intrin_bitcast(_From __v)
  {
    static_assert((__is_vector_type_v<_From> || __is_intrinsic_type_v<_From>)
		    && (__is_vector_type_v<_To> || __is_intrinsic_type_v<_To>));
    if constexpr (sizeof(_To) == sizeof(_From))
      return reinterpret_cast<_To>(__v);
    else if constexpr (sizeof(_From) > sizeof(_To))
      if constexpr (sizeof(_To) >= 16)
	return reinterpret_cast<const __may_alias<_To>&>(__v);
      else
	{
	  _To __r;
	  __builtin_memcpy(&__r, &__v, sizeof(_To));
	  return __r;
	}
#if _GLIBCXX_SIMD_X86INTRIN && !defined __clang__
    else if constexpr (__have_avx && sizeof(_From) == 16 && sizeof(_To) == 32)
      return reinterpret_cast<_To>(__builtin_ia32_ps256_ps(
	reinterpret_cast<__vector_type_t<float, 4>>(__v)));
    else if constexpr (__have_avx512f && sizeof(_From) == 16
		       && sizeof(_To) == 64)
      return reinterpret_cast<_To>(__builtin_ia32_ps512_ps(
	reinterpret_cast<__vector_type_t<float, 4>>(__v)));
    else if constexpr (__have_avx512f && sizeof(_From) == 32
		       && sizeof(_To) == 64)
      return reinterpret_cast<_To>(__builtin_ia32_ps512_256ps(
	reinterpret_cast<__vector_type_t<float, 8>>(__v)));
#endif // _GLIBCXX_SIMD_X86INTRIN
    else if constexpr (sizeof(__v) <= 8)
      return reinterpret_cast<_To>(
	__vector_type_t<__int_for_sizeof_t<_From>, sizeof(_To) / sizeof(_From)>{
	  reinterpret_cast<__int_for_sizeof_t<_From>>(__v)});
    else
      {
	static_assert(sizeof(_To) > sizeof(_From));
	_To __r = {};
	__builtin_memcpy(&__r, &__v, sizeof(_From));
	return __r;
      }
  }

// }}}
// __vector_bitcast{{{
template <typename _To, size_t _NN = 0, typename _From,
	  typename _FromVT = _VectorTraits<_From>,
	  size_t _Np = _NN == 0 ? sizeof(_From) / sizeof(_To) : _NN>
  _GLIBCXX_SIMD_INTRINSIC constexpr __vector_type_t<_To, _Np>
  __vector_bitcast(_From __x)
  {
    using _R = __vector_type_t<_To, _Np>;
    return __intrin_bitcast<_R>(__x);
  }

template <typename _To, size_t _NN = 0, typename _Tp, size_t _Nx,
	  size_t _Np
	  = _NN == 0 ? sizeof(_SimdWrapper<_Tp, _Nx>) / sizeof(_To) : _NN>
  _GLIBCXX_SIMD_INTRINSIC constexpr __vector_type_t<_To, _Np>
  __vector_bitcast(const _SimdWrapper<_Tp, _Nx>& __x)
  {
    static_assert(_Np > 1);
    return __intrin_bitcast<__vector_type_t<_To, _Np>>(__x._M_data);
  }

// }}}
// __convert_x86 declarations {{{
#ifdef _GLIBCXX_SIMD_WORKAROUND_PR85048
template <typename _To, typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _To __convert_x86(_Tp);

template <typename _To, typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _To __convert_x86(_Tp, _Tp);

template <typename _To, typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _To __convert_x86(_Tp, _Tp, _Tp, _Tp);

template <typename _To, typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _To __convert_x86(_Tp, _Tp, _Tp, _Tp, _Tp, _Tp, _Tp, _Tp);

template <typename _To, typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _To __convert_x86(_Tp, _Tp, _Tp, _Tp, _Tp, _Tp, _Tp, _Tp, _Tp, _Tp, _Tp, _Tp,
		    _Tp, _Tp, _Tp, _Tp);
#endif // _GLIBCXX_SIMD_WORKAROUND_PR85048

//}}}
// __bit_cast {{{
template <typename _To, typename _From>
  _GLIBCXX_SIMD_INTRINSIC constexpr _To
  __bit_cast(const _From __x)
  {
#if __has_builtin(__builtin_bit_cast)
    return __builtin_bit_cast(_To, __x);
#else
    static_assert(sizeof(_To) == sizeof(_From));
    constexpr bool __to_is_vectorizable
      = is_arithmetic_v<_To> || is_enum_v<_To>;
    constexpr bool __from_is_vectorizable
      = is_arithmetic_v<_From> || is_enum_v<_From>;
    if constexpr (__is_vector_type_v<_To> && __is_vector_type_v<_From>)
      return reinterpret_cast<_To>(__x);
    else if constexpr (__is_vector_type_v<_To> && __from_is_vectorizable)
      {
	using _FV [[gnu::vector_size(sizeof(_From))]] = _From;
	return reinterpret_cast<_To>(_FV{__x});
      }
    else if constexpr (__to_is_vectorizable && __from_is_vectorizable)
      {
	using _TV [[gnu::vector_size(sizeof(_To))]] = _To;
	using _FV [[gnu::vector_size(sizeof(_From))]] = _From;
	return reinterpret_cast<_TV>(_FV{__x})[0];
      }
    else if constexpr (__to_is_vectorizable && __is_vector_type_v<_From>)
      {
	using _TV [[gnu::vector_size(sizeof(_To))]] = _To;
	return reinterpret_cast<_TV>(__x)[0];
      }
    else
      {
	_To __r;
	__builtin_memcpy(reinterpret_cast<char*>(&__r),
			 reinterpret_cast<const char*>(&__x), sizeof(_To));
	return __r;
      }
#endif
  }

// }}}
// __to_intrin {{{
template <typename _Tp, typename _TVT = _VectorTraits<_Tp>,
	  typename _R
	  = __intrinsic_type_t<typename _TVT::value_type, _TVT::_S_full_size>>
  _GLIBCXX_SIMD_INTRINSIC constexpr _R
  __to_intrin(_Tp __x)
  {
    static_assert(sizeof(__x) <= sizeof(_R),
		  "__to_intrin may never drop values off the end");
    if constexpr (sizeof(__x) == sizeof(_R))
      return reinterpret_cast<_R>(__as_vector(__x));
    else
      {
	using _Up = __int_for_sizeof_t<_Tp>;
	return reinterpret_cast<_R>(
	  __vector_type_t<_Up, sizeof(_R) / sizeof(_Up)>{__bit_cast<_Up>(__x)});
      }
  }

// }}}
// __make_vector{{{
template <typename _Tp, typename... _Args>
  _GLIBCXX_SIMD_INTRINSIC constexpr __vector_type_t<_Tp, sizeof...(_Args)>
  __make_vector(const _Args&... __args)
  {
    return __vector_type_t<_Tp, sizeof...(_Args)>{static_cast<_Tp>(__args)...};
  }

// }}}
// __vector_broadcast{{{
template <size_t _Np, typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC constexpr __vector_type_t<_Tp, _Np>
  __vector_broadcast(_Tp __x)
  {
    return __call_with_n_evaluations<_Np>(
      [](auto... __xx) { return __vector_type_t<_Tp, _Np>{__xx...}; },
      [&__x](int) { return __x; });
  }

// }}}
// __generate_vector{{{
  template <typename _Tp, size_t _Np, typename _Gp, size_t... _I>
  _GLIBCXX_SIMD_INTRINSIC constexpr __vector_type_t<_Tp, _Np>
  __generate_vector_impl(_Gp&& __gen, index_sequence<_I...>)
  {
    return __vector_type_t<_Tp, _Np>{
      static_cast<_Tp>(__gen(_SizeConstant<_I>()))...};
  }

template <typename _V, typename _VVT = _VectorTraits<_V>, typename _Gp>
  _GLIBCXX_SIMD_INTRINSIC constexpr _V
  __generate_vector(_Gp&& __gen)
  {
    if constexpr (__is_vector_type_v<_V>)
      return __generate_vector_impl<typename _VVT::value_type,
				    _VVT::_S_full_size>(
	static_cast<_Gp&&>(__gen), make_index_sequence<_VVT::_S_full_size>());
    else
      return __generate_vector_impl<typename _VVT::value_type,
				    _VVT::_S_partial_width>(
	static_cast<_Gp&&>(__gen),
	make_index_sequence<_VVT::_S_partial_width>());
  }

template <typename _Tp, size_t _Np, typename _Gp>
  _GLIBCXX_SIMD_INTRINSIC constexpr __vector_type_t<_Tp, _Np>
  __generate_vector(_Gp&& __gen)
  {
    return __generate_vector_impl<_Tp, _Np>(static_cast<_Gp&&>(__gen),
					    make_index_sequence<_Np>());
  }

// }}}
// __xor{{{
template <typename _TW>
  _GLIBCXX_SIMD_INTRINSIC constexpr _TW
  __xor(_TW __a, _TW __b) noexcept
  {
    if constexpr (__is_vector_type_v<_TW> || __is_simd_wrapper_v<_TW>)
      {
	using _Tp = typename conditional_t<__is_simd_wrapper_v<_TW>, _TW,
					   _VectorTraitsImpl<_TW>>::value_type;
	if constexpr (is_floating_point_v<_Tp>)
	  {
	    using _Ip = make_unsigned_t<__int_for_sizeof_t<_Tp>>;
	    return __vector_bitcast<_Tp>(__vector_bitcast<_Ip>(__a)
					 ^ __vector_bitcast<_Ip>(__b));
	  }
	else if constexpr (__is_vector_type_v<_TW>)
	  return __a ^ __b;
	else
	  return __a._M_data ^ __b._M_data;
      }
    else
      return __a ^ __b;
  }

// }}}
// __or{{{
template <typename _TW>
  _GLIBCXX_SIMD_INTRINSIC constexpr _TW
  __or(_TW __a, _TW __b) noexcept
  {
    if constexpr (__is_vector_type_v<_TW> || __is_simd_wrapper_v<_TW>)
      {
	using _Tp = typename conditional_t<__is_simd_wrapper_v<_TW>, _TW,
					   _VectorTraitsImpl<_TW>>::value_type;
	if constexpr (is_floating_point_v<_Tp>)
	  {
	    using _Ip = make_unsigned_t<__int_for_sizeof_t<_Tp>>;
	    return __vector_bitcast<_Tp>(__vector_bitcast<_Ip>(__a)
					 | __vector_bitcast<_Ip>(__b));
	  }
	else if constexpr (__is_vector_type_v<_TW>)
	  return __a | __b;
	else
	  return __a._M_data | __b._M_data;
      }
    else
      return __a | __b;
  }

// }}}
// __and{{{
template <typename _TW>
  _GLIBCXX_SIMD_INTRINSIC constexpr _TW
  __and(_TW __a, _TW __b) noexcept
  {
    if constexpr (__is_vector_type_v<_TW> || __is_simd_wrapper_v<_TW>)
      {
	using _Tp = typename conditional_t<__is_simd_wrapper_v<_TW>, _TW,
					   _VectorTraitsImpl<_TW>>::value_type;
	if constexpr (is_floating_point_v<_Tp>)
	  {
	    using _Ip = make_unsigned_t<__int_for_sizeof_t<_Tp>>;
	    return __vector_bitcast<_Tp>(__vector_bitcast<_Ip>(__a)
					 & __vector_bitcast<_Ip>(__b));
	  }
	else if constexpr (__is_vector_type_v<_TW>)
	  return __a & __b;
	else
	  return __a._M_data & __b._M_data;
      }
    else
      return __a & __b;
  }

// }}}
// __andnot{{{
#if _GLIBCXX_SIMD_X86INTRIN && !defined __clang__
static constexpr struct
{
  _GLIBCXX_SIMD_INTRINSIC __v4sf
  operator()(__v4sf __a, __v4sf __b) const noexcept
  { return __builtin_ia32_andnps(__a, __b); }

  _GLIBCXX_SIMD_INTRINSIC __v2df
  operator()(__v2df __a, __v2df __b) const noexcept
  { return __builtin_ia32_andnpd(__a, __b); }

  _GLIBCXX_SIMD_INTRINSIC __v2di
  operator()(__v2di __a, __v2di __b) const noexcept
  { return __builtin_ia32_pandn128(__a, __b); }

  _GLIBCXX_SIMD_INTRINSIC __v8sf
  operator()(__v8sf __a, __v8sf __b) const noexcept
  { return __builtin_ia32_andnps256(__a, __b); }

  _GLIBCXX_SIMD_INTRINSIC __v4df
  operator()(__v4df __a, __v4df __b) const noexcept
  { return __builtin_ia32_andnpd256(__a, __b); }

  _GLIBCXX_SIMD_INTRINSIC __v4di
  operator()(__v4di __a, __v4di __b) const noexcept
  {
    if constexpr (__have_avx2)
      return __builtin_ia32_andnotsi256(__a, __b);
    else
      return reinterpret_cast<__v4di>(
	__builtin_ia32_andnpd256(reinterpret_cast<__v4df>(__a),
				 reinterpret_cast<__v4df>(__b)));
  }

  _GLIBCXX_SIMD_INTRINSIC __v16sf
  operator()(__v16sf __a, __v16sf __b) const noexcept
  {
    if constexpr (__have_avx512dq)
      return _mm512_andnot_ps(__a, __b);
    else
      return reinterpret_cast<__v16sf>(
	_mm512_andnot_si512(reinterpret_cast<__v8di>(__a),
			    reinterpret_cast<__v8di>(__b)));
  }

  _GLIBCXX_SIMD_INTRINSIC __v8df
  operator()(__v8df __a, __v8df __b) const noexcept
  {
    if constexpr (__have_avx512dq)
      return _mm512_andnot_pd(__a, __b);
    else
      return reinterpret_cast<__v8df>(
	_mm512_andnot_si512(reinterpret_cast<__v8di>(__a),
			    reinterpret_cast<__v8di>(__b)));
  }

  _GLIBCXX_SIMD_INTRINSIC __v8di
  operator()(__v8di __a, __v8di __b) const noexcept
  { return _mm512_andnot_si512(__a, __b); }
} _S_x86_andnot;
#endif // _GLIBCXX_SIMD_X86INTRIN && !__clang__

template <typename _TW>
  _GLIBCXX_SIMD_INTRINSIC constexpr _TW
  __andnot(_TW __a, _TW __b) noexcept
  {
    if constexpr (__is_vector_type_v<_TW> || __is_simd_wrapper_v<_TW>)
      {
	using _TVT = conditional_t<__is_simd_wrapper_v<_TW>, _TW,
				   _VectorTraitsImpl<_TW>>;
	using _Tp = typename _TVT::value_type;
#if _GLIBCXX_SIMD_X86INTRIN && !defined __clang__
	if constexpr (sizeof(_TW) >= 16)
	  {
	    const auto __ai = __to_intrin(__a);
	    const auto __bi = __to_intrin(__b);
	    if (!__builtin_is_constant_evaluated()
		&& !(__builtin_constant_p(__ai) && __builtin_constant_p(__bi)))
	      {
		const auto __r = _S_x86_andnot(__ai, __bi);
		if constexpr (is_convertible_v<decltype(__r), _TW>)
		  return __r;
		else
		  return reinterpret_cast<typename _TVT::type>(__r);
	      }
	  }
#endif // _GLIBCXX_SIMD_X86INTRIN
	using _Ip = make_unsigned_t<__int_for_sizeof_t<_Tp>>;
	return __vector_bitcast<_Tp>(~__vector_bitcast<_Ip>(__a)
				     & __vector_bitcast<_Ip>(__b));
      }
    else
      return ~__a & __b;
  }

// }}}
// __not{{{
template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC constexpr _Tp
  __not(_Tp __a) noexcept
  {
    if constexpr (is_floating_point_v<typename _TVT::value_type>)
      return reinterpret_cast<typename _TVT::type>(
	~__vector_bitcast<unsigned>(__a));
    else
      return ~__a;
  }

// }}}
// __concat{{{
template <typename _Tp, typename _TVT = _VectorTraits<_Tp>,
	  typename _R = __vector_type_t<typename _TVT::value_type,
					_TVT::_S_full_size * 2>>
  constexpr _R
  __concat(_Tp a_, _Tp b_)
  {
#ifdef _GLIBCXX_SIMD_WORKAROUND_XXX_1
    using _W
      = conditional_t<is_floating_point_v<typename _TVT::value_type>, double,
		      conditional_t<(sizeof(_Tp) >= 2 * sizeof(long long)),
				    long long, typename _TVT::value_type>>;
    constexpr int input_width = sizeof(_Tp) / sizeof(_W);
    const auto __a = __vector_bitcast<_W>(a_);
    const auto __b = __vector_bitcast<_W>(b_);
    using _Up = __vector_type_t<_W, sizeof(_R) / sizeof(_W)>;
#else
    constexpr int input_width = _TVT::_S_full_size;
    const _Tp& __a = a_;
    const _Tp& __b = b_;
    using _Up = _R;
#endif
    if constexpr (input_width == 2)
      return reinterpret_cast<_R>(_Up{__a[0], __a[1], __b[0], __b[1]});
    else if constexpr (input_width == 4)
      return reinterpret_cast<_R>(
	_Up{__a[0], __a[1], __a[2], __a[3], __b[0], __b[1], __b[2], __b[3]});
    else if constexpr (input_width == 8)
      return reinterpret_cast<_R>(
	_Up{__a[0], __a[1], __a[2], __a[3], __a[4], __a[5], __a[6], __a[7],
	    __b[0], __b[1], __b[2], __b[3], __b[4], __b[5], __b[6], __b[7]});
    else if constexpr (input_width == 16)
      return reinterpret_cast<_R>(
	_Up{__a[0],  __a[1],  __a[2],  __a[3],  __a[4],  __a[5],  __a[6],
	    __a[7],  __a[8],  __a[9],  __a[10], __a[11], __a[12], __a[13],
	    __a[14], __a[15], __b[0],  __b[1],  __b[2],  __b[3],  __b[4],
	    __b[5],  __b[6],  __b[7],  __b[8],  __b[9],  __b[10], __b[11],
	    __b[12], __b[13], __b[14], __b[15]});
    else if constexpr (input_width == 32)
      return reinterpret_cast<_R>(
	_Up{__a[0],  __a[1],  __a[2],  __a[3],  __a[4],  __a[5],  __a[6],
	    __a[7],  __a[8],  __a[9],  __a[10], __a[11], __a[12], __a[13],
	    __a[14], __a[15], __a[16], __a[17], __a[18], __a[19], __a[20],
	    __a[21], __a[22], __a[23], __a[24], __a[25], __a[26], __a[27],
	    __a[28], __a[29], __a[30], __a[31], __b[0],  __b[1],  __b[2],
	    __b[3],  __b[4],  __b[5],  __b[6],  __b[7],  __b[8],  __b[9],
	    __b[10], __b[11], __b[12], __b[13], __b[14], __b[15], __b[16],
	    __b[17], __b[18], __b[19], __b[20], __b[21], __b[22], __b[23],
	    __b[24], __b[25], __b[26], __b[27], __b[28], __b[29], __b[30],
	    __b[31]});
  }

// }}}
// __zero_extend {{{
template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  struct _ZeroExtendProxy
  {
    using value_type = typename _TVT::value_type;
    static constexpr size_t _Np = _TVT::_S_full_size;
    const _Tp __x;

    template <typename _To, typename _ToVT = _VectorTraits<_To>,
	      typename
	      = enable_if_t<is_same_v<typename _ToVT::value_type, value_type>>>
      _GLIBCXX_SIMD_INTRINSIC operator _To() const
      {
	constexpr size_t _ToN = _ToVT::_S_full_size;
	if constexpr (_ToN == _Np)
	  return __x;
	else if constexpr (_ToN == 2 * _Np)
	  {
#ifdef _GLIBCXX_SIMD_WORKAROUND_XXX_3
	    if constexpr (__have_avx && _TVT::template _S_is<float, 4>)
	      return __vector_bitcast<value_type>(
		_mm256_insertf128_ps(__m256(), __x, 0));
	    else if constexpr (__have_avx && _TVT::template _S_is<double, 2>)
	      return __vector_bitcast<value_type>(
		_mm256_insertf128_pd(__m256d(), __x, 0));
	    else if constexpr (__have_avx2 && _Np * sizeof(value_type) == 16)
	      return __vector_bitcast<value_type>(
		_mm256_insertf128_si256(__m256i(), __to_intrin(__x), 0));
	    else if constexpr (__have_avx512f && _TVT::template _S_is<float, 8>)
	      {
		if constexpr (__have_avx512dq)
		  return __vector_bitcast<value_type>(
		    _mm512_insertf32x8(__m512(), __x, 0));
		else
		  return reinterpret_cast<__m512>(
		    _mm512_insertf64x4(__m512d(),
				       reinterpret_cast<__m256d>(__x), 0));
	      }
	    else if constexpr (__have_avx512f
			       && _TVT::template _S_is<double, 4>)
	      return __vector_bitcast<value_type>(
		_mm512_insertf64x4(__m512d(), __x, 0));
	    else if constexpr (__have_avx512f && _Np * sizeof(value_type) == 32)
	      return __vector_bitcast<value_type>(
		_mm512_inserti64x4(__m512i(), __to_intrin(__x), 0));
#endif
	    return __concat(__x, _Tp());
	  }
	else if constexpr (_ToN == 4 * _Np)
	  {
#ifdef _GLIBCXX_SIMD_WORKAROUND_XXX_3
	    if constexpr (__have_avx512dq && _TVT::template _S_is<double, 2>)
	      {
		return __vector_bitcast<value_type>(
		  _mm512_insertf64x2(__m512d(), __x, 0));
	      }
	    else if constexpr (__have_avx512f
			       && is_floating_point_v<value_type>)
	      {
		return __vector_bitcast<value_type>(
		  _mm512_insertf32x4(__m512(), reinterpret_cast<__m128>(__x),
				     0));
	      }
	    else if constexpr (__have_avx512f && _Np * sizeof(value_type) == 16)
	      {
		return __vector_bitcast<value_type>(
		  _mm512_inserti32x4(__m512i(), __to_intrin(__x), 0));
	      }
#endif
	    return __concat(__concat(__x, _Tp()),
			    __vector_type_t<value_type, _Np * 2>());
	  }
	else if constexpr (_ToN == 8 * _Np)
	  return __concat(operator __vector_type_t<value_type, _Np * 4>(),
			  __vector_type_t<value_type, _Np * 4>());
	else if constexpr (_ToN == 16 * _Np)
	  return __concat(operator __vector_type_t<value_type, _Np * 8>(),
			  __vector_type_t<value_type, _Np * 8>());
	else
	  __assert_unreachable<_Tp>();
      }
  };

template <typename _Tp, typename _TVT = _VectorTraits<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC _ZeroExtendProxy<_Tp, _TVT>
  __zero_extend(_Tp __x)
  { return {__x}; }

// }}}
// __extract<_Np, By>{{{
template <int _Offset,
	  int _SplitBy,
	  typename _Tp,
	  typename _TVT = _VectorTraits<_Tp>,
	  typename _R = __vector_type_t<typename _TVT::value_type,
			  _TVT::_S_full_size / _SplitBy>>
  _GLIBCXX_SIMD_INTRINSIC constexpr _R
  __extract(_Tp __in)
  {
    using value_type = typename _TVT::value_type;
#if _GLIBCXX_SIMD_X86INTRIN // {{{
    if constexpr (sizeof(_Tp) == 64 && _SplitBy == 4 && _Offset > 0)
      {
	if constexpr (__have_avx512dq && is_same_v<double, value_type>)
	  return _mm512_extractf64x2_pd(__to_intrin(__in), _Offset);
	else if constexpr (is_floating_point_v<value_type>)
	  return __vector_bitcast<value_type>(
	    _mm512_extractf32x4_ps(__intrin_bitcast<__m512>(__in), _Offset));
	else
	  return reinterpret_cast<_R>(
	    _mm512_extracti32x4_epi32(__intrin_bitcast<__m512i>(__in),
				      _Offset));
      }
    else
#endif // _GLIBCXX_SIMD_X86INTRIN }}}
      {
#ifdef _GLIBCXX_SIMD_WORKAROUND_XXX_1
	using _W = conditional_t<
	  is_floating_point_v<value_type>, double,
	  conditional_t<(sizeof(_R) >= 16), long long, value_type>>;
	static_assert(sizeof(_R) % sizeof(_W) == 0);
	constexpr int __return_width = sizeof(_R) / sizeof(_W);
	using _Up = __vector_type_t<_W, __return_width>;
	const auto __x = __vector_bitcast<_W>(__in);
#else
      constexpr int __return_width = _TVT::_S_full_size / _SplitBy;
      using _Up = _R;
      const __vector_type_t<value_type, _TVT::_S_full_size>& __x
	= __in; // only needed for _Tp = _SimdWrapper<value_type, _Np>
#endif
	constexpr int _O = _Offset * __return_width;
	return __call_with_subscripts<__return_width, _O>(
	  __x, [](auto... __entries) {
	    return reinterpret_cast<_R>(_Up{__entries...});
	  });
      }
  }

// }}}
// __lo/__hi64[z]{{{
template <typename _Tp,
	  typename _R
	  = __vector_type8_t<typename _VectorTraits<_Tp>::value_type>>
  _GLIBCXX_SIMD_INTRINSIC constexpr _R
  __lo64(_Tp __x)
  {
    _R __r{};
    __builtin_memcpy(&__r, &__x, 8);
    return __r;
  }

template <typename _Tp,
	  typename _R
	  = __vector_type8_t<typename _VectorTraits<_Tp>::value_type>>
  _GLIBCXX_SIMD_INTRINSIC constexpr _R
  __hi64(_Tp __x)
  {
    static_assert(sizeof(_Tp) == 16, "use __hi64z if you meant it");
    _R __r{};
    __builtin_memcpy(&__r, reinterpret_cast<const char*>(&__x) + 8, 8);
    return __r;
  }

template <typename _Tp,
	  typename _R
	  = __vector_type8_t<typename _VectorTraits<_Tp>::value_type>>
  _GLIBCXX_SIMD_INTRINSIC constexpr _R
  __hi64z([[maybe_unused]] _Tp __x)
  {
    _R __r{};
    if constexpr (sizeof(_Tp) == 16)
      __builtin_memcpy(&__r, reinterpret_cast<const char*>(&__x) + 8, 8);
    return __r;
  }

// }}}
// __lo/__hi128{{{
template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __lo128(_Tp __x)
  { return __extract<0, sizeof(_Tp) / 16>(__x); }

template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __hi128(_Tp __x)
  {
    static_assert(sizeof(__x) == 32);
    return __extract<1, 2>(__x);
  }

// }}}
// __lo/__hi256{{{
template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __lo256(_Tp __x)
  {
    static_assert(sizeof(__x) == 64);
    return __extract<0, 2>(__x);
  }

template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __hi256(_Tp __x)
  {
    static_assert(sizeof(__x) == 64);
    return __extract<1, 2>(__x);
  }

// }}}
// __auto_bitcast{{{
template <typename _Tp>
  struct _AutoCast
  {
    static_assert(__is_vector_type_v<_Tp>);

    const _Tp __x;

    template <typename _Up, typename _UVT = _VectorTraits<_Up>>
      _GLIBCXX_SIMD_INTRINSIC constexpr operator _Up() const
      { return __intrin_bitcast<typename _UVT::type>(__x); }
  };

template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC constexpr _AutoCast<_Tp>
  __auto_bitcast(const _Tp& __x)
  { return {__x}; }

template <typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC constexpr
  _AutoCast<typename _SimdWrapper<_Tp, _Np>::_BuiltinType>
  __auto_bitcast(const _SimdWrapper<_Tp, _Np>& __x)
  { return {__x._M_data}; }

// }}}
// ^^^ ---- builtin vector types [[gnu::vector_size(N)]] and operations ---- ^^^

#if _GLIBCXX_SIMD_HAVE_SSE_ABI
// __bool_storage_member_type{{{
#if _GLIBCXX_SIMD_HAVE_AVX512F && _GLIBCXX_SIMD_X86INTRIN
template <size_t _Size>
  struct __bool_storage_member_type
  {
    static_assert((_Size & (_Size - 1)) != 0,
		  "This trait may only be used for non-power-of-2 sizes. "
		  "Power-of-2 sizes must be specialized.");
    using type =
      typename __bool_storage_member_type<std::__bit_ceil(_Size)>::type;
  };

template <>
  struct __bool_storage_member_type<1> { using type = bool; };

template <>
  struct __bool_storage_member_type<2> { using type = __mmask8; };

template <>
  struct __bool_storage_member_type<4> { using type = __mmask8; };

template <>
  struct __bool_storage_member_type<8> { using type = __mmask8; };

template <>
  struct __bool_storage_member_type<16> { using type = __mmask16; };

template <>
  struct __bool_storage_member_type<32> { using type = __mmask32; };

template <>
  struct __bool_storage_member_type<64> { using type = __mmask64; };
#endif // _GLIBCXX_SIMD_HAVE_AVX512F

// }}}
// __intrinsic_type (x86){{{
// the following excludes bool via __is_vectorizable
#if _GLIBCXX_SIMD_HAVE_SSE
template <typename _Tp, size_t _Bytes>
  struct __intrinsic_type<_Tp, _Bytes,
			  enable_if_t<__is_vectorizable_v<_Tp> && _Bytes <= 64>>
  {
    static_assert(!is_same_v<_Tp, long double>,
		  "no __intrinsic_type support for long double on x86");

    static constexpr size_t _S_VBytes = _Bytes <= 16   ? 16
					: _Bytes <= 32 ? 32
						       : 64;

    using type [[__gnu__::__vector_size__(_S_VBytes)]]
    = conditional_t<is_integral_v<_Tp>, long long int, _Tp>;
  };
#endif // _GLIBCXX_SIMD_HAVE_SSE

// }}}
#endif // _GLIBCXX_SIMD_HAVE_SSE_ABI
// __intrinsic_type (ARM){{{
#if _GLIBCXX_SIMD_HAVE_NEON
template <>
  struct __intrinsic_type<float, 8, void>
  { using type = float32x2_t; };

template <>
  struct __intrinsic_type<float, 16, void>
  { using type = float32x4_t; };

#if _GLIBCXX_SIMD_HAVE_NEON_A64
template <>
  struct __intrinsic_type<double, 8, void>
  { using type = float64x1_t; };

template <>
  struct __intrinsic_type<double, 16, void>
  { using type = float64x2_t; };
#endif

#define _GLIBCXX_SIMD_ARM_INTRIN(_Bits, _Np)                                   \
template <>                                                                    \
  struct __intrinsic_type<__int_with_sizeof_t<_Bits / 8>,                      \
			  _Np * _Bits / 8, void>                               \
  { using type = int##_Bits##x##_Np##_t; };                                    \
template <>                                                                    \
  struct __intrinsic_type<make_unsigned_t<__int_with_sizeof_t<_Bits / 8>>,     \
			  _Np * _Bits / 8, void>                               \
  { using type = uint##_Bits##x##_Np##_t; }
_GLIBCXX_SIMD_ARM_INTRIN(8, 8);
_GLIBCXX_SIMD_ARM_INTRIN(8, 16);
_GLIBCXX_SIMD_ARM_INTRIN(16, 4);
_GLIBCXX_SIMD_ARM_INTRIN(16, 8);
_GLIBCXX_SIMD_ARM_INTRIN(32, 2);
_GLIBCXX_SIMD_ARM_INTRIN(32, 4);
_GLIBCXX_SIMD_ARM_INTRIN(64, 1);
_GLIBCXX_SIMD_ARM_INTRIN(64, 2);
#undef _GLIBCXX_SIMD_ARM_INTRIN

template <typename _Tp, size_t _Bytes>
  struct __intrinsic_type<_Tp, _Bytes,
			  enable_if_t<__is_vectorizable_v<_Tp> && _Bytes <= 16>>
  {
    static constexpr int _SVecBytes = _Bytes <= 8 ? 8 : 16;
    using _Ip = __int_for_sizeof_t<_Tp>;
    using _Up = conditional_t<
      is_floating_point_v<_Tp>, _Tp,
      conditional_t<is_unsigned_v<_Tp>, make_unsigned_t<_Ip>, _Ip>>;
    static_assert(!is_same_v<_Tp, _Up> || _SVecBytes != _Bytes,
		  "should use explicit specialization above");
    using type = typename __intrinsic_type<_Up, _SVecBytes>::type;
  };
#endif // _GLIBCXX_SIMD_HAVE_NEON

// }}}
// __intrinsic_type (PPC){{{
#ifdef __ALTIVEC__
template <typename _Tp>
  struct __intrinsic_type_impl;

#define _GLIBCXX_SIMD_PPC_INTRIN(_Tp)                                          \
  template <>                                                                  \
    struct __intrinsic_type_impl<_Tp> { using type = __vector _Tp; }
_GLIBCXX_SIMD_PPC_INTRIN(float);
#ifdef __VSX__
_GLIBCXX_SIMD_PPC_INTRIN(double);
#endif
_GLIBCXX_SIMD_PPC_INTRIN(signed char);
_GLIBCXX_SIMD_PPC_INTRIN(unsigned char);
_GLIBCXX_SIMD_PPC_INTRIN(signed short);
_GLIBCXX_SIMD_PPC_INTRIN(unsigned short);
_GLIBCXX_SIMD_PPC_INTRIN(signed int);
_GLIBCXX_SIMD_PPC_INTRIN(unsigned int);
#if defined __VSX__ || __SIZEOF_LONG__ == 4
_GLIBCXX_SIMD_PPC_INTRIN(signed long);
_GLIBCXX_SIMD_PPC_INTRIN(unsigned long);
#endif
#ifdef __VSX__
_GLIBCXX_SIMD_PPC_INTRIN(signed long long);
_GLIBCXX_SIMD_PPC_INTRIN(unsigned long long);
#endif
#undef _GLIBCXX_SIMD_PPC_INTRIN

template <typename _Tp, size_t _Bytes>
  struct __intrinsic_type<_Tp, _Bytes,
			  enable_if_t<__is_vectorizable_v<_Tp> && _Bytes <= 16>>
  {
    static constexpr bool _S_is_ldouble = is_same_v<_Tp, long double>;
    // allow _Tp == long double with -mlong-double-64
    static_assert(!(_S_is_ldouble && sizeof(long double) > sizeof(double)),
		  "no __intrinsic_type support for 128-bit floating point on PowerPC");
#ifndef __VSX__
    static_assert(!(is_same_v<_Tp, double>
		    || (_S_is_ldouble && sizeof(long double) == sizeof(double))),
		  "no __intrinsic_type support for 64-bit floating point on PowerPC w/o VSX");
#endif
    using type =
      typename __intrinsic_type_impl<
		 conditional_t<is_floating_point_v<_Tp>,
			       conditional_t<_S_is_ldouble, double, _Tp>,
			       __int_for_sizeof_t<_Tp>>>::type;
  };
#endif // __ALTIVEC__

// }}}
// _SimdWrapper<bool>{{{1
template <size_t _Width>
  struct _SimdWrapper<bool, _Width,
		      void_t<typename __bool_storage_member_type<_Width>::type>>
  {
    using _BuiltinType = typename __bool_storage_member_type<_Width>::type;
    using value_type = bool;

    static constexpr size_t _S_full_size = sizeof(_BuiltinType) * __CHAR_BIT__;

    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper<bool, _S_full_size>
    __as_full_vector() const { return _M_data; }

    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper() = default;
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper(_BuiltinType __k)
      : _M_data(__k) {};

    _GLIBCXX_SIMD_INTRINSIC operator const _BuiltinType&() const
    { return _M_data; }

    _GLIBCXX_SIMD_INTRINSIC operator _BuiltinType&()
    { return _M_data; }

    _GLIBCXX_SIMD_INTRINSIC _BuiltinType __intrin() const
    { return _M_data; }

    _GLIBCXX_SIMD_INTRINSIC constexpr value_type operator[](size_t __i) const
    { return _M_data & (_BuiltinType(1) << __i); }

    template <size_t __i>
      _GLIBCXX_SIMD_INTRINSIC constexpr value_type
      operator[](_SizeConstant<__i>) const
      { return _M_data & (_BuiltinType(1) << __i); }

    _GLIBCXX_SIMD_INTRINSIC constexpr void _M_set(size_t __i, value_type __x)
    {
      if (__x)
	_M_data |= (_BuiltinType(1) << __i);
      else
	_M_data &= ~(_BuiltinType(1) << __i);
    }

    _GLIBCXX_SIMD_INTRINSIC
    constexpr bool _M_is_constprop() const
    { return __builtin_constant_p(_M_data); }

    _GLIBCXX_SIMD_INTRINSIC constexpr bool _M_is_constprop_none_of() const
    {
      if (__builtin_constant_p(_M_data))
	{
	  constexpr int __nbits = sizeof(_BuiltinType) * __CHAR_BIT__;
	  constexpr _BuiltinType __active_mask
	    = ~_BuiltinType() >> (__nbits - _Width);
	  return (_M_data & __active_mask) == 0;
	}
      return false;
    }

    _GLIBCXX_SIMD_INTRINSIC constexpr bool _M_is_constprop_all_of() const
    {
      if (__builtin_constant_p(_M_data))
	{
	  constexpr int __nbits = sizeof(_BuiltinType) * __CHAR_BIT__;
	  constexpr _BuiltinType __active_mask
	    = ~_BuiltinType() >> (__nbits - _Width);
	  return (_M_data & __active_mask) == __active_mask;
	}
      return false;
    }

    _BuiltinType _M_data;
  };

// _SimdWrapperBase{{{1
template <bool _MustZeroInitPadding, typename _BuiltinType>
  struct _SimdWrapperBase;

template <typename _BuiltinType>
  struct _SimdWrapperBase<false, _BuiltinType> // no padding or no SNaNs
  {
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapperBase() = default;
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapperBase(_BuiltinType __init)
      : _M_data(__init)
    {}

    _BuiltinType _M_data;
  };

template <typename _BuiltinType>
  struct _SimdWrapperBase<true, _BuiltinType> // with padding that needs to
					      // never become SNaN
  {
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapperBase() : _M_data() {}
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapperBase(_BuiltinType __init)
      : _M_data(__init)
    {}

    _BuiltinType _M_data;
  };

// }}}
// _SimdWrapper{{{
template <typename _Tp, size_t _Width>
  struct _SimdWrapper<
    _Tp, _Width,
    void_t<__vector_type_t<_Tp, _Width>, __intrinsic_type_t<_Tp, _Width>>>
    : _SimdWrapperBase<__has_iec559_behavior<__signaling_NaN, _Tp>::value
			 && sizeof(_Tp) * _Width
			      == sizeof(__vector_type_t<_Tp, _Width>),
		       __vector_type_t<_Tp, _Width>>
  {
    using _Base
      = _SimdWrapperBase<__has_iec559_behavior<__signaling_NaN, _Tp>::value
			   && sizeof(_Tp) * _Width
				== sizeof(__vector_type_t<_Tp, _Width>),
			 __vector_type_t<_Tp, _Width>>;

    static_assert(__is_vectorizable_v<_Tp>);
    static_assert(_Width >= 2); // 1 doesn't make sense, use _Tp directly then

    using _BuiltinType = __vector_type_t<_Tp, _Width>;
    using value_type = _Tp;

    static inline constexpr size_t _S_full_size
      = sizeof(_BuiltinType) / sizeof(value_type);
    static inline constexpr int _S_size = _Width;
    static inline constexpr bool _S_is_partial = _S_full_size != _S_size;

    using _Base::_M_data;

    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper<_Tp, _S_full_size>
    __as_full_vector() const
    { return _M_data; }

    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper(initializer_list<_Tp> __init)
      : _Base(__generate_from_n_evaluations<_Width, _BuiltinType>(
	[&](auto __i) { return __init.begin()[__i.value]; })) {}

    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper() = default;
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper(const _SimdWrapper&)
      = default;
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper(_SimdWrapper&&) = default;

    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper&
    operator=(const _SimdWrapper&) = default;
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper&
    operator=(_SimdWrapper&&) = default;

    template <typename _V, typename = enable_if_t<disjunction_v<
			     is_same<_V, __vector_type_t<_Tp, _Width>>,
			     is_same<_V, __intrinsic_type_t<_Tp, _Width>>>>>
      _GLIBCXX_SIMD_INTRINSIC constexpr _SimdWrapper(_V __x)
      // __vector_bitcast can convert e.g. __m128 to __vector(2) float
      : _Base(__vector_bitcast<_Tp, _Width>(__x)) {}

    template <typename... _As,
	      typename = enable_if_t<((is_same_v<simd_abi::scalar, _As> && ...)
				      && sizeof...(_As) <= _Width)>>
      _GLIBCXX_SIMD_INTRINSIC constexpr
      operator _SimdTuple<_Tp, _As...>() const
      {
	const auto& dd = _M_data; // workaround for GCC7 ICE
	return __generate_from_n_evaluations<sizeof...(_As),
					     _SimdTuple<_Tp, _As...>>([&](
	  auto __i) constexpr { return dd[int(__i)]; });
      }

    _GLIBCXX_SIMD_INTRINSIC constexpr operator const _BuiltinType&() const
    { return _M_data; }

    _GLIBCXX_SIMD_INTRINSIC constexpr operator _BuiltinType&()
    { return _M_data; }

    _GLIBCXX_SIMD_INTRINSIC constexpr _Tp operator[](size_t __i) const
    { return _M_data[__i]; }

    template <size_t __i>
      _GLIBCXX_SIMD_INTRINSIC constexpr _Tp operator[](_SizeConstant<__i>) const
      { return _M_data[__i]; }

    _GLIBCXX_SIMD_INTRINSIC constexpr void _M_set(size_t __i, _Tp __x)
    { _M_data[__i] = __x; }

    _GLIBCXX_SIMD_INTRINSIC
    constexpr bool _M_is_constprop() const
    { return __builtin_constant_p(_M_data); }

    _GLIBCXX_SIMD_INTRINSIC constexpr bool _M_is_constprop_none_of() const
    {
      if (__builtin_constant_p(_M_data))
	{
	  bool __r = true;
	  if constexpr (is_floating_point_v<_Tp>)
	    {
	      using _Ip = __int_for_sizeof_t<_Tp>;
	      const auto __intdata = __vector_bitcast<_Ip>(_M_data);
	      __execute_n_times<_Width>(
		[&](auto __i) { __r &= __intdata[__i.value] == _Ip(); });
	    }
	  else
	    __execute_n_times<_Width>(
	      [&](auto __i) { __r &= _M_data[__i.value] == _Tp(); });
	  return __r;
	}
      return false;
    }

    _GLIBCXX_SIMD_INTRINSIC constexpr bool _M_is_constprop_all_of() const
    {
      if (__builtin_constant_p(_M_data))
	{
	  bool __r = true;
	  if constexpr (is_floating_point_v<_Tp>)
	    {
	      using _Ip = __int_for_sizeof_t<_Tp>;
	      const auto __intdata = __vector_bitcast<_Ip>(_M_data);
	      __execute_n_times<_Width>(
		[&](auto __i) { __r &= __intdata[__i.value] == ~_Ip(); });
	    }
	  else
	    __execute_n_times<_Width>(
	      [&](auto __i) { __r &= _M_data[__i.value] == ~_Tp(); });
	  return __r;
	}
      return false;
    }
  };

// }}}

// __vectorized_sizeof {{{
template <typename _Tp>
  constexpr size_t
  __vectorized_sizeof()
  {
    if constexpr (!__is_vectorizable_v<_Tp>)
      return 0;

    if constexpr (sizeof(_Tp) <= 8)
      {
	// X86:
	if constexpr (__have_avx512bw)
	  return 64;
	if constexpr (__have_avx512f && sizeof(_Tp) >= 4)
	  return 64;
	if constexpr (__have_avx2)
	  return 32;
	if constexpr (__have_avx && is_floating_point_v<_Tp>)
	  return 32;
	if constexpr (__have_sse2)
	  return 16;
	if constexpr (__have_sse && is_same_v<_Tp, float>)
	  return 16;
	/* The following is too much trouble because of mixed MMX and x87 code.
	 * While nothing here explicitly calls MMX instructions of registers,
	 * they are still emitted but no EMMS cleanup is done.
	if constexpr (__have_mmx && sizeof(_Tp) <= 4 && is_integral_v<_Tp>)
	  return 8;
	 */

	// PowerPC:
	if constexpr (__have_power8vec
		      || (__have_power_vmx && (sizeof(_Tp) < 8))
		      || (__have_power_vsx && is_floating_point_v<_Tp>) )
	  return 16;

	// ARM:
	if constexpr (__have_neon_a64
		      || (__have_neon_a32 && !is_same_v<_Tp, double>) )
	  return 16;
	if constexpr (__have_neon
		      && sizeof(_Tp) < 8
		      // Only allow fp if the user allows non-ICE559 fp (e.g.
		      // via -ffast-math). ARMv7 NEON fp is not conforming to
		      // IEC559.
		      && (__support_neon_float || !is_floating_point_v<_Tp>))
	  return 16;
      }

    return sizeof(_Tp);
  }

// }}}
namespace simd_abi {
// most of simd_abi is defined in simd_detail.h
template <typename _Tp>
  inline constexpr int max_fixed_size
    = (__have_avx512bw && sizeof(_Tp) == 1) ? 64 : 32;

// compatible {{{
#if defined __x86_64__ || defined __aarch64__
template <typename _Tp>
  using compatible = conditional_t<(sizeof(_Tp) <= 8), _VecBuiltin<16>, scalar>;
#elif defined __ARM_NEON
// FIXME: not sure, probably needs to be scalar (or dependent on the hard-float
// ABI?)
template <typename _Tp>
  using compatible
    = conditional_t<(sizeof(_Tp) < 8
		     && (__support_neon_float || !is_floating_point_v<_Tp>)),
		    _VecBuiltin<16>, scalar>;
#else
template <typename>
  using compatible = scalar;
#endif

// }}}
// native {{{
template <typename _Tp>
  constexpr auto
  __determine_native_abi()
  {
    constexpr size_t __bytes = __vectorized_sizeof<_Tp>();
    if constexpr (__bytes == sizeof(_Tp))
      return static_cast<scalar*>(nullptr);
    else if constexpr (__have_avx512vl || (__have_avx512f && __bytes == 64))
      return static_cast<_VecBltnBtmsk<__bytes>*>(nullptr);
    else
      return static_cast<_VecBuiltin<__bytes>*>(nullptr);
  }

template <typename _Tp, typename = enable_if_t<__is_vectorizable_v<_Tp>>>
  using native = remove_pointer_t<decltype(__determine_native_abi<_Tp>())>;

// }}}
// __default_abi {{{
#if defined _GLIBCXX_SIMD_DEFAULT_ABI
template <typename _Tp>
  using __default_abi = _GLIBCXX_SIMD_DEFAULT_ABI<_Tp>;
#else
template <typename _Tp>
  using __default_abi = compatible<_Tp>;
#endif

// }}}
} // namespace simd_abi

// traits {{{1
// is_abi_tag {{{2
template <typename _Tp, typename = void_t<>>
  struct is_abi_tag : false_type {};

template <typename _Tp>
  struct is_abi_tag<_Tp, void_t<typename _Tp::_IsValidAbiTag>>
  : public _Tp::_IsValidAbiTag {};

template <typename _Tp>
  inline constexpr bool is_abi_tag_v = is_abi_tag<_Tp>::value;

// is_simd(_mask) {{{2
template <typename _Tp>
  struct is_simd : public false_type {};

template <typename _Tp>
  inline constexpr bool is_simd_v = is_simd<_Tp>::value;

template <typename _Tp>
  struct is_simd_mask : public false_type {};

template <typename _Tp>
inline constexpr bool is_simd_mask_v = is_simd_mask<_Tp>::value;

// simd_size {{{2
template <typename _Tp, typename _Abi, typename = void>
  struct __simd_size_impl {};

template <typename _Tp, typename _Abi>
  struct __simd_size_impl<
    _Tp, _Abi,
    enable_if_t<conjunction_v<__is_vectorizable<_Tp>, is_abi_tag<_Abi>>>>
    : _SizeConstant<_Abi::template _S_size<_Tp>> {};

template <typename _Tp, typename _Abi = simd_abi::__default_abi<_Tp>>
  struct simd_size : __simd_size_impl<_Tp, _Abi> {};

template <typename _Tp, typename _Abi = simd_abi::__default_abi<_Tp>>
  inline constexpr size_t simd_size_v = simd_size<_Tp, _Abi>::value;

// simd_abi::deduce {{{2
template <typename _Tp, size_t _Np, typename = void>
  struct __deduce_impl;

namespace simd_abi {
/**
 * @tparam _Tp   The requested `value_type` for the elements.
 * @tparam _Np    The requested number of elements.
 * @tparam _Abis This parameter is ignored, since this implementation cannot
 * make any use of it. Either __a good native ABI is matched and used as `type`
 * alias, or the `fixed_size<_Np>` ABI is used, which internally is built from
 * the best matching native ABIs.
 */
template <typename _Tp, size_t _Np, typename...>
  struct deduce : __deduce_impl<_Tp, _Np> {};

template <typename _Tp, size_t _Np, typename... _Abis>
  using deduce_t = typename deduce<_Tp, _Np, _Abis...>::type;
} // namespace simd_abi

// }}}2
// rebind_simd {{{2
template <typename _Tp, typename _V, typename = void>
  struct rebind_simd;

template <typename _Tp, typename _Up, typename _Abi>
  struct rebind_simd<
    _Tp, simd<_Up, _Abi>,
    void_t<simd_abi::deduce_t<_Tp, simd_size_v<_Up, _Abi>, _Abi>>>
  {
    using type
      = simd<_Tp, simd_abi::deduce_t<_Tp, simd_size_v<_Up, _Abi>, _Abi>>;
  };

template <typename _Tp, typename _Up, typename _Abi>
  struct rebind_simd<
    _Tp, simd_mask<_Up, _Abi>,
    void_t<simd_abi::deduce_t<_Tp, simd_size_v<_Up, _Abi>, _Abi>>>
  {
    using type
      = simd_mask<_Tp, simd_abi::deduce_t<_Tp, simd_size_v<_Up, _Abi>, _Abi>>;
  };

template <typename _Tp, typename _V>
  using rebind_simd_t = typename rebind_simd<_Tp, _V>::type;

// resize_simd {{{2
template <int _Np, typename _V, typename = void>
  struct resize_simd;

template <int _Np, typename _Tp, typename _Abi>
  struct resize_simd<_Np, simd<_Tp, _Abi>,
		     void_t<simd_abi::deduce_t<_Tp, _Np, _Abi>>>
  { using type = simd<_Tp, simd_abi::deduce_t<_Tp, _Np, _Abi>>; };

template <int _Np, typename _Tp, typename _Abi>
  struct resize_simd<_Np, simd_mask<_Tp, _Abi>,
		     void_t<simd_abi::deduce_t<_Tp, _Np, _Abi>>>
  { using type = simd_mask<_Tp, simd_abi::deduce_t<_Tp, _Np, _Abi>>; };

template <int _Np, typename _V>
  using resize_simd_t = typename resize_simd<_Np, _V>::type;

// }}}2
// memory_alignment {{{2
template <typename _Tp, typename _Up = typename _Tp::value_type>
  struct memory_alignment
  : public _SizeConstant<vector_aligned_tag::_S_alignment<_Tp, _Up>> {};

template <typename _Tp, typename _Up = typename _Tp::value_type>
  inline constexpr size_t memory_alignment_v = memory_alignment<_Tp, _Up>::value;

// class template simd [simd] {{{1
template <typename _Tp, typename _Abi = simd_abi::__default_abi<_Tp>>
  class simd;

template <typename _Tp, typename _Abi>
  struct is_simd<simd<_Tp, _Abi>> : public true_type {};

template <typename _Tp>
  using native_simd = simd<_Tp, simd_abi::native<_Tp>>;

template <typename _Tp, int _Np>
  using fixed_size_simd = simd<_Tp, simd_abi::fixed_size<_Np>>;

template <typename _Tp, size_t _Np>
  using __deduced_simd = simd<_Tp, simd_abi::deduce_t<_Tp, _Np>>;

// class template simd_mask [simd_mask] {{{1
template <typename _Tp, typename _Abi = simd_abi::__default_abi<_Tp>>
  class simd_mask;

template <typename _Tp, typename _Abi>
  struct is_simd_mask<simd_mask<_Tp, _Abi>> : public true_type {};

template <typename _Tp>
  using native_simd_mask = simd_mask<_Tp, simd_abi::native<_Tp>>;

template <typename _Tp, int _Np>
  using fixed_size_simd_mask = simd_mask<_Tp, simd_abi::fixed_size<_Np>>;

template <typename _Tp, size_t _Np>
  using __deduced_simd_mask = simd_mask<_Tp, simd_abi::deduce_t<_Tp, _Np>>;

// casts [simd.casts] {{{1
// static_simd_cast {{{2
template <typename _Tp, typename _Up, typename _Ap, bool = is_simd_v<_Tp>,
	  typename = void>
  struct __static_simd_cast_return_type;

template <typename _Tp, typename _A0, typename _Up, typename _Ap>
  struct __static_simd_cast_return_type<simd_mask<_Tp, _A0>, _Up, _Ap, false,
					void>
  : __static_simd_cast_return_type<simd<_Tp, _A0>, _Up, _Ap> {};

template <typename _Tp, typename _Up, typename _Ap>
  struct __static_simd_cast_return_type<
    _Tp, _Up, _Ap, true, enable_if_t<_Tp::size() == simd_size_v<_Up, _Ap>>>
  { using type = _Tp; };

template <typename _Tp, typename _Ap>
  struct __static_simd_cast_return_type<_Tp, _Tp, _Ap, false,
#ifdef _GLIBCXX_SIMD_FIX_P2TS_ISSUE66
					enable_if_t<__is_vectorizable_v<_Tp>>
#else
					void
#endif
					>
  { using type = simd<_Tp, _Ap>; };

template <typename _Tp, typename = void>
  struct __safe_make_signed { using type = _Tp;};

template <typename _Tp>
  struct __safe_make_signed<_Tp, enable_if_t<is_integral_v<_Tp>>>
  {
    // the extra make_unsigned_t is because of PR85951
    using type = make_signed_t<make_unsigned_t<_Tp>>;
  };

template <typename _Tp>
  using safe_make_signed_t = typename __safe_make_signed<_Tp>::type;

template <typename _Tp, typename _Up, typename _Ap>
  struct __static_simd_cast_return_type<_Tp, _Up, _Ap, false,
#ifdef _GLIBCXX_SIMD_FIX_P2TS_ISSUE66
					enable_if_t<__is_vectorizable_v<_Tp>>
#else
					void
#endif
					>
  {
    using type = conditional_t<
      (is_integral_v<_Up> && is_integral_v<_Tp> &&
#ifndef _GLIBCXX_SIMD_FIX_P2TS_ISSUE65
       is_signed_v<_Up> != is_signed_v<_Tp> &&
#endif
       is_same_v<safe_make_signed_t<_Up>, safe_make_signed_t<_Tp>>),
      simd<_Tp, _Ap>, fixed_size_simd<_Tp, simd_size_v<_Up, _Ap>>>;
  };

template <typename _Tp, typename _Up, typename _Ap,
	  typename _R
	  = typename __static_simd_cast_return_type<_Tp, _Up, _Ap>::type>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR _R
  static_simd_cast(const simd<_Up, _Ap>& __x)
  {
    if constexpr (is_same<_R, simd<_Up, _Ap>>::value)
      return __x;
    else
      {
	_SimdConverter<_Up, _Ap, typename _R::value_type, typename _R::abi_type>
	  __c;
	return _R(__private_init, __c(__data(__x)));
      }
  }

namespace __proposed {
template <typename _Tp, typename _Up, typename _Ap,
	  typename _R
	  = typename __static_simd_cast_return_type<_Tp, _Up, _Ap>::type>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR typename _R::mask_type
  static_simd_cast(const simd_mask<_Up, _Ap>& __x)
  {
    using _RM = typename _R::mask_type;
    return {__private_init, _RM::abi_type::_MaskImpl::template _S_convert<
			      typename _RM::simd_type::value_type>(__x)};
  }

template <typename _To, typename _Up, typename _Abi>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
  _To
  simd_bit_cast(const simd<_Up, _Abi>& __x)
  {
    using _Tp = typename _To::value_type;
    using _ToMember = typename _SimdTraits<_Tp, typename _To::abi_type>::_SimdMember;
    using _From = simd<_Up, _Abi>;
    using _FromMember = typename _SimdTraits<_Up, _Abi>::_SimdMember;
    // with concepts, the following should be constraints
    static_assert(sizeof(_To) == sizeof(_From));
    static_assert(is_trivially_copyable_v<_Tp> && is_trivially_copyable_v<_Up>);
    static_assert(is_trivially_copyable_v<_ToMember> && is_trivially_copyable_v<_FromMember>);
#if __has_builtin(__builtin_bit_cast)
    return {__private_init, __builtin_bit_cast(_ToMember, __data(__x))};
#else
    return {__private_init, __bit_cast<_ToMember>(__data(__x))};
#endif
  }

template <typename _To, typename _Up, typename _Abi>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
  _To
  simd_bit_cast(const simd_mask<_Up, _Abi>& __x)
  {
    using _From = simd_mask<_Up, _Abi>;
    static_assert(sizeof(_To) == sizeof(_From));
    static_assert(is_trivially_copyable_v<_From>);
    // _To can be simd<T, A>, specifically simd<T, fixed_size<N>> in which case _To is not trivially
    // copyable.
    if constexpr (is_simd_v<_To>)
      {
	using _Tp = typename _To::value_type;
	using _ToMember = typename _SimdTraits<_Tp, typename _To::abi_type>::_SimdMember;
	static_assert(is_trivially_copyable_v<_ToMember>);
#if __has_builtin(__builtin_bit_cast)
	return {__private_init, __builtin_bit_cast(_ToMember, __x)};
#else
	return {__private_init, __bit_cast<_ToMember>(__x)};
#endif
      }
    else
      {
	static_assert(is_trivially_copyable_v<_To>);
#if __has_builtin(__builtin_bit_cast)
	return __builtin_bit_cast(_To, __x);
#else
	return __bit_cast<_To>(__x);
#endif
      }
  }
} // namespace __proposed

// simd_cast {{{2
template <typename _Tp, typename _Up, typename _Ap,
	  typename _To = __value_type_or_identity_t<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR auto
  simd_cast(const simd<_ValuePreserving<_Up, _To>, _Ap>& __x)
    -> decltype(static_simd_cast<_Tp>(__x))
  { return static_simd_cast<_Tp>(__x); }

namespace __proposed {
template <typename _Tp, typename _Up, typename _Ap,
	  typename _To = __value_type_or_identity_t<_Tp>>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR auto
  simd_cast(const simd_mask<_ValuePreserving<_Up, _To>, _Ap>& __x)
    -> decltype(static_simd_cast<_Tp>(__x))
  { return static_simd_cast<_Tp>(__x); }
} // namespace __proposed

// }}}2
// resizing_simd_cast {{{
namespace __proposed {
/* Proposed spec:

template <class T, class U, class Abi>
T resizing_simd_cast(const simd<U, Abi>& x)

p1  Constraints:
    - is_simd_v<T> is true and
    - T::value_type is the same type as U

p2  Returns:
    A simd object with the i^th element initialized to x[i] for all i in the
    range of [0, min(T::size(), simd_size_v<U, Abi>)). If T::size() is larger
    than simd_size_v<U, Abi>, the remaining elements are value-initialized.

template <class T, class U, class Abi>
T resizing_simd_cast(const simd_mask<U, Abi>& x)

p1  Constraints: is_simd_mask_v<T> is true

p2  Returns:
    A simd_mask object with the i^th element initialized to x[i] for all i in
the range of [0, min(T::size(), simd_size_v<U, Abi>)). If T::size() is larger
    than simd_size_v<U, Abi>, the remaining elements are initialized to false.

 */

template <typename _Tp, typename _Up, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR enable_if_t<
  conjunction_v<is_simd<_Tp>, is_same<typename _Tp::value_type, _Up>>, _Tp>
  resizing_simd_cast(const simd<_Up, _Ap>& __x)
  {
    if constexpr (is_same_v<typename _Tp::abi_type, _Ap>)
      return __x;
    else if constexpr (simd_size_v<_Up, _Ap> == 1)
      {
	_Tp __r{};
	__r[0] = __x[0];
	return __r;
      }
    else if constexpr (_Tp::size() == 1)
      return __x[0];
    else if constexpr (sizeof(_Tp) == sizeof(__x)
		       && !__is_fixed_size_abi_v<_Ap>)
      return {__private_init,
	      __vector_bitcast<typename _Tp::value_type, _Tp::size()>(
		_Ap::_S_masked(__data(__x))._M_data)};
    else
      {
	_Tp __r{};
	__builtin_memcpy(&__data(__r), &__data(__x),
			 sizeof(_Up)
			   * std::min(_Tp::size(), simd_size_v<_Up, _Ap>));
	return __r;
      }
  }

template <typename _Tp, typename _Up, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
  enable_if_t<is_simd_mask_v<_Tp>, _Tp>
  resizing_simd_cast(const simd_mask<_Up, _Ap>& __x)
  {
    return {__private_init, _Tp::abi_type::_MaskImpl::template _S_convert<
			      typename _Tp::simd_type::value_type>(__x)};
  }
} // namespace __proposed

// }}}
// to_fixed_size {{{2
template <typename _Tp, int _Np>
  _GLIBCXX_SIMD_INTRINSIC fixed_size_simd<_Tp, _Np>
  to_fixed_size(const fixed_size_simd<_Tp, _Np>& __x)
  { return __x; }

template <typename _Tp, int _Np>
  _GLIBCXX_SIMD_INTRINSIC fixed_size_simd_mask<_Tp, _Np>
  to_fixed_size(const fixed_size_simd_mask<_Tp, _Np>& __x)
  { return __x; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC auto
  to_fixed_size(const simd<_Tp, _Ap>& __x)
  {
    return simd<_Tp, simd_abi::fixed_size<simd_size_v<_Tp, _Ap>>>([&__x](
      auto __i) constexpr { return __x[__i]; });
  }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC auto
  to_fixed_size(const simd_mask<_Tp, _Ap>& __x)
  {
    constexpr int _Np = simd_mask<_Tp, _Ap>::size();
    fixed_size_simd_mask<_Tp, _Np> __r;
    __execute_n_times<_Np>([&](auto __i) constexpr { __r[__i] = __x[__i]; });
    return __r;
  }

// to_native {{{2
template <typename _Tp, int _Np>
  _GLIBCXX_SIMD_INTRINSIC
  enable_if_t<(_Np == native_simd<_Tp>::size()), native_simd<_Tp>>
  to_native(const fixed_size_simd<_Tp, _Np>& __x)
  {
    alignas(memory_alignment_v<native_simd<_Tp>>) _Tp __mem[_Np];
    __x.copy_to(__mem, vector_aligned);
    return {__mem, vector_aligned};
  }

template <typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC
  enable_if_t<(_Np == native_simd_mask<_Tp>::size()), native_simd_mask<_Tp>>
  to_native(const fixed_size_simd_mask<_Tp, _Np>& __x)
  {
    return native_simd_mask<_Tp>([&](auto __i) constexpr { return __x[__i]; });
  }

// to_compatible {{{2
template <typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC enable_if_t<(_Np == simd<_Tp>::size()), simd<_Tp>>
  to_compatible(const simd<_Tp, simd_abi::fixed_size<_Np>>& __x)
  {
    alignas(memory_alignment_v<simd<_Tp>>) _Tp __mem[_Np];
    __x.copy_to(__mem, vector_aligned);
    return {__mem, vector_aligned};
  }

template <typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC
  enable_if_t<(_Np == simd_mask<_Tp>::size()), simd_mask<_Tp>>
  to_compatible(const simd_mask<_Tp, simd_abi::fixed_size<_Np>>& __x)
  { return simd_mask<_Tp>([&](auto __i) constexpr { return __x[__i]; }); }

// masked assignment [simd_mask.where] {{{1

// where_expression {{{1
// const_where_expression<M, T> {{{2
template <typename _M, typename _Tp>
  class const_where_expression
  {
    using _V = _Tp;
    static_assert(is_same_v<_V, __remove_cvref_t<_Tp>>);

    struct _Wrapper { using value_type = _V; };

  protected:
    using _Impl = typename _V::_Impl;

    using value_type =
      typename conditional_t<is_arithmetic_v<_V>, _Wrapper, _V>::value_type;

    _GLIBCXX_SIMD_INTRINSIC friend const _M&
    __get_mask(const const_where_expression& __x)
    { return __x._M_k; }

    _GLIBCXX_SIMD_INTRINSIC friend const _Tp&
    __get_lvalue(const const_where_expression& __x)
    { return __x._M_value; }

    const _M& _M_k;
    _Tp& _M_value;

  public:
    const_where_expression(const const_where_expression&) = delete;
    const_where_expression& operator=(const const_where_expression&) = delete;

    _GLIBCXX_SIMD_INTRINSIC const_where_expression(const _M& __kk, const _Tp& dd)
      : _M_k(__kk), _M_value(const_cast<_Tp&>(dd)) {}

    _GLIBCXX_SIMD_INTRINSIC _V
    operator-() const&&
    {
      return {__private_init,
	      _Impl::template _S_masked_unary<negate>(__data(_M_k),
						      __data(_M_value))};
    }

    template <typename _Up, typename _Flags>
      [[nodiscard]] _GLIBCXX_SIMD_INTRINSIC _V
      copy_from(const _LoadStorePtr<_Up, value_type>* __mem, _Flags) const&&
      {
	return {__private_init,
		_Impl::_S_masked_load(__data(_M_value), __data(_M_k),
				      _Flags::template _S_apply<_V>(__mem))};
      }

    template <typename _Up, typename _Flags>
      _GLIBCXX_SIMD_INTRINSIC void
      copy_to(_LoadStorePtr<_Up, value_type>* __mem, _Flags) const&&
      {
	_Impl::_S_masked_store(__data(_M_value),
			       _Flags::template _S_apply<_V>(__mem),
			       __data(_M_k));
      }
  };

// const_where_expression<bool, T> {{{2
template <typename _Tp>
  class const_where_expression<bool, _Tp>
  {
    using _M = bool;
    using _V = _Tp;

    static_assert(is_same_v<_V, __remove_cvref_t<_Tp>>);

    struct _Wrapper { using value_type = _V; };

  protected:
    using value_type =
      typename conditional_t<is_arithmetic_v<_V>, _Wrapper, _V>::value_type;

    _GLIBCXX_SIMD_INTRINSIC friend const _M&
    __get_mask(const const_where_expression& __x)
    { return __x._M_k; }

    _GLIBCXX_SIMD_INTRINSIC friend const _Tp&
    __get_lvalue(const const_where_expression& __x)
    { return __x._M_value; }

    const bool _M_k;
    _Tp& _M_value;

  public:
    const_where_expression(const const_where_expression&) = delete;
    const_where_expression& operator=(const const_where_expression&) = delete;

    _GLIBCXX_SIMD_INTRINSIC const_where_expression(const bool __kk, const _Tp& dd)
      : _M_k(__kk), _M_value(const_cast<_Tp&>(dd)) {}

    _GLIBCXX_SIMD_INTRINSIC _V operator-() const&&
    { return _M_k ? -_M_value : _M_value; }

    template <typename _Up, typename _Flags>
      [[nodiscard]] _GLIBCXX_SIMD_INTRINSIC _V
      copy_from(const _LoadStorePtr<_Up, value_type>* __mem, _Flags) const&&
      { return _M_k ? static_cast<_V>(__mem[0]) : _M_value; }

    template <typename _Up, typename _Flags>
      _GLIBCXX_SIMD_INTRINSIC void
      copy_to(_LoadStorePtr<_Up, value_type>* __mem, _Flags) const&&
      {
	if (_M_k)
	  __mem[0] = _M_value;
      }
  };

// where_expression<M, T> {{{2
template <typename _M, typename _Tp>
  class where_expression : public const_where_expression<_M, _Tp>
  {
    using _Impl = typename const_where_expression<_M, _Tp>::_Impl;

    static_assert(!is_const<_Tp>::value,
		  "where_expression may only be instantiated with __a non-const "
		  "_Tp parameter");

    using typename const_where_expression<_M, _Tp>::value_type;
    using const_where_expression<_M, _Tp>::_M_k;
    using const_where_expression<_M, _Tp>::_M_value;

    static_assert(
      is_same<typename _M::abi_type, typename _Tp::abi_type>::value, "");
    static_assert(_M::size() == _Tp::size(), "");

    _GLIBCXX_SIMD_INTRINSIC friend _Tp& __get_lvalue(where_expression& __x)
    { return __x._M_value; }

  public:
    where_expression(const where_expression&) = delete;
    where_expression& operator=(const where_expression&) = delete;

    _GLIBCXX_SIMD_INTRINSIC where_expression(const _M& __kk, _Tp& dd)
      : const_where_expression<_M, _Tp>(__kk, dd) {}

    template <typename _Up>
      _GLIBCXX_SIMD_INTRINSIC void operator=(_Up&& __x) &&
      {
	_Impl::_S_masked_assign(__data(_M_k), __data(_M_value),
				__to_value_type_or_member_type<_Tp>(
				  static_cast<_Up&&>(__x)));
      }

#define _GLIBCXX_SIMD_OP_(__op, __name)                                        \
  template <typename _Up>                                                      \
    _GLIBCXX_SIMD_INTRINSIC void operator __op##=(_Up&& __x)&&                 \
    {                                                                          \
      _Impl::template _S_masked_cassign(                                       \
	__data(_M_k), __data(_M_value),                                        \
	__to_value_type_or_member_type<_Tp>(static_cast<_Up&&>(__x)),          \
	[](auto __impl, auto __lhs, auto __rhs) constexpr {                    \
	return __impl.__name(__lhs, __rhs);                                    \
	});                                                                    \
    }                                                                          \
  static_assert(true)
    _GLIBCXX_SIMD_OP_(+, _S_plus);
    _GLIBCXX_SIMD_OP_(-, _S_minus);
    _GLIBCXX_SIMD_OP_(*, _S_multiplies);
    _GLIBCXX_SIMD_OP_(/, _S_divides);
    _GLIBCXX_SIMD_OP_(%, _S_modulus);
    _GLIBCXX_SIMD_OP_(&, _S_bit_and);
    _GLIBCXX_SIMD_OP_(|, _S_bit_or);
    _GLIBCXX_SIMD_OP_(^, _S_bit_xor);
    _GLIBCXX_SIMD_OP_(<<, _S_shift_left);
    _GLIBCXX_SIMD_OP_(>>, _S_shift_right);
#undef _GLIBCXX_SIMD_OP_

    _GLIBCXX_SIMD_INTRINSIC void operator++() &&
    {
      __data(_M_value)
	= _Impl::template _S_masked_unary<__increment>(__data(_M_k),
						       __data(_M_value));
    }

    _GLIBCXX_SIMD_INTRINSIC void operator++(int) &&
    {
      __data(_M_value)
	= _Impl::template _S_masked_unary<__increment>(__data(_M_k),
						       __data(_M_value));
    }

    _GLIBCXX_SIMD_INTRINSIC void operator--() &&
    {
      __data(_M_value)
	= _Impl::template _S_masked_unary<__decrement>(__data(_M_k),
						       __data(_M_value));
    }

    _GLIBCXX_SIMD_INTRINSIC void operator--(int) &&
    {
      __data(_M_value)
	= _Impl::template _S_masked_unary<__decrement>(__data(_M_k),
						       __data(_M_value));
    }

    // intentionally hides const_where_expression::copy_from
    template <typename _Up, typename _Flags>
      _GLIBCXX_SIMD_INTRINSIC void
      copy_from(const _LoadStorePtr<_Up, value_type>* __mem, _Flags) &&
      {
	__data(_M_value)
	  = _Impl::_S_masked_load(__data(_M_value), __data(_M_k),
				  _Flags::template _S_apply<_Tp>(__mem));
      }
  };

// where_expression<bool, T> {{{2
template <typename _Tp>
  class where_expression<bool, _Tp> : public const_where_expression<bool, _Tp>
  {
    using _M = bool;
    using typename const_where_expression<_M, _Tp>::value_type;
    using const_where_expression<_M, _Tp>::_M_k;
    using const_where_expression<_M, _Tp>::_M_value;

  public:
    where_expression(const where_expression&) = delete;
    where_expression& operator=(const where_expression&) = delete;

    _GLIBCXX_SIMD_INTRINSIC where_expression(const _M& __kk, _Tp& dd)
      : const_where_expression<_M, _Tp>(__kk, dd) {}

#define _GLIBCXX_SIMD_OP_(__op)                                                \
    template <typename _Up>                                                    \
      _GLIBCXX_SIMD_INTRINSIC void operator __op(_Up&& __x)&&                  \
      { if (_M_k) _M_value __op static_cast<_Up&&>(__x); }

    _GLIBCXX_SIMD_OP_(=)
    _GLIBCXX_SIMD_OP_(+=)
    _GLIBCXX_SIMD_OP_(-=)
    _GLIBCXX_SIMD_OP_(*=)
    _GLIBCXX_SIMD_OP_(/=)
    _GLIBCXX_SIMD_OP_(%=)
    _GLIBCXX_SIMD_OP_(&=)
    _GLIBCXX_SIMD_OP_(|=)
    _GLIBCXX_SIMD_OP_(^=)
    _GLIBCXX_SIMD_OP_(<<=)
    _GLIBCXX_SIMD_OP_(>>=)
  #undef _GLIBCXX_SIMD_OP_

    _GLIBCXX_SIMD_INTRINSIC void operator++() &&
    { if (_M_k) ++_M_value; }

    _GLIBCXX_SIMD_INTRINSIC void operator++(int) &&
    { if (_M_k) ++_M_value; }

    _GLIBCXX_SIMD_INTRINSIC void operator--() &&
    { if (_M_k) --_M_value; }

    _GLIBCXX_SIMD_INTRINSIC void operator--(int) &&
    { if (_M_k) --_M_value; }

    // intentionally hides const_where_expression::copy_from
    template <typename _Up, typename _Flags>
      _GLIBCXX_SIMD_INTRINSIC void
      copy_from(const _LoadStorePtr<_Up, value_type>* __mem, _Flags) &&
      { if (_M_k) _M_value = __mem[0]; }
  };

// where {{{1
template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC where_expression<simd_mask<_Tp, _Ap>, simd<_Tp, _Ap>>
  where(const typename simd<_Tp, _Ap>::mask_type& __k, simd<_Tp, _Ap>& __value)
  { return {__k, __value}; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC
    const_where_expression<simd_mask<_Tp, _Ap>, simd<_Tp, _Ap>>
    where(const typename simd<_Tp, _Ap>::mask_type& __k,
	  const simd<_Tp, _Ap>& __value)
  { return {__k, __value}; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC
    where_expression<simd_mask<_Tp, _Ap>, simd_mask<_Tp, _Ap>>
    where(const remove_const_t<simd_mask<_Tp, _Ap>>& __k,
	  simd_mask<_Tp, _Ap>& __value)
  { return {__k, __value}; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC
    const_where_expression<simd_mask<_Tp, _Ap>, simd_mask<_Tp, _Ap>>
    where(const remove_const_t<simd_mask<_Tp, _Ap>>& __k,
	  const simd_mask<_Tp, _Ap>& __value)
  { return {__k, __value}; }

template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC where_expression<bool, _Tp>
  where(_ExactBool __k, _Tp& __value)
  { return {__k, __value}; }

template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC const_where_expression<bool, _Tp>
  where(_ExactBool __k, const _Tp& __value)
  { return {__k, __value}; }

  template <typename _Tp, typename _Ap>
    void where(bool __k, simd<_Tp, _Ap>& __value) = delete;

  template <typename _Tp, typename _Ap>
    void where(bool __k, const simd<_Tp, _Ap>& __value) = delete;

// proposed mask iterations {{{1
namespace __proposed {
template <size_t _Np>
  class where_range
  {
    const bitset<_Np> __bits;

  public:
    where_range(bitset<_Np> __b) : __bits(__b) {}

    class iterator
    {
      size_t __mask;
      size_t __bit;

      _GLIBCXX_SIMD_INTRINSIC void __next_bit()
      { __bit = __builtin_ctzl(__mask); }

      _GLIBCXX_SIMD_INTRINSIC void __reset_lsb()
      {
	// 01100100 - 1 = 01100011
	__mask &= (__mask - 1);
	// __asm__("btr %1,%0" : "+r"(__mask) : "r"(__bit));
      }

    public:
      iterator(decltype(__mask) __m) : __mask(__m) { __next_bit(); }
      iterator(const iterator&) = default;
      iterator(iterator&&) = default;

      _GLIBCXX_SIMD_ALWAYS_INLINE size_t operator->() const
      { return __bit; }

      _GLIBCXX_SIMD_ALWAYS_INLINE size_t operator*() const
      { return __bit; }

      _GLIBCXX_SIMD_ALWAYS_INLINE iterator& operator++()
      {
	__reset_lsb();
	__next_bit();
	return *this;
      }

      _GLIBCXX_SIMD_ALWAYS_INLINE iterator operator++(int)
      {
	iterator __tmp = *this;
	__reset_lsb();
	__next_bit();
	return __tmp;
      }

      _GLIBCXX_SIMD_ALWAYS_INLINE bool operator==(const iterator& __rhs) const
      { return __mask == __rhs.__mask; }

      _GLIBCXX_SIMD_ALWAYS_INLINE bool operator!=(const iterator& __rhs) const
      { return __mask != __rhs.__mask; }
    };

    iterator begin() const
    { return __bits.to_ullong(); }

    iterator end() const
    { return 0; }
  };

template <typename _Tp, typename _Ap>
  where_range<simd_size_v<_Tp, _Ap>>
  where(const simd_mask<_Tp, _Ap>& __k)
  { return __k.__to_bitset(); }

} // namespace __proposed

// }}}1
// reductions [simd.reductions] {{{1
template <typename _Tp, typename _Abi, typename _BinaryOperation = plus<>>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR _Tp
  reduce(const simd<_Tp, _Abi>& __v,
	 _BinaryOperation __binary_op = _BinaryOperation())
  { return _Abi::_SimdImpl::_S_reduce(__v, __binary_op); }

template <typename _M, typename _V, typename _BinaryOperation = plus<>>
  _GLIBCXX_SIMD_INTRINSIC typename _V::value_type
  reduce(const const_where_expression<_M, _V>& __x,
	 typename _V::value_type __identity_element,
	 _BinaryOperation __binary_op)
  {
    if (__builtin_expect(none_of(__get_mask(__x)), false))
      return __identity_element;

    _V __tmp = __identity_element;
    _V::_Impl::_S_masked_assign(__data(__get_mask(__x)), __data(__tmp),
				__data(__get_lvalue(__x)));
    return reduce(__tmp, __binary_op);
  }

template <typename _M, typename _V>
  _GLIBCXX_SIMD_INTRINSIC typename _V::value_type
  reduce(const const_where_expression<_M, _V>& __x, plus<> __binary_op = {})
  { return reduce(__x, 0, __binary_op); }

template <typename _M, typename _V>
  _GLIBCXX_SIMD_INTRINSIC typename _V::value_type
  reduce(const const_where_expression<_M, _V>& __x, multiplies<> __binary_op)
  { return reduce(__x, 1, __binary_op); }

template <typename _M, typename _V>
  _GLIBCXX_SIMD_INTRINSIC typename _V::value_type
  reduce(const const_where_expression<_M, _V>& __x, bit_and<> __binary_op)
  { return reduce(__x, ~typename _V::value_type(), __binary_op); }

template <typename _M, typename _V>
  _GLIBCXX_SIMD_INTRINSIC typename _V::value_type
  reduce(const const_where_expression<_M, _V>& __x, bit_or<> __binary_op)
  { return reduce(__x, 0, __binary_op); }

template <typename _M, typename _V>
  _GLIBCXX_SIMD_INTRINSIC typename _V::value_type
  reduce(const const_where_expression<_M, _V>& __x, bit_xor<> __binary_op)
  { return reduce(__x, 0, __binary_op); }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR _Tp
  hmin(const simd<_Tp, _Abi>& __v) noexcept
  {
    return _Abi::_SimdImpl::_S_reduce(__v, __detail::_Minimum());
  }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR _Tp
  hmax(const simd<_Tp, _Abi>& __v) noexcept
  {
    return _Abi::_SimdImpl::_S_reduce(__v, __detail::_Maximum());
  }

template <typename _M, typename _V>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
  typename _V::value_type
  hmin(const const_where_expression<_M, _V>& __x) noexcept
  {
    using _Tp = typename _V::value_type;
    constexpr _Tp __id_elem =
#ifdef __FINITE_MATH_ONLY__
      __finite_max_v<_Tp>;
#else
      __value_or<__infinity, _Tp>(__finite_max_v<_Tp>);
#endif
    _V __tmp = __id_elem;
    _V::_Impl::_S_masked_assign(__data(__get_mask(__x)), __data(__tmp),
				__data(__get_lvalue(__x)));
    return _V::abi_type::_SimdImpl::_S_reduce(__tmp, __detail::_Minimum());
  }

template <typename _M, typename _V>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
  typename _V::value_type
  hmax(const const_where_expression<_M, _V>& __x) noexcept
  {
    using _Tp = typename _V::value_type;
    constexpr _Tp __id_elem =
#ifdef __FINITE_MATH_ONLY__
      __finite_min_v<_Tp>;
#else
      [] {
	if constexpr (__value_exists_v<__infinity, _Tp>)
	  return -__infinity_v<_Tp>;
	else
	  return __finite_min_v<_Tp>;
      }();
#endif
    _V __tmp = __id_elem;
    _V::_Impl::_S_masked_assign(__data(__get_mask(__x)), __data(__tmp),
				__data(__get_lvalue(__x)));
    return _V::abi_type::_SimdImpl::_S_reduce(__tmp, __detail::_Maximum());
  }

// }}}1
// algorithms [simd.alg] {{{
template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR simd<_Tp, _Ap>
  min(const simd<_Tp, _Ap>& __a, const simd<_Tp, _Ap>& __b)
  { return {__private_init, _Ap::_SimdImpl::_S_min(__data(__a), __data(__b))}; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR simd<_Tp, _Ap>
  max(const simd<_Tp, _Ap>& __a, const simd<_Tp, _Ap>& __b)
  { return {__private_init, _Ap::_SimdImpl::_S_max(__data(__a), __data(__b))}; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
  pair<simd<_Tp, _Ap>, simd<_Tp, _Ap>>
  minmax(const simd<_Tp, _Ap>& __a, const simd<_Tp, _Ap>& __b)
  {
    const auto pair_of_members
      = _Ap::_SimdImpl::_S_minmax(__data(__a), __data(__b));
    return {simd<_Tp, _Ap>(__private_init, pair_of_members.first),
	    simd<_Tp, _Ap>(__private_init, pair_of_members.second)};
  }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR simd<_Tp, _Ap>
  clamp(const simd<_Tp, _Ap>& __v, const simd<_Tp, _Ap>& __lo,
	const simd<_Tp, _Ap>& __hi)
  {
    using _Impl = typename _Ap::_SimdImpl;
    return {__private_init,
	    _Impl::_S_min(__data(__hi),
			  _Impl::_S_max(__data(__lo), __data(__v)))};
  }

// }}}

template <size_t... _Sizes, typename _Tp, typename _Ap,
	  typename = enable_if_t<((_Sizes + ...) == simd<_Tp, _Ap>::size())>>
  inline tuple<simd<_Tp, simd_abi::deduce_t<_Tp, _Sizes>>...>
  split(const simd<_Tp, _Ap>&);

// __extract_part {{{
template <int _Index, int _Total, int _Combine = 1, typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_CONST
  _SimdWrapper<_Tp, _Np / _Total * _Combine>
  __extract_part(const _SimdWrapper<_Tp, _Np> __x);

template <int Index, int Parts, int _Combine = 1, typename _Tp, typename _A0,
	  typename... _As>
  _GLIBCXX_SIMD_INTRINSIC auto
  __extract_part(const _SimdTuple<_Tp, _A0, _As...>& __x);

// }}}
// _SizeList {{{
template <size_t _V0, size_t... _Values>
  struct _SizeList
  {
    template <size_t _I>
      static constexpr size_t _S_at(_SizeConstant<_I> = {})
      {
	if constexpr (_I == 0)
	  return _V0;
	else
	  return _SizeList<_Values...>::template _S_at<_I - 1>();
      }

    template <size_t _I>
      static constexpr auto _S_before(_SizeConstant<_I> = {})
      {
	if constexpr (_I == 0)
	  return _SizeConstant<0>();
	else
	  return _SizeConstant<
	    _V0 + _SizeList<_Values...>::template _S_before<_I - 1>()>();
      }

    template <size_t _Np>
      static constexpr auto _S_pop_front(_SizeConstant<_Np> = {})
      {
	if constexpr (_Np == 0)
	  return _SizeList();
	else
	  return _SizeList<_Values...>::template _S_pop_front<_Np - 1>();
      }
  };

// }}}
// __extract_center {{{
template <typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC _SimdWrapper<_Tp, _Np / 2>
  __extract_center(_SimdWrapper<_Tp, _Np> __x)
  {
    static_assert(_Np >= 4);
    static_assert(_Np % 4 == 0); // x0 - x1 - x2 - x3 -> return {x1, x2}
#if _GLIBCXX_SIMD_X86INTRIN    // {{{
    if constexpr (__have_avx512f && sizeof(_Tp) * _Np == 64)
      {
	const auto __intrin = __to_intrin(__x);
	if constexpr (is_integral_v<_Tp>)
	  return __vector_bitcast<_Tp>(_mm512_castsi512_si256(
	    _mm512_shuffle_i32x4(__intrin, __intrin,
				 1 + 2 * 0x4 + 2 * 0x10 + 3 * 0x40)));
	else if constexpr (sizeof(_Tp) == 4)
	  return __vector_bitcast<_Tp>(_mm512_castps512_ps256(
	    _mm512_shuffle_f32x4(__intrin, __intrin,
				 1 + 2 * 0x4 + 2 * 0x10 + 3 * 0x40)));
	else if constexpr (sizeof(_Tp) == 8)
	  return __vector_bitcast<_Tp>(_mm512_castpd512_pd256(
	    _mm512_shuffle_f64x2(__intrin, __intrin,
				 1 + 2 * 0x4 + 2 * 0x10 + 3 * 0x40)));
	else
	  __assert_unreachable<_Tp>();
      }
    else if constexpr (sizeof(_Tp) * _Np == 32 && is_floating_point_v<_Tp>)
      return __vector_bitcast<_Tp>(
	_mm_shuffle_pd(__lo128(__vector_bitcast<double>(__x)),
		       __hi128(__vector_bitcast<double>(__x)), 1));
    else if constexpr (sizeof(__x) == 32 && sizeof(_Tp) * _Np <= 32)
      return __vector_bitcast<_Tp>(
	_mm_alignr_epi8(__hi128(__vector_bitcast<_LLong>(__x)),
			__lo128(__vector_bitcast<_LLong>(__x)),
			sizeof(_Tp) * _Np / 4));
    else
#endif // _GLIBCXX_SIMD_X86INTRIN }}}
      {
	__vector_type_t<_Tp, _Np / 2> __r;
	__builtin_memcpy(&__r,
			 reinterpret_cast<const char*>(&__x)
			   + sizeof(_Tp) * _Np / 4,
			 sizeof(_Tp) * _Np / 2);
	return __r;
      }
  }

template <typename _Tp, typename _A0, typename... _As>
  _GLIBCXX_SIMD_INTRINSIC
  _SimdWrapper<_Tp, _SimdTuple<_Tp, _A0, _As...>::_S_size() / 2>
  __extract_center(const _SimdTuple<_Tp, _A0, _As...>& __x)
  {
    if constexpr (sizeof...(_As) == 0)
      return __extract_center(__x.first);
    else
      return __extract_part<1, 4, 2>(__x);
  }

// }}}
// __split_wrapper {{{
template <size_t... _Sizes, typename _Tp, typename... _As>
  auto
  __split_wrapper(_SizeList<_Sizes...>, const _SimdTuple<_Tp, _As...>& __x)
  {
    return split<_Sizes...>(
      fixed_size_simd<_Tp, _SimdTuple<_Tp, _As...>::_S_size()>(__private_init,
							       __x));
  }

// }}}

// split<simd>(simd) {{{
template <typename _V, typename _Ap,
	  size_t Parts = simd_size_v<typename _V::value_type, _Ap> / _V::size()>
  enable_if_t<simd_size_v<typename _V::value_type, _Ap> == Parts * _V::size()
	      && is_simd_v<_V>, array<_V, Parts>>
  split(const simd<typename _V::value_type, _Ap>& __x)
  {
    using _Tp = typename _V::value_type;
    if constexpr (Parts == 1)
      {
	return {simd_cast<_V>(__x)};
      }
    else if (__x._M_is_constprop())
      {
	return __generate_from_n_evaluations<Parts, array<_V, Parts>>([&](
	  auto __i) constexpr {
	  return _V([&](auto __j) constexpr {
	    return __x[__i * _V::size() + __j];
	  });
	});
      }
    else if constexpr (
      __is_fixed_size_abi_v<_Ap>
      && (is_same_v<typename _V::abi_type, simd_abi::scalar>
	|| (__is_fixed_size_abi_v<typename _V::abi_type>
	  && sizeof(_V) == sizeof(_Tp) * _V::size() // _V doesn't have padding
	  )))
      {
	// fixed_size -> fixed_size (w/o padding) or scalar
#ifdef _GLIBCXX_SIMD_USE_ALIASING_LOADS
      const __may_alias<_Tp>* const __element_ptr
	= reinterpret_cast<const __may_alias<_Tp>*>(&__data(__x));
      return __generate_from_n_evaluations<Parts, array<_V, Parts>>([&](
	auto __i) constexpr {
	return _V(__element_ptr + __i * _V::size(), vector_aligned);
      });
#else
      const auto& __xx = __data(__x);
      return __generate_from_n_evaluations<Parts, array<_V, Parts>>([&](
	auto __i) constexpr {
	[[maybe_unused]] constexpr size_t __offset
	  = decltype(__i)::value * _V::size();
	return _V([&](auto __j) constexpr {
	  constexpr _SizeConstant<__j + __offset> __k;
	  return __xx[__k];
	});
      });
#endif
    }
  else if constexpr (is_same_v<typename _V::abi_type, simd_abi::scalar>)
    {
      // normally memcpy should work here as well
      return __generate_from_n_evaluations<Parts, array<_V, Parts>>([&](
	auto __i) constexpr { return __x[__i]; });
    }
  else
    {
      return __generate_from_n_evaluations<Parts, array<_V, Parts>>([&](
	auto __i) constexpr {
	if constexpr (__is_fixed_size_abi_v<typename _V::abi_type>)
	  return _V([&](auto __j) constexpr {
	    return __x[__i * _V::size() + __j];
	  });
	else
	  return _V(__private_init,
		    __extract_part<decltype(__i)::value, Parts>(__data(__x)));
      });
    }
  }

// }}}
// split<simd_mask>(simd_mask) {{{
template <typename _V, typename _Ap,
	  size_t _Parts
	  = simd_size_v<typename _V::simd_type::value_type, _Ap> / _V::size()>
  enable_if_t<is_simd_mask_v<_V> && simd_size_v<typename
    _V::simd_type::value_type, _Ap> == _Parts * _V::size(), array<_V, _Parts>>
  split(const simd_mask<typename _V::simd_type::value_type, _Ap>& __x)
  {
    if constexpr (is_same_v<_Ap, typename _V::abi_type>)
      return {__x};
    else if constexpr (_Parts == 1)
      return {__proposed::static_simd_cast<_V>(__x)};
    else if constexpr (_Parts == 2 && __is_sse_abi<typename _V::abi_type>()
		       && __is_avx_abi<_Ap>())
      return {_V(__private_init, __lo128(__data(__x))),
	      _V(__private_init, __hi128(__data(__x)))};
    else if constexpr (_V::size() <= __CHAR_BIT__ * sizeof(_ULLong))
      {
	const bitset __bits = __x.__to_bitset();
	return __generate_from_n_evaluations<_Parts, array<_V, _Parts>>([&](
	  auto __i) constexpr {
	  constexpr size_t __offset = __i * _V::size();
	  return _V(__bitset_init, (__bits >> __offset).to_ullong());
	});
      }
    else
      {
	return __generate_from_n_evaluations<_Parts, array<_V, _Parts>>([&](
	  auto __i) constexpr {
	  constexpr size_t __offset = __i * _V::size();
	  return _V(
	    __private_init, [&](auto __j) constexpr {
	      return __x[__j + __offset];
	    });
	});
      }
  }

// }}}
// split<_Sizes...>(simd) {{{
template <size_t... _Sizes, typename _Tp, typename _Ap, typename>
  _GLIBCXX_SIMD_ALWAYS_INLINE
  tuple<simd<_Tp, simd_abi::deduce_t<_Tp, _Sizes>>...>
  split(const simd<_Tp, _Ap>& __x)
  {
    using _SL = _SizeList<_Sizes...>;
    using _Tuple = tuple<__deduced_simd<_Tp, _Sizes>...>;
    constexpr size_t _Np = simd_size_v<_Tp, _Ap>;
    constexpr size_t _N0 = _SL::template _S_at<0>();
    using _V = __deduced_simd<_Tp, _N0>;

    if (__x._M_is_constprop())
      return __generate_from_n_evaluations<sizeof...(_Sizes), _Tuple>([&](
	auto __i) constexpr {
	using _Vi = __deduced_simd<_Tp, _SL::_S_at(__i)>;
	constexpr size_t __offset = _SL::_S_before(__i);
	return _Vi([&](auto __j) constexpr { return __x[__offset + __j]; });
      });
    else if constexpr (_Np == _N0)
      {
	static_assert(sizeof...(_Sizes) == 1);
	return {simd_cast<_V>(__x)};
      }
    else if constexpr // split from fixed_size, such that __x::first.size == _N0
      (__is_fixed_size_abi_v<
	 _Ap> && __fixed_size_storage_t<_Tp, _Np>::_S_first_size == _N0)
      {
	static_assert(
	  !__is_fixed_size_abi_v<typename _V::abi_type>,
	  "How can <_Tp, _Np> be __a single _SimdTuple entry but __a "
	  "fixed_size_simd "
	  "when deduced?");
	// extract first and recurse (__split_wrapper is needed to deduce a new
	// _Sizes pack)
	return tuple_cat(make_tuple(_V(__private_init, __data(__x).first)),
			 __split_wrapper(_SL::template _S_pop_front<1>(),
					 __data(__x).second));
      }
    else if constexpr ((!is_same_v<simd_abi::scalar,
				   simd_abi::deduce_t<_Tp, _Sizes>> && ...)
		       && (!__is_fixed_size_abi_v<
			     simd_abi::deduce_t<_Tp, _Sizes>> && ...))
      {
	if constexpr (((_Sizes * 2 == _Np) && ...))
	  return {{__private_init, __extract_part<0, 2>(__data(__x))},
		  {__private_init, __extract_part<1, 2>(__data(__x))}};
	else if constexpr (is_same_v<_SizeList<_Sizes...>,
				     _SizeList<_Np / 3, _Np / 3, _Np / 3>>)
	  return {{__private_init, __extract_part<0, 3>(__data(__x))},
		  {__private_init, __extract_part<1, 3>(__data(__x))},
		  {__private_init, __extract_part<2, 3>(__data(__x))}};
	else if constexpr (is_same_v<_SizeList<_Sizes...>,
				     _SizeList<2 * _Np / 3, _Np / 3>>)
	  return {{__private_init, __extract_part<0, 3, 2>(__data(__x))},
		  {__private_init, __extract_part<2, 3>(__data(__x))}};
	else if constexpr (is_same_v<_SizeList<_Sizes...>,
				     _SizeList<_Np / 3, 2 * _Np / 3>>)
	  return {{__private_init, __extract_part<0, 3>(__data(__x))},
		  {__private_init, __extract_part<1, 3, 2>(__data(__x))}};
	else if constexpr (is_same_v<_SizeList<_Sizes...>,
				     _SizeList<_Np / 2, _Np / 4, _Np / 4>>)
	  return {{__private_init, __extract_part<0, 2>(__data(__x))},
		  {__private_init, __extract_part<2, 4>(__data(__x))},
		  {__private_init, __extract_part<3, 4>(__data(__x))}};
	else if constexpr (is_same_v<_SizeList<_Sizes...>,
				     _SizeList<_Np / 4, _Np / 4, _Np / 2>>)
	  return {{__private_init, __extract_part<0, 4>(__data(__x))},
		  {__private_init, __extract_part<1, 4>(__data(__x))},
		  {__private_init, __extract_part<1, 2>(__data(__x))}};
	else if constexpr (is_same_v<_SizeList<_Sizes...>,
				     _SizeList<_Np / 4, _Np / 2, _Np / 4>>)
	  return {{__private_init, __extract_part<0, 4>(__data(__x))},
		  {__private_init, __extract_center(__data(__x))},
		  {__private_init, __extract_part<3, 4>(__data(__x))}};
	else if constexpr (((_Sizes * 4 == _Np) && ...))
	  return {{__private_init, __extract_part<0, 4>(__data(__x))},
		  {__private_init, __extract_part<1, 4>(__data(__x))},
		  {__private_init, __extract_part<2, 4>(__data(__x))},
		  {__private_init, __extract_part<3, 4>(__data(__x))}};
	// else fall through
      }
#ifdef _GLIBCXX_SIMD_USE_ALIASING_LOADS
    const __may_alias<_Tp>* const __element_ptr
      = reinterpret_cast<const __may_alias<_Tp>*>(&__x);
    return __generate_from_n_evaluations<sizeof...(_Sizes), _Tuple>([&](
      auto __i) constexpr {
      using _Vi = __deduced_simd<_Tp, _SL::_S_at(__i)>;
      constexpr size_t __offset = _SL::_S_before(__i);
      constexpr size_t __base_align = alignof(simd<_Tp, _Ap>);
      constexpr size_t __a
	= __base_align - ((__offset * sizeof(_Tp)) % __base_align);
      constexpr size_t __b = ((__a - 1) & __a) ^ __a;
      constexpr size_t __alignment = __b == 0 ? __a : __b;
      return _Vi(__element_ptr + __offset, overaligned<__alignment>);
    });
#else
    return __generate_from_n_evaluations<sizeof...(_Sizes), _Tuple>([&](
      auto __i) constexpr {
      using _Vi = __deduced_simd<_Tp, _SL::_S_at(__i)>;
      const auto& __xx = __data(__x);
      using _Offset = decltype(_SL::_S_before(__i));
      return _Vi([&](auto __j) constexpr {
	constexpr _SizeConstant<_Offset::value + __j> __k;
	return __xx[__k];
      });
    });
#endif
  }

// }}}

// __subscript_in_pack {{{
template <size_t _I, typename _Tp, typename _Ap, typename... _As>
  _GLIBCXX_SIMD_INTRINSIC constexpr _Tp
  __subscript_in_pack(const simd<_Tp, _Ap>& __x, const simd<_Tp, _As>&... __xs)
  {
    if constexpr (_I < simd_size_v<_Tp, _Ap>)
      return __x[_I];
    else
      return __subscript_in_pack<_I - simd_size_v<_Tp, _Ap>>(__xs...);
  }

// }}}
// __store_pack_of_simd {{{
template <typename _Tp, typename _A0, typename... _As>
  _GLIBCXX_SIMD_INTRINSIC void
  __store_pack_of_simd(char* __mem, const simd<_Tp, _A0>& __x0,
		       const simd<_Tp, _As>&... __xs)
  {
    constexpr size_t __n_bytes = sizeof(_Tp) * simd_size_v<_Tp, _A0>;
    __builtin_memcpy(__mem, &__data(__x0), __n_bytes);
    if constexpr (sizeof...(__xs) > 0)
      __store_pack_of_simd(__mem + __n_bytes, __xs...);
  }

// }}}
// concat(simd...) {{{
template <typename _Tp, typename... _As, typename = __detail::__odr_helper>
  inline _GLIBCXX_SIMD_CONSTEXPR
  simd<_Tp, simd_abi::deduce_t<_Tp, (simd_size_v<_Tp, _As> + ...)>>
  concat(const simd<_Tp, _As>&... __xs)
  {
    using _Rp = __deduced_simd<_Tp, (simd_size_v<_Tp, _As> + ...)>;
    if constexpr (sizeof...(__xs) == 1)
      return simd_cast<_Rp>(__xs...);
    else if ((... && __xs._M_is_constprop()))
      return simd<_Tp,
		  simd_abi::deduce_t<_Tp, (simd_size_v<_Tp, _As> + ...)>>([&](
	auto __i) constexpr { return __subscript_in_pack<__i>(__xs...); });
    else
      {
	_Rp __r{};
	__store_pack_of_simd(reinterpret_cast<char*>(&__data(__r)), __xs...);
	return __r;
      }
  }

// }}}
// concat(array<simd>) {{{
template <typename _Tp, typename _Abi, size_t _Np>
  _GLIBCXX_SIMD_ALWAYS_INLINE
  _GLIBCXX_SIMD_CONSTEXPR __deduced_simd<_Tp, simd_size_v<_Tp, _Abi> * _Np>
  concat(const array<simd<_Tp, _Abi>, _Np>& __x)
  {
    return __call_with_subscripts<_Np>(__x, [](const auto&... __xs) {
      return concat(__xs...);
    });
  }

// }}}

/// @cond undocumented
// _SmartReference {{{
template <typename _Up, typename _Accessor = _Up,
	  typename _ValueType = typename _Up::value_type>
  class _SmartReference
  {
    friend _Accessor;
    int _M_index;
    _Up& _M_obj;

    _GLIBCXX_SIMD_INTRINSIC constexpr _ValueType _M_read() const noexcept
    {
      if constexpr (is_arithmetic_v<_Up>)
	return _M_obj;
      else
	return _M_obj[_M_index];
    }

    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC constexpr void _M_write(_Tp&& __x) const
      { _Accessor::_S_set(_M_obj, _M_index, static_cast<_Tp&&>(__x)); }

  public:
    _GLIBCXX_SIMD_INTRINSIC constexpr
    _SmartReference(_Up& __o, int __i) noexcept
    : _M_index(__i), _M_obj(__o) {}

    using value_type = _ValueType;

    _GLIBCXX_SIMD_INTRINSIC _SmartReference(const _SmartReference&) = delete;

    _GLIBCXX_SIMD_INTRINSIC constexpr operator value_type() const noexcept
    { return _M_read(); }

    template <typename _Tp,
	      typename
	      = _ValuePreservingOrInt<__remove_cvref_t<_Tp>, value_type>>
      _GLIBCXX_SIMD_INTRINSIC constexpr _SmartReference operator=(_Tp&& __x) &&
      {
	_M_write(static_cast<_Tp&&>(__x));
	return {_M_obj, _M_index};
      }

#define _GLIBCXX_SIMD_OP_(__op)                                                \
    template <typename _Tp,                                                    \
	      typename _TT                                                     \
	      = decltype(declval<value_type>() __op declval<_Tp>()),           \
	      typename = _ValuePreservingOrInt<__remove_cvref_t<_Tp>, _TT>,    \
	      typename = _ValuePreservingOrInt<_TT, value_type>>               \
      _GLIBCXX_SIMD_INTRINSIC constexpr _SmartReference                        \
      operator __op##=(_Tp&& __x) &&                                           \
      {                                                                        \
	const value_type& __lhs = _M_read();                                   \
	_M_write(__lhs __op __x);                                              \
	return {_M_obj, _M_index};                                             \
      }
    _GLIBCXX_SIMD_ALL_ARITHMETICS(_GLIBCXX_SIMD_OP_);
    _GLIBCXX_SIMD_ALL_SHIFTS(_GLIBCXX_SIMD_OP_);
    _GLIBCXX_SIMD_ALL_BINARY(_GLIBCXX_SIMD_OP_);
#undef _GLIBCXX_SIMD_OP_

    template <typename _Tp = void,
	      typename
	      = decltype(++declval<conditional_t<true, value_type, _Tp>&>())>
      _GLIBCXX_SIMD_INTRINSIC constexpr _SmartReference operator++() &&
      {
	value_type __x = _M_read();
	_M_write(++__x);
	return {_M_obj, _M_index};
      }

    template <typename _Tp = void,
	      typename
	      = decltype(declval<conditional_t<true, value_type, _Tp>&>()++)>
      _GLIBCXX_SIMD_INTRINSIC constexpr value_type operator++(int) &&
      {
	const value_type __r = _M_read();
	value_type __x = __r;
	_M_write(++__x);
	return __r;
      }

    template <typename _Tp = void,
	      typename
	      = decltype(--declval<conditional_t<true, value_type, _Tp>&>())>
      _GLIBCXX_SIMD_INTRINSIC constexpr _SmartReference operator--() &&
      {
	value_type __x = _M_read();
	_M_write(--__x);
	return {_M_obj, _M_index};
      }

    template <typename _Tp = void,
	      typename
	      = decltype(declval<conditional_t<true, value_type, _Tp>&>()--)>
      _GLIBCXX_SIMD_INTRINSIC constexpr value_type operator--(int) &&
      {
	const value_type __r = _M_read();
	value_type __x = __r;
	_M_write(--__x);
	return __r;
      }

    _GLIBCXX_SIMD_INTRINSIC friend void
    swap(_SmartReference&& __a, _SmartReference&& __b) noexcept(
      conjunction<
	is_nothrow_constructible<value_type, _SmartReference&&>,
	is_nothrow_assignable<_SmartReference&&, value_type&&>>::value)
    {
      value_type __tmp = static_cast<_SmartReference&&>(__a);
      static_cast<_SmartReference&&>(__a) = static_cast<value_type>(__b);
      static_cast<_SmartReference&&>(__b) = std::move(__tmp);
    }

    _GLIBCXX_SIMD_INTRINSIC friend void
    swap(value_type& __a, _SmartReference&& __b) noexcept(
      conjunction<
	is_nothrow_constructible<value_type, value_type&&>,
	is_nothrow_assignable<value_type&, value_type&&>,
	is_nothrow_assignable<_SmartReference&&, value_type&&>>::value)
    {
      value_type __tmp(std::move(__a));
      __a = static_cast<value_type>(__b);
      static_cast<_SmartReference&&>(__b) = std::move(__tmp);
    }

    _GLIBCXX_SIMD_INTRINSIC friend void
    swap(_SmartReference&& __a, value_type& __b) noexcept(
      conjunction<
	is_nothrow_constructible<value_type, _SmartReference&&>,
	is_nothrow_assignable<value_type&, value_type&&>,
	is_nothrow_assignable<_SmartReference&&, value_type&&>>::value)
    {
      value_type __tmp(__a);
      static_cast<_SmartReference&&>(__a) = std::move(__b);
      __b = std::move(__tmp);
    }
  };

// }}}
// __scalar_abi_wrapper {{{
template <int _Bytes>
  struct __scalar_abi_wrapper
  {
    template <typename _Tp> static constexpr size_t _S_full_size = 1;
    template <typename _Tp> static constexpr size_t _S_size = 1;
    template <typename _Tp> static constexpr size_t _S_is_partial = false;

    template <typename _Tp, typename _Abi = simd_abi::scalar>
      static constexpr bool _S_is_valid_v
	= _Abi::template _IsValid<_Tp>::value && sizeof(_Tp) == _Bytes;
  };

// }}}
// __decay_abi metafunction {{{
template <typename _Tp>
  struct __decay_abi { using type = _Tp; };

template <int _Bytes>
  struct __decay_abi<__scalar_abi_wrapper<_Bytes>>
  { using type = simd_abi::scalar; };

// }}}
// __find_next_valid_abi metafunction {{{1
// Given an ABI tag A<N>, find an N2 < N such that A<N2>::_S_is_valid_v<_Tp> ==
// true, N2 is a power-of-2, and A<N2>::_S_is_partial<_Tp> is false. Break
// recursion at 2 elements in the resulting ABI tag. In this case
// type::_S_is_valid_v<_Tp> may be false.
template <template <int> class _Abi, int _Bytes, typename _Tp>
  struct __find_next_valid_abi
  {
    static constexpr auto _S_choose()
    {
      constexpr int _NextBytes = std::__bit_ceil(_Bytes) / 2;
      using _NextAbi = _Abi<_NextBytes>;
      if constexpr (_NextBytes < sizeof(_Tp) * 2) // break recursion
	return _Abi<_Bytes>();
      else if constexpr (_NextAbi::template _S_is_partial<_Tp> == false
			 && _NextAbi::template _S_is_valid_v<_Tp>)
	return _NextAbi();
      else
	return __find_next_valid_abi<_Abi, _NextBytes, _Tp>::_S_choose();
    }

    using type = decltype(_S_choose());
  };

template <int _Bytes, typename _Tp>
  struct __find_next_valid_abi<__scalar_abi_wrapper, _Bytes, _Tp>
  { using type = simd_abi::scalar; };

// _AbiList {{{1
template <template <int> class...>
  struct _AbiList
  {
    template <typename, int> static constexpr bool _S_has_valid_abi = false;
    template <typename, int> using _FirstValidAbi = void;
    template <typename, int> using _BestAbi = void;
  };

template <template <int> class _A0, template <int> class... _Rest>
  struct _AbiList<_A0, _Rest...>
  {
    template <typename _Tp, int _Np>
      static constexpr bool _S_has_valid_abi
	= _A0<sizeof(_Tp) * _Np>::template _S_is_valid_v<
	    _Tp> || _AbiList<_Rest...>::template _S_has_valid_abi<_Tp, _Np>;

    template <typename _Tp, int _Np>
      using _FirstValidAbi = conditional_t<
	_A0<sizeof(_Tp) * _Np>::template _S_is_valid_v<_Tp>,
	typename __decay_abi<_A0<sizeof(_Tp) * _Np>>::type,
	typename _AbiList<_Rest...>::template _FirstValidAbi<_Tp, _Np>>;

    template <typename _Tp, int _Np>
      static constexpr auto _S_determine_best_abi()
      {
	static_assert(_Np >= 1);
	constexpr int _Bytes = sizeof(_Tp) * _Np;
	if constexpr (_Np == 1)
	  return __make_dependent_t<_Tp, simd_abi::scalar>{};
	else
	  {
	    constexpr int __fullsize = _A0<_Bytes>::template _S_full_size<_Tp>;
	    // _A0<_Bytes> is good if:
	    // 1. The ABI tag is valid for _Tp
	    // 2. The storage overhead is no more than padding to fill the next
	    //    power-of-2 number of bytes
	    if constexpr (_A0<_Bytes>::template _S_is_valid_v<
			    _Tp> && __fullsize / 2 < _Np)
	      return typename __decay_abi<_A0<_Bytes>>::type{};
	    else
	      {
		using _Bp =
		  typename __find_next_valid_abi<_A0, _Bytes, _Tp>::type;
		if constexpr (_Bp::template _S_is_valid_v<
				_Tp> && _Bp::template _S_size<_Tp> <= _Np)
		  return _Bp{};
		else
		  return
		    typename _AbiList<_Rest...>::template _BestAbi<_Tp, _Np>{};
	      }
	  }
      }

    template <typename _Tp, int _Np>
      using _BestAbi = decltype(_S_determine_best_abi<_Tp, _Np>());
  };

// }}}1

// the following lists all native ABIs, which makes them accessible to
// simd_abi::deduce and select_best_vector_type_t (for fixed_size). Order
// matters: Whatever comes first has higher priority.
using _AllNativeAbis = _AbiList<simd_abi::_VecBltnBtmsk, simd_abi::_VecBuiltin,
				__scalar_abi_wrapper>;

// valid _SimdTraits specialization {{{1
template <typename _Tp, typename _Abi>
  struct _SimdTraits<_Tp, _Abi, void_t<typename _Abi::template _IsValid<_Tp>>>
  : _Abi::template __traits<_Tp> {};

// __deduce_impl specializations {{{1
// try all native ABIs (including scalar) first
template <typename _Tp, size_t _Np>
  struct __deduce_impl<
    _Tp, _Np, enable_if_t<_AllNativeAbis::template _S_has_valid_abi<_Tp, _Np>>>
  { using type = _AllNativeAbis::_FirstValidAbi<_Tp, _Np>; };

// fall back to fixed_size only if scalar and native ABIs don't match
template <typename _Tp, size_t _Np, typename = void>
  struct __deduce_fixed_size_fallback {};

template <typename _Tp, size_t _Np>
  struct __deduce_fixed_size_fallback<_Tp, _Np,
    enable_if_t<simd_abi::fixed_size<_Np>::template _S_is_valid_v<_Tp>>>
  { using type = simd_abi::fixed_size<_Np>; };

template <typename _Tp, size_t _Np, typename>
  struct __deduce_impl : public __deduce_fixed_size_fallback<_Tp, _Np> {};

//}}}1
/// @endcond

// simd_mask {{{
template <typename _Tp, typename _Abi>
  class simd_mask : public _SimdTraits<_Tp, _Abi>::_MaskBase
  {
    // types, tags, and friends {{{
    using _Traits = _SimdTraits<_Tp, _Abi>;
    using _MemberType = typename _Traits::_MaskMember;

    // We map all masks with equal element sizeof to a single integer type, the
    // one given by __int_for_sizeof_t<_Tp>. This is the approach
    // [[gnu::vector_size(N)]] types take as well and it reduces the number of
    // template specializations in the implementation classes.
    using _Ip = __int_for_sizeof_t<_Tp>;
    static constexpr _Ip* _S_type_tag = nullptr;

    friend typename _Traits::_MaskBase;
    friend class simd<_Tp, _Abi>;       // to construct masks on return
    friend typename _Traits::_SimdImpl; // to construct masks on return and
					// inspect data on masked operations
  public:
    using _Impl = typename _Traits::_MaskImpl;
    friend _Impl;

    // }}}
    // member types {{{
    using value_type = bool;
    using reference = _SmartReference<_MemberType, _Impl, value_type>;
    using simd_type = simd<_Tp, _Abi>;
    using abi_type = _Abi;

    // }}}
    static constexpr size_t size() // {{{
    { return __size_or_zero_v<_Tp, _Abi>; }

    // }}}
    // constructors & assignment {{{
    simd_mask() = default;
    simd_mask(const simd_mask&) = default;
    simd_mask(simd_mask&&) = default;
    simd_mask& operator=(const simd_mask&) = default;
    simd_mask& operator=(simd_mask&&) = default;

    // }}}
    // access to internal representation (optional feature) {{{
    _GLIBCXX_SIMD_ALWAYS_INLINE explicit
    simd_mask(typename _Traits::_MaskCastType __init)
    : _M_data{__init} {}
    // conversions to internal type is done in _MaskBase

    // }}}
    // bitset interface (extension to be proposed) {{{
    // TS_FEEDBACK:
    // Conversion of simd_mask to and from bitset makes it much easier to
    // interface with other facilities. I suggest adding `static
    // simd_mask::from_bitset` and `simd_mask::to_bitset`.
    _GLIBCXX_SIMD_ALWAYS_INLINE static simd_mask
    __from_bitset(bitset<size()> bs)
    { return {__bitset_init, bs}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE bitset<size()>
    __to_bitset() const
    { return _Impl::_S_to_bits(_M_data)._M_to_bitset(); }

    // }}}
    // explicit broadcast constructor {{{
    _GLIBCXX_SIMD_ALWAYS_INLINE explicit _GLIBCXX_SIMD_CONSTEXPR
    simd_mask(value_type __x)
    : _M_data(_Impl::template _S_broadcast<_Ip>(__x)) {}

    // }}}
    // implicit type conversion constructor {{{
  #ifdef _GLIBCXX_SIMD_ENABLE_IMPLICIT_MASK_CAST
    // proposed improvement
    template <typename _Up, typename _A2,
	      typename = enable_if_t<simd_size_v<_Up, _A2> == size()>>
      _GLIBCXX_SIMD_ALWAYS_INLINE explicit(sizeof(_MemberType)
	  != sizeof(typename _SimdTraits<_Up, _A2>::_MaskMember))
      simd_mask(const simd_mask<_Up, _A2>& __x)
      : simd_mask(__proposed::static_simd_cast<simd_mask>(__x)) {}
  #else
    // conforming to ISO/IEC 19570:2018
    template <typename _Up, typename = enable_if_t<conjunction<
			      is_same<abi_type, simd_abi::fixed_size<size()>>,
			      is_same<_Up, _Up>>::value>>
      _GLIBCXX_SIMD_ALWAYS_INLINE
      simd_mask(const simd_mask<_Up, simd_abi::fixed_size<size()>>& __x)
      : _M_data(_Impl::_S_from_bitmask(__data(__x), _S_type_tag)) {}
  #endif

    // }}}
    // load constructor {{{
    template <typename _Flags>
      _GLIBCXX_SIMD_ALWAYS_INLINE
      simd_mask(const value_type* __mem, _Flags)
      : _M_data(_Impl::template _S_load<_Ip>(
	_Flags::template _S_apply<simd_mask>(__mem))) {}

    template <typename _Flags>
      _GLIBCXX_SIMD_ALWAYS_INLINE
      simd_mask(const value_type* __mem, simd_mask __k, _Flags)
      : _M_data{}
      {
	_M_data
	  = _Impl::_S_masked_load(_M_data, __k._M_data,
				  _Flags::template _S_apply<simd_mask>(__mem));
      }

    // }}}
    // loads [simd_mask.load] {{{
    template <typename _Flags>
      _GLIBCXX_SIMD_ALWAYS_INLINE void
      copy_from(const value_type* __mem, _Flags)
      {
	_M_data = _Impl::template _S_load<_Ip>(
	  _Flags::template _S_apply<simd_mask>(__mem));
      }

    // }}}
    // stores [simd_mask.store] {{{
    template <typename _Flags>
      _GLIBCXX_SIMD_ALWAYS_INLINE void
      copy_to(value_type* __mem, _Flags) const
      { _Impl::_S_store(_M_data, _Flags::template _S_apply<simd_mask>(__mem)); }

    // }}}
    // scalar access {{{
    _GLIBCXX_SIMD_ALWAYS_INLINE reference
    operator[](size_t __i)
    {
      if (__i >= size())
	__invoke_ub("Subscript %d is out of range [0, %d]", __i, size() - 1);
      return {_M_data, int(__i)};
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE value_type
    operator[](size_t __i) const
    {
      if (__i >= size())
	__invoke_ub("Subscript %d is out of range [0, %d]", __i, size() - 1);
      if constexpr (__is_scalar_abi<_Abi>())
	return _M_data;
      else
	return static_cast<bool>(_M_data[__i]);
    }

    // }}}
    // negation {{{
    _GLIBCXX_SIMD_ALWAYS_INLINE simd_mask
    operator!() const
    { return {__private_init, _Impl::_S_bit_not(_M_data)}; }

    // }}}
    // simd_mask binary operators [simd_mask.binary] {{{
  #ifdef _GLIBCXX_SIMD_ENABLE_IMPLICIT_MASK_CAST
    // simd_mask<int> && simd_mask<uint> needs disambiguation
    template <typename _Up, typename _A2,
	      typename
	      = enable_if_t<is_convertible_v<simd_mask<_Up, _A2>, simd_mask>>>
      _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask
      operator&&(const simd_mask& __x, const simd_mask<_Up, _A2>& __y)
      {
	return {__private_init,
		_Impl::_S_logical_and(__x._M_data, simd_mask(__y)._M_data)};
      }

    template <typename _Up, typename _A2,
	      typename
	      = enable_if_t<is_convertible_v<simd_mask<_Up, _A2>, simd_mask>>>
      _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask
      operator||(const simd_mask& __x, const simd_mask<_Up, _A2>& __y)
      {
	return {__private_init,
		_Impl::_S_logical_or(__x._M_data, simd_mask(__y)._M_data)};
      }
  #endif // _GLIBCXX_SIMD_ENABLE_IMPLICIT_MASK_CAST

    _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask
    operator&&(const simd_mask& __x, const simd_mask& __y)
    {
      return {__private_init, _Impl::_S_logical_and(__x._M_data, __y._M_data)};
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask
    operator||(const simd_mask& __x, const simd_mask& __y)
    {
      return {__private_init, _Impl::_S_logical_or(__x._M_data, __y._M_data)};
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask
    operator&(const simd_mask& __x, const simd_mask& __y)
    { return {__private_init, _Impl::_S_bit_and(__x._M_data, __y._M_data)}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask
    operator|(const simd_mask& __x, const simd_mask& __y)
    { return {__private_init, _Impl::_S_bit_or(__x._M_data, __y._M_data)}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask
    operator^(const simd_mask& __x, const simd_mask& __y)
    { return {__private_init, _Impl::_S_bit_xor(__x._M_data, __y._M_data)}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask&
    operator&=(simd_mask& __x, const simd_mask& __y)
    {
      __x._M_data = _Impl::_S_bit_and(__x._M_data, __y._M_data);
      return __x;
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask&
    operator|=(simd_mask& __x, const simd_mask& __y)
    {
      __x._M_data = _Impl::_S_bit_or(__x._M_data, __y._M_data);
      return __x;
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE friend simd_mask&
    operator^=(simd_mask& __x, const simd_mask& __y)
    {
      __x._M_data = _Impl::_S_bit_xor(__x._M_data, __y._M_data);
      return __x;
    }

    // }}}
    // simd_mask compares [simd_mask.comparison] {{{
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd_mask
    operator==(const simd_mask& __x, const simd_mask& __y)
    { return !operator!=(__x, __y); }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd_mask
    operator!=(const simd_mask& __x, const simd_mask& __y)
    { return {__private_init, _Impl::_S_bit_xor(__x._M_data, __y._M_data)}; }

    // }}}
    // private_init ctor {{{
    _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
    simd_mask(_PrivateInit, typename _Traits::_MaskMember __init)
    : _M_data(__init) {}

    // }}}
    // private_init generator ctor {{{
    template <typename _Fp, typename = decltype(bool(declval<_Fp>()(size_t())))>
      _GLIBCXX_SIMD_INTRINSIC constexpr
      simd_mask(_PrivateInit, _Fp&& __gen)
      : _M_data()
      {
	__execute_n_times<size()>([&](auto __i) constexpr {
	  _Impl::_S_set(_M_data, __i, __gen(__i));
	});
      }

    // }}}
    // bitset_init ctor {{{
    _GLIBCXX_SIMD_INTRINSIC simd_mask(_BitsetInit, bitset<size()> __init)
    : _M_data(
	_Impl::_S_from_bitmask(_SanitizedBitMask<size()>(__init), _S_type_tag))
    {}

    // }}}
    // __cvt {{{
    // TS_FEEDBACK:
    // The conversion operator this implements should be a ctor on simd_mask.
    // Once you call .__cvt() on a simd_mask it converts conveniently.
    // A useful variation: add `explicit(sizeof(_Tp) != sizeof(_Up))`
    struct _CvtProxy
    {
      template <typename _Up, typename _A2,
		typename
		= enable_if_t<simd_size_v<_Up, _A2> == simd_size_v<_Tp, _Abi>>>
	_GLIBCXX_SIMD_ALWAYS_INLINE
	operator simd_mask<_Up, _A2>() &&
	{
	  using namespace std::experimental::__proposed;
	  return static_simd_cast<simd_mask<_Up, _A2>>(_M_data);
	}

      const simd_mask<_Tp, _Abi>& _M_data;
    };

    _GLIBCXX_SIMD_INTRINSIC _CvtProxy
    __cvt() const
    { return {*this}; }

    // }}}
    // operator?: overloads (suggested extension) {{{
  #ifdef __GXX_CONDITIONAL_IS_OVERLOADABLE__
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd_mask
    operator?:(const simd_mask& __k, const simd_mask& __where_true,
	       const simd_mask& __where_false)
    {
      auto __ret = __where_false;
      _Impl::_S_masked_assign(__k._M_data, __ret._M_data, __where_true._M_data);
      return __ret;
    }

    template <typename _U1, typename _U2,
	      typename _Rp = simd<common_type_t<_U1, _U2>, _Abi>,
	      typename = enable_if_t<conjunction_v<
		is_convertible<_U1, _Rp>, is_convertible<_U2, _Rp>,
		is_convertible<simd_mask, typename _Rp::mask_type>>>>
      _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend _Rp
      operator?:(const simd_mask& __k, const _U1& __where_true,
		 const _U2& __where_false)
      {
	_Rp __ret = __where_false;
	_Rp::_Impl::_S_masked_assign(
	  __data(static_cast<typename _Rp::mask_type>(__k)), __data(__ret),
	  __data(static_cast<_Rp>(__where_true)));
	return __ret;
      }

  #ifdef _GLIBCXX_SIMD_ENABLE_IMPLICIT_MASK_CAST
    template <typename _Kp, typename _Ak, typename _Up, typename _Au,
	      typename = enable_if_t<
		conjunction_v<is_convertible<simd_mask<_Kp, _Ak>, simd_mask>,
			      is_convertible<simd_mask<_Up, _Au>, simd_mask>>>>
      _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd_mask
      operator?:(const simd_mask<_Kp, _Ak>& __k, const simd_mask& __where_true,
		 const simd_mask<_Up, _Au>& __where_false)
      {
	simd_mask __ret = __where_false;
	_Impl::_S_masked_assign(simd_mask(__k)._M_data, __ret._M_data,
				__where_true._M_data);
	return __ret;
      }
  #endif // _GLIBCXX_SIMD_ENABLE_IMPLICIT_MASK_CAST
  #endif // __GXX_CONDITIONAL_IS_OVERLOADABLE__

    // }}}
    // _M_is_constprop {{{
    _GLIBCXX_SIMD_INTRINSIC constexpr bool
    _M_is_constprop() const
    {
      if constexpr (__is_scalar_abi<_Abi>())
	return __builtin_constant_p(_M_data);
      else
	return _M_data._M_is_constprop();
    }

    // }}}

  private:
    friend const auto& __data<_Tp, abi_type>(const simd_mask&);
    friend auto& __data<_Tp, abi_type>(simd_mask&);
    alignas(_Traits::_S_mask_align) _MemberType _M_data;
  };

// }}}

/// @cond undocumented
// __data(simd_mask) {{{
template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC constexpr const auto&
  __data(const simd_mask<_Tp, _Ap>& __x)
  { return __x._M_data; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto&
  __data(simd_mask<_Tp, _Ap>& __x)
  { return __x._M_data; }

// }}}
/// @endcond

// simd_mask reductions [simd_mask.reductions] {{{
template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR bool
  all_of(const simd_mask<_Tp, _Abi>& __k) noexcept
  {
    if (__builtin_is_constant_evaluated() || __k._M_is_constprop())
      {
	for (size_t __i = 0; __i < simd_size_v<_Tp, _Abi>; ++__i)
	  if (!__k[__i])
	    return false;
	return true;
      }
    else
      return _Abi::_MaskImpl::_S_all_of(__k);
  }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR bool
  any_of(const simd_mask<_Tp, _Abi>& __k) noexcept
  {
    if (__builtin_is_constant_evaluated() || __k._M_is_constprop())
      {
	for (size_t __i = 0; __i < simd_size_v<_Tp, _Abi>; ++__i)
	  if (__k[__i])
	    return true;
	return false;
      }
    else
      return _Abi::_MaskImpl::_S_any_of(__k);
  }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR bool
  none_of(const simd_mask<_Tp, _Abi>& __k) noexcept
  {
    if (__builtin_is_constant_evaluated() || __k._M_is_constprop())
      {
	for (size_t __i = 0; __i < simd_size_v<_Tp, _Abi>; ++__i)
	  if (__k[__i])
	    return false;
	return true;
      }
    else
      return _Abi::_MaskImpl::_S_none_of(__k);
  }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR bool
  some_of(const simd_mask<_Tp, _Abi>& __k) noexcept
  {
    if (__builtin_is_constant_evaluated() || __k._M_is_constprop())
      {
	for (size_t __i = 1; __i < simd_size_v<_Tp, _Abi>; ++__i)
	  if (__k[__i] != __k[__i - 1])
	    return true;
	return false;
      }
    else
      return _Abi::_MaskImpl::_S_some_of(__k);
  }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR int
  popcount(const simd_mask<_Tp, _Abi>& __k) noexcept
  {
    if (__builtin_is_constant_evaluated() || __k._M_is_constprop())
      {
	const int __r = __call_with_subscripts<simd_size_v<_Tp, _Abi>>(
	  __k, [](auto... __elements) { return ((__elements != 0) + ...); });
	if (__builtin_is_constant_evaluated() || __builtin_constant_p(__r))
	  return __r;
      }
    return _Abi::_MaskImpl::_S_popcount(__k);
  }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR int
  find_first_set(const simd_mask<_Tp, _Abi>& __k)
  {
    if (__builtin_is_constant_evaluated() || __k._M_is_constprop())
      {
	constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	const size_t _Idx = __call_with_n_evaluations<_Np>(
	  [](auto... __indexes) { return std::min({__indexes...}); },
	  [&](auto __i) { return __k[__i] ? +__i : _Np; });
	if (_Idx >= _Np)
	  __invoke_ub("find_first_set(empty mask) is UB");
	if (__builtin_constant_p(_Idx))
	  return _Idx;
      }
    return _Abi::_MaskImpl::_S_find_first_set(__k);
  }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR int
  find_last_set(const simd_mask<_Tp, _Abi>& __k)
  {
    if (__builtin_is_constant_evaluated() || __k._M_is_constprop())
      {
	constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	const int _Idx = __call_with_n_evaluations<_Np>(
	  [](auto... __indexes) { return std::max({__indexes...}); },
	  [&](auto __i) { return __k[__i] ? int(__i) : -1; });
	if (_Idx < 0)
	  __invoke_ub("find_first_set(empty mask) is UB");
	if (__builtin_constant_p(_Idx))
	  return _Idx;
      }
    return _Abi::_MaskImpl::_S_find_last_set(__k);
  }

_GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR bool
all_of(_ExactBool __x) noexcept
{ return __x; }

_GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR bool
any_of(_ExactBool __x) noexcept
{ return __x; }

_GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR bool
none_of(_ExactBool __x) noexcept
{ return !__x; }

_GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR bool
some_of(_ExactBool) noexcept
{ return false; }

_GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR int
popcount(_ExactBool __x) noexcept
{ return __x; }

_GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR int
find_first_set(_ExactBool)
{ return 0; }

_GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR int
find_last_set(_ExactBool)
{ return 0; }

// }}}

/// @cond undocumented
// _SimdIntOperators{{{1
template <typename _V, typename _Tp, typename _Abi, bool>
  class _SimdIntOperators {};

template <typename _V, typename _Tp, typename _Abi>
  class _SimdIntOperators<_V, _Tp, _Abi, true>
  {
    using _Impl = typename _SimdTraits<_Tp, _Abi>::_SimdImpl;

    _GLIBCXX_SIMD_INTRINSIC const _V& __derived() const
    { return *static_cast<const _V*>(this); }

    template <typename _Up>
      _GLIBCXX_SIMD_INTRINSIC static _GLIBCXX_SIMD_CONSTEXPR _V
      _S_make_derived(_Up&& __d)
      { return {__private_init, static_cast<_Up&&>(__d)}; }

  public:
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V&
    operator%=(_V& __lhs, const _V& __x)
    { return __lhs = __lhs % __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V&
    operator&=(_V& __lhs, const _V& __x)
    { return __lhs = __lhs & __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V&
    operator|=(_V& __lhs, const _V& __x)
    { return __lhs = __lhs | __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V&
    operator^=(_V& __lhs, const _V& __x)
    { return __lhs = __lhs ^ __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V&
    operator<<=(_V& __lhs, const _V& __x)
    { return __lhs = __lhs << __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V&
    operator>>=(_V& __lhs, const _V& __x)
    { return __lhs = __lhs >> __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V&
    operator<<=(_V& __lhs, int __x)
    { return __lhs = __lhs << __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V&
    operator>>=(_V& __lhs, int __x)
    { return __lhs = __lhs >> __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V
    operator%(const _V& __x, const _V& __y)
    {
      return _SimdIntOperators::_S_make_derived(
	_Impl::_S_modulus(__data(__x), __data(__y)));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V
    operator&(const _V& __x, const _V& __y)
    {
      return _SimdIntOperators::_S_make_derived(
	_Impl::_S_bit_and(__data(__x), __data(__y)));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V
    operator|(const _V& __x, const _V& __y)
    {
      return _SimdIntOperators::_S_make_derived(
	_Impl::_S_bit_or(__data(__x), __data(__y)));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V
    operator^(const _V& __x, const _V& __y)
    {
      return _SimdIntOperators::_S_make_derived(
	_Impl::_S_bit_xor(__data(__x), __data(__y)));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V
    operator<<(const _V& __x, const _V& __y)
    {
      return _SimdIntOperators::_S_make_derived(
	_Impl::_S_bit_shift_left(__data(__x), __data(__y)));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V
    operator>>(const _V& __x, const _V& __y)
    {
      return _SimdIntOperators::_S_make_derived(
	_Impl::_S_bit_shift_right(__data(__x), __data(__y)));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V
    operator<<(const _V& __x, int __y)
    {
      if (__y < 0)
	__invoke_ub("The behavior is undefined if the right operand of a "
		    "shift operation is negative. [expr.shift]\nA shift by "
		    "%d was requested",
		    __y);
      if (size_t(__y) >= sizeof(declval<_Tp>() << __y) * __CHAR_BIT__)
	__invoke_ub(
	  "The behavior is undefined if the right operand of a "
	  "shift operation is greater than or equal to the width of the "
	  "promoted left operand. [expr.shift]\nA shift by %d was requested",
	  __y);
      return _SimdIntOperators::_S_make_derived(
	_Impl::_S_bit_shift_left(__data(__x), __y));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend
    _V
    operator>>(const _V& __x, int __y)
    {
      if (__y < 0)
	__invoke_ub(
	  "The behavior is undefined if the right operand of a shift "
	  "operation is negative. [expr.shift]\nA shift by %d was requested",
	  __y);
      if (size_t(__y) >= sizeof(declval<_Tp>() << __y) * __CHAR_BIT__)
	__invoke_ub(
	  "The behavior is undefined if the right operand of a shift "
	  "operation is greater than or equal to the width of the promoted "
	  "left operand. [expr.shift]\nA shift by %d was requested",
	  __y);
      return _SimdIntOperators::_S_make_derived(
	_Impl::_S_bit_shift_right(__data(__x), __y));
    }

    // unary operators (for integral _Tp)
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR
    _V
    operator~() const
    { return {__private_init, _Impl::_S_complement(__derived()._M_data)}; }
  };

//}}}1
/// @endcond

// simd {{{
template <typename _Tp, typename _Abi>
  class simd : public _SimdIntOperators<
		 simd<_Tp, _Abi>, _Tp, _Abi,
		 conjunction<is_integral<_Tp>,
			     typename _SimdTraits<_Tp, _Abi>::_IsValid>::value>,
	       public _SimdTraits<_Tp, _Abi>::_SimdBase
  {
    using _Traits = _SimdTraits<_Tp, _Abi>;
    using _MemberType = typename _Traits::_SimdMember;
    using _CastType = typename _Traits::_SimdCastType;
    static constexpr _Tp* _S_type_tag = nullptr;
    friend typename _Traits::_SimdBase;

  public:
    using _Impl = typename _Traits::_SimdImpl;
    friend _Impl;
    friend _SimdIntOperators<simd, _Tp, _Abi, true>;

    using value_type = _Tp;
    using reference = _SmartReference<_MemberType, _Impl, value_type>;
    using mask_type = simd_mask<_Tp, _Abi>;
    using abi_type = _Abi;

    static constexpr size_t size()
    { return __size_or_zero_v<_Tp, _Abi>; }

    _GLIBCXX_SIMD_CONSTEXPR simd() = default;
    _GLIBCXX_SIMD_CONSTEXPR simd(const simd&) = default;
    _GLIBCXX_SIMD_CONSTEXPR simd(simd&&) noexcept = default;
    _GLIBCXX_SIMD_CONSTEXPR simd& operator=(const simd&) = default;
    _GLIBCXX_SIMD_CONSTEXPR simd& operator=(simd&&) noexcept = default;

    // implicit broadcast constructor
    template <typename _Up,
	      typename = enable_if_t<!is_same_v<__remove_cvref_t<_Up>, bool>>>
      _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR
      simd(_ValuePreservingOrInt<_Up, value_type>&& __x)
      : _M_data(
	_Impl::_S_broadcast(static_cast<value_type>(static_cast<_Up&&>(__x))))
      {}

    // implicit type conversion constructor (convert from fixed_size to
    // fixed_size)
    template <typename _Up>
      _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR
      simd(const simd<_Up, simd_abi::fixed_size<size()>>& __x,
	   enable_if_t<
	     conjunction<
	       is_same<simd_abi::fixed_size<size()>, abi_type>,
	       negation<__is_narrowing_conversion<_Up, value_type>>,
	       __converts_to_higher_integer_rank<_Up, value_type>>::value,
	     void*> = nullptr)
      : simd{static_cast<array<_Up, size()>>(__x).data(), vector_aligned} {}

      // explicit type conversion constructor
#ifdef _GLIBCXX_SIMD_ENABLE_STATIC_CAST
    template <typename _Up, typename _A2,
	      typename = decltype(static_simd_cast<simd>(
		declval<const simd<_Up, _A2>&>()))>
      _GLIBCXX_SIMD_ALWAYS_INLINE explicit _GLIBCXX_SIMD_CONSTEXPR
      simd(const simd<_Up, _A2>& __x)
      : simd(static_simd_cast<simd>(__x)) {}
#endif // _GLIBCXX_SIMD_ENABLE_STATIC_CAST

    // generator constructor
    template <typename _Fp>
      _GLIBCXX_SIMD_ALWAYS_INLINE explicit _GLIBCXX_SIMD_CONSTEXPR
      simd(_Fp&& __gen, _ValuePreservingOrInt<decltype(declval<_Fp>()(
						declval<_SizeConstant<0>&>())),
					      value_type>* = nullptr)
      : _M_data(_Impl::_S_generator(static_cast<_Fp&&>(__gen), _S_type_tag)) {}

    // load constructor
    template <typename _Up, typename _Flags>
      _GLIBCXX_SIMD_ALWAYS_INLINE
      simd(const _Up* __mem, _Flags)
      : _M_data(
	  _Impl::_S_load(_Flags::template _S_apply<simd>(__mem), _S_type_tag))
      {}

    // loads [simd.load]
    template <typename _Up, typename _Flags>
      _GLIBCXX_SIMD_ALWAYS_INLINE void
      copy_from(const _Vectorizable<_Up>* __mem, _Flags)
      {
	_M_data = static_cast<decltype(_M_data)>(
	  _Impl::_S_load(_Flags::template _S_apply<simd>(__mem), _S_type_tag));
      }

    // stores [simd.store]
    template <typename _Up, typename _Flags>
      _GLIBCXX_SIMD_ALWAYS_INLINE void
      copy_to(_Vectorizable<_Up>* __mem, _Flags) const
      {
	_Impl::_S_store(_M_data, _Flags::template _S_apply<simd>(__mem),
			_S_type_tag);
      }

    // scalar access
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR reference
    operator[](size_t __i)
    { return {_M_data, int(__i)}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR value_type
    operator[]([[maybe_unused]] size_t __i) const
    {
      if constexpr (__is_scalar_abi<_Abi>())
	{
	  _GLIBCXX_DEBUG_ASSERT(__i == 0);
	  return _M_data;
	}
      else
	return _M_data[__i];
    }

    // increment and decrement:
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR simd&
    operator++()
    {
      _Impl::_S_increment(_M_data);
      return *this;
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR simd
    operator++(int)
    {
      simd __r = *this;
      _Impl::_S_increment(_M_data);
      return __r;
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR simd&
    operator--()
    {
      _Impl::_S_decrement(_M_data);
      return *this;
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR simd
    operator--(int)
    {
      simd __r = *this;
      _Impl::_S_decrement(_M_data);
      return __r;
    }

    // unary operators (for any _Tp)
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR mask_type
    operator!() const
    { return {__private_init, _Impl::_S_negate(_M_data)}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR simd
    operator+() const
    { return *this; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR simd
    operator-() const
    { return {__private_init, _Impl::_S_unary_minus(_M_data)}; }

    // access to internal representation (suggested extension)
    _GLIBCXX_SIMD_ALWAYS_INLINE explicit _GLIBCXX_SIMD_CONSTEXPR
    simd(_CastType __init) : _M_data(__init) {}

    // compound assignment [simd.cassign]
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd&
    operator+=(simd& __lhs, const simd& __x)
    { return __lhs = __lhs + __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd&
    operator-=(simd& __lhs, const simd& __x)
    { return __lhs = __lhs - __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd&
    operator*=(simd& __lhs, const simd& __x)
    { return __lhs = __lhs * __x; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd&
    operator/=(simd& __lhs, const simd& __x)
    { return __lhs = __lhs / __x; }

    // binary operators [simd.binary]
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd
    operator+(const simd& __x, const simd& __y)
    { return {__private_init, _Impl::_S_plus(__x._M_data, __y._M_data)}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd
    operator-(const simd& __x, const simd& __y)
    { return {__private_init, _Impl::_S_minus(__x._M_data, __y._M_data)}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd
    operator*(const simd& __x, const simd& __y)
    { return {__private_init, _Impl::_S_multiplies(__x._M_data, __y._M_data)}; }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd
    operator/(const simd& __x, const simd& __y)
    { return {__private_init, _Impl::_S_divides(__x._M_data, __y._M_data)}; }

    // compares [simd.comparison]
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend mask_type
    operator==(const simd& __x, const simd& __y)
    { return simd::_S_make_mask(_Impl::_S_equal_to(__x._M_data, __y._M_data)); }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend mask_type
    operator!=(const simd& __x, const simd& __y)
    {
      return simd::_S_make_mask(
	_Impl::_S_not_equal_to(__x._M_data, __y._M_data));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend mask_type
    operator<(const simd& __x, const simd& __y)
    { return simd::_S_make_mask(_Impl::_S_less(__x._M_data, __y._M_data)); }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend mask_type
    operator<=(const simd& __x, const simd& __y)
    {
      return simd::_S_make_mask(_Impl::_S_less_equal(__x._M_data, __y._M_data));
    }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend mask_type
    operator>(const simd& __x, const simd& __y)
    { return simd::_S_make_mask(_Impl::_S_less(__y._M_data, __x._M_data)); }

    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend mask_type
    operator>=(const simd& __x, const simd& __y)
    {
      return simd::_S_make_mask(_Impl::_S_less_equal(__y._M_data, __x._M_data));
    }

    // operator?: overloads (suggested extension) {{{
#ifdef __GXX_CONDITIONAL_IS_OVERLOADABLE__
    _GLIBCXX_SIMD_ALWAYS_INLINE _GLIBCXX_SIMD_CONSTEXPR friend simd
    operator?:(const mask_type& __k, const simd& __where_true,
	const simd& __where_false)
    {
      auto __ret = __where_false;
      _Impl::_S_masked_assign(__data(__k), __data(__ret), __data(__where_true));
      return __ret;
    }

#endif // __GXX_CONDITIONAL_IS_OVERLOADABLE__
    // }}}

    // "private" because of the first arguments's namespace
    _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
    simd(_PrivateInit, const _MemberType& __init)
    : _M_data(__init) {}

    // "private" because of the first arguments's namespace
    _GLIBCXX_SIMD_INTRINSIC
    simd(_BitsetInit, bitset<size()> __init) : _M_data()
    { where(mask_type(__bitset_init, __init), *this) = ~*this; }

    _GLIBCXX_SIMD_INTRINSIC constexpr bool
    _M_is_constprop() const
    {
      if constexpr (__is_scalar_abi<_Abi>())
	return __builtin_constant_p(_M_data);
      else
	return _M_data._M_is_constprop();
    }

  private:
    _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR static mask_type
    _S_make_mask(typename mask_type::_MemberType __k)
    { return {__private_init, __k}; }

    friend const auto& __data<value_type, abi_type>(const simd&);
    friend auto& __data<value_type, abi_type>(simd&);
    alignas(_Traits::_S_simd_align) _MemberType _M_data;
  };

// }}}
/// @cond undocumented
// __data {{{
template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC constexpr const auto&
  __data(const simd<_Tp, _Ap>& __x)
  { return __x._M_data; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto&
  __data(simd<_Tp, _Ap>& __x)
  { return __x._M_data; }

// }}}
namespace __float_bitwise_operators { //{{{
template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR simd<_Tp, _Ap>
  operator^(const simd<_Tp, _Ap>& __a, const simd<_Tp, _Ap>& __b)
  {
    return {__private_init,
	    _Ap::_SimdImpl::_S_bit_xor(__data(__a), __data(__b))};
  }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR simd<_Tp, _Ap>
  operator|(const simd<_Tp, _Ap>& __a, const simd<_Tp, _Ap>& __b)
  {
    return {__private_init,
	    _Ap::_SimdImpl::_S_bit_or(__data(__a), __data(__b))};
  }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR simd<_Tp, _Ap>
  operator&(const simd<_Tp, _Ap>& __a, const simd<_Tp, _Ap>& __b)
  {
    return {__private_init,
	    _Ap::_SimdImpl::_S_bit_and(__data(__a), __data(__b))};
  }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC _GLIBCXX_SIMD_CONSTEXPR
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Ap>>
  operator~(const simd<_Tp, _Ap>& __a)
  { return {__private_init, _Ap::_SimdImpl::_S_complement(__data(__a))}; }
} // namespace __float_bitwise_operators }}}
/// @endcond

/// @}
_GLIBCXX_SIMD_END_NAMESPACE

#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_H

// vim: foldmethod=marker foldmarker={{{,}}}
