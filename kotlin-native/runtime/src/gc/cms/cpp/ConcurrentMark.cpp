/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMark.hpp"

#include "MarkAndSweepUtils.hpp"
#include "GCStatistics.hpp"
#include "Utils.hpp"
#include "GCImpl.hpp"

using namespace kotlin;

namespace {

class FlushActionActivator final : public mm::ExtraSafePointActionActivator<FlushActionActivator> {};

} // namespace

void gc::mark::ConcurrentMark::ThreadData::ensureFlushActionExecuted() noexcept {
    flushAction_->ensureExecuted([this] { markQueue()->forceFlush(); });
}

void gc::mark::ConcurrentMark::ThreadData::onSafePoint() noexcept {
    FlushActionActivator::doIfActive([this] { ensureFlushActionExecuted(); });
}

void gc::mark::ConcurrentMark::beginMarkingEpoch(gc::GCHandle gcHandle) {
    gcHandle_ = gcHandle;

    lockedMutatorsList_ = mm::ThreadRegistry::Instance().LockForIter();

    parallelProcessor_.construct();
}

void gc::mark::ConcurrentMark::endMarkingEpoch() {
    parallelProcessor_.destroy();
    resetMutatorFlags();
    lockedMutatorsList_ = std::nullopt;
}

void gc::mark::ConcurrentMark::runMainInSTW() {
    ParallelProcessor::Worker mainWorker(*parallelProcessor_);
    GCLogDebug(gcHandle().getEpoch(), "Creating main (#0) mark worker");

    // create mutator mark queues
    for (auto& thread : *lockedMutatorsList_) {
        thread.gc().impl().gc().mark().markQueue().construct(*parallelProcessor_);
    }

    completeMutatorsRootSet(mainWorker);

    barriers::enableBarriers(gcHandle().getEpoch());
    resumeTheWorld(gcHandle());

    // global root set must be collected after all the mutator's global data have been published
    collectRootSetGlobals<MarkTraits>(gcHandle(), mainWorker);

    // Mutator threads might release their internal batch at a pretty arbitrary moment (during a barrier execution with overflow).
    // So there are not so many reliable ways to track releases of new work.
    // The number of batches sharad inside a parallel processor may only grow,
    // we use this number to decide when to finish the mark.
    auto everSharedBatches = parallelProcessor_->batchesEverShared();
    size_t iter = 0;
    bool terminateInSTW = false;
    do {
        GCLogDebug(gcHandle().getEpoch(), "Building mark closure (attempt #%zu)", iter);
        Mark<MarkTraits>(gcHandle(), mainWorker);

        RuntimeCheck(iter <= compiler::concurrentMarkMaxIterations(), "Failed to terminate mark in STW in a single iteration");
        ++iter;
        if (iter == compiler::concurrentMarkMaxIterations()) {
            fprintf(stderr, "EMERGENCY MARK TERMINATION\n");
            GCLogWarning(gcHandle().getEpoch(), "Finishing mark closure in STW after (%zu concurrent attempts)", iter);
            stopTheWorld(gcHandle(), "GC stop the world #2: concurrent mark took too long");
            terminateInSTW = true;
        }
    } while (!tryTerminateMark(everSharedBatches));

    // By this point mutator mark queues may not be populated anymore.
    // However, some threads may still try to enqueue a marked object, before they observe the barrier disablement.
    // Thus, mark queue destruction takes place only later below.

    gc::processWeaks<DefaultProcessWeaksTraits>(gcHandle(), mm::SpecialRefRegistry::instance());

    if (!terminateInSTW) {
        // Mutator threads execute weak barrier in "native" state. This hack maes them stop with the rest of the world.
        std::unique_lock markTerminationGuard(markTerminationMutex_);

        stopTheWorld(gcHandle(), "GC stop the world #2: prepare to sweep");
    }

    barriers::disableBarriers();

    for (auto& thread : *lockedMutatorsList_) {
        thread.gc().impl().gc().mark().markQueue().destroy();
    }
    endMarkingEpoch();
}

void gc::mark::ConcurrentMark::runOnMutator(mm::ThreadData&) {
    // no-op
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

void gc::mark::ConcurrentMark::tryCollectRootSet(mm::ThreadData& thread, MarkTraits::MarkQueue& markQueue) {
    auto& gcData = thread.gc().impl().gc();
    if (!gcData.tryLockRootSet()) return;

    GCLogDebug(gcHandle().getEpoch(), "Root set collection on thread %d for thread %d", konan::currentThreadId(), thread.threadId());
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
    // there is only on possibility for this counter to remain the same as on a previous iteration:
    // 1. Mutator local queues are empty,
    // 2. AND were empty before the flush request was made,
    // 3. AND the last attempt at completing mark closure encountered 0 new objects // FIXME this is actually redundant
    const auto nowSharedBatches = parallelProcessor_->batchesEverShared();
    if (nowSharedBatches > everSharedBatches) {
        everSharedBatches = nowSharedBatches;
        parallelProcessor_->resetForNewWork();
        return false;
    }
    RuntimeAssert(nowSharedBatches == everSharedBatches, "This number must decrease");

    barriers::switchToWeakProcessingBarriers();
    return true;
}

void gc::mark::ConcurrentMark::flushMutatorQueues() noexcept {
    for (auto& mutator : *lockedMutatorsList_) {
        mutator.gc().impl().gc().mark().flushAction_.construct();
    }

    {
        FlushActionActivator flushActivator{};

        // wait all mutators flushed
        while (true) {
            bool allDone = true;
            for (auto& mutator : *lockedMutatorsList_) {
                auto& markData = mutator.gc().impl().gc().mark();
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
        mutator.gc().impl().gc().mark().flushAction_.destroy();
    }
}

void gc::mark::ConcurrentMark::resetMutatorFlags() {
    for (auto& mut : *lockedMutatorsList_) {
        mut.gc().impl().gc().clearMarkFlags();
    }
}

bool gc::mark::test_support::flushActionRequested() {
    return FlushActionActivator::isActive();
}
