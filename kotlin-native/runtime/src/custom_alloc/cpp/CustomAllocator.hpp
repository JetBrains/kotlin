/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_ALLOCATOR_HPP_
#define CUSTOM_ALLOC_CPP_ALLOCATOR_HPP_

#include <atomic>
#include <cstring>

#include "ExtraObjectData.hpp"
#include "ExtraObjectPage.hpp"
#include "GCScheduler.hpp"
#include "Heap.hpp"
#include "NextFitPage.hpp"
#include "Memory.h"
#include "FixedBlockPage.hpp"

namespace kotlin::alloc {

class CustomAllocator {
public:
    explicit CustomAllocator(Heap& heap, gc::GCSchedulerThreadData& gcScheduler) noexcept;

    ObjHeader* CreateObject(const TypeInfo* typeInfo) noexcept;

    ArrayHeader* CreateArray(const TypeInfo* typeInfo, uint32_t count) noexcept;

    mm::ExtraObjectData* CreateExtraObject() noexcept;

    static mm::ExtraObjectData& CreateExtraObjectDataForObject(
            mm::ThreadData* threadData, ObjHeader* baseObject, const TypeInfo* info) noexcept;

    void PrepareForGC() noexcept;

    FinalizerQueue ExtractFinalizerQueue() noexcept;

    static size_t GetAllocatedHeapSize(ObjHeader* object) noexcept;

private:
    uint8_t* Allocate(uint64_t cellCount) noexcept;
    uint8_t* AllocateInSingleObjectPage(uint64_t cellCount) noexcept;
    uint8_t* AllocateInNextFitPage(uint32_t cellCount) noexcept;
    uint8_t* AllocateInFixedBlockPage(uint32_t cellCount) noexcept;

    Heap& heap_;
    gc::GCSchedulerThreadData& gcScheduler_;
    NextFitPage* nextFitPage_;
    FixedBlockPage* fixedBlockPages_[FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE + 1];
    ExtraObjectPage* extraObjectPage_;
    FinalizerQueue finalizerQueue_;
};

} // namespace kotlin::alloc

#endif
