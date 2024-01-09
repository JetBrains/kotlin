/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <mutex>

#include "GCStatistics.hpp"
#include "ManuallyScoped.hpp"
#include "ObjectData.hpp"
#include "ParallelProcessor.hpp"
#include "SafePoint.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

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
 *      Prevents the possibility of inserting a strong reference to a ewakly-reachable object behind the mark front.
 * 3. Concurrently, the marker thread builds a mark closure. Unmarked objects hidden in a mutator mark queues may still exist.
 * 4. Pause mutators once more, drain local mark queues, and complete the mark closure, this time non-concurrently.
 *    // TODO build closure fully concurrent
 * 5. Process and clean weak references. // TODO process weak refs concurrently
 * 6. Prepare mared heap for sweeping and resume mutation. // TODO prepare heap without a pause
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

        static void clear(AnyQueue& queue) noexcept {
            RuntimeAssert(queue.localEmpty(), "Mark queue must be empty");
        }

        static ALWAYS_INLINE ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
            auto* obj = queue.tryPop();
            if (obj) {
                auto object = alloc::objectForObjectData(*obj);
                RuntimeLogDebug({logging::Tag::kGCMark}, "Dequeued %p", object);
                return object;
            }
            return nullptr;
        }

        static ALWAYS_INLINE bool tryEnqueue(AnyQueue& queue, ObjHeader* object) noexcept {
            auto& objectData = alloc::objectDataForObject(object);
            bool pushed = queue.tryPush(objectData);
            if (pushed) {
                RuntimeLogDebug({logging::Tag::kGCMark}, "Enqueued %p", object);
            }
            return pushed;
        }

        static ALWAYS_INLINE bool tryMark(ObjHeader* object) noexcept {
            auto& objectData = alloc::objectDataForObject(object);
            bool pushed = objectData.tryMark();
            if (pushed) {
                RuntimeLogDebug({logging::Tag::kGCMark}, "Marked %p", object);
            }
            return pushed;
        }

        static ALWAYS_INLINE void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
            auto process = object->type_info()->processObjectInMark;
            RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);
            process(static_cast<void*>(&markQueue), object);
        }
    };

    class ThreadData : private Pinned {
    public:
        auto& markQueue() noexcept { return markQueue_; }
    private:
        ManuallyScoped<MutatorQueue> markQueue_{};
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

private:
    GCHandle& gcHandle();

    void completeMutatorsRootSet(MarkTraits::MarkQueue& markQueue);
    void tryCollectRootSet(mm::ThreadData& thread, ParallelProcessor::Worker& markQueue);
    void parallelMark(ParallelProcessor::Worker& worker);

    void resetMutatorFlags();

    GCHandle gcHandle_ = GCHandle::invalid();
    std::optional<mm::ThreadRegistry::Iterable> lockedMutatorsList_;
    ManuallyScoped<ParallelProcessor> parallelProcessor_{};
};
} // namespace kotlin::gc::mark
