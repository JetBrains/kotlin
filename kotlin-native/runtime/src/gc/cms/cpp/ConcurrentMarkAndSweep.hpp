/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstddef>

#include "Allocator.hpp"
#include "Barriers.hpp"
#include "FinalizerProcessor.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "GCStatistics.hpp"
#include "IntrusiveList.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ObjectFactory.hpp"
#include "ScopedThread.hpp"
#include "ThreadData.hpp"
#include "Types.h"
#include "Utils.hpp"
#include "std_support/Memory.hpp"

#ifdef CUSTOM_ALLOCATOR
#include "CustomAllocator.hpp"
#include "CustomFinalizerProcessor.hpp"
#include "Heap.hpp"
#endif

namespace kotlin {
namespace gc {

// Stop-the-world parallel mark + concurrent sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
// TODO: Also make marking run concurrently with Kotlin threads.
class ConcurrentMarkAndSweep : private Pinned {
public:
    class ObjectData {
    public:
        bool tryMark() noexcept {
            return trySetNext(reinterpret_cast<ObjectData*>(1));
        }

        bool marked() const noexcept { return next() != nullptr; }

        bool tryResetMark() noexcept {
            if (next() == nullptr) return false;
            next_.store(nullptr, std::memory_order_relaxed);
            return true;
        }

    private:
        friend struct DefaultIntrusiveForwardListTraits<ObjectData>;

        ObjectData* next() const noexcept { return next_.load(std::memory_order_relaxed); }
        void setNext(ObjectData* next) noexcept {
            RuntimeAssert(next, "next cannot be nullptr");
            next_.store(next, std::memory_order_relaxed);
        }
        bool trySetNext(ObjectData* next) noexcept {
            RuntimeAssert(next, "next cannot be nullptr");
            ObjectData* expected = nullptr;
            return next_.compare_exchange_strong(expected, next, std::memory_order_relaxed);
        }

        std::atomic<ObjectData*> next_ = nullptr;
    };

    enum MarkingBehavior { kMarkOwnStack, kDoNotMark };

    using MarkQueue = intrusive_forward_list<ObjectData>;

    class ThreadData : private Pinned {
    public:
        using ObjectData = ConcurrentMarkAndSweep::ObjectData;

        using Allocator = AllocatorWithGC<Allocator, ThreadData>;

        explicit ThreadData(
                ConcurrentMarkAndSweep& gc, mm::ThreadData& threadData, gcScheduler::GCSchedulerThreadData& gcScheduler) noexcept :
            gc_(gc), threadData_(threadData), gcScheduler_(gcScheduler) {}
        ~ThreadData() = default;

        void SafePointAllocation(size_t size) noexcept;

        void Schedule() noexcept;
        void ScheduleAndWaitFullGC() noexcept;
        void ScheduleAndWaitFullGCWithFinalizers() noexcept;

        void OnOOM(size_t size) noexcept;

        void OnSuspendForGC() noexcept;

        void safePoint() noexcept { barriers_.onCheckpoint(); }

        Allocator CreateAllocator() noexcept { return Allocator(gc::Allocator(), *this); }

        BarriersThreadData& barriers() noexcept { return barriers_; }

    private:
        friend ConcurrentMarkAndSweep;
        ConcurrentMarkAndSweep& gc_;
        mm::ThreadData& threadData_;
        gcScheduler::GCSchedulerThreadData& gcScheduler_;
        std::atomic<bool> marking_;
        BarriersThreadData barriers_;
    };

    using Allocator = ThreadData::Allocator;

#ifndef CUSTOM_ALLOCATOR
    using FinalizerQueue = mm::ObjectFactory<ConcurrentMarkAndSweep>::FinalizerQueue;
    using FinalizerQueueTraits = mm::ObjectFactory<ConcurrentMarkAndSweep>::FinalizerQueueTraits;
#else
    using FinalizerQueue = alloc::FinalizerQueue;
    using FinalizerQueueTraits = alloc::FinalizerQueueTraits;
#endif

#ifdef CUSTOM_ALLOCATOR
    explicit ConcurrentMarkAndSweep(gcScheduler::GCScheduler& scheduler) noexcept;
#else
    ConcurrentMarkAndSweep(mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory, gcScheduler::GCScheduler& scheduler) noexcept;
#endif
    ~ConcurrentMarkAndSweep();

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;
    void SetMarkingBehaviorForTests(MarkingBehavior markingBehavior) noexcept;
    void SetMarkingRequested(uint64_t epoch) noexcept;
    void WaitForThreadsReadyToMark() noexcept;
    void CollectRootSetAndStartMarking(GCHandle gcHandle) noexcept;

#ifdef CUSTOM_ALLOCATOR
    alloc::Heap& heap() noexcept { return heap_; }
#endif

    int64_t Schedule() noexcept { return state_.schedule(); }
    void WaitFinalized(int64_t epoch) noexcept { state_.waitEpochFinalized(epoch); }

private:
    void PerformFullGC(int64_t epoch) noexcept;

#ifndef CUSTOM_ALLOCATOR
    mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory_;
#else
    alloc::Heap heap_;
#endif
    gcScheduler::GCScheduler& gcScheduler_;

    GCStateHolder state_;
    ScopedThread gcThread_;
    FinalizerProcessor<FinalizerQueue, FinalizerQueueTraits> finalizerProcessor_;

    MarkQueue markQueue_;
    MarkingBehavior markingBehavior_;
};

namespace internal {
struct MarkTraits {
    using MarkQueue = gc::ConcurrentMarkAndSweep::MarkQueue;

    static void clear(MarkQueue& queue) noexcept { queue.clear(); }

    static ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
        if (auto* top = queue.try_pop_front()) {
            auto node = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(*top);
            return node->GetObjHeader();
        }
        return nullptr;
    }

    static bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(object).ObjectData();
        return queue.try_push_front(objectData);
    }

    static bool tryMark(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(object).ObjectData();
        return objectData.tryMark();
    }

    static void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
        auto process = object->type_info()->processObjectInMark;
        RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);
        process(static_cast<void*>(&markQueue), object);
    }
};
} // namespace internal

} // namespace gc
} // namespace kotlin
