// Internal macros for the simd implementation -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_DETAIL_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_DETAIL_H_

#if __cplusplus >= 201703L

#include <cstddef>
#include <cstdint>

/// @cond undocumented

#define _GLIBCXX_SIMD_BEGIN_NAMESPACE                                          \
  namespace std _GLIBCXX_VISIBILITY(default)                                   \
  {                                                                            \
    _GLIBCXX_BEGIN_NAMESPACE_VERSION                                           \
      namespace experimental {                                                 \
      inline namespace parallelism_v2 {
#define _GLIBCXX_SIMD_END_NAMESPACE                                            \
  }                                                                            \
  }                                                                            \
  _GLIBCXX_END_NAMESPACE_VERSION                                               \
  }

// ISA extension detection. The following defines all the _GLIBCXX_SIMD_HAVE_XXX
// macros ARM{{{
#if defined __ARM_NEON
#define _GLIBCXX_SIMD_HAVE_NEON 1
#else
#define _GLIBCXX_SIMD_HAVE_NEON 0
#endif
#if defined __ARM_NEON && (__ARM_ARCH >= 8 || defined __aarch64__)
#define _GLIBCXX_SIMD_HAVE_NEON_A32 1
#else
#define _GLIBCXX_SIMD_HAVE_NEON_A32 0
#endif
#if defined __ARM_NEON && defined __aarch64__
#define _GLIBCXX_SIMD_HAVE_NEON_A64 1
#else
#define _GLIBCXX_SIMD_HAVE_NEON_A64 0
#endif
//}}}
// x86{{{
#ifdef __MMX__
#define _GLIBCXX_SIMD_HAVE_MMX 1
#else
#define _GLIBCXX_SIMD_HAVE_MMX 0
#endif
#if defined __SSE__ || defined __x86_64__
#define _GLIBCXX_SIMD_HAVE_SSE 1
#else
#define _GLIBCXX_SIMD_HAVE_SSE 0
#endif
#if defined __SSE2__ || defined __x86_64__
#define _GLIBCXX_SIMD_HAVE_SSE2 1
#else
#define _GLIBCXX_SIMD_HAVE_SSE2 0
#endif
#ifdef __SSE3__
#define _GLIBCXX_SIMD_HAVE_SSE3 1
#else
#define _GLIBCXX_SIMD_HAVE_SSE3 0
#endif
#ifdef __SSSE3__
#define _GLIBCXX_SIMD_HAVE_SSSE3 1
#else
#define _GLIBCXX_SIMD_HAVE_SSSE3 0
#endif
#ifdef __SSE4_1__
#define _GLIBCXX_SIMD_HAVE_SSE4_1 1
#else
#define _GLIBCXX_SIMD_HAVE_SSE4_1 0
#endif
#ifdef __SSE4_2__
#define _GLIBCXX_SIMD_HAVE_SSE4_2 1
#else
#define _GLIBCXX_SIMD_HAVE_SSE4_2 0
#endif
#ifdef __XOP__
#define _GLIBCXX_SIMD_HAVE_XOP 1
#else
#define _GLIBCXX_SIMD_HAVE_XOP 0
#endif
#ifdef __AVX__
#define _GLIBCXX_SIMD_HAVE_AVX 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX 0
#endif
#ifdef __AVX2__
#define _GLIBCXX_SIMD_HAVE_AVX2 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX2 0
#endif
#ifdef __BMI__
#define _GLIBCXX_SIMD_HAVE_BMI1 1
#else
#define _GLIBCXX_SIMD_HAVE_BMI1 0
#endif
#ifdef __BMI2__
#define _GLIBCXX_SIMD_HAVE_BMI2 1
#else
#define _GLIBCXX_SIMD_HAVE_BMI2 0
#endif
#ifdef __LZCNT__
#define _GLIBCXX_SIMD_HAVE_LZCNT 1
#else
#define _GLIBCXX_SIMD_HAVE_LZCNT 0
#endif
#ifdef __SSE4A__
#define _GLIBCXX_SIMD_HAVE_SSE4A 1
#else
#define _GLIBCXX_SIMD_HAVE_SSE4A 0
#endif
#ifdef __FMA__
#define _GLIBCXX_SIMD_HAVE_FMA 1
#else
#define _GLIBCXX_SIMD_HAVE_FMA 0
#endif
#ifdef __FMA4__
#define _GLIBCXX_SIMD_HAVE_FMA4 1
#else
#define _GLIBCXX_SIMD_HAVE_FMA4 0
#endif
#ifdef __F16C__
#define _GLIBCXX_SIMD_HAVE_F16C 1
#else
#define _GLIBCXX_SIMD_HAVE_F16C 0
#endif
#ifdef __POPCNT__
#define _GLIBCXX_SIMD_HAVE_POPCNT 1
#else
#define _GLIBCXX_SIMD_HAVE_POPCNT 0
#endif
#ifdef __AVX512F__
#define _GLIBCXX_SIMD_HAVE_AVX512F 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512F 0
#endif
#ifdef __AVX512DQ__
#define _GLIBCXX_SIMD_HAVE_AVX512DQ 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512DQ 0
#endif
#ifdef __AVX512VL__
#define _GLIBCXX_SIMD_HAVE_AVX512VL 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512VL 0
#endif
#ifdef __AVX512BW__
#define _GLIBCXX_SIMD_HAVE_AVX512BW 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512BW 0
#endif
#ifdef __AVX512BITALG__
#define _GLIBCXX_SIMD_HAVE_AVX512BITALG 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512BITALG 0
#endif
#ifdef __AVX512VBMI2__
#define _GLIBCXX_SIMD_HAVE_AVX512VBMI2 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512VBMI2 0
#endif
#ifdef __AVX512VBMI__
#define _GLIBCXX_SIMD_HAVE_AVX512VBMI 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512VBMI 0
#endif
#ifdef __AVX512IFMA__
#define _GLIBCXX_SIMD_HAVE_AVX512IFMA 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512IFMA 0
#endif
#ifdef __AVX512CD__
#define _GLIBCXX_SIMD_HAVE_AVX512CD 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512CD 0
#endif
#ifdef __AVX512VNNI__
#define _GLIBCXX_SIMD_HAVE_AVX512VNNI 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512VNNI 0
#endif
#ifdef __AVX512VPOPCNTDQ__
#define _GLIBCXX_SIMD_HAVE_AVX512VPOPCNTDQ 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512VPOPCNTDQ 0
#endif
#ifdef __AVX512VP2INTERSECT__
#define _GLIBCXX_SIMD_HAVE_AVX512VP2INTERSECT 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512VP2INTERSECT 0
#endif

