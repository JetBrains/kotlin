/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Barriers.hpp"

#include <algorithm>
#include <atomic>

#include "GCImpl.hpp"
#include "SafePoint.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

namespace {

std::atomic<ObjHeader* (*)(ObjHeader*)> weakRefBarrier = nullptr;
std::atomic<int64_t> weakProcessingEpoch = 0;

ObjHeader* weakRefBarrierImpl(ObjHeader* weakReferee) noexcept {
    if (!weakReferee) return nullptr;
    // When weak ref barriers are enabled, marked state cannot change and the
    // object cannot be deleted.
    if (!gc::isMarked(weakReferee)) {
        return nullptr;
    }
    return weakReferee;
}

NO_INLINE ObjHeader* weakRefReadSlowPath(std::atomic<ObjHeader*>& weakReferee) noexcept {
    // reread an action to avoid register pollution outside the function
    auto barrier = weakRefBarrier.load(std::memory_order_seq_cst);

    // The referee must be reread here to avoid a possible race with the concurrent barrier disablement.
    // NOTE: at the moment the barriers are disabled in STW, meaning this all is not the case.
    //
    // Consider the following situation:
    // 1. GC thread enables the barriers and resumes mutators.
    // 2. A mutator reads obj_ into local variable and sleeps.
    // 3. GC thread finishes weak processing and disables the barriers.
    // 4. The mutator continues executing, sees that there's no more barriers (but have not yet communicated this fact with the GC)
    //    and returns object from the local variable.
    // Why reading inside the barrier code fixes it:
    // 1. We read the barrier with seq_cst and do it before reading obj_.
    // 2. If there's a barrier, we execute it, and the GC is definitely waiting for us to finish,
    //    and the object is still alive but is not marked (if it's marked, then there's nothing to go wrong)
    // 3. If there's no barrier, then the GC has already cleaned up all the weaks and we read obj_ after that happening.
    auto* weak = weakReferee.load(std::memory_order_relaxed);

    return barrier ? barrier(weak) : weak;
}

} // namespace

void gc::BarriersThreadData::onThreadRegistration() noexcept {
    if (weakRefBarrier.load(std::memory_order_acquire) != nullptr) {
        startMarkingNewObjects(GCHandle::getByEpoch(weakProcessingEpoch.load(std::memory_order_relaxed)));
    }
}

ALWAYS_INLINE void gc::BarriersThreadData::onSafePoint() noexcept {}

void gc::BarriersThreadData::startMarkingNewObjects(gc::GCHandle gcHandle) noexcept {
    RuntimeAssert(weakRefBarrier.load(std::memory_order_relaxed) != nullptr, "New allocations marking may only be requested by weak ref barriers");
    markHandle_ = gcHandle.mark();
}

void gc::BarriersThreadData::stopMarkingNewObjects() noexcept {
    RuntimeAssert(weakRefBarrier.load(std::memory_order_relaxed) == nullptr, "New allocations marking could only been requested by weak ref barriers");
    markHandle_ = std::nullopt;
}

bool gc::BarriersThreadData::shouldMarkNewObjects() const noexcept {
    return markHandle_.has_value();
}

ALWAYS_INLINE void gc::BarriersThreadData::onAllocation(ObjHeader* allocated) {
    if (compiler::concurrentWeakSweep()) {
        bool shouldMark = shouldMarkNewObjects();
        bool barriersEnabled = weakRefBarrier.load(std::memory_order_relaxed) != nullptr;
        RuntimeAssert(shouldMark == barriersEnabled, "New allocations marking must happen with and only with weak ref barriers");
        if (shouldMark) {
            auto& objectData = alloc::objectDataForObject(allocated);
            objectData.markUncontended();
            markHandle_->addObject();
        }
    }
}

void gc::EnableWeakRefBarriers(int64_t epoch) noexcept {
    auto mutators = mm::ThreadRegistry::Instance().LockForIter();
    weakProcessingEpoch.store(epoch, std::memory_order_relaxed);
    weakRefBarrier.store(weakRefBarrierImpl, std::memory_order_seq_cst);
    for (auto& mutator: mutators) {
        mutator.gc().impl().gc().barriers().startMarkingNewObjects(GCHandle::getByEpoch(epoch));
    }
}

void gc::DisableWeakRefBarriers() noexcept {
    auto mutators = mm::ThreadRegistry::Instance().LockForIter();
    weakRefBarrier.store(nullptr, std::memory_order_seq_cst);
    for (auto& mutator: mutators) {
        mutator.gc().impl().gc().barriers().stopMarkingNewObjects();
    }
}

OBJ_GETTER(gc::WeakRefRead, std::atomic<ObjHeader*>& weakReferee) noexcept {
    if (!compiler::concurrentWeakSweep()) {
        RETURN_OBJ(weakReferee.load(std::memory_order_relaxed));
    }

    // Copying the scheme from SafePoint.cpp: branch + indirect call.
    auto barrier = weakRefBarrier.load(std::memory_order_relaxed);
    ObjHeader* result;
    if (__builtin_expect(barrier != nullptr, false)) {
        result = weakRefReadSlowPath(weakReferee);
    } else {
        result = weakReferee.load(std::memory_order_relaxed);
    }
    RETURN_OBJ(result);
}
