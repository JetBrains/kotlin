/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "polyhash/common.h"
#include "polyhash/PolyHash.h"

#include <arm_neon.h>

namespace {

alignas(32) constexpr auto p32 = DecreasingPowers<32>(31);   // [base^31, base^30, .., base^2, base, 1]
alignas(32) constexpr auto b32 = RepeatingPowers<8>(31, 32); // [base^32, base^32, .., base^32] (8)
alignas(32) constexpr auto b16 = RepeatingPowers<8>(31, 16); // [base^16, base^16, .., base^16] (8)
alignas(32) constexpr auto b8  = RepeatingPowers<8>(31, 8);  // [base^8,  base^8,  .., base^8 ] (8)
alignas(32) constexpr auto b4  = RepeatingPowers<8>(31, 4);  // [base^4,  base^4,  .., base^4 ] (8)

struct NeonTraits {
    using VecType = uint32x4_t;
    using Vec128Type = uint32x4_t;

    ALWAYS_INLINE static VecType initVec() { return vdupq_n_u32(0); }
    ALWAYS_INLINE static Vec128Type initVec128() { return vdupq_n_u32(0); }
    ALWAYS_INLINE static int vec128toInt(Vec128Type x) { return vgetq_lane_u32(x, 0); }
    ALWAYS_INLINE static VecType load(uint16_t const* x) { return vmovl_u16(vld1_u16(x)); }
    ALWAYS_INLINE static VecType load(uint8_t const* x) { return vmovl_u16(vget_low_u16(vmovl_u8(vcreate_u8(*reinterpret_cast<uint32_t const*>(x))))); }
    ALWAYS_INLINE static Vec128Type vec128Mul(Vec128Type x, Vec128Type y) { return vmulq_u32(x, y); }
    ALWAYS_INLINE static Vec128Type vec128Add(Vec128Type x, Vec128Type y) { return vaddq_u32(x, y); }
    ALWAYS_INLINE static VecType vecMul(VecType x, VecType y) { return vmulq_u32(x, y); }
    ALWAYS_INLINE static VecType vecAdd(VecType x, VecType y) { return vaddq_u32(x, y); }
    ALWAYS_INLINE static Vec128Type squash2(VecType x, VecType y) {
        return squash1(vaddq_u32(x, y)); // [x0 + y0, x1 + y1, x2 + y2, x3 + y3]
    }

    ALWAYS_INLINE static uint32x4_t squash1(uint32x4_t z) {
    #ifdef __aarch64__
        return vdupq_n_u32(vaddvq_u32(z)); // [z0..3, same, same, same]
    #else
        uint32x2_t lo = vget_low_u32(z);   // [z0, z1]
        uint32x2_t hi = vget_high_u32(z);  // [z2, z3]
        uint32x2_t sum = vadd_u32(lo, hi); // [z0 + z2, z1 + z3]
        sum = vpadd_u32(sum, sum);         // [z0..3, same]
        return vcombine_u32(sum, sum);     // [z0..3, same, same, same]
    #endif
    };

#include "polyhash/attributeSensitiveFunctions.inc"

    template <typename UnitType>
    static int polyHashUnaligned(int n, UnitType const* str) {
        Vec128Type res = initVec128();
        if (n >= 122) {
            polyHashUnroll8<NeonTraits>(n, str, res, &b32[0], &p32[0]);
        }
        polyHashUnroll4<NeonTraits>(n, str, res, &b16[0], &p32[16]);
        polyHashUnroll2<NeonTraits>(n, str, res, &b8[0], &p32[24]);
        polyHashTail<NeonTraits>(n, str, res, &b4[0], &p32[28]);
        return vec128toInt(res);
    }
};

#if defined(__aarch64__)
    const bool neonSupported = true; // AArch64 always supports Neon.
#elif defined(__ANDROID__)
    #include <cpu-features.h>
    const bool neonSupported = android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON;
#elif defined(__APPLE__)
    const bool neonSupported = true; // It is supported starting from iPhone 3GS.
#elif defined(__linux__) or defined(__unix__)
    #include <sys/auxv.h>
    #include <asm/hwcap.h>
    const bool neonSupported = getauxval(AT_HWCAP) & HWCAP_NEON;
#else
    #error "Not supported"
#endif

}

template <typename UnitType>
int polyHash(int length, UnitType const* str) {
    if (!neonSupported) {
        return polyHash_naive(length, str);
    }
    uint32_t res = NeonTraits::polyHashUnaligned(length / 4, str);
    for (int i = length & 0xFFFFFFFC; i < length; ++i)
        res = res * 31 + str[i];
    return res;
}
