/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstdint>

#include "Common.h"

extern "C" uintptr_t Kotlin_mm_compressedRefsBase;

namespace kotlin::mm {

// Objects are 8-byte aligned, so 32-bit offsets address a 32 GB window.
inline constexpr unsigned kCompressedRefsShift = 3;
inline constexpr uint64_t kCompressedRefsWindowSize = uint64_t{1} << (32 + kCompressedRefsShift);

ALWAYS_INLINE inline uintptr_t compressedRefsBase() noexcept {
    return Kotlin_mm_compressedRefsBase;
}

}