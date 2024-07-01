/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <mutex>

#include "GCStatistics.hpp"
#include "ManuallyScoped.hpp"
#include "ObjectData.hpp"
#include "concurrent/ParallelProcessor.hpp"
#include "SafePoint.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"
#include "concurrent/Once.hpp"


namespace kotlin::gc::mark {

/**
 * Implementation of a Mark GC phase, that runs concurrent with mutator threads, using a "Snapshot at the Beginning" approach.
 * Steps:
 * 1. Pause all mutators, collect their root sets.
 * 2. Mutators resume, maintaining a weak tri-collor invariant with the help of:
 *    - Deletion write barrier (Dijkstra et al):
 *      Remembers overwritten values in a thread-local mark queue.
 *      Barrier+write combination doesn't need to be atomic,
 *      as only references from mark phase beginning matter for the SatB approach.
 *    - Read barrier for weak refs:
 *      Remembers each object read via a weak reference in a thread-local mark queue.
 *      Prevents the possibility of inserting a strong reference to a weakly-reachable object behind the mark front.
 * 3. Concurrently, the marker thread builds a mark closure.
 *    From time to time the mutator threads flush their mark queues into the global one.
 * 4. In case the mark closure is complete, replace the remembering weak read barrier with
 *    the barrier that hides unmarked (dead) referents.
 * 5. Process and clean weak references.
 * 6. Pause mutators once again and disable all the barreirs.
 * 7. Prepare marked heap for sweeping and resume mutation. // TODO prepare heap without a pause
 */
class ConcurrentMark : private Pinned {
    using MarkStackImpl = intrusive_forward_list<GC::ObjectData>;

public:
    // work balancing parameters were chosen pretty arbitrary
    using ParallelProcessor = ParallelProcessor<MarkStackImpl, 512, 4096>;
    using MutatorQueue = ParallelProcessor::WorkSource;

    class MarkTraits {
    public:
        using MarkQueue = ParallelProcessor::Worker;
        using AnyQueue = ParallelProcessor::WorkSource;

        static constexpr auto kAllowHeapToStackRefs = false;

        ALWAYS_INLINE static void clear(AnyQueue& queue) noexcept { RuntimeAssert(queue.localEmpty(), "Mark queue must be empty"); }

        static PERFORMANCE_INLINE ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
            auto* obj = queue.tryPop();
            if (obj) {
                auto object = alloc::objectForObjectData(*obj);
                RuntimeLogDebug({logging::Tag::kGCMark}, "Dequeued %p", object);
                return object;
            }
            return nullptr;
        }

        static PERFORMANCE_INLINE bool tryEnqueue(AnyQueue& queue, ObjHeader* object) noexcept {
            auto& objectData = alloc::objectDataForObject(object);
            bool pushed = queue.tryPush(objectData);
            if (pushed) {
                RuntimeLogDebug({logging::Tag::kGCMark}, "Enqueued %p", object);
            }
            return pushed;
        }

        static PERFORMANCE_INLINE bool tryMark(ObjHeader* object) noexcept {
            auto& objectData = alloc::objectDataForObject(object);
            bool pushed = objectData.tryMark();
            if (pushed) {
                RuntimeLogDebug({logging::Tag::kGCMark}, "Marked %p", object);
            }
            return pushed;
        }

        static PERFORMANCE_INLINE void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
            auto process = object->type_info()->processObjectInMark;
            RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);
            process(static_cast<void*>(&markQueue), object);
        }
    };

    class ThreadData : private Pinned {
        friend ConcurrentMark;

    public:
        auto& markQueue() noexcept { return markQueue_; }
        void onSafePoint() noexcept;

    private:
        void ensureFlushActionExecuted() noexcept;

        ManuallyScoped<MutatorQueue, true> markQueue_{};
        ManuallyScoped<OnceExecutable, true> flushAction_{};
    };

    void beginMarkingEpoch(GCHandle gcHandle);
    void endMarkingEpoch();

    /** To be run by a single "main" GC thread during STW. */
    void runMainInSTW();

    /**
     * To be run by mutator threads that would like to participate in mark.
     * Will wait for STW detection by a "main" routine.
     */
    void runOnMutator(mm::ThreadData& mutatorThread);

    /**
     * Weak reference reads may be mutually exclusive with certain parts of mark oprocess.
     * Every read must be guarded by the object returned by this method.
     */
    auto weakReadProtector() noexcept {
        auto markTerminationGuard = std::shared_lock{markTerminationMutex_, std::defer_lock};
        while (!markTerminationGuard.try_lock()) {
            mm::safePoint();
        }
        return markTerminationGuard;
    }

private:
    GCHandle& gcHandle();

    void completeMutatorsRootSet(MarkTraits::MarkQueue& markQueue);
    void tryCollectRootSet(mm::ThreadData& thread, ParallelProcessor::Worker& markQueue);
    bool tryTerminateMark(std::size_t& everSharedBatches) noexcept;
    void flushMutatorQueues() noexcept;

    void resetMutatorFlags();

    GCHandle gcHandle_ = GCHandle::invalid();
    std::optional<mm::ThreadRegistry::Iterable> lockedMutatorsList_;
    ManuallyScoped<ParallelProcessor, true> parallelProcessor_{};

    RWSpinLock<MutexThreadStateHandling::kIgnore> markTerminationMutex_;
};

namespace test_support {
bool flushActionRequested();
}

} // namespace kotlin::gc::mark
