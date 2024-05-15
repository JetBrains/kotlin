/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AllocatorImpl.hpp"

#include "GCApi.hpp"
#include "Heap.hpp"

using namespace kotlin;

alloc::Allocator::ThreadData::ThreadData(Allocator& allocator) noexcept : impl_(std::make_unique<Impl>(allocator.impl())) {}

alloc::Allocator::ThreadData::~ThreadData() = default;

ALWAYS_INLINE ObjHeader* alloc::Allocator::ThreadData::allocateObject(const TypeInfo* typeInfo) noexcept {
    return impl_->alloc().CreateObject(typeInfo);
}

ALWAYS_INLINE ArrayHeader* alloc::Allocator::ThreadData::allocateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept {
    return impl_->alloc().CreateArray(typeInfo, elements);
}

ALWAYS_INLINE mm::ExtraObjectData& alloc::Allocator::ThreadData::allocateExtraObjectData(
        ObjHeader* object, const TypeInfo* typeInfo) noexcept {
    return impl_->alloc().CreateExtraObjectDataForObject(object, typeInfo);
}

ALWAYS_INLINE void alloc::Allocator::ThreadData::destroyUnattachedExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
    extraObject.setFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE);
}

void alloc::Allocator::ThreadData::prepareForGC() noexcept {
    impl_->alloc().PrepareForGC();
}

void alloc::Allocator::ThreadData::clearForTests() noexcept {
    impl_->alloc().PrepareForGC();
}

alloc::Allocator::Allocator() noexcept : impl_(std::make_unique<Impl>()) {}

alloc::Allocator::~Allocator() = default;

void alloc::Allocator::prepareForGC() noexcept {
    impl_->heap().PrepareForGC();
}

void alloc::Allocator::clearForTests() noexcept {
    impl_->heap().ClearForTests();
}

size_t alloc::Allocator::estimateOverheadPerThread() noexcept {
    if (compiler::disableAllocatorOverheadEstimate()) return 0;
    return impl_->heap().EstimateOverheadPerThread();
}

void alloc::initObjectPool() noexcept {}

void alloc::compactObjectPoolInCurrentThread() noexcept {}

gc::GC::ObjectData& alloc::objectDataForObject(ObjHeader* object) noexcept {
    return HeapObjHeader::from(object).objectData();
}

ObjHeader* alloc::objectForObjectData(gc::GC::ObjectData& objectData) noexcept {
    return HeapObjHeader::from(objectData).object();
}

size_t alloc::allocatedHeapSize(ObjHeader* object) noexcept {
    return CustomAllocator::GetAllocatedHeapSize(object);
}

size_t alloc::allocatedBytes() noexcept {
    return GetAllocatedBytes();
}

void alloc::destroyExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
    extraObject.ReleaseAssociatedObject();
    if (extraObject.GetBaseObject()) {
        // If there's an object attached to this extra object, the next
        // GC sweep will have to resolve this cycle.
        extraObject.setFlag(mm::ExtraObjectData::FLAGS_FINALIZED);
    } else {
        // If there's no object attached to this extra object, the next
        // GC sweep will just collect this extra object.
        extraObject.setFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE);
    }
}
