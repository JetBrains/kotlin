/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Allocator.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "Utils.hpp"
#include "concurrent/UtilityThread.hpp"
#include "GlobalData.hpp"
#include "GCStatistics.hpp"
#include "MarkAndSweepUtils.hpp"

namespace kotlin::gc::internal {

template <typename GCTraits>
class MainGCThread : private MoveOnly {
public:
    MainGCThread(GCStateHolder& state, alloc::Allocator& allocator,
                 gcScheduler::GCScheduler& gcScheduler, typename GCTraits::Mark& mark) noexcept :
            state_(state),
            allocator_(allocator),
            gcScheduler_(gcScheduler),
            mark_(mark),
            thread_(std::string_view("Main GC thread"), [this] { body(); }) {}

private:
    void body() noexcept {
        RuntimeLogInfo({ kTagGC }, "Initializing %s GC.", GCTraits::kName);
        while (true) {
            if (auto epoch = state_.waitScheduled()) {
                PerformFullGC(*epoch);
            } else {
                break;
            }
        }
        mark_.requestShutdown();
    }

    void PerformFullGC(int64_t epoch) noexcept {
        auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();

        auto gcHandle = GCHandle::create(epoch);

        mark_.setupBeforeSTW(gcHandle);

        stopTheWorld(gcHandle, "GC stop the world: mark");

        gcScheduler_.onGCStart();

        state_.start(epoch);

        mark_.markInSTW();

        // TODO outline as mark_.isolateMarkedHeapAndFinishMark()
        // By this point all the alive heap must be marked.
        // All the mutations (incl. allocations) after this method will be subject for the next GC.
        // This should really be done by each individual thread while waiting
        for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
            thread.allocator().prepareForGC();
        }
        allocator_.prepareForGC();

        if (GCTraits::kConcurrentSweep) {
            resumeTheWorld(gcHandle);
        }

        allocator_.sweep(gcHandle);
        gcScheduler_.onGCFinish(epoch, gcHandle.getKeptSizeBytes());

        if (!GCTraits::kConcurrentSweep) {
            resumeTheWorld(gcHandle);
        }

        state_.finish(epoch);
        gcHandle.finished();

        // This may start a new thread. On some pthreads implementations, this may block waiting for concurrent thread
        // destructors running. So, it must ensured that no locks are held by this point.
        // TODO: Consider having an always on sleeping finalizer thread.
        allocator_.scheduleFinalization(gcHandle);
    }

    GCStateHolder& state_;
    alloc::Allocator& allocator_;
    gcScheduler::GCScheduler& gcScheduler_;
    typename GCTraits::Mark& mark_;
    UtilityThread thread_;
};

} // namespace kotlin::gc::internal
