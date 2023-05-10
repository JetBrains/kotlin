/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SingleObjectPage.hpp"

#include <atomic>
#include <cstdint>

#include "CustomLogging.hpp"
#include "CustomAllocConstants.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

SingleObjectPage* SingleObjectPage::Create(uint64_t cellCount) noexcept {
    CustomAllocInfo("SingleObjectPage::Create(%" PRIu64 ")", cellCount);
    RuntimeAssert(cellCount > NEXT_FIT_PAGE_MAX_BLOCK_SIZE, "blockSize too small for SingleObjectPage");
    uint64_t size = sizeof(SingleObjectPage) + cellCount * sizeof(uint64_t);
    return new (SafeAlloc(size)) SingleObjectPage(size);
}

SingleObjectPage::SingleObjectPage(size_t size) noexcept : size_(size) {}

void SingleObjectPage::Destroy() noexcept {
    Free(this, size_);
}

uint8_t* SingleObjectPage::Data() noexcept {
    return data_;
}

uint8_t* SingleObjectPage::TryAllocate() noexcept {
    if (isAllocated_) return nullptr;
    isAllocated_ = true;
    return Data();
}

bool SingleObjectPage::Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocDebug("SingleObjectPage@%p::Sweep()", this);
    if (SweepObject(Data(), finalizerQueue, sweepHandle)) {
        return true;
    }
    isAllocated_ = false;
    return false;
}

} // namespace kotlin::alloc
