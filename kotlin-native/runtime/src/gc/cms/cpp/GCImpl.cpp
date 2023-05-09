/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include "GC.hpp"
#include "GCStatistics.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ThreadSuspension.hpp"
#include "std_support/Memory.hpp"

using namespace kotlin;

namespace {

ALWAYS_INLINE void SafePointRegular(gc::GC::ThreadData& threadData, size_t weight) noexcept {
    threadData.impl().gcScheduler().OnSafePointRegular(weight);
    mm::SuspendIfRequested();
}

} // namespace

gc::GC::ThreadData::ThreadData(GC& gc, mm::ThreadData& threadData) noexcept : impl_(std_support::make_unique<Impl>(gc, threadData)) {}

gc::GC::ThreadData::~ThreadData() = default;

ALWAYS_INLINE void gc::GC::ThreadData::SafePointFunctionPrologue() noexcept {
    SafePointRegular(*this, GCSchedulerThreadData::kFunctionPrologueWeight);
}

ALWAYS_INLINE void gc::GC::ThreadData::SafePointLoopBody() noexcept {
    SafePointRegular(*this, GCSchedulerThreadData::kLoopBodyWeight);
}

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

void gc::GC::ThreadData::OnStoppedForGC() noexcept {
    impl_->gcScheduler().OnStoppedForGC();
}

void gc::GC::ThreadData::OnSuspendForGC() noexcept {
    impl_->gc().OnSuspendForGC();
}

gc::GC::GC() noexcept : impl_(std_support::make_unique<Impl>()) {}

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

gc::GCSchedulerConfig& gc::GC::gcSchedulerConfig() noexcept {
    return impl_->gcScheduler().config();
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
