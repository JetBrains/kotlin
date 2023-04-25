#pragma once

#include <mutex>
#include <condition_variable>

#include "GCStatistics.hpp"
#include "MarkStack.hpp"
#include "std_support/Vector.hpp"
#include "CooperativeIntrusiveList.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

namespace kotlin::gc::mark {

namespace test {
class ParallelMarkTestSupport;
}

class MarkPacer : private Pinned {
public:
    enum class Phase {
        /** Mark is not in progress. */
        kIdle,
        /**
         * MarkDispatcher is ready to recruit new workers.
         *
         * In case of cooperative mark mutator threads are welcome to mark their own root sets.
         * Each thread is free to start as soon as it reaches a safe point.
         * No need to wait for others.
         */
        kReady,
        /**
         * All mutator threads must be in a safe state at this point:
         * 1) Suspended on a safe point;
         * 2) In the native code;
         * 3) Registered as cooperative markers during previous phase.
         *
         * Now all the GC workers are summoned to participate in root set collection and mark.
         * Parallel mark can't stop before all the expected workers begun the marking.
         */
        kParallelMark,
        /** A shutdown was requested. There is nothing more to wait for. */
        kShutdown,
    };

    void beginEpoch(uint64_t epoch);
    void waitNewEpochReadyOrShutdown() const;

    bool isRecruiting() const;

    void requestParallelMark();
    void waitForParallelMark() const;

    void reset();
    void waitEpochFinished(uint64_t epoch) const;

    void requestShutdown();
    bool shutdownRequested() const;

private:
    bool is(Phase phase) const;
    void waitFor(Phase phase) const;
    void begin(Phase phase);

    std::atomic<uint64_t> epoch_ = 0;
    std::atomic<Phase> phase_ = Phase::kIdle;
    mutable std::mutex mutex_;
    mutable std::condition_variable cond_;
};

/**
 * Parallel mark dispatcher.
 * Mark can be performed on one or more threads.
 * Each threads wanting to participate have to:
 * instantiate a `MarkJob` and execute an appropriate run- routine when ready to mark.
 * There must be exactly one executor of a `MarkJob.runMainInSTW()`.
 *
 * Mark jobs are able to balance work between each other through sharing/stealing.
 */
class MarkDispatcher : private Pinned {
    friend test::ParallelMarkTestSupport;
public:
    static const std::size_t kInitialJobsArraySize = 128; // big enough to avoid most of the possible reallocations

    // These numbers were chosen pretty arbitrary.
    /** A marker will from time to time share this amount of jobs with others. */
    static const std::size_t kMinWorkSizeToShare = 256;
    /** A marker depleted of work will try to steal at most `kMaxWorkSieToSteal` jobs from a victim worker at once. */
    static const std::size_t kMaxWorkSizeToSteal = kMinWorkSizeToShare / 2;
    /** A marker will iterate over other workers this number of times searching for a victim for work-stealing. */
    static const std::size_t kStealingAttemptCyclesBeforeWait = 4;

    /** See `MarkDispatcher`. */
    class MarkJob : private Pinned {
        friend class MarkDispatcher;
        friend test::ParallelMarkTestSupport;
    public:
        explicit MarkJob(MarkDispatcher& dispatcher) : dispatcher_(dispatcher) {}

        /** To be run by a single "main" GC thread during STW. */
        void runMainInSTW();
        /**
         * To be run by mutator threads that would like to participate in mark.
         * Will wait for STW detection by a "main" routine.
         */
        void runOnMutator(mm::ThreadData& mutatorThread);
        /**
         * To be run by auxiliary GC threads.
         * Will wait for STW detection by a "main" routine.
         */
        void runAuxiliary();

    private:
        void completeMutatorsRootSet();
        void collectRootSet(mm::ThreadData& thread);

        void parallelMark();
        bool tryAcquireWork();
        void performWork(GCHandle::GCMarkScope& markHandle);
        bool shareWork();

        MarkDispatcher& dispatcher_;
        CooperativeIntrusiveList<ObjectData> workList_;

        // used in logs
        const int carrierThreadId_ = konan::currentThreadId();
    };

    explicit MarkDispatcher(std::size_t gcWorkerPoolSize, bool mutatorsCooperate);

    void beginMarkingEpoch(gc::GCHandle gcHandle);
    void waitForThreadsPauseMutation() noexcept;

    void requestShutdown();
    bool shutdownRequested() const;

    template<typename Pred>
    void reset(bool mutatorsCooperate, size_t gcWorkerPoolSize, Pred waitForWorkersToFinish) {
        pacer_.requestShutdown();
        waitForWorkersToFinish();
        pacer_.reset();
        setParallelismLevel(mutatorsCooperate, gcWorkerPoolSize);
    }

private:
    GCHandle& gcHandle();

    void setParallelismLevel(bool mutatorsCooperate, size_t gcWorkerPoolSize);

    void expandJobsArrayIfNeeded();

    void registerTask(MarkJob& task);
    // primarily to be used in assertions
    bool isRegistered(const MarkJob& task) const;

    void allCooperativeMutatorsAreRegistered();

    template <typename Pred>
    bool allMutators(Pred predicate) noexcept {
        for (auto& thread : *lockedMutatorsList_) {
            if (!predicate(thread)) {
                return false;
            }
        }
        return true;
    }
    void resetMutatorFlags();

    std::size_t gcWorkerPoolSize_ = 1;
    bool mutatorsCooperate_ = false;

    GCHandle gcHandle_ = GCHandle::invalid();
    MarkPacer pacer_;

    std::optional<mm::ThreadRegistry::Iterable> lockedMutatorsList_;

    std_support::vector<std::atomic<MarkJob*>> jobs_{kInitialJobsArraySize};
    std::size_t expectedJobs_ = 0;
    std::atomic<std::size_t> registeredJobs_ = 0;
    std::atomic<std::size_t> waitingJobs_ = 0;
    std::atomic<bool> allDone_ = false;
    std::mutex waitMutex_;
    std::condition_variable waitCV_;
};

} // namespace kotlin::gc::mark

