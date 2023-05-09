/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Heap.hpp"

#include <atomic>
#include <cstdint>
#include <cstdlib>
#include <cinttypes>
#include <new>

#include "CustomAllocConstants.hpp"
#include "AtomicStack.hpp"
#include "CustomLogging.hpp"
#include "ExtraObjectPage.hpp"
#include "ThreadRegistry.hpp"
#include "GCImpl.hpp"

namespace kotlin::alloc {

Heap::~Heap() noexcept {
    ExtraObjectPage* page;
    while ((page = extraObjectPages_.Pop())) page->Destroy();
    while ((page = usedExtraObjectPages_.Pop())) page->Destroy();
}

void Heap::PrepareForGC() noexcept {
    CustomAllocDebug("Heap::PrepareForGC()");
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        thread.gc().impl().alloc().PrepareForGC();
    }

    nextFitPages_.PrepareForGC();
    singleObjectPages_.PrepareForGC();
    for (int blockSize = 0; blockSize <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++blockSize) {
        fixedBlockPages_[blockSize].PrepareForGC();
    }
    usedExtraObjectPages_.TransferAllFrom(std::move(extraObjectPages_));
}

void Heap::Sweep(gc::GCHandle gcHandle) noexcept {
    auto sweepHandle = gcHandle.sweep();
    CustomAllocDebug("Heap::Sweep()");
    for (int blockSize = 0; blockSize <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++blockSize) {
        fixedBlockPages_[blockSize].Sweep(sweepHandle);
    }
    nextFitPages_.Sweep(sweepHandle);
    singleObjectPages_.SweepAndFree(sweepHandle);
}

AtomicStack<ExtraObjectCell> Heap::SweepExtraObjects(gc::GCHandle gcHandle) noexcept {
    auto sweepHandle = gcHandle.sweepExtraObjects();
    CustomAllocDebug("Heap::SweepExtraObjects()");
    AtomicStack<ExtraObjectCell> finalizerQueue;
    ExtraObjectPage* page;
    while ((page = usedExtraObjectPages_.Pop())) {
        if (!page->Sweep(sweepHandle, finalizerQueue)) {
            CustomAllocInfo("SweepExtraObjects free(%p)", page);
            page->Destroy();
        } else {
            extraObjectPages_.Push(page);
        }
    }
    return finalizerQueue;
}

NextFitPage* Heap::GetNextFitPage(uint32_t cellCount) noexcept {
    CustomAllocDebug("Heap::GetNextFitPage()");
    return nextFitPages_.GetPage(cellCount);
}

FixedBlockPage* Heap::GetFixedBlockPage(uint32_t cellCount) noexcept {
    CustomAllocDebug("Heap::GetFixedBlockPage()");
    return fixedBlockPages_[cellCount].GetPage(cellCount);
}

SingleObjectPage* Heap::GetSingleObjectPage(uint64_t cellCount) noexcept {
    CustomAllocInfo("CustomAllocator::AllocateInSingleObjectPage(%" PRIu64 ")", cellCount);
    return singleObjectPages_.NewPage(cellCount);
}

ExtraObjectPage* Heap::GetExtraObjectPage() noexcept {
    CustomAllocInfo("CustomAllocator::GetExtraObjectPage()");
    ExtraObjectPage* page = extraObjectPages_.Pop();
    if (page == nullptr) {
        page = ExtraObjectPage::Create();
    }
    usedExtraObjectPages_.Push(page);
    return page;
}

} // namespace kotlin::alloc
