/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "LargePage.hpp"

#include <atomic>
#include <cstdint>

#include "CustomLogging.hpp"
#include "CustomAllocConstants.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

LargePage* LargePage::Create(uint64_t cellCount) noexcept {
    CustomAllocInfo("LargePage::Create(%" PRIu64 ")", cellCount);
    RuntimeAssert(cellCount > MEDIUM_PAGE_MAX_BLOCK_SIZE, "blockSize too small for large page");
    uint64_t size = sizeof(LargePage) + cellCount * sizeof(uint64_t);
    auto* page = new (SafeAlloc(size)) LargePage();
    page->size_ = size;
    return page;
}

void LargePage::Destroy() noexcept {
    Free(this, size_);
}

uint8_t* LargePage::Data() noexcept {
    return data_;
}

uint8_t* LargePage::TryAllocate() noexcept {
    if (isAllocated_) return nullptr;
    isAllocated_ = true;
    return Data();
}

bool LargePage::Sweep(gc::GCHandle::GCSweepScope& sweepHandle) noexcept {
    CustomAllocDebug("LargePage@%p::Sweep()", this);
    if (!TryResetMark(Data())) {
        isAllocated_ = false;
        sweepHandle.addKeptObject();
        return false;
    }
    sweepHandle.addSweptObject();
    return true;
}

} // namespace kotlin::alloc
