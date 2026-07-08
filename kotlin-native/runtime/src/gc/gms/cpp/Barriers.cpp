/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Barriers.hpp"

#include <algorithm>
#include <atomic>

#include "GCImpl.hpp"
#include "Memory.h"
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
std::atomic<uint64_t> markingEpoch = 0;

// Whether the generational write barrier (remembered-set recording) is active. See header.
std::atomic<bool> generationalActiveFlag = false;

// Whether a collection (mark + possibly-concurrent sweep) is currently in progress. See header.
std::atomic<bool> collectionInProgressFlag = false;

// Set when the next collection must be Full (remembered-set overflow or explicit GC.collect()).
std::atomic<bool> forceFullCollectionFlag = false;

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
    return mm::GlobalData::Instance().gc().impl().mark_;
}

inline constexpr auto kTagBarriers = logging::Tag::kBarriers;
#define BarriersLogDebug(phase, format, ...) RuntimeLogDebug({kTagBarriers}, "[%s]" format, toString(phase), ##__VA_ARGS__)

// Slow path of the generational barrier: append the written slot to the current thread's
// remembered set (SSB). Kept out of line so the enabled-check on the barrier fast path stays tiny.
// (RememberedSet::kCapacity is consumed here; see RememberedSet.hpp.)
NO_INLINE void recordRememberedSlotForCurrentThread(ObjHeader** slot, bool exact) noexcept {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    if (threadData == nullptr) {
        // Stores from unattached threads cannot create old->young edges into the Kotlin heap that
        // would be missed: such stores go through the ExternalRCRef machinery, handled elsewhere.
        return;
    }
    if (__builtin_expect(!threadData->gc().impl().barriers_.rememberedSet().record(slot, exact), false)) {
        // Buffer full: force the next collection to be Full, which does not need the remembered set.
        gc::barriers::requestFullCollection();
    }
}

} // namespace

void gc::barriers::BarriersThreadData::onThreadRegistration() noexcept {
    if (currentPhase() != BarriersPhase::kDisabled) {
        startMarkingNewObjects(GCHandle::getByEpoch(markingEpoch.load(std::memory_order_relaxed)));
    }
}

