/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_POLYHASH_NAIVE_H
#define RUNTIME_POLYHASH_NAIVE_H

#include <cstdint>

inline int polyHash_naive(int length, uint16_t const* str) {
    uint32_t res = 0;
    for (int i = 0; i < length; ++i)
        res = res * 31 + str[i];
    return res;
}

#endif  // RUNTIME_POLYHASH_NAIVE_H
