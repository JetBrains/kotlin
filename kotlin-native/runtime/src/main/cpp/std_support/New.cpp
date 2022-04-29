/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/New.hpp"

#include "std_support/CStdlib.hpp"

using namespace kotlin;

// TODO: Maybe malloc instead of calloc?

void* operator new(std::size_t count, kotlin::std_support::kalloc_t) noexcept {
    return std_support::calloc(1, count);
}

void operator delete(void* ptr, kotlin::std_support::kalloc_t) noexcept {
    std_support::free(ptr);
}
