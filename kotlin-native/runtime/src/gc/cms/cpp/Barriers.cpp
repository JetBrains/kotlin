/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Barriers.hpp"

#include <algorithm>
#include <atomic>

#include "GCImpl.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

namespace {

enum class BarriersPhase {
    /** Normal execution */
    kDisabled,
    /** During mark closure building */
    kMarkClosure,
    /** After the mark closure is built, but before the mark completed (during weak ref processing) */
    kWeakProcessing
};

const char* toString(BarriersPhase barriersPhase) {
    switch (barriersPhase) {
        case BarriersPhase::kDisabled:
            return "none";
        case BarriersPhase::kMarkClosure:
            return "mark";
        case BarriersPhase::kWeakProcessing:
            return "weak-processing";
    }
}

std::atomic barriersPhase = BarriersPhase::kDisabled;
std::atomic<int64_t> markingEpoch = 0;

BarriersPhase currentPhase() noexcept {
    return barriersPhase.load(std::memory_order_acquire);
}

BarriersPhase currentPhaseRelaxed() noexcept {
    return barriersPhase.load(std::memory_order_relaxed);
}

ALWAYS_INLINE void assertPhase(BarriersPhase actual, BarriersPhase expected) noexcept {
    RuntimeAssert(actual == expected, "Barriers phase: expected %s but observed %s", toString(expected), toString(actual));
}

ALWAYS_INLINE void assertPhase(BarriersPhase expected) noexcept {
    assertPhase(currentPhaseRelaxed(), expected);
}

ALWAYS_INLINE void assertPhaseNot(BarriersPhase expected) noexcept {
    RuntimeAssert(currentPhaseRelaxed() != expected, "Barriers phase: phase %s not expected", toString(expected));
}

void switchPhase(BarriersPhase from, BarriersPhase to) noexcept {
    auto prev = barriersPhase.exchange(to, std::memory_order_release);
    assertPhase(prev, from);
}

auto& markDispatcher() noexcept {
    return mm::GlobalData::Instance().gc().impl().gc().mark();
}

inline constexpr auto kTagBarriers = logging::Tag::kBarriers;
#define BarriersLogDebug(phase, format, ...) RuntimeLogDebug({kTagBarriers}, "[%s]" format, toString(phase), ##__VA_ARGS__)

} // namespace

void gc::barriers::BarriersThreadData::onThreadRegistration() noexcept {
    if (currentPhase() != BarriersPhase::kDisabled) {
        startMarkingNewObjects(GCHandle::getByEpoch(markingEpoch.load(std::memory_order_relaxed)));
    }
}

void gc::barriers::BarriersThreadData::startMarkingNewObjects(gc::GCHandle gcHandle) noexcept {
    assertPhaseNot(BarriersPhase::kDisabled);
    markHandle_ = gcHandle.mark();
}

void gc::barriers::BarriersThreadData::stopMarkingNewObjects() noexcept {
    assertPhase(BarriersPhase::kDisabled);
    markHandle_ = std::nullopt;
}

bool gc::barriers::BarriersThreadData::shouldMarkNewObjects() const noexcept {
    return markHandle_.has_value();
}

PERFORMANCE_INLINE void gc::barriers::BarriersThreadData::onAllocation(ObjHeader* allocated) {
    BarriersLogDebug(currentPhaseRelaxed(), "Allocation %p", allocated);
    if (shouldMarkNewObjects()) {
        auto& objectData = alloc::objectDataForObject(allocated);
        objectData.markUncontended();
        markHandle_->addObject();
    }
}

void gc::barriers::enableBarriers(int64_t epoch) noexcept {
    auto mutators = mm::ThreadRegistry::Instance().LockForIter();
    markingEpoch.store(epoch, std::memory_order_relaxed);
    switchPhase(BarriersPhase::kDisabled, BarriersPhase::kMarkClosure);
    for (auto& mutator : mutators) {
        mutator.gc().impl().gc().barriers().startMarkingNewObjects(GCHandle::getByEpoch(epoch));
    }
}