#if _GLIBCXX_SIMD_HAVE_SSE
#define _GLIBCXX_SIMD_HAVE_SSE_ABI 1
#else
#define _GLIBCXX_SIMD_HAVE_SSE_ABI 0
#endif
#if _GLIBCXX_SIMD_HAVE_SSE2
#define _GLIBCXX_SIMD_HAVE_FULL_SSE_ABI 1
#else
#define _GLIBCXX_SIMD_HAVE_FULL_SSE_ABI 0
#endif

#if _GLIBCXX_SIMD_HAVE_AVX
#define _GLIBCXX_SIMD_HAVE_AVX_ABI 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX_ABI 0
#endif
#if _GLIBCXX_SIMD_HAVE_AVX2
#define _GLIBCXX_SIMD_HAVE_FULL_AVX_ABI 1
#else
#define _GLIBCXX_SIMD_HAVE_FULL_AVX_ABI 0
#endif

#if _GLIBCXX_SIMD_HAVE_AVX512F
#define _GLIBCXX_SIMD_HAVE_AVX512_ABI 1
#else
#define _GLIBCXX_SIMD_HAVE_AVX512_ABI 0
#endif
#if _GLIBCXX_SIMD_HAVE_AVX512BW
#define _GLIBCXX_SIMD_HAVE_FULL_AVX512_ABI 1
#else
#define _GLIBCXX_SIMD_HAVE_FULL_AVX512_ABI 0
#endif

#if defined __x86_64__ && !_GLIBCXX_SIMD_HAVE_SSE2
#error "Use of SSE2 is required on AMD64"
#endif
//}}}

#ifdef __clang__
#define _GLIBCXX_SIMD_NORMAL_MATH
#else
#define _GLIBCXX_SIMD_NORMAL_MATH                                              \
  [[__gnu__::__optimize__("finite-math-only,no-signed-zeros")]]
#endif
#define _GLIBCXX_SIMD_NEVER_INLINE [[__gnu__::__noinline__]]
#define _GLIBCXX_SIMD_INTRINSIC                                                \
  [[__gnu__::__always_inline__, __gnu__::__artificial__]] inline
