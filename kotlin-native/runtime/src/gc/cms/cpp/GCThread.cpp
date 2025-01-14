/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCThread.hpp"

#include "Allocator.hpp"
#include "AllocatorImpl.hpp"
#include "ConcurrentMark.hpp"
#include "GCScheduler.hpp"
#include "GCStatistics.hpp"
#include "Logging.hpp"
#include "MarkAndSweepUtils.hpp"
#include "RootSet.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadSuspension.hpp"

using namespace kotlin;

gc::internal::GCThread::GCThread(
        GCStateHolder& state,
        mark::ConcurrentMark& markDispatcher,
        alloc::Allocator& allocator,
        gcScheduler::GCScheduler& gcScheduler) noexcept :
    state_(state),
    markDispatcher_(markDispatcher),
    allocator_(allocator),
    gcScheduler_(gcScheduler),
    thread_(std::string_view("Main GC thread"), [this] { body(); }) {}

void gc::internal::GCThread::body() noexcept {
    RuntimeLogWarning({kTagGC}, "Initializing Concurrent Mark and Sweep GC.");
    while (true) {
        if (auto epoch = state_.waitScheduled()) {
            PerformFullGC(*epoch);
        } else {
            break;
        }
    }
}

void gc::internal::GCThread::PerformFullGC(int64_t epoch) noexcept {
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();

    auto gcHandle = GCHandle::create(epoch);

    markDispatcher_.beginMarkingEpoch(gcHandle);
    GCLogDebug(epoch, "Main GC requested marking in mutators");

    stopTheWorld(gcHandle, "GC stop the world #1: collect root set");

    auto& scheduler = gcScheduler_;
    scheduler.onGCStart();

    state_.start(epoch);

    markDispatcher_.runMainInSTW();

    // TODO outline as mark_.isolateMarkedHeapAndFinishMark()
    // By this point all the alive heap must be marked.
    // All the mutations (incl. allocations) after this method will be subject for the next GC.

    // This should really be done by each individual thread while waiting
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        thread.allocator().prepareForGC();
    }
    allocator_.prepareForGC();

    // Taking the locks before the pause is completed. So that any destroying thread
    // would not publish into the global state at an unexpected time.
    auto sweepState = allocator_.impl().prepareForSweep();

    resumeTheWorld(gcHandle);

    auto finalizerQueue = allocator_.impl().sweep(gcHandle, std::move(sweepState));

    scheduler.onGCFinish(epoch, gcHandle.getKeptSizeBytes());
    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();

    // This may start a new thread. On some pthreads implementations, this may block waiting for concurrent thread
    // destructors running. So, it must ensured that no locks are held by this point.
    // TODO: Consider having an always on sleeping finalizer thread.
    allocator_.impl().scheduleFinalization(std::move(finalizerQueue), epoch);
}
