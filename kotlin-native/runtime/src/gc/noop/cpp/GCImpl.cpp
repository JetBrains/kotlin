/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include "GC.hpp"
#include "std_support/Memory.hpp"
#include "GlobalData.hpp"
#include "GCStatistics.hpp"
#include "ObjectOps.hpp"

using namespace kotlin;

gc::GC::ThreadData::ThreadData(GC& gc, gcScheduler::GCSchedulerThreadData&, mm::ThreadData& threadData) noexcept :
    impl_(std_support::make_unique<Impl>(gc, threadData)) {}

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
    impl_->objectFactoryThreadQueue().Publish();
}

void gc::GC::ThreadData::ClearForTests() noexcept {
    impl_->objectFactoryThreadQueue().ClearForTests();
}

ALWAYS_INLINE ObjHeader* gc::GC::ThreadData::CreateObject(const TypeInfo* typeInfo) noexcept {
    return impl_->objectFactoryThreadQueue().CreateObject(typeInfo);
}

ALWAYS_INLINE ArrayHeader* gc::GC::ThreadData::CreateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept {
    return impl_->objectFactoryThreadQueue().CreateArray(typeInfo, elements);
}

void gc::GC::ThreadData::OnSuspendForGC() noexcept { }

void gc::GC::ThreadData::safePoint() noexcept {}

gc::GC::GC(gcScheduler::GCScheduler&) noexcept : impl_(std_support::make_unique<Impl>()) {}

gc::GC::~GC() = default;

// static
size_t gc::GC::GetAllocatedHeapSize(ObjHeader* object) noexcept {
    return mm::ObjectFactory<GCImpl>::GetAllocatedHeapSize(object);
}

size_t gc::GC::GetTotalHeapObjectsSizeBytes() const noexcept {
    return allocatedBytes();
}

void gc::GC::ClearForTests() noexcept {
    impl_->objectFactory().ClearForTests();
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
void gc::GC::WaitFinalizers(int64_t epoch) noexcept {}

bool gc::isMarked(ObjHeader* object) noexcept {
    RuntimeAssert(false, "Should not reach here");
    return true;
}

ALWAYS_INLINE OBJ_GETTER(gc::tryRef, std::atomic<ObjHeader*>& object) noexcept {
    RETURN_OBJ(object.load(std::memory_order_relaxed));
}
