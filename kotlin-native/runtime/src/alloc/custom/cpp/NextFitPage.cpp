/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "NextFitPage.hpp"

#include <atomic>
#include <cstdint>
#include <vector>

#include "CustomLogging.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

NextFitPage* NextFitPage::Create(uint32_t cellCount) noexcept {
    CustomAllocInfo("NextFitPage::Create(%u)", cellCount);
    RuntimeAssert(cellCount < NextFitPage::cellCount(), "cellCount is too large for NextFitPage");
    return new (SafeAlloc(SIZE)) NextFitPage(cellCount);
}

void NextFitPage::Destroy() noexcept {
    Free(this, SIZE);
}

NextFitPage::NextFitPage(uint32_t cellCount) noexcept : curBlock_(cells_) {
    cells_[0] = Cell(0); // Size 0 ensures any actual use would break
    cells_[1] = Cell(NextFitPage::cellCount() - 1);
}

uint8_t* NextFitPage::TryAllocate(uint32_t blockSize) noexcept {
    CustomAllocDebug("NextFitPage@%p::TryAllocate(%u)", this, blockSize);
    // +1 accounts for header, since cell->size also includes header cell
    uint32_t cellsNeeded = blockSize + 1;
    uint8_t* allocated = curBlock_->TryAllocate(cellsNeeded);
    if (allocated) return allocated;

    UpdateCurBlock(cellsNeeded);
    allocated = curBlock_->TryAllocate(cellsNeeded);
    if (allocated) return allocated;

    allocatedSizeTracker_.onPageOverflow(GetAllocatedSizeBytes());
    return nullptr;
}

bool NextFitPage::Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocDebug("NextFitPage@%p::Sweep()", this);
    Cell* end = cells_ + NextFitPage::cellCount();
    std::size_t aliveBytes = 0;
    for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
        if (block->isAllocated_) {
            if (SweepObject(block->data_, finalizerQueue, sweepHandle)) {
                aliveBytes += AllocationSize::cells(block->size_).inBytes();
            } else {
                block->Deallocate();
            }
        }
    }
    Cell* maxBlock = cells_; // size 0 block
    for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
        if (block->isAllocated_) continue;
        for (auto* next = block->Next(); next != end; next = block->Next()) {
            if (next->isAllocated_) {
                break;
            }
            block->size_ += next->size_;
            memset(next, 0, sizeof(*next));
        }
        if (block->size_ > maxBlock->size_) maxBlock = block;
    }
    curBlock_ = maxBlock;

    RuntimeAssert(aliveBytes == GetAllocatedSizeBytes(),
                  "Sweep counted %zu alive bytes, while GetAllocatedSizeBytes() returns %zu", aliveBytes, GetAllocatedSizeBytes());
    allocatedSizeTracker_.afterSweep(aliveBytes);

    return aliveBytes > 0;
}

void NextFitPage::UpdateCurBlock(uint32_t cellsNeeded) noexcept {
    CustomAllocDebug("NextFitPage@%p::UpdateCurBlock(%u)", this, cellsNeeded);
    if (curBlock_ == cells_) curBlock_ = cells_ + 1; // only used as a starting point
    Cell* end = cells_ + NextFitPage::cellCount();
    Cell* maxBlock = cells_; // size 0 block
    for (Cell* block = curBlock_; block != end; block = block->Next()) {
        if (!block->isAllocated_ && block->size_ > maxBlock->size_) {
            maxBlock = block;
            if (block->size_ >= cellsNeeded) {
                curBlock_ = maxBlock;
                return;
            }
        }
    }
    CustomAllocDebug("NextFitPage@%p::UpdateCurBlock: starting from beginning", this);
    for (Cell* block = cells_ + 1; block != curBlock_; block = block->Next()) {
        if (!block->isAllocated_ && block->size_ > maxBlock->size_) {
            maxBlock = block;
            if (block->size_ >= cellsNeeded) {
                curBlock_ = maxBlock;
                return;
            }
        }
    }
    curBlock_ = maxBlock;
}

bool NextFitPage::CheckInvariants() noexcept {
    if (curBlock_ < cells_ || curBlock_ >= cells_ + cellCount()) return false;
    for (Cell* cur = cells_ + 1;; cur = cur->Next()) {
        if (cur->Next() <= cur) return false;
        if (cur->Next() > cells_ + NextFitPage::cellCount()) return false;
        if (cur->Next() == cells_ + NextFitPage::cellCount()) return true;
    }
}

std::vector<uint8_t*> NextFitPage::GetAllocatedBlocks() noexcept {
    std::vector<uint8_t*> allocated;
    TraverseAllocatedBlocks([&allocated](uint8_t* block) {
        allocated.push_back(block);
    });
    return allocated;
}

std::size_t NextFitPage::GetAllocatedSizeBytes() noexcept {
    std::size_t allocatedBytes = 0;
    Cell* end = cells_ + NextFitPage::cellCount();
    for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
        if (block->isAllocated_) {
            allocatedBytes += block->size_ * sizeof(Cell);
        }
    }
    return allocatedBytes;
}

} // namespace kotlin::alloc
