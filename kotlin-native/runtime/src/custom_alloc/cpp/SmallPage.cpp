/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SmallPage.hpp"

#include <atomic>
#include <cstdint>
#include <cstring>
#include <random>

#include "CustomLogging.hpp"
#include "CustomAllocConstants.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

SmallPage* SmallPage::Create(uint32_t blockSize) noexcept {
    CustomAllocInfo("SmallPage::Create(%u)", blockSize);
    RuntimeAssert(blockSize <= SMALL_PAGE_MAX_BLOCK_SIZE, "blockSize too large for small page");
    return new (SafeAlloc(SMALL_PAGE_SIZE)) SmallPage(blockSize);
}

void SmallPage::Destroy() noexcept {
    std_support::free(this);
}

SmallPage::SmallPage(uint32_t blockSize) noexcept : blockSize_(blockSize) {
    CustomAllocInfo("SmallPage(%p)::SmallPage(%u)", this, blockSize);
    nextFree_ = cells_;
    SmallCell* end = cells_ + (SMALL_PAGE_CELL_COUNT + 1 - blockSize_);
    for (SmallCell* cell = cells_; cell < end; cell = cell->nextFree) {
        cell->nextFree = cell + blockSize;
    }
}

uint8_t* SmallPage::TryAllocate() noexcept {
    SmallCell* end = cells_ + (SMALL_PAGE_CELL_COUNT + 1 - blockSize_);
    SmallCell* freeBlock = nextFree_;
    if (freeBlock >= end) {
        return nullptr;
    }
    nextFree_ = freeBlock->nextFree;
    CustomAllocDebug("SmallPage(%p){%u}::TryAllocate() = %p", this, blockSize_, freeBlock);
    return freeBlock->data;
}

bool SmallPage::Sweep() noexcept {
    CustomAllocInfo("SmallPage(%p)::Sweep()", this);
    // `end` is after the last legal allocation of a block, but does not
    // necessarily match an actual block starting point.
    SmallCell* end = cells_ + (SMALL_PAGE_CELL_COUNT + 1 - blockSize_);
    bool alive = false;
    SmallCell** nextFree = &nextFree_;
    for (SmallCell* cell = cells_; cell < end; cell += blockSize_) {
        // If the current cell is free, move on.
        if (cell == *nextFree) {
            nextFree = &cell->nextFree;
            continue;
        }
        // If the current cell was marked, it's alive, and the whole page is alive.
        if (TryResetMark(cell)) {
            alive = true;
            continue;
        }
        CustomAllocInfo("SmallPage(%p)::Sweep: reclaim %p", this, cell);
        // Free the current block and insert it into the free list.
        cell->nextFree = *nextFree;
        *nextFree = cell;
        nextFree = &cell->nextFree;
    }
    return alive;
}

} // namespace kotlin::alloc
