/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include <memory>

#include "ConcurrentMarkAndSweep.hpp"
#include "GC.hpp"
#include "GCStatistics.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ObjectOps.hpp"

using namespace kotlin;

gc::GC::ThreadData::ThreadData(GC& gc, mm::ThreadData& threadData) noexcept : impl_(std::make_unique<Impl>(gc, threadData)) {}

gc::GC::ThreadData::~ThreadData() = default;

ALWAYS_INLINE void gc::GC::ThreadData::onAllocation(ObjHeader* object) noexcept {
    impl().gc().barriers().onAllocation(object);
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

gc::GC::GC(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler) noexcept :
    impl_(std::make_unique<Impl>(allocator, gcScheduler)) {}

gc::GC::~GC() = default;

void gc::GC::ClearForTests() noexcept {
    impl_->gc().StopFinalizerThreadIfRunning();
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
    return alloc::objectDataForObject(object).marked();
}

ALWAYS_INLINE OBJ_GETTER(gc::tryRef, std::atomic<ObjHeader*>& object) noexcept {
    RETURN_RESULT_OF(gc::WeakRefRead, object);
}

ALWAYS_INLINE bool gc::tryResetMark(GC::ObjectData& objectData) noexcept {
    return objectData.tryResetMark();
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
