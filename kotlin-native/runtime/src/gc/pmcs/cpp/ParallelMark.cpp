/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ParallelMark.hpp"

#include "MarkAndSweepUtils.hpp"
#include "GCStatistics.hpp"
#include "Utils.hpp"

// required to access gc thread data
#include "GCImpl.hpp"

using namespace kotlin;

namespace {

template<typename Cond>
void spinWait(Cond&& until) {
    while (!until()) {
        std::this_thread::yield();
    }
}

} // namespace

bool gc::mark::MarkPacer::is(gc::mark::MarkPacer::Phase phase) const {
    std::unique_lock lock(mutex_);
    return phase_ == phase;
}

void gc::mark::MarkPacer::begin(gc::mark::MarkPacer::Phase phase) {
    {
        std::unique_lock lock(mutex_);
        phase_ = phase;
    }
    cond_.notify_all();
}

void gc::mark::MarkPacer::wait(gc::mark::MarkPacer::Phase phase) {
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [=]() { return phase_ >= phase; });
}

void gc::mark::MarkPacer::beginEpoch(uint64_t epoch) {
    epoch_ = epoch;
    begin(Phase::kReady);
    GCLogDebug(epoch_.load(), "Mark is ready to recruit workers in a new epoch.");
}

void gc::mark::MarkPacer::waitNewEpochReadyOrShutdown() const {
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this]() { return phase_ >= Phase::kReady; });
}

void gc::mark::MarkPacer::waitEpochFinished(uint64_t currentEpoch) const {
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this, currentEpoch]() {
        return phase_ == Phase::kIdle || phase_ == Phase::kShutdown || epoch_.load(std::memory_order_relaxed) > currentEpoch;
    });
}

bool gc::mark::MarkPacer::acceptingNewWorkers() const {
    std::unique_lock lock(mutex_);
    return Phase::kReady <= phase_ && phase_ <= Phase::kParallelMark;
}


gc::mark::ParallelMark::ParallelMark(bool mutatorsCooperate) {
    std::size_t maxParallelism = std::thread::hardware_concurrency();
    if (maxParallelism == 0) {
        maxParallelism = std::numeric_limits<std::size_t>::max();
    }
    setParallelismLevel(maxParallelism, mutatorsCooperate);
}

void gc::mark::ParallelMark::beginMarkingEpoch(gc::GCHandle gcHandle) {
    gcHandle_ = gcHandle;

    lockedMutatorsList_ = mm::ThreadRegistry::Instance().LockForIter();

    parallelProcessor_.construct();

    if (!compiler::gcMarkSingleThreaded()) {
        std::unique_lock guard(workerCreationMutex_);
        pacer_.beginEpoch(gcHandle.getEpoch());
        // main worker is always accounted, so others would not be able to exhaust all the parallelism before main is instantiated
        activeWorkersCount_ = 1;
    }
}

void gc::mark::ParallelMark::endMarkingEpoch() {
    if (!compiler::gcMarkSingleThreaded()) {
        // We must now wait for every worker to finish the Mark procedure:
        // wake up from possible waiting, publish statistics, etc.
        // Only then it's safe to destroy the parallelProcessor and proceed to other GC tasks such as sweep.
        spinWait([=]() { return activeWorkersCount_.load(std::memory_order_relaxed) == 0; });

        std::unique_lock guard(workerCreationMutex_);
        RuntimeAssert(activeWorkersCount_ == 0, "All the workers must already finish");
        pacer_.begin(MarkPacer::Phase::kIdle);
    }
    parallelProcessor_.destroy();
    resetMutatorFlags();
    lockedMutatorsList_ = std::nullopt;
}

void gc::mark::ParallelMark::runMainInSTW() {
    if (compiler::gcMarkSingleThreaded()) {
        ParallelProcessor::Worker worker(*parallelProcessor_);
        gc::collectRootSet<MarkTraits>(gcHandle(), worker, [] (mm::ThreadData&) { return true; });
        gc::Mark<MarkTraits>(gcHandle(), worker);
    } else {
        RuntimeAssert(activeWorkersCount_ > 0, "Main worker must always be accounted");
        ParallelProcessor::Worker mainWorker(*parallelProcessor_);
        GCLogDebug(gcHandle().getEpoch(), "Creating main (#0) mark worker");

        pacer_.begin(MarkPacer::Phase::kRootSet);
        completeMutatorsRootSet(mainWorker);
        spinWait([this] {
            return allMutators([](mm::ThreadData& mut) { return mut.gc().impl().gc().published(); });
        });
        // global root set must be collected after all the mutator's global data have been published
        collectRootSetGlobals<MarkTraits>(gcHandle(), mainWorker);

        pacer_.begin(MarkPacer::Phase::kParallelMark);
        parallelMark(mainWorker);
    }
}

