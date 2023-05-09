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
    auto* page = new (SafeAlloc(size)) SingleObjectPage();
    page->size_ = size;
    return page;
}

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

bool SingleObjectPage::Sweep(gc::GCHandle::GCSweepScope& sweepHandle) noexcept {
    CustomAllocDebug("SingleObjectPage@%p::Sweep()", this);
    if (!TryResetMark(Data())) {
        isAllocated_ = false;
        sweepHandle.addKeptObject();
        return false;
    }
    sweepHandle.addSweptObject();
    return true;
}

} // namespace kotlin::alloc
