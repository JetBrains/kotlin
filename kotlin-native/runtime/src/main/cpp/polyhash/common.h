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

#endif  // RUNTIME_POLYHASH_COMMON_H
