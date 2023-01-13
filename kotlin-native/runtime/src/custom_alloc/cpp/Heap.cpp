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
#include "CustomLogging.hpp"
#include "LargePage.hpp"
#include "MediumPage.hpp"
#include "SmallPage.hpp"
#include "ThreadRegistry.hpp"
#include "GCImpl.hpp"

namespace kotlin::alloc {

void Heap::PrepareForGC() noexcept {
    CustomAllocDebug("Heap::PrepareForGC()");
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        thread.gc().impl().alloc().PrepareForGC();
    }

    mediumPages_.PrepareForGC();
    largePages_.PrepareForGC();
    for (size_t blockSize = 0; blockSize <= SMALL_PAGE_MAX_BLOCK_SIZE; ++blockSize) {
        smallPages_[blockSize].PrepareForGC();
    }
}

void Heap::Sweep() noexcept {
    CustomAllocDebug("Heap::Sweep()");
    for (size_t blockSize = 0; blockSize <= SMALL_PAGE_MAX_BLOCK_SIZE; ++blockSize) {
        smallPages_[blockSize].Sweep();
    }
    mediumPages_.Sweep();
    largePages_.SweepAndFree();
}

MediumPage* Heap::GetMediumPage(uint32_t cellCount) noexcept {
    CustomAllocDebug("Heap::GetMediumPage()");
    return mediumPages_.GetPage(cellCount);
}

SmallPage* Heap::GetSmallPage(uint32_t cellCount) noexcept {
    CustomAllocDebug("Heap::GetSmallPage()");
    return smallPages_[cellCount].GetPage(cellCount);
}

LargePage* Heap::GetLargePage(uint64_t cellCount) noexcept {
    CustomAllocInfo("CustomAllocator::AllocateInLargePage(%" PRIu64 ")", cellCount);
    return largePages_.NewPage(cellCount);
}

} // namespace kotlin::alloc
