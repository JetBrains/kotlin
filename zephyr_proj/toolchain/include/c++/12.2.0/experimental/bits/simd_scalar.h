// Simd scalar ABI specific implementations -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_SCALAR_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_SCALAR_H_
#if __cplusplus >= 201703L

#include <cmath>

_GLIBCXX_SIMD_BEGIN_NAMESPACE

// __promote_preserving_unsigned{{{
// work around crazy semantics of unsigned integers of lower rank than int:
// Before applying an operator the operands are promoted to int. In which case
// over- or underflow is UB, even though the operand types were unsigned.
template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC constexpr decltype(auto)
  __promote_preserving_unsigned(const _Tp& __x)
  {
    if constexpr (is_signed_v<decltype(+__x)> && is_unsigned_v<_Tp>)
      return static_cast<unsigned int>(__x);
    else
      return __x;
  }

// }}}

struct _CommonImplScalar;
struct _CommonImplBuiltin;
struct _SimdImplScalar;
struct _MaskImplScalar;

// simd_abi::_Scalar {{{
struct simd_abi::_Scalar
{
  template <typename _Tp>
    static constexpr size_t _S_size = 1;

  template <typename _Tp>
    static constexpr size_t _S_full_size = 1;

  template <typename _Tp>
    static constexpr bool _S_is_partial = false;

  struct _IsValidAbiTag : true_type {};

  template <typename _Tp>
    struct _IsValidSizeFor : true_type {};

  template <typename _Tp>
    struct _IsValid : __is_vectorizable<_Tp> {};

  template <typename _Tp>
    static constexpr bool _S_is_valid_v = _IsValid<_Tp>::value;

  _GLIBCXX_SIMD_INTRINSIC static constexpr bool _S_masked(bool __x)
  { return __x; }

  using _CommonImpl = _CommonImplScalar;
  using _SimdImpl = _SimdImplScalar;
  using _MaskImpl = _MaskImplScalar;

  template <typename _Tp, bool = _S_is_valid_v<_Tp>>
    struct __traits : _InvalidTraits {};

  template <typename _Tp>
    struct __traits<_Tp, true>
    {
      using _IsValid = true_type;
      using _SimdImpl = _SimdImplScalar;
      using _MaskImpl = _MaskImplScalar;
      using _SimdMember = _Tp;
      using _MaskMember = bool;

      static constexpr size_t _S_simd_align = alignof(_SimdMember);
      static constexpr size_t _S_mask_align = alignof(_MaskMember);

      // nothing the user can spell converts to/from simd/simd_mask
      struct _SimdCastType { _SimdCastType() = delete; };
      struct _MaskCastType { _MaskCastType() = delete; };
      struct _SimdBase {};
      struct _MaskBase {};
    };
};

// }}}
// _CommonImplScalar {{{
struct _CommonImplScalar
{
  // _S_store {{{
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static void _S_store(_Tp __x, void* __addr)
    { __builtin_memcpy(__addr, &__x, sizeof(_Tp)); }

  // }}}
  // _S_store_bool_array(_BitMask) {{{
  template <size_t _Np, bool _Sanitized>
    _GLIBCXX_SIMD_INTRINSIC static constexpr void
    _S_store_bool_array(_BitMask<_Np, _Sanitized> __x, bool* __mem)
    {
      __make_dependent_t<decltype(__x), _CommonImplBuiltin>::_S_store_bool_array(
	__x, __mem);
    }

  // }}}
};

// }}}
// _SimdImplScalar {{{
struct _SimdImplScalar
{
  // member types {{{2
  using abi_type = simd_abi::scalar;

  template <typename _Tp>
    using _TypeTag = _Tp*;

  // _S_broadcast {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _Tp _S_broadcast(_Tp __x) noexcept
    { return __x; }

  // _S_generator {{{2
  template <typename _Fp, typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static constexpr _Tp _S_generator(_Fp&& __gen,
							      _TypeTag<_Tp>)
    { return __gen(_SizeConstant<0>()); }

  // _S_load {{{2
  template <typename _Tp, typename _Up>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_load(const _Up* __mem,
					       _TypeTag<_Tp>) noexcept
    { return static_cast<_Tp>(__mem[0]); }

