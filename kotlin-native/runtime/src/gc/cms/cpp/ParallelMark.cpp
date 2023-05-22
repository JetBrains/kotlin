#include "ParallelMark.hpp"

#include "MarkStack.hpp"
#include "MarkAndSweepUtils.hpp"
#include "GCStatistics.hpp"
#include "Utils.hpp"
#include "std_support/Memory.hpp"

// required to access gc thread data
#include "GCImpl.hpp"

using namespace kotlin;

namespace {

template<typename Iterable, typename Pred>
std::size_t count(Iterable&& iterable, Pred&& pred) {
    std::size_t size = 0;
    for (auto& i: iterable) {
        if (pred(i)) {
            size += 1;
        }
    }
    return size;
}

template<typename Iterable>
std::size_t count(Iterable&& iterable) {
    std::size_t size = 0;
    for (const auto& i: iterable) {
        std::ignore = i;
        size += 1;
    }
    return size;
}

template<typename Cond>
void spinWait(Cond&& until) {
    while (!until()) {
        std::this_thread::yield();
    }
}

} // namespace

void gc::mark::MarkPacer::beginEpoch(uint64_t epoch) {
    epoch_ = epoch;
    begin(Phase::kReady);
    GCLogDebug(epoch_.load(), "Mark is ready to recruit workers in a new epoch.");
}

void gc::mark::MarkPacer::waitNewEpochReadyOrShutdown() const {
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this]() { return (phase_.load(std::memory_order_relaxed) >= Phase::kReady); });
}

bool gc::mark::MarkPacer::isRecruiting() const {
    return is(Phase::kReady) || is(Phase::kParallelMark);
}

void gc::mark::MarkPacer::waitForParallelMark() const {
    if (is(Phase::kParallelMark)) return;
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this]() { return is(Phase::kParallelMark); });
}

void gc::mark::MarkPacer::requestParallelMark() {
    RuntimeAssert(is(Phase::kReady), "Must prepare first");
    begin(Phase::kParallelMark);
    GCLogDebug(epoch_.load(), "Parallel mark has begun.");
}

void gc::mark::MarkPacer::reset() {
    begin(Phase::kIdle);
}

void gc::mark::MarkPacer::waitEpochFinished(uint64_t currentEpoch) const {
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this, currentEpoch]() {
        return is(Phase::kIdle) || shutdownRequested() || epoch_.load(std::memory_order_relaxed) > currentEpoch;
    });
}

void gc::mark::MarkPacer::requestShutdown() {
    begin(Phase::kShutdown);
}

bool gc::mark::MarkPacer::shutdownRequested() const {
    return is(Phase::kShutdown);
}

bool gc::mark::MarkPacer::is(gc::mark::MarkPacer::Phase phase) const {
    return phase_.load(std::memory_order_relaxed) == phase;
}

void gc::mark::MarkPacer::begin(gc::mark::MarkPacer::Phase phase) {
    {
        std::unique_lock lock(mutex_);
        phase_.store(phase, std::memory_order_release);
    }
    cond_.notify_all();
}

gc::mark::MarkDispatcher::MarkJob::MarkJob(gc::mark::MarkDispatcher& dispatcher) : dispatcher_(dispatcher) {}

void gc::mark::MarkDispatcher::MarkJob::runMainInSTW() {
    RuntimeAssert(dispatcher_.activeWorkers_ > 0, "Main worker must always be \"active\"");
    ParallelProcessor::Worker parallelWorker(*dispatcher_.parallelProcessor_);

    dispatcher_.pacer_.requestParallelMark();
    completeMutatorsRootSet(parallelWorker.workList());
    spinWait([&] {
        return dispatcher_.allMutators([](mm::ThreadData& mut) { return mut.gc().impl().gc().published(); });
    });
    // global root set must be collected after all the mutator's global data have been published
    collectRootSetGlobals<MarkTraits>(dispatcher_.gcHandle(), parallelWorker.workList());
    dispatcher_.allCooperativeMutatorsAreRegistered();
    parallelMark(parallelWorker);
}

