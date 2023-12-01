/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include "Common.h"
#include "GCStatistics.hpp"
#include "KAssert.h"
#include "Logging.hpp"

using namespace kotlin;

gc::GC::ThreadData::ThreadData(GC& gc, mm::ThreadData& threadData) noexcept {}

gc::GC::ThreadData::~ThreadData() = default;

void gc::GC::ThreadData::OnSuspendForGC() noexcept { }

void gc::GC::ThreadData::safePoint() noexcept {}

void gc::GC::ThreadData::onThreadRegistration() noexcept {}

ALWAYS_INLINE void gc::GC::ThreadData::onAllocation(ObjHeader* object) noexcept {}

gc::GC::GC(alloc::Allocator&, gcScheduler::GCScheduler&) noexcept {
    RuntimeLogInfo({kTagGC}, "No-op GC initialized");
}

gc::GC::~GC() = default;

void gc::GC::ClearForTests() noexcept {
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

int64_t gc::GC::Schedule() noexcept {
    return 0;
}

void gc::GC::WaitFinished(int64_t epoch) noexcept {}

void gc::GC::WaitFinalizers(int64_t epoch) noexcept {}

void gc::GC::configureMainThreadFinalizerProcessor(std::function<void(alloc::RunLoopFinalizerProcessorConfig&)> f) noexcept {}

bool gc::GC::mainThreadFinalizerProcessorAvailable() noexcept {
    return false;
}

ALWAYS_INLINE void gc::beforeHeapRefUpdate(mm::DirectRefAccessor ref, ObjHeader* value) noexcept {}

ALWAYS_INLINE OBJ_GETTER(gc::weakRefReadBarrier, std::atomic<ObjHeader*>& weakReferee) noexcept {
    RETURN_OBJ(weakReferee.load(std::memory_order_relaxed));
}

bool gc::isMarked(ObjHeader* object) noexcept {
    RuntimeAssert(false, "Should not reach here");
    return true;
}

ALWAYS_INLINE bool gc::tryResetMark(GC::ObjectData& objectData) noexcept {
    RuntimeAssert(false, "Should not reach here");
    return true;
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
