/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include "Common.h"
#include "GC.hpp"
#include "GCStatistics.hpp"
#include "NoOpGC.hpp"
#include "ObjectAlloc.hpp"
#include "ObjectOps.hpp"
#include "ThreadData.hpp"
#include "std_support/Memory.hpp"

using namespace kotlin;

gc::GC::ThreadData::ThreadData(GC& gc, mm::ThreadData& threadData) noexcept : impl_(std_support::make_unique<Impl>(gc, threadData)) {}

gc::GC::ThreadData::~ThreadData() = default;

void gc::GC::ThreadData::PublishObjectFactory() noexcept {
#ifndef CUSTOM_ALLOCATOR
    impl_->extraObjectDataFactoryThreadQueue().Publish();
    impl_->objectFactoryThreadQueue().Publish();
#endif
}

void gc::GC::ThreadData::ClearForTests() noexcept {
#ifndef CUSTOM_ALLOCATOR
    impl_->extraObjectDataFactoryThreadQueue().ClearForTests();
    impl_->objectFactoryThreadQueue().ClearForTests();
#else
    impl_->alloc().PrepareForGC();
#endif
}

ALWAYS_INLINE ObjHeader* gc::GC::ThreadData::CreateObject(const TypeInfo* typeInfo) noexcept {
#ifndef CUSTOM_ALLOCATOR
    return impl_->objectFactoryThreadQueue().CreateObject(typeInfo);
#else
    return impl_->alloc().CreateObject(typeInfo);
#endif
}

ALWAYS_INLINE ArrayHeader* gc::GC::ThreadData::CreateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept {
#ifndef CUSTOM_ALLOCATOR
    return impl_->objectFactoryThreadQueue().CreateArray(typeInfo, elements);
#else
    return impl_->alloc().CreateArray(typeInfo, elements);
#endif
}

ALWAYS_INLINE mm::ExtraObjectData& gc::GC::ThreadData::CreateExtraObjectDataForObject(
        ObjHeader* object, const TypeInfo* typeInfo) noexcept {
#ifndef CUSTOM_ALLOCATOR
    return impl_->extraObjectDataFactoryThreadQueue().CreateExtraObjectDataForObject(object, typeInfo);
#else
    return impl_->alloc().CreateExtraObjectDataForObject(object, typeInfo);
#endif
}

ALWAYS_INLINE void gc::GC::ThreadData::DestroyUnattachedExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
#ifndef CUSTOM_ALLOCATOR
    impl_->extraObjectDataFactoryThreadQueue().DestroyExtraObjectData(extraObject);
#else
    extraObject.setFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE);
#endif
}

void gc::GC::ThreadData::OnSuspendForGC() noexcept { }

void gc::GC::ThreadData::safePoint() noexcept {}

void gc::GC::ThreadData::onThreadRegistration() noexcept {}

gc::GC::GC(gcScheduler::GCScheduler&) noexcept : impl_(std_support::make_unique<Impl>()) {}

gc::GC::~GC() = default;

// static
size_t gc::GC::GetAllocatedHeapSize(ObjHeader* object) noexcept {
#ifndef CUSTOM_ALLOCATOR
    return ObjectFactory::GetAllocatedHeapSize(object);
#else
    return alloc::CustomAllocator::GetAllocatedHeapSize(object);
#endif
}

size_t gc::GC::GetTotalHeapObjectsSizeBytes() const noexcept {
    return allocatedBytes();
}

void gc::GC::ClearForTests() noexcept {
#ifndef CUSTOM_ALLOCATOR
    impl_->extraObjectDataFactory().ClearForTests();
    impl_->objectFactory().ClearForTests();
#else
    impl_->gc().heap().ClearForTests();
#endif
    GCHandle::ClearForTests();
}

void gc::GC::StartFinalizerThreadIfNeeded() noexcept {}

void gc::GC::StopFinalizerThreadIfRunning() noexcept {}

bool gc::GC::FinalizersThreadIsRunning() noexcept {
    return false;
}

// static
ALWAYS_INLINE void gc::GC::processObjectInMark(void* state, ObjHeader* object) noexcept {}

// static
ALWAYS_INLINE void gc::GC::processArrayInMark(void* state, ArrayHeader* array) noexcept {}

// static
ALWAYS_INLINE void gc::GC::processFieldInMark(void* state, ObjHeader* field) noexcept {}

int64_t gc::GC::Schedule() noexcept {
    return 0;
}

void gc::GC::WaitFinished(int64_t epoch) noexcept {}

void gc::GC::WaitFinalizers(int64_t epoch) noexcept {}

bool gc::isMarked(ObjHeader* object) noexcept {
    RuntimeAssert(false, "Should not reach here");
    return true;
}

ALWAYS_INLINE OBJ_GETTER(gc::tryRef, std::atomic<ObjHeader*>& object) noexcept {
    RETURN_OBJ(object.load(std::memory_order_relaxed));
}

ALWAYS_INLINE bool gc::tryResetMark(GC::ObjectData& objectData) noexcept {
    RuntimeAssert(false, "Should not reach here");
    return true;
}

// static
ALWAYS_INLINE void gc::GC::DestroyExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
#ifndef CUSTOM_ALLOCATOR
    extraObject.Uninstall();
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->gc().impl().extraObjectDataFactoryThreadQueue().DestroyExtraObjectData(extraObject);
#else
    extraObject.ReleaseAssociatedObject();
    extraObject.setFlag(mm::ExtraObjectData::FLAGS_FINALIZED);
#endif
}

// static
ALWAYS_INLINE uint64_t type_layout::descriptor<gc::GC::ObjectData>::type::size() noexcept {
    return 0;
}

// static
ALWAYS_INLINE size_t type_layout::descriptor<gc::GC::ObjectData>::type::alignment() noexcept {
    return 1;
}

// static
ALWAYS_INLINE gc::GC::ObjectData* type_layout::descriptor<gc::GC::ObjectData>::type::construct(uint8_t* ptr) noexcept {
    return reinterpret_cast<gc::GC::ObjectData*>(ptr);
}
