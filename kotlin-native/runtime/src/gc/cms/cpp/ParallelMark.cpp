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

template<typename Cond>
void spinWait(Cond&& until) {
    while (!until()) {
        std::this_thread::yield();
    }
}

} // namespace

bool gc::mark::MarkPacer::is(gc::mark::MarkPacer::Phase phase) const {
    return phase_.load(std::memory_order_relaxed) == phase;
}

void gc::mark::MarkPacer::begin(gc::mark::MarkPacer::Phase phase) {
    {
        std::unique_lock lock(mutex_);
        GCLogDebug(epoch_.load(), "Phase #%d begins", phase); // FIXME
        phase_.store(phase, std::memory_order_release);
        // FIXME move out?
        cond_.notify_all();
    }
}

void gc::mark::MarkPacer::wait(gc::mark::MarkPacer::Phase phase) {
    if (phase_.load(std::memory_order_relaxed) >= phase) return;
    std::unique_lock lock(mutex_);
    GCLogDebug(epoch_.load(), "Waiting for phase #%d", phase); // FIXME
    cond_.wait(lock, [this, phase]() {
        Phase ph = phase_.load(std::memory_order_relaxed);
        GCLogDebug(epoch_.load(), "Woke up and seen phase #%d", ph); // FIXME
        return ph >= phase;
    });
}

void gc::mark::MarkPacer::beginEpoch(uint64_t epoch) {
    epoch_ = epoch;
    begin(Phase::kReady);
    GCLogDebug(epoch_.load(), "Mark is ready to recruit workers in a new epoch.");
}

void gc::mark::MarkPacer::waitNewEpochReadyOrShutdown() const {
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this]() { return (phase_.load(std::memory_order_relaxed) >= Phase::kReady); });
}

void gc::mark::MarkPacer::waitEpochFinished(uint64_t currentEpoch) const {
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this, currentEpoch]() {
        return is(Phase::kIdle) || is(Phase::kShutdown) || epoch_.load(std::memory_order_relaxed) > currentEpoch;
    });
}

bool gc::mark::MarkPacer::acceptingNewWorkers() const {
    return Phase::kReady <= phase_ && phase_ < Phase::kParallelMark;
}


gc::mark::ParallelMark::ParallelMark(bool mutatorsCooperate, std::size_t auxWorkersPoolSize) {
    std::size_t maxParallelism = std::thread::hardware_concurrency();
    if (maxParallelism == 0) {
        maxParallelism = std::numeric_limits<std::size_t>::max();
    }
    setParallelismLevel(maxParallelism, mutatorsCooperate, auxWorkersPoolSize);
}

void gc::mark::ParallelMark::beginMarkingEpoch(gc::GCHandle gcHandle) {
    gcHandle_ = gcHandle;

    lockedMutatorsList_ = mm::ThreadRegistry::Instance().LockForIter();

    // main worker is always accounted, so others would not be able to exhaust all the parallelism before main is instantiated
    activeWorkersCount_ = 1;
    auxWorkersCount_ = 0;

    parallelProcessor_.construct();

    pacer_.beginEpoch(gcHandle.getEpoch());
}

void gc::mark::ParallelMark::waitForThreadsPauseMutation() noexcept {
    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "Dispatcher thread must not be registered");
    spinWait([this] {
        return allMutators([](mm::ThreadData& mut) {
            return mm::isSuspendedOrNative(mut) || mut.gc().impl().gc().cooperative();
        });
    });
}

void gc::mark::ParallelMark::endMarkingEpoch() {
    pacer_.begin(MarkPacer::Phase::kIdle);
    parallelProcessor_.destroy();
    resetMutatorFlags();
    lockedMutatorsList_ = std::nullopt;
}

void gc::mark::ParallelMark::runMainInSTW() {
    RuntimeAssert(activeWorkersCount_ > 0, "Main worker must always be accounted");
    ParallelProcessor::Worker mainWorker(*parallelProcessor_);
    GCLogDebug(gcHandle().getEpoch(), "Creating main mark worker");

    if (compiler::gcMarkSingleThreaded()) {
        gc::collectRootSet<MarkTraits>(gcHandle(), mainWorker, [] (mm::ThreadData&) { return true; });
        gc::Mark<MarkTraits>(gcHandle(), mainWorker);
    } else {
        pacer_.begin(MarkPacer::Phase::kRootSet);
        completeMutatorsRootSet(mainWorker);
        spinWait([this] {
            return allMutators([](mm::ThreadData& mut) { return mut.gc().impl().gc().published(); });
        });
        // global root set must be collected after all the mutator's global data have been published
        collectRootSetGlobals<MarkTraits>(gcHandle(), mainWorker);
        spinWait([this] {
            return auxWorkersCount_.load(std::memory_order_relaxed) == auxWorkersPoolSize_
                    || activeWorkersCount_.load(std::memory_order_relaxed) == maxParallelism_;
        });

        std::unique_lock guard(workerCreationMutex_);
        GCLogInfo(gcHandle().getEpoch(), "Exactly %zu workers participate in mark (%s)", activeWorkersCount_.load(std::memory_order_relaxed),
                  mutatorsCooperate_ ? "including cooperative mutators" : "mutators cooperation was not requested"
        );
        pacer_.begin(MarkPacer::Phase::kParallelMark);
        parallelMark(mainWorker);
    }
}

