// Math overloads for simd -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_MATH_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_MATH_H_

#if __cplusplus >= 201703L

#include <utility>
#include <iomanip>

_GLIBCXX_SIMD_BEGIN_NAMESPACE
template <typename _Tp, typename _V>
  using _Samesize = fixed_size_simd<_Tp, _V::size()>;

// _Math_return_type {{{
template <typename _DoubleR, typename _Tp, typename _Abi>
  struct _Math_return_type;

template <typename _DoubleR, typename _Tp, typename _Abi>
  using _Math_return_type_t =
    typename _Math_return_type<_DoubleR, _Tp, _Abi>::type;

template <typename _Tp, typename _Abi>
  struct _Math_return_type<double, _Tp, _Abi>
  { using type = simd<_Tp, _Abi>; };

template <typename _Tp, typename _Abi>
  struct _Math_return_type<bool, _Tp, _Abi>
  { using type = simd_mask<_Tp, _Abi>; };

template <typename _DoubleR, typename _Tp, typename _Abi>
  struct _Math_return_type
  { using type = fixed_size_simd<_DoubleR, simd_size_v<_Tp, _Abi>>; };

//}}}
// _GLIBCXX_SIMD_MATH_CALL_ {{{
#define _GLIBCXX_SIMD_MATH_CALL_(__name)                                       \
template <typename _Tp, typename _Abi, typename...,                            \
	  typename _R = _Math_return_type_t<                                   \
	    decltype(std::__name(declval<double>())), _Tp, _Abi>>              \
  _GLIBCXX_SIMD_ALWAYS_INLINE                                                  \
  enable_if_t<is_floating_point_v<_Tp>, _R>                                    \
  __name(simd<_Tp, _Abi> __x)                                                  \
  { return {__private_init, _Abi::_SimdImpl::_S_##__name(__data(__x))}; }

// }}}
//_Extra_argument_type{{{
template <typename _Up, typename _Tp, typename _Abi>
  struct _Extra_argument_type;

template <typename _Tp, typename _Abi>
  struct _Extra_argument_type<_Tp*, _Tp, _Abi>
  {
    using type = simd<_Tp, _Abi>*;
    static constexpr double* declval();
    static constexpr bool __needs_temporary_scalar = true;

    _GLIBCXX_SIMD_INTRINSIC static constexpr auto _S_data(type __x)
    { return &__data(*__x); }
  };

template <typename _Up, typename _Tp, typename _Abi>
  struct _Extra_argument_type<_Up*, _Tp, _Abi>
  {
    static_assert(is_integral_v<_Up>);
    using type = fixed_size_simd<_Up, simd_size_v<_Tp, _Abi>>*;
    static constexpr _Up* declval();
    static constexpr bool __needs_temporary_scalar = true;

    _GLIBCXX_SIMD_INTRINSIC static constexpr auto _S_data(type __x)
    { return &__data(*__x); }
  };

template <typename _Tp, typename _Abi>
  struct _Extra_argument_type<_Tp, _Tp, _Abi>
  {
    using type = simd<_Tp, _Abi>;
    static constexpr double declval();
    static constexpr bool __needs_temporary_scalar = false;

    _GLIBCXX_SIMD_INTRINSIC static constexpr decltype(auto)
    _S_data(const type& __x)
    { return __data(__x); }
  };

template <typename _Up, typename _Tp, typename _Abi>
  struct _Extra_argument_type
  {
    static_assert(is_integral_v<_Up>);
    using type = fixed_size_simd<_Up, simd_size_v<_Tp, _Abi>>;
    static constexpr _Up declval();
    static constexpr bool __needs_temporary_scalar = false;

    _GLIBCXX_SIMD_INTRINSIC static constexpr decltype(auto)
    _S_data(const type& __x)
    { return __data(__x); }
  };

//}}}
// _GLIBCXX_SIMD_MATH_CALL2_ {{{
#define _GLIBCXX_SIMD_MATH_CALL2_(__name, __arg2)                              \
template <                                                                     \
  typename _Tp, typename _Abi, typename...,                                    \
  typename _Arg2 = _Extra_argument_type<__arg2, _Tp, _Abi>,                    \
  typename _R = _Math_return_type_t<                                           \
    decltype(std::__name(declval<double>(), _Arg2::declval())), _Tp, _Abi>>    \
  _GLIBCXX_SIMD_ALWAYS_INLINE                                                  \
  enable_if_t<is_floating_point_v<_Tp>, _R>                                    \
  __name(const simd<_Tp, _Abi>& __x, const typename _Arg2::type& __y)          \
  {                                                                            \
    return {__private_init,                                                    \
	    _Abi::_SimdImpl::_S_##__name(__data(__x), _Arg2::_S_data(__y))};   \
  }                                                                            \
template <typename _Up, typename _Tp, typename _Abi>                           \
  _GLIBCXX_SIMD_INTRINSIC _Math_return_type_t<                                 \
    decltype(std::__name(                                                      \
      declval<double>(),                                                       \
      declval<enable_if_t<                                                     \
	conjunction_v<                                                         \
	  is_same<__arg2, _Tp>,                                                \
	  negation<is_same<__remove_cvref_t<_Up>, simd<_Tp, _Abi>>>,           \
	  is_convertible<_Up, simd<_Tp, _Abi>>, is_floating_point<_Tp>>,       \
	double>>())),                                                          \
    _Tp, _Abi>                                                                 \
  __name(_Up&& __xx, const simd<_Tp, _Abi>& __yy)                              \
  { return __name(simd<_Tp, _Abi>(static_cast<_Up&&>(__xx)), __yy); }

// }}}
// _GLIBCXX_SIMD_MATH_CALL3_ {{{
#define _GLIBCXX_SIMD_MATH_CALL3_(__name, __arg2, __arg3)                      \
template <typename _Tp, typename _Abi, typename...,                            \
	  typename _Arg2 = _Extra_argument_type<__arg2, _Tp, _Abi>,            \
	  typename _Arg3 = _Extra_argument_type<__arg3, _Tp, _Abi>,            \
	  typename _R = _Math_return_type_t<                                   \
	    decltype(std::__name(declval<double>(), _Arg2::declval(),          \
				 _Arg3::declval())),                           \
	    _Tp, _Abi>>                                                        \
  _GLIBCXX_SIMD_ALWAYS_INLINE                                                  \
  enable_if_t<is_floating_point_v<_Tp>, _R>                                    \
  __name(const simd<_Tp, _Abi>& __x, const typename _Arg2::type& __y,          \
	 const typename _Arg3::type& __z)                                      \
  {                                                                            \
    return {__private_init,                                                    \
	    _Abi::_SimdImpl::_S_##__name(__data(__x), _Arg2::_S_data(__y),     \
					 _Arg3::_S_data(__z))};                \
  }                                                                            \
template <                                                                     \
  typename _T0, typename _T1, typename _T2, typename...,                       \
  typename _U0 = __remove_cvref_t<_T0>,                                        \
  typename _U1 = __remove_cvref_t<_T1>,                                        \
  typename _U2 = __remove_cvref_t<_T2>,                                        \
  typename _Simd = conditional_t<is_simd_v<_U1>, _U1, _U2>,                    \
  typename = enable_if_t<conjunction_v<                                        \
    is_simd<_Simd>, is_convertible<_T0&&, _Simd>,                              \
    is_convertible<_T1&&, _Simd>, is_convertible<_T2&&, _Simd>,                \
    negation<conjunction<                                                      \
      is_simd<_U0>, is_floating_point<__value_type_or_identity_t<_U0>>>>>>>    \
  _GLIBCXX_SIMD_INTRINSIC decltype(__name(declval<const _Simd&>(),             \
					  declval<const _Simd&>(),             \
					  declval<const _Simd&>()))            \
  __name(_T0&& __xx, _T1&& __yy, _T2&& __zz)                                   \
  {                                                                            \
    return __name(_Simd(static_cast<_T0&&>(__xx)),                             \
		  _Simd(static_cast<_T1&&>(__yy)),                             \
		  _Simd(static_cast<_T2&&>(__zz)));                            \
  }

// }}}
// __cosSeries {{{
template <typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE static simd<float, _Abi>
  __cosSeries(const simd<float, _Abi>& __x)
  {
    const simd<float, _Abi> __x2 = __x * __x;
    simd<float, _Abi> __y;
    __y = 0x1.ap-16f;                  //  1/8!
    __y = __y * __x2 - 0x1.6c1p-10f;   // -1/6!
    __y = __y * __x2 + 0x1.555556p-5f; //  1/4!
    return __y * (__x2 * __x2) - .5f * __x2 + 1.f;
  }

template <typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE static simd<double, _Abi>
  __cosSeries(const simd<double, _Abi>& __x)
  {
    const simd<double, _Abi> __x2 = __x * __x;
    simd<double, _Abi> __y;
    __y = 0x1.AC00000000000p-45;              //  1/16!
    __y = __y * __x2 - 0x1.9394000000000p-37; // -1/14!
    __y = __y * __x2 + 0x1.1EED8C0000000p-29; //  1/12!
    __y = __y * __x2 - 0x1.27E4FB7400000p-22; // -1/10!
    __y = __y * __x2 + 0x1.A01A01A018000p-16; //  1/8!
    __y = __y * __x2 - 0x1.6C16C16C16C00p-10; // -1/6!
    __y = __y * __x2 + 0x1.5555555555554p-5;  //  1/4!
    return (__y * __x2 - .5f) * __x2 + 1.f;
  }

// }}}
// __sinSeries {{{
template <typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE static simd<float, _Abi>
  __sinSeries(const simd<float, _Abi>& __x)
  {
    const simd<float, _Abi> __x2 = __x * __x;
    simd<float, _Abi> __y;
    __y = -0x1.9CC000p-13f;            // -1/7!
    __y = __y * __x2 + 0x1.111100p-7f; //  1/5!
    __y = __y * __x2 - 0x1.555556p-3f; // -1/3!
    return __y * (__x2 * __x) + __x;
  }

template <typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE static simd<double, _Abi>
  __sinSeries(const simd<double, _Abi>& __x)
  {
    // __x  = [0, 0.7854 = pi/4]
    // __x² = [0, 0.6169 = pi²/8]
    const simd<double, _Abi> __x2 = __x * __x;
    simd<double, _Abi> __y;
    __y = -0x1.ACF0000000000p-41;             // -1/15!
    __y = __y * __x2 + 0x1.6124400000000p-33; //  1/13!
    __y = __y * __x2 - 0x1.AE64567000000p-26; // -1/11!
    __y = __y * __x2 + 0x1.71DE3A5540000p-19; //  1/9!
    __y = __y * __x2 - 0x1.A01A01A01A000p-13; // -1/7!
    __y = __y * __x2 + 0x1.1111111111110p-7;  //  1/5!
    __y = __y * __x2 - 0x1.5555555555555p-3;  // -1/3!
    return __y * (__x2 * __x) + __x;
  }

// }}}
// __zero_low_bits {{{
template <int _Bits, typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi>
  __zero_low_bits(simd<_Tp, _Abi> __x)
  {
    const simd<_Tp, _Abi> __bitmask
      = __bit_cast<_Tp>(~make_unsigned_t<__int_for_sizeof_t<_Tp>>() << _Bits);
    return {__private_init,
	    _Abi::_SimdImpl::_S_bit_and(__data(__x), __data(__bitmask))};
  }

// }}}
// __fold_input {{{

/**@internal
 * Fold @p x into [-¼π, ¼π] and remember the quadrant it came from:
 * quadrant 0: [-¼π,  ¼π]
 * quadrant 1: [ ¼π,  ¾π]
 * quadrant 2: [ ¾π, 1¼π]
 * quadrant 3: [1¼π, 1¾π]
 *
 * The algorithm determines `y` as the multiple `x - y * ¼π = [-¼π, ¼π]`. Using
 * a bitmask, `y` is reduced to `quadrant`. `y` can be calculated as
 * ```
 * y = trunc(x / ¼π);
 * y += fmod(y, 2);
 * ```
 * This can be simplified by moving the (implicit) division by 2 into the
 * truncation expression. The `+= fmod` effect can the be achieved by using
 * rounding instead of truncation: `y = round(x / ½π) * 2`. If precision allows,
 * `2/π * x` is better (faster).
 */
template <typename _Tp, typename _Abi>
  struct _Folded
  {
    simd<_Tp, _Abi> _M_x;
    rebind_simd_t<int, simd<_Tp, _Abi>> _M_quadrant;
  };

namespace __math_float {
inline constexpr float __pi_over_4 = 0x1.921FB6p-1f; // π/4
inline constexpr float __2_over_pi = 0x1.45F306p-1f; // 2/π
inline constexpr float __pi_2_5bits0
  = 0x1.921fc0p0f; // π/2, 5 0-bits (least significant)
inline constexpr float __pi_2_5bits0_rem
  = -0x1.5777a6p-21f; // π/2 - __pi_2_5bits0
} // namespace __math_float
namespace __math_double {
inline constexpr double __pi_over_4 = 0x1.921fb54442d18p-1; // π/4
inline constexpr double __2_over_pi = 0x1.45F306DC9C883p-1; // 2/π
inline constexpr double __pi_2 = 0x1.921fb54442d18p0;       // π/2
} // namespace __math_double

template <typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _Folded<float, _Abi>
  __fold_input(const simd<float, _Abi>& __x)
  {
    using _V = simd<float, _Abi>;
    using _IV = rebind_simd_t<int, _V>;
    using namespace __math_float;
    _Folded<float, _Abi> __r;
    __r._M_x = abs(__x);
#if 0
    // zero most mantissa bits:
    constexpr float __1_over_pi = 0x1.45F306p-2f; // 1/π
    const auto __y = (__r._M_x * __1_over_pi + 0x1.8p23f) - 0x1.8p23f;
    // split π into 4 parts, the first three with 13 trailing zeros (to make the
    // following multiplications precise):
    constexpr float __pi0 = 0x1.920000p1f;
    constexpr float __pi1 = 0x1.fb4000p-11f;
    constexpr float __pi2 = 0x1.444000p-23f;
    constexpr float __pi3 = 0x1.68c234p-38f;
    __r._M_x - __y*__pi0 - __y*__pi1 - __y*__pi2 - __y*__pi3
#else
    if (_GLIBCXX_SIMD_IS_UNLIKELY(all_of(__r._M_x < __pi_over_4)))
      __r._M_quadrant = 0;
    else if (_GLIBCXX_SIMD_IS_LIKELY(all_of(__r._M_x < 6 * __pi_over_4)))
      {
	const _V __y = nearbyint(__r._M_x * __2_over_pi);
	__r._M_quadrant = static_simd_cast<_IV>(__y) & 3; // __y mod 4
	__r._M_x -= __y * __pi_2_5bits0;
	__r._M_x -= __y * __pi_2_5bits0_rem;
      }
    else
      {
	using __math_double::__2_over_pi;
	using __math_double::__pi_2;
	using _VD = rebind_simd_t<double, _V>;
	_VD __xd = static_simd_cast<_VD>(__r._M_x);
	_VD __y = nearbyint(__xd * __2_over_pi);
	__r._M_quadrant = static_simd_cast<_IV>(__y) & 3; // = __y mod 4
	__r._M_x = static_simd_cast<_V>(__xd - __y * __pi_2);
      }
#endif
    return __r;
  }

template <typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE _Folded<double, _Abi>
  __fold_input(const simd<double, _Abi>& __x)
  {
    using _V = simd<double, _Abi>;
    using _IV = rebind_simd_t<int, _V>;
    using namespace __math_double;

    _Folded<double, _Abi> __r;
    __r._M_x = abs(__x);
    if (_GLIBCXX_SIMD_IS_UNLIKELY(all_of(__r._M_x < __pi_over_4)))
      {
	__r._M_quadrant = 0;
	return __r;
      }
    const _V __y = nearbyint(__r._M_x / (2 * __pi_over_4));
    __r._M_quadrant = static_simd_cast<_IV>(__y) & 3;

    if (_GLIBCXX_SIMD_IS_LIKELY(all_of(__r._M_x < 1025 * __pi_over_4)))
      {
	// x - y * pi/2, y uses no more than 11 mantissa bits
	__r._M_x -= __y * 0x1.921FB54443000p0;
	__r._M_x -= __y * -0x1.73DCB3B39A000p-43;
	__r._M_x -= __y * 0x1.45C06E0E68948p-86;
      }
    else if (_GLIBCXX_SIMD_IS_LIKELY(all_of(__y <= 0x1.0p30)))
      {
	// x - y * pi/2, y uses no more than 29 mantissa bits
	__r._M_x -= __y * 0x1.921FB40000000p0;
	__r._M_x -= __y * 0x1.4442D00000000p-24;
	__r._M_x -= __y * 0x1.8469898CC5170p-48;
      }
    else
      {
	// x - y * pi/2, y may require all mantissa bits
	const _V __y_hi = __zero_low_bits<26>(__y);
	const _V __y_lo = __y - __y_hi;
	const auto __pi_2_1 = 0x1.921FB50000000p0;
	const auto __pi_2_2 = 0x1.110B460000000p-26;
	const auto __pi_2_3 = 0x1.1A62630000000p-54;
	const auto __pi_2_4 = 0x1.8A2E03707344Ap-81;
	__r._M_x = __r._M_x - __y_hi * __pi_2_1
		   - max(__y_hi * __pi_2_2, __y_lo * __pi_2_1)
		   - min(__y_hi * __pi_2_2, __y_lo * __pi_2_1)
		   - max(__y_hi * __pi_2_3, __y_lo * __pi_2_2)
		   - min(__y_hi * __pi_2_3, __y_lo * __pi_2_2)
		   - max(__y * __pi_2_4, __y_lo * __pi_2_3)
		   - min(__y * __pi_2_4, __y_lo * __pi_2_3);
      }
    return __r;
  }

// }}}
// __extract_exponent_as_int {{{
template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_INTRINSIC
  rebind_simd_t<int, simd<_Tp, _Abi>>
  __extract_exponent_as_int(const simd<_Tp, _Abi>& __v)
  {
    using _Vp = simd<_Tp, _Abi>;
    using _Up = make_unsigned_t<__int_for_sizeof_t<_Tp>>;
    using namespace std::experimental::__float_bitwise_operators;
    using namespace std::experimental::__proposed;
    const _Vp __exponent_mask
      = __infinity_v<_Tp>; // 0x7f800000 or 0x7ff0000000000000
    return static_simd_cast<rebind_simd_t<int, _Vp>>(
	     simd_bit_cast<rebind_simd_t<_Up, _Vp>>(__v & __exponent_mask)
      >> (__digits_v<_Tp> - 1));
  }

// }}}
// __impl_or_fallback {{{
template <typename ImplFun, typename FallbackFun, typename... _Args>
  _GLIBCXX_SIMD_INTRINSIC auto
  __impl_or_fallback_dispatch(int, ImplFun&& __impl_fun, FallbackFun&&,
			      _Args&&... __args)
    -> decltype(__impl_fun(static_cast<_Args&&>(__args)...))
  { return __impl_fun(static_cast<_Args&&>(__args)...); }

template <typename ImplFun, typename FallbackFun, typename... _Args,
	  typename = __detail::__odr_helper>
  inline auto
  __impl_or_fallback_dispatch(float, ImplFun&&, FallbackFun&& __fallback_fun,
			      _Args&&... __args)
    -> decltype(__fallback_fun(static_cast<_Args&&>(__args)...))
  { return __fallback_fun(static_cast<_Args&&>(__args)...); }

template <typename... _Args>
  _GLIBCXX_SIMD_INTRINSIC auto
  __impl_or_fallback(_Args&&... __args)
  {
    return __impl_or_fallback_dispatch(int(), static_cast<_Args&&>(__args)...);
  }
//}}}

// trigonometric functions {{{
_GLIBCXX_SIMD_MATH_CALL_(acos)
_GLIBCXX_SIMD_MATH_CALL_(asin)
_GLIBCXX_SIMD_MATH_CALL_(atan)
_GLIBCXX_SIMD_MATH_CALL2_(atan2, _Tp)

/*
 * algorithm for sine and cosine:
 *
 * The result can be calculated with sine or cosine depending on the π/4 section
 * the input is in. sine   ≈ __x + __x³ cosine ≈ 1 - __x²
 *
 * sine:
 * Map -__x to __x and invert the output
 * Extend precision of __x - n * π/4 by calculating
 * ((__x - n * p1) - n * p2) - n * p3 (p1 + p2 + p3 = π/4)
 *
 * Calculate Taylor series with tuned coefficients.
 * Fix sign.
 */
// cos{{{
template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  cos(const simd<_Tp, _Abi>& __x)
  {
    using _V = simd<_Tp, _Abi>;
    if constexpr (__is_scalar_abi<_Abi>() || __is_fixed_size_abi_v<_Abi>)
      return {__private_init, _Abi::_SimdImpl::_S_cos(__data(__x))};
    else
      {
	if constexpr (is_same_v<_Tp, float>)
	  if (_GLIBCXX_SIMD_IS_UNLIKELY(any_of(abs(__x) >= 393382)))
	    return static_simd_cast<_V>(
	      cos(static_simd_cast<rebind_simd_t<double, _V>>(__x)));

	const auto __f = __fold_input(__x);
	// quadrant | effect
	//        0 | cosSeries, +
	//        1 | sinSeries, -
	//        2 | cosSeries, -
	//        3 | sinSeries, +
	using namespace std::experimental::__float_bitwise_operators;
	const _V __sign_flip
	  = _V(-0.f) & static_simd_cast<_V>((1 + __f._M_quadrant) << 30);

	const auto __need_cos = (__f._M_quadrant & 1) == 0;
	if (_GLIBCXX_SIMD_IS_UNLIKELY(all_of(__need_cos)))
	  return __sign_flip ^ __cosSeries(__f._M_x);
	else if (_GLIBCXX_SIMD_IS_UNLIKELY(none_of(__need_cos)))
	  return __sign_flip ^ __sinSeries(__f._M_x);
	else // some_of(__need_cos)
	  {
	    _V __r = __sinSeries(__f._M_x);
	    where(__need_cos.__cvt(), __r) = __cosSeries(__f._M_x);
	    return __r ^ __sign_flip;
	  }
      }
  }

template <typename _Tp>
  _GLIBCXX_SIMD_ALWAYS_INLINE
  enable_if_t<is_floating_point<_Tp>::value, simd<_Tp, simd_abi::scalar>>
  cos(simd<_Tp, simd_abi::scalar> __x)
  { return std::cos(__data(__x)); }

//}}}
// sin{{{
template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  sin(const simd<_Tp, _Abi>& __x)
  {
    using _V = simd<_Tp, _Abi>;
    if constexpr (__is_scalar_abi<_Abi>() || __is_fixed_size_abi_v<_Abi>)
      return {__private_init, _Abi::_SimdImpl::_S_sin(__data(__x))};
    else
      {
	if constexpr (is_same_v<_Tp, float>)
	  if (_GLIBCXX_SIMD_IS_UNLIKELY(any_of(abs(__x) >= 527449)))
	    return static_simd_cast<_V>(
	      sin(static_simd_cast<rebind_simd_t<double, _V>>(__x)));

	const auto __f = __fold_input(__x);
	// quadrant | effect
	//        0 | sinSeries
	//        1 | cosSeries
	//        2 | sinSeries, sign flip
	//        3 | cosSeries, sign flip
	using namespace std::experimental::__float_bitwise_operators;
	const auto __sign_flip
	  = (__x ^ static_simd_cast<_V>(1 - __f._M_quadrant)) & _V(_Tp(-0.));

	const auto __need_sin = (__f._M_quadrant & 1) == 0;
	if (_GLIBCXX_SIMD_IS_UNLIKELY(all_of(__need_sin)))
	  return __sign_flip ^ __sinSeries(__f._M_x);
	else if (_GLIBCXX_SIMD_IS_UNLIKELY(none_of(__need_sin)))
	  return __sign_flip ^ __cosSeries(__f._M_x);
	else // some_of(__need_sin)
	  {
	    _V __r = __cosSeries(__f._M_x);
	    where(__need_sin.__cvt(), __r) = __sinSeries(__f._M_x);
	    return __sign_flip ^ __r;
	  }
      }
  }

template <typename _Tp>
  _GLIBCXX_SIMD_ALWAYS_INLINE
  enable_if_t<is_floating_point<_Tp>::value, simd<_Tp, simd_abi::scalar>>
  sin(simd<_Tp, simd_abi::scalar> __x)
  { return std::sin(__data(__x)); }

//}}}
_GLIBCXX_SIMD_MATH_CALL_(tan)
_GLIBCXX_SIMD_MATH_CALL_(acosh)
_GLIBCXX_SIMD_MATH_CALL_(asinh)
_GLIBCXX_SIMD_MATH_CALL_(atanh)
_GLIBCXX_SIMD_MATH_CALL_(cosh)
_GLIBCXX_SIMD_MATH_CALL_(sinh)
_GLIBCXX_SIMD_MATH_CALL_(tanh)
// }}}
// exponential functions {{{
_GLIBCXX_SIMD_MATH_CALL_(exp)
_GLIBCXX_SIMD_MATH_CALL_(exp2)
_GLIBCXX_SIMD_MATH_CALL_(expm1)

// }}}
// frexp {{{
#if _GLIBCXX_SIMD_X86INTRIN
template <typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC
  _SimdWrapper<_Tp, _Np>
  __getexp(_SimdWrapper<_Tp, _Np> __x)
  {
    if constexpr (__have_avx512vl && __is_sse_ps<_Tp, _Np>())
      return __auto_bitcast(_mm_getexp_ps(__to_intrin(__x)));
    else if constexpr (__have_avx512f && __is_sse_ps<_Tp, _Np>())
      return __auto_bitcast(_mm512_getexp_ps(__auto_bitcast(__to_intrin(__x))));
    else if constexpr (__have_avx512vl && __is_sse_pd<_Tp, _Np>())
      return _mm_getexp_pd(__x);
    else if constexpr (__have_avx512f && __is_sse_pd<_Tp, _Np>())
      return __lo128(_mm512_getexp_pd(__auto_bitcast(__x)));
    else if constexpr (__have_avx512vl && __is_avx_ps<_Tp, _Np>())
      return _mm256_getexp_ps(__x);
    else if constexpr (__have_avx512f && __is_avx_ps<_Tp, _Np>())
      return __lo256(_mm512_getexp_ps(__auto_bitcast(__x)));
    else if constexpr (__have_avx512vl && __is_avx_pd<_Tp, _Np>())
      return _mm256_getexp_pd(__x);
    else if constexpr (__have_avx512f && __is_avx_pd<_Tp, _Np>())
      return __lo256(_mm512_getexp_pd(__auto_bitcast(__x)));
    else if constexpr (__is_avx512_ps<_Tp, _Np>())
      return _mm512_getexp_ps(__x);
    else if constexpr (__is_avx512_pd<_Tp, _Np>())
      return _mm512_getexp_pd(__x);
    else
      __assert_unreachable<_Tp>();
  }

template <typename _Tp, size_t _Np>
  _GLIBCXX_SIMD_INTRINSIC
  _SimdWrapper<_Tp, _Np>
  __getmant_avx512(_SimdWrapper<_Tp, _Np> __x)
  {
    if constexpr (__have_avx512vl && __is_sse_ps<_Tp, _Np>())
      return __auto_bitcast(_mm_getmant_ps(__to_intrin(__x), _MM_MANT_NORM_p5_1,
					   _MM_MANT_SIGN_src));
    else if constexpr (__have_avx512f && __is_sse_ps<_Tp, _Np>())
      return __auto_bitcast(_mm512_getmant_ps(__auto_bitcast(__to_intrin(__x)),
					      _MM_MANT_NORM_p5_1,
					      _MM_MANT_SIGN_src));
    else if constexpr (__have_avx512vl && __is_sse_pd<_Tp, _Np>())
      return _mm_getmant_pd(__x, _MM_MANT_NORM_p5_1, _MM_MANT_SIGN_src);
    else if constexpr (__have_avx512f && __is_sse_pd<_Tp, _Np>())
      return __lo128(_mm512_getmant_pd(__auto_bitcast(__x), _MM_MANT_NORM_p5_1,
				       _MM_MANT_SIGN_src));
    else if constexpr (__have_avx512vl && __is_avx_ps<_Tp, _Np>())
      return _mm256_getmant_ps(__x, _MM_MANT_NORM_p5_1, _MM_MANT_SIGN_src);
    else if constexpr (__have_avx512f && __is_avx_ps<_Tp, _Np>())
      return __lo256(_mm512_getmant_ps(__auto_bitcast(__x), _MM_MANT_NORM_p5_1,
				       _MM_MANT_SIGN_src));
    else if constexpr (__have_avx512vl && __is_avx_pd<_Tp, _Np>())
      return _mm256_getmant_pd(__x, _MM_MANT_NORM_p5_1, _MM_MANT_SIGN_src);
    else if constexpr (__have_avx512f && __is_avx_pd<_Tp, _Np>())
      return __lo256(_mm512_getmant_pd(__auto_bitcast(__x), _MM_MANT_NORM_p5_1,
				       _MM_MANT_SIGN_src));
    else if constexpr (__is_avx512_ps<_Tp, _Np>())
      return _mm512_getmant_ps(__x, _MM_MANT_NORM_p5_1, _MM_MANT_SIGN_src);
    else if constexpr (__is_avx512_pd<_Tp, _Np>())
      return _mm512_getmant_pd(__x, _MM_MANT_NORM_p5_1, _MM_MANT_SIGN_src);
    else
      __assert_unreachable<_Tp>();
  }
#endif // _GLIBCXX_SIMD_X86INTRIN

/**
 * splits @p __v into exponent and mantissa, the sign is kept with the mantissa
 *
 * The return value will be in the range [0.5, 1.0[
 * The @p __e value will be an integer defining the power-of-two exponent
 */
template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  frexp(const simd<_Tp, _Abi>& __x, _Samesize<int, simd<_Tp, _Abi>>* __exp)
  {
    if constexpr (simd_size_v<_Tp, _Abi> == 1)
      {
	int __tmp;
	const auto __r = std::frexp(__x[0], &__tmp);
	(*__exp)[0] = __tmp;
	return __r;
      }
    else if constexpr (__is_fixed_size_abi_v<_Abi>)
      return {__private_init, _Abi::_SimdImpl::_S_frexp(__data(__x), __data(*__exp))};
#if _GLIBCXX_SIMD_X86INTRIN
    else if constexpr (__have_avx512f)
      {
	constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
	constexpr size_t _NI = _Np < 4 ? 4 : _Np;
	const auto __v = __data(__x);
	const auto __isnonzero
	  = _Abi::_SimdImpl::_S_isnonzerovalue_mask(__v._M_data);
	const _SimdWrapper<int, _NI> __exp_plus1
	  = 1 + __convert<_SimdWrapper<int, _NI>>(__getexp(__v))._M_data;
	const _SimdWrapper<int, _Np> __e = __wrapper_bitcast<int, _Np>(
	  _Abi::_CommonImpl::_S_blend(_SimdWrapper<bool, _NI>(__isnonzero),
				      _SimdWrapper<int, _NI>(), __exp_plus1));
	simd_abi::deduce_t<int, _Np>::_CommonImpl::_S_store(__e, __exp);
	return {__private_init,
		_Abi::_CommonImpl::_S_blend(_SimdWrapper<bool, _Np>(
					      __isnonzero),
					    __v, __getmant_avx512(__v))};
      }
#endif // _GLIBCXX_SIMD_X86INTRIN
    else
      {
	// fallback implementation
	static_assert(sizeof(_Tp) == 4 || sizeof(_Tp) == 8);
	using _V = simd<_Tp, _Abi>;
	using _IV = rebind_simd_t<int, _V>;
	using namespace std::experimental::__proposed;
	using namespace std::experimental::__float_bitwise_operators;

	constexpr int __exp_adjust = sizeof(_Tp) == 4 ? 0x7e : 0x3fe;
	constexpr int __exp_offset = sizeof(_Tp) == 4 ? 0x70 : 0x200;
	constexpr _Tp __subnorm_scale = sizeof(_Tp) == 4 ? 0x1p112 : 0x1p512;
	_GLIBCXX_SIMD_USE_CONSTEXPR_API _V __exponent_mask
	  = __infinity_v<_Tp>; // 0x7f800000 or 0x7ff0000000000000
	_GLIBCXX_SIMD_USE_CONSTEXPR_API _V __p5_1_exponent
	  = -(2 - __epsilon_v<_Tp>) / 2; // 0xbf7fffff or 0xbfefffffffffffff

	_V __mant = __p5_1_exponent & (__exponent_mask | __x); // +/-[.5, 1)
	const _IV __exponent_bits = __extract_exponent_as_int(__x);
	if (_GLIBCXX_SIMD_IS_LIKELY(all_of(isnormal(__x))))
	  {
	    *__exp
	      = simd_cast<_Samesize<int, _V>>(__exponent_bits - __exp_adjust);
	    return __mant;
	  }

#if __FINITE_MATH_ONLY__
	// at least one element of __x is 0 or subnormal, the rest is normal
	// (inf and NaN are excluded by -ffinite-math-only)
	const auto __iszero_inf_nan = __x == 0;
#else
	using _Ip = __int_for_sizeof_t<_Tp>;
	const auto __as_int = simd_bit_cast<rebind_simd_t<_Ip, _V>>(abs(__x));
	const auto __inf = simd_bit_cast<rebind_simd_t<_Ip, _V>>(_V(__infinity_v<_Tp>));
	const auto __iszero_inf_nan = static_simd_cast<typename _V::mask_type>(
	  __as_int == 0 || __as_int >= __inf);
#endif

	const _V __scaled_subnormal = __x * __subnorm_scale;
	const _V __mant_subnormal
	  = __p5_1_exponent & (__exponent_mask | __scaled_subnormal);
	where(!isnormal(__x), __mant) = __mant_subnormal;
	where(__iszero_inf_nan, __mant) = __x;
	_IV __e = __extract_exponent_as_int(__scaled_subnormal);
	using _MaskType =
	  typename conditional_t<sizeof(typename _V::value_type) == sizeof(int),
				 _V, _IV>::mask_type;
	const _MaskType __value_isnormal = isnormal(__x).__cvt();
	where(__value_isnormal.__cvt(), __e) = __exponent_bits;
	static_assert(sizeof(_IV) == sizeof(__value_isnormal));
	const _IV __offset
	  = (simd_bit_cast<_IV>(__value_isnormal) & _IV(__exp_adjust))
	      | (simd_bit_cast<_IV>(static_simd_cast<_MaskType>(__exponent_bits == 0)
				      & static_simd_cast<_MaskType>(__x != 0))
		   & _IV(__exp_adjust + __exp_offset));
	*__exp = simd_cast<_Samesize<int, _V>>(__e - __offset);
	return __mant;
      }
  }

// }}}
_GLIBCXX_SIMD_MATH_CALL2_(ldexp, int)
_GLIBCXX_SIMD_MATH_CALL_(ilogb)

// logarithms {{{
_GLIBCXX_SIMD_MATH_CALL_(log)
_GLIBCXX_SIMD_MATH_CALL_(log10)
_GLIBCXX_SIMD_MATH_CALL_(log1p)
_GLIBCXX_SIMD_MATH_CALL_(log2)

//}}}
// logb{{{
template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point<_Tp>::value, simd<_Tp, _Abi>>
  logb(const simd<_Tp, _Abi>& __x)
  {
    constexpr size_t _Np = simd_size_v<_Tp, _Abi>;
    if constexpr (_Np == 1)
      return std::logb(__x[0]);
    else if constexpr (__is_fixed_size_abi_v<_Abi>)
      return {__private_init, _Abi::_SimdImpl::_S_logb(__data(__x))};
#if _GLIBCXX_SIMD_X86INTRIN // {{{
    else if constexpr (__have_avx512vl && __is_sse_ps<_Tp, _Np>())
      return {__private_init,
	      __auto_bitcast(_mm_getexp_ps(__to_intrin(__as_vector(__x))))};
    else if constexpr (__have_avx512vl && __is_sse_pd<_Tp, _Np>())
      return {__private_init, _mm_getexp_pd(__data(__x))};
    else if constexpr (__have_avx512vl && __is_avx_ps<_Tp, _Np>())
      return {__private_init, _mm256_getexp_ps(__data(__x))};
    else if constexpr (__have_avx512vl && __is_avx_pd<_Tp, _Np>())
      return {__private_init, _mm256_getexp_pd(__data(__x))};
    else if constexpr (__have_avx512f && __is_avx_ps<_Tp, _Np>())
      return {__private_init,
	      __lo256(_mm512_getexp_ps(__auto_bitcast(__data(__x))))};
    else if constexpr (__have_avx512f && __is_avx_pd<_Tp, _Np>())
      return {__private_init,
	      __lo256(_mm512_getexp_pd(__auto_bitcast(__data(__x))))};
    else if constexpr (__is_avx512_ps<_Tp, _Np>())
      return {__private_init, _mm512_getexp_ps(__data(__x))};
    else if constexpr (__is_avx512_pd<_Tp, _Np>())
      return {__private_init, _mm512_getexp_pd(__data(__x))};
#endif // _GLIBCXX_SIMD_X86INTRIN }}}
    else
      {
	using _V = simd<_Tp, _Abi>;
	using namespace std::experimental::__proposed;
	auto __is_normal = isnormal(__x);

	// work on abs(__x) to reflect the return value on Linux for negative
	// inputs (domain-error => implementation-defined value is returned)
	const _V abs_x = abs(__x);

	// __exponent(__x) returns the exponent value (bias removed) as
	// simd<_Up> with integral _Up
	auto&& __exponent = [](const _V& __v) {
	  using namespace std::experimental::__proposed;
	  using _IV = rebind_simd_t<
	    conditional_t<sizeof(_Tp) == sizeof(_LLong), _LLong, int>, _V>;
	  return (simd_bit_cast<_IV>(__v) >> (__digits_v<_Tp> - 1))
		 - (__max_exponent_v<_Tp> - 1);
	};
	_V __r = static_simd_cast<_V>(__exponent(abs_x));
	if (_GLIBCXX_SIMD_IS_LIKELY(all_of(__is_normal)))
	  // without corner cases (nan, inf, subnormal, zero) we have our
	  // answer:
	  return __r;
	const auto __is_zero = __x == 0;
	const auto __is_nan = isnan(__x);
	const auto __is_inf = isinf(__x);
	where(__is_zero, __r) = -__infinity_v<_Tp>;
	where(__is_nan, __r) = __x;
	where(__is_inf, __r) = __infinity_v<_Tp>;
	__is_normal |= __is_zero || __is_nan || __is_inf;
	if (all_of(__is_normal))
	  // at this point everything but subnormals is handled
	  return __r;
	// subnormals repeat the exponent extraction after multiplication of the
	// input with __a floating point value that has 112 (0x70) in its exponent
	// (not too big for sp and large enough for dp)
	const _V __scaled = abs_x * _Tp(0x1p112);
	_V __scaled_exp = static_simd_cast<_V>(__exponent(__scaled) - 112);
	where(__is_normal, __scaled_exp) = __r;
	return __scaled_exp;
      }
  }

//}}}
template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  modf(const simd<_Tp, _Abi>& __x, simd<_Tp, _Abi>* __iptr)
  {
    if constexpr (simd_size_v<_Tp, _Abi> == 1)
      {
	_Tp __tmp;
	_Tp __r = std::modf(__x[0], &__tmp);
	__iptr[0] = __tmp;
	return __r;
      }
    else
      {
	const auto __integral = trunc(__x);
	*__iptr = __integral;
	auto __r = __x - __integral;
#if !__FINITE_MATH_ONLY__
	where(isinf(__x), __r) = _Tp();
#endif
	return copysign(__r, __x);
      }
  }

_GLIBCXX_SIMD_MATH_CALL2_(scalbn, int)
_GLIBCXX_SIMD_MATH_CALL2_(scalbln, long)

_GLIBCXX_SIMD_MATH_CALL_(cbrt)

_GLIBCXX_SIMD_MATH_CALL_(abs)
_GLIBCXX_SIMD_MATH_CALL_(fabs)

// [parallel.simd.math] only asks for is_floating_point_v<_Tp> and forgot to
// allow signed integral _Tp
template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE
  enable_if_t<!is_floating_point_v<_Tp> && is_signed_v<_Tp>, simd<_Tp, _Abi>>
  abs(const simd<_Tp, _Abi>& __x)
  { return {__private_init, _Abi::_SimdImpl::_S_abs(__data(__x))}; }

#define _GLIBCXX_SIMD_CVTING2(_NAME)                                           \
template <typename _Tp, typename _Abi>                                         \
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi> _NAME(                               \
    const simd<_Tp, _Abi>& __x, const __type_identity_t<simd<_Tp, _Abi>>& __y) \
  {                                                                            \
    return _NAME(__x, __y);                                                    \
  }                                                                            \
                                                                               \
template <typename _Tp, typename _Abi>                                         \
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi> _NAME(                               \
    const __type_identity_t<simd<_Tp, _Abi>>& __x, const simd<_Tp, _Abi>& __y) \
  {                                                                            \
    return _NAME(__x, __y);                                                    \
  }

#define _GLIBCXX_SIMD_CVTING3(_NAME)                                           \
template <typename _Tp, typename _Abi>                                         \
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi> _NAME(                               \
    const __type_identity_t<simd<_Tp, _Abi>>& __x, const simd<_Tp, _Abi>& __y, \
    const simd<_Tp, _Abi>& __z)                                                \
  {                                                                            \
    return _NAME(__x, __y, __z);                                               \
  }                                                                            \
                                                                               \
template <typename _Tp, typename _Abi>                                         \
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi> _NAME(                               \
    const simd<_Tp, _Abi>& __x, const __type_identity_t<simd<_Tp, _Abi>>& __y, \
    const simd<_Tp, _Abi>& __z)                                                \
  {                                                                            \
    return _NAME(__x, __y, __z);                                               \
  }                                                                            \
                                                                               \
template <typename _Tp, typename _Abi>                                         \
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi> _NAME(                               \
    const simd<_Tp, _Abi>& __x, const simd<_Tp, _Abi>& __y,                    \
    const __type_identity_t<simd<_Tp, _Abi>>& __z)                             \
  {                                                                            \
    return _NAME(__x, __y, __z);                                               \
  }                                                                            \
                                                                               \
template <typename _Tp, typename _Abi>                                         \
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi> _NAME(                               \
    const simd<_Tp, _Abi>& __x, const __type_identity_t<simd<_Tp, _Abi>>& __y, \
    const __type_identity_t<simd<_Tp, _Abi>>& __z)                             \
  {                                                                            \
    return _NAME(__x, __y, __z);                                               \
  }                                                                            \
                                                                               \
template <typename _Tp, typename _Abi>                                         \
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi> _NAME(                               \
    const __type_identity_t<simd<_Tp, _Abi>>& __x, const simd<_Tp, _Abi>& __y, \
    const __type_identity_t<simd<_Tp, _Abi>>& __z)                             \
  {                                                                            \
    return _NAME(__x, __y, __z);                                               \
  }                                                                            \
                                                                               \
template <typename _Tp, typename _Abi>                                         \
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi> _NAME(                               \
    const __type_identity_t<simd<_Tp, _Abi>>& __x,                             \
    const __type_identity_t<simd<_Tp, _Abi>>& __y, const simd<_Tp, _Abi>& __z) \
  {                                                                            \
    return _NAME(__x, __y, __z);                                               \
  }

template <typename _R, typename _ToApply, typename _Tp, typename... _Tps>
  _GLIBCXX_SIMD_INTRINSIC _R
  __fixed_size_apply(_ToApply&& __apply, const _Tp& __arg0,
		     const _Tps&... __args)
  {
    return {__private_init,
	    __data(__arg0)._M_apply_per_chunk(
	      [&](auto __impl, const auto&... __inner) {
		using _V = typename decltype(__impl)::simd_type;
		return __data(__apply(_V(__private_init, __inner)...));
	      },
	      __data(__args)...)};
  }

template <typename _VV, typename = __detail::__odr_helper>
  __remove_cvref_t<_VV>
  __hypot(_VV __x, _VV __y)
  {
    using _V = __remove_cvref_t<_VV>;
    using _Tp = typename _V::value_type;
    if constexpr (_V::size() == 1)
      return std::hypot(_Tp(__x[0]), _Tp(__y[0]));
    else if constexpr (__is_fixed_size_abi_v<typename _V::abi_type>)
      {
	return __fixed_size_apply<_V>([](auto __a,
					 auto __b) { return hypot(__a, __b); },
				      __x, __y);
      }
    else
      {
	// A simple solution for _Tp == float would be to cast to double and
	// simply calculate sqrt(x²+y²) as it can't over-/underflow anymore with
	// dp. It still needs the Annex F fixups though and isn't faster on
	// Skylake-AVX512 (not even for SSE and AVX vectors, and really bad for
	// AVX-512).
	using namespace __float_bitwise_operators;
	using namespace __proposed;
	_V __absx = abs(__x);          // no error
	_V __absy = abs(__y);          // no error
	_V __hi = max(__absx, __absy); // no error
	_V __lo = min(__absy, __absx); // no error

	// round __hi down to the next power-of-2:
	_GLIBCXX_SIMD_USE_CONSTEXPR_API _V __inf(__infinity_v<_Tp>);

#ifndef __FAST_MATH__
	if constexpr (__have_neon && !__have_neon_a32)
	  { // With ARMv7 NEON, we have no subnormals and must use slightly
	    // different strategy
	    const _V __hi_exp = __hi & __inf;
	    _V __scale_back = __hi_exp;
	    // For large exponents (max & max/2) the inversion comes too close
	    // to subnormals. Subtract 3 from the exponent:
	    where(__hi_exp > 1, __scale_back) = __hi_exp * _Tp(0.125);
	    // Invert and adjust for the off-by-one error of inversion via xor:
	    const _V __scale = (__scale_back ^ __inf) * _Tp(.5);
	    const _V __h1 = __hi * __scale;
	    const _V __l1 = __lo * __scale;
	    _V __r = __scale_back * sqrt(__h1 * __h1 + __l1 * __l1);
	    // Fix up hypot(0, 0) to not be NaN:
	    where(__hi == 0, __r) = 0;
	    return __r;
	  }
#endif

#ifdef __FAST_MATH__
	// With fast-math, ignore precision of subnormals and inputs from
	// __finite_max_v/2 to __finite_max_v. This removes all
	// branching/masking.
	if constexpr (true)
#else
	if (_GLIBCXX_SIMD_IS_LIKELY(all_of(isnormal(__x))
				    && all_of(isnormal(__y))))
#endif
	  {
	    const _V __hi_exp = __hi & __inf;
	    //((__hi + __hi) & __inf) ^ __inf almost works for computing
	    //__scale,
	    // except when (__hi + __hi) & __inf == __inf, in which case __scale
	    // becomes 0 (should be min/2 instead) and thus loses the
	    // information from __lo.
#ifdef __FAST_MATH__
	    using _Ip = __int_for_sizeof_t<_Tp>;
	    using _IV = rebind_simd_t<_Ip, _V>;
	    const auto __as_int = simd_bit_cast<_IV>(__hi_exp);
	    const _V __scale
	      = simd_bit_cast<_V>(2 * simd_bit_cast<_Ip>(_Tp(1)) - __as_int);
#else
	    const _V __scale = (__hi_exp ^ __inf) * _Tp(.5);
#endif
	    _GLIBCXX_SIMD_USE_CONSTEXPR_API _V __mant_mask
	      = __norm_min_v<_Tp> - __denorm_min_v<_Tp>;
	    const _V __h1 = (__hi & __mant_mask) | _V(1);
	    const _V __l1 = __lo * __scale;
	    return __hi_exp * sqrt(__h1 * __h1 + __l1 * __l1);
	  }
	else
	  {
	    // slower path to support subnormals
	    // if __hi is subnormal, avoid scaling by inf & final mul by 0
	    // (which yields NaN) by using min()
	    _V __scale = _V(1 / __norm_min_v<_Tp>);
	    // invert exponent w/o error and w/o using the slow divider unit:
	    // xor inverts the exponent but off by 1. Multiplication with .5
	    // adjusts for the discrepancy.
	    where(__hi >= __norm_min_v<_Tp>, __scale)
	      = ((__hi & __inf) ^ __inf) * _Tp(.5);
	    // adjust final exponent for subnormal inputs
	    _V __hi_exp = __norm_min_v<_Tp>;
	    where(__hi >= __norm_min_v<_Tp>, __hi_exp)
	      = __hi & __inf;         // no error
	    _V __h1 = __hi * __scale; // no error
	    _V __l1 = __lo * __scale; // no error

	    // sqrt(x²+y²) = e*sqrt((x/e)²+(y/e)²):
	    // this ensures no overflow in the argument to sqrt
	    _V __r = __hi_exp * sqrt(__h1 * __h1 + __l1 * __l1);
#ifdef __STDC_IEC_559__
	    // fixup for Annex F requirements
	    // the naive fixup goes like this:
	    //
	    // where(__l1 == 0, __r)                      = __hi;
	    // where(isunordered(__x, __y), __r)          = __quiet_NaN_v<_Tp>;
	    // where(isinf(__absx) || isinf(__absy), __r) = __inf;
	    //
	    // The fixup can be prepared in parallel with the sqrt, requiring a
	    // single blend step after hi_exp * sqrt, reducing latency and
	    // throughput:
	    _V __fixup = __hi; // __lo == 0
	    where(isunordered(__x, __y), __fixup) = __quiet_NaN_v<_Tp>;
	    where(isinf(__absx) || isinf(__absy), __fixup) = __inf;
	    where(!(__lo == 0 || isunordered(__x, __y)
		    || (isinf(__absx) || isinf(__absy))),
		  __fixup)
	      = __r;
	    __r = __fixup;
#endif
	    return __r;
	  }
      }
  }

template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi>
  hypot(const simd<_Tp, _Abi>& __x, const simd<_Tp, _Abi>& __y)
  {
    return __hypot<conditional_t<__is_fixed_size_abi_v<_Abi>,
				 const simd<_Tp, _Abi>&, simd<_Tp, _Abi>>>(__x,
									   __y);
  }

_GLIBCXX_SIMD_CVTING2(hypot)

  template <typename _VV, typename = __detail::__odr_helper>
  __remove_cvref_t<_VV>
  __hypot(_VV __x, _VV __y, _VV __z)
  {
    using _V = __remove_cvref_t<_VV>;
    using _Abi = typename _V::abi_type;
    using _Tp = typename _V::value_type;
    /* FIXME: enable after PR77776 is resolved
    if constexpr (_V::size() == 1)
      return std::hypot(_Tp(__x[0]), _Tp(__y[0]), _Tp(__z[0]));
    else
    */
    if constexpr (__is_fixed_size_abi_v<_Abi> && _V::size() > 1)
      {
	return __fixed_size_apply<simd<_Tp, _Abi>>(
	  [](auto __a, auto __b, auto __c) { return hypot(__a, __b, __c); },
	  __x, __y, __z);
      }
    else
      {
	using namespace __float_bitwise_operators;
	using namespace __proposed;
	const _V __absx = abs(__x);                 // no error
	const _V __absy = abs(__y);                 // no error
	const _V __absz = abs(__z);                 // no error
	_V __hi = max(max(__absx, __absy), __absz); // no error
	_V __l0 = min(__absz, max(__absx, __absy)); // no error
	_V __l1 = min(__absy, __absx);              // no error
	if constexpr (__digits_v<_Tp> == 64 && __max_exponent_v<_Tp> == 0x4000
		      && __min_exponent_v<_Tp> == -0x3FFD && _V::size() == 1)
	  { // Seems like x87 fp80, where bit 63 is always 1 unless subnormal or
	    // NaN. In this case the bit-tricks don't work, they require IEC559
	    // binary32 or binary64 format.
#ifdef __STDC_IEC_559__
	    // fixup for Annex F requirements
	    if (isinf(__absx[0]) || isinf(__absy[0]) || isinf(__absz[0]))
	      return __infinity_v<_Tp>;
	    else if (isunordered(__absx[0], __absy[0] + __absz[0]))
	      return __quiet_NaN_v<_Tp>;
	    else if (__l0[0] == 0 && __l1[0] == 0)
	      return __hi;
#endif
	    _V __hi_exp = __hi;
	    const _ULLong __tmp = 0x8000'0000'0000'0000ull;
	    __builtin_memcpy(&__data(__hi_exp), &__tmp, 8);
	    const _V __scale = 1 / __hi_exp;
	    __hi *= __scale;
	    __l0 *= __scale;
	    __l1 *= __scale;
	    return __hi_exp * sqrt((__l0 * __l0 + __l1 * __l1) + __hi * __hi);
	  }
	else
	  {
	    // round __hi down to the next power-of-2:
	    _GLIBCXX_SIMD_USE_CONSTEXPR_API _V __inf(__infinity_v<_Tp>);

#ifndef __FAST_MATH__
	    if constexpr (_V::size() > 1 && __have_neon && !__have_neon_a32)
	      { // With ARMv7 NEON, we have no subnormals and must use slightly
		// different strategy
		const _V __hi_exp = __hi & __inf;
		_V __scale_back = __hi_exp;
		// For large exponents (max & max/2) the inversion comes too
		// close to subnormals. Subtract 3 from the exponent:
		where(__hi_exp > 1, __scale_back) = __hi_exp * _Tp(0.125);
		// Invert and adjust for the off-by-one error of inversion via
		// xor:
		const _V __scale = (__scale_back ^ __inf) * _Tp(.5);
		const _V __h1 = __hi * __scale;
		__l0 *= __scale;
		__l1 *= __scale;
		_V __lo = __l0 * __l0
			  + __l1 * __l1; // add the two smaller values first
		asm("" : "+m"(__lo));
		_V __r = __scale_back * sqrt(__h1 * __h1 + __lo);
		// Fix up hypot(0, 0, 0) to not be NaN:
		where(__hi == 0, __r) = 0;
		return __r;
	      }
#endif

#ifdef __FAST_MATH__
	    // With fast-math, ignore precision of subnormals and inputs from
	    // __finite_max_v/2 to __finite_max_v. This removes all
	    // branching/masking.
	    if constexpr (true)
#else
	    if (_GLIBCXX_SIMD_IS_LIKELY(all_of(isnormal(__x))
					&& all_of(isnormal(__y))
					&& all_of(isnormal(__z))))
#endif
	      {
		const _V __hi_exp = __hi & __inf;
		//((__hi + __hi) & __inf) ^ __inf almost works for computing
		//__scale, except when (__hi + __hi) & __inf == __inf, in which
		// case __scale
		// becomes 0 (should be min/2 instead) and thus loses the
		// information from __lo.
#ifdef __FAST_MATH__
		using _Ip = __int_for_sizeof_t<_Tp>;
		using _IV = rebind_simd_t<_Ip, _V>;
		const auto __as_int = simd_bit_cast<_IV>(__hi_exp);
		const _V __scale
		  = simd_bit_cast<_V>(2 * simd_bit_cast<_Ip>(_Tp(1)) - __as_int);
#else
		const _V __scale = (__hi_exp ^ __inf) * _Tp(.5);
#endif
		constexpr _Tp __mant_mask
		  = __norm_min_v<_Tp> - __denorm_min_v<_Tp>;
		const _V __h1 = (__hi & _V(__mant_mask)) | _V(1);
		__l0 *= __scale;
		__l1 *= __scale;
		const _V __lo
		  = __l0 * __l0
		    + __l1 * __l1; // add the two smaller values first
		return __hi_exp * sqrt(__lo + __h1 * __h1);
	      }
	    else
	      {
		// slower path to support subnormals
		// if __hi is subnormal, avoid scaling by inf & final mul by 0
		// (which yields NaN) by using min()
		_V __scale = _V(1 / __norm_min_v<_Tp>);
		// invert exponent w/o error and w/o using the slow divider
		// unit: xor inverts the exponent but off by 1. Multiplication
		// with .5 adjusts for the discrepancy.
		where(__hi >= __norm_min_v<_Tp>, __scale)
		  = ((__hi & __inf) ^ __inf) * _Tp(.5);
		// adjust final exponent for subnormal inputs
		_V __hi_exp = __norm_min_v<_Tp>;
		where(__hi >= __norm_min_v<_Tp>, __hi_exp)
		  = __hi & __inf;         // no error
		_V __h1 = __hi * __scale; // no error
		__l0 *= __scale;          // no error
		__l1 *= __scale;          // no error
		_V __lo = __l0 * __l0
			  + __l1 * __l1; // add the two smaller values first
		_V __r = __hi_exp * sqrt(__lo + __h1 * __h1);
#ifdef __STDC_IEC_559__
		// fixup for Annex F requirements
		_V __fixup = __hi; // __lo == 0
		// where(__lo == 0, __fixup)                   = __hi;
		where(isunordered(__x, __y + __z), __fixup)
		  = __quiet_NaN_v<_Tp>;
		where(isinf(__absx) || isinf(__absy) || isinf(__absz), __fixup)
		  = __inf;
		// Instead of __lo == 0, the following could depend on __h1² ==
		// __h1² + __lo (i.e. __hi is so much larger than the other two
		// inputs that the result is exactly __hi). While this may
		// improve precision, it is likely to reduce efficiency if the
		// ISA has FMAs (because __h1² + __lo is an FMA, but the
		// intermediate
		// __h1² must be kept)
		where(!(__lo == 0 || isunordered(__x, __y + __z)
			|| isinf(__absx) || isinf(__absy) || isinf(__absz)),
		      __fixup)
		  = __r;
		__r = __fixup;
#endif
		return __r;
	      }
	  }
      }
  }

  template <typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_INTRINSIC simd<_Tp, _Abi>
  hypot(const simd<_Tp, _Abi>& __x, const simd<_Tp, _Abi>& __y,
	const simd<_Tp, _Abi>& __z)
  {
    return __hypot<conditional_t<__is_fixed_size_abi_v<_Abi>,
				 const simd<_Tp, _Abi>&, simd<_Tp, _Abi>>>(__x,
									   __y,
									   __z);
  }

_GLIBCXX_SIMD_CVTING3(hypot)

_GLIBCXX_SIMD_MATH_CALL2_(pow, _Tp)

_GLIBCXX_SIMD_MATH_CALL_(sqrt)
_GLIBCXX_SIMD_MATH_CALL_(erf)
_GLIBCXX_SIMD_MATH_CALL_(erfc)
_GLIBCXX_SIMD_MATH_CALL_(lgamma)
_GLIBCXX_SIMD_MATH_CALL_(tgamma)
_GLIBCXX_SIMD_MATH_CALL_(ceil)
_GLIBCXX_SIMD_MATH_CALL_(floor)
_GLIBCXX_SIMD_MATH_CALL_(nearbyint)
_GLIBCXX_SIMD_MATH_CALL_(rint)
_GLIBCXX_SIMD_MATH_CALL_(lrint)
_GLIBCXX_SIMD_MATH_CALL_(llrint)

_GLIBCXX_SIMD_MATH_CALL_(round)
_GLIBCXX_SIMD_MATH_CALL_(lround)
_GLIBCXX_SIMD_MATH_CALL_(llround)

_GLIBCXX_SIMD_MATH_CALL_(trunc)

_GLIBCXX_SIMD_MATH_CALL2_(fmod, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(remainder, _Tp)
_GLIBCXX_SIMD_MATH_CALL3_(remquo, _Tp, int*)

template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  copysign(const simd<_Tp, _Abi>& __x, const simd<_Tp, _Abi>& __y)
  {
    if constexpr (simd_size_v<_Tp, _Abi> == 1)
      return std::copysign(__x[0], __y[0]);
    else if constexpr (__is_fixed_size_abi_v<_Abi>)
      return {__private_init, _Abi::_SimdImpl::_S_copysign(__data(__x), __data(__y))};
    else
      {
	using _V = simd<_Tp, _Abi>;
	using namespace std::experimental::__float_bitwise_operators;
	_GLIBCXX_SIMD_USE_CONSTEXPR_API auto __signmask = _V(1) ^ _V(-1);
	return (__x & ~__signmask) | (__y & __signmask);
      }
  }

_GLIBCXX_SIMD_MATH_CALL2_(nextafter, _Tp)
// not covered in [parallel.simd.math]:
// _GLIBCXX_SIMD_MATH_CALL2_(nexttoward, long double)
_GLIBCXX_SIMD_MATH_CALL2_(fdim, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(fmax, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(fmin, _Tp)

_GLIBCXX_SIMD_MATH_CALL3_(fma, _Tp, _Tp)
_GLIBCXX_SIMD_MATH_CALL_(fpclassify)
_GLIBCXX_SIMD_MATH_CALL_(isfinite)

// isnan and isinf require special treatment because old glibc may declare
// `int isinf(double)`.
template <typename _Tp, typename _Abi, typename...,
	  typename _R = _Math_return_type_t<bool, _Tp, _Abi>>
  _GLIBCXX_SIMD_ALWAYS_INLINE
  enable_if_t<is_floating_point_v<_Tp>, _R>
  isinf(simd<_Tp, _Abi> __x)
  { return {__private_init, _Abi::_SimdImpl::_S_isinf(__data(__x))}; }

template <typename _Tp, typename _Abi, typename...,
	  typename _R = _Math_return_type_t<bool, _Tp, _Abi>>
  _GLIBCXX_SIMD_ALWAYS_INLINE
  enable_if_t<is_floating_point_v<_Tp>, _R>
  isnan(simd<_Tp, _Abi> __x)
  { return {__private_init, _Abi::_SimdImpl::_S_isnan(__data(__x))}; }

_GLIBCXX_SIMD_MATH_CALL_(isnormal)

template <typename..., typename _Tp, typename _Abi>
  _GLIBCXX_SIMD_ALWAYS_INLINE
  simd_mask<_Tp, _Abi>
  signbit(simd<_Tp, _Abi> __x)
  {
    if constexpr (is_integral_v<_Tp>)
      {
	if constexpr (is_unsigned_v<_Tp>)
	  return simd_mask<_Tp, _Abi>{}; // false
	else
	  return __x < 0;
      }
    else
      return {__private_init, _Abi::_SimdImpl::_S_signbit(__data(__x))};
  }

_GLIBCXX_SIMD_MATH_CALL2_(isgreater, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(isgreaterequal, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(isless, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(islessequal, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(islessgreater, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(isunordered, _Tp)

/* not covered in [parallel.simd.math]
template <typename _Abi> __doublev<_Abi> nan(const char* tagp);
template <typename _Abi> __floatv<_Abi> nanf(const char* tagp);
template <typename _Abi> __ldoublev<_Abi> nanl(const char* tagp);

template <typename _V> struct simd_div_t {
    _V quot, rem;
};

template <typename _Abi>
simd_div_t<_SCharv<_Abi>> div(_SCharv<_Abi> numer,
					 _SCharv<_Abi> denom);
template <typename _Abi>
simd_div_t<__shortv<_Abi>> div(__shortv<_Abi> numer,
					 __shortv<_Abi> denom);
template <typename _Abi>
simd_div_t<__intv<_Abi>> div(__intv<_Abi> numer, __intv<_Abi> denom);
template <typename _Abi>
simd_div_t<__longv<_Abi>> div(__longv<_Abi> numer,
					__longv<_Abi> denom);
template <typename _Abi>
simd_div_t<__llongv<_Abi>> div(__llongv<_Abi> numer,
					 __llongv<_Abi> denom);
*/

// special math {{{
template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  assoc_laguerre(const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __n,
		 const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __m,
		 const simd<_Tp, _Abi>& __x)
  {
    return simd<_Tp, _Abi>([&](auto __i) {
      return std::assoc_laguerre(__n[__i], __m[__i], __x[__i]);
    });
  }

template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  assoc_legendre(const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __n,
		 const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __m,
		 const simd<_Tp, _Abi>& __x)
  {
    return simd<_Tp, _Abi>([&](auto __i) {
      return std::assoc_legendre(__n[__i], __m[__i], __x[__i]);
    });
  }

_GLIBCXX_SIMD_MATH_CALL2_(beta, _Tp)
_GLIBCXX_SIMD_MATH_CALL_(comp_ellint_1)
_GLIBCXX_SIMD_MATH_CALL_(comp_ellint_2)
_GLIBCXX_SIMD_MATH_CALL2_(comp_ellint_3, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(cyl_bessel_i, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(cyl_bessel_j, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(cyl_bessel_k, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(cyl_neumann, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(ellint_1, _Tp)
_GLIBCXX_SIMD_MATH_CALL2_(ellint_2, _Tp)
_GLIBCXX_SIMD_MATH_CALL3_(ellint_3, _Tp, _Tp)
_GLIBCXX_SIMD_MATH_CALL_(expint)

template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  hermite(const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __n,
	  const simd<_Tp, _Abi>& __x)
  {
    return simd<_Tp, _Abi>(
      [&](auto __i) { return std::hermite(__n[__i], __x[__i]); });
  }

template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  laguerre(const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __n,
	   const simd<_Tp, _Abi>& __x)
  {
    return simd<_Tp, _Abi>(
      [&](auto __i) { return std::laguerre(__n[__i], __x[__i]); });
  }

template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  legendre(const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __n,
	   const simd<_Tp, _Abi>& __x)
  {
    return simd<_Tp, _Abi>(
      [&](auto __i) { return std::legendre(__n[__i], __x[__i]); });
  }

_GLIBCXX_SIMD_MATH_CALL_(riemann_zeta)

template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  sph_bessel(const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __n,
	     const simd<_Tp, _Abi>& __x)
  {
    return simd<_Tp, _Abi>(
      [&](auto __i) { return std::sph_bessel(__n[__i], __x[__i]); });
  }

template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  sph_legendre(const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __l,
	       const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __m,
	       const simd<_Tp, _Abi>& theta)
  {
    return simd<_Tp, _Abi>([&](auto __i) {
      return std::assoc_legendre(__l[__i], __m[__i], theta[__i]);
    });
  }

template <typename _Tp, typename _Abi, typename = __detail::__odr_helper>
  enable_if_t<is_floating_point_v<_Tp>, simd<_Tp, _Abi>>
  sph_neumann(const fixed_size_simd<unsigned, simd_size_v<_Tp, _Abi>>& __n,
	      const simd<_Tp, _Abi>& __x)
  {
    return simd<_Tp, _Abi>(
      [&](auto __i) { return std::sph_neumann(__n[__i], __x[__i]); });
  }
// }}}

#undef _GLIBCXX_SIMD_CVTING2
#undef _GLIBCXX_SIMD_CVTING3
#undef _GLIBCXX_SIMD_MATH_CALL_
#undef _GLIBCXX_SIMD_MATH_CALL2_
#undef _GLIBCXX_SIMD_MATH_CALL3_

_GLIBCXX_SIMD_END_NAMESPACE

#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_MATH_H_

// vim: foldmethod=marker sw=2 ts=8 noet sts=2
