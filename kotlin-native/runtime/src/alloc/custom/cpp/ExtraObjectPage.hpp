/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_EXTRA_OBJECTPAGE_HPP_
#define CUSTOM_ALLOC_CPP_EXTRA_OBJECTPAGE_HPP_

#include <atomic>
#include <cstddef>
#include <cstdint>

#include "Constants.hpp"
#include "AnyPage.hpp"
#include "AtomicStack.hpp"
#include "CustomFinalizerProcessor.hpp"
#include "ExtraObjectCell.hpp"
#include "ExtraObjectData.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

class alignas(kPageAlignment) ExtraObjectPage : public MultiObjectPage<ExtraObjectPage> {
public:
    static inline constexpr const size_t SIZE = 64 * KiB;

    static inline constexpr int extraObjectCount() {
        return (SIZE - sizeof(ExtraObjectPage)) / sizeof(ExtraObjectCell);
    }

    using GCSweepScope = gc::GCHandle::GCSweepExtraObjectsScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweepExtraObjects(); }

    static ExtraObjectPage* Create(uint32_t ignored) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page
    mm::ExtraObjectData* TryAllocate() noexcept;

    bool Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept;

    template <typename F>
    void TraverseAllocatedObjects(F process) noexcept(noexcept(process(std::declval<kotlin::mm::ExtraObjectData*>()))) {
        ExtraObjectCell* end = cells_ + extraObjectCount();
        std::atomic<ExtraObjectCell*>* nextFree = &nextFree_;
        for (ExtraObjectCell* cell = cells_; cell < end; ++cell) {
            // If the current cell is free, move on.
            if (cell == nextFree->load(std::memory_order_relaxed)) {
                nextFree = &cell->next_;
                continue;
            }
            process(cell->Data());
        }
    }

private:
    ExtraObjectPage() noexcept;

    std::atomic<ExtraObjectCell*> nextFree_;

    ExtraObjectCell cells_[];
};

} // namespace kotlin::alloc

#endif