void gc::mark::ParallelMark::runOnMutator(mm::ThreadData& mutatorThread) {
    if (compiler::gcMarkSingleThreaded() || !mutatorsCooperate_) return;

    RuntimeAssert(mutatorThread.suspensionData().state() == ThreadState::kRunnable, "Must be kRunnable");
    mutatorThread.suspensionData().setStateNoSuspend(ThreadState::kNative);

    auto epoch = gcHandle().getEpoch();
    auto parallelWorker = createWorker();
    if (parallelWorker) {
        auto& gcData = mutatorThread.gc().impl().gc();
        gcData.beginCooperation();
        GCLogDebug(epoch, "Mutator thread cooperates in marking");

        tryCollectRootSet(mutatorThread, *parallelWorker);

        completeRootSetAndMark(*parallelWorker);
    }

    mutatorThread.suspensionData().setStateNoSuspend(ThreadState::kRunnable);
}

void gc::mark::ParallelMark::runAuxiliary() {
    RuntimeAssert(!compiler::gcMarkSingleThreaded(), "Should not reach here during single threaded mark");

    pacer_.waitNewEpochReadyOrShutdown();
    if (pacer_.is(MarkPacer::Phase::kShutdown)) return;

    auto curEpoch = gcHandle().getEpoch();
    auto parallelWorker = createWorker();
    if (parallelWorker) {
        auto workerNum = auxWorkersCount_.fetch_add(1, std::memory_order_relaxed);
        GCLogDebug(gcHandle().getEpoch(), "Aux worker %zu starts", workerNum);
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

void gc::mark::ParallelMark::setParallelismLevel(size_t maxParallelism, bool mutatorsCooperate, std::size_t auxWorkersPoolSize) {
    RuntimeCheck(maxParallelism > 0, "Parallelism level can't be 0");
    maxParallelism_ = std::min(maxParallelism, kMaxWorkers);
    mutatorsCooperate_ = mutatorsCooperate;
    auxWorkersPoolSize_ = auxWorkersPoolSize;
    RuntimeLogInfo({kTagGC},
                   "Set up parallel mark with maxParallelism = %zu, auxWorkersPoolSize = %zu and %s" "cooperative mutators",
                   maxParallelism_, auxWorkersPoolSize_, (mutatorsCooperate_ ? "" : "non-"));
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

    GCLogDebug(gcHandle().getEpoch(), "Root set collection for thread %d", thread.threadId());
    gcData.publish();
    collectRootSetForThread<MarkTraits>(gcHandle(), markQueue, thread);
}

void gc::mark::ParallelMark::parallelMark(ParallelProcessor::Worker& worker) {
    GCLogDebug(gcHandle().getEpoch(), "Mark task has begun");
    Mark<MarkTraits>(gcHandle(), worker);
    // We must now wait for every worker to finish the Mark procedure:
    // wake up from possible waiting, publish statistics, etc.
    // Only then it's safe to destroy the parallelProcessor and proceed to other GC tasks such as sweep.
    waitEveryWorkerTermination();
}

std::optional<gc::mark::ParallelMark::ParallelProcessor::Worker> gc::mark::ParallelMark::createWorker() {
    std::unique_lock guard(workerCreationMutex_);
    if (!pacer_.acceptingNewWorkers() || activeWorkersCount_.load(std::memory_order_relaxed) >= maxParallelism_) return std::nullopt;

    GCLogDebug(gcHandle().getEpoch(), "Creating %zu'th mark worker", activeWorkersCount_.load(std::memory_order_relaxed));
    activeWorkersCount_.store(activeWorkersCount_.load(std::memory_order_relaxed) + 1, std::memory_order_relaxed);
    return std::make_optional<ParallelProcessor::Worker>(*parallelProcessor_);
}

void gc::mark::ParallelMark::waitEveryWorkerTermination() {
    auto curEpoch = gcHandle().getEpoch();
    --activeWorkersCount_;
    spinWait([=]() { return curEpoch != gcHandle().getEpoch() || activeWorkersCount_.load(std::memory_order_relaxed) == 0; });
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
