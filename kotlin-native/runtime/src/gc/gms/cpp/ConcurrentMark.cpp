/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMark.hpp"

#include "Allocator.hpp"
#include "MarkAndSweepUtils.hpp"
#include "GCStatistics.hpp"
#include "GCImpl.hpp"

#include <optional>

using namespace kotlin;

// NOTE: seedRememberedSets drains the per-thread RememberedSet (RememberedSet.hpp); this TU is
// recompiled when kCapacity changes so its view of the buffer stays consistent with the barrier.

namespace {

class FlushActionActivator final : public mm::ExtraSafePointActionActivator<FlushActionActivator> {};

// Whether the in-flight collection is Eden. Read on the mark hot path via MarkTraits, so kept as a
// plain atomic. Set/reset by the collection driver around the mark phase (single GC thread).
std::atomic<bool> edenCollectionFlag = false;

// Liveness predicate for weak-reference processing. Unlike the default (mark bit only), during an
// Eden collection old objects are alive even though they carry no mark this cycle, so weak references
// to them must be retained. During a Full collection this coincides with the default.
struct GmsProcessWeaksTraits {
    static bool IsMarked(ObjHeader* obj) noexcept {
        auto& objectData = alloc::objectDataForObject(obj);
        if (objectData.marked()) return true;
        if (gc::mark::edenCollection() && objectData.isOld()) return true;
        return false;
    }
};

} // namespace

void gc::mark::setEdenCollection(bool eden) noexcept {
    edenCollectionFlag.store(eden, std::memory_order_release);
}

bool gc::mark::edenCollection() noexcept {
    return edenCollectionFlag.load(std::memory_order_acquire);
}

void gc::mark::ConcurrentMark::ThreadData::onSuspendForGC() noexcept {}

bool gc::mark::ConcurrentMark::ThreadData::tryLockRootSet() noexcept {
    bool expected = false;
    bool locked = rootSetLocked_.compare_exchange_strong(expected, true, std::memory_order_acq_rel);
    if (locked) {
        RuntimeLogDebug(
                {kTagGC}, "Thread %" PRIuPTR " have exclusively acquired thread %" PRIuPTR "'s root set", konan::currentThreadId(),
                threadData_.threadId());
    }
    return locked;
}

void gc::mark::ConcurrentMark::ThreadData::publish() noexcept {
    threadData_.Publish();
}

void gc::mark::ConcurrentMark::ThreadData::clearMarkFlags() noexcept {
    rootSetLocked_.store(false, std::memory_order_release);
}

void gc::mark::ConcurrentMark::ThreadData::ensureFlushActionExecuted() noexcept {
    flushAction_->ensureExecuted([this] { markQueue()->forceFlush(); });
}

void gc::mark::ConcurrentMark::ThreadData::onSafePoint() noexcept {
    FlushActionActivator::doIfActive([this] { ensureFlushActionExecuted(); });
}

void gc::mark::ConcurrentMark::setupBeforeSTW(GCHandle gcHandle) {
    gcHandle_ = gcHandle;

    lockedMutatorsList_ = mm::ThreadRegistry::Instance().LockForIter();

    parallelProcessor_.construct();
}

void gc::mark::ConcurrentMark::endMarkingEpoch() {
    parallelProcessor_.destroy();
    resetMutatorFlags();
    lockedMutatorsList_ = std::nullopt;
}

