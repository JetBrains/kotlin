/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include <memory>

#include "GC.hpp"
#include "GCStatistics.hpp"
#include "GlobalData.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ObjectOps.hpp"
#include "SameThreadMarkAndSweep.hpp"

using namespace kotlin;

gc::GC::ThreadData::ThreadData(GC& gc, mm::ThreadData& threadData) noexcept : impl_(std::make_unique<Impl>(gc, threadData)) {}

gc::GC::ThreadData::~ThreadData() = default;

void gc::GC::ThreadData::OnSuspendForGC() noexcept { }

void gc::GC::ThreadData::safePoint() noexcept {}

void gc::GC::ThreadData::onThreadRegistration() noexcept {}

ALWAYS_INLINE void gc::GC::ThreadData::onAllocation(ObjHeader* object) noexcept {}

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
PERFORMANCE_INLINE void gc::GC::processObjectInMark(void* state, ObjHeader* object) noexcept {
    gc::internal::processObjectInMark<gc::internal::MarkTraits>(state, object);
}

// static
PERFORMANCE_INLINE void gc::GC::processArrayInMark(void* state, ArrayHeader* array) noexcept {
    gc::internal::processArrayInMark<gc::internal::MarkTraits>(state, array);
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

void gc::GC::configureMainThreadFinalizerProcessor(std::function<void(alloc::RunLoopFinalizerProcessorConfig&)> f) noexcept {
    impl_->gc().mainThreadFinalizerProcessor().withConfig(std::move(f));
}

bool gc::GC::mainThreadFinalizerProcessorAvailable() noexcept {
    return impl_->gc().mainThreadFinalizerProcessor().available();
}

ALWAYS_INLINE void gc::beforeHeapRefUpdate(mm::DirectRefAccessor ref, ObjHeader* value, bool loadAtomic) noexcept {}

ALWAYS_INLINE OBJ_GETTER(gc::weakRefReadBarrier, std_support::atomic_ref<ObjHeader*> weakReferee) noexcept {
    RETURN_OBJ(weakReferee.load(std::memory_order_relaxed));
}

bool gc::isMarked(ObjHeader* object) noexcept {
    return alloc::objectDataForObject(object).marked();
}

PERFORMANCE_INLINE bool gc::tryResetMark(GC::ObjectData& objectData) noexcept {
    return objectData.tryResetMark();
}

ALWAYS_INLINE bool gc::barriers::SpecialRefReleaseGuard::isNoop() { return true; }
ALWAYS_INLINE gc::barriers::SpecialRefReleaseGuard::SpecialRefReleaseGuard(mm::DirectRefAccessor) noexcept {}
ALWAYS_INLINE gc::barriers::SpecialRefReleaseGuard::SpecialRefReleaseGuard(SpecialRefReleaseGuard&&) noexcept = default;
ALWAYS_INLINE gc::barriers::SpecialRefReleaseGuard::~SpecialRefReleaseGuard() noexcept = default;
ALWAYS_INLINE gc::barriers::SpecialRefReleaseGuard& gc::barriers::SpecialRefReleaseGuard::SpecialRefReleaseGuard::operator=(SpecialRefReleaseGuard&&) noexcept = default;

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
