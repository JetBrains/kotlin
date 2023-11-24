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
 * TODO
 */
class ConcurrentMark : private Pinned {
    using MarkStackImpl = intrusive_forward_list<GC::ObjectData>;
    // work balancing parameters were chosen pretty arbitrary

public:
    using ParallelProcessor = ParallelProcessor<MarkStackImpl, 512, 4096>;
    using MutatorQueue = ParallelProcessor::WorkSource;

    class MarkTraits {
    public:
        using MarkQueue = ParallelProcessor::Worker;
        using AnyQueue = ParallelProcessor::WorkSource;

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

    void beginMarkingEpoch(gc::GCHandle gcHandle);
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

