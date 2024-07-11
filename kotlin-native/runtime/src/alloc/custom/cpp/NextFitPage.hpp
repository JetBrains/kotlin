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
#include "ExtraObjectPage.hpp"
#include "GCStatistics.hpp"
#include "AllocationSize.hpp"

namespace kotlin::alloc {

class alignas(kPageAlignment) NextFitPage : public MultiObjectPage<NextFitPage> {
public:
    static inline constexpr const size_t SIZE = 256 * KiB;

    static inline constexpr int cellCount() {
        return AllocationSize::bytesExactly(SIZE - sizeof(NextFitPage)).inCells();
    }

    static inline constexpr int maxBlockSize() {
        return cellCount() - 2;
    }

    using GCSweepScope = gc::GCHandle::GCSweepScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweep(); }

    static NextFitPage* Create(uint32_t cellCount) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page is big enough
    uint8_t* TryAllocate(uint32_t blockSize) noexcept;

    bool Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept;

    // TODO: Do we need this, or should we implement Dump on top of GetAllocatedBlocks()?
    template <typename F>
    void TraverseAllocatedBlocks(F process) noexcept(noexcept(process(std::declval<uint8_t*>()))) {
        Cell* end = cells_ + cellCount();
        for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
            if (block->isAllocated_) {
                process(block->data_);
            }
        }
    }

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
    Cell cells_[]; // cells_[0] is reserved for an empty block
};

} // namespace kotlin::alloc

#endif
