/*
* Copyright 2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#pragma once

#include <cinttypes>

#include "Cell.hpp"
#include "KAssert.h"

namespace kotlin::alloc {

// Eventually, this type should be used as the only type to represent allocation size both in bytes, in cells and other possible units.
class AllocationSize {
    // Make sure, that the AllocationSize is able to represent all the sizes imaginable on 32bit platforms,
    // so that the allocation could gracefully fail later in `SafeAlloc` and not just silently allocate an overflown value.
    // See KT-54659.
    using ValueType = uint64_t;

    static constexpr size_t kCellSize = sizeof(Cell);

    explicit constexpr AllocationSize(ValueType cells) : cells_(cells) {}

public:
    static constexpr AllocationSize cells(uint64_t cells) noexcept {
        return AllocationSize{cells};
    }
    static constexpr AllocationSize bytesAtLeast(uint64_t bytes) noexcept {
        auto cellCount = (bytes + kCellSize - 1) / kCellSize;
        return cells(cellCount);
    }
    static constexpr AllocationSize bytesExactly(uint64_t bytes) {
        AllocationSize atLeast = bytesAtLeast(bytes);
        if (atLeast.inBytes() != bytes) {
            RuntimeFail("The allocations size %" PRIu64 " must be a multiple of Cell size", bytes);
        }
        return atLeast;
    }

    constexpr AllocationSize() noexcept : cells_(0) {}
    constexpr AllocationSize(const AllocationSize& other) noexcept = default;
    constexpr AllocationSize& operator=(const AllocationSize& other) noexcept = default;

    constexpr uint64_t inCells() const noexcept { return cells_; }
    constexpr uint64_t inBytes() const noexcept { return inCells() * kCellSize; }

    constexpr bool operator==(const AllocationSize& other) const noexcept { return cells_ == other.cells_; }
    constexpr bool operator!=(const AllocationSize& other) const noexcept { return !(*this == other); }

    constexpr bool operator<(const AllocationSize& other) const noexcept { return cells_ < other.cells_; }
    constexpr bool operator>(const AllocationSize& other) const noexcept { return cells_ > other.cells_; }
    constexpr bool operator<=(const AllocationSize& other) const noexcept { return cells_ <= other.cells_; }
    constexpr bool operator>=(const AllocationSize& other) const noexcept { return cells_ >= other.cells_; }

    constexpr AllocationSize& operator+=(const AllocationSize& other) noexcept {
        cells_ += other.cells_;
        return *this;
    }
    constexpr AllocationSize operator+(const AllocationSize& other) const noexcept { return AllocationSize{*this} += other; }


    AllocationSize& operator-=(const AllocationSize& other) noexcept {
        RuntimeAssert(cells_ >= other.cells_, "Subtraction would cause a negative value");
        cells_ -= other.cells_;
        return *this;
    }
    AllocationSize operator-(const AllocationSize& other) const noexcept { return AllocationSize{*this} -= other; }

    constexpr AllocationSize& operator*=(uint32_t multiplier) noexcept {
        cells_ *= multiplier;
        return *this;
    }
    constexpr AllocationSize operator*(uint32_t multiplier) const noexcept { return AllocationSize{*this} *= multiplier; }

private:
    ValueType cells_ = 0;
};


static_assert(AllocationSize::cells(0) == AllocationSize::bytesAtLeast(0));
static_assert(AllocationSize::cells(0).inBytes() == 0);

static_assert(AllocationSize::cells(37).inCells() == 37);
static_assert(AllocationSize::cells(38).inCells() == 38);

static_assert(AllocationSize::bytesAtLeast(37 * sizeof(Cell) + 0).inBytes() == 37 * sizeof(Cell));
static_assert(AllocationSize::bytesAtLeast(37 * sizeof(Cell) + 1).inBytes() == 38 * sizeof(Cell));

static_assert(AllocationSize::bytesExactly(37 * sizeof(Cell)).inBytes() == 37 * sizeof(Cell));

static_assert(AllocationSize::cells(3) + AllocationSize::cells(7) == AllocationSize::cells(10));
static_assert(AllocationSize::cells(3) * 7 == AllocationSize::cells(21));

}