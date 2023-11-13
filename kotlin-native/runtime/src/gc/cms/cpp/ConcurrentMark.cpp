/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMark.hpp"

#include "MarkAndSweepUtils.hpp"
#include "GCStatistics.hpp"
#include "Utils.hpp"

// required to access gc thread data
#include "GCImpl.hpp"

using namespace kotlin;

namespace {

gc::mark::ConcurrentMark::ThreadData::FlushAction flushAction{};

} // namespace

mm::OncePerThreadAction<gc::mark::ConcurrentMark::ThreadData::FlushAction>::ThreadData&
gc::mark::ConcurrentMark::ThreadData::FlushAction::getUtilityData(mm::ThreadData& threadData) {
    return threadData.gc().impl().gc().mark().flushActionData_;
}

void gc::mark::ConcurrentMark::ThreadData::FlushAction::action(mm::ThreadData& threadData) noexcept {
    auto& markData = threadData.gc().impl().gc().mark();
    bool flushed = markData.markQueue()->forceFlush();
    RuntimeAssert(flushed, "Don't know yet what to handle overflow");
}

gc::mark::ConcurrentMark::ThreadData::ThreadData(mm::ThreadData& base) : flushActionData_(flushAction, base) {}

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
    RuntimeAssert(!compiler::gcMarkSingleThreaded(), "Not yet sure how this should work");
    ParallelProcessor::Worker mainWorker(*parallelProcessor_);
    GCLogDebug(gcHandle().getEpoch(), "Creating main (#0) mark worker");

    // create mutator mark queues
    for (auto& thread : *lockedMutatorsList_) {
        thread.gc().impl().gc().mark().markQueue().construct(*parallelProcessor_);
    }

    // collect root set
    completeMutatorsRootSet(mainWorker);

    // global root set must be collected after all the mutator's global data have been published
    collectRootSetGlobals<MarkTraits>(gcHandle(), mainWorker);

    barriers::enableMarkBarriers();

    // TODO move all the STW management in a single place
    resumeTheWorld(gcHandle());

    // build mark closure
    parallelMark(mainWorker);

    // TODO resume the world much later when the mark closure is completed
    stopTheWorld(gcHandle());

    flushAction.ensurePerformed(*lockedMutatorsList_);

    barriers::disableMarkBarriers();

    // TODO check if there is actually new work flushed
    parallelProcessor_->undo();
    // complete mark closure form newly found objects
    parallelMark(mainWorker);
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

    GCLogDebug(gcHandle().getEpoch(), "Root set collection on thread %d for thread %d",
               konan::currentThreadId(), thread.threadId());
    gcData.publish();
    collectRootSetForThread<MarkTraits>(gcHandle(), markQueue, thread);
}

void gc::mark::ConcurrentMark::parallelMark(ParallelProcessor::Worker& worker) {
    GCLogDebug(gcHandle().getEpoch(), "Mark loop has begun");
    Mark<MarkTraits>(gcHandle(), worker);
}

void gc::mark::ConcurrentMark::resetMutatorFlags() {
    for (auto& mut: *lockedMutatorsList_) {
        auto& gcData = mut.gc().impl().gc();
        if (!compiler::gcMarkSingleThreaded()) {
            // single threaded mark do not use this flag
            RuntimeAssert(gcData.published(), "Must have been published during mark");
        }
        gcData.clearMarkFlags();
    }
}
