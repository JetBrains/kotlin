/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectPage.hpp"

#include <atomic>
#include <cstdint>
#include <cstring>
#include <random>

#include "AtomicStack.hpp"
#include "CustomAllocConstants.hpp"
#include "CustomLogging.hpp"
#include "ExtraObjectData.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

ExtraObjectPage* ExtraObjectPage::Create(uint32_t ignored) noexcept {
    CustomAllocInfo("ExtraObjectPage::Create()");
    return new (SafeAlloc(EXTRA_OBJECT_PAGE_SIZE)) ExtraObjectPage();
}

ExtraObjectPage::ExtraObjectPage() noexcept {
    CustomAllocInfo("ExtraObjectPage(%p)::ExtraObjectPage()", this);
    nextFree_.store(cells_, std::memory_order_relaxed);
    ExtraObjectCell* end = cells_ + EXTRA_OBJECT_COUNT;
    for (ExtraObjectCell* cell = cells_; cell < end; cell = cell->next_.load(std::memory_order_relaxed)) {
        cell->next_.store(cell + 1, std::memory_order_relaxed);
    }
}

void ExtraObjectPage::Destroy() noexcept {
    Free(this, EXTRA_OBJECT_PAGE_SIZE);
}

mm::ExtraObjectData* ExtraObjectPage::TryAllocate() noexcept {
    auto* next = nextFree_.load(std::memory_order_relaxed);
    if (next >= cells_ + EXTRA_OBJECT_COUNT) {
        return nullptr;
    }
    ExtraObjectCell* freeBlock = next;
    nextFree_.store(freeBlock->next_.load(std::memory_order_relaxed), std::memory_order_relaxed);
    CustomAllocDebug("ExtraObjectPage(%p)::TryAllocate() = %p", this, freeBlock->Data());
    return freeBlock->Data();
}

bool ExtraObjectPage::Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocInfo("ExtraObjectPage(%p)::Sweep()", this);
    // `end` is after the last legal allocation of a block, but does not
    // necessarily match an actual block starting point.
    ExtraObjectCell* end = cells_ + EXTRA_OBJECT_COUNT;
    bool alive = false;
    std::atomic<ExtraObjectCell*>* nextFree = &nextFree_;
    for (ExtraObjectCell* cell = cells_; cell < end; ++cell) {
        // If the current cell is free, move on.
        if (cell == nextFree->load(std::memory_order_relaxed)) {
            nextFree = &cell->next_;
            continue;
        }
        if (SweepExtraObject(cell->Data(), sweepHandle)) {
            // If the current cell was marked, it's alive, and the whole page is alive.
            alive = true;
        } else {
            // Free the current block and insert it into the free list.
            cell->next_.store(nextFree->load(std::memory_order_relaxed), std::memory_order_relaxed);
            nextFree->store(cell, std::memory_order_relaxed);
            nextFree = &cell->next_;
        }
    }
    return alive;
}

} // namespace kotlin::alloc
