/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectAlloc.hpp"

#if KONAN_INTERNAL_DLMALLOC
extern "C" void* dlcalloc(size_t, size_t);
extern "C" void dlfree(void*);

#define callocImpl dlcalloc
#define freeImpl dlfree
#else
#include <cstdlib>

#define callocImpl ::calloc
#define freeImpl ::free
#endif

using namespace kotlin;

void kotlin::initObjectPool() noexcept {}

void* kotlin::allocateInObjectPool(size_t size) noexcept {
    // TODO: Check that alignment to kObjectAlignment is satisfied.
    return callocImpl(1, size);
}

void kotlin::freeInObjectPool(void* ptr) noexcept {
    freeImpl(ptr);
}

void kotlin::compactObjectPoolInCurrentThread() noexcept {}

void kotlin::compactObjectPoolInMainThread() noexcept {}