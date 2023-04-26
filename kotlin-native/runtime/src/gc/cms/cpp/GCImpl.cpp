/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include "GC.hpp"
#include "GCStatistics.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ObjectOps.hpp"
#include "ThreadSuspension.hpp"
#include "std_support/Memory.hpp"

using namespace kotlin;

gc::GC::ThreadData::ThreadData(GC& gc, gcScheduler::GCSchedulerThreadData& gcScheduler, mm::ThreadData& threadData) noexcept :
    impl_(std_support::make_unique<Impl>(gc, gcScheduler, threadData)) {}

gc::GC::ThreadData::~ThreadData() = default;

void gc::GC::ThreadData::Schedule() noexcept {
    impl_->gc().Schedule();
}

void gc::GC::ThreadData::ScheduleAndWaitFullGC() noexcept {
    impl_->gc().ScheduleAndWaitFullGC();
}

void gc::GC::ThreadData::ScheduleAndWaitFullGCWithFinalizers() noexcept {
    impl_->gc().ScheduleAndWaitFullGCWithFinalizers();
}

void gc::GC::ThreadData::Publish() noexcept {
#ifndef CUSTOM_ALLOCATOR
    impl_->objectFactoryThreadQueue().Publish();
#endif
}

void gc::GC::ThreadData::ClearForTests() noexcept {
#ifndef CUSTOM_ALLOCATOR
    impl_->objectFactoryThreadQueue().ClearForTests();
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

void gc::GC::ThreadData::OnSuspendForGC() noexcept {
    impl_->gc().OnSuspendForGC();
}

void gc::GC::ThreadData::safePoint() noexcept {
    impl_->gc().safePoint();
}

gc::GC::GC(gcScheduler::GCScheduler& gcScheduler) noexcept : impl_(std_support::make_unique<Impl>(gcScheduler)) {}

gc::GC::~GC() = default;

// static
size_t gc::GC::GetAllocatedHeapSize(ObjHeader* object) noexcept {
#ifdef CUSTOM_ALLOCATOR
    return alloc::CustomAllocator::GetAllocatedHeapSize(object);
#else
    return mm::ObjectFactory<GCImpl>::GetAllocatedHeapSize(object);
#endif
}

size_t gc::GC::GetTotalHeapObjectsSizeBytes() const noexcept {
    return allocatedBytes();
}

void gc::GC::ClearForTests() noexcept {
    impl_->gc().StopFinalizerThreadIfRunning();
#ifndef CUSTOM_ALLOCATOR
    impl_->objectFactory().ClearForTests();
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
    gc::internal::processObjectInMark<gc::internal::MarkTraits>(state, object);
}

// static
ALWAYS_INLINE void gc::GC::processArrayInMark(void* state, ArrayHeader* array) noexcept {
    gc::internal::processArrayInMark<gc::internal::MarkTraits>(state, array);
}

// static
ALWAYS_INLINE void gc::GC::processFieldInMark(void* state, ObjHeader* field) noexcept {
    gc::internal::processFieldInMark<gc::internal::MarkTraits>(state, field);
}

int64_t gc::GC::Schedule() noexcept {
    return impl_->gc().Schedule();
}

void gc::GC::WaitFinalizers(int64_t epoch) noexcept {
    impl_->gc().WaitFinalized(epoch);
}

bool gc::isMarked(ObjHeader* object) noexcept {
    auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(object).ObjectData();
    return objectData.marked();
}

ALWAYS_INLINE OBJ_GETTER(gc::tryRef, std::atomic<ObjHeader*>& object) noexcept {
    RETURN_RESULT_OF(gc::WeakRefRead, object);
}