void gc::mark::ConcurrentMark::markInSTW() {
    std::unique_lock markLock(markMutex_);
    ParallelProcessor::Worker mainWorker(*parallelProcessor_);
    GCLogDebug(gcHandle().getEpoch(), "Creating main (#0) mark worker");

    // create mutator mark queues
    for (auto& thread : *lockedMutatorsList_) {
        thread.gc().impl().mark_.markQueue().construct(*parallelProcessor_);
    }

    completeMutatorsRootSet(mainWorker);

    if (edenCollection()) {
        // Feed the remembered set (old->young edges recorded by the write barrier) into the mark
        // closure as additional roots. Must happen while the world is stopped.
        seedRememberedSets(mainWorker);
    } else {
        // Full collection re-traces everything from roots, so the remembered set is not needed.
        clearRememberedSets();
    }

    barriers::enableBarriers(gcHandle().getEpoch());
    resumeTheWorld(gcHandle());

    // global root set must be collected after all the mutator's global data have been published
    collectRootSetGlobals<MarkTraits>(gcHandle(), mainWorker);

    // Mutator threads might release their internal batch at a pretty arbitrary moment (during a barrier execution with overflow).
    // So there are not so many reliable ways to track releases of new work.
    // The number of batches shared inside a parallel processor may only grow,
    // we use this number to decide when to finish the mark.
    auto everSharedBatches = parallelProcessor_->batchesEverShared();

    bool terminateInSTW = true;
    for (uint32_t iter = 0; iter < compiler::concurrentMarkMaxIterations(); ++iter) {
        GCLogDebug(gcHandle().getEpoch(), "Building mark closure (attempt #%u)", iter);
        Mark<MarkTraits>(gcHandle(), mainWorker);
        if (tryTerminateMark(everSharedBatches)) {
            terminateInSTW = false; // successfully terminated mark phase concurrently
            break;
        }
    }
    if (terminateInSTW) {
        GCLogWarning(
                gcHandle().getEpoch(), "Finishing mark closure in STW after %u concurrent attempts",
                compiler::concurrentMarkMaxIterations());
        stopTheWorld(gcHandle(), "GC stop the world: concurrent mark took too long");
        flushMutatorQueues(); // No need for mark termination lock: STW is stronger than it
        Mark<MarkTraits>(gcHandle(), mainWorker);
        // Weak processing expects barriers in the correct state even in STW
        barriers::switchToWeakProcessingBarriers();
    }

    // By this point mutator mark queues cannot be populated anymore.
    // However, some threads may still try to enqueue a marked object before they observe the barriers were disabled.
    // Thus, mark queue destruction takes place only later below (in STW).

    gc::processWeaks<GmsProcessWeaksTraits>(gcHandle(), mm::ExternalRCRefRegistry::instance());

    if (!terminateInSTW) {
        stopTheWorld(gcHandle(), "GC stop the world: prepare to sweep");
    }

    barriers::disableBarriers();

    for (auto& thread : *lockedMutatorsList_) {
        thread.gc().impl().mark_.markQueue().destroy();
    }
    endMarkingEpoch();
}

gc::GCHandle& gc::mark::ConcurrentMark::gcHandle() {
    RuntimeAssert(gcHandle_.isValid(), "GCHandle must be initialized");
    return gcHandle_;
}

void gc::mark::ConcurrentMark::completeMutatorsRootSet(MarkTraits::MarkQueue& markQueue) {
    // workers compete for mutators to collect their root set
    for (auto& thread : *lockedMutatorsList_) {
        tryCollectRootSet(thread, markQueue);
    }
}

void gc::mark::ConcurrentMark::seedRememberedSets(MarkTraits::MarkQueue& markQueue) {
    // The write barrier records candidate heap-ref slots. With a known, stable old owner the entry is
    // exact and can be drained directly. Ownerless paths and the collection-window fallback can still
    // record young-value slots whose container is young; draining such a slot inside a dead young
    // container would enqueue its referent as a root and thereby resurrect and promote dead young
    // garbage into the old generation. So conservative entries resolve each slot back to its containing
    // object and keep the referent only when the container is genuinely old -- the real source of an
    // old->young edge. This mirrors JSC/Riptide, which filters on the source object's age, while gms
    // adapts the remembered-set unit to slots.
    // Nothing recorded this cycle? Return before building the snapshot. Constructing a
    // HeapLayoutSnapshot collects and sorts every page range (O(pages log pages)) and runs inside the
    // Eden STW pause; the common minor collection has few or no old->young edges, so a large heap with
    // an empty remembered set must not pay for a snapshot it would never query.
    bool anySlots = false;
    bool anyConservativeSlots = false;
    for (auto& thread : *lockedMutatorsList_) {
        auto& rememberedSet = thread.gc().impl().barriers_.rememberedSet();
        if (!rememberedSet.empty()) {
            anySlots = true;
            anyConservativeSlots |= rememberedSet.hasConservativeEntries();
        }
    }
    if (!anySlots) return;

    std::optional<alloc::HeapLayoutSnapshot> heapLayout;
    if (anyConservativeSlots) {
        heapLayout.emplace();
    }
    // Resolve the container BEFORE dereferencing the slot. A recorded slot can go stale: the
    // collection-window barrier (see beforeHeapRefUpdate) records a slot by the value's age
    // regardless of the owner's age, so it also records slots inside young owners; a dead-young owner
    // is then reclaimed by the concurrent sweep and its cell reused, leaving the recorded slot
    // pointing at unrelated memory. Reading `*slot` first would load that garbage and mark it (a CAS
    // on a wild "mark word" -- observed crashing on read-only .text under -opt/escape-analysis).
    // containerOf() is a pure address-range lookup that never dereferences the slot, so resolving the
    // container first is always safe. We enqueue only when the container is a genuinely old, live heap
    // object -- the sole real source of an old->young edge:
    //   * young / dead-young / reclaimed-and-reused container -> not an old->young edge (a young owner
    //     is itself traced by Eden), and the slot may be stale -> skip without reading it.
    //   * container is old and live (old objects are only reclaimed by a Full, which clears the whole
    //     remembered set) -> the slot is live; read it and enqueue the referent.
    //   * container unresolvable on the custom allocator (global slot, or address outside the heap) ->
    //     globals are scanned as roots every collection, so there is no old->young edge to recover ->
    //     skip.
    //
    // resolves == false means the backend cannot recover any container (legacy/std allocator). With the
    // age filter unavailable, a stale slot could not be detected and `*slot` would be an unguarded read
    // of possibly-freed memory -- the exact afd34b6 crash. Production therefore never runs Eden on such
    // a backend: GmsCollectionPolicy::choosePolicyScope forces Full when the allocator cannot resolve
    // interior pointers (Full needs no remembered-set drain). The only remaining caller with
    // resolves == false is a test that pins the scope to Eden (setForcedScopeForTests) over a
    // controlled graph with no dead-young owners, so no recorded slot is stale and the read is safe.
    const bool resolves = heapLayout.has_value() ? heapLayout->resolvesInteriorPointers() : true;
    for (auto& thread : *lockedMutatorsList_) {
        thread.gc().impl().barriers_.rememberedSet().drainInto([&](kotlin::gc::RememberedSet::Entry entry) noexcept {
            ObjHeader** slot = entry.slot();
            if (!entry.exact() && resolves) {
                ObjHeader* container = heapLayout->containerOf(slot);
                // Skip young / dead-young / reclaimed / unresolvable containers: not an old->young edge,
                // and the slot may be stale -- so this must return before `*slot` is read.
                if (container == nullptr || !alloc::objectDataForObject(container).isOld()) return;
            }
            ObjHeader* referent = *slot;
            if (referent != nullptr && referent->heap()) {
                // MarkTraits::tryEnqueue still filters out referents that are themselves already old.
                MarkTraits::tryEnqueue(markQueue, referent);
            }
        });
    }
}

