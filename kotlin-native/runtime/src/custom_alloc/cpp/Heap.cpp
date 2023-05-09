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
    extraObjectPages_.PrepareForGC();
}

FinalizerQueue Heap::Sweep(gc::GCHandle gcHandle) noexcept {
    FinalizerQueue finalizerQueue;
    CustomAllocDebug("Heap::Sweep()");
    {
        auto sweepHandle = gcHandle.sweep();
        for (int blockSize = 0; blockSize <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++blockSize) {
            fixedBlockPages_[blockSize].Sweep(sweepHandle, finalizerQueue);
        }
        nextFitPages_.Sweep(sweepHandle, finalizerQueue);
        singleObjectPages_.SweepAndFree(sweepHandle, finalizerQueue);
    }
    {
        auto sweepHandle = gcHandle.sweepExtraObjects();
        extraObjectPages_.Sweep(sweepHandle, finalizerQueue);
    }
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        finalizerQueue.TransferAllFrom(thread.gc().impl().alloc().ExtractFinalizerQueue());
    }
    CustomAllocDebug("Heap::Sweep done");
    return finalizerQueue;
}

NextFitPage* Heap::GetNextFitPage(uint32_t cellCount, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocDebug("Heap::GetNextFitPage()");
    return nextFitPages_.GetPage(cellCount, finalizerQueue);
}

FixedBlockPage* Heap::GetFixedBlockPage(uint32_t cellCount, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocDebug("Heap::GetFixedBlockPage()");
    return fixedBlockPages_[cellCount].GetPage(cellCount, finalizerQueue);
}

SingleObjectPage* Heap::GetSingleObjectPage(uint64_t cellCount, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocInfo("CustomAllocator::AllocateInSingleObjectPage(%" PRIu64 ")", cellCount);
    return singleObjectPages_.NewPage(cellCount);
}

ExtraObjectPage* Heap::GetExtraObjectPage(FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocInfo("CustomAllocator::GetExtraObjectPage()");
    return extraObjectPages_.GetPage(0, finalizerQueue);
}

} // namespace kotlin::alloc
