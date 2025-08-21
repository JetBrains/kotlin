/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "polyhash/common.h"
#include "polyhash/PolyHash.h"

#define __SSE41__ __attribute__((target("sse4.1")))
#define __AVX2__ __attribute__((target("avx2")))

#include <immintrin.h>

namespace {

alignas(32) constexpr auto p64 = DecreasingPowers<64>(31);   // [base^63, base^62, .., base^2, base, 1]
alignas(32) constexpr auto b64 = RepeatingPowers<8>(31, 64); // [base^64, base^64, .., base^64] (8)
alignas(32) constexpr auto b32 = RepeatingPowers<8>(31, 32); // [base^32, base^32, .., base^32] (8)
alignas(32) constexpr auto b16 = RepeatingPowers<8>(31, 16); // [base^16, base^16, .., base^16] (8)
alignas(32) constexpr auto b8  = RepeatingPowers<8>(31, 8);  // [base^8,  base^8,  .., base^8 ] (8)
alignas(32) constexpr auto b4  = RepeatingPowers<8>(31, 4);  // [base^4,  base^4,  .., base^4 ] (8)

#pragma clang attribute push(__SSE41__, apply_to = function)

struct SSETraits {
    using VecType = __m128i;
    using Vec128Type = __m128i;

    static VecType initVec() { return _mm_setzero_si128(); }
    static Vec128Type initVec128() { return _mm_setzero_si128(); }
    static int vec128toInt(Vec128Type x) { return _mm_cvtsi128_si32(x); }
    static VecType load(uint16_t const* x) { return _mm_cvtepu16_epi32(_mm_loadl_epi64(reinterpret_cast<__m128i const*>(x))); }
    static VecType load(uint8_t const* x) { return _mm_cvtepu8_epi32(_mm_set_epi32(0, 0, 0, *reinterpret_cast<uint32_t const*>(x))); }
    static Vec128Type vec128Mul(Vec128Type x, Vec128Type y) { return _mm_mullo_epi32(x, y); }
    static Vec128Type vec128Add(Vec128Type x, Vec128Type y) { return _mm_add_epi32(x, y); }
    static VecType vecMul(VecType x, VecType y) { return _mm_mullo_epi32(x, y); }
    static VecType vecAdd(VecType x, VecType y) { return _mm_add_epi32(x, y); }
    static Vec128Type squash2(VecType x, VecType y) {
        return squash1(_mm_hadd_epi32(x, y)); // [x0 + x1, x2 + x3, y0 + y1, y2 + y3]
    }

    static Vec128Type squash1(VecType z) {
        VecType sum = _mm_hadd_epi32(z, z); // [z0 + z1, z2 + z3, z0 + z1, z2 + z3]
        return _mm_hadd_epi32(sum, sum);    // [z0..3, same, same, same]
    }

#include "polyhash/attributeSensitiveFunctions.inc"

    template <typename UnitType>
    static int polyHashUnaligned(int n, UnitType const* str) {
        Vec128Type res = initVec128();
        if (n >= 8) {
            polyHashUnroll4<SSETraits>(n, str, res, &b16[0], &p64[48]);
        }
        polyHashUnroll2<SSETraits>(n, str, res, &b8[0], &p64[56]);
        polyHashTail<SSETraits>(n, str, res, &b4[0], &p64[60]);
        return vec128toInt(res);
    }
};

#pragma clang attribute pop

#pragma clang attribute push(__AVX2__, apply_to = function)

struct AVX2Traits {
    using VecType = __m256i;
    using Vec128Type = __m128i;

    static VecType initVec() { return _mm256_setzero_si256(); }
    static Vec128Type initVec128() { return _mm_setzero_si128(); }
    static int vec128toInt(Vec128Type x) { return _mm_cvtsi128_si32(x); }
    static VecType load(uint16_t const* x) { return _mm256_cvtepu16_epi32(*reinterpret_cast<__m128i const*>(x)); }
    static VecType load(uint8_t const* x) { return _mm256_cvtepu8_epi32(_mm_loadl_epi64(reinterpret_cast<__m128i const*>(x))); }
    static Vec128Type vec128Mul(Vec128Type x, Vec128Type y) { return _mm_mullo_epi32(x, y); }
    static Vec128Type vec128Add(Vec128Type x, Vec128Type y) { return _mm_add_epi32(x, y); }
    static VecType vecMul(VecType x, VecType y) { return _mm256_mullo_epi32(x, y); }
    static VecType vecAdd(VecType x, VecType y) { return _mm256_add_epi32(x, y); }
    static Vec128Type squash2(VecType x, VecType y) {
        return squash1(_mm256_hadd_epi32(x, y)); // [x0 + x1, x2 + x3, y0 + y1, y2 + y3, x4 + x5, x6 + x7, y4 + y5, y6 + y7]
    }

    static Vec128Type squash1(VecType z) {
        VecType sum = _mm256_hadd_epi32(z, z);            // [z0 + z1, z2 + z3, z0 + z1, z2 + z3, z4 + z5, z6 + z7, z4 + z5, z6 + z7]
        sum = _mm256_hadd_epi32(sum, sum);                // [z0..3, z0..3, z0..3, z0..3, z4..7, z4..7, z4..7, z4..7]
        Vec128Type lo = _mm256_extracti128_si256(sum, 0); // [z0..3, same, same, same]
        Vec128Type hi = _mm256_extracti128_si256(sum, 1); // [z4..7, same, same, same]
        return _mm_add_epi32(lo, hi);                     // [z0..7, same, same, same]
    }

#include "polyhash/attributeSensitiveFunctions.inc"

    template <typename UnitType>
    static int polyHashUnaligned(int n, UnitType const* str) {
        Vec128Type res = initVec128();
        if (n >= 32) {
#if defined(__x86_64__)
            // Such big unrolling requires 64-bit mode (in 32-bit mode there are only 8 vector registers)
            if (n >= 144) {
                polyHashUnroll8<AVX2Traits>(n, str, res, &b64[0], &p64[0]);
            }
#endif
            polyHashUnroll4<AVX2Traits>(n, str, res, &b32[0], &p64[32]);
        }
        polyHashUnroll2<AVX2Traits>(n, str, res, &b16[0], &p64[48]);
        polyHashTail<AVX2Traits>(n, str, res, &b8[0], &p64[56]);
        polyHashTail<SSETraits>(n, str, res, &b4[0], &p64[60]);
        return vec128toInt(res);
    }
};

#pragma clang attribute pop

    bool initialized = false;
    bool sseSupported = false;
    bool avx2Supported = false;

}

template <typename UnitType>
int polyHash(int length, UnitType const* str) {
    if (!initialized) {
        initialized = true;
        sseSupported = __builtin_cpu_supports("sse4.1");
        avx2Supported = __builtin_cpu_supports("avx2");
    }
    if (length < 16 || (!sseSupported && !avx2Supported)) {
        return polyHash_naive(length, str);
    }
    uint32_t res = length < 32 || !avx2Supported
        ? SSETraits::polyHashUnaligned(length / 4, str)
        : AVX2Traits::polyHashUnaligned(length / 4, str);
    for (int i = length & 0xFFFFFFFC; i < length; ++i)
        res = res * 31 + str[i];
    return res;
}
