/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_FIXEDBLOCKPAGE_HPP_
#define CUSTOM_ALLOC_CPP_FIXEDBLOCKPAGE_HPP_

#include <atomic>
#include <cstdint>
#include <vector>

#include "AtomicStack.hpp"
#include "ExtraObjectPage.hpp"
#include "GCStatistics.hpp"
#include "AnyPage.hpp"

namespace kotlin::alloc {

struct alignas(8) FixedCellRange {
    uint32_t first;
    uint32_t last;
};

struct alignas(8) FixedBlockCell {
    // The FixedBlockCell either contains data or a pointer to the next free cell
    union {
        uint8_t data[];
        FixedCellRange nextFree;
    };
};

class alignas(kPageAlignment) FixedBlockPage : public AnyPage<FixedBlockPage> {
public:
    using GCSweepScope = gc::GCHandle::GCSweepScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweep(); }

    static FixedBlockPage* Create(uint32_t blockSize) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page
    uint8_t* TryAllocate() noexcept;

    bool Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept;
    void Recycle() noexcept;

    // Testing method
    std::vector<uint8_t*> GetAllocatedBlocks() noexcept;

    static FixedBlockPage& containing(ObjHeader*) noexcept;

    auto escapedCount() const noexcept { return escapedCount_; }
    auto diedCount() const noexcept { return diedCount_; }
    auto capacity() const noexcept { return end_ / blockSize_; }

    void contEscaped() noexcept { ++escapedCount_; }
    void countDied() noexcept { ++diedCount_; }

private:
    explicit FixedBlockPage(uint32_t blockSize) noexcept;

    FixedCellRange nextFree_;

    uint32_t blockSize_;
    uint32_t end_;

    uint32_t escapedCount_ = 0;
    uint32_t diedCount_ = 0;

    FixedBlockCell cells_[];
};

} // namespace kotlin::alloc

#endif
