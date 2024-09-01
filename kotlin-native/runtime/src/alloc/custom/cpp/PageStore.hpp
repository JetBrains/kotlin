/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_PAGESTORE_HPP_
#define CUSTOM_ALLOC_CPP_PAGESTORE_HPP_

#include <atomic>
#include <cstdint>
#include <vector>

#include "AtomicStack.hpp"
#include "ExtraObjectPage.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

template <class T>
class PageStore {
public:
    using GCSweepScope = typename T::GCSweepScope;

    void PrepareForGC() noexcept {
        unswept_.TransferAllFrom(std::move(ready_));
        unswept_.TransferAllFrom(std::move(used_));
        T* page;
        while ((page = empty_.Pop())) page->Destroy();
    }

    void Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
        while (SweepSingle(sweepHandle, unswept_.Pop(), unswept_, ready_, finalizerQueue)) {
        }
    }

    T* GetPage(uint32_t cellCount, FinalizerQueue& finalizerQueue, std::atomic<std::size_t>& concurrentSweepersCount_) noexcept {
        T* page;
        if ((page = ready_.Pop())) {
            used_.Push(page);
            return page;
        }
        {
            auto handle = gc::GCHandle::currentEpoch();
            ScopeGuard counterGuard(
                    [&]() { ++concurrentSweepersCount_; },
                    [&]() { --concurrentSweepersCount_; }
            );

            if ((page = unswept_.Pop())) {
                // If there're unswept_ pages, the GC is in progress.
                GCSweepScope sweepHandle = T::currentGCSweepScope(*handle);
                if ((page = SweepSingle(sweepHandle, page, unswept_, used_, finalizerQueue))) {
                    return page;
                }
            }
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

    bool isEmpty() noexcept {
        return empty_.isEmpty() &&
            ready_.isEmpty() &&
            used_.isEmpty() &&
            unswept_.isEmpty();
    }

    ~PageStore() noexcept {
        Clear();
    }

private:
    friend class Heap;

    T* SweepSingle(GCSweepScope& sweepHandle, T* page, AtomicStack<T>& from, AtomicStack<T>& to, FinalizerQueue& finalizerQueue) noexcept {
        if (!page) {
            return nullptr;
        }
        do {
            if (page->Sweep(sweepHandle, finalizerQueue)) {
                to.Push(page);
                return page;
            }
            empty_.Push(page);
        } while ((page = from.Pop()));
        return nullptr;
    }

    // Testing method
    std::vector<T*> GetPages() noexcept {
        std::vector<T*> pages;
        for (T* page : ready_.GetElements()) pages.push_back(page);
        for (T* page : used_.GetElements()) pages.push_back(page);
        for (T* page : unswept_.GetElements()) pages.push_back(page);
        return pages;
    }

    void Clear() noexcept {
        while (T* page = empty_.Pop()) page->Destroy();
        while (T* page = ready_.Pop()) page->Destroy();
        while (T* page = used_.Pop()) page->Destroy();
        while (T* page = unswept_.Pop()) page->Destroy();
    }

    template <typename F>
    void TraversePages(F process) noexcept(noexcept(process(std::declval<T*>()))) {
        empty_.TraverseElements(process);
        ready_.TraverseElements(process);
        used_.TraverseElements(process);
        unswept_.TraverseElements(process);
    }

    AtomicStack<T> empty_;
    AtomicStack<T> ready_;
    AtomicStack<T> used_;
    AtomicStack<T> unswept_;
};

template <>
class PageStore<SingleObjectPage> {
public:
    using GCSweepScope = typename SingleObjectPage::GCSweepScope;

    void PrepareForGC() noexcept {
        unswept_.TransferAllFrom(std::move(used_));
    }

    void Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
        while (auto* page = unswept_.Pop()) {
            if (page->SweepAndDestroy(sweepHandle, finalizerQueue)) {
                used_.Push(page);
            }
        }
    }

    SingleObjectPage* NewPage(uint64_t cellCount) noexcept {
        auto* page = SingleObjectPage::Create(cellCount);
        used_.Push(page);
        return page;
    }

    bool isEmpty() noexcept {
        return used_.isEmpty() && unswept_.isEmpty();
    }

    ~PageStore() noexcept {
        Clear();
    }

private:
    friend class Heap;

    // Testing method
    std::vector<SingleObjectPage*> GetPages() noexcept {
        std::vector<SingleObjectPage*> pages;
        for (auto* page : used_.GetElements()) pages.push_back(page);
        for (auto* page : unswept_.GetElements()) pages.push_back(page);
        return pages;
    }

    void Clear() noexcept {
        while (auto* page = used_.Pop()) page->Destroy();
        while (auto* page = unswept_.Pop()) page->Destroy();
    }

    template <typename F>
    void TraversePages(F process) noexcept(noexcept(process(std::declval<SingleObjectPage*>()))) {
        used_.TraverseElements(process);
        unswept_.TraverseElements(process);
    }

    AtomicStack<SingleObjectPage> used_;
    AtomicStack<SingleObjectPage> unswept_;
};

} // namespace kotlin::alloc

#endif
