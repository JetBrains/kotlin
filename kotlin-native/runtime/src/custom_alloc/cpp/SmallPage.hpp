/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_SMALLPAGE_HPP_
#define CUSTOM_ALLOC_CPP_SMALLPAGE_HPP_

#include <atomic>
#include <cstdint>

#include "AtomicStack.hpp"

namespace kotlin::alloc {

struct alignas(8) SmallCell {
    // The SmallCell either contains data or a pointer to the next free cell
    union {
        uint8_t data[];
        SmallCell* nextFree;
    };
};

class alignas(8) SmallPage {
public:
    static SmallPage* Create(uint32_t blockSize) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page
    uint8_t* TryAllocate() noexcept;

    bool Sweep() noexcept;

private:
    friend class AtomicStack<SmallPage>;

    explicit SmallPage(uint32_t blockSize) noexcept;

    // Used for linking pages together in `pages` queue or in `unswept` queue.
    SmallPage* next_;
    uint32_t blockSize_;
    SmallCell* nextFree_;
    SmallCell cells_[];
};

} // namespace kotlin::alloc

#endif
