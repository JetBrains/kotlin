/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Barriers.hpp"

#include "GCImpl.hpp"
#include "SafePoint.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

#if __has_feature(thread_sanitizer)
#include <sanitizer/tsan_interface.h>
#endif

using namespace kotlin;

namespace {

gc::barriers::BarriersThreadData::ActualizeAction actualizeThreadDataAction{};

gc::barriers::internal::BarriersControlProto barriersControlProto{};

template<typename ActionOnProto, typename CheckOnThread>
void ensureEachThread(ActionOnProto&& action, CheckOnThread&& check) {
    auto threads = mm::ThreadRegistry::Instance().LockForIter();
    action(barriersControlProto);
    actualizeThreadDataAction.ensurePerformed(threads);
    for (auto& t: threads) {
        RuntimeAssert(check(t.gc().impl().gc().barriers()), "Threads state must be actualized after the action");
    }
}

inline constexpr auto kTagBarriers = logging::Tag::kBarriers;

#define BarriersLogDebug(active, format, ...) RuntimeLogDebug({kTagBarriers}, "%s" format, active ? "[active] " : "", ##__VA_ARGS__)

} // namespace


ALWAYS_INLINE void gc::barriers::internal::BarriersControl::checkInvariants() noexcept {
    if (!compiler::runtimeAssertsEnabled()) return;
    if (markNewObjects_ || concurrentMarkBarriers_ || weakProcessingBarriers_) {
        RuntimeAssert(gcHandle_.isValid(), "A valid GC handle required");
    }
    if (concurrentMarkBarriers_ || weakProcessingBarriers_) {
        RuntimeAssert(markNewObjects_, "New objects marking must also be enabled");
    }
}

gc::barriers::internal::BarriersControl gc::barriers::internal::BarriersControlProto::getValues() const noexcept {
    std::shared_lock lock(mutex_);
    return *reinterpret_cast<const BarriersControl*>(this);
}

void gc::barriers::internal::BarriersControlProto::beginMarkingEpoch(GCHandle gcHandle) noexcept {
    std::unique_lock lock(mutex_);
    gcHandle_ = gcHandle;
    RuntimeLogDebug({kTagBarriers}, "Marking epoch #%" PRIu64 " begun", gcHandle.getEpoch());
    checkInvariants();
}

void gc::barriers::internal::BarriersControlProto::endMarkingEpoch() noexcept {
    std::unique_lock lock(mutex_);
    markNewObjects_ = false;
    RuntimeLogDebug({kTagBarriers}, "Marking epoch #%" PRIu64 " ended", gcHandle_.getEpoch());
    gcHandle_ = GCHandle::invalid();
    checkInvariants();
}

void gc::barriers::internal::BarriersControlProto::enableConcurrentMarkBarriers() noexcept {
    std::unique_lock lock(mutex_);
    concurrentMarkBarriers_ = true;
    markNewObjects_ = true;
    RuntimeLogDebug({kTagBarriers}, "Concurrent mark barriers enabled");
    checkInvariants();
}

void gc::barriers::internal::BarriersControlProto::disableConcurrentMarkBarriers() noexcept {
    std::unique_lock lock(mutex_);
    concurrentMarkBarriers_ = false;
    // continue to mark new objects as it may still be required for weak processing etc.
    RuntimeLogDebug({kTagBarriers}, "Concurrent mark barriers disabled");
    checkInvariants();
}

void gc::barriers::internal::BarriersControlProto::enableWeakProcessingBarriers() noexcept {
    std::unique_lock lock(mutex_);
    weakProcessingBarriers_ = true;
    markNewObjects_ = true;
    RuntimeLogDebug({kTagBarriers}, "Weak processing barriers enabled");
    checkInvariants();
}

void gc::barriers::internal::BarriersControlProto::disableWeakProcessingBarriers() noexcept {
    std::unique_lock lock(mutex_);
    weakProcessingBarriers_ = false;
    // continue to mark new objects as it may still be required for concurrent mark etc.
    RuntimeLogDebug({kTagBarriers}, "Weak processing barriers disabled");
    checkInvariants();
}


