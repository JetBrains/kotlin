/*
 * Copyright 2022-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AllocatedSizeTracker.hpp"
#include "AllocatorImpl.hpp"
#include "GlobalData.hpp"
#include "KAssert.h"

using namespace kotlin;

namespace {
void (*schedulerNotificationTestHook)(std::size_t) = nullptr;
}

void alloc::AllocatedSizeTracker::Page::onPageOverflow(std::size_t allocatedBytes) noexcept {
    RuntimeAssert(allocatedBytes >= allocatedBytesLastRecorded_,
                  "A page can't overflow with less allocated bytes (%zu) than there were after the last sweep (%zu)",
                  allocatedBytes, allocatedBytesLastRecorded_);
    auto allocatedSinceLastSweep = allocatedBytes - allocatedBytesLastRecorded_;
    allocatedBytesLastRecorded_ = allocatedBytes;
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifferenceAndNotifyScheduler(static_cast<std::ptrdiff_t>(allocatedSinceLastSweep));
}

void alloc::AllocatedSizeTracker::Page::afterSweep(std::size_t allocatedBytes) noexcept {
    auto diffBytes = static_cast<std::ptrdiff_t>(allocatedBytes) - static_cast<std::ptrdiff_t>(allocatedBytesLastRecorded_);
    allocatedBytesLastRecorded_ = allocatedBytes;
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifference(diffBytes);
}

std::size_t alloc::AllocatedSizeTracker::Heap::recordDifference(std::ptrdiff_t diffBytes) noexcept {
    auto prevRecord = allocatedBytes_.fetch_add(diffBytes, std::memory_order_relaxed);
    RuntimeAssert(diffBytes >= 0 || prevRecord >= -diffBytes, "Negative overflow: %td+(%td) must be >= 0", prevRecord, diffBytes);
    return prevRecord + diffBytes;
}

void alloc::AllocatedSizeTracker::Heap::recordDifferenceAndNotifyScheduler(std::ptrdiff_t diffBytes) noexcept {
    auto nowAllocated = recordDifference(diffBytes);

    if (schedulerNotificationTestHook) {
        schedulerNotificationTestHook(nowAllocated);
    }
    OnMemoryAllocation(nowAllocated);
}

void alloc::test_support::setSchedulerNotificationHook(void (*hook)(std::size_t)) noexcept {
    schedulerNotificationTestHook = hook;
}
