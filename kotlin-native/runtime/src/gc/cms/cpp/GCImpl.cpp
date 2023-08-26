/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include "ConcurrentMarkAndSweep.hpp"
#include "GC.hpp"
#include "GCStatistics.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ObjectAlloc.hpp"
#include "ObjectOps.hpp"
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
    ObjHeader* obj;
#ifndef CUSTOM_ALLOCATOR
    obj = impl_->objectFactoryThreadQueue().CreateObject(typeInfo);
#else
    obj = impl_->alloc().CreateObject(typeInfo);
#endif
    impl().gc().barriers().onAllocation(obj);
    return obj;
}

ALWAYS_INLINE ArrayHeader* gc::GC::ThreadData::CreateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept {
    ArrayHeader* arr;
#ifndef CUSTOM_ALLOCATOR
    arr = impl_->objectFactoryThreadQueue().CreateArray(typeInfo, elements);
#else
    arr = impl_->alloc().CreateArray(typeInfo, elements);
#endif
    impl().gc().barriers().onAllocation(arr->obj());
    return arr;
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

void gc::GC::ThreadData::OnSuspendForGC() noexcept {
    impl_->gc().OnSuspendForGC();
}

void gc::GC::ThreadData::safePoint() noexcept {
    impl_->gc().safePoint();
}

void gc::GC::ThreadData::onThreadRegistration() noexcept {
    impl_->gc().onThreadRegistration();
}

gc::GC::GC(gcScheduler::GCScheduler& gcScheduler) noexcept : impl_(std_support::make_unique<Impl>(gcScheduler)) {}

gc::GC::~GC() = default;

// static
size_t gc::GC::GetAllocatedHeapSize(ObjHeader* object) noexcept {
#ifdef CUSTOM_ALLOCATOR
    return alloc::CustomAllocator::GetAllocatedHeapSize(object);
#else
    return ObjectFactory::GetAllocatedHeapSize(object);
#endif
}

size_t gc::GC::GetTotalHeapObjectsSizeBytes() const noexcept {
    return allocatedBytes();
}

void gc::GC::ClearForTests() noexcept {
    impl_->gc().StopFinalizerThreadIfRunning();
#ifndef CUSTOM_ALLOCATOR
    impl_->extraObjectDataFactory().ClearForTests();
    impl_->objectFactory().ClearForTests();
#else
    impl_->gc().heap().ClearForTests();
#endif
    GCHandle::ClearForTests();
}

void gc::GC::StartFinalizerThreadIfNeeded() noexcept {
    impl_->gc().StartFinalizerThreadIfNeeded();
}

void gc::GC::StopFinalizerThreadIfRunning() noexcept {
    impl_->gc().StopFinalizerThreadIfRunning();
}

bool gc::GC::FinalizersThreadIsRunning() noexcept {
    return impl_->gc().FinalizersThreadIsRunning();
}

// static
ALWAYS_INLINE void gc::GC::processObjectInMark(void* state, ObjHeader* object) noexcept {
    gc::internal::processObjectInMark<gc::mark::ParallelMark::MarkTraits>(state, object);
}

// static
ALWAYS_INLINE void gc::GC::processArrayInMark(void* state, ArrayHeader* array) noexcept {
    gc::internal::processArrayInMark<gc::mark::ParallelMark::MarkTraits>(state, array);
}

// static
ALWAYS_INLINE void gc::GC::processFieldInMark(void* state, ObjHeader* field) noexcept {
    gc::internal::processFieldInMark<gc::mark::ParallelMark::MarkTraits>(state, field);
}

int64_t gc::GC::Schedule() noexcept {
    return impl_->gc().state().schedule();
}

void gc::GC::WaitFinished(int64_t epoch) noexcept {
    impl_->gc().state().waitEpochFinished(epoch);
}

void gc::GC::WaitFinalizers(int64_t epoch) noexcept {
    impl_->gc().state().waitEpochFinalized(epoch);
}

bool gc::isMarked(ObjHeader* object) noexcept {
    return objectDataForObject(object).marked();
}

ALWAYS_INLINE OBJ_GETTER(gc::tryRef, std::atomic<ObjHeader*>& object) noexcept {
    RETURN_RESULT_OF(gc::WeakRefRead, object);
}

ALWAYS_INLINE bool gc::tryResetMark(GC::ObjectData& objectData) noexcept {
    return objectData.tryResetMark();
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
    return sizeof(gc::GC::ObjectData);
}

// static
ALWAYS_INLINE size_t type_layout::descriptor<gc::GC::ObjectData>::type::alignment() noexcept {
    return alignof(gc::GC::ObjectData);
}

// static
ALWAYS_INLINE gc::GC::ObjectData* type_layout::descriptor<gc::GC::ObjectData>::type::construct(uint8_t* ptr) noexcept {
    return new (ptr) gc::GC::ObjectData();
}
