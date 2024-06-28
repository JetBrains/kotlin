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
    RuntimeAssert(allocatedBytes >= allocatedBytesLastRecorded_,
                  "A page can't overflow with less allocated bytes (%zu) than there were after the last sweep (%zu)",
                  allocatedBytes, allocatedBytesLastRecorded_);
    auto allocatedSinceLastSweep = allocatedBytes - allocatedBytesLastRecorded_;
    allocatedBytesLastRecorded_ = allocatedBytes;
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifference(static_cast<std::ptrdiff_t>(allocatedSinceLastSweep), true);
}

void alloc::AllocatedSizeTracker::Page::afterSweep(std::size_t allocatedBytes) noexcept {
    auto diffBytes = static_cast<std::ptrdiff_t>(allocatedBytes) - static_cast<std::ptrdiff_t>(allocatedBytesLastRecorded_);
    allocatedBytesLastRecorded_ = allocatedBytes;
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifference(diffBytes, false);
}

void alloc::AllocatedSizeTracker::Heap::recordDifference(std::ptrdiff_t diffBytes, bool notifyScheduler) noexcept {
    auto prevRecord = allocatedBytes_.fetch_add(diffBytes, std::memory_order_relaxed);
    RuntimeAssert(diffBytes >= 0 || prevRecord >= -diffBytes, "Negative overflow: %td+(%td) must be >= 0", prevRecord, diffBytes);
    if (notifyScheduler) {
        OnMemoryAllocation(prevRecord + diffBytes);
    }
}
