/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "polyhash/PolyHash.h"
#include "polyhash/naive.h"
#include "polyhash/x86.h"
#include "polyhash/arm.h"

int polyHash(int length, uint16_t const* str) {
#if defined(__x86_64__) or defined(__i386__)
    return polyHash_x86(length, str);
#elif defined(__arm__) or defined(__aarch64__)
    return polyHash_arm(length, str);
#else
    return polyHash_naive(length, str);
#endif
}