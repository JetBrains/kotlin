/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CustomAllocator.hpp"

#include <atomic>
#include <cstdint>
#include <cstdlib>
#include <cinttypes>
#include <cstring>
#include <new>

#include "CustomAllocConstants.hpp"
#include "CustomLogging.hpp"
#include "ExtraObjectData.hpp"
#include "ExtraObjectPage.hpp"
#include "GC.hpp"
#include "GCScheduler.hpp"
#include "KAssert.h"
#include "SingleObjectPage.hpp"
#include "NextFitPage.hpp"
#include "Memory.h"
#include "FixedBlockPage.hpp"
#include "GCApi.hpp"
#include "TypeInfo.h"

namespace kotlin::alloc {

CustomAllocator::CustomAllocator(Heap& heap) noexcept : heap_(heap), nextFitPage_(nullptr), extraObjectPage_(nullptr) {
    CustomAllocInfo("CustomAllocator::CustomAllocator(heap)");
    memset(fixedBlockPages_, 0, sizeof(fixedBlockPages_));
}

CustomAllocator::~CustomAllocator() {
    heap_.AddToFinalizerQueue(std::move(finalizerQueue_));
}

ObjHeader* CustomAllocator::CreateObject(const TypeInfo* typeInfo) noexcept {
    RuntimeAssert(!typeInfo->IsArray(), "Must not be an array");
    auto descriptor = HeapObject::make_descriptor(typeInfo);
    auto& heapObject = *descriptor.construct(Allocate(descriptor.size()));
    ObjHeader* object = heapObject.header(descriptor).object();
    if (typeInfo->flags_ & TF_HAS_FINALIZER) {
        auto* extraObject = CreateExtraObject();
        object->typeInfoOrMeta_ = reinterpret_cast<TypeInfo*>(new (extraObject) mm::ExtraObjectData(object, typeInfo));
        CustomAllocDebug("CustomAllocator: %p gets extraObject %p", object, extraObject);
        CustomAllocDebug("CustomAllocator: %p->BaseObject == %p", extraObject, extraObject->GetBaseObject());
    } else {
        object->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    }
    return object;
}

ArrayHeader* CustomAllocator::CreateArray(const TypeInfo* typeInfo, uint32_t count) noexcept {
    CustomAllocDebug("CustomAllocator@%p::CreateArray(%d)", this ,count);
    RuntimeAssert(typeInfo->IsArray(), "Must be an array");
    auto descriptor = HeapArray::make_descriptor(typeInfo, count);
    auto& heapArray = *descriptor.construct(Allocate(descriptor.size()));
    ArrayHeader* array = heapArray.header(descriptor).array();
    array->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    array->count_ = count;
    return array;
}

mm::ExtraObjectData* CustomAllocator::CreateExtraObject() noexcept {
    CustomAllocDebug("CustomAllocator::CreateExtraObject()");
    ExtraObjectPage* page = extraObjectPage_;
    if (page) {
        mm::ExtraObjectData* block = page->TryAllocate();
        if (block) {
            memset(block, 0, sizeof(mm::ExtraObjectData));
            return block;
        }
    }
    CustomAllocDebug("Failed to allocate in current ExtraObjectPage");
    while ((page = heap_.GetExtraObjectPage(finalizerQueue_))) {
        mm::ExtraObjectData* block = page->TryAllocate();
        if (block) {
            extraObjectPage_ = page;
            memset(block, 0, sizeof(mm::ExtraObjectData));
            return block;
        }
    }
    return nullptr;
}

mm::ExtraObjectData& CustomAllocator::CreateExtraObjectDataForObject(
        ObjHeader* baseObject, const TypeInfo* info) noexcept {
    mm::ExtraObjectData* extraObject = CreateExtraObject();
    return *new (extraObject) mm::ExtraObjectData(baseObject, info);
}

FinalizerQueue CustomAllocator::ExtractFinalizerQueue() noexcept {
    return std::move(finalizerQueue_);
}

void CustomAllocator::PrepareForGC() noexcept {
    CustomAllocInfo("CustomAllocator@%p::PrepareForGC()", this);
    nextFitPage_ = nullptr;
    memset(fixedBlockPages_, 0, sizeof(fixedBlockPages_));
    extraObjectPage_ = nullptr;
}

// static
size_t CustomAllocator::GetAllocatedHeapSize(ObjHeader* object) noexcept {
    RuntimeAssert(object->heap(), "Object must be a heap object");
    const auto* typeInfo = object->type_info();
    if (typeInfo->IsArray()) {
        return HeapArray::make_descriptor(typeInfo, object->array()->count_).size();
    } else {
        return HeapObject::make_descriptor(typeInfo).size();
    }
}

uint8_t* CustomAllocator::Allocate(uint64_t size) noexcept {
    RuntimeAssert(size, "CustomAllocator::Allocate cannot allocate 0 bytes");
    CustomAllocDebug("CustomAllocator::Allocate(%" PRIu64 ")", size);
    uint64_t cellCount = (size + sizeof(Cell) - 1) / sizeof(Cell);
    if (cellCount <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE) {
        return AllocateInFixedBlockPage(cellCount);
    } else if (cellCount > NEXT_FIT_PAGE_MAX_BLOCK_SIZE) {
        return AllocateInSingleObjectPage(cellCount);
    } else {
        return AllocateInNextFitPage(cellCount);
    }
}

uint8_t* CustomAllocator::AllocateInSingleObjectPage(uint64_t cellCount) noexcept {
    CustomAllocDebug("CustomAllocator::AllocateInSingleObjectPage(%" PRIu64 ")", cellCount);
    uint8_t* block = heap_.GetSingleObjectPage(cellCount, finalizerQueue_)->TryAllocate();
    return block;
}

uint8_t* CustomAllocator::AllocateInNextFitPage(uint32_t cellCount) noexcept {
    CustomAllocDebug("CustomAllocator::AllocateInNextFitPage(%u)", cellCount);
    if (nextFitPage_) {
        uint8_t* block = nextFitPage_->TryAllocate(cellCount);
        if (block) return block;
    }
    CustomAllocDebug("Failed to allocate in curPage");
    while (true) {
        nextFitPage_ = heap_.GetNextFitPage(cellCount, finalizerQueue_);
        uint8_t* block = nextFitPage_->TryAllocate(cellCount);
        if (block) return block;
    }
}

uint8_t* CustomAllocator::AllocateInFixedBlockPage(uint32_t cellCount) noexcept {
    CustomAllocDebug("CustomAllocator::AllocateInFixedBlockPage(%u)", cellCount);
    FixedBlockPage* page = fixedBlockPages_[cellCount];
    if (page) {
        uint8_t* block = page->TryAllocate();
        if (block) return block;
    }
    CustomAllocDebug("Failed to allocate in current FixedBlockPage");
    while ((page = heap_.GetFixedBlockPage(cellCount, finalizerQueue_))) {
        uint8_t* block = page->TryAllocate();
        if (block) {
            fixedBlockPages_[cellCount] = page;
            return block;
        }
    }
    return nullptr;
}

} // namespace kotlin::alloc
