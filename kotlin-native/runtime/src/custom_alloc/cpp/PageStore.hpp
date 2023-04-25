/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_PAGESTORE_HPP_
#define CUSTOM_ALLOC_CPP_PAGESTORE_HPP_

#include <atomic>

#include "AtomicStack.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

template <class T>
class PageStore {
public:
    void PrepareForGC() noexcept {
        unswept_.TransferAllFrom(std::move(ready_));
        unswept_.TransferAllFrom(std::move(used_));
        T* page;
        while ((page = empty_.Pop())) page->Destroy();
    }

    void Sweep(gc::GCHandle::GCSweepScope& sweepHandle) noexcept {
        while (SweepSingle(sweepHandle, unswept_.Pop(), unswept_, ready_)) {
        }
    }

    void SweepAndFree(gc::GCHandle::GCSweepScope& sweepHandle) noexcept {
        T* page;
        while ((page = unswept_.Pop())) {
            if (page->Sweep(sweepHandle)) {
                ready_.Push(page);
            } else {
                page->Destroy();
            }
        }
    }

    T* GetPage(uint32_t cellCount) noexcept {
        T* page;
        if ((page = unswept_.Pop())) {
            // If there're unswept_ pages, the GC is in progress.
            auto sweepHandle = gc::GCHandle::currentEpoch()->sweep();
            if ((page = SweepSingle(sweepHandle, page, unswept_, used_))) {
                return page;
            }
        }
        if ((page = ready_.Pop())) {
            used_.Push(page);
            return page;
        }
        if ((page = empty_.Pop())) {
            used_.Push(page);
            return page;
        }
        return NewPage(cellCount);
    }

    T* NewPage(uint64_t cellCount) noexcept {
        T* page = T::Create(cellCount);
        used_.Push(page);
        return page;
    }

    ~PageStore() noexcept {
        T* page;
        while ((page = empty_.Pop())) page->Destroy();
        while ((page = ready_.Pop())) page->Destroy();
        while ((page = used_.Pop())) page->Destroy();
        while ((page = unswept_.Pop())) page->Destroy();
    }

private:
    T* SweepSingle(gc::GCHandle::GCSweepScope& sweepHandle, T* page, AtomicStack<T>& from, AtomicStack<T>& to) noexcept {
        if (!page) {
            return nullptr;
        }
        do {
            if (page->Sweep(sweepHandle)) {
                to.Push(page);
                return page;
            }
            empty_.Push(page);
        } while ((page = from.Pop()));
        return nullptr;
    }

    AtomicStack<T> empty_;
    AtomicStack<T> ready_;
    AtomicStack<T> used_;
    AtomicStack<T> unswept_;
};

} // namespace kotlin::alloc

#endif
