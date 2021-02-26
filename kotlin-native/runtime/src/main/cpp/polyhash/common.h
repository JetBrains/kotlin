/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_POLYHASH_COMMON_H
#define RUNTIME_POLYHASH_COMMON_H

#include <array>
#include <cstdint>
#include "polyhash/naive.h"
#include "../Common.h"

constexpr uint32_t Power(uint32_t base, uint8_t exponent) {
    uint32_t result = 1;
    for (uint8_t i = 0; i < exponent; ++i) {
        result *= base;
    }
    return result;
}

template <uint8_t Exponent>
constexpr std::array<uint32_t, Exponent> DecreasingPowers(uint32_t base) {
    std::array<uint32_t, Exponent> result = {};
    uint32_t current = 1;
    for (auto it = result.rbegin(); it != result.rend(); ++it) {
        *it = current;
        current *= base;
    }
    return result;
}

template <size_t Count>
constexpr std::array<uint32_t, Count> RepeatingPowers(uint32_t base, uint8_t exponent) {
    std::array<uint32_t, Count> result = {};
    uint32_t value = Power(base, exponent);
    for (auto& element : result)
        element = value;
    return result;
}

template<typename Traits>
ALWAYS_INLINE void polyHashTail(int& n, uint16_t const*& str, typename Traits::Vec128Type& res, uint32_t const* b, uint32_t const* p) {
    using VecType = typename Traits::VecType;
    using Vec128Type = typename Traits::Vec128Type;
    using U16VecType = typename Traits::U16VecType;

    const int vecLength = sizeof(VecType) / 4;
    if (n < vecLength / 4) return;

    VecType x = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str));
    res = Traits::vec128Mul(res, *reinterpret_cast<Vec128Type const*>(b));
    VecType z = Traits::vecMul(x, *reinterpret_cast<VecType const*>(p));
    res = Traits::vec128Add(res, Traits::squash1(z));

    str += vecLength;
    n -= vecLength / 4;
}

template<typename Traits>
ALWAYS_INLINE void polyHashUnroll2(int& n, uint16_t const*& str, typename Traits::Vec128Type& res, uint32_t const* b, uint32_t const* p) {
    using VecType = typename Traits::VecType;
    using Vec128Type = typename Traits::Vec128Type;
    using U16VecType = typename Traits::U16VecType;

    const int vecLength = sizeof(VecType) / 4;
    if (n < vecLength / 2) return;

    res = Traits::vec128Mul(res, *reinterpret_cast<Vec128Type const*>(b));

    VecType res0 = Traits::initVec();
    VecType res1 = Traits::initVec();

    do {
        VecType x0 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str));
        VecType x1 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength));
        res0 = Traits::vecMul(res0, *reinterpret_cast<VecType const*>(b));
        res1 = Traits::vecMul(res1, *reinterpret_cast<VecType const*>(b));
        VecType z0 = Traits::vecMul(x0, *reinterpret_cast<VecType const*>(p));
        VecType z1 = Traits::vecMul(x1, *reinterpret_cast<VecType const*>(p + vecLength));
        res0 = Traits::vecAdd(res0, z0);
        res1 = Traits::vecAdd(res1, z1);

        str += vecLength * 2;
        n -= vecLength / 2;
    } while (n >= vecLength / 2);

    res = Traits::vec128Add(res, Traits::squash2(res0, res1));
}

template<typename Traits>
ALWAYS_INLINE void polyHashUnroll4(int& n, uint16_t const*& str, typename Traits::Vec128Type& res, uint32_t const* b, uint32_t const* p) {
    using VecType = typename Traits::VecType;
    using Vec128Type = typename Traits::Vec128Type;
    using U16VecType = typename Traits::U16VecType;

    const int vecLength = sizeof(VecType) / 4;
    if (n < vecLength) return;

    res = Traits::vec128Mul(res, *reinterpret_cast<Vec128Type const*>(b));

    VecType res0 = Traits::initVec();
    VecType res1 = Traits::initVec();
    VecType res2 = Traits::initVec();
    VecType res3 = Traits::initVec();

    do {
        VecType x0 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str));
        VecType x1 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength));
        VecType x2 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength * 2));
        VecType x3 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength * 3));
        res0 = Traits::vecMul(res0, *reinterpret_cast<VecType const*>(b));
        res1 = Traits::vecMul(res1, *reinterpret_cast<VecType const*>(b));
        res2 = Traits::vecMul(res2, *reinterpret_cast<VecType const*>(b));
        res3 = Traits::vecMul(res3, *reinterpret_cast<VecType const*>(b));
        VecType z0 = Traits::vecMul(x0, *reinterpret_cast<VecType const*>(p));
        VecType z1 = Traits::vecMul(x1, *reinterpret_cast<VecType const*>(p + vecLength));
        VecType z2 = Traits::vecMul(x2, *reinterpret_cast<VecType const*>(p + vecLength * 2));
        VecType z3 = Traits::vecMul(x3, *reinterpret_cast<VecType const*>(p + vecLength * 3));
        res0 = Traits::vecAdd(res0, z0);
        res1 = Traits::vecAdd(res1, z1);
        res2 = Traits::vecAdd(res2, z2);
        res3 = Traits::vecAdd(res3, z3);

        str += vecLength * 4;
        n -= vecLength;
    } while (n >= vecLength);

    res = Traits::vec128Add(res, Traits::vec128Add(Traits::squash2(res0, res1), Traits::squash2(res2, res3)));
}

