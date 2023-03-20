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

void yield() noexcept {
    std::this_thread::yield();
}

template<typename Cond>
void waitFast(Cond&& until) {
    while (!until()) {
        yield();
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
    waitFor(Phase::kParallelMark);
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

void gc::mark::MarkPacer::waitFor(gc::mark::MarkPacer::Phase phase) const {
    if (is(phase)) return;
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this, phase]() { return is(phase); });
}

void gc::mark::MarkPacer::begin(gc::mark::MarkPacer::Phase phase) {
    {
        std::unique_lock lock(mutex_);
        phase_.store(phase, std::memory_order_release);
    }
    cond_.notify_all();
}

void gc::mark::MarkDispatcher::MarkJob::runMainInSTW() {
    dispatcher_.registerTask(*this);
    dispatcher_.pacer_.requestParallelMark();
    completeMutatorsRootSet();
    waitFast([&] { 
        return dispatcher_.allMutators([](mm::ThreadData& mut){ return mut.gc().impl().gc().published(); }); 
    });
    // global root set must be collected after all the mutator's global data have been published
    collectRootSetGlobals<gc::mark::MarkTraits>(dispatcher_.gcHandle(), workList_);
    dispatcher_.allCooperativeMutatorsAreRegistered();
    parallelMark();
    dispatcher_.pacer_.reset();
    dispatcher_.resetMutatorFlags();
    dispatcher_.lockedMutatorsList_ = std::nullopt;
}

void gc::mark::MarkDispatcher::MarkJob::runOnMutator(mm::ThreadData& mutatorThread) {
    if (!dispatcher_.mutatorsCooperate_) return;
    if (!dispatcher_.pacer_.isRecruiting()) return;

    auto& gcData = mutatorThread.gc().impl().gc();
    // It's possible that some other worker has already acquired this thread's root set.
    // e.g. if the thread was in native context during workers awakening
    // but then has returned from native and arrived ad a safe point
    if (gcData.tryLockRootSet()) {
        gcData.beginCooperation();
        auto epoch = dispatcher_.gcHandle().getEpoch();
        GCLogDebug(epoch, "Mutator thread %d takes part in marking", konan::currentThreadId());

        dispatcher_.registerTask(*this);
        collectRootSet(mutatorThread);

        dispatcher_.pacer_.waitForParallelMark();
        completeMutatorsRootSet();
        parallelMark();
    }
}

void gc::mark::MarkDispatcher::MarkJob::runAuxiliary() {
    dispatcher_.pacer_.waitNewEpochReadyOrShutdown();
    if (dispatcher_.pacer_.shutdownRequested()) return;

    dispatcher_.registerTask(*this);
    auto curEpoch = dispatcher_.gcHandle().getEpoch();
    dispatcher_.pacer_.waitForParallelMark();
    completeMutatorsRootSet();
    parallelMark();
    dispatcher_.pacer_.waitEpochFinished(curEpoch);
}

void gc::mark::MarkDispatcher::MarkJob::completeMutatorsRootSet() {
    // workers compete for mutators to collect their root set
    for (auto& thread: *dispatcher_.lockedMutatorsList_) {
        auto& gcData = thread.gc().impl().gc();
        if (gcData.tryLockRootSet()) {
            collectRootSet(thread);
        }
    }
}

void gc::mark::MarkDispatcher::MarkJob::collectRootSet(mm::ThreadData& thread) {
    GCLogDebug(dispatcher_.gcHandle().getEpoch(), "Root set collection on thread %d for thread %d",
               konan::currentThreadId(), thread.threadId());
    thread.gc().impl().gc().publish();
    collectRootSetForThread<gc::mark::MarkTraits>(dispatcher_.gcHandle(), workList_, thread);
}

// Parallel mark

