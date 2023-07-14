/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_EXTRA_OBJECTPAGE_HPP_
#define CUSTOM_ALLOC_CPP_EXTRA_OBJECTPAGE_HPP_

#include <atomic>
#include <cstddef>
#include <cstdint>

#include "AtomicStack.hpp"
#include "ExtraObjectData.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

struct ExtraObjectCell {
    mm::ExtraObjectData* Data() { return reinterpret_cast<mm::ExtraObjectData*>(data_); }

    // This is used to simultaneously build two lists: a free list and a finalizers queue.
    // A cell cannot exist in both of them, but can be in neither when it's alive.
    ExtraObjectCell* next_;
    struct alignas(mm::ExtraObjectData) {
        uint8_t data_[sizeof(mm::ExtraObjectData)];
    };

    static ExtraObjectCell* fromExtraObject(mm::ExtraObjectData* extraObjectData) {
        return reinterpret_cast<ExtraObjectCell*>(reinterpret_cast<uint8_t*>(extraObjectData) - offsetof(ExtraObjectCell, data_));
    }
};

using FinalizerQueue = AtomicStack<ExtraObjectCell>;

class alignas(8) ExtraObjectPage {
public:
    using GCSweepScope = gc::GCHandle::GCSweepExtraObjectsScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweepExtraObjects(); }

    static ExtraObjectPage* Create(uint32_t ignored) noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page
    mm::ExtraObjectData* TryAllocate() noexcept;

    bool Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept;

//    class iterator {
//    public:
//        explicit iterator(ExtraObjectPage& page, ExtraObjectCell* cell)
//            : cell_(cell)
//            , end_(page.cells_ + EXTRA_OBJECT_COUNT)
//            , nextFree_(&page.nextFree_) {
//            skipFree();
//        }
//
//        mm::ExtraObjectData& operator*() noexcept { return *obj(); }
//        mm::ExtraObjectData* operator->() noexcept { return obj(); }
//
//        iterator& operator++() noexcept {
//            ++cell_;
//            skipFree();
//            return *this;
//        }
//        iterator operator++(int) noexcept {
//            auto result = *this;
//            ++(*this);
//            return result;
//        }
//
//        bool operator==(const iterator& rhs) const noexcept { return cell_ == rhs.cell_; }
//        bool operator!=(const iterator& rhs) const noexcept { return !(*this == rhs); }
//    private:
//        void skipFree() {
//            while (cell_ < end_ && cell_ == *nextFree_) {
//                nextFree_ = &cell_->next_;
//                ++cell_;
//            }
//        }
//        mm::ExtraObjectData* obj() const {
//            return cell_->Data();
//        }
//        ExtraObjectCell* cell_;
//        ExtraObjectCell* end_;
//        ExtraObjectCell** nextFree_;
//    };
//
//    iterator begin() { return iterator(*this, cells_); }
//    iterator end() { return iterator(*this, cells_ + EXTRA_OBJECT_COUNT); }

private:
    friend class AtomicStack<ExtraObjectPage>;

    ExtraObjectPage() noexcept;

    // Used for linking pages together in `pages` queue or in `unswept` queue.
    ExtraObjectPage* next_;
    ExtraObjectCell* nextFree_;
    ExtraObjectCell cells_[];
};

} // namespace kotlin::alloc

#endif
