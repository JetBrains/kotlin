/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstddef>

#include "Allocator.hpp"
#include "Barriers.hpp"
#include "ExtraObjectDataFactory.hpp"
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
#include "MarkStack.hpp"
#include "ParallelMark.hpp"

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

    class ThreadData : private Pinned {
    public:
        using ObjectData = mark::ObjectData;
        using Allocator = AllocatorWithGC<Allocator, ThreadData>;

        explicit ThreadData(ConcurrentMarkAndSweep& gc, mm::ThreadData& threadData) noexcept
            : gc_(gc), threadData_(threadData) {}

        ~ThreadData() = default;

        void OnOOM(size_t size) noexcept;

        void OnSuspendForGC() noexcept;

        void safePoint() noexcept { barriers_.onCheckpoint(); }

        Allocator CreateAllocator() noexcept { return Allocator(gc::Allocator(), *this); }

        BarriersThreadData& barriers() noexcept { return barriers_; }

        bool tryLockRootSet();
        void beginCooperation();
        bool cooperative() const;
        void publish();
        bool published() const;
        void clearMarkFlags();

        mm::ThreadData& commonThreadData() const;

    private:
        friend ConcurrentMarkAndSweep;
        ConcurrentMarkAndSweep& gc_;
        mm::ThreadData& threadData_;
        BarriersThreadData barriers_;

        std::atomic<bool> rootSetLocked_ = false;
        std::atomic<bool> published_ = false;
        std::atomic<bool> cooperative_ = false;
    };

    using ObjectData = ThreadData::ObjectData;
    using Allocator = ThreadData::Allocator;

#ifndef CUSTOM_ALLOCATOR
    using FinalizerQueue = mm::ObjectFactory<ConcurrentMarkAndSweep>::FinalizerQueue;
    using FinalizerQueueTraits = mm::ObjectFactory<ConcurrentMarkAndSweep>::FinalizerQueueTraits;
#else
    using FinalizerQueue = alloc::FinalizerQueue;
    using FinalizerQueueTraits = alloc::FinalizerQueueTraits;
#endif

#ifdef CUSTOM_ALLOCATOR
    explicit ConcurrentMarkAndSweep(gcScheduler::GCScheduler& scheduler,
                                    bool mutatorsCooperate, std::size_t auxGCThreads) noexcept;
#else
    ConcurrentMarkAndSweep(
            mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory,
            mm::ExtraObjectDataFactory& extraObjectDataFactory,
            gcScheduler::GCScheduler& scheduler,
            bool mutatorsCooperate, std::size_t auxGCThreads) noexcept;
#endif
    ~ConcurrentMarkAndSweep();

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;

    void reconfigure(std::size_t maxParallelism, bool mutatorsCooperate, size_t auxGCThreads) noexcept;

#ifdef CUSTOM_ALLOCATOR
    alloc::Heap& heap() noexcept { return heap_; }
#endif

    GCStateHolder& state() noexcept { return state_; }

private:
    void mainGCThreadBody();
    void auxiliaryGCThreadBody();
    void PerformFullGC(int64_t epoch) noexcept;

#ifndef CUSTOM_ALLOCATOR
    mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory_;
    mm::ExtraObjectDataFactory& extraObjectDataFactory_;
#else
    alloc::Heap heap_;
#endif
    gcScheduler::GCScheduler& gcScheduler_;

    GCStateHolder state_;
    FinalizerProcessor<FinalizerQueue, FinalizerQueueTraits> finalizerProcessor_;

    mark::ParallelMark markDispatcher_;
    ScopedThread mainThread_;
    std_support::vector<ScopedThread> auxThreads_;
};

} // namespace gc
} // namespace kotlin
