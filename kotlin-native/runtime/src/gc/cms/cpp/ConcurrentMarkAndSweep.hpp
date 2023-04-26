/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstddef>

#include "Allocator.hpp"
#include "GCScheduler.hpp"
#include "IntrusiveList.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ObjectFactory.hpp"
#include "ScopedThread.hpp"
#include "ThreadData.hpp"
#include "Types.h"
#include "Utils.hpp"
#include "GCState.hpp"
#include "std_support/Memory.hpp"
#include "GCStatistics.hpp"
#include "MarkStack.hpp"
#include "ParallelMark.hpp"
#include "Barriers.hpp"

#ifdef CUSTOM_ALLOCATOR
#include "CustomAllocator.hpp"
#include "Heap.hpp"

namespace kotlin::alloc {
class CustomFinalizerProcessor;
}
#endif

namespace kotlin {
namespace gc {

#ifndef CUSTOM_ALLOCATOR
class FinalizerProcessor;
#endif

class ConcurrentMarkAndSweep;

// Stop-the-world parallel mark + concurrent sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
// TODO: Also make marking run concurrently with Kotlin threads.
class ConcurrentMarkAndSweep : private Pinned {
public:

    class ThreadData : private Pinned {
    public:
        using ObjectData = mark::ObjectData;
        using Allocator = AllocatorWithGC<Allocator, ThreadData>;

        explicit ThreadData(ConcurrentMarkAndSweep& gc, mm::ThreadData& threadData, GCSchedulerThreadData& gcScheduler) noexcept :
            gc_(gc), threadData_(threadData), gcScheduler_(gcScheduler) {}
        ~ThreadData() = default;

        void SafePointAllocation(size_t size) noexcept;

        void Schedule() noexcept;
        void ScheduleAndWaitFullGC() noexcept;
        void ScheduleAndWaitFullGCWithFinalizers() noexcept;

        void OnOOM(size_t size) noexcept;

        void OnSuspendForGC() noexcept;

        Allocator CreateAllocator() noexcept { return Allocator(gc::Allocator(), *this); }

        bool tryLockRootSet();
        bool rootSetLocked() const;
        void beginCooperation();
        bool cooperative() const;
        void publish(); // TODO make publish
        bool published() const;
        void clearMarkFlags();

        mm::ThreadData& commonThreadData() const;
        BarriersThreadData& barriers() { return barriersThreadData_; };

    private:
        friend ConcurrentMarkAndSweep;
        ConcurrentMarkAndSweep& gc_;
        mm::ThreadData& threadData_;
        GCSchedulerThreadData& gcScheduler_;

        std::atomic<bool> rootSetLocked_ = false;
        std::atomic<bool> published_ = false;
        std::atomic<bool> cooperative_ = false;

        BarriersThreadData barriersThreadData_;
    };

    using ObjectData = ThreadData::ObjectData;
    using Allocator = ThreadData::Allocator;

#ifdef CUSTOM_ALLOCATOR
    explicit ConcurrentMarkAndSweep(GCScheduler& scheduler,
                           bool mutatorsCooperate, std::size_t auxGCThreads) noexcept;
#else
    ConcurrentMarkAndSweep(mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory, GCScheduler& scheduler,
    bool mutatorsCooperate, std::size_t auxGCThreads) noexcept;
#endif
    ~ConcurrentMarkAndSweep();

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;

    void reconfigure(bool mutatorsCooperate, size_t auxGCThreads);

#ifdef CUSTOM_ALLOCATOR
    alloc::Heap& heap() noexcept { return heap_; }
#endif

private:
    void mainGCThreadBody();
    void auxiliaryGCThreadBody();
    // Returns `true` if GC has happened, and `false` if not (because someone else has suspended the threads).
    bool PerformFullGC(int64_t epoch, mark::MarkDispatcher::MarkJob& markContext) noexcept;

#ifndef CUSTOM_ALLOCATOR
    mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory_;
#else
    alloc::Heap heap_;
#endif
    GCScheduler& gcScheduler_;

    GCStateHolder state_;
#ifndef CUSTOM_ALLOCATOR
    std_support::unique_ptr<FinalizerProcessor> finalizerProcessor_;
#else
    std_support::unique_ptr<alloc::CustomFinalizerProcessor> finalizerProcessor_;
#endif

    mark::MarkDispatcher markDispatcher_;
    ScopedThread mainThread_;
    std_support::vector<ScopedThread> auxThreads_;
};

} // namespace gc
} // namespace kotlin
