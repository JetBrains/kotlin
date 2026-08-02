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
    return new (SafeAlloc(SIZE())) FixedBlockPage(blockSize);
}

void FixedBlockPage::Destroy() noexcept {
    Free(this, SIZE());
}

FixedBlockPage::FixedBlockPage(uint32_t blockSize) noexcept : blockSize_(blockSize) {
    CustomAllocInfo("FixedBlockPage(%p)::FixedBlockPage(%u)", this, blockSize);
    nextFree_.first = 0;
    nextFree_.last = cellCount() / blockSize * blockSize;
    end_ = cellCount() / blockSize * blockSize;
}

uint8_t* FixedBlockPage::TryAllocate() noexcept {
    uint32_t next = nextFree_.first;
    if (next < nextFree_.last) {
        nextFree_.first += blockSize_;
        allocatedSinceSweep_ = true; // page now holds a young object; must be swept next Eden
        return cells_[next].data;
    }
    if (next >= end_) {
        allocatedSizeTracker_.onPageOverflow(end_ * sizeof(FixedBlockCell));
        return nullptr;
    }
    nextFree_ = cells_[next].nextFree;
    memset(&cells_[next], 0, sizeof(cells_[next]));
    allocatedSinceSweep_ = true; // page now holds a young object; must be swept next Eden
    return cells_[next].data;
}

ObjHeader* FixedBlockPage::objectContainingInteriorPointer(void* interiorPointer) noexcept {
    auto* p = reinterpret_cast<uint8_t*>(interiorPointer);
    auto* base = reinterpret_cast<uint8_t*>(cells_);
    auto* limit = base + static_cast<size_t>(end_) * sizeof(FixedBlockCell);
    if (p < base || p >= limit) return nullptr;
    // All blocks are `blockSize_` cells wide and start at cell offsets that are multiples of
    // `blockSize_` from `cells_`, so the containing block's first cell is found by rounding down.
    size_t cellIndex = static_cast<size_t>(p - base) / sizeof(FixedBlockCell);
    uint32_t blockStartCell = static_cast<uint32_t>((cellIndex / blockSize_) * blockSize_);
    // Reject a pointer into a free (unallocated) cell. This resolver filters the generational
    // remembered set, which can hold a slot inside an object that was freed since the slot was recorded
    // (see ConcurrentMark::seedRememberedSets); returning a free cell as a "container" would let the
    // drain read its first word -- a FixedCellRange free-list link, whose low bits can spoof isOld() --
    // and then dereference the stale slot. Walk the free list (ranges are ordered by ascending start
    // cell, each run's link stored in its last cell) and treat the block as free iff it falls in a run.
    // This makes the resolver correct by construction rather than relying on swept cells being zeroed,
    // mirroring the isAllocated_ guard NextFitPage applies for the same reason.
    for (FixedCellRange range = nextFree_;;) {
        if (blockStartCell < range.first) break; // before this free run: the block is occupied.
        if (blockStartCell <= range.last) return nullptr; // inside a free run [first, last]: unallocated.
        if (range.last >= end_) break; // no further free runs: the block is occupied.
        range = cells_[range.last].nextFree;
    }
    auto* block = reinterpret_cast<uint8_t*>(&cells_[blockStartCell]);
    return reinterpret_cast<CustomHeapObject*>(block)->object();
}

std::vector<uint8_t*> FixedBlockPage::GetAllocatedBlocks() noexcept {
    std::vector<uint8_t*> allocated;
    CustomAllocInfo("FixedBlockPage(%p)::Sweep()", this);
    TraverseAllocatedBlocks([&allocated](uint8_t* block) { allocated.push_back(block); });
    return allocated;
}

} // namespace kotlin::alloc
