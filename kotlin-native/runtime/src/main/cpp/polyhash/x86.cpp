/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "polyhash/common.h"
#include "polyhash/x86.h"

#if defined(__x86_64__) or defined(__i386__)

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

struct SSETraits {
    using VecType = __m128i;
    using Vec128Type = __m128i;
    using U16VecType = __m128i;

    __SSE41__ static VecType initVec() { return _mm_setzero_si128(); }
    __SSE41__ static Vec128Type initVec128() { return _mm_setzero_si128(); }
    __SSE41__ static int vec128toInt(Vec128Type x) { return _mm_cvtsi128_si32(x); }
    __SSE41__ static VecType u16Load(U16VecType x) { return _mm_cvtepu16_epi32(x); }
    __SSE41__ static Vec128Type vec128Mul(Vec128Type x, Vec128Type y) { return _mm_mullo_epi32(x, y); }
    __SSE41__ static Vec128Type vec128Add(Vec128Type x, Vec128Type y) { return _mm_add_epi32(x, y); }
    __SSE41__ static VecType vecMul(VecType x, VecType y) { return _mm_mullo_epi32(x, y); }
    __SSE41__ static VecType vecAdd(VecType x, VecType y) { return _mm_add_epi32(x, y); }
    __SSE41__ static Vec128Type squash2(VecType x, VecType y) {
        return squash1(_mm_hadd_epi32(x, y)); // [x0 + x1, x2 + x3, y0 + y1, y2 + y3]
    }

    __SSE41__ static Vec128Type squash1(VecType z) {
        VecType sum = _mm_hadd_epi32(z, z); // [z0 + z1, z2 + z3, z0 + z1, z2 + z3]
        return _mm_hadd_epi32(sum, sum);    // [z0..3, same, same, same]
    }

    __SSE41__ static int polyHashUnalignedUnrollUpTo8(int n, uint16_t const* str) {
        Vec128Type res = initVec128();

        polyHashUnroll2<SSETraits>(n, str, res, &b8[0], &p64[56]);
        polyHashTail<SSETraits>(n, str, res, &b4[0], &p64[60]);

        return vec128toInt(res);
    }

    __SSE41__ static int polyHashUnalignedUnrollUpTo16(int n, uint16_t const* str) {
        Vec128Type res = initVec128();

        polyHashUnroll4<SSETraits>(n, str, res, &b16[0], &p64[48]);
        polyHashUnroll2<SSETraits>(n, str, res, &b8[0], &p64[56]);
        polyHashTail<SSETraits>(n, str, res, &b4[0], &p64[60]);

        return vec128toInt(res);
    }
};

struct AVX2Traits {
    using VecType = __m256i;
    using Vec128Type = __m128i;
    using U16VecType = __m128i;

    __AVX2__ static VecType initVec() { return _mm256_setzero_si256(); }
    __AVX2__ static Vec128Type initVec128() { return _mm_setzero_si128(); }
    __AVX2__ static int vec128toInt(Vec128Type x) { return _mm_cvtsi128_si32(x); }
    __AVX2__ static VecType u16Load(U16VecType x) { return _mm256_cvtepu16_epi32(x); }
    __AVX2__ static Vec128Type vec128Mul(Vec128Type x, Vec128Type y) { return _mm_mullo_epi32(x, y); }
    __AVX2__ static Vec128Type vec128Add(Vec128Type x, Vec128Type y) { return _mm_add_epi32(x, y); }
    __AVX2__ static VecType vecMul(VecType x, VecType y) { return _mm256_mullo_epi32(x, y); }
    __AVX2__ static VecType vecAdd(VecType x, VecType y) { return _mm256_add_epi32(x, y); }
    __AVX2__ static Vec128Type squash2(VecType x, VecType y) {
        return squash1(_mm256_hadd_epi32(x, y)); // [x0 + x1, x2 + x3, y0 + y1, y2 + y3, x4 + x5, x6 + x7, y4 + y5, y6 + y7]
    }