void gc::mark::MarkDispatcher::MarkJob::parallelMark() {
    GCLogDebug(dispatcher_.gcHandle().getEpoch(), "Mark task has begun on thread %d", konan::currentThreadId());
    RuntimeAssert(dispatcher_.isRegistered(*this), "A task must be registered in dispatcher before the start of execution");
    {
        // scoped in order to collect mark statistics before TODO what?
        auto markHandle = dispatcher_.gcHandle().mark();
        while (true) {
            tryAcquireWork();
            if (workList_.localEmpty()) {
                std::unique_lock lock(dispatcher_.waitMutex_);

                auto nowWaiting = dispatcher_.waitingJobs_.fetch_add(1, std::memory_order_relaxed) + 1;
                GCLogTrace(dispatcher_.gcHandle().getEpoch(),
                           "Mark task %d goes to sleep (now sleeping %zu registered %zu expected %zu)",
                           carrierThreadId_,
                           nowWaiting,
                           dispatcher_.registeredJobs_.load(std::memory_order_relaxed),
                           dispatcher_.expectedJobs_);

                if (dispatcher_.allDone_) {
                    break;
                }

                auto registeredJobs = dispatcher_.registeredJobs_.load(std::memory_order_relaxed);
                if (nowWaiting == registeredJobs && registeredJobs == dispatcher_.expectedJobs_) {
                    // we are the last ones awake
                    GCLogTrace(dispatcher_.gcHandle().getEpoch(), "Mark task %d has detected termination", carrierThreadId_);
                    dispatcher_.allDone_ = true;
                    lock.unlock();
                    dispatcher_.waitCV_.notify_all();
                    break;
                }

                dispatcher_.waitCV_.wait(lock);
                if (dispatcher_.allDone_) {
                    break;
                }
                dispatcher_.waitingJobs_.fetch_sub(1, std::memory_order_relaxed);
                GCLogTrace(dispatcher_.gcHandle().getEpoch(), "Mark task %d woke", carrierThreadId_);
            } else {
                performWork(markHandle);
            }
        }
        RuntimeAssert(workList_.localEmpty(), "There should be no local tasks left");
        RuntimeAssert(workList_.sharedEmpty(), "There should be no shared tasks left");
    }
    RuntimeAssert(dispatcher_.allDone_, "Work must be done");
    dispatcher_.waitingJobs_.fetch_sub(1, std::memory_order_relaxed);
    GCLogTrace(dispatcher_.gcHandle().getEpoch(), "Mark task %d waits for others to terminate", carrierThreadId_);
    waitFast([&] {
        bool allDone = dispatcher_.allDone_.load(std::memory_order_relaxed);
        bool allTerminated = dispatcher_.waitingJobs_.load(std::memory_order_relaxed) == 0;
        return !allDone || allTerminated;
    });
    GCLogTrace(dispatcher_.gcHandle().getEpoch(), "Mark task %d finally finishes", carrierThreadId_);
}

bool gc::mark::MarkDispatcher::MarkJob::tryAcquireWork() {
    if (!workList_.localEmpty()) {
        return true;
    }

    // check own shared queue first
    auto selfStolen = workList_.tryStealFractionFrom(workList_, kFractionToSteal);
    if (selfStolen > 0) {
        GCLogTrace(dispatcher_.gcHandle().getEpoch(), "Mark task %d has stolen %zu tasks from itself", carrierThreadId_, selfStolen);
        return true;
    }

    for (size_t i = 0; i < kStealingAttemptCyclesBeforeWait; ++i) {
        auto registeredJobs = dispatcher_.registeredJobs_.load(std::memory_order_acquire);
        for (size_t vi = 0; vi < registeredJobs; ++vi) {
            auto victim = dispatcher_.jobs_[vi].load(std::memory_order_relaxed);
            RuntimeAssert(victim != nullptr, "victim job can not be null here");
            auto stolen = workList_.tryStealFractionFrom(victim->workList_, kFractionToSteal);
            if (stolen > 0) {
                GCLogTrace(dispatcher_.gcHandle().getEpoch(), "Mark task %d has stolen %zu tasks from %d", carrierThreadId_, stolen, victim->carrierThreadId_);
                return true;
            }
        }
        yield();
    }
    GCLogTrace(dispatcher_.gcHandle().getEpoch(),"Mark task %d has not found a victim to steal from :(", carrierThreadId_);

    return false;
}

void gc::mark::MarkDispatcher::MarkJob::performWork(gc::GCHandle::GCMarkScope& markHandle) {
    // FIXME copy&pasted from (MarkAndSweepUtils.hpp) gc::Mark<mark::MarkTraits>
    GCLogTrace(dispatcher_.gcHandle().getEpoch(),"Mark task %d begins to mark %zu objects", carrierThreadId_, workList_.localSize());
    // this is an extremely hot loop
    // TODO assembly-perfect:
    //  - evaluate precessInMark indirect call vs branch
    //  - consider outlining extra objects check
    //  - consider outlining wrk-sharing
    //  - consider preforming work sharing in a middle of a large array
    //  - try marking cold branches with smth like __builtin_expect
    while (auto top = workList_.tryPopLocal()) {
        if (workList_.localSize() > MarkDispatcher::kMinWorkSizeToShare) {
            shareWork();
        }
        auto obj = top->objHeader();

        // TODO: Consider moving it to the sweep phase to make this loop more tight.
        //       This, however, requires care with scheduler interoperation.
        markHandle.addObject(mm::GetAllocatedHeapSize(obj));

        mark::MarkTraits::processInMark(workList_, obj);

        // TODO: Consider moving it before processInMark to make the latter something of a tail call.
        if (auto* extraObjectData = mm::ExtraObjectData::Get(obj)) {
            gc::internal::processExtraObjectData<mark::MarkTraits>(markHandle, workList_, *extraObjectData, obj);
        }
    }
}

bool gc::mark::MarkDispatcher::MarkJob::shareWork() {
    // TODO consider sharing only half of the work
    //  to avoid pretty common sequence of "share all" -> "steal half from itself"
    RuntimeAssert(!workList_.localEmpty(), "There has to be something to share");
    auto shared = workList_.shareAll();
    if (shared > 0) {
        GCLogTrace(dispatcher_.gcHandle().getEpoch(),"Mark task %d has shared %zu tasks", carrierThreadId_, shared);
        if (dispatcher_.waitingJobs_.load(std::memory_order_relaxed) > 0) {
            dispatcher_.waitCV_.notify_all();
        }
    }
    return shared > 0;
}

