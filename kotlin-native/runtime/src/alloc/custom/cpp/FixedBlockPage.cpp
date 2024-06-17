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
    nextFreeCell_ = cells_;
    end_ = FIXED_BLOCK_PAGE_CELL_COUNT / blockSize * blockSize;
    nextFreeRangeLast_ = &cells_[end_];
}

uint8_t* FixedBlockPage::TryAllocate(uint32_t blockSize) noexcept {
    auto fastAllocated = TryAllocateFast(blockSize);
    if (fastAllocated) return *fastAllocated;
    return TryAllocateBackup();
}
ALWAYS_INLINE std::optional<uint8_t*> FixedBlockPage::TryAllocateFast(uint32_t blockSize) noexcept {
    RuntimeAssert(blockSize == blockSize_, "Trying to allocate block of size %d in the FixedBlockPage with block size %d", blockSize, blockSize_);
    auto next = nextFreeCell_;
    if (next < nextFreeRangeLast_) {
        nextFreeCell_ = next + blockSize;
        return next->data;
    }
    return std::nullopt;
}
uint8_t* FixedBlockPage::TryAllocateBackup() noexcept {
    RuntimeAssert(nextFreeCell_ >= nextFreeRangeLast_, ""); // TODO comment
    // FIXME generalize
    auto next = nextFreeCell_; // FIXME reread?
    if (next >= &cells_[end_]) {
        return nullptr;
    }
    nextFreeCell_ = &cells_[next->nextFree.first];
    nextFreeRangeLast_ = &cells_[next->nextFree.last];
    memset(next, 0, sizeof(FixedBlockCell));
    return next->data;
}

void FixedBlockPage::OnPageOverflow() noexcept {
    RuntimeAssert(nextFreeCell_ >= &cells_[end_], "Page must overflow"); // FIXME only == ?
    allocatedSizeTracker_.onPageOverflow(end_ * sizeof(FixedBlockCell));
}

bool FixedBlockPage::Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocInfo("FixedBlockPage(%p)::Sweep()", this);
    FixedCellRange nextFree{static_cast<uint32_t>(nextFreeCell_ - cells_), static_cast<uint32_t>(nextFreeRangeLast_ - cells_)}; // Accessing the previous free list structure.
    FixedCellRange newRangewInPageHeader{0, 0};
    FixedCellRange* prevRange = &newRangewInPageHeader; // Creating the new free list structure. // FIXME complicated here
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
    nextFreeCell_ = &cells_[newRangewInPageHeader.first];
    nextFreeRangeLast_ = &cells_[newRangewInPageHeader.last];

    allocatedSizeTracker_.afterSweep(aliveBlocksCount * blockSize_ * sizeof(FixedBlockCell));

    // The page is alive iff a range stored in the page header covers the entire page.
    return nextFreeCell_ > cells_ || nextFreeRangeLast_ < &cells_[end_];
}

std::vector<uint8_t*> FixedBlockPage::GetAllocatedBlocks() noexcept {
    std::vector<uint8_t*> allocated;
    CustomAllocInfo("FixedBlockPage(%p)::Sweep()", this);
    FixedCellRange nextFree{static_cast<uint32_t>(nextFreeCell_ - cells_), static_cast<uint32_t>(nextFreeRangeLast_ - cells_)}; // Accessing the previous free list structure.
    for (uint32_t cell = 0 ; cell < end_ ; cell += blockSize_) {
        for (; cell < nextFree.first ; cell += blockSize_) {
            allocated.push_back(cells_[cell].data);
        }
        if (nextFree.last >= end_) {
            break;
        }
        cell = nextFree.last;
        nextFree = cells_[cell].nextFree;
    }
    return allocated;
}

} // namespace kotlin::alloc
