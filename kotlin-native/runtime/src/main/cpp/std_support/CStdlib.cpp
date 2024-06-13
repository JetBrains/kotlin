/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/CStdlib.hpp"

#include <mm_malloc.h>

#include "Alignment.hpp"
#include "KAssert.h"

#ifdef KONAN_ZEPHYR
extern "C" void* k_aligned_alloc(std::size_t align, std::size_t size);
#endif

using namespace kotlin;

void* std_support::aligned_malloc(std::size_t alignment, std::size_t size) noexcept {
#ifdef KONAN_ZEPHYR
    return k_aligned_alloc(alignment, size);
#else
    // Enforcing alignment requirements of std::aligned_alloc.
    RuntimeAssert(IsValidAlignment(alignment), "Invalid alignment %zu", alignment);
    RuntimeAssert(IsAligned(size, alignment), "Size %zu must be aligned to %zu", size, alignment);
    return ::_mm_malloc(size, alignment);
#endif
}

void std_support::aligned_free(void* ptr) noexcept {
    return ::_mm_free(ptr);
}
