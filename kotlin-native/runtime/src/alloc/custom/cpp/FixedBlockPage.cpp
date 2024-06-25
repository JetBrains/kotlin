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
#include "GCApi.hpp"

namespace kotlin::alloc {

FixedBlockPage* FixedBlockPage::Create(uint32_t blockSize) noexcept {
    CustomAllocInfo("FixedBlockPage::Create(%u)", blockSize);
    RuntimeAssert(blockSize <= MAX_BLOCK_SIZE, "blockSize too large for FixedBlockPage");
    return new (SafeAlloc(SIZE)) FixedBlockPage(blockSize);
}

void FixedBlockPage::Destroy() noexcept {
    Free(this, SIZE);
}

FixedBlockPage::FixedBlockPage(uint32_t blockSize) noexcept : blockSize_(blockSize) {
    CustomAllocInfo("FixedBlockPage(%p)::FixedBlockPage(%u)", this, blockSize);
    nextFree_.first = 0;
    nextFree_.last = cellCount() / blockSize * blockSize;
    end_ = cellCount() / blockSize * blockSize;
}

PERFORMANCE_INLINE uint8_t* FixedBlockPage::TryAllocate(uint32_t blockSize) noexcept {
    RuntimeAssert(blockSize == blockSize_, "Trying to allocate block of size %d in the FixedBlockPage with block size %d", blockSize, blockSize_);
    uint32_t next = nextFree_.first;
    if (next < nextFree_.last) {
        nextFree_.first += blockSize;
        return cells_[next].data;
    }
    auto end = FIXED_BLOCK_PAGE_CELL_COUNT / blockSize * blockSize;
    if (next >= end) {
        return nullptr;
    }
    nextFree_ = cells_[next].nextFree;
    memset(&cells_[next], 0, sizeof(cells_[next]));
    return cells_[next].data;
}

void FixedBlockPage::OnPageOverflow() noexcept {
    RuntimeAssert(nextFree_.first >= end_, "Page must overflow");
    allocatedSizeTracker_.onPageOverflow(end_ * sizeof(FixedBlockCell));
}

bool FixedBlockPage::Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocInfo("FixedBlockPage(%p)::Sweep()", this);
    FixedCellRange nextFree = nextFree_; // Accessing the previous free list structure.
    FixedCellRange* prevRange = &nextFree_; // Creating the new free list structure.
    uint32_t prevLive = -blockSize_;
    std::size_t aliveBlocksCount = 0;
    for (uint32_t cell = 0 ; cell < end_ ; cell += blockSize_) {
        // Go through the occupied cells.
        for (; cell < nextFree.first ; cell += blockSize_) {
            if (!SweepObject(cells_[cell].data, finalizerQueue, sweepHandle)) {
                // We should null this cell out, but we will do so in batch later.
                continue;
            }
            ++aliveBlocksCount;
            if (prevLive + blockSize_ < cell) {
                // We found an alive cell that ended a run of swept cells or a known unoccupied range.
                uint32_t prevCell = cell - blockSize_;
                // Nulling in batch.
                memset(&cells_[prevLive + blockSize_], 0, (prevCell - prevLive) * sizeof(FixedBlockCell));
                // Updating the free list structure.
                prevRange->first = prevLive + blockSize_;
                prevRange->last = prevCell;
                // And the next unoccupied range will be stored in the last unoccupied cell.
                prevRange = &cells_[prevCell].nextFree;
            }
            prevLive = cell;
        }
        // `cell` now points to a known unoccupied range.
        if (nextFree.last < end_) {
            cell = nextFree.last;
            nextFree = cells_[cell].nextFree;
            continue;
        }
        prevRange->first = prevLive + blockSize_;
        memset(&cells_[prevLive + blockSize_], 0, (cell - prevLive - blockSize_) * sizeof(FixedBlockCell));
        prevRange->last = end_;
        // And we're done.
        break;
    }

    allocatedSizeTracker_.afterSweep(aliveBlocksCount * blockSize_ * sizeof(FixedBlockCell));

    // The page is alive iff a range stored in the page header covers the entire page.
    return nextFree_.first > 0 || nextFree_.last < end_;
}

std::vector<uint8_t*> FixedBlockPage::GetAllocatedBlocks() noexcept {
    std::vector<uint8_t*> allocated;
    CustomAllocInfo("FixedBlockPage(%p)::Sweep()", this);
    TraverseAllocatedBlocks([&allocated](uint8_t* block) {
        allocated.push_back(block);
    });
    return allocated;
}

} // namespace kotlin::alloc
