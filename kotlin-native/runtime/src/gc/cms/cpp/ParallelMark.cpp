#include "ParallelMark.hpp"

#include "Barriers.hpp"
#include "MarkStack.hpp"
#include "MarkAndSweepUtils.hpp"
#include "GCStatistics.hpp"
#include "SafePoint.hpp"
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

void flushLocalQueue0(mm::ThreadData& thread) {
    if (!thread.gc().impl().gc().markQueue_->localEmpty()) {
        bool flushed = thread.gc().impl().gc().markQueue_->forceFlush();
        if (!flushed) {
            RuntimeLogDebug({kTagGC}, "Failed to flush local queue (tid#%d) !!!!!!!!", thread.threadId());
        }
        thread.gc().impl().gc().gc_.markDispatcher_.newWork_ = true; // TODO release
        RuntimeLogDebug({kTagGC}, "Local queue (tid#%d) force flush", thread.threadId());
    } else {
        RuntimeLogDebug({kTagGC}, "Local queue (tid#%d) is empty", thread.threadId());
    }
}

void flushLocalQueue(mm::ThreadData& thread) {
    std::unique_lock lock(thread.gc().impl().gc().flushMutex_);
    flushLocalQueue0(thread);
}

} // namespace

bool gc::mark::MarkPacer::is(gc::mark::MarkPacer::Phase phase) const {
    return phase_.load(std::memory_order_relaxed) == phase;
}

void gc::mark::MarkPacer::begin(gc::mark::MarkPacer::Phase phase) {
    {
        std::unique_lock lock(mutex_);
        phase_.store(phase, std::memory_order_relaxed);
    }
    cond_.notify_all();
}

void gc::mark::MarkPacer::wait(gc::mark::MarkPacer::Phase phase) {
    if (phase_.load(std::memory_order_relaxed) >= phase) return;
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
    cond_.wait(lock, [this]() { return (phase_.load(std::memory_order_relaxed) >= Phase::kReady); });
}

void gc::mark::MarkPacer::waitEpochFinished(uint64_t currentEpoch) const {
    std::unique_lock lock(mutex_);
    cond_.wait(lock, [this, currentEpoch]() {
        return is(Phase::kIdle) || is(Phase::kShutdown) || epoch_.load(std::memory_order_relaxed) > currentEpoch;
    });
}

bool gc::mark::MarkPacer::acceptingNewWorkers() const {
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

void gc::mark::ParallelMark::waitForThreadsPauseMutation() noexcept {
    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "Dispatcher thread must not be registered");
    spinWait([this] {
        return allMutators([](mm::ThreadData& mut) {
            return mm::isSuspendedOrNative(mut) || mut.gc().impl().gc().cooperative();
        });
    });
}

