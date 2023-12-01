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
#include "ConcurrentMark.hpp"
#include "ScopedThread.hpp"
#include "ThreadData.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace gc {

// TODO concurrent mark + concurrent sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
// TODO: Make marking run concurrently with Kotlin threads.
class ConcurrentMarkAndSweep : private Pinned {
public:
    class ThreadData : private Pinned {
    public:
        explicit ThreadData(ConcurrentMarkAndSweep& gc, mm::ThreadData& threadData) noexcept : gc_(gc), threadData_(threadData) {}
        ~ThreadData() = default;

        void OnSuspendForGC() noexcept;

        void safePoint() noexcept { barriers_.onSafePoint(); }

        void onThreadRegistration() noexcept { barriers_.onThreadRegistration(); }

        bool tryLockRootSet();
        void publish();
        bool published() const;
        void clearMarkFlags();

        auto& commonThreadData() const noexcept { return threadData_; }
        auto& barriers() noexcept { return barriers_; }
        auto& mark() noexcept { return mark_; }

    private:
        friend ConcurrentMarkAndSweep;
        ConcurrentMarkAndSweep& gc_;
        mm::ThreadData& threadData_;
        barriers::BarriersThreadData barriers_;
        mark::ConcurrentMark::ThreadData mark_;

        std::atomic<bool> rootSetLocked_ = false;
        std::atomic<bool> published_ = false;
    };

    ConcurrentMarkAndSweep(
            alloc::Allocator& allocator, gcScheduler::GCScheduler& scheduler, bool mutatorsCooperate, std::size_t auxGCThreads) noexcept;
    ~ConcurrentMarkAndSweep();

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;

    GCStateHolder& state() noexcept { return state_; }
    alloc::MainThreadFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits>& mainThreadFinalizerProcessor() noexcept {
        return mainThreadFinalizerProcessor_;
    }

private:
    void mainGCThreadBody();
    void PerformFullGC(int64_t epoch) noexcept;

    alloc::Allocator& allocator_;
    gcScheduler::GCScheduler& gcScheduler_;

    GCStateHolder state_;
    FinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits> finalizerProcessor_;
    alloc::MainThreadFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits> mainThreadFinalizerProcessor_;

    mark::ConcurrentMark markDispatcher_;
    ScopedThread mainThread_;
    std::vector<ScopedThread> auxThreads_;
};

} // namespace gc
} // namespace kotlin