void gc::barriers::switchToWeakProcessingBarriers() noexcept {
    switchPhase(BarriersPhase::kMarkClosure, BarriersPhase::kWeakProcessing);
}

void gc::barriers::disableBarriers() noexcept {
    auto mutators = mm::ThreadRegistry::Instance().LockForIter();
    switchPhase(BarriersPhase::kWeakProcessing, BarriersPhase::kDisabled);
    for (auto& mutator : mutators) {
        mutator.gc().impl().gc().barriers().stopMarkingNewObjects();
    }
}

namespace {

// TODO decide whether it's really beneficial to NO_INLINE the slow path
NO_INLINE void beforeHeapRefUpdateSlowPath(mm::DirectRefAccessor ref, ObjHeader* value) noexcept {
    auto prev = ref.load();
    if (prev != nullptr && prev->heap()) {
        // TODO Redundant if the destination object is black.
        //      Yet at the moment there is now efficient way to distinguish black and gray objects.

        // TODO perhaps it would be better to pass the thread data from outside
        auto& threadData = *mm::ThreadRegistry::Instance().CurrentThreadData();
        auto& markQueue = *threadData.gc().impl().gc().mark().markQueue();
        gc::mark::ConcurrentMark::MarkTraits::tryEnqueue(markQueue, prev);
        // No need to add the marked object in statistics here.
        // Objects will be counted on dequeue.
    }
}

} // namespace

PERFORMANCE_INLINE void gc::barriers::beforeHeapRefUpdate(mm::DirectRefAccessor ref, ObjHeader* value) noexcept {
    auto phase = currentPhase();
    BarriersLogDebug(phase, "Write *%p <- %p (%p overwritten)", ref.location(), value, ref.load());
    if (__builtin_expect(phase == BarriersPhase::kMarkClosure, false)) {
        beforeHeapRefUpdateSlowPath(ref, value);
    }
}

namespace {

/**
 * Before the mark closure is built, every weak read may resurrect a weakly-reachable object.
 * Thus, the referent must be pushed in a mark queue, in case it wold be resurrected behind the mark front.
 */
NO_INLINE void weakRefReadInMarkSlowPath(ObjHeader* weakReferee) noexcept {
    assertPhase(BarriersPhase::kMarkClosure);
    auto& threadData = *mm::ThreadRegistry::Instance().CurrentThreadData();
    auto& markQueue = *threadData.gc().impl().gc().mark().markQueue();
    gc::mark::ConcurrentMark::MarkTraits::tryEnqueue(markQueue, weakReferee);
}

/** After the mark closure is built, but weak refs are not yet nulled out, every weak read should check if the weak referent is marked. */
NO_INLINE ObjHeader* weakRefReadInWeakSweepSlowPath(ObjHeader* weakReferee) noexcept {
    assertPhase(BarriersPhase::kWeakProcessing);
    if (!gc::isMarked(weakReferee)) {
        return nullptr;
    }
    return weakReferee;
}

} // namespace

PERFORMANCE_INLINE ObjHeader* gc::barriers::weakRefReadBarrier(std::atomic<ObjHeader*>& weakReferee) noexcept {
    if (__builtin_expect(currentPhase() != BarriersPhase::kDisabled, false)) {
        // Mark dispatcher requires weak reads be protected by the following:
        auto weakReadProtector = markDispatcher().weakReadProtector();
        AssertThreadState(ThreadState::kRunnable);

        auto weak = weakReferee.load(std::memory_order_relaxed);
        if (!weak) return nullptr;

        auto phase = currentPhase();
        BarriersLogDebug(phase, "Weak read %p", weak);

        if (__builtin_expect(phase == BarriersPhase::kMarkClosure, false)) {
            weakRefReadInMarkSlowPath(weak);
        } else {
            if (__builtin_expect(phase == BarriersPhase::kWeakProcessing, false)) {
                // TODO reread the referee here under the barrier guard
                //      if `disableBarriers` would be possible outside of STW
                return weakRefReadInWeakSweepSlowPath(weak);
            }
        }
        return weak;
    }

    return weakReferee.load(std::memory_order_relaxed);
}
