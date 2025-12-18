/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectAlloc.hpp"

#include <atomic>
#include <cstdlib>

#include "GlobalData.hpp"

using namespace kotlin;

namespace {

std::atomic<size_t> allocatedBytesCounter = 0;

} // namespace

void* alloc::allocateInObjectPool(size_t size) noexcept {
    // TODO: Check that alignment to kObjectAlignment is satisfied.
    void* result = ::calloc(1, size);
    auto newSize = allocatedBytesCounter.fetch_add(size, std::memory_order_relaxed);
    newSize += size;
    mm::GlobalData::Instance().gcScheduler().setAllocatedBytes(newSize);
    return result;
}

void alloc::freeInObjectPool(void* ptr, size_t size) noexcept {
    allocatedBytesCounter.fetch_sub(size, std::memory_order_relaxed);
    ::free(ptr);
}

size_t alloc::allocatedBytes() noexcept {
    return allocatedBytesCounter.load();
}