void gc::mark::ParallelMark::runOnMutator(mm::ThreadData& mutatorThread) {
    if (compiler::gcMarkSingleThreaded() || !mutatorsCooperate_) return;

    auto parallelWorker = createWorker();
    if (parallelWorker) {
        GCLogDebug(gcHandle().getEpoch(), "Mutator thread cooperates in marking");

        tryCollectRootSet(mutatorThread, *parallelWorker);

        completeRootSetAndMark(*parallelWorker);
    }
}

void gc::mark::ParallelMark::runAuxiliary() {
    RuntimeAssert(!compiler::gcMarkSingleThreaded(), "Should not reach here during single threaded mark");

    pacer_.waitNewEpochReadyOrShutdown();
    if (pacer_.is(MarkPacer::Phase::kShutdown)) return;

    auto curEpoch = gcHandle().getEpoch();
    auto parallelWorker = createWorker();
    if (parallelWorker) {
        completeRootSetAndMark(*parallelWorker);
    }

    pacer_.waitEpochFinished(curEpoch);
}

void gc::mark::ParallelMark::requestShutdown() {
    pacer_.begin(MarkPacer::Phase::kShutdown);
}

bool gc::mark::ParallelMark::shutdownRequested() const {
    return pacer_.is(MarkPacer::Phase::kShutdown);
}


gc::GCHandle& gc::mark::ParallelMark::gcHandle() {
    RuntimeAssert(gcHandle_.isValid(), "GCHandle must be initialized");
    return gcHandle_;
}

void gc::mark::ParallelMark::setParallelismLevel(size_t maxParallelism, bool mutatorsCooperate) {
    RuntimeCheck(maxParallelism > 0, "Parallelism level can't be 0");
    maxParallelism_ = maxParallelism;
    mutatorsCooperate_ = mutatorsCooperate;
    RuntimeLogInfo({kTagGC},
                   "Set up parallel mark with maxParallelism = %zu and %s" "cooperative mutators",
                   maxParallelism_, (mutatorsCooperate_ ? "" : "non-"));
}

void gc::mark::ParallelMark::completeRootSetAndMark(ParallelProcessor::Worker& parallelWorker) {
    pacer_.wait(MarkPacer::Phase::kRootSet);
    completeMutatorsRootSet(parallelWorker);
    pacer_.wait(MarkPacer::Phase::kParallelMark);
    parallelMark(parallelWorker);
}

void gc::mark::ParallelMark::completeMutatorsRootSet(MarkTraits::MarkQueue& markQueue) {
    // workers compete for mutators to collect their root set
    for (auto& thread: *lockedMutatorsList_) {
        tryCollectRootSet(thread, markQueue);
    }
}

void gc::mark::ParallelMark::tryCollectRootSet(mm::ThreadData& thread, MarkTraits::MarkQueue& markQueue) {
    auto& gcData = thread.gc().impl().gc();
    if (!gcData.tryLockRootSet()) return;

    GCLogDebug(gcHandle().getEpoch(), "Root set collection on thread %d for thread %d",
               konan::currentThreadId(), thread.threadId());
    gcData.publish();
    collectRootSetForThread<MarkTraits>(gcHandle(), markQueue, thread);
}

void gc::mark::ParallelMark::parallelMark(ParallelProcessor::Worker& worker) {
    GCLogDebug(gcHandle().getEpoch(), "Mark loop has begun");
    Mark<MarkTraits>(gcHandle(), worker);

    std::unique_lock guard(workerCreationMutex_);
    activeWorkersCount_.fetch_sub(1, std::memory_order_relaxed);
}

std::optional<gc::mark::ParallelMark::ParallelProcessor::Worker> gc::mark::ParallelMark::createWorker() {
    std::unique_lock guard(workerCreationMutex_);
    if (!pacer_.acceptingNewWorkers() ||
        activeWorkersCount_.load(std::memory_order_relaxed) >= maxParallelism_ ||
        activeWorkersCount_.load(std::memory_order_relaxed) == 0) return std::nullopt;

    auto num = activeWorkersCount_.fetch_add(1, std::memory_order_relaxed);
    GCLogDebug(gcHandle().getEpoch(), "Creating mark worker #%zu", num);
    return std::make_optional<ParallelProcessor::Worker>(*parallelProcessor_);
}

void gc::mark::ParallelMark::resetMutatorFlags() {
    for (auto& mut: *lockedMutatorsList_) {
        auto& gcData = mut.gc().impl().gc();
        if (!compiler::gcMarkSingleThreaded()) {
            // single threaded mark do not use this flag
            RuntimeAssert(gcData.published(), "Must have been published during mark");
        }
        gcData.clearMarkFlags();
    }
}