void gc::barriers::BarriersThreadData::onThreadUnregistration() noexcept {
    if (!rememberedSet_.empty()) {
        // The per-thread buffer dies with ThreadData. Preserve correctness by making the next
        // collection Full, which re-traces old objects and therefore does not need this buffer.
        requestFullCollection();
        rememberedSet_.clear();
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

void gc::barriers::enableBarriers(uint64_t epoch) noexcept {
    auto mutators = mm::ThreadRegistry::Instance().LockForIter();
    markingEpoch.store(epoch, std::memory_order_relaxed);
    switchPhase(BarriersPhase::kDisabled, BarriersPhase::kMarkClosure);
    for (auto& mutator : mutators) {
        mutator.gc().impl().barriers_.startMarkingNewObjects(GCHandle::getByEpoch(epoch));
    }
}

void gc::barriers::switchToWeakProcessingBarriers() noexcept {
    switchPhase(BarriersPhase::kMarkClosure, BarriersPhase::kWeakProcessing);
}

void gc::barriers::disableBarriers() noexcept {
    auto mutators = mm::ThreadRegistry::Instance().LockForIter();
    switchPhase(BarriersPhase::kWeakProcessing, BarriersPhase::kDisabled);
    for (auto& mutator : mutators) {
        mutator.gc().impl().barriers_.stopMarkingNewObjects();
    }
}

namespace {

// TODO decide whether it's really beneficial to NO_INLINE the slow path
NO_INLINE void beforeHeapRefUpdateSlowPath(mm::DirectRefAccessor ref, ObjHeader* value, bool loadAtomic) noexcept {
    AssertThreadState(ThreadState::kRunnable);

    ObjHeader* prev;
    if (loadAtomic) {
        prev = ref.loadAtomic(std::memory_order_relaxed);
    } else {
        prev = ref.load();
    }

    // SATB deletion barrier: remember the overwritten (previous) referent.
    const bool deletion = prev != nullptr && prev->heap();
    // Eden insertion barrier: during a *concurrent* Eden mark the new referent may become reachable
    // only through an old object, which Eden does not trace. The remembered set only covers edges
    // created before the STW snapshot, so edges created during concurrent marking must be caught here
    // by marking the new referent directly. (No-op for young containers/values already reachable;
    // MarkTraits::tryEnqueue filters out old referents.)
    const bool edenInsertion = gc::mark::edenCollection() && value != nullptr && value->heap();

    if (!deletion && !edenInsertion) {
        return;
    }

    // TODO perhaps it would be better to pass the thread data from outside
    auto& threadData = *mm::ThreadRegistry::Instance().CurrentThreadData();
    auto& markQueue = *threadData.gc().impl().mark_.markQueue();
    if (deletion) {
        gc::mark::ConcurrentMark::MarkTraits::tryEnqueue(markQueue, prev);
    }
    if (edenInsertion) {
        gc::mark::ConcurrentMark::MarkTraits::tryEnqueue(markQueue, value);
    }
    // No need to add the marked object in statistics here.
    // Objects will be counted on dequeue.
}

} // namespace

void gc::barriers::setGenerationalActive(bool active) noexcept {
    generationalActiveFlag.store(active, std::memory_order_relaxed);
}

bool gc::barriers::generationalActive() noexcept {
    return generationalActiveFlag.load(std::memory_order_relaxed);
}

void gc::barriers::setCollectionInProgress(bool inProgress) noexcept {
    collectionInProgressFlag.store(inProgress, std::memory_order_release);
}

bool gc::barriers::collectionInProgress() noexcept {
    return collectionInProgressFlag.load(std::memory_order_acquire);
}

void gc::barriers::requestFullCollection() noexcept {
    forceFullCollectionFlag.store(true, std::memory_order_release);
}

bool gc::barriers::takeFullCollectionRequest() noexcept {
    return forceFullCollectionFlag.exchange(false, std::memory_order_acq_rel);
}

void gc::barriers::clearFullCollectionRequestForTests() noexcept {
    forceFullCollectionFlag.store(false, std::memory_order_relaxed);
}

namespace {

// JSC-style source-age filter (the R1 fix). Decides whether a store must be recorded in the
// generational remembered set. The remembered set exists solely to find YOUNG objects that are
// reachable only through an OLD object at Eden time; so we need to record only genuine old->young
// edges. Recording young->young (the dominant churn: every constructor field store) is what
// overflowed the fixed-capacity SSB and forced every collection to Full -- defeating Eden entirely.
//
//   owner == nullptr : container unknown (static/global slots, old runtime helper paths, C++ helpers).
//                      Globals are scanned as roots every collection, but to stay correct for any
//                      non-root owner-less store we conservatively record young values; the drain
//                      re-filters by resolving the container's age. Generated instance/array stores,
//                      including instance volatile/atomic stores, pass the owner and avoid this path.
//   owner not heap   : stack-allocated / permanent container -> reached via the root set, never
//                      needs remembering.
//   owner young      : young->young (or young->old) -> the young owner is itself traced by Eden, so
//                      its outgoing edges are followed directly; no remembering. (Overflow fix.)
//   owner old        : genuine old-gen source. Record unless the value is itself already old
//                      (old->old is irrelevant to Eden, which never traces old objects).
ALWAYS_INLINE bool shouldRememberOldToYoung(ObjHeader* owner, ObjHeader* value) noexcept {
    if (owner == nullptr) {
        // Owner unknown -- the store came through a path that does not thread the container (e.g.
        // static/global stores, legacy runtime helpers, or C++ RefField helpers). We cannot apply the
        // source-age filter, so fall back to the VALUE's age:
        // an old->young edge always stores a young value, so recording every young-value store still
        // captures all real edges, while an old-value store cannot create one and is dropped. This
        // keeps the remembered set from being flooded by old-value volatile writes (young->young churn
        // with a young value is still over-recorded and filtered later at the drain).
        return !alloc::objectDataForObject(value).isOld();
    }
    if (!owner->heap()) return false;
    if (!alloc::objectDataForObject(owner).isOld()) return false;
    return !alloc::objectDataForObject(value).isOld();
}

} // namespace

PERFORMANCE_INLINE void gc::barriers::beforeHeapRefUpdate(
        ObjHeader* owner, mm::DirectRefAccessor ref, ObjHeader* value, bool loadAtomic) noexcept {
    // Generational remembered-set (SSB) barrier. Record the overwritten slot only for a genuine
    // old->young edge (see shouldRememberOldToYoung) so the SSB stays bounded and Eden can run.
    // Gated on generational mode being active. The gms GC enables it once at construction and never
    // clears it, so for essentially the whole process lifetime this branch is taken -- do NOT hint it
    // not-taken. (An earlier __builtin_expect(generationalActive(), false) here inverted the
    // prediction and pushed the recording logic onto the cold path for every heap-ref store, the
    // hottest mutator path.) The flag still guards the brief pre-GC-construction startup window.
    if (generationalActive()) {
        if (value != nullptr && value->heap()) {
            // Between collections, ages are stable and the cheap source-age filter is exact. While a
            // collection is in progress the concurrent sweep promotes survivors one at a time, so an
            // owner's "old" bit is transiently unstable: a store into an owner about to be promoted
            // this cycle could read it as young and be wrongly skipped, dropping a genuine old->young
            // edge (the owner is old, hence untraced, by the next Eden). During the collection window
            // we therefore record by the VALUE's age; the STW drain (seedRememberedSets) re-resolves
            // the container age once promotions have settled, so this stays correct and only
            // over-records transiently. See the R3 concurrent-sweep audit.
            //
            // The owner's *heap-ness*, unlike its old bit, is stable across a collection, so both paths
            // require a heap (or unknown) owner. A non-heap owner -- a stack/arena object produced by
            // escape analysis, a permanent, or a global -- is reached through the root set every
            // collection and is never promoted, so it can hold no old->young edge worth remembering.
            // Recording its slot only wastes remembered-set space and (for an escape-analyzed owner
            // whose frame later unwinds) risks a stale slot; owner == nullptr means "container unknown"
            // and is handled conservatively by the drain.
            const bool ownerRemembers = owner == nullptr || owner->heap();
            const bool inCollection = __builtin_expect(collectionInProgress(), false);
            const bool remember =
                    ownerRemembers && (inCollection ? !alloc::objectDataForObject(value).isOld() : shouldRememberOldToYoung(owner, value));
            if (remember) {
                // Stable, known-owner old->young stores are exact: the owner was old when recorded,
                // and Eden never reclaims old objects, so the STW drain can safely read this slot
                // without building a heap-layout snapshot. Ownerless stores and collection-window
                // stores remain conservative and are validated by container recovery.
                const bool exact = owner != nullptr && !inCollection;
                recordRememberedSlotForCurrentThread(ref.location(), exact);
            }
        }
    }

    auto phase = currentPhase();
    BarriersLogDebug(phase, "Write *%p <- %p (%p overwritten)", ref.location(), value, ref.load());
    if (__builtin_expect(phase == BarriersPhase::kMarkClosure, false)) {
        beforeHeapRefUpdateSlowPath(ref, value, loadAtomic);
    }
}

PERFORMANCE_INLINE gc::barriers::ExternalRCRefReleaseGuard::Impl::Impl(mm::DirectRefAccessor ref) noexcept {
    // Can be called with any possible thread state: kotlin, native, unattached thread.
    // This guard synchronizes with the `ConcurrentMark` via the ThreadRegistry lock.
    // It must be done before the barriers phase check.
    if (mm::ThreadRegistry::IsCurrentThreadRegistered()) {
        // If the thread is registered, just ensure, that the root set mutation happens will happen in the runnable state
        stateGuard_ = ThreadStateGuard{ThreadState::kRunnable, true};
        // NOTE that this barrier must be executed before the RC decrement.
        // An RC-ref release stores nullptr (not a young pointer), so no old->young edge is created;
        // owner is irrelevant here (nullptr).
        beforeHeapRefUpdate(nullptr, ref, nullptr, false);
    } else {
        // In case of an unregistered thread, just do thing outside the mark phase.
        // NOTE This code can be called from quite an unexpected places (such as TLS destructors).
        // One can't simply register the thread from here.
        markMutex_ = markDispatcher().markMutex();
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
    auto& markQueue = *threadData.gc().impl().mark_.markQueue();
    gc::mark::ConcurrentMark::MarkTraits::tryEnqueue(markQueue, weakReferee);
}

/** After the mark closure is built, but weak refs are not yet nulled out, every weak read should check if the weak referent is marked. */
NO_INLINE ObjHeader* weakRefReadInWeakSweepSlowPath(ObjHeader* weakReferee) noexcept {
    assertPhase(BarriersPhase::kWeakProcessing);
    // gc::isMarked is Eden-aware: during an Eden collection it already reports old survivors as live
    // (they carry no mark this cycle but are implicitly alive), so a weak to an old referent is kept
    // and only weaks to dead young objects are nulled. Mirrors GmsProcessWeaksTraits::IsMarked.
    if (gc::isMarked(weakReferee)) {
        return weakReferee;
    }
    return nullptr;
}

} // namespace

PERFORMANCE_INLINE ObjHeader* gc::barriers::weakRefReadBarrier(std_support::atomic_ref<ObjHeader*> weakReferee) noexcept {
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