  // _S_masked_load {{{2
  template <typename _Tp, typename _Up>
    _GLIBCXX_SIMD_INTRINSIC
    static _Tp _S_masked_load(_Tp __merge, bool __k,
				     const _Up* __mem) noexcept
    {
      if (__k)
	__merge = static_cast<_Tp>(__mem[0]);
      return __merge;
    }

  // _S_store {{{2
  template <typename _Tp, typename _Up>
    _GLIBCXX_SIMD_INTRINSIC
    static void _S_store(_Tp __v, _Up* __mem, _TypeTag<_Tp>) noexcept
    { __mem[0] = static_cast<_Up>(__v); }

  // _S_masked_store {{{2
  template <typename _Tp, typename _Up>
    _GLIBCXX_SIMD_INTRINSIC
    static void _S_masked_store(const _Tp __v, _Up* __mem,
				       const bool __k) noexcept
    { if (__k) __mem[0] = __v; }

  // _S_negate {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr bool _S_negate(_Tp __x) noexcept
    { return !__x; }

  // _S_reduce {{{2
  template <typename _Tp, typename _BinaryOperation>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp
    _S_reduce(const simd<_Tp, simd_abi::scalar>& __x, const _BinaryOperation&)
    { return __x._M_data; }

  // _S_min, _S_max {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_min(const _Tp __a, const _Tp __b)
    { return std::min(__a, __b); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_max(const _Tp __a, const _Tp __b)
    { return std::max(__a, __b); }

  // _S_complement {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_complement(_Tp __x) noexcept
    { return static_cast<_Tp>(~__x); }

  // _S_unary_minus {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_unary_minus(_Tp __x) noexcept
    { return static_cast<_Tp>(-__x); }

  // arithmetic operators {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_plus(_Tp __x, _Tp __y)
    {
      return static_cast<_Tp>(__promote_preserving_unsigned(__x)
			      + __promote_preserving_unsigned(__y));
    }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_minus(_Tp __x, _Tp __y)
    {
      return static_cast<_Tp>(__promote_preserving_unsigned(__x)
			      - __promote_preserving_unsigned(__y));
    }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_multiplies(_Tp __x, _Tp __y)
    {
      return static_cast<_Tp>(__promote_preserving_unsigned(__x)
			      * __promote_preserving_unsigned(__y));
    }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_divides(_Tp __x, _Tp __y)
    {
      return static_cast<_Tp>(__promote_preserving_unsigned(__x)
			      / __promote_preserving_unsigned(__y));
    }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_modulus(_Tp __x, _Tp __y)
    {
      return static_cast<_Tp>(__promote_preserving_unsigned(__x)
			      % __promote_preserving_unsigned(__y));
    }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_bit_and(_Tp __x, _Tp __y)
    {
      if constexpr (is_floating_point_v<_Tp>)
	{
	  using _Ip = __int_for_sizeof_t<_Tp>;
	  return __bit_cast<_Tp>(__bit_cast<_Ip>(__x) & __bit_cast<_Ip>(__y));
	}
      else
	return static_cast<_Tp>(__promote_preserving_unsigned(__x)
				& __promote_preserving_unsigned(__y));
    }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_bit_or(_Tp __x, _Tp __y)
    {
      if constexpr (is_floating_point_v<_Tp>)
	{
	  using _Ip = __int_for_sizeof_t<_Tp>;
	  return __bit_cast<_Tp>(__bit_cast<_Ip>(__x) | __bit_cast<_Ip>(__y));
	}
      else
	return static_cast<_Tp>(__promote_preserving_unsigned(__x)
				| __promote_preserving_unsigned(__y));
    }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_bit_xor(_Tp __x, _Tp __y)
    {
      if constexpr (is_floating_point_v<_Tp>)
	{
	  using _Ip = __int_for_sizeof_t<_Tp>;
	  return __bit_cast<_Tp>(__bit_cast<_Ip>(__x) ^ __bit_cast<_Ip>(__y));
	}
      else
	return static_cast<_Tp>(__promote_preserving_unsigned(__x)
				^ __promote_preserving_unsigned(__y));
    }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_bit_shift_left(_Tp __x, int __y)
    { return static_cast<_Tp>(__promote_preserving_unsigned(__x) << __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    static constexpr _Tp _S_bit_shift_right(_Tp __x, int __y)
    { return static_cast<_Tp>(__promote_preserving_unsigned(__x) >> __y); }

  // math {{{2
  // frexp, modf and copysign implemented in simd_math.h
  template <typename _Tp>
    using _ST = _SimdTuple<_Tp, simd_abi::scalar>;

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_acos(_Tp __x)
    { return std::acos(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_asin(_Tp __x)
    { return std::asin(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_atan(_Tp __x)
    { return std::atan(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_cos(_Tp __x)
    { return std::cos(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_sin(_Tp __x)
    { return std::sin(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_tan(_Tp __x)
    { return std::tan(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_acosh(_Tp __x)
    { return std::acosh(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_asinh(_Tp __x)
    { return std::asinh(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_atanh(_Tp __x)
    { return std::atanh(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_cosh(_Tp __x)
    { return std::cosh(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_sinh(_Tp __x)
    { return std::sinh(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_tanh(_Tp __x)
    { return std::tanh(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_atan2(_Tp __x, _Tp __y)
    { return std::atan2(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_exp(_Tp __x)
    { return std::exp(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_exp2(_Tp __x)
    { return std::exp2(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_expm1(_Tp __x)
    { return std::expm1(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_log(_Tp __x)
    { return std::log(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_log10(_Tp __x)
    { return std::log10(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_log1p(_Tp __x)
    { return std::log1p(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_log2(_Tp __x)
    { return std::log2(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_logb(_Tp __x)
    { return std::logb(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _ST<int> _S_ilogb(_Tp __x)
    { return {std::ilogb(__x)}; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_pow(_Tp __x, _Tp __y)
    { return std::pow(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_abs(_Tp __x)
    { return std::abs(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_fabs(_Tp __x)
    { return std::fabs(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_sqrt(_Tp __x)
    { return std::sqrt(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_cbrt(_Tp __x)
    { return std::cbrt(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_erf(_Tp __x)
    { return std::erf(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_erfc(_Tp __x)
    { return std::erfc(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_lgamma(_Tp __x)
    { return std::lgamma(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_tgamma(_Tp __x)
    { return std::tgamma(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_trunc(_Tp __x)
    { return std::trunc(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_floor(_Tp __x)
    { return std::floor(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_ceil(_Tp __x)
    { return std::ceil(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_nearbyint(_Tp __x)
    { return std::nearbyint(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_rint(_Tp __x)
    { return std::rint(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _ST<long> _S_lrint(_Tp __x)
    { return {std::lrint(__x)}; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _ST<long long> _S_llrint(_Tp __x)
    { return {std::llrint(__x)}; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_round(_Tp __x)
    { return std::round(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _ST<long> _S_lround(_Tp __x)
    { return {std::lround(__x)}; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _ST<long long> _S_llround(_Tp __x)
    { return {std::llround(__x)}; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_ldexp(_Tp __x, _ST<int> __y)
    { return std::ldexp(__x, __y.first); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_scalbn(_Tp __x, _ST<int> __y)
    { return std::scalbn(__x, __y.first); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_scalbln(_Tp __x, _ST<long> __y)
    { return std::scalbln(__x, __y.first); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_fmod(_Tp __x, _Tp __y)
    { return std::fmod(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_remainder(_Tp __x, _Tp __y)
    { return std::remainder(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_nextafter(_Tp __x, _Tp __y)
    { return std::nextafter(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_fdim(_Tp __x, _Tp __y)
    { return std::fdim(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_fmax(_Tp __x, _Tp __y)
    { return std::fmax(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_fmin(_Tp __x, _Tp __y)
    { return std::fmin(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_fma(_Tp __x, _Tp __y, _Tp __z)
    { return std::fma(__x, __y, __z); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC static _Tp _S_remquo(_Tp __x, _Tp __y, _ST<int>* __z)
    { return std::remquo(__x, __y, &__z->first); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static _ST<int> _S_fpclassify(_Tp __x)
    { return {std::fpclassify(__x)}; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_isfinite(_Tp __x)
    { return std::isfinite(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_isinf(_Tp __x)
    { return std::isinf(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_isnan(_Tp __x)
    { return std::isnan(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_isnormal(_Tp __x)
    { return std::isnormal(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_signbit(_Tp __x)
    { return std::signbit(__x); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_isgreater(_Tp __x, _Tp __y)
    { return std::isgreater(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_isgreaterequal(_Tp __x,
								    _Tp __y)
    { return std::isgreaterequal(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_isless(_Tp __x, _Tp __y)
    { return std::isless(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_islessequal(_Tp __x, _Tp __y)
    { return std::islessequal(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_islessgreater(_Tp __x,
								   _Tp __y)
    { return std::islessgreater(__x, __y); }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_isunordered(_Tp __x,
								 _Tp __y)
    { return std::isunordered(__x, __y); }

  // _S_increment & _S_decrement{{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    constexpr static void _S_increment(_Tp& __x)
    { ++__x; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC
    constexpr static void _S_decrement(_Tp& __x)
    { --__x; }


  // compares {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_equal_to(_Tp __x, _Tp __y)
    { return __x == __y; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_not_equal_to(_Tp __x,
								  _Tp __y)
    { return __x != __y; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_less(_Tp __x, _Tp __y)
    { return __x < __y; }

  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool _S_less_equal(_Tp __x,
								_Tp __y)
    { return __x <= __y; }

  // smart_reference access {{{2
  template <typename _Tp, typename _Up>
    _GLIBCXX_SIMD_INTRINSIC
    constexpr static void _S_set(_Tp& __v, [[maybe_unused]] int __i,
				 _Up&& __x) noexcept
    {
      _GLIBCXX_DEBUG_ASSERT(__i == 0);
      __v = static_cast<_Up&&>(__x);
    }

  // _S_masked_assign {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static void
    _S_masked_assign(bool __k, _Tp& __lhs, _Tp __rhs)
    { if (__k) __lhs = __rhs; }

  // _S_masked_cassign {{{2
  template <typename _Op, typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static void
    _S_masked_cassign(const bool __k, _Tp& __lhs, const _Tp __rhs, _Op __op)
    { if (__k) __lhs = __op(_SimdImplScalar{}, __lhs, __rhs); }

  // _S_masked_unary {{{2
  template <template <typename> class _Op, typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static _Tp _S_masked_unary(const bool __k,
								 const _Tp __v)
    { return static_cast<_Tp>(__k ? _Op<_Tp>{}(__v) : __v); }

  // }}}2
};

// }}}
// _MaskImplScalar {{{
struct _MaskImplScalar
{
  // member types {{{
  template <typename _Tp>
    using _TypeTag = _Tp*;

  // }}}
  // _S_broadcast {{{
  template <typename>
    _GLIBCXX_SIMD_INTRINSIC static constexpr bool _S_broadcast(bool __x)
    { return __x; }

  // }}}
  // _S_load {{{
  template <typename>
    _GLIBCXX_SIMD_INTRINSIC static constexpr bool _S_load(const bool* __mem)
    { return __mem[0]; }

  // }}}
  // _S_to_bits {{{
  _GLIBCXX_SIMD_INTRINSIC static constexpr _SanitizedBitMask<1>
  _S_to_bits(bool __x)
  { return __x; }

  // }}}
  // _S_convert {{{
  template <typename, bool _Sanitized>
    _GLIBCXX_SIMD_INTRINSIC static constexpr bool
    _S_convert(_BitMask<1, _Sanitized> __x)
    { return __x[0]; }

  template <typename, typename _Up, typename _UAbi>
    _GLIBCXX_SIMD_INTRINSIC static constexpr bool
    _S_convert(simd_mask<_Up, _UAbi> __x)
    { return __x[0]; }

  // }}}
  // _S_from_bitmask {{{2
  template <typename _Tp>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool
    _S_from_bitmask(_SanitizedBitMask<1> __bits, _TypeTag<_Tp>) noexcept
    { return __bits[0]; }

  // _S_masked_load {{{2
  _GLIBCXX_SIMD_INTRINSIC constexpr static bool
  _S_masked_load(bool __merge, bool __mask, const bool* __mem) noexcept
  {
    if (__mask)
      __merge = __mem[0];
    return __merge;
  }

  // _S_store {{{2
  _GLIBCXX_SIMD_INTRINSIC static void _S_store(bool __v, bool* __mem) noexcept
  { __mem[0] = __v; }

  // _S_masked_store {{{2
  _GLIBCXX_SIMD_INTRINSIC static void
  _S_masked_store(const bool __v, bool* __mem, const bool __k) noexcept
  {
    if (__k)
      __mem[0] = __v;
  }

  // logical and bitwise operators {{{2
  _GLIBCXX_SIMD_INTRINSIC
  static constexpr bool _S_logical_and(bool __x, bool __y)
  { return __x && __y; }

  _GLIBCXX_SIMD_INTRINSIC
  static constexpr bool _S_logical_or(bool __x, bool __y)
  { return __x || __y; }

  _GLIBCXX_SIMD_INTRINSIC
  static constexpr bool _S_bit_not(bool __x)
  { return !__x; }

  _GLIBCXX_SIMD_INTRINSIC
  static constexpr bool _S_bit_and(bool __x, bool __y)
  { return __x && __y; }

  _GLIBCXX_SIMD_INTRINSIC
  static constexpr bool _S_bit_or(bool __x, bool __y)
  { return __x || __y; }

  _GLIBCXX_SIMD_INTRINSIC
  static constexpr bool _S_bit_xor(bool __x, bool __y)
  { return __x != __y; }

  // smart_reference access {{{2
  _GLIBCXX_SIMD_INTRINSIC
  constexpr static void _S_set(bool& __k, [[maybe_unused]] int __i,
			       bool __x) noexcept
  {
    _GLIBCXX_DEBUG_ASSERT(__i == 0);
    __k = __x;
  }

  // _S_masked_assign {{{2
  _GLIBCXX_SIMD_INTRINSIC static void _S_masked_assign(bool __k, bool& __lhs,
						       bool __rhs)
  {
    if (__k)
      __lhs = __rhs;
  }

  // }}}2
  // _S_all_of {{{
  template <typename _Tp, typename _Abi>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool
    _S_all_of(simd_mask<_Tp, _Abi> __k)
    { return __k._M_data; }

  // }}}
  // _S_any_of {{{
  template <typename _Tp, typename _Abi>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool
    _S_any_of(simd_mask<_Tp, _Abi> __k)
    { return __k._M_data; }

  // }}}
  // _S_none_of {{{
  template <typename _Tp, typename _Abi>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool
    _S_none_of(simd_mask<_Tp, _Abi> __k)
    { return !__k._M_data; }

  // }}}
  // _S_some_of {{{
  template <typename _Tp, typename _Abi>
    _GLIBCXX_SIMD_INTRINSIC constexpr static bool
    _S_some_of(simd_mask<_Tp, _Abi>)
    { return false; }

  // }}}
  // _S_popcount {{{
  template <typename _Tp, typename _Abi>
    _GLIBCXX_SIMD_INTRINSIC constexpr static int
    _S_popcount(simd_mask<_Tp, _Abi> __k)
    { return __k._M_data; }

  // }}}
  // _S_find_first_set {{{
  template <typename _Tp, typename _Abi>
    _GLIBCXX_SIMD_INTRINSIC constexpr static int
    _S_find_first_set(simd_mask<_Tp, _Abi>)
    { return 0; }

  // }}}
  // _S_find_last_set {{{
  template <typename _Tp, typename _Abi>
    _GLIBCXX_SIMD_INTRINSIC constexpr static int
    _S_find_last_set(simd_mask<_Tp, _Abi>)
    { return 0; }

  // }}}
};

// }}}

_GLIBCXX_SIMD_END_NAMESPACE
#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_SCALAR_H_

// vim: foldmethod=marker sw=2 noet ts=8 sts=2 tw=80