mm::OncePerThreadAction<gc::barriers::BarriersThreadData::ActualizeAction>::ThreadData&
gc::barriers::BarriersThreadData::ActualizeAction::getUtilityData(mm::ThreadData& threadData) {
    return threadData.gc().impl().gc().barriers().actualizeActionData_;
}

void gc::barriers::BarriersThreadData::ActualizeAction::action(mm::ThreadData& threadData) noexcept {
    threadData.gc().impl().gc().barriers().actualizeFrom(barriersControlProto.getValues());
}


gc::barriers::BarriersThreadData::BarriersThreadData(mm::ThreadData& threadData)
    : base_(threadData), actualizeActionData_(actualizeThreadDataAction, threadData) {
    // No point in actualization here. Properly synchronized actualization will happen on thread registration.
}

void gc::barriers::BarriersThreadData::actualizeFrom(gc::barriers::internal::BarriersControl proto) noexcept {
    gcHandle_ = proto.gcHandle();

    bool wasMarkingNewObjects = markNewObjects_;
    bool startsMarkingNewObjects = proto.markNewObjects();
    if (!wasMarkingNewObjects && startsMarkingNewObjects) {
        markHandle_ = gcHandle_.mark();
    } else if (wasMarkingNewObjects && !startsMarkingNewObjects) {
        markHandle_ = GCHandle::invalid().mark();
    }
    markNewObjects_ = startsMarkingNewObjects;

    concurrentMarkBarriers_ = proto.concurrentMarkBarriers();
    weakProcessingBarriers_ = proto.weakProcessingBarriers();

    RuntimeLogDebug({kTagBarriers}, "Thread #%d data actualized", base_.threadId());
}

void gc::barriers::BarriersThreadData::onThreadRegistration() noexcept {
    actualizeFrom(barriersControlProto.getValues());
}

ALWAYS_INLINE void gc::barriers::BarriersThreadData::onSafePoint() noexcept {
    actualizeActionData_.onSafePoint();
}

ALWAYS_INLINE void gc::barriers::BarriersThreadData::onAllocation(ObjHeader* allocated) noexcept {
    bool shouldMark = markNewObjects();
    RuntimeAssert(shouldMark == barriersControlProto.markNewObjects(), "Thread barriers data must be synchronized with the proto");
    BarriersLogDebug(shouldMark, "Allocation %p", allocated);
    if (shouldMark) {
        auto& objectData = alloc::objectDataForObject(allocated);
        objectData.markUncontended();
        markHandle_.addObject();
    }

    // TODO Such a barrier may also help to avoid some page-faults in faulty programs with unsafe object publication
    //      even in absence of concurrent GC.
    //      Consider moving into the common allocation path.
#if __has_feature(thread_sanitizer)
    // The fence bellow orders the object's initialization before it's publication.
    // Which is important for concurrent mark barriers in order to observe correctly initialized mark-word.
    // However, TSAN doesn't support atomic_thread_fence, so we have to do some other release here.
    __tsan_release(allocated);
#endif
    std::atomic_thread_fence(std::memory_order_release);
}

ALWAYS_INLINE void gc::barriers::BarriersThreadData::beforeHeapRefUpdate(mm::DirectRefAccessor ref, ObjHeader* value) noexcept {
    if (__builtin_expect(concurrentMarkBarriers(), false)) {
        beforeHeapRefUpdateSlowPath(ref, value);
    } else {
        BarriersLogDebug(false, "Write *%p <- %p (%p overwritten)", ref.location(), value, ref.load());
    }
}

