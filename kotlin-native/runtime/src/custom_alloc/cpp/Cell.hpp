/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_CELL_HPP_
#define CUSTOM_ALLOC_CPP_CELL_HPP_

#include <cstdint>
#include <cstring>

namespace kotlin::alloc {

// All allocations are whole units of cells.
class Cell {
public:
    explicit Cell(uint32_t size) noexcept;

    // Allocate `cellsNeeded` blocks at the end of this block, possibly the
    // whole block, or null if it doesn't fit.
    uint8_t* TryAllocate(uint32_t cellsNeeded) noexcept;

    // Marks block as no longer allocated.
    void Deallocate() noexcept;

    // The next block.
    Cell* Next() noexcept;

private:
    friend class NextFitPage;

    uint32_t isAllocated_;
    uint32_t size_;
    // The allocated data follows immediately after the header block
    uint8_t data_[];
};

} // namespace kotlin::alloc

#endif
