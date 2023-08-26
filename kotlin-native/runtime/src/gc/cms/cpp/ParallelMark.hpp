#pragma once

#include <mutex>
#include <condition_variable>

#include "GCStatistics.hpp"
#include "ManuallyScoped.hpp"
#include "ObjectData.hpp"
#include "ParallelProcessor.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"
#include "std_support/Vector.hpp"

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
         * Now all the GC workers are summoned to participate in a root set collection.
         */
        kRootSet,
        /**
         * Root set is collected. No more workers can be instantiated, time to begin parallel mark.
         * Parallel mark can't stop before all the created workers begin the marking.
         */
        kParallelMark,
        /** A shutdown was requested. There is nothing more to wait for. */
        kShutdown,
    };

    bool is(Phase phase) const;
    void begin(Phase phase);
    void wait(Phase phase);

    void beginEpoch(uint64_t epoch);
    void waitNewEpochReadyOrShutdown() const;
    void waitEpochFinished(uint64_t epoch) const;

    bool acceptingNewWorkers() const;

private:
    std::atomic<uint64_t> epoch_ = 0;
    std::atomic<Phase> phase_ = Phase::kIdle;
    mutable std::mutex mutex_;
    mutable std::condition_variable cond_;
};

/**
 * Parallel mark dispatcher.
 * Mark can be performed on one or more threads.
 * Each threads wanting to participate have to execute an appropriate run- routine when ready to mark.
 * There must be exactly one executor of a `runMainInSTW()`.
 *
 * Mark workers are able to balance work between each other through sharing/stealing.
 */
class ParallelMark : private Pinned {
    using MarkStackImpl = intrusive_forward_list<GC::ObjectData>;
    // work balancing parameters were chosen pretty arbitrary
    using ParallelProcessor = ParallelProcessor<MarkStackImpl, 512, 4096>;
public:
    class MarkTraits {
    public:
        using MarkQueue = ParallelProcessor::Worker;

        static void clear(MarkQueue& queue) noexcept {
            RuntimeAssert(queue.localEmpty(), "Mark queue must be empty");
        }

        static ALWAYS_INLINE ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
            auto* obj = compiler::gcMarkSingleThreaded() ? queue.tryPopLocal() : queue.tryPop();
            if (obj) {
                return objectForObjectData(*obj);
            }
            return nullptr;
        }

        static ALWAYS_INLINE bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
            auto& objectData = objectDataForObject(object);
            return compiler::gcMarkSingleThreaded() ? queue.tryPushLocal(objectData) : queue.tryPush(objectData);
        }

        static ALWAYS_INLINE bool tryMark(ObjHeader* object) noexcept {
            auto& objectData = objectDataForObject(object);
            return objectData.tryMark();
        }

        static ALWAYS_INLINE void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
            auto process = object->type_info()->processObjectInMark;
            RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);
            process(static_cast<void*>(&markQueue), object);
        }
    };

    ParallelMark(bool mutatorsCooperate);

    void beginMarkingEpoch(gc::GCHandle gcHandle);
    void waitForThreadsPauseMutation() noexcept;
    void endMarkingEpoch();

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

    void requestShutdown();
    bool shutdownRequested() const;

    template<typename Pred>
    void reset(std::size_t maxParallelism, bool mutatorsCooperate, Pred waitForWorkersToFinish) {
        pacer_.begin(MarkPacer::Phase::kShutdown);
        waitForWorkersToFinish();
        pacer_.begin(MarkPacer::Phase::kIdle);
        setParallelismLevel(maxParallelism, mutatorsCooperate);
    }

private:
    GCHandle& gcHandle();

    void setParallelismLevel(size_t maxParallelism, bool mutatorsCooperate);

    template <typename Pred>
    bool allMutators(Pred predicate) noexcept {
        for (auto& thread : *lockedMutatorsList_) {
            if (!predicate(thread)) {
                return false;
            }
        }
        return true;
    }

    void completeRootSetAndMark(ParallelProcessor::Worker& parallelWorker);
    void completeMutatorsRootSet(MarkTraits::MarkQueue& markQueue);
    void tryCollectRootSet(mm::ThreadData& thread, ParallelProcessor::Worker& markQueue);
    void parallelMark(ParallelProcessor::Worker& worker);

    std::optional<ParallelProcessor::Worker> createWorker();

    void resetMutatorFlags();

    std::size_t maxParallelism_ = 1;
    bool mutatorsCooperate_ = false;

    GCHandle gcHandle_ = GCHandle::invalid();
    MarkPacer pacer_;
    std::optional<mm::ThreadRegistry::Iterable> lockedMutatorsList_;
    ManuallyScoped<ParallelProcessor> parallelProcessor_{};

    std::mutex workerCreationMutex_;
    std::atomic<std::size_t> activeWorkersCount_ = 0;
};

} // namespace kotlin::gc::mark