// TODO decide whether it's beneficial to NO_INLINE the slow path
void gc::barriers::BarriersThreadData::beforeHeapRefUpdateSlowPath(mm::DirectRefAccessor ref, ObjHeader* value) noexcept {
    auto prev = ref.loadAtomic(std::memory_order_consume);
    BarriersLogDebug(true, "Write *%p <- %p (%p overwritten)", ref.location(), value, prev);
    if (prev != nullptr) {
#if __has_feature(thread_sanitizer)
        // Tell TSAN, that we acquire here the object's memory,
        // released previously on allocation with atomic_thread_fence and __tsan_release workaround.
        __tsan_acquire(prev);
#endif
        // FIXME Redundant if the destination object is black.
        //       Yet at the moment there is now efficient way to distinguish black and gray objects.

        auto& objectData = alloc::objectDataForObject(prev);

        // TODO This is just a dummy. Replace with enqueueing when thread-local mark queues will be implemented.
        bool marked = objectData.tryMark();

        if (marked) {
            // TODO maybe we could go branch-less?
            markHandle_.addObject();
        }
    }
}

ALWAYS_INLINE OBJ_GETTER(gc::barriers::BarriersThreadData::weakRefReadBarrier, ObjHeader* weakReferee) noexcept {
    if (compiler::concurrentWeakSweep()) {
        bool barrierActive = weakProcessingBarriers();
        BarriersLogDebug(barrierActive, "Weak read: referee = %p", weakReferee);
        if (__builtin_expect(barrierActive, false)) {
            RETURN_RESULT_OF(weakRefReadBarrierSlowPath, weakReferee);
        }
    }

    RETURN_OBJ(weakReferee);
}

// TODO decide whether it's beneficial to NO_INLINE the slow path
OBJ_GETTER(gc::barriers::BarriersThreadData::weakRefReadBarrierSlowPath, ObjHeader* weakReferee) noexcept {
    if (!weakReferee) return nullptr;
    // When weak ref barriers are enabled, marked state cannot change and the
    // object cannot be deleted.
    if (!gc::isMarked(weakReferee)) {
        return nullptr;
    }
    return weakReferee;
}


void gc::barriers::beginMarkingEpoch(GCHandle gcHandle) noexcept {
    ensureEachThread(
            [=](internal::BarriersControlProto& barrierControl) { barrierControl.beginMarkingEpoch(gcHandle); },
            [=](BarriersThreadData& barriersThreadData) {
                return barriersThreadData.gcHandle().getEpoch() == gcHandle.getEpoch();
            }
    );
}

void gc::barriers::endMarkingEpoch() noexcept {
    ensureEachThread(
            [](internal::BarriersControlProto& barrierControl) { barrierControl.endMarkingEpoch(); },
            [](BarriersThreadData& barriersThreadData) {
                return !barriersThreadData.gcHandle().isValid() &&
                        !barriersThreadData.markNewObjects() &&
                        !barriersThreadData.concurrentMarkBarriers() &&
                        !barriersThreadData.weakProcessingBarriers();
            }
    );
}

void gc::barriers::enableMarkBarriers() noexcept {
    ensureEachThread(
            [](internal::BarriersControlProto& barrierControl) { barrierControl.enableConcurrentMarkBarriers(); },
            [](BarriersThreadData& barriersThreadData) { return barriersThreadData.concurrentMarkBarriers(); }
    );
}

void gc::barriers::disableMarkBarriers() noexcept {
    ensureEachThread(
            [](internal::BarriersControlProto& barrierControl) { barrierControl.disableConcurrentMarkBarriers(); },
            [](BarriersThreadData& barriersThreadData) { return !barriersThreadData.concurrentMarkBarriers(); }
    );
}

void gc::barriers::enableWeakRefBarriers() noexcept {
    ensureEachThread(
            [](internal::BarriersControlProto& barrierControl) { barrierControl.enableWeakProcessingBarriers(); },
            [](BarriersThreadData& barriersThreadData) { return barriersThreadData.weakProcessingBarriers(); }
    );
}

void gc::barriers::disableWeakRefBarriers() noexcept {
    ensureEachThread(
            [](internal::BarriersControlProto& barrierControl) { barrierControl.disableWeakProcessingBarriers(); },
            [](BarriersThreadData& barriersThreadData) { return !barriersThreadData.weakProcessingBarriers(); }
    );
}
