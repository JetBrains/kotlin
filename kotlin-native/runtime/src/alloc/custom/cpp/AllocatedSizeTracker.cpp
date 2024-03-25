/*
 * Copyright 2022-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AllocatedSizeTracker.hpp"
#include "AllocatorImpl.hpp"
#include "GlobalData.hpp"
#include "KAssert.h"

using namespace kotlin;

void alloc::AllocatedSizeTracker::Page::onPageOverflow(std::size_t allocatedBytes) noexcept {
    auto allocatedSinceLastSweep = allocatedBytes - allocatedBytesLastRecorded_;
    allocatedBytesLastRecorded_ = allocatedBytes;
    RuntimeAssert(allocatedSinceLastSweep >= 0, "");
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifference(static_cast<std::ptrdiff_t>(allocatedSinceLastSweep));
    heap.allocatedSizeTracker().notifyScheduler();
}

void alloc::AllocatedSizeTracker::Page::afterSweep(std::size_t allocatedBytes) noexcept {
    auto diffBytes = static_cast<std::ptrdiff_t>(allocatedBytes) - static_cast<std::ptrdiff_t>(allocatedBytesLastRecorded_);
    allocatedBytesLastRecorded_ = allocatedBytes;
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifference(diffBytes);
}

void alloc::AllocatedSizeTracker::Heap::recordDifference(std::ptrdiff_t diffBytes) noexcept {
    auto prevRecord = allocatedBytes_.fetch_add(diffBytes, std::memory_order_relaxed);
    RuntimeAssert(diffBytes >= 0 || prevRecord >= static_cast<std::size_t>(-diffBytes),
                  "Negative overflow: %zu+(%zd) must be >= 0", prevRecord, diffBytes);
}

void alloc::AllocatedSizeTracker::Heap::notifyScheduler() const noexcept {
    OnMemoryAllocation(allocatedBytes_.load(std::memory_order_relaxed));
}
