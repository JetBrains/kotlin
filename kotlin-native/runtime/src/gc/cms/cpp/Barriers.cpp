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
    auto* weak = weakReferee.load(std::memory_order_relaxed);
    return barrier ? barrier(weak) : weak;
}

void waitForThreadsToReachCheckpoint() {
    // Reset checkpoint on all threads.
    for (auto& thr : mm::ThreadRegistry::Instance().LockForIter()) {
        thr.gc().impl().gc().barriers().resetCheckpoint();
    }

    mm::SafePointActivator safePointActivator;

    // Disable new threads coming and going.
    auto threads = mm::ThreadRegistry::Instance().LockForIter();
    // And wait for all threads to either have passed safepoint or to be in the native state.
    // Either of these mean that none of them are inside a weak reference accessing code.
    while (!std::all_of(threads.begin(), threads.end(), [](mm::ThreadData& thread) noexcept {
        return thread.gc().impl().gc().barriers().visitedCheckpoint() || thread.suspensionData().suspendedOrNative();
    })) {
        std::this_thread::yield();
    }
}

} // namespace

void gc::BarriersThreadData::onCheckpoint() noexcept {
    visitedCheckpoint_.store(true, std::memory_order_release);
}

void gc::BarriersThreadData::resetCheckpoint() noexcept {
    visitedCheckpoint_.store(false, std::memory_order_release);
}

bool gc::BarriersThreadData::visitedCheckpoint() const noexcept {
    return visitedCheckpoint_.load(std::memory_order_acquire);
}

void gc::EnableWeakRefBarriers() noexcept {
    weakRefBarrier.store(weakRefBarrierImpl, std::memory_order_seq_cst);
}

void gc::DisableWeakRefBarriers() noexcept {
    weakRefBarrier.store(nullptr, std::memory_order_seq_cst);
    waitForThreadsToReachCheckpoint();
}

OBJ_GETTER(kotlin::gc::WeakRefRead, std::atomic<ObjHeader*>& weakReferee) noexcept {
    // TODO: Make this work with GCs that can stop thread at any point.

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