#define _GLIBCXX_SIMD_ALWAYS_INLINE [[__gnu__::__always_inline__]] inline
#define _GLIBCXX_SIMD_IS_UNLIKELY(__x) __builtin_expect(__x, 0)
#define _GLIBCXX_SIMD_IS_LIKELY(__x) __builtin_expect(__x, 1)

#if defined __STRICT_ANSI__ && __STRICT_ANSI__
#define _GLIBCXX_SIMD_CONSTEXPR
#define _GLIBCXX_SIMD_USE_CONSTEXPR_API const
#else
#define _GLIBCXX_SIMD_CONSTEXPR constexpr
#define _GLIBCXX_SIMD_USE_CONSTEXPR_API constexpr
#endif

#if defined __clang__
#define _GLIBCXX_SIMD_USE_CONSTEXPR const
#else
#define _GLIBCXX_SIMD_USE_CONSTEXPR constexpr
#endif

#define _GLIBCXX_SIMD_LIST_BINARY(__macro) __macro(|) __macro(&) __macro(^)
#define _GLIBCXX_SIMD_LIST_SHIFTS(__macro) __macro(<<) __macro(>>)
#define _GLIBCXX_SIMD_LIST_ARITHMETICS(__macro)                                \
  __macro(+) __macro(-) __macro(*) __macro(/) __macro(%)

#define _GLIBCXX_SIMD_ALL_BINARY(__macro)                                      \
  _GLIBCXX_SIMD_LIST_BINARY(__macro) static_assert(true)
#define _GLIBCXX_SIMD_ALL_SHIFTS(__macro)                                      \
  _GLIBCXX_SIMD_LIST_SHIFTS(__macro) static_assert(true)
#define _GLIBCXX_SIMD_ALL_ARITHMETICS(__macro)                                 \
  _GLIBCXX_SIMD_LIST_ARITHMETICS(__macro) static_assert(true)

#ifdef _GLIBCXX_SIMD_NO_ALWAYS_INLINE
#undef _GLIBCXX_SIMD_ALWAYS_INLINE
#define _GLIBCXX_SIMD_ALWAYS_INLINE inline
#undef _GLIBCXX_SIMD_INTRINSIC
#define _GLIBCXX_SIMD_INTRINSIC inline
#endif

#if _GLIBCXX_SIMD_HAVE_SSE || _GLIBCXX_SIMD_HAVE_MMX
#define _GLIBCXX_SIMD_X86INTRIN 1
#else
#define _GLIBCXX_SIMD_X86INTRIN 0
#endif

// workaround macros {{{
// use aliasing loads to help GCC understand the data accesses better
// This also seems to hide a miscompilation on swap(x[i], x[i + 1]) with
// fixed_size_simd<float, 16> x.
#define _GLIBCXX_SIMD_USE_ALIASING_LOADS 1

// vector conversions on x86 not optimized:
#if _GLIBCXX_SIMD_X86INTRIN
#define _GLIBCXX_SIMD_WORKAROUND_PR85048 1
#endif

// integer division not optimized
#define _GLIBCXX_SIMD_WORKAROUND_PR90993 1

// very bad codegen for extraction and concatenation of 128/256 "subregisters"
// with sizeof(element type) < 8: https://godbolt.org/g/mqUsgM
#if _GLIBCXX_SIMD_X86INTRIN
#define _GLIBCXX_SIMD_WORKAROUND_XXX_1 1
#endif

// bad codegen for 8 Byte memcpy to __vector_type_t<char, 16>
#define _GLIBCXX_SIMD_WORKAROUND_PR90424 1

// bad codegen for zero-extend using simple concat(__x, 0)
#if _GLIBCXX_SIMD_X86INTRIN
#define _GLIBCXX_SIMD_WORKAROUND_XXX_3 1
#endif

// https://github.com/cplusplus/parallelism-ts/issues/65 (incorrect return type
// of static_simd_cast)
#define _GLIBCXX_SIMD_FIX_P2TS_ISSUE65 1

// https://github.com/cplusplus/parallelism-ts/issues/66 (incorrect SFINAE
// constraint on (static)_simd_cast)
#define _GLIBCXX_SIMD_FIX_P2TS_ISSUE66 1
// }}}

/// @endcond

#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_DETAIL_H_

// vim: foldmethod=marker