void gc::mark::MarkDispatcher::MarkJob::runOnMutator(mm::ThreadData& mutatorThread) {
    if (compiler::gcMarkSingleThreaded() || !dispatcher_.mutatorsCooperate_) return;
    if (!dispatcher_.authorizeNewWorkerCreation()) return;
    ParallelProcessor::Worker parallelWorker(*dispatcher_.parallelProcessor_);

    auto& gcData = mutatorThread.gc().impl().gc();
    gcData.beginCooperation();

    // It's possible that some other worker has already acquired this thread's root set.
    // e.g. if the thread was in native context during workers awakening
    // but then has returned from native and arrived ad a safe point
    if (gcData.tryLockRootSet()) {
        auto epoch = dispatcher_.gcHandle().getEpoch();
        GCLogDebug(epoch, "Mutator thread %d takes part in marking", konan::currentThreadId());

        collectRootSet(mutatorThread, parallelWorker.workList());
    }

    dispatcher_.pacer_.waitForParallelMark();
    completeMutatorsRootSet(parallelWorker.workList());
    parallelMark(parallelWorker);
}

void gc::mark::MarkDispatcher::MarkJob::runAuxiliary() {
    dispatcher_.pacer_.waitNewEpochReadyOrShutdown();
    if (dispatcher_.pacer_.shutdownRequested()) return;

    auto curEpoch = dispatcher_.gcHandle().getEpoch();
    if (dispatcher_.authorizeNewWorkerCreation()) {
        // no additional synchronization/pacing needed, because the auxiliary workers are always "expected",
        // so the parallel mark would wait
        ParallelProcessor::Worker parallelWorker(*dispatcher_.parallelProcessor_);
        dispatcher_.pacer_.waitForParallelMark();
        completeMutatorsRootSet(parallelWorker.workList());
        parallelMark(parallelWorker);
    } else {
        RuntimeAssert(dispatcher_.activeWorkers_ == dispatcher_.parallelProcessor_->expectedWorkers(), "Must not decline workers that might be expected");
    }

    dispatcher_.pacer_.waitEpochFinished(curEpoch);
}

void gc::mark::MarkDispatcher::MarkJob::completeMutatorsRootSet(MarkTraits::MarkQueue& markQueue) {
    // workers compete for mutators to collect their root set
    for (auto& thread: *dispatcher_.lockedMutatorsList_) {
        auto& gcData = thread.gc().impl().gc();
        if (gcData.tryLockRootSet()) {
            collectRootSet(thread, markQueue);
        }
    }
}

void gc::mark::MarkDispatcher::MarkJob::collectRootSet(mm::ThreadData& thread, MarkTraits::MarkQueue& markQueue) {
    GCLogDebug(dispatcher_.gcHandle().getEpoch(), "Root set collection on thread %d for thread %d",
               konan::currentThreadId(), thread.threadId());
    auto& gcData = thread.gc().impl().gc();
    gcData.publish();
    bool markItself = konan::currentThreadId() == thread.threadId();
    RuntimeAssert(markItself == gcData.cooperative(), "A mutator can mark it's own root set iff the mutator cooperate");
    collectRootSetForThread<MarkTraits>(dispatcher_.gcHandle(), markQueue, thread);
}

// Parallel mark

void gc::mark::MarkDispatcher::MarkJob::parallelMark(ParallelProcessor::Worker& worker) {
    GCLogDebug(dispatcher_.gcHandle().getEpoch(), "Mark task has begun on thread %d", konan::currentThreadId());
    {
        auto markHandle = dispatcher_.gcHandle().mark();
        worker.performWork([&markHandle, this](MarkTraits::MarkQueue& markStack) {
            GCLogDebug(dispatcher_.gcHandle().getEpoch(),"Mark task begins to mark a new portion of objects");
            Mark<MarkTraits>(markHandle, markStack);
        });
    }
    // wait for other threads to collect statistics
    worker.waitEveryWorkerTermination();
}

// dispatcher

gc::mark::MarkDispatcher::MarkDispatcher(std::size_t gcWorkerPoolSize, bool mutatorsCooperate) {
    std::size_t maxParallelism = std::thread::hardware_concurrency();
    if (maxParallelism == 0) {
        maxParallelism = std::numeric_limits<std::size_t>::max();
    }
    setParallelismLevel(maxParallelism, mutatorsCooperate, gcWorkerPoolSize);
}

void gc::mark::MarkDispatcher::beginMarkingEpoch(gc::GCHandle gcHandle) {
    gcHandle_ = gcHandle;

    lockedMutatorsList_ = mm::ThreadRegistry::Instance().LockForIter();
    activeWorkers_ = 1; // main worker is always active

    // initially expect all the mark tasks possible:
    // one for each mutator existing at the moment and one for each gc thread
    auto maxPossibleMutators = mutatorsCooperate_ ? count(*lockedMutatorsList_) : 0;
    auto maxExpectedJobs = maxPossibleMutators + gcWorkerPoolSize_;
    maxExpectedJobs = std::min({maxExpectedJobs, maxParallelism_, kMaxWorkers});
    GCLogDebug(gcHandle.getEpoch(), "Expecting at most %zu markers", maxExpectedJobs);

    parallelProcessor_.construct(maxExpectedJobs);

    pacer_.beginEpoch(gcHandle.getEpoch());
}

