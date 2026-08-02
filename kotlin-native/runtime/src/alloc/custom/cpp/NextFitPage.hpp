/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_NEXTFITPAGE_HPP_
#define CUSTOM_ALLOC_CPP_NEXTFITPAGE_HPP_

#include <atomic>
#include <cstdint>
#include <vector>

#include "Constants.hpp"
#include "AnyPage.hpp"
#include "AtomicStack.hpp"
#include "Cell.hpp"
#include "GCStatistics.hpp"
#include "AllocationSize.hpp"
#include "CustomLogging.hpp"
#include "CustomFinalizerProcessor.hpp"

namespace kotlin::alloc {

class alignas(kPageAlignment) NextFitPage : public MultiObjectPage<NextFitPage> {
public:
    static inline constexpr const size_t SIZE = 256 * KiB;

    static inline constexpr int cellCount() { return AllocationSize::bytesExactly(SIZE - sizeof(NextFitPage)).inCells(); }

    static inline constexpr int maxBlockSize() { return cellCount() - 2; }

    static NextFitPage* Create(uint32_t cellCount) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page is big enough
    uint8_t* TryAllocate(uint32_t blockSize) noexcept;

    template <typename SweepTraits>
    bool Sweep(typename SweepTraits::GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
        CustomAllocDebug("NextFitPage@%p::Sweep()", this);
        if (SweepTraits::kCanSkipCleanOldPages && gc::sweepSkipsCleanOldPages() && !allocatedSinceSweep_ && lastSweepSkippable_) {
            // Clean-old page during an Eden collection: see FixedBlockPage::Sweep for the argument.
            sweepHandle.addKeptObjects(lastKeptCount_, lastKeptSizeBytes_);
            return lastKeptCount_ > 0;
        }
        const uint64_t keptCountBefore = sweepHandle.keptCountSoFar();
        const size_t keptBytesBefore = sweepHandle.keptSizeBytesSoFar();
        const uint64_t markedBefore = sweepHandle.markedCountSoFar();

        Cell* end = cells_ + NextFitPage::cellCount();
        std::size_t aliveBytes = 0;
        for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
            if (block->isAllocated_) {
                if (!SweepTraits::trySweepElement(block->data_, finalizerQueue, sweepHandle)) {
                    aliveBytes += AllocationSize::cells(block->size_).inBytes();
                } else {
                    block->Deallocate();
                }
            }
        }
        Cell* maxBlock = cells_; // size 0 block
        for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
            if (block->isAllocated_) continue;
            for (auto* next = block->Next(); next != end; next = block->Next()) {
                if (next->isAllocated_) {
                    break;
                }
                block->size_ += next->size_;
                memset(next, 0, sizeof(*next));
            }
            if (block->size_ > maxBlock->size_) maxBlock = block;
        }
        curBlock_ = maxBlock;

        RuntimeAssert(
                aliveBytes == GetAllocatedSizeBytes(), "Sweep counted %zu alive bytes, while GetAllocatedSizeBytes() returns %zu",
                aliveBytes, GetAllocatedSizeBytes());
        allocatedSizeTracker_.afterSweep(aliveBytes);

        // Cache this page's kept contribution for a later Eden clean-old-page skip (see FixedBlockPage).
        lastKeptCount_ = sweepHandle.keptCountSoFar() - keptCountBefore;
        lastKeptSizeBytes_ = sweepHandle.keptSizeBytesSoFar() - keptBytesBefore;
        lastSweepSkippable_ = (sweepHandle.markedCountSoFar() - markedBefore) == 0;
        allocatedSinceSweep_ = false;

        return aliveBytes > 0;
    }

    template <typename F>
    void TraverseAllocatedBlocks(F process) noexcept(noexcept(process(std::declval<uint8_t*>()))) {
        Cell* end = cells_ + cellCount();
        for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
            if (block->isAllocated_) {
                process(block->data_);
            }
        }
    }

    // End address of this page (exclusive). Used to build the interior-pointer index.
    uint8_t* pageEnd() noexcept { return reinterpret_cast<uint8_t*>(this) + SIZE; }

    // Maps an interior pointer that lies within this page (e.g. a reference-field slot) to the heap
    // object containing it, or nullptr if it does not fall inside an allocated block. Walks this page's
    // cells, so O(cells-in-page). Valid only while the world is stopped. See HeapLayoutSnapshot.
    ObjHeader* objectContainingInteriorPointer(void* interiorPointer) noexcept;

    // Testing method
    bool CheckInvariants() noexcept;

    // Testing method
    std::vector<uint8_t*> GetAllocatedBlocks() noexcept;

private:
    explicit NextFitPage(uint32_t cellCount) noexcept;

    // Looks for a block big enough to hold cellsNeeded. If none big enough is
    // found, update to the largest one.
    void UpdateCurBlock(uint32_t cellsNeeded) noexcept;

    std::size_t GetAllocatedSizeBytes() noexcept;

    Cell* curBlock_;
    // Generational (gms) Eden clean-old-page skip state (see FixedBlockPage). Ignored by
    // non-generational collectors (gc::sweepSkipsCleanOldPages() is always false for them).
    bool allocatedSinceSweep_ = true;
    bool lastSweepSkippable_ = false;
    uint64_t lastKeptCount_ = 0;
    size_t lastKeptSizeBytes_ = 0;
    Cell cells_[]; // cells_[0] is reserved for an empty block
};

// ensure const-evaluatable
static_assert(NextFitPage::cellCount() > 0);

} // namespace kotlin::alloc

#endif
