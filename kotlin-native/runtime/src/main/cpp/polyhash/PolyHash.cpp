/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "polyhash/PolyHash.h"
#include "polyhash/naive.h"

#if defined(__x86_64__) or defined(__i386__)
#include "polyhash/PolyHash-x86.h"
#elif (defined(__arm__) or defined(__aarch64__)) and defined(__ARM_NEON)
#include "polyhash/PolyHash-arm.h"
#else

template <typename UnitType>
int polyHash(int length, UnitType const* str) {
    return polyHash_naive(length, str);
}

#endif

template int polyHash<uint16_t>(int, uint16_t const*);
template int polyHash<uint8_t>(int, uint8_t const*);