void gc::mark::ParallelMark::endMarkingEpoch() {
    if (!compiler::gcMarkSingleThreaded()) {
        // We must now wait for every worker to finish the Mark procedure:
        // wake up from possible waiting, publish statistics, etc.
        // Only then it's safe to destroy the parallelProcessor and proceed to other GC tasks such as sweep.
        // FIXME spinWait([=]() { return activeWorkersCount_.load(std::memory_order_relaxed) == 0; });

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

        for (auto& thread : *lockedMutatorsList_) {
            thread.gc().impl().gc().markQueue_.construct(*parallelProcessor_);
        }

/////////
//        // wait for threads to confirm mark queue creation
//        // NOTE at this point they can already start marking (=enqueueing) something
//        waitForThreadsToReachCheckpoint();
//
//        // FIXME
//        //     at this point a mutator can already be black
//        //     but write barriers are not yet active
//
//        // FIXME serial RS collection :c
//        EnableMarkBarriers();
//        {
//            for (auto& mut : *lockedMutatorsList_) {
//                mut.gc().impl().gc().barriers().resetCheckpoint();
//            }
//            mm::SafePointActivator safePointActivator;
//
//            spinWait([this, &mainWorker] {
//                return allMutators([this, &mainWorker](mm::ThreadData& mut) {
//                    auto& gcData = mut.gc().impl().gc();
//                    if (gcData.barriers().visitedCheckpoint()) return true;
//
//                    if (mut.suspensionData().suspendedOrNative()) {
//                        tryCollectRootSet(mut, mainWorker, false);
//                        return true;
//                    }
//                    return false;
//                });
//            });
//        }
//
//        spinWait([this] {
//            return allMutators([](mm::ThreadData& mut) { return mut.gc().impl().gc().rootSetCollected_.load(std::memory_order_acquire); });
//        });
//
/////////

        {
            bool didSuspend = mm::RequestThreadsSuspension();
            RuntimeAssert(didSuspend, "Only GC thread can request suspension");
            // TODO gcHandle().suspensionRequested();
            RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "GC must run on unregistered thread");
            mm::WaitForThreadsSuspension();
            GCLogDebug(gcHandle().getEpoch(), "All threads have paused mutation");
            // TODO gcHandle().threadsAreSuspended();

            // NOTE Before suspend a thread will try to collect it's root set

            // In STW
            EnableMarkBarriers();
            // TODO collect RS somewhere here?

            mm::ResumeThreads();
            // TODO gcHandle().threadsAreResumed();
        }

        {
            // for threads to wait unlit their RS's are collected
            mm::SafePointActivator spGuard;

            // complete RS for those who were in native code
            pacer_.begin(MarkPacer::Phase::kRootSet);
            completeMutatorsRootSet(mainWorker);
            spinWait([this] {
                return allMutators([](mm::ThreadData& mut) { return mut.gc().impl().gc().rootSetCollected_.load(std::memory_order_acquire); });
            });
        }

/////////

        // global root set must be collected after all the mutator's global data have been published
        // FIXME lock special refs registry for iteration!!!
        collectRootSetGlobals<MarkTraits>(gcHandle(), mainWorker);

        pacer_.begin(MarkPacer::Phase::kParallelMark);
        //parallelMark(mainWorker);
        {
            std::size_t iter = 0;
            while (true) {
                GCLogDebug(gcHandle().getEpoch(), "Mark loop %zu has begun", iter);
                Mark<MarkTraits>(gcHandle(), mainWorker);
                parallelProcessor_->undo();
                
                // flush all queues
                {
                    newWork_ = false;
                    for (auto& mut : *lockedMutatorsList_) {
                        mut.gc().impl().gc().barriers().resetCheckpoint();
                    }
                    mm::SafePointActivator safePointActivator;

                    spinWait([this] {
                        GCLogDebug(gcHandle().getEpoch(), "Checking all mutators");
                        return allMutators([](mm::ThreadData& mut) {
                            auto& gcData = mut.gc().impl().gc();
                            if (gcData.barriers().visitedCheckpoint()) return true;
                            if (mut.suspensionData().suspendedOrNative()) {
                                std::unique_lock lock(gcData.flushMutex_);
                                flushLocalQueue0(mut);
                                gcData.barriers().onCheckpoint();
                                return true;
                            }
                            return false;
                        });
                    });
                }
                GCLogDebug(gcHandle().getEpoch(), "Mutator queues flushed");

                if (!newWork_) {
                    GCLogDebug(gcHandle().getEpoch(), "No new work found");
                    RuntimeAssert(!parallelProcessor_->workAvailable(), "Must be no work");
                    break;
                }
                GCLogDebug(gcHandle().getEpoch(), "New work found");
                ++iter;
            }
            GCLogDebug(gcHandle().getEpoch(), "Stopping parallel processing");
            parallelProcessor_->stop();

            {
                std::unique_lock guard(workerCreationMutex_);
                activeWorkersCount_.fetch_sub(1, std::memory_order_relaxed);
                pacer_.begin(MarkPacer::Phase::kTearDown);
            }

            // We must now wait for every worker to finish the Mark procedure:
            // wake up from possible waiting, publish statistics, etc.
            // Only then it's safe to destroy the parallelProcessor and proceed to other GC tasks such as sweep.
            spinWait([=]() { return activeWorkersCount_.load(std::memory_order_relaxed) == 0; });
        }
    }
}

