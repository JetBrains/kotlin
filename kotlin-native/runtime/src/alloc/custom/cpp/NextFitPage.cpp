/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "NextFitPage.hpp"

#include <atomic>
#include <cstdint>
#include <vector>

#include "CustomLogging.hpp"
#include "CustomAllocConstants.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

NextFitPage* NextFitPage::Create(uint32_t cellCount) noexcept {
    CustomAllocInfo("NextFitPage::Create(%u)", cellCount);
    RuntimeAssert(cellCount < NEXT_FIT_PAGE_CELL_COUNT, "cellCount is too large for NextFitPage");
    return new (SafeAlloc(NEXT_FIT_PAGE_SIZE)) NextFitPage(cellCount);
}

void NextFitPage::Destroy() noexcept {
    Free(this, NEXT_FIT_PAGE_SIZE);
}

NextFitPage::NextFitPage(uint32_t cellCount) noexcept : curBlock_(cells_) {
    cells_[0] = Cell(0); // Size 0 ensures any actual use would break
    cells_[1] = Cell(NEXT_FIT_PAGE_CELL_COUNT - 1);
}

uint8_t* NextFitPage::TryAllocate(uint32_t blockSize) noexcept {
    CustomAllocDebug("NextFitPage@%p::TryAllocate(%u)", this, blockSize);
    // +1 accounts for header, since cell->size also includes header cell
    uint32_t cellsNeeded = blockSize + 1;
    uint8_t* block = curBlock_->TryAllocate(cellsNeeded);
    if (block) return block;
    UpdateCurBlock(cellsNeeded);
    return curBlock_->TryAllocate(cellsNeeded);
}

bool NextFitPage::Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
    CustomAllocDebug("NextFitPage@%p::Sweep()", this);
    Cell* end = cells_ + NEXT_FIT_PAGE_CELL_COUNT;
    bool alive = false;
    for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
        if (block->isAllocated_) {
            if (SweepObject(block->data_, finalizerQueue, sweepHandle)) {
                alive = true;
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
    return alive;
}

void NextFitPage::UpdateCurBlock(uint32_t cellsNeeded) noexcept {
    CustomAllocDebug("NextFitPage@%p::UpdateCurBlock(%u)", this, cellsNeeded);
    if (curBlock_ == cells_) curBlock_ = cells_ + 1; // only used as a starting point
    Cell* end = cells_ + NEXT_FIT_PAGE_CELL_COUNT;
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
    if (curBlock_ < cells_ || curBlock_ >= cells_ + NEXT_FIT_PAGE_CELL_COUNT) return false;
    for (Cell* cur = cells_ + 1;; cur = cur->Next()) {
        if (cur->Next() <= cur) return false;
        if (cur->Next() > cells_ + NEXT_FIT_PAGE_CELL_COUNT) return false;
        if (cur->Next() == cells_ + NEXT_FIT_PAGE_CELL_COUNT) return true;
    }
}

std::vector<uint8_t*> NextFitPage::GetAllocatedBlocks() noexcept {
    std::vector<uint8_t*> allocated;
    Cell* end = cells_ + NEXT_FIT_PAGE_CELL_COUNT;
    for (Cell* block = cells_ + 1; block != end; block = block->Next()) {
        if (block->isAllocated_) {
            allocated.push_back(block->data_);
        }
    }
    return allocated;
}

} // namespace kotlin::alloc
