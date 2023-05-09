/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FixedBlockPage.hpp"

#include <atomic>
#include <cstdint>
#include <cstring>
#include <random>

#include "CustomLogging.hpp"
#include "CustomAllocConstants.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

FixedBlockPage* FixedBlockPage::Create(uint32_t blockSize) noexcept {
    CustomAllocInfo("FixedBlockPage::Create(%u)", blockSize);
    RuntimeAssert(blockSize <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE, "blockSize too large for FixedBlockPage");
    return new (SafeAlloc(FIXED_BLOCK_PAGE_SIZE)) FixedBlockPage(blockSize);
}

void FixedBlockPage::Destroy() noexcept {
    Free(this, FIXED_BLOCK_PAGE_SIZE);
}

FixedBlockPage::FixedBlockPage(uint32_t blockSize) noexcept : blockSize_(blockSize) {
    CustomAllocInfo("FixedBlockPage(%p)::FixedBlockPage(%u)", this, blockSize);
    nextFree_ = cells_;
    FixedBlockCell* end = cells_ + (FIXED_BLOCK_PAGE_CELL_COUNT + 1 - blockSize_);
    for (FixedBlockCell* cell = cells_; cell < end; cell = cell->nextFree) {
        cell->nextFree = cell + blockSize;
    }
}

uint8_t* FixedBlockPage::TryAllocate() noexcept {
    FixedBlockCell* end = cells_ + (FIXED_BLOCK_PAGE_CELL_COUNT + 1 - blockSize_);
    FixedBlockCell* freeBlock = nextFree_;
    if (freeBlock >= end) {
        return nullptr;
    }
    nextFree_ = freeBlock->nextFree;
    CustomAllocDebug("FixedBlockPage(%p){%u}::TryAllocate() = %p", this, blockSize_, freeBlock->data);
    return freeBlock->data;
}

bool FixedBlockPage::Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocInfo("FixedBlockPage(%p)::Sweep()", this);
    // `end` is after the last legal allocation of a block, but does not
    // necessarily match an actual block starting point.
    FixedBlockCell* end = cells_ + (FIXED_BLOCK_PAGE_CELL_COUNT + 1 - blockSize_);
    bool alive = false;
    FixedBlockCell** nextFree = &nextFree_;
    for (FixedBlockCell* cell = cells_; cell < end; cell += blockSize_) {
        // If the current cell is free, move on.
        if (cell == *nextFree) {
            nextFree = &cell->nextFree;
            continue;
        }
        // If the current cell was marked, it's alive, and the whole page is alive.
        if (SweepObject(cell->data, finalizerQueue, sweepHandle)) {
            alive = true;
            sweepHandle.addKeptObject();
            continue;
        }
        CustomAllocInfo("FixedBlockPage(%p)::Sweep: reclaim %p", this, cell);
        // Free the current block and insert it into the free list.
        cell->nextFree = *nextFree;
        *nextFree = cell;
        nextFree = &cell->nextFree;
        sweepHandle.addSweptObject();
    }
    return alive;
}

} // namespace kotlin::alloc