void gc::mark::ConcurrentMark::clearRememberedSets() {
    for (auto& thread : *lockedMutatorsList_) {
        thread.gc().impl().barriers_.rememberedSet().clear();
    }
}

void gc::mark::ConcurrentMark::tryCollectRootSet(mm::ThreadData& thread, MarkTraits::MarkQueue& markQueue) {
    auto& gcData = thread.gc().impl().mark_;
    if (!gcData.tryLockRootSet()) return;

    GCLogDebug(
            gcHandle().getEpoch(), "Root set collection on thread %" PRIuPTR " for thread %" PRIuPTR, konan::currentThreadId(),
            thread.threadId());
    gcData.publish();
    collectRootSetForThread<MarkTraits>(gcHandle(), markQueue, thread);
}

/** Terminates the mark loop if possible, otherwise returns `false`. */
bool gc::mark::ConcurrentMark::tryTerminateMark(std::size_t& everSharedBatches) noexcept {
    // prevent unwanted mutations (such as weak-reachable resurrection) during termination detection
    std::unique_lock markTerminationGuard(markTerminationMutex_);

    // has to happen under the termination lock guard
    flushMutatorQueues();

    // After the mutators have been forced to flush their local queues,
    // there is only one possibility for this counter to remain the same as on a previous iteration:
    // 1. Mutator local queues are empty,
    // 2. AND were empty before the flush request was made,
    // 3. AND the last attempt at completing mark closure encountered 0 new objects // FIXME this is actually redundant
    const auto nowSharedBatches = parallelProcessor_->batchesEverShared();
    if (nowSharedBatches > everSharedBatches) {
        everSharedBatches = nowSharedBatches;
        parallelProcessor_->resetForNewWork();
        return false;
    }
    RuntimeAssert(nowSharedBatches == everSharedBatches, "This number must never decrease");

    barriers::switchToWeakProcessingBarriers();
    return true;
}

void gc::mark::ConcurrentMark::flushMutatorQueues() noexcept {
    for (auto& mutator : *lockedMutatorsList_) {
        mutator.gc().impl().mark_.flushAction_.construct();
    }

    {
        FlushActionActivator flushActivator{};

        // wait all mutators flushed
        while (true) {
            bool allDone = true;
            for (auto& mutator : *lockedMutatorsList_) {
                auto& markData = mutator.gc().impl().mark_;
                if (mutator.suspensionData().suspendedOrNative()) {
                    markData.ensureFlushActionExecuted();
                } else if (!markData.flushAction_->executed()) {
                    allDone = false;
                }
            }
            if (allDone) break;
            std::this_thread::yield();
        }
    }

    // It's guaranteed by the activator that no mutator thread would access somethingFlushed_ at this point.
    for (auto& mutator : *lockedMutatorsList_) {
        mutator.gc().impl().mark_.flushAction_.destroy();
    }
}

void gc::mark::ConcurrentMark::resetMutatorFlags() {
    for (auto& mut : *lockedMutatorsList_) {
        mut.gc().impl().mark_.clearMarkFlags();
    }
}

bool gc::mark::test_support::flushActionRequested() {
    return FlushActionActivator::isActive();
}
