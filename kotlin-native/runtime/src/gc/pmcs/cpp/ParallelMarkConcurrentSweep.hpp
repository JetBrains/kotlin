/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstddef>
#include <memory>
#include <vector>

#include "AllocatorImpl.hpp"
#include "Barriers.hpp"
#include "FinalizerProcessor.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "GCStatistics.hpp"
#include "IntrusiveList.hpp"
#include "MainThreadFinalizerProcessor.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ObjectData.hpp"
#include "ParallelMark.hpp"
#include "ScopedThread.hpp"
#include "ThreadData.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace gc {

// Stop-the-world parallel mark + concurrent sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
class ParallelMarkConcurrentSweep : private Pinned {
public:
    class ThreadData : private Pinned {
    public:
        explicit ThreadData(ParallelMarkConcurrentSweep& gc, mm::ThreadData& threadData) noexcept : gc_(gc), threadData_(threadData) {}
        ~ThreadData() = default;

        void OnSuspendForGC() noexcept;

        void safePoint() noexcept { barriers_.onSafePoint(); }

        void onThreadRegistration() noexcept { barriers_.onThreadRegistration(); }

        BarriersThreadData& barriers() noexcept { return barriers_; }

        bool tryLockRootSet();
        void publish();
        bool published() const;
        void clearMarkFlags();

        mm::ThreadData& commonThreadData() const;

    private:
        friend ParallelMarkConcurrentSweep;
        ParallelMarkConcurrentSweep& gc_;
        mm::ThreadData& threadData_;
        BarriersThreadData barriers_;

        std::atomic<bool> rootSetLocked_ = false;
        std::atomic<bool> published_ = false;
    };

    ParallelMarkConcurrentSweep(
            alloc::Allocator& allocator, gcScheduler::GCScheduler& scheduler, bool mutatorsCooperate, std::size_t auxGCThreads) noexcept;
    ~ParallelMarkConcurrentSweep();

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;

    void reconfigure(std::size_t maxParallelism, bool mutatorsCooperate, size_t auxGCThreads) noexcept;

    GCStateHolder& state() noexcept { return state_; }
    alloc::MainThreadFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits>& mainThreadFinalizerProcessor() noexcept {
        return mainThreadFinalizerProcessor_;
    }

private:
    void mainGCThreadBody();
    void auxiliaryGCThreadBody();
    void PerformFullGC(int64_t epoch) noexcept;

    alloc::Allocator& allocator_;
    gcScheduler::GCScheduler& gcScheduler_;

    GCStateHolder state_;
    FinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits> finalizerProcessor_;
    alloc::MainThreadFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits> mainThreadFinalizerProcessor_;

    mark::ParallelMark markDispatcher_;
    ScopedThread mainThread_;
    std::vector<ScopedThread> auxThreads_;
};

} // namespace gc
} // namespace kotlin