    __AVX2__ static Vec128Type squash1(VecType z) {
        VecType sum = _mm256_hadd_epi32(z, z);            // [z0 + z1, z2 + z3, z0 + z1, z2 + z3, z4 + z5, z6 + z7, z4 + z5, z6 + z7]
        sum = _mm256_hadd_epi32(sum, sum);                // [z0..3, z0..3, z0..3, z0..3, z4..7, z4..7, z4..7, z4..7]
        Vec128Type lo = _mm256_extracti128_si256(sum, 0); // [z0..3, same, same, same]
        Vec128Type hi = _mm256_extracti128_si256(sum, 1); // [z4..7, same, same, same]
        return _mm_add_epi32(lo, hi);                     // [z0..7, same, same, same]
    }

    __AVX2__ static int polyHashUnalignedUnrollUpTo16(int n, uint16_t const* str) {
        Vec128Type res = initVec128();

        polyHashUnroll2<AVX2Traits>(n, str, res, &b16[0], &p64[48]);
        polyHashTail<AVX2Traits>(n, str, res, &b8[0], &p64[56]);
        polyHashTail<SSETraits>(n, str, res, &b4[0], &p64[60]);

        return vec128toInt(res);
    }

    __AVX2__ static int polyHashUnalignedUnrollUpTo32(int n, uint16_t const* str) {
        Vec128Type res = initVec128();

        polyHashUnroll4<AVX2Traits>(n, str, res, &b32[0], &p64[32]);
        polyHashUnroll2<AVX2Traits>(n, str, res, &b16[0], &p64[48]);
        polyHashTail<AVX2Traits>(n, str, res, &b8[0], &p64[56]);
        polyHashTail<SSETraits>(n, str, res, &b4[0], &p64[60]);

        return vec128toInt(res);
    }

    __AVX2__ static int polyHashUnalignedUnrollUpTo64(int n, uint16_t const* str) {
        Vec128Type res = initVec128();

        polyHashUnroll8<AVX2Traits>(n, str, res, &b64[0], &p64[0]);
        polyHashUnroll4<AVX2Traits>(n, str, res, &b32[0], &p64[32]);
        polyHashUnroll2<AVX2Traits>(n, str, res, &b16[0], &p64[48]);
        polyHashTail<AVX2Traits>(n, str, res, &b8[0], &p64[56]);
        polyHashTail<SSETraits>(n, str, res, &b4[0], &p64[60]);

        return vec128toInt(res);
    }
};

#if defined(__x86_64__)
    const bool x64 = true;
#else
    const bool x64 = false;
#endif
    bool initialized = false;
    bool sseSupported = false;
    bool avx2Supported = false;

}

int polyHash_x86(int length, uint16_t const* str) {
    if (!initialized) {
        initialized = true;
        sseSupported = __builtin_cpu_supports("sse4.1");
        avx2Supported = __builtin_cpu_supports("avx2");
    }
    if (length < 16 || (!sseSupported && !avx2Supported)) {
        // Either vectorization is not supported or the string is too short to gain from it.
        return polyHash_naive(length, str);
    }
    uint32_t res;
    if (length < 32)
        res = SSETraits::polyHashUnalignedUnrollUpTo8(length / 4, str);
    else if (!avx2Supported)
        res = SSETraits::polyHashUnalignedUnrollUpTo16(length / 4, str);
    else if (length < 128)
        res = AVX2Traits::polyHashUnalignedUnrollUpTo16(length / 4, str);
    else if (!x64 || length < 576)
        res = AVX2Traits::polyHashUnalignedUnrollUpTo32(length / 4, str);
    else // Such big unrolling requires 64-bit mode (in 32-bit mode there are only 8 vector registers)
        res = AVX2Traits::polyHashUnalignedUnrollUpTo64(length / 4, str);

    // Handle the tail naively.
    for (int i = length & 0xFFFFFFFC; i < length; ++i)
        res = res * 31 + str[i];
    return res;
}

#endif
