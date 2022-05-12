/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectAlloc.hpp"

#include "../../mimalloc/c/include/mimalloc.h"
#include "Alignment.hpp"

using namespace kotlin;

void kotlin::initObjectPool() noexcept {
    mi_thread_init();
}

void* kotlin::allocateInObjectPool(size_t size) noexcept {
    return mi_calloc_aligned(1, size, kObjectAlignment);
}

void kotlin::freeInObjectPool(void* ptr) noexcept {
    mi_free(ptr);
}
