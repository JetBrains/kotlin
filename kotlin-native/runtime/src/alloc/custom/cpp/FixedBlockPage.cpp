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

namespace {
    const uint32_t BUCKET_BIT_LENGTH = 2;
    const uint32_t BUCKET_BIT_MASK = (1 << BUCKET_BIT_LENGTH) - 1;
}

ALWAYS_INLINE uint32_t FixedBlockPage::BucketIndex(uint32_t blockSize) noexcept {
    // If blockSize isn't big enough for the bucket to contain two sizes, then escape early
    if (blockSize < 2 << BUCKET_BIT_LENGTH) {
        return blockSize;
    }
    // Test if we have IEEE754 floating point on target CPU
    if constexpr (std::numeric_limits<float>::is_iec559) {
        // Convert to float
        float f = blockSize;
        // Take the raw bits of the floating point number. With C++20, this is std::bit_cast
        uint32_t bits = *(int*)&f;
        // Extract the exponent and BIT_LENGTH number of most significant bits of the fractional part
        uint32_t bucket = bits >> (23 - BUCKET_BIT_LENGTH);
        // Subtract the bias, so the buckets can start from 0 and align with the early escape
        bucket -= (128 << BUCKET_BIT_LENGTH);
        return bucket;
    } else {
        // Emulate the IEEE754 buckets, with bit length instead of exponent
        // Example for bit length 3
        // blockSize:  83 = 0b1010011
        // msb:               |- 9 -|
        // fraction, 3 bits: 0b010
        // bucket:     (9<<3) | 0b010
        // subtract:      4 << 3 = 32
        int msb = 31 - __builtin_clz(blockSize);
        int fraction = blockSize >> (msb - BUCKET_BIT_LENGTH) &BUCKET_BIT_MASK;
        int bucket = (msb << BUCKET_BIT_LENGTH) | fraction;
        bucket -= ((BUCKET_BIT_LENGTH - 1) << BUCKET_BIT_LENGTH);
        return bucket;
    }
}

ALWAYS_INLINE uint32_t FixedBlockPage::BucketSize(uint32_t blockSize) noexcept {
    uint32_t bucketSize = blockSize | (uint32_t(-1) >> (__builtin_clz(blockSize) + BUCKET_BIT_LENGTH + 1));
    return bucketSize;
}

ALWAYS_INLINE uint8_t* FixedBlockPage::TryAllocate(uint32_t blockSize) noexcept {
    RuntimeAssert(blockSize == blockSize_, "Trying to allocate block of size %d in the FixedBlockPage with block size %d", blockSize, blockSize_);
    uint32_t next = nextFree_.first;
    if (next < nextFree_.last) {
        nextFree_.first += blockSize_;
        return cells_[next].data;
    }
    if (next >= end_) {
        allocatedSizeTracker_.onPageOverflow(end_ * sizeof(FixedBlockCell));
        return nullptr;
    }
    nextFree_ = cells_[next].nextFree;
    memset(&cells_[next], 0, sizeof(cells_[next]));
    return cells_[next].data;
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
