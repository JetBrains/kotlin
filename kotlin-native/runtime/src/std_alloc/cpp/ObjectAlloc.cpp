/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectAlloc.hpp"

#ifndef KONAN_NO_THREADS
#include <atomic>
#endif

#include "Memory.h"

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

namespace {

#ifndef KONAN_NO_THREADS
std::atomic<size_t> allocatedBytesCounter = 0;
#else
size_t allocatedBytesCounter = 0;
#endif

} // namespace

void kotlin::initObjectPool() noexcept {}

void* kotlin::allocateInObjectPool(size_t size) noexcept {
    // TODO: Check that alignment to kObjectAlignment is satisfied.
    void* result = callocImpl(1, size);
#ifndef KONAN_NO_THREADS
    auto newSize = allocatedBytesCounter.fetch_add(size, std::memory_order_relaxed);
    newSize += size;
#else
    allocatedBytesCounter += size;
    auto newSize = allocatedBytesCounter;
#endif
    OnMemoryAllocation(newSize);
    return result;
}

void kotlin::freeInObjectPool(void* ptr, size_t size) noexcept {
#ifndef KONAN_NO_THREADS
    allocatedBytesCounter.fetch_sub(size, std::memory_order_relaxed);
#else
    allocatedBytesCounter -= size;
#endif
    freeImpl(ptr);
}

void kotlin::compactObjectPoolInCurrentThread() noexcept {}

void kotlin::compactObjectPoolInMainThread() noexcept {}

size_t kotlin::allocatedBytes() noexcept {
#ifndef KONAN_NO_THREADS
    return allocatedBytesCounter.load();
#else
    return allocatedBytesCounter;
#endif
}
