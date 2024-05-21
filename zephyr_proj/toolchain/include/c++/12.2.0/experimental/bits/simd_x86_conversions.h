// x86 specific conversion optimizations -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_X86_CONVERSIONS_H
#define _GLIBCXX_EXPERIMENTAL_SIMD_X86_CONVERSIONS_H

#if __cplusplus >= 201703L

// work around PR85827
// 1-arg __convert_x86 {{{1
template <typename _To, typename _V, typename _Traits>
  _GLIBCXX_SIMD_INTRINSIC _To
  __convert_x86(_V __v)
  {
    static_assert(__is_vector_type_v<_V>);
    using _Tp = typename _Traits::value_type;
    constexpr size_t _Np = _Traits::_S_full_size;
    [[maybe_unused]] const auto __intrin = __to_intrin(__v);
    using _Up = typename _VectorTraits<_To>::value_type;
    constexpr size_t _M = _VectorTraits<_To>::_S_full_size;

    // [xyz]_to_[xyz] {{{2
    [[maybe_unused]] constexpr bool __x_to_x
      = sizeof(__v) <= 16 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __x_to_y
      = sizeof(__v) <= 16 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __x_to_z
      = sizeof(__v) <= 16 && sizeof(_To) == 64;
    [[maybe_unused]] constexpr bool __y_to_x
      = sizeof(__v) == 32 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __y_to_y
      = sizeof(__v) == 32 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __y_to_z
      = sizeof(__v) == 32 && sizeof(_To) == 64;
    [[maybe_unused]] constexpr bool __z_to_x
      = sizeof(__v) == 64 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __z_to_y
      = sizeof(__v) == 64 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __z_to_z
      = sizeof(__v) == 64 && sizeof(_To) == 64;

    // iX_to_iX {{{2
    [[maybe_unused]] constexpr bool __i_to_i
      = is_integral_v<_Up> && is_integral_v<_Tp>;
    [[maybe_unused]] constexpr bool __i8_to_i16
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i8_to_i32
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __i8_to_i64
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i16_to_i8
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i16_to_i32
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __i16_to_i64
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i32_to_i8
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i32_to_i16
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i32_to_i64
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i64_to_i8
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i64_to_i16
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i64_to_i32
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 4;

    // [fsu]X_to_[fsu]X {{{2
    // ibw = integral && byte or word, i.e. char and short with any signedness
    [[maybe_unused]] constexpr bool __s64_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s32_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s16_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s8_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u64_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u32_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u16_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u8_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s64_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __s32_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u64_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u32_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __f32_to_s64
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_s32
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_u64
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_u32
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f64_to_s64
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_s32
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_u64
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_u32
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __ibw_to_f32
      = is_integral_v<_Tp> && sizeof(_Tp) <= 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __ibw_to_f64
      = is_integral_v<_Tp> && sizeof(_Tp) <= 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __f32_to_ibw
      = is_integral_v<_Up> && sizeof(_Up) <= 2
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f64_to_ibw
      = is_integral_v<_Up> && sizeof(_Up) <= 2
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f32_to_f64
      = is_floating_point_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __f64_to_f32
      = is_floating_point_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;

    if constexpr (__i_to_i && __y_to_x && !__have_avx2) //{{{2
      return __convert_x86<_To>(__lo128(__v), __hi128(__v));
    else if constexpr (__i_to_i && __x_to_y && !__have_avx2) //{{{2
      return __concat(__convert_x86<__vector_type_t<_Up, _M / 2>>(__v),
		      __convert_x86<__vector_type_t<_Up, _M / 2>>(
			__extract_part<1, _Np / _M * 2>(__v)));
    else if constexpr (__i_to_i) //{{{2
      {
	static_assert(__x_to_x || __have_avx2,
		      "integral conversions with ymm registers require AVX2");
	static_assert(__have_avx512bw
			|| ((sizeof(_Tp) >= 4 || sizeof(__v) < 64)
			    && (sizeof(_Up) >= 4 || sizeof(_To) < 64)),
		      "8/16-bit integers in zmm registers require AVX512BW");
	static_assert((sizeof(__v) < 64 && sizeof(_To) < 64) || __have_avx512f,
		      "integral conversions with ymm registers require AVX2");
      }
    if constexpr (is_floating_point_v<_Tp> == is_floating_point_v<_Up> && //{{{2
		  sizeof(_Tp) == sizeof(_Up))
      {
	// conversion uses simple bit reinterpretation (or no conversion at all)
	if constexpr (_Np >= _M)
	  return __intrin_bitcast<_To>(__v);
	else
	  return __zero_extend(__vector_bitcast<_Up>(__v));
      }
    else if constexpr (_Np < _M && sizeof(_To) > 16) //{{{2
      // zero extend (eg. xmm -> ymm)
      return __zero_extend(
	__convert_x86<__vector_type_t<
	  _Up, (16 / sizeof(_Up) > _Np) ? 16 / sizeof(_Up) : _Np>>(__v));
    else if constexpr (_Np > _M && sizeof(__v) > 16) //{{{2
      // partial input (eg. ymm -> xmm)
      return __convert_x86<_To>(__extract_part<0, _Np / _M>(__v));
    else if constexpr (__i64_to_i32) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm_cvtepi64_epi32(__intrin));
	else if constexpr (__x_to_x)
	  return __auto_bitcast(
	    _mm_shuffle_ps(__vector_bitcast<float>(__v), __m128(), 8));
	else if constexpr (__y_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepi64_epi32(__intrin));
	else if constexpr (__y_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepi64_epi32(__auto_bitcast(__v))));
	else if constexpr (__y_to_x)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm256_permute4x64_epi64(_mm256_shuffle_epi32(__intrin, 8),
					     0 + 4 * 2)));
	else if constexpr (__z_to_y)
	  return __intrin_bitcast<_To>(_mm512_cvtepi64_epi32(__intrin));
      }
    else if constexpr (__i64_to_i16) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm_cvtepi64_epi16(__intrin));
	else if constexpr (__x_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepi64_epi16(__auto_bitcast(__v))));
	else if constexpr (__x_to_x && __have_ssse3)
	  {
	    return __intrin_bitcast<_To>(
	      _mm_shuffle_epi8(__intrin,
			       _mm_setr_epi8(0, 1, 8, 9, -0x80, -0x80, -0x80,
					     -0x80, -0x80, -0x80, -0x80, -0x80,
					     -0x80, -0x80, -0x80, -0x80)));
	    // fallback without SSSE3
	  }
	else if constexpr (__y_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepi64_epi16(__intrin));
	else if constexpr (__y_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepi64_epi16(__auto_bitcast(__v))));
	else if constexpr (__y_to_x)
	  {
	    const auto __a = _mm256_shuffle_epi8(
	      __intrin,
	      _mm256_setr_epi8(0, 1, 8, 9, -0x80, -0x80, -0x80, -0x80, -0x80,
			       -0x80, -0x80, -0x80, -0x80, -0x80, -0x80, -0x80,
			       -0x80, -0x80, -0x80, -0x80, 0, 1, 8, 9, -0x80,
			       -0x80, -0x80, -0x80, -0x80, -0x80, -0x80,
			       -0x80));
	    return __intrin_bitcast<_To>(__lo128(__a) | __hi128(__a));
	  }
	else if constexpr (__z_to_x)
	  return __intrin_bitcast<_To>(_mm512_cvtepi64_epi16(__intrin));
      }
    else if constexpr (__i64_to_i8) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm_cvtepi64_epi8(__intrin));
	else if constexpr (__x_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepi64_epi8(__zero_extend(__intrin))));
	else if constexpr (__y_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepi64_epi8(__intrin));
	else if constexpr (__y_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    _mm512_cvtepi64_epi8(__zero_extend(__intrin)));
	else if constexpr (__z_to_x)
	  return __intrin_bitcast<_To>(_mm512_cvtepi64_epi8(__intrin));
      }
    else if constexpr (__i32_to_i64) //{{{2
      {
	if constexpr (__have_sse4_1 && __x_to_x)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm_cvtepi32_epi64(__intrin)
					 : _mm_cvtepu32_epi64(__intrin));
	else if constexpr (__x_to_x)
	  {
	    return __intrin_bitcast<_To>(
	      _mm_unpacklo_epi32(__intrin, is_signed_v<_Tp>
					     ? _mm_srai_epi32(__intrin, 31)
					     : __m128i()));
	  }
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm256_cvtepi32_epi64(__intrin)
					 : _mm256_cvtepu32_epi64(__intrin));
	else if constexpr (__y_to_z)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm512_cvtepi32_epi64(__intrin)
					 : _mm512_cvtepu32_epi64(__intrin));
      }
    else if constexpr (__i32_to_i16) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm_cvtepi32_epi16(__intrin));
	else if constexpr (__x_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepi32_epi16(__auto_bitcast(__v))));
	else if constexpr (__x_to_x && __have_ssse3)
	  return __intrin_bitcast<_To>(_mm_shuffle_epi8(
	    __intrin, _mm_setr_epi8(0, 1, 4, 5, 8, 9, 12, 13, -0x80, -0x80,
				    -0x80, -0x80, -0x80, -0x80, -0x80, -0x80)));
	else if constexpr (__x_to_x)
	  {
	    auto __a = _mm_unpacklo_epi16(__intrin, __m128i()); // 0o.o 1o.o
	    auto __b = _mm_unpackhi_epi16(__intrin, __m128i()); // 2o.o 3o.o
	    auto __c = _mm_unpacklo_epi16(__a, __b);            // 02oo ..oo
	    auto __d = _mm_unpackhi_epi16(__a, __b);            // 13oo ..oo
	    return __intrin_bitcast<_To>(
	      _mm_unpacklo_epi16(__c, __d)); // 0123 oooo
	  }
	else if constexpr (__y_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepi32_epi16(__intrin));
	else if constexpr (__y_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepi32_epi16(__auto_bitcast(__v))));
	else if constexpr (__y_to_x)
	  {
	    auto __a = _mm256_shuffle_epi8(
	      __intrin,
	      _mm256_setr_epi8(0, 1, 4, 5, 8, 9, 12, 13, -0x80, -0x80, -0x80,
			       -0x80, -0x80, -0x80, -0x80, -0x80, 0, 1, 4, 5, 8,
			       9, 12, 13, -0x80, -0x80, -0x80, -0x80, -0x80,
			       -0x80, -0x80, -0x80));
	    return __intrin_bitcast<_To>(__lo128(
	      _mm256_permute4x64_epi64(__a,
				       0xf8))); // __a[0] __a[2] | __a[3] __a[3]
	  }
	else if constexpr (__z_to_y)
	  return __intrin_bitcast<_To>(_mm512_cvtepi32_epi16(__intrin));
      }
    else if constexpr (__i32_to_i8) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm_cvtepi32_epi8(__intrin));
	else if constexpr (__x_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepi32_epi8(__zero_extend(__intrin))));
	else if constexpr (__x_to_x && __have_ssse3)
	  {
	    return __intrin_bitcast<_To>(
	      _mm_shuffle_epi8(__intrin,
			       _mm_setr_epi8(0, 4, 8, 12, -0x80, -0x80, -0x80,
					     -0x80, -0x80, -0x80, -0x80, -0x80,
					     -0x80, -0x80, -0x80, -0x80)));
	  }
	else if constexpr (__x_to_x)
	  {
	    const auto __a
	      = _mm_unpacklo_epi8(__intrin, __intrin); // 0... .... 1... ....
	    const auto __b
	      = _mm_unpackhi_epi8(__intrin, __intrin);    // 2... .... 3... ....
	    const auto __c = _mm_unpacklo_epi8(__a, __b); // 02.. .... .... ....
	    const auto __d = _mm_unpackhi_epi8(__a, __b); // 13.. .... .... ....
	    const auto __e = _mm_unpacklo_epi8(__c, __d); // 0123 .... .... ....
	    return __intrin_bitcast<_To>(__e & _mm_cvtsi32_si128(-1));
	  }
	else if constexpr (__y_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepi32_epi8(__intrin));
	else if constexpr (__y_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    _mm512_cvtepi32_epi8(__zero_extend(__intrin)));
	else if constexpr (__z_to_x)
	  return __intrin_bitcast<_To>(_mm512_cvtepi32_epi8(__intrin));
      }
    else if constexpr (__i16_to_i64) //{{{2
      {
	if constexpr (__x_to_x && __have_sse4_1)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm_cvtepi16_epi64(__intrin)
					 : _mm_cvtepu16_epi64(__intrin));
	else if constexpr (__x_to_x && is_signed_v<_Tp>)
	  {
	    auto __x = _mm_srai_epi16(__intrin, 15);
	    auto __y = _mm_unpacklo_epi16(__intrin, __x);
	    __x = _mm_unpacklo_epi16(__x, __x);
	    return __intrin_bitcast<_To>(_mm_unpacklo_epi32(__y, __x));
	  }
	else if constexpr (__x_to_x)
	  return __intrin_bitcast<_To>(
	    _mm_unpacklo_epi32(_mm_unpacklo_epi16(__intrin, __m128i()),
			       __m128i()));
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm256_cvtepi16_epi64(__intrin)
					 : _mm256_cvtepu16_epi64(__intrin));
	else if constexpr (__x_to_z)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm512_cvtepi16_epi64(__intrin)
					 : _mm512_cvtepu16_epi64(__intrin));
      }
    else if constexpr (__i16_to_i32) //{{{2
      {
	if constexpr (__x_to_x && __have_sse4_1)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm_cvtepi16_epi32(__intrin)
					 : _mm_cvtepu16_epi32(__intrin));
	else if constexpr (__x_to_x && is_signed_v<_Tp>)
	  return __intrin_bitcast<_To>(
	    _mm_srai_epi32(_mm_unpacklo_epi16(__intrin, __intrin), 16));
	else if constexpr (__x_to_x && is_unsigned_v<_Tp>)
	  return __intrin_bitcast<_To>(_mm_unpacklo_epi16(__intrin, __m128i()));
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm256_cvtepi16_epi32(__intrin)
					 : _mm256_cvtepu16_epi32(__intrin));
	else if constexpr (__y_to_z)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm512_cvtepi16_epi32(__intrin)
					 : _mm512_cvtepu16_epi32(__intrin));
      }
    else if constexpr (__i16_to_i8) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512bw_vl)
	  return __intrin_bitcast<_To>(_mm_cvtepi16_epi8(__intrin));
	else if constexpr (__x_to_x && __have_avx512bw)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepi16_epi8(__zero_extend(__intrin))));
	else if constexpr (__x_to_x && __have_ssse3)
	  return __intrin_bitcast<_To>(_mm_shuffle_epi8(
	    __intrin, _mm_setr_epi8(0, 2, 4, 6, 8, 10, 12, 14, -0x80, -0x80,
				    -0x80, -0x80, -0x80, -0x80, -0x80, -0x80)));
	else if constexpr (__x_to_x)
	  {
	    auto __a
	      = _mm_unpacklo_epi8(__intrin, __intrin); // 00.. 11.. 22.. 33..
	    auto __b
	      = _mm_unpackhi_epi8(__intrin, __intrin); // 44.. 55.. 66.. 77..
	    auto __c = _mm_unpacklo_epi8(__a, __b);    // 0404 .... 1515 ....
	    auto __d = _mm_unpackhi_epi8(__a, __b);    // 2626 .... 3737 ....
	    auto __e = _mm_unpacklo_epi8(__c, __d);    // 0246 0246 .... ....
	    auto __f = _mm_unpackhi_epi8(__c, __d);    // 1357 1357 .... ....
	    return __intrin_bitcast<_To>(_mm_unpacklo_epi8(__e, __f));
	  }
	else if constexpr (__y_to_x && __have_avx512bw_vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepi16_epi8(__intrin));
	else if constexpr (__y_to_x && __have_avx512bw)
	  return __intrin_bitcast<_To>(
	    __lo256(_mm512_cvtepi16_epi8(__zero_extend(__intrin))));
	else if constexpr (__y_to_x)
	  {
	    auto __a = _mm256_shuffle_epi8(
	      __intrin,
	      _mm256_setr_epi8(0, 2, 4, 6, 8, 10, 12, 14, -0x80, -0x80, -0x80,
			       -0x80, -0x80, -0x80, -0x80, -0x80, -0x80, -0x80,
			       -0x80, -0x80, -0x80, -0x80, -0x80, -0x80, 0, 2,
			       4, 6, 8, 10, 12, 14));
	    return __intrin_bitcast<_To>(__lo128(__a) | __hi128(__a));
	  }
	else if constexpr (__z_to_y && __have_avx512bw)
	  return __intrin_bitcast<_To>(_mm512_cvtepi16_epi8(__intrin));
	else if constexpr (__z_to_y)
	  __assert_unreachable<_Tp>();
      }
    else if constexpr (__i8_to_i64) //{{{2
      {
	if constexpr (__x_to_x && __have_sse4_1)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm_cvtepi8_epi64(__intrin)
					 : _mm_cvtepu8_epi64(__intrin));
	else if constexpr (__x_to_x && is_signed_v<_Tp>)
	  {
	    if constexpr (__have_ssse3)
	      {
		auto __dup = _mm_unpacklo_epi8(__intrin, __intrin);
		auto __epi16 = _mm_srai_epi16(__dup, 8);
		_mm_shuffle_epi8(__epi16,
				 _mm_setr_epi8(0, 1, 1, 1, 1, 1, 1, 1, 2, 3, 3,
					       3, 3, 3, 3, 3));
	      }
	    else
	      {
		auto __x = _mm_unpacklo_epi8(__intrin, __intrin);
		__x = _mm_unpacklo_epi16(__x, __x);
		return __intrin_bitcast<_To>(
		  _mm_unpacklo_epi32(_mm_srai_epi32(__x, 24),
				     _mm_srai_epi32(__x, 31)));
	      }
	  }
	else if constexpr (__x_to_x)
	  {
	    return __intrin_bitcast<_To>(_mm_unpacklo_epi32(
	      _mm_unpacklo_epi16(_mm_unpacklo_epi8(__intrin, __m128i()),
				 __m128i()),
	      __m128i()));
	  }
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm256_cvtepi8_epi64(__intrin)
					 : _mm256_cvtepu8_epi64(__intrin));
	else if constexpr (__x_to_z)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm512_cvtepi8_epi64(__intrin)
					 : _mm512_cvtepu8_epi64(__intrin));
      }
    else if constexpr (__i8_to_i32) //{{{2
      {
	if constexpr (__x_to_x && __have_sse4_1)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm_cvtepi8_epi32(__intrin)
					 : _mm_cvtepu8_epi32(__intrin));
	else if constexpr (__x_to_x && is_signed_v<_Tp>)
	  {
	    const auto __x = _mm_unpacklo_epi8(__intrin, __intrin);
	    return __intrin_bitcast<_To>(
	      _mm_srai_epi32(_mm_unpacklo_epi16(__x, __x), 24));
	  }
	else if constexpr (__x_to_x && is_unsigned_v<_Tp>)
	  return __intrin_bitcast<_To>(
	    _mm_unpacklo_epi16(_mm_unpacklo_epi8(__intrin, __m128i()),
			       __m128i()));
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm256_cvtepi8_epi32(__intrin)
					 : _mm256_cvtepu8_epi32(__intrin));
	else if constexpr (__x_to_z)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm512_cvtepi8_epi32(__intrin)
					 : _mm512_cvtepu8_epi32(__intrin));
      }
    else if constexpr (__i8_to_i16) //{{{2
      {
	if constexpr (__x_to_x && __have_sse4_1)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm_cvtepi8_epi16(__intrin)
					 : _mm_cvtepu8_epi16(__intrin));
	else if constexpr (__x_to_x && is_signed_v<_Tp>)
	  return __intrin_bitcast<_To>(
	    _mm_srai_epi16(_mm_unpacklo_epi8(__intrin, __intrin), 8));
	else if constexpr (__x_to_x && is_unsigned_v<_Tp>)
	  return __intrin_bitcast<_To>(_mm_unpacklo_epi8(__intrin, __m128i()));
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm256_cvtepi8_epi16(__intrin)
					 : _mm256_cvtepu8_epi16(__intrin));
	else if constexpr (__y_to_z && __have_avx512bw)
	  return __intrin_bitcast<_To>(is_signed_v<_Tp>
					 ? _mm512_cvtepi8_epi16(__intrin)
					 : _mm512_cvtepu8_epi16(__intrin));
	else if constexpr (__y_to_z)
	  __assert_unreachable<_Tp>();
      }
    else if constexpr (__f32_to_s64) //{{{2
      {
	if constexpr (__have_avx512dq_vl && __x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvttps_epi64(__intrin));
	else if constexpr (__have_avx512dq_vl && __x_to_y)
	  return __intrin_bitcast<_To>(_mm256_cvttps_epi64(__intrin));
	else if constexpr (__have_avx512dq && __y_to_z)
	  return __intrin_bitcast<_To>(_mm512_cvttps_epi64(__intrin));
	// else use scalar fallback
      }
    else if constexpr (__f32_to_u64) //{{{2
      {
	if constexpr (__have_avx512dq_vl && __x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvttps_epu64(__intrin));
	else if constexpr (__have_avx512dq_vl && __x_to_y)
	  return __intrin_bitcast<_To>(_mm256_cvttps_epu64(__intrin));
	else if constexpr (__have_avx512dq && __y_to_z)
	  return __intrin_bitcast<_To>(_mm512_cvttps_epu64(__intrin));
	// else use scalar fallback
      }
    else if constexpr (__f32_to_s32) //{{{2
      {
	if constexpr (__x_to_x || __y_to_y || __z_to_z)
	  {
	    // go to fallback, it does the right thing
	  }
	else
	  __assert_unreachable<_Tp>();
      }
    else if constexpr (__f32_to_u32) //{{{2
      {
	if constexpr (__have_avx512vl && __x_to_x)
	  return __auto_bitcast(_mm_cvttps_epu32(__intrin));
	else if constexpr (__have_avx512f && __x_to_x)
	  return __auto_bitcast(
	    __lo128(_mm512_cvttps_epu32(__auto_bitcast(__v))));
	else if constexpr (__have_avx512vl && __y_to_y)
	  return __vector_bitcast<_Up>(_mm256_cvttps_epu32(__intrin));
	else if constexpr (__have_avx512f && __y_to_y)
	  return __vector_bitcast<_Up>(
	    __lo256(_mm512_cvttps_epu32(__auto_bitcast(__v))));
	else if constexpr (__x_to_x || __y_to_y || __z_to_z)
	  {
	    // go to fallback, it does the right thing. We can't use the
	    // _mm_floor_ps - 0x8000'0000 trick for f32->u32 because it would
	    // discard small input values (only 24 mantissa bits)
	  }
	else
	  __assert_unreachable<_Tp>();
      }
    else if constexpr (__f32_to_ibw) //{{{2
      return __convert_x86<_To>(__convert_x86<__vector_type_t<int, _Np>>(__v));
    else if constexpr (__f64_to_s64) //{{{2
      {
	if constexpr (__have_avx512dq_vl && __x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvttpd_epi64(__intrin));
	else if constexpr (__have_avx512dq_vl && __y_to_y)
	  return __intrin_bitcast<_To>(_mm256_cvttpd_epi64(__intrin));
	else if constexpr (__have_avx512dq && __z_to_z)
	  return __intrin_bitcast<_To>(_mm512_cvttpd_epi64(__intrin));
	// else use scalar fallback
      }
    else if constexpr (__f64_to_u64) //{{{2
      {
	if constexpr (__have_avx512dq_vl && __x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvttpd_epu64(__intrin));
	else if constexpr (__have_avx512dq_vl && __y_to_y)
	  return __intrin_bitcast<_To>(_mm256_cvttpd_epu64(__intrin));
	else if constexpr (__have_avx512dq && __z_to_z)
	  return __intrin_bitcast<_To>(_mm512_cvttpd_epu64(__intrin));
	// else use scalar fallback
      }
    else if constexpr (__f64_to_s32) //{{{2
      {
	if constexpr (__x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvttpd_epi32(__intrin));
	else if constexpr (__y_to_x)
	  return __intrin_bitcast<_To>(_mm256_cvttpd_epi32(__intrin));
	else if constexpr (__z_to_y)
	  return __intrin_bitcast<_To>(_mm512_cvttpd_epi32(__intrin));
      }
    else if constexpr (__f64_to_u32) //{{{2
      {
	if constexpr (__have_avx512vl && __x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvttpd_epu32(__intrin));
	else if constexpr (__have_sse4_1 && __x_to_x)
	  return __vector_bitcast<_Up, _M>(
		   _mm_cvttpd_epi32(_mm_floor_pd(__intrin) - 0x8000'0000u))
		 ^ 0x8000'0000u;
	else if constexpr (__x_to_x)
	  {
	    // use scalar fallback: it's only 2 values to convert, can't get
	    // much better than scalar decomposition
	  }
	else if constexpr (__have_avx512vl && __y_to_x)
	  return __intrin_bitcast<_To>(_mm256_cvttpd_epu32(__intrin));
	else if constexpr (__y_to_x)
	  {
	    return __intrin_bitcast<_To>(
	      __vector_bitcast<_Up>(
		_mm256_cvttpd_epi32(_mm256_floor_pd(__intrin) - 0x8000'0000u))
	      ^ 0x8000'0000u);
	  }
	else if constexpr (__z_to_y)
	  return __intrin_bitcast<_To>(_mm512_cvttpd_epu32(__intrin));
      }
    else if constexpr (__f64_to_ibw) //{{{2
      {
	return __convert_x86<_To>(
	  __convert_x86<__vector_type_t<int, (_Np < 4 ? 4 : _Np)>>(__v));
      }
    else if constexpr (__s64_to_f32) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512dq_vl)
	  return __intrin_bitcast<_To>(_mm_cvtepi64_ps(__intrin));
	else if constexpr (__y_to_x && __have_avx512dq_vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepi64_ps(__intrin));
	else if constexpr (__z_to_y && __have_avx512dq)
	  return __intrin_bitcast<_To>(_mm512_cvtepi64_ps(__intrin));
	else if constexpr (__z_to_y)
	  return __intrin_bitcast<_To>(
	    _mm512_cvtpd_ps(__convert_x86<__vector_type_t<double, 8>>(__v)));
      }
    else if constexpr (__u64_to_f32) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512dq_vl)
	  return __intrin_bitcast<_To>(_mm_cvtepu64_ps(__intrin));
	else if constexpr (__y_to_x && __have_avx512dq_vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepu64_ps(__intrin));
	else if constexpr (__z_to_y && __have_avx512dq)
	  return __intrin_bitcast<_To>(_mm512_cvtepu64_ps(__intrin));
	else if constexpr (__z_to_y)
	  {
	    return __intrin_bitcast<_To>(
	      __lo256(_mm512_cvtepu32_ps(__auto_bitcast(
		_mm512_cvtepi64_epi32(_mm512_srai_epi64(__intrin, 32)))))
		* 0x100000000LL
	      + __lo256(_mm512_cvtepu32_ps(
		__auto_bitcast(_mm512_cvtepi64_epi32(__intrin)))));
	  }
      }
    else if constexpr (__s32_to_f32) //{{{2
      {
	// use fallback (builtin conversion)
      }
    else if constexpr (__u32_to_f32) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512vl)
	  {
	    // use fallback
	  }
	else if constexpr (__x_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepu32_ps(__auto_bitcast(__v))));
	else if constexpr (__x_to_x && (__have_fma || __have_fma4))
	  // work around PR85819
	  return __auto_bitcast(0x10000
				  * _mm_cvtepi32_ps(__to_intrin(__v >> 16))
				+ _mm_cvtepi32_ps(__to_intrin(__v & 0xffff)));
	else if constexpr (__y_to_y && __have_avx512vl)
	  {
	    // use fallback
	  }
	else if constexpr (__y_to_y && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo256(_mm512_cvtepu32_ps(__auto_bitcast(__v))));
	else if constexpr (__y_to_y)
	  // work around PR85819
	  return 0x10000 * _mm256_cvtepi32_ps(__to_intrin(__v >> 16))
		 + _mm256_cvtepi32_ps(__to_intrin(__v & 0xffff));
	// else use fallback (builtin conversion)
      }
    else if constexpr (__ibw_to_f32) //{{{2
      {
	if constexpr (_M <= 4 || __have_avx2)
	  return __convert_x86<_To>(
	    __convert_x86<__vector_type_t<int, _M>>(__v));
	else
	  {
	    static_assert(__x_to_y);
	    __m128i __a, __b;
	    if constexpr (__have_sse4_1)
	      {
		__a = sizeof(_Tp) == 2
			? (is_signed_v<_Tp> ? _mm_cvtepi16_epi32(__intrin)
					    : _mm_cvtepu16_epi32(__intrin))
			: (is_signed_v<_Tp> ? _mm_cvtepi8_epi32(__intrin)
					    : _mm_cvtepu8_epi32(__intrin));
		const auto __w
		  = _mm_shuffle_epi32(__intrin, sizeof(_Tp) == 2 ? 0xee : 0xe9);
		__b = sizeof(_Tp) == 2
			? (is_signed_v<_Tp> ? _mm_cvtepi16_epi32(__w)
					    : _mm_cvtepu16_epi32(__w))
			: (is_signed_v<_Tp> ? _mm_cvtepi8_epi32(__w)
					    : _mm_cvtepu8_epi32(__w));
	      }
	    else
	      {
		__m128i __tmp;
		if constexpr (sizeof(_Tp) == 1)
		  {
		    __tmp = is_signed_v<_Tp>
			      ? _mm_srai_epi16(_mm_unpacklo_epi8(__intrin,
								 __intrin),
					       8)
			      : _mm_unpacklo_epi8(__intrin, __m128i());
		  }
		else
		  {
		    static_assert(sizeof(_Tp) == 2);
		    __tmp = __intrin;
		  }
		__a = is_signed_v<_Tp>
			? _mm_srai_epi32(_mm_unpacklo_epi16(__tmp, __tmp), 16)
			: _mm_unpacklo_epi16(__tmp, __m128i());
		__b = is_signed_v<_Tp>
			? _mm_srai_epi32(_mm_unpackhi_epi16(__tmp, __tmp), 16)
			: _mm_unpackhi_epi16(__tmp, __m128i());
	      }
	    return __convert_x86<_To>(__vector_bitcast<int>(__a),
				      __vector_bitcast<int>(__b));
	  }
      }
    else if constexpr (__s64_to_f64) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512dq_vl)
	  return __intrin_bitcast<_To>(_mm_cvtepi64_pd(__intrin));
	else if constexpr (__y_to_y && __have_avx512dq_vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepi64_pd(__intrin));
	else if constexpr (__z_to_z && __have_avx512dq)
	  return __intrin_bitcast<_To>(_mm512_cvtepi64_pd(__intrin));
	else if constexpr (__z_to_z)
	  {
	    return __intrin_bitcast<_To>(
	      _mm512_cvtepi32_pd(_mm512_cvtepi64_epi32(__to_intrin(__v >> 32)))
		* 0x100000000LL
	      + _mm512_cvtepu32_pd(_mm512_cvtepi64_epi32(__intrin)));
	  }
      }
    else if constexpr (__u64_to_f64) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512dq_vl)
	  return __intrin_bitcast<_To>(_mm_cvtepu64_pd(__intrin));
	else if constexpr (__y_to_y && __have_avx512dq_vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepu64_pd(__intrin));
	else if constexpr (__z_to_z && __have_avx512dq)
	  return __intrin_bitcast<_To>(_mm512_cvtepu64_pd(__intrin));
	else if constexpr (__z_to_z)
	  {
	    return __intrin_bitcast<_To>(
	      _mm512_cvtepu32_pd(_mm512_cvtepi64_epi32(__to_intrin(__v >> 32)))
		* 0x100000000LL
	      + _mm512_cvtepu32_pd(_mm512_cvtepi64_epi32(__intrin)));
	  }
      }
    else if constexpr (__s32_to_f64) //{{{2
      {
	if constexpr (__x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvtepi32_pd(__intrin));
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(_mm256_cvtepi32_pd(__intrin));
	else if constexpr (__y_to_z)
	  return __intrin_bitcast<_To>(_mm512_cvtepi32_pd(__intrin));
      }
    else if constexpr (__u32_to_f64) //{{{2
      {
	if constexpr (__x_to_x && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm_cvtepu32_pd(__intrin));
	else if constexpr (__x_to_x && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo128(_mm512_cvtepu32_pd(__auto_bitcast(__v))));
	else if constexpr (__x_to_x)
	  return __intrin_bitcast<_To>(
	    _mm_cvtepi32_pd(__to_intrin(__v ^ 0x8000'0000u)) + 0x8000'0000u);
	else if constexpr (__x_to_y && __have_avx512vl)
	  return __intrin_bitcast<_To>(_mm256_cvtepu32_pd(__intrin));
	else if constexpr (__x_to_y && __have_avx512f)
	  return __intrin_bitcast<_To>(
	    __lo256(_mm512_cvtepu32_pd(__auto_bitcast(__v))));
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(
	    _mm256_cvtepi32_pd(__to_intrin(__v ^ 0x8000'0000u)) + 0x8000'0000u);
	else if constexpr (__y_to_z)
	  return __intrin_bitcast<_To>(_mm512_cvtepu32_pd(__intrin));
      }
    else if constexpr (__ibw_to_f64) //{{{2
      {
	return __convert_x86<_To>(
	  __convert_x86<__vector_type_t<int, std::max(size_t(4), _M)>>(__v));
      }
    else if constexpr (__f32_to_f64) //{{{2
      {
	if constexpr (__x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvtps_pd(__intrin));
	else if constexpr (__x_to_y)
	  return __intrin_bitcast<_To>(_mm256_cvtps_pd(__intrin));
	else if constexpr (__y_to_z)
	  return __intrin_bitcast<_To>(_mm512_cvtps_pd(__intrin));
      }
    else if constexpr (__f64_to_f32) //{{{2
      {
	if constexpr (__x_to_x)
	  return __intrin_bitcast<_To>(_mm_cvtpd_ps(__intrin));
	else if constexpr (__y_to_x)
	  return __intrin_bitcast<_To>(_mm256_cvtpd_ps(__intrin));
	else if constexpr (__z_to_y)
	  return __intrin_bitcast<_To>(_mm512_cvtpd_ps(__intrin));
      }
    else //{{{2
      __assert_unreachable<_Tp>();

    // fallback:{{{2
    return __vector_convert<_To>(__v, make_index_sequence<std::min(_M, _Np)>());
    //}}}
  }

// }}}
// 2-arg __convert_x86 {{{1
template <typename _To, typename _V, typename _Traits>
  _GLIBCXX_SIMD_INTRINSIC _To
  __convert_x86(_V __v0, _V __v1)
  {
    static_assert(__is_vector_type_v<_V>);
    using _Tp = typename _Traits::value_type;
    constexpr size_t _Np = _Traits::_S_full_size;
    [[maybe_unused]] const auto __i0 = __to_intrin(__v0);
    [[maybe_unused]] const auto __i1 = __to_intrin(__v1);
    using _Up = typename _VectorTraits<_To>::value_type;
    constexpr size_t _M = _VectorTraits<_To>::_S_full_size;

    static_assert(2 * _Np <= _M,
		  "__v1 would be discarded; use the one-argument "
		  "__convert_x86 overload instead");

    // [xyz]_to_[xyz] {{{2
    [[maybe_unused]] constexpr bool __x_to_x
      = sizeof(__v0) <= 16 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __x_to_y
      = sizeof(__v0) <= 16 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __x_to_z
      = sizeof(__v0) <= 16 && sizeof(_To) == 64;
    [[maybe_unused]] constexpr bool __y_to_x
      = sizeof(__v0) == 32 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __y_to_y
      = sizeof(__v0) == 32 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __y_to_z
      = sizeof(__v0) == 32 && sizeof(_To) == 64;
    [[maybe_unused]] constexpr bool __z_to_x
      = sizeof(__v0) == 64 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __z_to_y
      = sizeof(__v0) == 64 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __z_to_z
      = sizeof(__v0) == 64 && sizeof(_To) == 64;

    // iX_to_iX {{{2
    [[maybe_unused]] constexpr bool __i_to_i
      = is_integral_v<_Up> && is_integral_v<_Tp>;
    [[maybe_unused]] constexpr bool __i8_to_i16
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i8_to_i32
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __i8_to_i64
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i16_to_i8
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i16_to_i32
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __i16_to_i64
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i32_to_i8
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i32_to_i16
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i32_to_i64
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i64_to_i8
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i64_to_i16
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i64_to_i32
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 4;

    // [fsu]X_to_[fsu]X {{{2
    // ibw = integral && byte or word, i.e. char and short with any signedness
    [[maybe_unused]] constexpr bool __i64_to_f32
      = is_integral_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s32_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s16_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s8_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u32_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u16_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u8_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s64_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __s32_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __s16_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __s8_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u64_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u32_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u16_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u8_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __f32_to_s64
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_s32
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_u64
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_u32
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f64_to_s64
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_s32
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_u64
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_u32
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f32_to_ibw
      = is_integral_v<_Up> && sizeof(_Up) <= 2
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f64_to_ibw
      = is_integral_v<_Up> && sizeof(_Up) <= 2
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f32_to_f64
      = is_floating_point_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __f64_to_f32
      = is_floating_point_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;

    if constexpr (__i_to_i && __y_to_x && !__have_avx2) //{{{2
      // <double, 4>, <double, 4> => <short, 8>
      return __convert_x86<_To>(__lo128(__v0), __hi128(__v0), __lo128(__v1),
				__hi128(__v1));
    else if constexpr (__i_to_i) // assert ISA {{{2
      {
	static_assert(__x_to_x || __have_avx2,
		      "integral conversions with ymm registers require AVX2");
	static_assert(__have_avx512bw
			|| ((sizeof(_Tp) >= 4 || sizeof(__v0) < 64)
			    && (sizeof(_Up) >= 4 || sizeof(_To) < 64)),
		      "8/16-bit integers in zmm registers require AVX512BW");
	static_assert((sizeof(__v0) < 64 && sizeof(_To) < 64) || __have_avx512f,
		      "integral conversions with ymm registers require AVX2");
      }
    // concat => use 1-arg __convert_x86 {{{2
    if constexpr (sizeof(__v0) < 16 || (sizeof(__v0) == 16 && __have_avx2)
		  || (sizeof(__v0) == 16 && __have_avx
		      && is_floating_point_v<_Tp>)
		  || (sizeof(__v0) == 32 && __have_avx512f
		      && (sizeof(_Tp) >= 4 || __have_avx512bw)))
      {
	// The ISA can handle wider input registers, so concat and use one-arg
	// implementation. This reduces code duplication considerably.
	return __convert_x86<_To>(__concat(__v0, __v1));
      }
    else //{{{2
      {
	// conversion using bit reinterpretation (or no conversion at all)
	// should all go through the concat branch above:
	static_assert(
	  !(is_floating_point_v<
	      _Tp> == is_floating_point_v<_Up> && sizeof(_Tp) == sizeof(_Up)));
	// handle all zero extension{{{2
	if constexpr (2 * _Np < _M && sizeof(_To) > 16)
	  {
	    constexpr size_t Min = 16 / sizeof(_Up);
	    return __zero_extend(
	      __convert_x86<
		__vector_type_t<_Up, (Min > 2 * _Np) ? Min : 2 * _Np>>(__v0,
								       __v1));
	  }
	else if constexpr (__i64_to_i32) //{{{2
	  {
	    if constexpr (__x_to_x)
	      return __auto_bitcast(_mm_shuffle_ps(__auto_bitcast(__v0),
						   __auto_bitcast(__v1), 0x88));
	    else if constexpr (__y_to_y)
	      {
		// AVX512F is not available (would concat otherwise)
		return __auto_bitcast(
		  __xzyw(_mm256_shuffle_ps(__auto_bitcast(__v0),
					   __auto_bitcast(__v1), 0x88)));
		// alternative:
		// const auto v0_abxxcdxx = _mm256_shuffle_epi32(__v0, 8);
		// const auto v1_efxxghxx = _mm256_shuffle_epi32(__v1, 8);
		// const auto v_abefcdgh = _mm256_unpacklo_epi64(v0_abxxcdxx,
		// v1_efxxghxx); return _mm256_permute4x64_epi64(v_abefcdgh,
		// 0x01 * 0 + 0x04 * 2 + 0x10 * 1 + 0x40 * 3);  // abcdefgh
	      }
	    else if constexpr (__z_to_z)
	      return __intrin_bitcast<_To>(
		__concat(_mm512_cvtepi64_epi32(__i0),
			 _mm512_cvtepi64_epi32(__i1)));
	  }
	else if constexpr (__i64_to_i16) //{{{2
	  {
	    if constexpr (__x_to_x)
	      {
		// AVX2 is not available (would concat otherwise)
		if constexpr (__have_sse4_1)
		  {
		    return __intrin_bitcast<_To>(_mm_shuffle_epi8(
		      _mm_blend_epi16(__i0, _mm_slli_si128(__i1, 4), 0x44),
		      _mm_setr_epi8(0, 1, 8, 9, 4, 5, 12, 13, -0x80, -0x80,
				    -0x80, -0x80, -0x80, -0x80, -0x80, -0x80)));
		  }
		else
		  {
		    return __vector_type_t<_Up, _M>{_Up(__v0[0]), _Up(__v0[1]),
						    _Up(__v1[0]), _Up(__v1[1])};
		  }
	      }
	    else if constexpr (__y_to_x)
	      {
		auto __a
		  = _mm256_unpacklo_epi16(__i0, __i1); // 04.. .... 26.. ....
		auto __b
		  = _mm256_unpackhi_epi16(__i0, __i1); // 15.. .... 37.. ....
		auto __c
		  = _mm256_unpacklo_epi16(__a, __b); // 0145 .... 2367 ....
		return __intrin_bitcast<_To>(
		  _mm_unpacklo_epi32(__lo128(__c), __hi128(__c))); // 0123 4567
	      }
	    else if constexpr (__z_to_y)
	      return __intrin_bitcast<_To>(
		__concat(_mm512_cvtepi64_epi16(__i0),
			 _mm512_cvtepi64_epi16(__i1)));
	  }
	else if constexpr (__i64_to_i8) //{{{2
	  {
	    if constexpr (__x_to_x && __have_sse4_1)
	      {
		return __intrin_bitcast<_To>(_mm_shuffle_epi8(
		  _mm_blend_epi16(__i0, _mm_slli_si128(__i1, 4), 0x44),
		  _mm_setr_epi8(0, 8, 4, 12, -0x80, -0x80, -0x80, -0x80, -0x80,
				-0x80, -0x80, -0x80, -0x80, -0x80, -0x80,
				-0x80)));
	      }
	    else if constexpr (__x_to_x && __have_ssse3)
	      {
		return __intrin_bitcast<_To>(_mm_unpacklo_epi16(
		  _mm_shuffle_epi8(
		    __i0, _mm_setr_epi8(0, 8, -0x80, -0x80, -0x80, -0x80, -0x80,
					-0x80, -0x80, -0x80, -0x80, -0x80,
					-0x80, -0x80, -0x80, -0x80)),
		  _mm_shuffle_epi8(
		    __i1, _mm_setr_epi8(0, 8, -0x80, -0x80, -0x80, -0x80, -0x80,
					-0x80, -0x80, -0x80, -0x80, -0x80,
					-0x80, -0x80, -0x80, -0x80))));
	      }
	    else if constexpr (__x_to_x)
	      {
		return __vector_type_t<_Up, _M>{_Up(__v0[0]), _Up(__v0[1]),
						_Up(__v1[0]), _Up(__v1[1])};
	      }
	    else if constexpr (__y_to_x)
	      {
		const auto __a = _mm256_shuffle_epi8(
		  _mm256_blend_epi32(__i0, _mm256_slli_epi64(__i1, 32), 0xAA),
		  _mm256_setr_epi8(0, 8, -0x80, -0x80, 4, 12, -0x80, -0x80,
				   -0x80, -0x80, -0x80, -0x80, -0x80, -0x80,
				   -0x80, -0x80, -0x80, -0x80, 0, 8, -0x80,
				   -0x80, 4, 12, -0x80, -0x80, -0x80, -0x80,
				   -0x80, -0x80, -0x80, -0x80));
		return __intrin_bitcast<_To>(__lo128(__a) | __hi128(__a));
	      } // __z_to_x uses concat fallback
	  }
	else if constexpr (__i32_to_i16) //{{{2
	  {
	    if constexpr (__x_to_x)
	      {
		// AVX2 is not available (would concat otherwise)
		if constexpr (__have_sse4_1)
		  {
		    return __intrin_bitcast<_To>(_mm_shuffle_epi8(
		      _mm_blend_epi16(__i0, _mm_slli_si128(__i1, 2), 0xaa),
		      _mm_setr_epi8(0, 1, 4, 5, 8, 9, 12, 13, 2, 3, 6, 7, 10,
				    11, 14, 15)));
		  }
		else if constexpr (__have_ssse3)
		  {
		    return __intrin_bitcast<_To>(
		      _mm_hadd_epi16(__to_intrin(__v0 << 16),
				     __to_intrin(__v1 << 16)));
		    /*
		    return _mm_unpacklo_epi64(
			_mm_shuffle_epi8(__i0, _mm_setr_epi8(0, 1, 4, 5, 8, 9,
		    12, 13, 8, 9, 12, 13, 12, 13, 14, 15)),
		    _mm_shuffle_epi8(__i1, _mm_setr_epi8(0, 1, 4, 5, 8, 9, 12,
		    13, 8, 9, 12, 13, 12, 13, 14, 15)));
							   */
		  }
		else
		  {
		    auto __a = _mm_unpacklo_epi16(__i0, __i1); // 04.. 15..
		    auto __b = _mm_unpackhi_epi16(__i0, __i1); // 26.. 37..
		    auto __c = _mm_unpacklo_epi16(__a, __b);   // 0246 ....
		    auto __d = _mm_unpackhi_epi16(__a, __b);   // 1357 ....
		    return __intrin_bitcast<_To>(
		      _mm_unpacklo_epi16(__c, __d)); // 0123 4567
		  }
	      }
	    else if constexpr (__y_to_y)
	      {
		const auto __shuf
		  = _mm256_setr_epi8(0, 1, 4, 5, 8, 9, 12, 13, -0x80, -0x80,
				     -0x80, -0x80, -0x80, -0x80, -0x80, -0x80,
				     0, 1, 4, 5, 8, 9, 12, 13, -0x80, -0x80,
				     -0x80, -0x80, -0x80, -0x80, -0x80, -0x80);
		auto __a = _mm256_shuffle_epi8(__i0, __shuf);
		auto __b = _mm256_shuffle_epi8(__i1, __shuf);
		return __intrin_bitcast<_To>(
		  __xzyw(_mm256_unpacklo_epi64(__a, __b)));
	      } // __z_to_z uses concat fallback
	  }
	else if constexpr (__i32_to_i8) //{{{2
	  {
	    if constexpr (__x_to_x && __have_ssse3)
	      {
		const auto shufmask
		  = _mm_setr_epi8(0, 4, 8, 12, -0x80, -0x80, -0x80, -0x80,
				  -0x80, -0x80, -0x80, -0x80, -0x80, -0x80,
				  -0x80, -0x80);
		return __intrin_bitcast<_To>(
		  _mm_unpacklo_epi32(_mm_shuffle_epi8(__i0, shufmask),
				     _mm_shuffle_epi8(__i1, shufmask)));
	      }
	    else if constexpr (__x_to_x)
	      {
		auto __a = _mm_unpacklo_epi8(__i0, __i1); // 04.. .... 15.. ....
		auto __b = _mm_unpackhi_epi8(__i0, __i1); // 26.. .... 37.. ....
		auto __c = _mm_unpacklo_epi8(__a, __b);   // 0246 .... .... ....
		auto __d = _mm_unpackhi_epi8(__a, __b);   // 1357 .... .... ....
		auto __e = _mm_unpacklo_epi8(__c, __d);   // 0123 4567 .... ....
		return __intrin_bitcast<_To>(__e & __m128i{-1, 0});
	      }
	    else if constexpr (__y_to_x)
	      {
		const auto __a = _mm256_shuffle_epi8(
		  _mm256_blend_epi16(__i0, _mm256_slli_epi32(__i1, 16), 0xAA),
		  _mm256_setr_epi8(0, 4, 8, 12, -0x80, -0x80, -0x80, -0x80, 2,
				   6, 10, 14, -0x80, -0x80, -0x80, -0x80, -0x80,
				   -0x80, -0x80, -0x80, 0, 4, 8, 12, -0x80,
				   -0x80, -0x80, -0x80, 2, 6, 10, 14));
		return __intrin_bitcast<_To>(__lo128(__a) | __hi128(__a));
	      } // __z_to_y uses concat fallback
	  }
	else if constexpr (__i16_to_i8) //{{{2
	  {
	    if constexpr (__x_to_x && __have_ssse3)
	      {
		const auto __shuf = reinterpret_cast<__m128i>(
		  __vector_type_t<_UChar, 16>{0, 2, 4, 6, 8, 10, 12, 14, 0x80,
					      0x80, 0x80, 0x80, 0x80, 0x80,
					      0x80, 0x80});
		return __intrin_bitcast<_To>(
		  _mm_unpacklo_epi64(_mm_shuffle_epi8(__i0, __shuf),
				     _mm_shuffle_epi8(__i1, __shuf)));
	      }
	    else if constexpr (__x_to_x)
	      {
		auto __a = _mm_unpacklo_epi8(__i0, __i1); // 08.. 19.. 2A.. 3B..
		auto __b = _mm_unpackhi_epi8(__i0, __i1); // 4C.. 5D.. 6E.. 7F..
		auto __c = _mm_unpacklo_epi8(__a, __b);   // 048C .... 159D ....
		auto __d = _mm_unpackhi_epi8(__a, __b);   // 26AE .... 37BF ....
		auto __e = _mm_unpacklo_epi8(__c, __d);   // 0246 8ACE .... ....
		auto __f = _mm_unpackhi_epi8(__c, __d);   // 1357 9BDF .... ....
		return __intrin_bitcast<_To>(_mm_unpacklo_epi8(__e, __f));
	      }
	    else if constexpr (__y_to_y)
	      {
		return __intrin_bitcast<_To>(__xzyw(_mm256_shuffle_epi8(
		  (__to_intrin(__v0) & _mm256_set1_epi32(0x00ff00ff))
		    | _mm256_slli_epi16(__i1, 8),
		  _mm256_setr_epi8(0, 2, 4, 6, 8, 10, 12, 14, 1, 3, 5, 7, 9, 11,
				   13, 15, 0, 2, 4, 6, 8, 10, 12, 14, 1, 3, 5,
				   7, 9, 11, 13, 15))));
	      } // __z_to_z uses concat fallback
	  }
	else if constexpr (__i64_to_f32) //{{{2
	  {
	    if constexpr (__x_to_x)
	      return __make_wrapper<float>(__v0[0], __v0[1], __v1[0], __v1[1]);
	    else if constexpr (__y_to_y)
	      {
		static_assert(__y_to_y && __have_avx2);
		const auto __a = _mm256_unpacklo_epi32(__i0, __i1); // aeAE cgCG
		const auto __b = _mm256_unpackhi_epi32(__i0, __i1); // bfBF dhDH
		const auto __lo32
		  = _mm256_unpacklo_epi32(__a, __b); // abef cdgh
		const auto __hi32 = __vector_bitcast<
		  conditional_t<is_signed_v<_Tp>, int, _UInt>>(
		  _mm256_unpackhi_epi32(__a, __b)); // ABEF CDGH
		const auto __hi
		  = 0x100000000LL
		    * __convert_x86<__vector_type_t<float, 8>>(__hi32);
		const auto __mid
		  = 0x10000 * _mm256_cvtepi32_ps(_mm256_srli_epi32(__lo32, 16));
		const auto __lo
		  = _mm256_cvtepi32_ps(_mm256_set1_epi32(0x0000ffffu) & __lo32);
		return __xzyw((__hi + __mid) + __lo);
	      }
	    else if constexpr (__z_to_z && __have_avx512dq)
	      {
		return is_signed_v<_Tp> ? __concat(_mm512_cvtepi64_ps(__i0),
						   _mm512_cvtepi64_ps(__i1))
					: __concat(_mm512_cvtepu64_ps(__i0),
						   _mm512_cvtepu64_ps(__i1));
	      }
	    else if constexpr (__z_to_z && is_signed_v<_Tp>)
	      {
		const __m512 __hi32 = _mm512_cvtepi32_ps(
		  __concat(_mm512_cvtepi64_epi32(__to_intrin(__v0 >> 32)),
			   _mm512_cvtepi64_epi32(__to_intrin(__v1 >> 32))));
		const __m512i __lo32 = __concat(_mm512_cvtepi64_epi32(__i0),
						_mm512_cvtepi64_epi32(__i1));
		// split low 32-bits, because if __hi32 is a small negative
		// number, the 24-bit mantissa may lose important information if
		// any of the high 8 bits of __lo32 is set, leading to
		// catastrophic cancelation in the FMA
		const __m512 __hi16
		  = _mm512_cvtepu32_ps(_mm512_set1_epi32(0xffff0000u) & __lo32);
		const __m512 __lo16
		  = _mm512_cvtepi32_ps(_mm512_set1_epi32(0x0000ffffu) & __lo32);
		return (__hi32 * 0x100000000LL + __hi16) + __lo16;
	      }
	    else if constexpr (__z_to_z && is_unsigned_v<_Tp>)
	      {
		return __intrin_bitcast<_To>(
		  _mm512_cvtepu32_ps(__concat(
		    _mm512_cvtepi64_epi32(_mm512_srai_epi64(__i0, 32)),
		    _mm512_cvtepi64_epi32(_mm512_srai_epi64(__i1, 32))))
		    * 0x100000000LL
		  + _mm512_cvtepu32_ps(__concat(_mm512_cvtepi64_epi32(__i0),
						_mm512_cvtepi64_epi32(__i1))));
	      }
	  }
	else if constexpr (__f64_to_s32) //{{{2
	  {
	    // use concat fallback
	  }
	else if constexpr (__f64_to_u32) //{{{2
	  {
	    if constexpr (__x_to_x && __have_sse4_1)
	      {
		return __vector_bitcast<_Up, _M>(_mm_unpacklo_epi64(
			 _mm_cvttpd_epi32(_mm_floor_pd(__i0) - 0x8000'0000u),
			 _mm_cvttpd_epi32(_mm_floor_pd(__i1) - 0x8000'0000u)))
		       ^ 0x8000'0000u;
		// without SSE4.1 just use the scalar fallback, it's only four
		// values
	      }
	    else if constexpr (__y_to_y)
	      {
		return __vector_bitcast<_Up>(
			 __concat(_mm256_cvttpd_epi32(_mm256_floor_pd(__i0)
						      - 0x8000'0000u),
				  _mm256_cvttpd_epi32(_mm256_floor_pd(__i1)
						      - 0x8000'0000u)))
		       ^ 0x8000'0000u;
	      } // __z_to_z uses fallback
	  }
	else if constexpr (__f64_to_ibw) //{{{2
	  {
	    // one-arg __f64_to_ibw goes via _SimdWrapper<int, ?>. The fallback
	    // would go via two independet conversions to _SimdWrapper<_To> and
	    // subsequent interleaving. This is better, because f64->__i32
	    // allows to combine __v0 and __v1 into one register: if constexpr
	    // (__z_to_x || __y_to_x) {
	    return __convert_x86<_To>(
	      __convert_x86<__vector_type_t<int, _Np * 2>>(__v0, __v1));
	    //}
	  }
	else if constexpr (__f32_to_ibw) //{{{2
	  {
	    return __convert_x86<_To>(
	      __convert_x86<__vector_type_t<int, _Np>>(__v0),
	      __convert_x86<__vector_type_t<int, _Np>>(__v1));
	  } //}}}

	// fallback: {{{2
	if constexpr (sizeof(_To) >= 32)
	  // if _To is ymm or zmm, then _SimdWrapper<_Up, _M / 2> is xmm or ymm
	  return __concat(__convert_x86<__vector_type_t<_Up, _M / 2>>(__v0),
			  __convert_x86<__vector_type_t<_Up, _M / 2>>(__v1));
	else if constexpr (sizeof(_To) == 16)
	  {
	    const auto __lo = __to_intrin(__convert_x86<_To>(__v0));
	    const auto __hi = __to_intrin(__convert_x86<_To>(__v1));
	    if constexpr (sizeof(_Up) * _Np == 8)
	      {
		if constexpr (is_floating_point_v<_Up>)
		  return __auto_bitcast(
		    _mm_unpacklo_pd(__vector_bitcast<double>(__lo),
				    __vector_bitcast<double>(__hi)));
		else
		  return __intrin_bitcast<_To>(_mm_unpacklo_epi64(__lo, __hi));
	      }
	    else if constexpr (sizeof(_Up) * _Np == 4)
	      {
		if constexpr (is_floating_point_v<_Up>)
		  return __auto_bitcast(
		    _mm_unpacklo_ps(__vector_bitcast<float>(__lo),
				    __vector_bitcast<float>(__hi)));
		else
		  return __intrin_bitcast<_To>(_mm_unpacklo_epi32(__lo, __hi));
	      }
	    else if constexpr (sizeof(_Up) * _Np == 2)
	      return __intrin_bitcast<_To>(_mm_unpacklo_epi16(__lo, __hi));
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return __vector_convert<_To>(__v0, __v1, make_index_sequence<_Np>());
	//}}}
      }
  }

//}}}1
// 4-arg __convert_x86 {{{1
template <typename _To, typename _V, typename _Traits>
  _GLIBCXX_SIMD_INTRINSIC _To
  __convert_x86(_V __v0, _V __v1, _V __v2, _V __v3)
  {
    static_assert(__is_vector_type_v<_V>);
    using _Tp = typename _Traits::value_type;
    constexpr size_t _Np = _Traits::_S_full_size;
    [[maybe_unused]] const auto __i0 = __to_intrin(__v0);
    [[maybe_unused]] const auto __i1 = __to_intrin(__v1);
    [[maybe_unused]] const auto __i2 = __to_intrin(__v2);
    [[maybe_unused]] const auto __i3 = __to_intrin(__v3);
    using _Up = typename _VectorTraits<_To>::value_type;
    constexpr size_t _M = _VectorTraits<_To>::_S_full_size;

    static_assert(4 * _Np <= _M,
		  "__v2/__v3 would be discarded; use the two/one-argument "
		  "__convert_x86 overload instead");

    // [xyz]_to_[xyz] {{{2
    [[maybe_unused]] constexpr bool __x_to_x
      = sizeof(__v0) <= 16 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __x_to_y
      = sizeof(__v0) <= 16 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __x_to_z
      = sizeof(__v0) <= 16 && sizeof(_To) == 64;
    [[maybe_unused]] constexpr bool __y_to_x
      = sizeof(__v0) == 32 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __y_to_y
      = sizeof(__v0) == 32 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __y_to_z
      = sizeof(__v0) == 32 && sizeof(_To) == 64;
    [[maybe_unused]] constexpr bool __z_to_x
      = sizeof(__v0) == 64 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __z_to_y
      = sizeof(__v0) == 64 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __z_to_z
      = sizeof(__v0) == 64 && sizeof(_To) == 64;

    // iX_to_iX {{{2
    [[maybe_unused]] constexpr bool __i_to_i
      = is_integral_v<_Up> && is_integral_v<_Tp>;
    [[maybe_unused]] constexpr bool __i8_to_i16
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i8_to_i32
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __i8_to_i64
      = __i_to_i && sizeof(_Tp) == 1 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i16_to_i8
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i16_to_i32
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __i16_to_i64
      = __i_to_i && sizeof(_Tp) == 2 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i32_to_i8
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i32_to_i16
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i32_to_i64
      = __i_to_i && sizeof(_Tp) == 4 && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __i64_to_i8
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __i64_to_i16
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 2;
    [[maybe_unused]] constexpr bool __i64_to_i32
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 4;

    // [fsu]X_to_[fsu]X {{{2
    // ibw = integral && byte or word, i.e. char and short with any signedness
    [[maybe_unused]] constexpr bool __i64_to_f32
      = is_integral_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s32_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s16_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s8_to_f32
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u32_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u16_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __u8_to_f32
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;
    [[maybe_unused]] constexpr bool __s64_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __s32_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __s16_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __s8_to_f64
      = is_integral_v<_Tp> && is_signed_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u64_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u32_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u16_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 2
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __u8_to_f64
      = is_integral_v<_Tp> && is_unsigned_v<_Tp> && sizeof(_Tp) == 1
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __f32_to_s64
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_s32
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_u64
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f32_to_u32
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f64_to_s64
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_s32
      = is_integral_v<_Up> && is_signed_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_u64
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 8
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f64_to_u32
      = is_integral_v<_Up> && is_unsigned_v<_Up> && sizeof(_Up) == 4
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f32_to_ibw
      = is_integral_v<_Up> && sizeof(_Up) <= 2
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 4;
    [[maybe_unused]] constexpr bool __f64_to_ibw
      = is_integral_v<_Up> && sizeof(_Up) <= 2
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;
    [[maybe_unused]] constexpr bool __f32_to_f64
      = is_floating_point_v<_Tp> && sizeof(_Tp) == 4
	&& is_floating_point_v<_Up> && sizeof(_Up) == 8;
    [[maybe_unused]] constexpr bool __f64_to_f32
      = is_floating_point_v<_Tp> && sizeof(_Tp) == 8
	&& is_floating_point_v<_Up> && sizeof(_Up) == 4;

    if constexpr (__i_to_i && __y_to_x && !__have_avx2) //{{{2
      {
	// <double, 4>, <double, 4>, <double, 4>, <double, 4> => <char, 16>
	return __convert_x86<_To>(__lo128(__v0), __hi128(__v0), __lo128(__v1),
				  __hi128(__v1), __lo128(__v2), __hi128(__v2),
				  __lo128(__v3), __hi128(__v3));
      }
    else if constexpr (__i_to_i) // assert ISA {{{2
      {
	static_assert(__x_to_x || __have_avx2,
		      "integral conversions with ymm registers require AVX2");
	static_assert(__have_avx512bw
			|| ((sizeof(_Tp) >= 4 || sizeof(__v0) < 64)
			    && (sizeof(_Up) >= 4 || sizeof(_To) < 64)),
		      "8/16-bit integers in zmm registers require AVX512BW");
	static_assert((sizeof(__v0) < 64 && sizeof(_To) < 64) || __have_avx512f,
		      "integral conversions with ymm registers require AVX2");
      }
    // concat => use 2-arg __convert_x86 {{{2
    if constexpr (sizeof(__v0) < 16 || (sizeof(__v0) == 16 && __have_avx2)
		  || (sizeof(__v0) == 16 && __have_avx
		      && is_floating_point_v<_Tp>)
		  || (sizeof(__v0) == 32 && __have_avx512f))
      {
	// The ISA can handle wider input registers, so concat and use two-arg
	// implementation. This reduces code duplication considerably.
	return __convert_x86<_To>(__concat(__v0, __v1), __concat(__v2, __v3));
      }
    else //{{{2
      {
	// conversion using bit reinterpretation (or no conversion at all)
	// should all go through the concat branch above:
	static_assert(
	  !(is_floating_point_v<
	      _Tp> == is_floating_point_v<_Up> && sizeof(_Tp) == sizeof(_Up)));
	// handle all zero extension{{{2
	if constexpr (4 * _Np < _M && sizeof(_To) > 16)
	  {
	    constexpr size_t Min = 16 / sizeof(_Up);
	    return __zero_extend(
	      __convert_x86<
		__vector_type_t<_Up, (Min > 4 * _Np) ? Min : 4 * _Np>>(
		__v0, __v1, __v2, __v3));
	  }
	else if constexpr (__i64_to_i16) //{{{2
	  {
	    if constexpr (__x_to_x && __have_sse4_1)
	      {
		return __intrin_bitcast<_To>(_mm_shuffle_epi8(
		  _mm_blend_epi16(
		    _mm_blend_epi16(__i0, _mm_slli_si128(__i1, 2), 0x22),
		    _mm_blend_epi16(_mm_slli_si128(__i2, 4),
				    _mm_slli_si128(__i3, 6), 0x88),
		    0xcc),
		  _mm_setr_epi8(0, 1, 8, 9, 2, 3, 10, 11, 4, 5, 12, 13, 6, 7,
				14, 15)));
	      }
	    else if constexpr (__y_to_y && __have_avx2)
	      {
		return __intrin_bitcast<_To>(_mm256_shuffle_epi8(
		  __xzyw(_mm256_blend_epi16(
		    __auto_bitcast(
		      _mm256_shuffle_ps(__vector_bitcast<float>(__v0),
					__vector_bitcast<float>(__v2),
					0x88)), // 0.1. 8.9. 2.3. A.B.
		    __to_intrin(__vector_bitcast<int>(_mm256_shuffle_ps(
				  __vector_bitcast<float>(__v1),
				  __vector_bitcast<float>(__v3), 0x88))
				<< 16), // .4.5 .C.D .6.7 .E.F
		    0xaa)               // 0415 8C9D 2637 AEBF
			 ),             // 0415 2637 8C9D AEBF
		  _mm256_setr_epi8(0, 1, 4, 5, 8, 9, 12, 13, 2, 3, 6, 7, 10, 11,
				   14, 15, 0, 1, 4, 5, 8, 9, 12, 13, 2, 3, 6, 7,
				   10, 11, 14, 15)));
		/*
		auto __a = _mm256_unpacklo_epi16(__v0, __v1);  // 04.. .... 26..
		.... auto __b = _mm256_unpackhi_epi16(__v0, __v1);  // 15..
		.... 37.. .... auto __c = _mm256_unpacklo_epi16(__v2, __v3);  //
		8C.. .... AE.. .... auto __d = _mm256_unpackhi_epi16(__v2,
		__v3);
		// 9D.. .... BF.. .... auto __e = _mm256_unpacklo_epi16(__a,
		__b);
		// 0145 .... 2367 .... auto __f = _mm256_unpacklo_epi16(__c,
		__d);
		// 89CD .... ABEF .... auto __g = _mm256_unpacklo_epi64(__e,
		__f);
		// 0145 89CD 2367 ABEF return __concat(
		    _mm_unpacklo_epi32(__lo128(__g), __hi128(__g)),
		    _mm_unpackhi_epi32(__lo128(__g), __hi128(__g)));  // 0123
		4567 89AB CDEF
		    */
	      } // else use fallback
	  }
	else if constexpr (__i64_to_i8) //{{{2
	  {
	    if constexpr (__x_to_x)
	      {
		// TODO: use fallback for now
	      }
	    else if constexpr (__y_to_x)
	      {
		auto __a
		  = _mm256_srli_epi32(_mm256_slli_epi32(__i0, 24), 24)
		    | _mm256_srli_epi32(_mm256_slli_epi32(__i1, 24), 16)
		    | _mm256_srli_epi32(_mm256_slli_epi32(__i2, 24), 8)
		    | _mm256_slli_epi32(
		      __i3, 24); // 048C .... 159D .... 26AE .... 37BF ....
		/*return _mm_shuffle_epi8(
		    _mm_blend_epi32(__lo128(__a) << 32, __hi128(__a), 0x5),
		    _mm_setr_epi8(4, 12, 0, 8, 5, 13, 1, 9, 6, 14, 2, 10, 7, 15,
		   3, 11));*/
		auto __b = _mm256_unpackhi_epi64(
		  __a, __a); // 159D .... 159D .... 37BF .... 37BF ....
		auto __c = _mm256_unpacklo_epi8(
		  __a, __b); // 0145 89CD .... .... 2367 ABEF .... ....
		return __intrin_bitcast<_To>(
		  _mm_unpacklo_epi16(__lo128(__c),
				     __hi128(__c))); // 0123 4567 89AB CDEF
	      }
	  }
	else if constexpr (__i32_to_i8) //{{{2
	  {
	    if constexpr (__x_to_x)
	      {
		if constexpr (__have_ssse3)
		  {
		    const auto __x0 = __vector_bitcast<_UInt>(__v0) & 0xff;
		    const auto __x1 = (__vector_bitcast<_UInt>(__v1) & 0xff)
				      << 8;
		    const auto __x2 = (__vector_bitcast<_UInt>(__v2) & 0xff)
				      << 16;
		    const auto __x3 = __vector_bitcast<_UInt>(__v3) << 24;
		    return __intrin_bitcast<_To>(
		      _mm_shuffle_epi8(__to_intrin(__x0 | __x1 | __x2 | __x3),
				       _mm_setr_epi8(0, 4, 8, 12, 1, 5, 9, 13,
						     2, 6, 10, 14, 3, 7, 11,
						     15)));
		  }
		else
		  {
		    auto __a
		      = _mm_unpacklo_epi8(__i0, __i2); // 08.. .... 19.. ....
		    auto __b
		      = _mm_unpackhi_epi8(__i0, __i2); // 2A.. .... 3B.. ....
		    auto __c
		      = _mm_unpacklo_epi8(__i1, __i3); // 4C.. .... 5D.. ....
		    auto __d
		      = _mm_unpackhi_epi8(__i1, __i3); // 6E.. .... 7F.. ....
		    auto __e
		      = _mm_unpacklo_epi8(__a, __c); // 048C .... .... ....
		    auto __f
		      = _mm_unpackhi_epi8(__a, __c); // 159D .... .... ....
		    auto __g
		      = _mm_unpacklo_epi8(__b, __d); // 26AE .... .... ....
		    auto __h
		      = _mm_unpackhi_epi8(__b, __d); // 37BF .... .... ....
		    return __intrin_bitcast<_To>(_mm_unpacklo_epi8(
		      _mm_unpacklo_epi8(__e, __g), // 0246 8ACE .... ....
		      _mm_unpacklo_epi8(__f, __h)  // 1357 9BDF .... ....
		      ));                          // 0123 4567 89AB CDEF
		  }
	      }
	    else if constexpr (__y_to_y)
	      {
		const auto __a = _mm256_shuffle_epi8(
		  __to_intrin((__vector_bitcast<_UShort>(_mm256_blend_epi16(
				 __i0, _mm256_slli_epi32(__i1, 16), 0xAA))
			       & 0xff)
			      | (__vector_bitcast<_UShort>(_mm256_blend_epi16(
				   __i2, _mm256_slli_epi32(__i3, 16), 0xAA))
				 << 8)),
		  _mm256_setr_epi8(0, 4, 8, 12, 2, 6, 10, 14, 1, 5, 9, 13, 3, 7,
				   11, 15, 0, 4, 8, 12, 2, 6, 10, 14, 1, 5, 9,
				   13, 3, 7, 11, 15));
		return __intrin_bitcast<_To>(_mm256_permutevar8x32_epi32(
		  __a, _mm256_setr_epi32(0, 4, 1, 5, 2, 6, 3, 7)));
	      }
	  }
	else if constexpr (__i64_to_f32) //{{{2
	  {
	    // this branch is only relevant with AVX and w/o AVX2 (i.e. no ymm
	    // integers)
	    if constexpr (__x_to_y)
	      {
		return __make_wrapper<float>(__v0[0], __v0[1], __v1[0], __v1[1],
					     __v2[0], __v2[1], __v3[0],
					     __v3[1]);

		const auto __a = _mm_unpacklo_epi32(__i0, __i1);   // acAC
		const auto __b = _mm_unpackhi_epi32(__i0, __i1);   // bdBD
		const auto __c = _mm_unpacklo_epi32(__i2, __i3);   // egEG
		const auto __d = _mm_unpackhi_epi32(__i2, __i3);   // fhFH
		const auto __lo32a = _mm_unpacklo_epi32(__a, __b); // abcd
		const auto __lo32b = _mm_unpacklo_epi32(__c, __d); // efgh
		const auto __hi32 = __vector_bitcast<
		  conditional_t<is_signed_v<_Tp>, int, _UInt>>(
		  __concat(_mm_unpackhi_epi32(__a, __b),
			   _mm_unpackhi_epi32(__c, __d))); // ABCD EFGH
		const auto __hi
		  = 0x100000000LL
		    * __convert_x86<__vector_type_t<float, 8>>(__hi32);
		const auto __mid
		  = 0x10000
		    * _mm256_cvtepi32_ps(__concat(_mm_srli_epi32(__lo32a, 16),
						  _mm_srli_epi32(__lo32b, 16)));
		const auto __lo = _mm256_cvtepi32_ps(
		  __concat(_mm_set1_epi32(0x0000ffffu) & __lo32a,
			   _mm_set1_epi32(0x0000ffffu) & __lo32b));
		return (__hi + __mid) + __lo;
	      }
	  }
	else if constexpr (__f64_to_ibw) //{{{2
	  {
	    return __convert_x86<_To>(
	      __convert_x86<__vector_type_t<int, _Np * 2>>(__v0, __v1),
	      __convert_x86<__vector_type_t<int, _Np * 2>>(__v2, __v3));
	  }
	else if constexpr (__f32_to_ibw) //{{{2
	  {
	    return __convert_x86<_To>(
	      __convert_x86<__vector_type_t<int, _Np>>(__v0),
	      __convert_x86<__vector_type_t<int, _Np>>(__v1),
	      __convert_x86<__vector_type_t<int, _Np>>(__v2),
	      __convert_x86<__vector_type_t<int, _Np>>(__v3));
	  } //}}}

	// fallback: {{{2
	if constexpr (sizeof(_To) >= 32)
	  // if _To is ymm or zmm, then _SimdWrapper<_Up, _M / 2> is xmm or ymm
	  return __concat(__convert_x86<__vector_type_t<_Up, _M / 2>>(__v0,
								      __v1),
			  __convert_x86<__vector_type_t<_Up, _M / 2>>(__v2,
								      __v3));
	else if constexpr (sizeof(_To) == 16)
	  {
	    const auto __lo = __to_intrin(__convert_x86<_To>(__v0, __v1));
	    const auto __hi = __to_intrin(__convert_x86<_To>(__v2, __v3));
	    if constexpr (sizeof(_Up) * _Np * 2 == 8)
	      {
		if constexpr (is_floating_point_v<_Up>)
		  return __auto_bitcast(_mm_unpacklo_pd(__lo, __hi));
		else
		  return __intrin_bitcast<_To>(_mm_unpacklo_epi64(__lo, __hi));
	      }
	    else if constexpr (sizeof(_Up) * _Np * 2 == 4)
	      {
		if constexpr (is_floating_point_v<_Up>)
		  return __auto_bitcast(_mm_unpacklo_ps(__lo, __hi));
		else
		  return __intrin_bitcast<_To>(_mm_unpacklo_epi32(__lo, __hi));
	      }
	    else
	      __assert_unreachable<_Tp>();
	  }
	else
	  return __vector_convert<_To>(__v0, __v1, __v2, __v3,
				       make_index_sequence<_Np>());
	//}}}2
      }
  }

//}}}
// 8-arg __convert_x86 {{{1
template <typename _To, typename _V, typename _Traits>
  _GLIBCXX_SIMD_INTRINSIC _To
  __convert_x86(_V __v0, _V __v1, _V __v2, _V __v3, _V __v4, _V __v5, _V __v6,
		_V __v7)
  {
    static_assert(__is_vector_type_v<_V>);
    using _Tp = typename _Traits::value_type;
    constexpr size_t _Np = _Traits::_S_full_size;
    [[maybe_unused]] const auto __i0 = __to_intrin(__v0);
    [[maybe_unused]] const auto __i1 = __to_intrin(__v1);
    [[maybe_unused]] const auto __i2 = __to_intrin(__v2);
    [[maybe_unused]] const auto __i3 = __to_intrin(__v3);
    [[maybe_unused]] const auto __i4 = __to_intrin(__v4);
    [[maybe_unused]] const auto __i5 = __to_intrin(__v5);
    [[maybe_unused]] const auto __i6 = __to_intrin(__v6);
    [[maybe_unused]] const auto __i7 = __to_intrin(__v7);
    using _Up = typename _VectorTraits<_To>::value_type;
    constexpr size_t _M = _VectorTraits<_To>::_S_full_size;

    static_assert(8 * _Np <= _M,
		  "__v4-__v7 would be discarded; use the four/two/one-argument "
		  "__convert_x86 overload instead");

    // [xyz]_to_[xyz] {{{2
    [[maybe_unused]] constexpr bool __x_to_x
      = sizeof(__v0) <= 16 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __x_to_y
      = sizeof(__v0) <= 16 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __x_to_z
      = sizeof(__v0) <= 16 && sizeof(_To) == 64;
    [[maybe_unused]] constexpr bool __y_to_x
      = sizeof(__v0) == 32 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __y_to_y
      = sizeof(__v0) == 32 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __y_to_z
      = sizeof(__v0) == 32 && sizeof(_To) == 64;
    [[maybe_unused]] constexpr bool __z_to_x
      = sizeof(__v0) == 64 && sizeof(_To) <= 16;
    [[maybe_unused]] constexpr bool __z_to_y
      = sizeof(__v0) == 64 && sizeof(_To) == 32;
    [[maybe_unused]] constexpr bool __z_to_z
      = sizeof(__v0) == 64 && sizeof(_To) == 64;

    // [if]X_to_i8 {{{2
    [[maybe_unused]] constexpr bool __i_to_i
      = is_integral_v<_Up> && is_integral_v<_Tp>;
    [[maybe_unused]] constexpr bool __i64_to_i8
      = __i_to_i && sizeof(_Tp) == 8 && sizeof(_Up) == 1;
    [[maybe_unused]] constexpr bool __f64_to_i8
      = is_integral_v<_Up> && sizeof(_Up) == 1
	&& is_floating_point_v<_Tp> && sizeof(_Tp) == 8;

    if constexpr (__i_to_i) // assert ISA {{{2
      {
	static_assert(__x_to_x || __have_avx2,
		      "integral conversions with ymm registers require AVX2");
	static_assert(__have_avx512bw
			|| ((sizeof(_Tp) >= 4 || sizeof(__v0) < 64)
			    && (sizeof(_Up) >= 4 || sizeof(_To) < 64)),
		      "8/16-bit integers in zmm registers require AVX512BW");
	static_assert((sizeof(__v0) < 64 && sizeof(_To) < 64) || __have_avx512f,
		      "integral conversions with ymm registers require AVX2");
      }
    // concat => use 4-arg __convert_x86 {{{2
    if constexpr (sizeof(__v0) < 16 || (sizeof(__v0) == 16 && __have_avx2)
		  || (sizeof(__v0) == 16 && __have_avx
		      && is_floating_point_v<_Tp>)
		  || (sizeof(__v0) == 32 && __have_avx512f))
      {
	// The ISA can handle wider input registers, so concat and use two-arg
	// implementation. This reduces code duplication considerably.
	return __convert_x86<_To>(__concat(__v0, __v1), __concat(__v2, __v3),
				  __concat(__v4, __v5), __concat(__v6, __v7));
      }
    else //{{{2
      {
	// conversion using bit reinterpretation (or no conversion at all)
	// should all go through the concat branch above:
	static_assert(
	  !(is_floating_point_v<
	      _Tp> == is_floating_point_v<_Up> && sizeof(_Tp) == sizeof(_Up)));
	static_assert(!(8 * _Np < _M && sizeof(_To) > 16),
		      "zero extension should be impossible");
	if constexpr (__i64_to_i8) //{{{2
	  {
	    if constexpr (__x_to_x && __have_ssse3)
	      {
		// unsure whether this is better than the variant below
		return __intrin_bitcast<_To>(_mm_shuffle_epi8(
		  __to_intrin(
		    (((__v0 & 0xff) | ((__v1 & 0xff) << 8))
		     | (((__v2 & 0xff) << 16) | ((__v3 & 0xff) << 24)))
		    | ((((__v4 & 0xff) << 32) | ((__v5 & 0xff) << 40))
		       | (((__v6 & 0xff) << 48) | (__v7 << 56)))),
		  _mm_setr_epi8(0, 8, 1, 9, 2, 10, 3, 11, 4, 12, 5, 13, 6, 14,
				7, 15)));
	      }
	    else if constexpr (__x_to_x)
	      {
		const auto __a = _mm_unpacklo_epi8(__i0, __i1); // ac
		const auto __b = _mm_unpackhi_epi8(__i0, __i1); // bd
		const auto __c = _mm_unpacklo_epi8(__i2, __i3); // eg
		const auto __d = _mm_unpackhi_epi8(__i2, __i3); // fh
		const auto __e = _mm_unpacklo_epi8(__i4, __i5); // ik
		const auto __f = _mm_unpackhi_epi8(__i4, __i5); // jl
		const auto __g = _mm_unpacklo_epi8(__i6, __i7); // mo
		const auto __h = _mm_unpackhi_epi8(__i6, __i7); // np
		return __intrin_bitcast<_To>(_mm_unpacklo_epi64(
		  _mm_unpacklo_epi32(_mm_unpacklo_epi8(__a, __b),  // abcd
				     _mm_unpacklo_epi8(__c, __d)), // efgh
		  _mm_unpacklo_epi32(_mm_unpacklo_epi8(__e, __f),  // ijkl
				     _mm_unpacklo_epi8(__g, __h))  // mnop
		  ));
	      }
	    else if constexpr (__y_to_y)
	      {
		auto __a = // 048C GKOS 159D HLPT 26AE IMQU 37BF JNRV
		  __to_intrin(
		    (((__v0 & 0xff) | ((__v1 & 0xff) << 8))
		     | (((__v2 & 0xff) << 16) | ((__v3 & 0xff) << 24)))
		    | ((((__v4 & 0xff) << 32) | ((__v5 & 0xff) << 40))
		       | (((__v6 & 0xff) << 48) | ((__v7 << 56)))));
		/*
		auto __b = _mm256_unpackhi_epi64(__a, __a);  // 159D HLPT 159D
		HLPT 37BF JNRV 37BF JNRV auto __c = _mm256_unpacklo_epi8(__a,
		__b);  // 0145 89CD GHKL OPST 2367 ABEF IJMN QRUV auto __d =
		__xzyw(__c); // 0145 89CD 2367 ABEF GHKL OPST IJMN QRUV return
		_mm256_shuffle_epi8(
		    __d, _mm256_setr_epi8(0, 1, 8, 9, 2, 3, 10, 11, 4, 5, 12,
		13, 6, 7, 14, 15, 0, 1, 8, 9, 2, 3, 10, 11, 4, 5, 12, 13, 6, 7,
		14, 15));
					*/
		auto __b = _mm256_shuffle_epi8( // 0145 89CD GHKL OPST 2367 ABEF
						// IJMN QRUV
		  __a, _mm256_setr_epi8(0, 8, 1, 9, 2, 10, 3, 11, 4, 12, 5, 13,
					6, 14, 7, 15, 0, 8, 1, 9, 2, 10, 3, 11,
					4, 12, 5, 13, 6, 14, 7, 15));
		auto __c
		  = __xzyw(__b); // 0145 89CD 2367 ABEF GHKL OPST IJMN QRUV
		return __intrin_bitcast<_To>(_mm256_shuffle_epi8(
		  __c, _mm256_setr_epi8(0, 1, 8, 9, 2, 3, 10, 11, 4, 5, 12, 13,
					6, 7, 14, 15, 0, 1, 8, 9, 2, 3, 10, 11,
					4, 5, 12, 13, 6, 7, 14, 15)));
	      }
	    else if constexpr (__z_to_z)
	      {
		return __concat(
		  __convert_x86<__vector_type_t<_Up, _M / 2>>(__v0, __v1, __v2,
							      __v3),
		  __convert_x86<__vector_type_t<_Up, _M / 2>>(__v4, __v5, __v6,
							      __v7));
	      }
	  }
	else if constexpr (__f64_to_i8) //{{{2
	  {
	    return __convert_x86<_To>(
	      __convert_x86<__vector_type_t<int, _Np * 2>>(__v0, __v1),
	      __convert_x86<__vector_type_t<int, _Np * 2>>(__v2, __v3),
	      __convert_x86<__vector_type_t<int, _Np * 2>>(__v4, __v5),
	      __convert_x86<__vector_type_t<int, _Np * 2>>(__v6, __v7));
	  }
	else // unreachable {{{2
	  __assert_unreachable<_Tp>();
	//}}}

	// fallback: {{{2
	if constexpr (sizeof(_To) >= 32)
	  // if _To is ymm or zmm, then _SimdWrapper<_Up, _M / 2> is xmm or ymm
	  return __concat(
	    __convert_x86<__vector_type_t<_Up, _M / 2>>(__v0, __v1, __v2, __v3),
	    __convert_x86<__vector_type_t<_Up, _M / 2>>(__v4, __v5, __v6,
							__v7));
	else if constexpr (sizeof(_To) == 16)
	  {
	    const auto __lo
	      = __to_intrin(__convert_x86<_To>(__v0, __v1, __v2, __v3));
	    const auto __hi
	      = __to_intrin(__convert_x86<_To>(__v4, __v5, __v6, __v7));
	    static_assert(sizeof(_Up) == 1 && _Np == 2);
	    return __intrin_bitcast<_To>(_mm_unpacklo_epi64(__lo, __hi));
	  }
	else
	  {
	    __assert_unreachable<_Tp>();
	    // return __vector_convert<_To>(__v0, __v1, __v2, __v3, __v4, __v5,
	    // __v6, __v7,
	    //                             make_index_sequence<_Np>());
	  } //}}}2
      }
  }

//}}}
// 16-arg __convert_x86 {{{1
template <typename _To, typename _V, typename _Traits>
  _GLIBCXX_SIMD_INTRINSIC _To
  __convert_x86(_V __v0, _V __v1, _V __v2, _V __v3, _V __v4, _V __v5, _V __v6,
		_V __v7, _V __v8, _V __v9, _V __v10, _V __v11, _V __v12,
		_V __v13, _V __v14, _V __v15)
  {
    // concat => use 8-arg __convert_x86
    return __convert_x86<_To>(__concat(__v0, __v1), __concat(__v2, __v3),
			      __concat(__v4, __v5), __concat(__v6, __v7),
			      __concat(__v8, __v9), __concat(__v10, __v11),
			      __concat(__v12, __v13), __concat(__v14, __v15));
  }

//}}}

#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_X86_CONVERSIONS_H

// vim: foldmethod=marker
