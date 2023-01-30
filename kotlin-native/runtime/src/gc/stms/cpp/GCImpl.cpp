/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include "GC.hpp"
#include "MarkAndSweepUtils.hpp"
#include "std_support/Memory.hpp"
#include "GlobalData.hpp"
#include "GCStatistics.hpp"

using namespace kotlin;

namespace {

ALWAYS_INLINE void SafePointRegular(gc::GC::ThreadData& threadData, size_t weight) noexcept {
    threadData.impl().gcScheduler().OnSafePointRegular(weight);
    auto flag = gc::internal::loadSafepointFlag();
    if (flag != gc::SameThreadMarkAndSweep::SafepointFlag::kNone) {
        threadData.impl().gc().SafePointSlowPath(flag);
    }
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

void gc::GC::ThreadData::OnStoppedForGC() noexcept {
    impl_->gcScheduler().OnStoppedForGC();
}

void gc::GC::ThreadData::OnSuspendForGC() noexcept { }

gc::GC::GC() noexcept : impl_(std_support::make_unique<Impl>()) {}

gc::GC::~GC() = default;

// static
size_t gc::GC::GetAllocatedHeapSize(ObjHeader* object) noexcept {
    return mm::ObjectFactory<GCImpl>::GetAllocatedHeapSize(object);
}

size_t gc::GC::GetTotalHeapObjectsSizeBytes() const noexcept {
    return allocatedBytes();
}

gc::GCSchedulerConfig& gc::GC::gcSchedulerConfig() noexcept {
    return impl_->gcScheduler().config();
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