// dispatcher

gc::mark::MarkDispatcher::MarkDispatcher(std::size_t gcWorkerPoolSize, bool mutatorsCooperate)
        : gcWorkerPoolSize_(gcWorkerPoolSize), mutatorsCooperate_(mutatorsCooperate) {}

void gc::mark::MarkDispatcher::beginMarkingEpoch(gc::GCHandle gcHandle) {
    gcHandle_ = gcHandle;

    lockedMutatorsList_ = mm::ThreadRegistry::Instance().LockForIter();

    // ensure there is enough storage for all the mark tasks possible:
    // one for each mutator existing at the moment and one for each gc thread
    auto maxPossibleMutators = mutatorsCooperate_ ? count(*lockedMutatorsList_) : 0;
    expectedJobs_ = maxPossibleMutators + gcWorkerPoolSize_;
    expandJobsArrayIfNeeded();
    GCLogDebug(gcHandle.getEpoch(), "Expecting at most %zu markers", expectedJobs_);
    registeredJobs_ = 0;
    RuntimeAssert(waitingJobs_ == 0, "There must be no workers waiting from the previous epoch");

    allDone_ = false;

    pacer_.beginEpoch(gcHandle.getEpoch());
}

void gc::mark::MarkDispatcher::waitForThreadsPauseMutation() noexcept {
    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "Dispatcher thread must not be registered");
    waitFast([this] {
        return allMutators([](mm::ThreadData& mut){ return mm::isSuspendedOrNative(mut) || mut.gc().impl().gc().cooperative(); });
    });
}

void gc::mark::MarkDispatcher::registerTask(MarkJob& task) {
    RuntimeAssert(pacer_.isRecruiting(), "Dispatcher must be ready to register new workers");
    RuntimeAssert(task.workList_.localEmpty(), "Mark queue of unregistered task must be empty (e.g. fully depleted during previous GC)");
    RuntimeAssert(task.workList_.sharedEmpty(), "Shared mark queue of unregistered task must be empty (e.g. fully depleted during previous GC)");
    RuntimeAssert(!allDone_, "Dispatcher must wait for every possible task to register before finishing mark");
    RuntimeAssert(!isRegistered(task), "Task registration is not idempotent");

    std::size_t newTaskIdx;
    while (true) {
        newTaskIdx = registeredJobs_.load(std::memory_order_acquire);
        RuntimeAssert(newTaskIdx < expectedJobs_, "Impossible to register more tasks than expected");
        MarkJob* expected = nullptr;
        if (jobs_[newTaskIdx].compare_exchange_weak(expected, &task, std::memory_order_relaxed)) {
            break;
        }
    }
    auto nowRegistered = newTaskIdx + 1;
    registeredJobs_.store(nowRegistered, std::memory_order_release);
    GCLogDebug(gcHandle().getEpoch(), "Mark task is registered on thread %d", konan::currentThreadId());

    if (nowRegistered == expectedJobs_) {
        GCLogDebug(gcHandle().getEpoch(), "Mark input is complete");
    }

    if (!task.workList_.sharedEmpty() && waitingJobs_.load(std::memory_order_relaxed) > 0) {
        waitCV_.notify_all();
    }
}

bool gc::mark::MarkDispatcher::isRegistered(const MarkJob& task) const {
    for (const auto& t: jobs_) {
        if (t.load(std::memory_order_relaxed) == &task) return true;
    }
    return false;
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

void gc::mark::MarkDispatcher::expandJobsArrayIfNeeded() {
    if (expectedJobs_ > jobs_.size()) {
        jobs_ = std_support::vector<std::atomic<MarkJob*>>(expectedJobs_);
    }
    for (auto& task : jobs_) {
        task.store(nullptr, std::memory_order_relaxed);
    }
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
    if (!mutatorsCooperate_) return; // nothing to care about

    // now that we know the exact number of cooperative mutators,
    // calculate expectedTasks_'s precise value
    RuntimeAssert(allMutators(
            [](mm::ThreadData& mut) { return mut.gc().impl().gc().rootSetLocked(); }), 
                  "All the mutators must be either cooperative or locked by other marker");
    auto exactExpectedJobs = count(*lockedMutatorsList_,
                                   [](mm::ThreadData& mut){ return mut.gc().impl().gc().cooperative(); })
                                           + gcWorkerPoolSize_;
    RuntimeAssert(exactExpectedJobs <= expectedJobs_, "Previous expectation must have been not less");
    expectedJobs_ = exactExpectedJobs;
    GCLogDebug(gcHandle().getEpoch(), "Expecting exactly %zu markers", expectedJobs_);
    RuntimeAssert(registeredJobs_.load(std::memory_order_relaxed) <= expectedJobs_, "Must not have registered more jobs than expected");
    RuntimeAssert(expectedJobs_ <= jobs_.size(), "Tasks array should already be allocated");
}
