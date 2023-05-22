#pragma once

#include <mutex>
#include <condition_variable>

#include "GCStatistics.hpp"
#include "MarkStack.hpp"
#include "std_support/Vector.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"
#include "parproc/ParallelProcessor.hpp"
#include "parproc/CooperativeWorkLists.hpp"
#include "ManuallyScoped.hpp"

namespace kotlin::gc::mark {

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

    static constexpr std::size_t kMaxWorkers = 512;

    using MarkStackImpl = intrusive_forward_list<ObjectData>;

    template<typename WorkProcessor>
    using CoopWorkListImpl = SharableQueuePerWorker<WorkProcessor, MarkStackImpl, 256>;

    using ParallelProcessor = ParallelProcessor<kMaxWorkers, CoopWorkListImpl>;

public:
    class MarkTraits {
    public:
        using MarkQueue = ParallelProcessor::WorkListInterface;
        using ObjectFactory = ObjectData::ObjectFactory;

        static void clear(MarkQueue& queue) noexcept {
            RuntimeAssert(queue.empty(), "Mark queue must be empty");
        }

        static ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
            if (auto* obj = queue.tryPop()) {
                auto node = ObjectFactory::NodeRef::From(*obj);
                return node->GetObjHeader();
            }
            return nullptr;
        }

        static bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
            auto& objectData = ObjectFactory::NodeRef::From(object).ObjectData();
            return queue.tryPush(objectData);
        }

        static bool tryMark(ObjHeader* object) noexcept {
            auto& objectData = ObjectFactory::NodeRef::From(object).ObjectData();
            return objectData.tryMark();
        }

        static void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
            auto process = object->type_info()->processObjectInMark;
            RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);
            process(static_cast<void*>(&markQueue), object);
        }
    };

    /** See `MarkDispatcher`. */
    class MarkJob : private Pinned {
        friend class MarkDispatcher;
    public:
        explicit MarkJob(MarkDispatcher& dispatcher);

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
        void completeMutatorsRootSet(MarkTraits::MarkQueue& markQueue);
        void collectRootSet(mm::ThreadData& thread, MarkTraits::MarkQueue& markQueue);

        void parallelMark(ParallelProcessor::Worker& worker);

        MarkDispatcher& dispatcher_;
    };

    explicit MarkDispatcher(std::size_t gcWorkerPoolSize, bool mutatorsCooperate);

    void beginMarkingEpoch(gc::GCHandle gcHandle);
    void waitForThreadsPauseMutation() noexcept;
    void endMarkingEpoch();

    void requestShutdown();
    bool shutdownRequested() const;

    template<typename Pred>
    void reset(std::size_t maxParallelism, bool mutatorsCooperate, size_t gcWorkerPoolSize, Pred waitForWorkersToFinish) {
        pacer_.requestShutdown();
        waitForWorkersToFinish();
        pacer_.reset();
        setParallelismLevel(maxParallelism, mutatorsCooperate, gcWorkerPoolSize);
    }

private:
    GCHandle& gcHandle();

    void setParallelismLevel(size_t maxParallelism, bool mutatorsCooperate, size_t gcWorkerPoolSize);

    bool authorizeNewWorkerCreation();

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

    std::size_t maxParallelism_ = 1;
    std::size_t gcWorkerPoolSize_ = 1;
    bool mutatorsCooperate_ = false;


    GCHandle gcHandle_ = GCHandle::invalid();
    MarkPacer pacer_;

    std::optional<mm::ThreadRegistry::Iterable> lockedMutatorsList_;

    ManuallyScoped<ParallelProcessor> parallelProcessor_;
    std::atomic<std::size_t> activeWorkers_ = 0;
};

} // namespace kotlin::gc::mark

