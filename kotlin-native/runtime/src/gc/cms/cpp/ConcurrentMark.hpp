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

        static void clear(MarkQueue& queue) noexcept {
            RuntimeAssert(queue.localEmpty(), "Mark queue must be empty");
        }

        static ALWAYS_INLINE ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
            auto* obj = compiler::gcMarkSingleThreaded() ? queue.tryPopLocal() : queue.tryPop();
            if (obj) {
                return alloc::objectForObjectData(*obj);
            }
            return nullptr;
        }

        static ALWAYS_INLINE bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
            auto& objectData = alloc::objectDataForObject(object);
            return compiler::gcMarkSingleThreaded() ? queue.tryPushLocal(objectData) : queue.tryPush(objectData);
        }

        static ALWAYS_INLINE bool tryMark(ObjHeader* object) noexcept {
            auto& objectData = alloc::objectDataForObject(object);
            return objectData.tryMark();
        }

        static ALWAYS_INLINE void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
            auto process = object->type_info()->processObjectInMark;
            RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);
            process(static_cast<void*>(&markQueue), object);
        }
    };

    class ThreadData : private Pinned {
    public:
        class FlushAction : public mm::OncePerThreadAction<FlushAction> {
        public:
            static OncePerThreadAction::ThreadData& getUtilityData(mm::ThreadData& threadData);
            static void action(mm::ThreadData& threadData) noexcept;
        };

        ThreadData(mm::ThreadData& base);

        auto& markQueue() noexcept { return markQueue_; }

    private:
        ManuallyScoped<mark::ConcurrentMark::MutatorQueue> markQueue_;
        FlushAction::ThreadData flushActionData_;
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