template<typename Traits>
ALWAYS_INLINE void polyHashUnroll8(int& n, uint16_t const*& str, typename Traits::Vec128Type& res, uint32_t const* b, uint32_t const* p) {
    using VecType = typename Traits::VecType;
    using Vec128Type = typename Traits::Vec128Type;
    using U16VecType = typename Traits::U16VecType;

    const int vecLength = sizeof(VecType) / 4;
    if (n < vecLength * 2) return;

    VecType res0 = Traits::initVec();
    VecType res1 = Traits::initVec();
    VecType res2 = Traits::initVec();
    VecType res3 = Traits::initVec();
    VecType res4 = Traits::initVec();
    VecType res5 = Traits::initVec();
    VecType res6 = Traits::initVec();
    VecType res7 = Traits::initVec();

    do {
        VecType x0 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str));
        VecType x1 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength));
        VecType x2 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength * 2));
        VecType x3 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength * 3));
        VecType x4 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength * 4));
        VecType x5 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength * 5));
        VecType x6 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength * 6));
        VecType x7 = Traits::u16Load(*reinterpret_cast<U16VecType const*>(str + vecLength * 7));
        res0 = Traits::vecMul(res0, *reinterpret_cast<VecType const*>(b));
        res1 = Traits::vecMul(res1, *reinterpret_cast<VecType const*>(b));
        res2 = Traits::vecMul(res2, *reinterpret_cast<VecType const*>(b));
        res3 = Traits::vecMul(res3, *reinterpret_cast<VecType const*>(b));
        res4 = Traits::vecMul(res4, *reinterpret_cast<VecType const*>(b));
        res5 = Traits::vecMul(res5, *reinterpret_cast<VecType const*>(b));
        res6 = Traits::vecMul(res6, *reinterpret_cast<VecType const*>(b));
        res7 = Traits::vecMul(res7, *reinterpret_cast<VecType const*>(b));
        VecType z0 = Traits::vecMul(x0, *reinterpret_cast<VecType const*>(p));
        VecType z1 = Traits::vecMul(x1, *reinterpret_cast<VecType const*>(p + vecLength));
        VecType z2 = Traits::vecMul(x2, *reinterpret_cast<VecType const*>(p + vecLength * 2));
        VecType z3 = Traits::vecMul(x3, *reinterpret_cast<VecType const*>(p + vecLength * 3));
        VecType z4 = Traits::vecMul(x4, *reinterpret_cast<VecType const*>(p + vecLength * 4));
        VecType z5 = Traits::vecMul(x5, *reinterpret_cast<VecType const*>(p + vecLength * 5));
        VecType z6 = Traits::vecMul(x6, *reinterpret_cast<VecType const*>(p + vecLength * 6));
        VecType z7 = Traits::vecMul(x7, *reinterpret_cast<VecType const*>(p + vecLength * 7));
        res0 = Traits::vecAdd(res0, z0);
        res1 = Traits::vecAdd(res1, z1);
        res2 = Traits::vecAdd(res2, z2);
        res3 = Traits::vecAdd(res3, z3);
        res4 = Traits::vecAdd(res4, z4);
        res5 = Traits::vecAdd(res5, z5);
        res6 = Traits::vecAdd(res6, z6);
        res7 = Traits::vecAdd(res7, z7);

        str += vecLength * 8;
        n -= vecLength * 2;
    } while (n >= vecLength * 2);

    Vec128Type sum1 = Traits::vec128Add(Traits::squash2(res0, res1), Traits::squash2(res2, res3));
    Vec128Type sum2 = Traits::vec128Add(Traits::squash2(res4, res5), Traits::squash2(res6, res7));
    res = Traits::vec128Add(res, Traits::vec128Add(sum1, sum2));
}

#endif  // RUNTIME_POLYHASH_COMMON_H
