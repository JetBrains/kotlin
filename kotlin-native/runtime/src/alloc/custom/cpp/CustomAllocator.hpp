/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_ALLOCATOR_HPP_
#define CUSTOM_ALLOC_CPP_ALLOCATOR_HPP_

#include <atomic>
#include <cstring>

#include "AllocationSize.hpp"
#include "ExtraObjectData.hpp"
#include "Heap.hpp"
#include "NextFitPage.hpp"
#include "Memory.h"
#include "FixedBlockPage.hpp"

namespace kotlin::alloc {

class CustomAllocator {
public:
    explicit CustomAllocator(Heap& heap) noexcept;

    ~CustomAllocator();

    ObjHeader* CreateObject(const TypeInfo* typeInfo) noexcept;

    ArrayHeader* CreateArray(const TypeInfo* typeInfo, uint32_t count) noexcept;

    mm::ExtraObjectData* CreateExtraObjectDataForObject(ObjHeader* baseObject, const TypeInfo* type) noexcept;

    void PrepareForGC() noexcept;

    FinalizerQueue ExtractFinalizerQueue() noexcept;

    static size_t GetAllocatedHeapSize(ObjHeader* object) noexcept;

    Heap& heap() noexcept {
        return heap_;
    }

private:
    uint8_t* Allocate(AllocationSize size) noexcept;
    uint8_t* AllocateInSingleObjectPage(uint64_t cellCount) noexcept;
    uint8_t* AllocateInNextFitPage(uint32_t cellCount) noexcept;
    uint8_t* AllocateInFixedBlockPage(uint32_t cellCount) noexcept;
    uint8_t* AllocateExtraObjectData() noexcept;

    Heap& heap_;
    NextFitPage* nextFitPage_;
    FixedBlockPage* fixedBlockPages_[FixedBlockPage::MAX_BLOCK_SIZE + 1];
    FixedBlockPage* extraObjectPage_;
    FinalizerQueue finalizerQueue_;
};

} // namespace kotlin::alloc

#endif
