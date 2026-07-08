/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_HEAP_HPP_
#define CUSTOM_ALLOC_CPP_HEAP_HPP_

#include <atomic>
#include <mutex>
#include <cstring>

#include "AtomicStack.hpp"
#include "ExtraObjectData.hpp"
#include "GCStatistics.hpp"
#include "Memory.h"
#include "SingleObjectPage.hpp"
#include "NextFitPage.hpp"
#include "PageStore.hpp"
#include "FixedBlockPage.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

class Heap {
public:
    // Called once by the GC thread after all mutators have been suspended
    void PrepareForGC() noexcept;

    // Sweep through all remaining pages, freeing those blocks where CanReclaim
    // returns true. If multiple sweepers are active, each page will only be
    // seen by one sweeper.
    FinalizerQueue Sweep(gc::GCHandle gcHandle) noexcept;

    FixedBlockPage* GetFixedBlockPage(uint32_t cellCount, FinalizerQueue& finalizerQueue) noexcept;
    NextFitPage* GetNextFitPage(uint32_t cellCount, FinalizerQueue& finalizerQueue) noexcept;
    SingleObjectPage* GetSingleObjectPage(uint64_t cellCount) noexcept;
    FixedBlockPage* GetFixedBlockExtraObjectPage(FinalizerQueue& finalizerQueue) noexcept;
    SingleObjectPage* GetSingleExtraObjectPage() noexcept;

    void AddToFinalizerQueue(FinalizerQueue queue) noexcept;
    FinalizerQueue ExtractFinalizerQueue() noexcept;

    // One object-holding page's address range plus a function that resolves an interior pointer within
    // that page to the heap object containing it. `resolve` is a captureless function (not a virtual
    // call) selected by page kind.
    struct PageRange {
        uint8_t* begin;
        uint8_t* end;
        ObjHeader* (*resolve)(void* page, void* interiorPointer) noexcept;
        void* page;
    };

    // Appends the address ranges of every object-holding page (fixed-block, next-fit, single-object;
    // NOT extra-object pages) to `out`. Call only while the world is stopped. Backs HeapLayoutSnapshot,
    // which the generational GC uses to map a remembered-set slot back to its containing object.
    void SnapshotPageRanges(std::vector<PageRange>& out) noexcept;

    // Test method
    std::vector<ObjHeader*> GetAllocatedObjects() noexcept;
    void ClearForTests() noexcept;

    auto& allocatedSizeTracker() noexcept { return allocatedSizeTracker_; }

    template <typename T>
    void TraverseAllocatedObjects(T process) noexcept(noexcept(process(std::declval<ObjHeader*>()))) {
        for (int blockSize = 0; blockSize <= FixedBlockPage::MAX_BLOCK_SIZE; ++blockSize) {
            fixedBlockPages_[blockSize].TraversePages([process](auto* page) {
                page->TraverseAllocatedBlocks([process](auto* block) { process(reinterpret_cast<CustomHeapObject*>(block)->object()); });
            });
        }
        nextFitPages_.TraversePages([process](auto* page) {
            page->TraverseAllocatedBlocks([process](auto* block) { process(reinterpret_cast<CustomHeapObject*>(block)->object()); });
        });
        singleObjectPages_.TraversePages([process](auto* page) {
            page->TraverseAllocatedBlocks([process](auto* block) { process(reinterpret_cast<CustomHeapObject*>(block)->object()); });
        });
    }

    template <typename T>
    void TraverseAllocatedExtraObjects(T process) noexcept(noexcept(process(std::declval<kotlin::mm::ExtraObjectData*>()))) {
        fixedBlockExtraObjectPages_.TraversePages([process](auto* page) {
            page->TraverseAllocatedBlocks([process](uint8_t* block) { process(reinterpret_cast<ExtraObjectCell*>(block)->Data()); });
        });
        singleExtraObjectPages_.TraversePages([process](auto* page) {
            page->TraverseAllocatedBlocks([process](uint8_t* block) { process(reinterpret_cast<ExtraObjectCell*>(block)->Data()); });
        });
    }

private:
    PageStore<FixedBlockPage, ObjectSweepTraits> fixedBlockPages_[FixedBlockPage::MAX_BLOCK_SIZE + 1];
    PageStore<NextFitPage, ObjectSweepTraits> nextFitPages_;
    PageStore<SingleObjectPage, ObjectSweepTraits> singleObjectPages_;
    PageStore<FixedBlockPage, ExtraDataSweepTraits> fixedBlockExtraObjectPages_;
    PageStore<SingleObjectPage, ExtraDataSweepTraits> singleExtraObjectPages_;

    FinalizerQueue pendingFinalizerQueue_;
    std::mutex pendingFinalizerQueueMutex_;

    std::atomic<std::size_t> concurrentSweepersCount_ = 0;

    AllocatedSizeTracker::Heap allocatedSizeTracker_{};
};

} // namespace kotlin::alloc

#endif
