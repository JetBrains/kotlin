/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/CStdlib.hpp"

#include <cstdint>
#include <cstdlib>
#include <mm_malloc.h>
#include <unistd.h>

#include "Alignment.hpp"
#include "KAssert.h"

using namespace kotlin;

void* std_support::malloc(std::size_t size) noexcept {
    return std::malloc(size);
}

void* std_support::aligned_malloc(std::size_t alignment, std::size_t size) noexcept {
    // Enforcing alignment requirements of std::aligned_alloc.
    RuntimeAssert(IsValidAlignment(alignment), "Invalid alignment %zu", alignment);
    RuntimeAssert(IsAligned(size, alignment), "Size %zu must be aligned to %zu", size, alignment);
    return ::_mm_malloc(size, alignment);
}

void* std_support::calloc(std::size_t num, std::size_t size) noexcept {
    return std::calloc(num, size);
}

void* std_support::realloc(void* ptr, std::size_t size) noexcept {
    return std::realloc(ptr, size);
}

void std_support::free(void* ptr) noexcept {
    return std::free(ptr);
}

void std_support::aligned_free(void* ptr) noexcept {
    return ::_mm_free(ptr);
}
