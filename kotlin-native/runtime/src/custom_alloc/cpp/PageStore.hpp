/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_PAGESTORE_HPP_
#define CUSTOM_ALLOC_CPP_PAGESTORE_HPP_

#include <atomic>

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

    void SweepAndFree(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
        T* page;
        while ((page = unswept_.Pop())) {
            if (page->Sweep(sweepHandle, finalizerQueue)) {
                ready_.Push(page);
            } else {
                page->Destroy();
            }
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

    ~PageStore() noexcept {
        T* page;
        while ((page = empty_.Pop())) page->Destroy();
        while ((page = ready_.Pop())) page->Destroy();
        while ((page = used_.Pop())) page->Destroy();
        while ((page = unswept_.Pop())) page->Destroy();
    }

//    class iterator {
//    public:
//        iterator(PageStore<T>& owner, std::size_t startStack) : curStack_(startStack) {
//            stackIterators_[0] = owner.empty_.begin();
//            stackIterators_[1] = owner.ready_.begin();
//            stackIterators_[2] = owner.used_.begin();
//            stackIterators_[3] = owner.unswept_.begin();
//
//            stackBoundaries_[0] = owner.empty_.end();
//            stackBoundaries_[1] = owner.ready_.end();
//            stackBoundaries_[2] = owner.used_.end();
//            stackBoundaries_[3] = owner.unswept_.end();
//        }
//        explicit iterator(PageStore<T>& owner) : iterator(owner, 4) {}
//
//        HeapObjHeader& operator*() noexcept { return *cur(); }
//        HeapObjHeader* operator->() noexcept { return cur(); }
//
//        iterator& operator++() noexcept {
//            auto& updatedIter = ++stackIterators_[curStack_];
//            if (updatedIter == stackBoundaries_[curStack_]) {
//                ++curStack_;
//            }
//            return *this;
//        }
//        iterator operator++(int) noexcept {
//            auto result = *this;
//            ++(*this);
//            return result;
//        }
//
//        bool operator==(const iterator& rhs) const noexcept {
//            if (curStack_ != rhs.curStack_) return false;
//            if (curStack_ > 3) return true;
//            return stackIterators_[curStack_] == rhs.stackIterators_[curStack_];
//        }
//        bool operator!=(const iterator& rhs) const noexcept { return !(*this == rhs); }
//    private:
//        HeapObjHeader* cur() noexcept {
//            return **(stackIterators_[curStack_]);
//        }
//
//        typename AtomicStack<T>::iterator stackIterators_[4];
//        typename AtomicStack<T>::iterator stackBoundaries_[4];
//        std::size_t curStack_ = 0;
//    };
//
//    iterator begin() { return iterator(*this, 0); }
//    iterator end() { return iterator(*this, 4); }

    template<typename Fun>
    void forEach(Fun fun) {
        for (auto& emptyPage: empty_) {
            for (auto& obj: emptyPage) {
                fun(obj);
            }
        }
        for (auto& readyPage: ready_) {
            for (auto& obj: readyPage) {
                fun(obj);
            }
        }
        for (auto& usedPage: used_) {
            for (auto& obj: usedPage) {
                fun(obj);
            }
        }
        for (auto& unsweptPage: unswept_) {
            for (auto& obj: unsweptPage) {
                fun(obj);
            }
        }
    }

    void graphviz(std::ostream& out) {
        for (auto& emptyPage: empty_) {
            emptyPage.graphviz(out, "Empty");
        }
        for (auto& readyPage: ready_) {
            readyPage.graphviz(out, "Ready");
        }
        for (auto& usedPage: used_) {
            usedPage.graphviz(out, "Used");
        }
        for (auto& unsweptPage: unswept_) {
            unsweptPage.graphviz(out, "Unswept");
        }
    }

private:
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

    AtomicStack<T> empty_;
    AtomicStack<T> ready_;
    AtomicStack<T> used_;
    AtomicStack<T> unswept_;
};

} // namespace kotlin::alloc

#endif