void gc::mark::MarkDispatcher::waitForThreadsPauseMutation() noexcept {
    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "Dispatcher thread must not be registered");
    spinWait([this] {
        return allMutators([](mm::ThreadData& mut) {
            return mm::isSuspendedOrNative(mut) || mut.gc().impl().gc().cooperative();
        });
    });
}

void gc::mark::MarkDispatcher::endMarkingEpoch() {
    pacer_.reset();
    parallelProcessor_.destroy();
    resetMutatorFlags();
    lockedMutatorsList_ = std::nullopt;
}

void gc::mark::MarkDispatcher::requestShutdown() {
    pacer_.requestShutdown();
}

bool gc::mark::MarkDispatcher::shutdownRequested() const {
    return pacer_.shutdownRequested();
}

gc::GCHandle& gc::mark::MarkDispatcher::gcHandle() {
    RuntimeAssert(gcHandle_.isValid(), "GCHandle must be initialized");
    return gcHandle_;
}

void gc::mark::MarkDispatcher::setParallelismLevel(size_t maxParallelism, bool mutatorsCooperate, size_t gcWorkerPoolSize) {
    if (compiler::gcMarkSingleThreaded()) {
        RuntimeCheck(!mutatorsCooperate && gcWorkerPoolSize == 1, "Single threaded mark does not support additional mark workers");
    }
    RuntimeCheck(maxParallelism > 0, "Parallelism level can't be 0");
    RuntimeLogInfo({kTagGC},
                   "Setting up parallel mark with maxParallelism = %zu, gcWorkerPoolSize = %zu and %s" "cooperative mutators",
                   maxParallelism, gcWorkerPoolSize, (mutatorsCooperate ? "" : "non-"));
    maxParallelism_ = maxParallelism;
    mutatorsCooperate_ = mutatorsCooperate;
    gcWorkerPoolSize_ = gcWorkerPoolSize;
}

bool gc::mark::MarkDispatcher::authorizeNewWorkerCreation() {
    if (!pacer_.isRecruiting()) return false;
    auto activeWorkers = activeWorkers_.load(std::memory_order_relaxed);
    while (activeWorkers < maxParallelism_ && activeWorkers < parallelProcessor_->expectedWorkers()) {
        if (activeWorkers_.compare_exchange_weak(activeWorkers,
                                                 activeWorkers + 1,
                                                 std::memory_order_relaxed)) {
            return true;
        }
    }
    return false;
}

void gc::mark::MarkDispatcher::resetMutatorFlags() {
    for (auto& mut: *lockedMutatorsList_) {
        auto& gcData = mut.gc().impl().gc();
        RuntimeAssert(gcData.rootSetLocked(), "Must have been locked during mark");
        RuntimeAssert(gcData.published(), "Must have been published during mark");
        gcData.clearMarkFlags();
    }
}

void gc::mark::MarkDispatcher::allCooperativeMutatorsAreRegistered() {
    if (mutatorsCooperate_) {
        // now that we know the exact number of cooperative mutators,
        // calculate expectedTasks_'s precise value
        RuntimeAssert(allMutators(
                [](mm::ThreadData& mut) { return mut.gc().impl().gc().rootSetLocked(); }),
                      "All the mutators must be either cooperative or locked by other marker");

        auto exactExpectedJobs = count(*lockedMutatorsList_,
                                       [](mm::ThreadData& mut) { return mut.gc().impl().gc().cooperative(); })
                + gcWorkerPoolSize_;
        exactExpectedJobs = std::min(exactExpectedJobs, parallelProcessor_->expectedWorkers());
        parallelProcessor_->lowerExpectations(exactExpectedJobs);
    }
    const auto coopMutatorsDescr = mutatorsCooperate_
                                          ? "including cooperative mutators"
                                          : "mutators cooperation will not be requested";
    GCLogInfo(gcHandle().getEpoch(), "Expecting exactly %zu markers (%s)", parallelProcessor_->expectedWorkers(), coopMutatorsDescr);
}
