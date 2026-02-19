/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_POLYHASH_NAIVE_H
#define RUNTIME_POLYHASH_NAIVE_H

#include <cstdint>

template <typename It>
inline int polyHash_naive(It begin, It end) {
    uint32_t res = 0;
    while (begin != end)
        res = res * 31 + static_cast<uint16_t>(*begin++);
    return res;
}

template <typename UnitType>
inline int polyHash_naive(int length, UnitType const* str) {
    return polyHash_naive(str, str + length);
}

#endif  // RUNTIME_POLYHASH_NAIVE_H
