/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_FIXEDBLOCKPAGE_HPP_
#define CUSTOM_ALLOC_CPP_FIXEDBLOCKPAGE_HPP_

#include <atomic>
#include <cstdint>

#include "AtomicStack.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

struct alignas(8) FixedBlockCell {
    // The FixedBlockCell either contains data or a pointer to the next free cell
    union {
        uint8_t data[];
        FixedBlockCell* nextFree;
    };
};

class alignas(8) FixedBlockPage {
public:
    static FixedBlockPage* Create(uint32_t blockSize) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page
    uint8_t* TryAllocate() noexcept;

    bool Sweep(gc::GCHandle::GCSweepScope& sweepHandle) noexcept;

private:
    friend class AtomicStack<FixedBlockPage>;

    explicit FixedBlockPage(uint32_t blockSize) noexcept;

    // Used for linking pages together in `pages` queue or in `unswept` queue.
    FixedBlockPage* next_;
    uint32_t blockSize_;
    FixedBlockCell* nextFree_;
    FixedBlockCell cells_[];
};

} // namespace kotlin::alloc

#endif