void gc::mark::ParallelMark::onSafePoint(mm::ThreadData& mutatorThread) {
    auto& gcData = mutatorThread.gc().impl().gc();
    if (pacer_.is(MarkPacer::Phase::kReady) || pacer_.is(MarkPacer::Phase::kRootSet)) {
        tryCollectRootSet(mutatorThread, *gcData.markQueue_, true);
        RuntimeAssert(gcData.rootSetCollected_, "The root set must be collected");
        // this is the release?
        flushLocalQueue(mutatorThread);
    }
    if (pacer_.is(MarkPacer::Phase::kParallelMark)) {
        RuntimeAssert(gcData.rootSetCollected_, "The root set must be collected");
        flushLocalQueue(mutatorThread);
    }
    if (pacer_.is(MarkPacer::Phase::kIdle)) {
        // FIXME use after deinit
        // RuntimeAssert(gcData.markQueue_->localEmpty(), "The queue must be empty");
    }
}

void gc::mark::ParallelMark::runOnMutator(mm::ThreadData& mutatorThread) {
    RuntimeAssert(false, "Should not suspend mutators in CMS");

    if (compiler::gcMarkSingleThreaded() || !mutatorsCooperate_) return;

    auto epoch = gcHandle().getEpoch();
    auto parallelWorker = createWorker();
    if (parallelWorker) {
        auto& gcData = mutatorThread.gc().impl().gc();
        gcData.beginCooperation();
        GCLogDebug(epoch, "Mutator thread cooperates in marking");

        tryCollectRootSet(mutatorThread, *parallelWorker, false);

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

void gc::mark::ParallelMark::completeMutatorsRootSet(MarkTraits::MarkQueue& workerMarkQueue) {
    // workers compete for mutators to collect their root set
    for (auto& mutator: *lockedMutatorsList_) {
        tryCollectRootSet(mutator, workerMarkQueue, false);
    }
}

void gc::mark::ParallelMark::tryCollectRootSet(mm::ThreadData& thread, ParallelProcessor::WorkSource& markQueue, bool block) {
    auto& gcData = thread.gc().impl().gc();
    std::unique_lock lock(gcData.rootSetMutex_, std::defer_lock);
    if (block) {
        lock.lock();
    } else {
        bool locked = lock.try_lock();
        if (!locked) return;
    }
    RuntimeAssert(lock, "Must be locked");
    // TODO maybe replace orders by relaxed? (and write a comment about mutex etc.)
    if (gcData.rootSetCollected_.load(std::memory_order_acquire)) return;

    GCLogDebug(gcHandle().getEpoch(), "Root set collection on thread %d for thread %d",
               konan::currentThreadId(), thread.threadId());
    gcData.publish();
    collectRootSetForThread<MarkTraits>(gcHandle(), markQueue, thread);
    gcData.rootSetCollected_.store(true, std::memory_order_release);
}

void gc::mark::ParallelMark::parallelMark(ParallelProcessor::Worker& worker) {
    std::size_t iter = 0;
    while (pacer_.is(MarkPacer::Phase::kParallelMark)) {
        GCLogDebug(gcHandle().getEpoch(), "Mark loop %zu has begun", iter);
        Mark<MarkTraits>(gcHandle(), worker);
        std::this_thread::yield();
        // TODO rest a little bit more?
        ++iter;
    }

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
            // TODO mutex?
            std::unique_lock lock(gcData.rootSetMutex_);
            gcData.markQueue_.destroy();
        }
        gcData.clearMarkFlags();
    }
}
