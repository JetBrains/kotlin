/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_NEXTFITPAGE_HPP_
#define CUSTOM_ALLOC_CPP_NEXTFITPAGE_HPP_

#include <atomic>
#include <cstdint>

#include "AtomicStack.hpp"
#include "Cell.hpp"
#include "ExtraObjectPage.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

class alignas(8) NextFitPage {
public:
    using GCSweepScope = gc::GCHandle::GCSweepScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweep(); }

    static NextFitPage* Create(uint32_t cellCount) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page is big enough
    uint8_t* TryAllocate(uint32_t blockSize) noexcept;

    bool Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept;

    // Testing method
    bool CheckInvariants() noexcept;

private:
    explicit NextFitPage(uint32_t cellCount) noexcept;

    // Looks for a block big enough to hold cellsNeeded. If none big enough is
    // found, update to the largest one.
    void UpdateCurBlock(uint32_t cellsNeeded) noexcept;

    friend class AtomicStack<NextFitPage>;
    NextFitPage* next_;

    Cell* curBlock_;
    Cell cells_[]; // cells_[0] is reserved for an empty block
};

} // namespace kotlin::alloc

#endif
