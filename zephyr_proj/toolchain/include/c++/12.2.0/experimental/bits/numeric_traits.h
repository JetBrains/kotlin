// Definition of numeric_limits replacement traits P1841R1 -*- C++ -*-

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

#include <type_traits>

namespace std {

template <template <typename> class _Trait, typename _Tp, typename = void>
  struct __value_exists_impl : false_type {};

template <template <typename> class _Trait, typename _Tp>
  struct __value_exists_impl<_Trait, _Tp, void_t<decltype(_Trait<_Tp>::value)>>
  : true_type {};

template <typename _Tp, bool = is_arithmetic_v<_Tp>>
  struct __digits_impl {};

template <typename _Tp>
  struct __digits_impl<_Tp, true>
  {
    static inline constexpr int value
      = sizeof(_Tp) * __CHAR_BIT__ - is_signed_v<_Tp>;
  };

template <>
  struct __digits_impl<float, true>
  { static inline constexpr int value = __FLT_MANT_DIG__; };

template <>
  struct __digits_impl<double, true>
  { static inline constexpr int value = __DBL_MANT_DIG__; };

template <>
  struct __digits_impl<long double, true>
  { static inline constexpr int value = __LDBL_MANT_DIG__; };

template <typename _Tp, bool = is_arithmetic_v<_Tp>>
  struct __digits10_impl {};

template <typename _Tp>
  struct __digits10_impl<_Tp, true>
  {
    // The fraction 643/2136 approximates log10(2) to 7 significant digits.
    static inline constexpr int value = __digits_impl<_Tp>::value * 643L / 2136;
  };

template <>
  struct __digits10_impl<float, true>
  { static inline constexpr int value = __FLT_DIG__; };

template <>
  struct __digits10_impl<double, true>
  { static inline constexpr int value = __DBL_DIG__; };

template <>
  struct __digits10_impl<long double, true>
  { static inline constexpr int value = __LDBL_DIG__; };

template <typename _Tp, bool = is_arithmetic_v<_Tp>>
  struct __max_digits10_impl {};

template <typename _Tp>
  struct __max_digits10_impl<_Tp, true>
  {
    static inline constexpr int value
      = is_floating_point_v<_Tp> ? 2 + __digits_impl<_Tp>::value * 643L / 2136
				 : __digits10_impl<_Tp>::value + 1;
  };

template <typename _Tp>
  struct __max_exponent_impl {};

template <>
  struct __max_exponent_impl<float>
  { static inline constexpr int value = __FLT_MAX_EXP__; };

template <>
  struct __max_exponent_impl<double>
  { static inline constexpr int value = __DBL_MAX_EXP__; };

template <>
  struct __max_exponent_impl<long double>
  { static inline constexpr int value = __LDBL_MAX_EXP__; };

template <typename _Tp>
  struct __max_exponent10_impl {};

template <>
  struct __max_exponent10_impl<float>
  { static inline constexpr int value = __FLT_MAX_10_EXP__; };

template <>
  struct __max_exponent10_impl<double>
  { static inline constexpr int value = __DBL_MAX_10_EXP__; };

template <>
  struct __max_exponent10_impl<long double>
  { static inline constexpr int value = __LDBL_MAX_10_EXP__; };

template <typename _Tp>
  struct __min_exponent_impl {};

template <>
  struct __min_exponent_impl<float>
  { static inline constexpr int value = __FLT_MIN_EXP__; };

template <>
  struct __min_exponent_impl<double>
  { static inline constexpr int value = __DBL_MIN_EXP__; };

template <>
  struct __min_exponent_impl<long double>
  { static inline constexpr int value = __LDBL_MIN_EXP__; };

template <typename _Tp>
  struct __min_exponent10_impl {};

template <>
  struct __min_exponent10_impl<float>
  { static inline constexpr int value = __FLT_MIN_10_EXP__; };

template <>
  struct __min_exponent10_impl<double>
  { static inline constexpr int value = __DBL_MIN_10_EXP__; };

template <>
  struct __min_exponent10_impl<long double>
  { static inline constexpr int value = __LDBL_MIN_10_EXP__; };

template <typename _Tp, bool = is_arithmetic_v<_Tp>>
  struct __radix_impl {};

template <typename _Tp>
  struct __radix_impl<_Tp, true>
  {
    static inline constexpr int value
      = is_floating_point_v<_Tp> ? __FLT_RADIX__ : 2;
  };

// [num.traits.util], numeric utility traits
template <template <typename> class _Trait, typename _Tp>
  struct __value_exists : __value_exists_impl<_Trait, _Tp> {};

template <template <typename> class _Trait, typename _Tp>
  inline constexpr bool __value_exists_v = __value_exists<_Trait, _Tp>::value;

template <template <typename> class _Trait, typename _Tp, typename _Up = _Tp>
  inline constexpr _Up
  __value_or(_Up __def = _Up()) noexcept
  {
    if constexpr (__value_exists_v<_Trait, _Tp>)
      return static_cast<_Up>(_Trait<_Tp>::value);
    else
      return __def;
  }

template <typename _Tp, bool = is_arithmetic_v<_Tp>>
  struct __norm_min_impl {};

template <typename _Tp>
  struct __norm_min_impl<_Tp, true>
  { static inline constexpr _Tp value = 1; };

template <>
  struct __norm_min_impl<float, true>
  { static inline constexpr float value = __FLT_MIN__; };

template <>
  struct __norm_min_impl<double, true>
  { static inline constexpr double value = __DBL_MIN__; };

template <>
  struct __norm_min_impl<long double, true>
  { static inline constexpr long double value = __LDBL_MIN__; };

template <typename _Tp>
  struct __denorm_min_impl : __norm_min_impl<_Tp> {};

#if __FLT_HAS_DENORM__
template <>
  struct __denorm_min_impl<float>
  { static inline constexpr float value = __FLT_DENORM_MIN__; };
#endif

#if __DBL_HAS_DENORM__
template <>
  struct __denorm_min_impl<double>
  { static inline constexpr double value = __DBL_DENORM_MIN__; };
#endif

#if __LDBL_HAS_DENORM__
template <>
  struct __denorm_min_impl<long double>
  { static inline constexpr long double value = __LDBL_DENORM_MIN__; };
#endif

template <typename _Tp>
  struct __epsilon_impl {};

template <>
  struct __epsilon_impl<float>
  { static inline constexpr float value = __FLT_EPSILON__; };

template <>
  struct __epsilon_impl<double>
  { static inline constexpr double value = __DBL_EPSILON__; };

template <>
  struct __epsilon_impl<long double>
  { static inline constexpr long double value = __LDBL_EPSILON__; };

template <typename _Tp, bool = is_arithmetic_v<_Tp>>
  struct __finite_min_impl {};

template <typename _Tp>
  struct __finite_min_impl<_Tp, true>
  {
    static inline constexpr _Tp value
      = is_unsigned_v<_Tp> ? _Tp()
			   : -2 * (_Tp(1) << __digits_impl<_Tp>::value - 1);
  };

template <>
  struct __finite_min_impl<float, true>
  { static inline constexpr float value = -__FLT_MAX__; };

template <>
  struct __finite_min_impl<double, true>
  { static inline constexpr double value = -__DBL_MAX__; };

template <>
  struct __finite_min_impl<long double, true>
  { static inline constexpr long double value = -__LDBL_MAX__; };

template <typename _Tp, bool = is_arithmetic_v<_Tp>>
  struct __finite_max_impl {};

template <typename _Tp>
  struct __finite_max_impl<_Tp, true>
  { static inline constexpr _Tp value = ~__finite_min_impl<_Tp>::value; };

template <>
  struct __finite_max_impl<float, true>
  { static inline constexpr float value = __FLT_MAX__; };

template <>
  struct __finite_max_impl<double, true>
  { static inline constexpr double value = __DBL_MAX__; };

template <>
  struct __finite_max_impl<long double, true>
  { static inline constexpr long double value = __LDBL_MAX__; };

template <typename _Tp>
  struct __infinity_impl {};

#if __FLT_HAS_INFINITY__
template <>
  struct __infinity_impl<float>
  { static inline constexpr float value = __builtin_inff(); };
#endif

#if __DBL_HAS_INFINITY__
template <>
  struct __infinity_impl<double>
  { static inline constexpr double value = __builtin_inf(); };
#endif

#if __LDBL_HAS_INFINITY__
template <>
  struct __infinity_impl<long double>
  { static inline constexpr long double value = __builtin_infl(); };
#endif

template <typename _Tp>
  struct __quiet_NaN_impl {};

#if __FLT_HAS_QUIET_NAN__
template <>
  struct __quiet_NaN_impl<float>
  { static inline constexpr float value = __builtin_nanf(""); };
#endif

#if __DBL_HAS_QUIET_NAN__
template <>
  struct __quiet_NaN_impl<double>
  { static inline constexpr double value = __builtin_nan(""); };
#endif

#if __LDBL_HAS_QUIET_NAN__
template <>
  struct __quiet_NaN_impl<long double>
  { static inline constexpr long double value = __builtin_nanl(""); };
#endif

template <typename _Tp, bool = is_floating_point_v<_Tp>>
  struct __reciprocal_overflow_threshold_impl {};

template <typename _Tp>
  struct __reciprocal_overflow_threshold_impl<_Tp, true>
  {
    // This typically yields a subnormal value. Is this incorrect for
    // flush-to-zero configurations?
    static constexpr _Tp _S_search(_Tp __ok, _Tp __overflows)
    {
      const _Tp __mid = (__ok + __overflows) / 2;
      // 1/__mid without -ffast-math is not a constant expression if it
      // overflows. Therefore divide 1 by the radix before division.
      // Consequently finite_max (the threshold) must be scaled by the
      // same value.
      if (__mid == __ok || __mid == __overflows)
	return __ok;
      else if (_Tp(1) / (__radix_impl<_Tp>::value * __mid)
	       <= __finite_max_impl<_Tp>::value / __radix_impl<_Tp>::value)
	return _S_search(__mid, __overflows);
      else
	return _S_search(__ok, __mid);
    }

