/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SameThreadMarkAndSweep.hpp"

#include <cinttypes>

#include "CompilerConstants.hpp"
#include "GlobalData.hpp"
#include "Logging.hpp"
#include "MarkAndSweepUtils.hpp"
#include "Memory.h"
#include "RootSet.hpp"
#include "Runtime.h"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadSuspension.hpp"

using namespace kotlin;

namespace {

struct MarkTraits {
    static bool IsMarked(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(object).GCObjectData();
        return objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack;
    }

    static bool TryMark(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(object).GCObjectData();
        if (objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack) return false;
        objectData.setColor(gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack);
        return true;
    };
};

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SameThreadMarkAndSweep>;

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.GCObjectData();
        if (objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kWhite) return false;
        objectData.setColor(gc::SameThreadMarkAndSweep::ObjectData::Color::kWhite);
        return true;
    }
};

struct FinalizeTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SameThreadMarkAndSweep>;
};

} // namespace

void gc::SameThreadMarkAndSweep::ThreadData::SafePointFunctionEpilogue() noexcept {
    SafePointRegular(GCScheduler::ThreadData::kFunctionEpilogueWeight);
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointLoopBody() noexcept {
    SafePointRegular(GCScheduler::ThreadData::kLoopBodyWeight);
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointExceptionUnwind() noexcept {
    SafePointRegular(GCScheduler::ThreadData::kExceptionUnwindWeight);
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    threadData_.suspensionData().suspendIfRequested();
    auto& scheduler = threadData_.gcScheduler();
    if (scheduler.OnSafePointAllocation(size)) {
        RuntimeLogDebug({kTagGC}, "Attempt to GC at SafePointAllocation size=%zu", size);
        PerformFullGC();
    }
}

void gc::SameThreadMarkAndSweep::ThreadData::PerformFullGC() noexcept {
    mm::ObjectFactory<gc::SameThreadMarkAndSweep>::FinalizerQueue finalizerQueue;
    {
        // Switch state to native to simulate this thread being a GC thread.
        // As a bonus, if we failed to suspend threads (which means some other thread asked for a GC),
        // we will automatically suspend at the scope exit.
        // TODO: Cannot use `threadData_` here, because there's no way to transform `mm::ThreadData` into `MemoryState*`.
        ThreadStateGuard guard(ThreadState::kNative);
        finalizerQueue = gc_.PerformFullGC();
    }

    // Finalizers are run after threads are resumed, because finalizers may request GC themselves, which would
    // try to suspend threads again. Also, we run finalizers in the runnable state, because they may be executing
    // kotlin code.

    // TODO: These will actually need to be run on a separate thread.
    // TODO: Cannot use `threadData_` here, because there's no way to transform `mm::ThreadData` into `MemoryState*`.
    AssertThreadState(ThreadState::kRunnable);
    RuntimeLogDebug({kTagGC}, "Starting to run finalizers");
    auto timeBeforeUs = konan::getTimeMicros();
    finalizerQueue.Finalize();
    auto timeAfterUs = konan::getTimeMicros();
    RuntimeLogInfo({kTagGC}, "Finished running finalizers in %" PRIu64 " microseconds", timeAfterUs - timeBeforeUs);
}

void gc::SameThreadMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
    PerformFullGC();
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointRegular(size_t weight) noexcept {
    threadData_.suspensionData().suspendIfRequested();
    auto& scheduler = threadData_.gcScheduler();
    if (scheduler.OnSafePointRegular(weight)) {
        RuntimeLogDebug({kTagGC}, "Attempt to GC at SafePointRegular weight=%zu", weight);
        PerformFullGC();
    }
}

mm::ObjectFactory<gc::SameThreadMarkAndSweep>::FinalizerQueue gc::SameThreadMarkAndSweep::PerformFullGC() noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to suspend threads by thread %d", konan::currentThreadId());
    auto timeStartUs = konan::getTimeMicros();
    bool didSuspend = mm::SuspendThreads();
    auto timeSuspendUs = konan::getTimeMicros();
    if (!didSuspend) {
        RuntimeLogDebug({kTagGC}, "Failed to suspend threads");
        // Somebody else suspended the threads, and so ran a GC.
        // TODO: This breaks if suspension is used by something apart from GC.
        return {};
    }
    RuntimeLogDebug({kTagGC}, "Suspended all threads in %" PRIu64 " microseconds", timeSuspendUs - timeStartUs);

    auto& scheduler = mm::GlobalData::Instance().gcScheduler();
    scheduler.gcData().OnPerformFullGC();

    RuntimeLogInfo({kTagGC}, "Started GC epoch %zu. Time since last GC %" PRIu64 " microseconds", epoch_, timeStartUs - lastGCTimestampUs_);
    KStdVector<ObjHeader*> graySet;
    for (auto& thread : mm::GlobalData::Instance().threadRegistry().LockForIter()) {
        // TODO: Maybe it's more efficient to do by the suspending thread?
        thread.Publish();
        thread.gcScheduler().OnStoppedForGC();
        size_t stack = 0;
        size_t tls = 0;
        for (auto value : mm::ThreadRootSet(thread)) {
            if (!isNullOrMarker(value.object)) {
                graySet.push_back(value.object);
                switch (value.source) {
                    case mm::ThreadRootSet::Source::kStack:
                        ++stack;
                        break;
                    case mm::ThreadRootSet::Source::kTLS:
                        ++tls;
                        break;
                }
            }
        }
        RuntimeLogDebug({kTagGC}, "Collected root set for thread stack=%zu tls=%zu", stack, tls);
    }
    mm::StableRefRegistry::Instance().ProcessDeletions();
    size_t global = 0;
    size_t stableRef = 0;
    for (auto value : mm::GlobalRootSet()) {
        if (!isNullOrMarker(value.object)) {
            graySet.push_back(value.object);
            switch (value.source) {
                case mm::GlobalRootSet::Source::kGlobal:
                    ++global;
                    break;
                case mm::GlobalRootSet::Source::kStableRef:
                    ++stableRef;
                    break;
            }
        }
    }
    auto timeRootSetUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Collected global root set global=%zu stableRef=%zu", global, stableRef);

    // Can be unsafe, because we've stopped the world.
    auto objectsCountBefore = mm::GlobalData::Instance().objectFactory().GetSizeUnsafe();

    RuntimeLogInfo({kTagGC}, "Collected root set of size %zu of which %zu are stable refs in %" PRIu64 " microseconds", graySet.size(), stableRef, timeRootSetUs - timeSuspendUs);
    gc::Mark<MarkTraits>(std::move(graySet));
    auto timeMarkUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Marked in %" PRIu64 " microseconds", timeMarkUs - timeRootSetUs);
    auto finalizerQueue = gc::Sweep<SweepTraits>(mm::GlobalData::Instance().objectFactory());
    auto timeSweepUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Sweeped in %" PRIu64 " microseconds", timeSweepUs - timeMarkUs);

    // Can be unsafe, because we've stopped the world.
    auto objectsCountAfter = mm::GlobalData::Instance().objectFactory().GetSizeUnsafe();

    mm::ResumeThreads();
    auto timeResumeUs = konan::getTimeMicros();

    RuntimeLogDebug({kTagGC}, "Resumed threads in %" PRIu64 " microseconds.", timeResumeUs - timeSweepUs);

    auto finalizersCount = finalizerQueue.size();
    auto collectedCount = objectsCountBefore - objectsCountAfter - finalizersCount;

    RuntimeLogInfo(
            {kTagGC},
            "Finished GC epoch %zu. Collected %zu objects, to be finalized %zu objects, %zu objects remain. Total pause time %" PRIu64
            " microseconds",
            epoch_, collectedCount, finalizersCount, objectsCountAfter, timeResumeUs - timeStartUs);
    ++epoch_;
    lastGCTimestampUs_ = timeResumeUs;

    return finalizerQueue;
}
