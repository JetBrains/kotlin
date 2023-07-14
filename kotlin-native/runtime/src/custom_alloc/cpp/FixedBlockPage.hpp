/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_FIXEDBLOCKPAGE_HPP_
#define CUSTOM_ALLOC_CPP_FIXEDBLOCKPAGE_HPP_

#include <atomic>
#include <cstdint>

#include "AtomicStack.hpp"
#include "ExtraObjectPage.hpp"
#include "GCStatistics.hpp"

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

class alignas(8) FixedBlockPage {
public:
    using GCSweepScope = gc::GCHandle::GCSweepScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweep(); }

    static FixedBlockPage* Create(uint32_t blockSize) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page
    uint8_t* TryAllocate() noexcept;

    bool Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept;

    class iterator {
    public:
        iterator(FixedBlockPage& page, uint32_t cell) : page_(page), cell_(cell) {
            nextFree_ = page_.nextFree_;
            // FIXME fight copy&paste
            if (cell_ >= nextFree_.first) {
                cell_ = nextFree_.last;
                if (cell_ < page_.end_) {
                    nextFree_ = page_.cells_[cell_].nextFree;
                    cell_ += page_.blockSize_;
                }
            }
        }
        explicit iterator(FixedBlockPage& page) : iterator(page, 0) {}

        uint8_t& operator*() noexcept { return *page_.cells_[cell_].data; }
        uint8_t* operator->() noexcept { return page_.cells_[cell_].data; }

        iterator& operator++() noexcept {
            cell_ += page_.blockSize_;
            if (cell_ < nextFree_.first) {
            } else {
                cell_ = nextFree_.last;
                if (cell_ < page_.end_) {
                    nextFree_ = page_.cells_[cell_].nextFree;
                    cell_ += page_.blockSize_;
                }
            }
            return *this;
        }
        iterator operator++(int) noexcept {
            auto result = *this;
            ++(*this);
            return result;
        }

        bool operator==(const iterator& rhs) const noexcept { return &page_ == &rhs.page_ && cell_ == rhs.cell_; }
        bool operator!=(const iterator& rhs) const noexcept { return !(*this == rhs); }
    private:
        FixedBlockPage& page_;
        FixedCellRange nextFree_;
        uint32_t cell_;
    };

    iterator begin() { return iterator(*this); }
    iterator end() { return iterator(*this, end_); }

    void graphviz(std::ostream& out, std::string_view stackName);

private:
    explicit FixedBlockPage(uint32_t blockSize) noexcept;

    friend class AtomicStack<FixedBlockPage>;

    // Used for linking pages together in `pages` queue or in `unswept` queue.
    FixedBlockPage* next_;
    FixedCellRange nextFree_;
    uint32_t blockSize_;
    uint32_t end_;
    FixedBlockCell cells_[];
};

} // namespace kotlin::alloc

#endif