    static inline constexpr _Tp value
      = _S_search(_Tp(1.01) / __finite_max_impl<_Tp>::value,
		  _Tp(0.99) / __finite_max_impl<_Tp>::value);
  };

template <typename _Tp, bool = is_floating_point_v<_Tp>>
  struct __round_error_impl {};

template <typename _Tp>
  struct __round_error_impl<_Tp, true>
  { static inline constexpr _Tp value = 0.5; };

template <typename _Tp>
  struct __signaling_NaN_impl {};

#if __FLT_HAS_QUIET_NAN__
template <>
  struct __signaling_NaN_impl<float>
  { static inline constexpr float value = __builtin_nansf(""); };
#endif

#if __DBL_HAS_QUIET_NAN__
template <>
  struct __signaling_NaN_impl<double>
  { static inline constexpr double value = __builtin_nans(""); };
#endif

#if __LDBL_HAS_QUIET_NAN__
template <>
  struct __signaling_NaN_impl<long double>
  { static inline constexpr long double value = __builtin_nansl(""); };
#endif

// [num.traits.val], numeric distinguished value traits
template <typename _Tp>
  struct __denorm_min : __denorm_min_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __epsilon : __epsilon_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __finite_max : __finite_max_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __finite_min : __finite_min_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __infinity : __infinity_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __norm_min : __norm_min_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __quiet_NaN : __quiet_NaN_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __reciprocal_overflow_threshold
  : __reciprocal_overflow_threshold_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __round_error : __round_error_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __signaling_NaN : __signaling_NaN_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  inline constexpr auto __denorm_min_v = __denorm_min<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __epsilon_v = __epsilon<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __finite_max_v = __finite_max<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __finite_min_v = __finite_min<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __infinity_v = __infinity<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __norm_min_v = __norm_min<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __quiet_NaN_v = __quiet_NaN<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __reciprocal_overflow_threshold_v
    = __reciprocal_overflow_threshold<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __round_error_v = __round_error<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __signaling_NaN_v = __signaling_NaN<_Tp>::value;

// [num.traits.char], numeric characteristics traits
template <typename _Tp>
  struct __digits : __digits_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __digits10 : __digits10_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __max_digits10 : __max_digits10_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __max_exponent : __max_exponent_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __max_exponent10 : __max_exponent10_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __min_exponent : __min_exponent_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __min_exponent10 : __min_exponent10_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  struct __radix : __radix_impl<remove_cv_t<_Tp>> {};

template <typename _Tp>
  inline constexpr auto __digits_v = __digits<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __digits10_v = __digits10<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __max_digits10_v = __max_digits10<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __max_exponent_v = __max_exponent<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __max_exponent10_v = __max_exponent10<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __min_exponent_v = __min_exponent<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __min_exponent10_v = __min_exponent10<_Tp>::value;

template <typename _Tp>
  inline constexpr auto __radix_v = __radix<_Tp>::value;

// mkretz's extensions
// TODO: does GCC tell me? __GCC_IEC_559 >= 2 is not the right answer
template <typename _Tp>
  struct __has_iec559_storage_format : true_type {};

template <typename _Tp>
  inline constexpr bool __has_iec559_storage_format_v
    = __has_iec559_storage_format<_Tp>::value;

/* To propose:
   If __has_iec559_behavior<__quiet_NaN, T> is true the following holds:
     - nan == nan is false
     - isnan(nan) is true
     - isnan(nan + x) is true
     - isnan(inf/inf) is true
     - isnan(0/0) is true
     - isunordered(nan, x) is true

   If __has_iec559_behavior<__infinity, T> is true the following holds (x is
   neither nan nor inf):
     - isinf(inf) is true
     - isinf(inf + x) is true
     - isinf(1/0) is true
 */
template <template <typename> class _Trait, typename _Tp>
  struct __has_iec559_behavior : false_type {};

template <template <typename> class _Trait, typename _Tp>
  inline constexpr bool __has_iec559_behavior_v
    = __has_iec559_behavior<_Trait, _Tp>::value;

#if !__FINITE_MATH_ONLY__
#if __FLT_HAS_QUIET_NAN__
template <>
  struct __has_iec559_behavior<__quiet_NaN, float> : true_type {};
#endif

#if __DBL_HAS_QUIET_NAN__
template <>
  struct __has_iec559_behavior<__quiet_NaN, double> : true_type {};
#endif

#if __LDBL_HAS_QUIET_NAN__
template <>
  struct __has_iec559_behavior<__quiet_NaN, long double> : true_type {};
#endif

#if __FLT_HAS_INFINITY__
template <>
  struct __has_iec559_behavior<__infinity, float> : true_type {};
#endif

#if __DBL_HAS_INFINITY__
template <>
  struct __has_iec559_behavior<__infinity, double> : true_type {};
#endif

#if __LDBL_HAS_INFINITY__
template <>
  struct __has_iec559_behavior<__infinity, long double> : true_type {};
#endif

#ifdef __SUPPORT_SNAN__
#if __FLT_HAS_QUIET_NAN__
template <>
  struct __has_iec559_behavior<__signaling_NaN, float> : true_type {};
#endif

#if __DBL_HAS_QUIET_NAN__
template <>
  struct __has_iec559_behavior<__signaling_NaN, double> : true_type {};
#endif

#if __LDBL_HAS_QUIET_NAN__
template <>
  struct __has_iec559_behavior<__signaling_NaN, long double> : true_type {};
#endif

#endif
#endif // __FINITE_MATH_ONLY__

} // namespace std
