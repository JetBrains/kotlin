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
    for (auto& thread: *lockedMutatorsList_) {
        thread.gc().impl().gc().mark().markQueue().construct(*parallelProcessor_);
    }

    completeMutatorsRootSet(mainWorker);

    // global root set must be collected after all the mutator's global data have been published
    collectRootSetGlobals <MarkTraits>(gcHandle(), mainWorker);

    barriers::enableMarkBarriers(gcHandle().getEpoch());

    resumeTheWorld(gcHandle());

    // build mark closure
    parallelMark(mainWorker);

    // TODO resume the world much later when the mark closure is completed
    stopTheWorld(gcHandle(), "GC stop the world #2: complete mark closure");

    barriers::disableMarkBarriers();

    bool refsRemainInMutatorQueues = false;
    do {
        for (auto& mutator: *lockedMutatorsList_) {
            const bool markQueueNowEmpty = mutator.gc().impl().gc().mark().markQueue()->forceFlush();
            if (!markQueueNowEmpty) {
                refsRemainInMutatorQueues = true;
            }
        }

        parallelProcessor_->resetForNewWork();
        // complete mark closure form newly found objects
        parallelMark(mainWorker);
    } while (refsRemainInMutatorQueues);

    for (auto& thread: *lockedMutatorsList_) {
        auto& markQueue = thread.gc().impl().gc().mark().markQueue();
        RuntimeAssert(markQueue->retainsNoWork(), ""); // TODO move into queue's destuctor?
        markQueue.destroy();
    }
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
    for (auto& thread: *lockedMutatorsList_) {
        tryCollectRootSet(thread, markQueue);
    }
}

void gc::mark::ConcurrentMark::tryCollectRootSet(mm::ThreadData& thread, MarkTraits::MarkQueue& markQueue) {
    auto& gcData = thread.gc().impl().gc();
    if (!gcData.tryLockRootSet()) return;

    GCLogDebug(gcHandle().getEpoch(), "Root set collection on thread %d for thread %d", konan::currentThreadId(), thread.threadId());
    gcData.publish();
    collectRootSetForThread <MarkTraits>(gcHandle(), markQueue, thread);
}

void gc::mark::ConcurrentMark::parallelMark(ParallelProcessor::Worker& worker) {
    GCLogDebug(gcHandle().getEpoch(), "Mark loop has begun");
    Mark <MarkTraits>(gcHandle(), worker);
}

void gc::mark::ConcurrentMark::resetMutatorFlags() {
    for (auto& mut: *lockedMutatorsList_) {
        mut.gc().impl().gc().clearMarkFlags();
    }
}
