/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Utils.hpp"

#include <climits>
#include <cstdint>
#include <cstring>

using namespace kotlin;

namespace {

// Hash combine functions derived from boost ones.
// Copyright 2005-2014 Daniel James.

template <typename X, typename R>
constexpr auto rotl32(X x, R r) noexcept { return (x << r) | (x >> (32 - r)); }

template<size_t Bits>
struct HashCompineImpl {
    template <typename SizeT>
    constexpr static SizeT fn(SizeT seed, SizeT value) {
        seed ^= value + 0x9e3779b9 + (seed<<6) + (seed>>2);
        return seed;
    }
};

template<>
struct HashCompineImpl<32> {
    constexpr static uint32_t fn(uint32_t h1, uint32_t k1) {
        const uint32_t c1 = 0xcc9e2d51;
        const uint32_t c2 = 0x1b873593;

        k1 *= c1;
        k1 = rotl32(k1,15);
        k1 *= c2;

        h1 ^= k1;
        h1 = rotl32(h1,13);
        h1 = h1*5+0xe6546b64;

        return h1;
    }
};

template<>
struct HashCompineImpl<64> {
    constexpr static uint64_t fn(uint64_t h, uint64_t k) {
        const uint64_t m = (uint64_t(0xc6a4a793) << 32) + 0x5bd1e995;
        const int r = 47;

        k *= m;
        k ^= k >> r;
        k *= m;

        h ^= k;
        h *= m;

        // Completely arbitrary number, to prevent 0's
        // from hashing to 0.
        h += 0xe6546b64;

        return h;
    }
};

} // namespace

size_t kotlin::CombineHash(size_t seed, size_t value) {
    return HashCompineImpl<sizeof(std::size_t) * CHAR_BIT>::fn(seed, value);
}

bool kotlin::isZeroed(std_support::span<uint8_t> span) noexcept {
    if (span.size() == 0) return true;
    if (span[0] != 0) return false;
    return memcmp(span.data(), span.data() + 1, span.size() - 1) == 0;
}
