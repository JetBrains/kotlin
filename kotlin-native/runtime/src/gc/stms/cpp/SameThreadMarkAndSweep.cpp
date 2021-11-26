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
    using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

    static bool IsMarkedByExtraObject(mm::ExtraObjectData &object) noexcept {
        auto *baseObject = object.GetBaseObject();
        if (!baseObject->heap()) return true;
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(baseObject).GCObjectData();
        return objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack;
    }

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

// Global, because it's accessed on a hot path: avoid memory load from `this`.
std::atomic<gc::SameThreadMarkAndSweep::SafepointFlag> gSafepointFlag = gc::SameThreadMarkAndSweep::SafepointFlag::kNone;

} // namespace

ALWAYS_INLINE void gc::SameThreadMarkAndSweep::ThreadData::SafePointFunctionPrologue() noexcept {
    SafePointRegular(GCSchedulerThreadData::kFunctionPrologueWeight);
}

ALWAYS_INLINE void gc::SameThreadMarkAndSweep::ThreadData::SafePointLoopBody() noexcept {
    SafePointRegular(GCSchedulerThreadData::kLoopBodyWeight);
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    threadData_.gcScheduler().OnSafePointAllocation(size);
    SafepointFlag flag = gSafepointFlag.load();
    if (flag != SafepointFlag::kNone) {
        SafePointSlowPath(flag);
    }
}

void gc::SameThreadMarkAndSweep::ThreadData::PerformFullGC() noexcept {
    auto didGC = gc_.PerformFullGC();

    if (!didGC) {
        // If we failed to suspend threads, someone else might be asking to suspend them.
        threadData_.suspensionData().suspendIfRequested();
    }
}

void gc::SameThreadMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
    PerformFullGC();
}

ALWAYS_INLINE void gc::SameThreadMarkAndSweep::ThreadData::SafePointRegular(size_t weight) noexcept {
    threadData_.gcScheduler().OnSafePointRegular(weight);
    SafepointFlag flag = gSafepointFlag.load();
    if (flag != SafepointFlag::kNone) {
        SafePointSlowPath(flag);
    }
}

NO_INLINE void gc::SameThreadMarkAndSweep::ThreadData::SafePointSlowPath(SafepointFlag flag) noexcept {
    RuntimeAssert(flag != SafepointFlag::kNone, "Must've been handled by the caller");
    // No need to check for kNeedsSuspend, because `suspendIfRequested` checks for its own atomic.
    threadData_.suspensionData().suspendIfRequested();
    if (flag == SafepointFlag::kNeedsGC) {
        RuntimeLogDebug({kTagGC}, "Attempt to GC at SafePoint");
        PerformFullGC();
    }
}

gc::SameThreadMarkAndSweep::SameThreadMarkAndSweep() noexcept {
    mm::GlobalData::Instance().gcScheduler().SetScheduleGC([]() {
        RuntimeLogDebug({kTagGC}, "Scheduling GC by thread %d", konan::currentThreadId());
        gSafepointFlag = SafepointFlag::kNeedsGC;
    });
}

bool gc::SameThreadMarkAndSweep::PerformFullGC() noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to suspend threads by thread %d", konan::currentThreadId());
    auto timeStartUs = konan::getTimeMicros();
    bool didSuspend = mm::RequestThreadsSuspension();
    if (!didSuspend) {
        RuntimeLogDebug({kTagGC}, "Failed to suspend threads by thread %d", konan::currentThreadId());
        // Somebody else suspended the threads, and so ran a GC.
        // TODO: This breaks if suspension is used by something apart from GC.
        return false;
    }
    RuntimeLogDebug({kTagGC}, "Requested thread suspension by thread %d", konan::currentThreadId());
    gSafepointFlag = SafepointFlag::kNeedsSuspend;

    mm::ObjectFactory<gc::SameThreadMarkAndSweep>::FinalizerQueue finalizerQueue;
    {
        // Switch state to native to simulate this thread being a GC thread.
        ThreadStateGuard guard(ThreadState::kNative);

        mm::WaitForThreadsSuspension();
        auto timeSuspendUs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC}, "Suspended all threads in %" PRIu64 " microseconds", timeSuspendUs - timeStartUs);

        auto& scheduler = mm::GlobalData::Instance().gcScheduler();
        scheduler.gcData().OnPerformFullGC();

        RuntimeLogInfo(
                {kTagGC}, "Started GC epoch %zu. Time since last GC %" PRIu64 " microseconds", epoch_, timeStartUs - lastGCTimestampUs_);
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

        RuntimeLogInfo(
                {kTagGC}, "Collected root set of size %zu of which %zu are stable refs in %" PRIu64 " microseconds", graySet.size(),
                stableRef, timeRootSetUs - timeSuspendUs);
        gc::Mark<MarkTraits>(std::move(graySet));
        auto timeMarkUs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC}, "Marked in %" PRIu64 " microseconds", timeMarkUs - timeRootSetUs);
        gc::SweepExtraObjects<SweepTraits>(mm::GlobalData::Instance().extraObjectDataFactory());
        auto timeSweepExtraObjectsUs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC}, "Sweeped extra objects in %" PRIu64 " microseconds", timeSweepExtraObjectsUs - timeMarkUs);
        finalizerQueue = gc::Sweep<SweepTraits>(mm::GlobalData::Instance().objectFactory());
        auto timeSweepUs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC}, "Sweeped in %" PRIu64 " microseconds", timeSweepUs - timeSweepExtraObjectsUs);

        // Can be unsafe, because we've stopped the world.
        auto objectsCountAfter = mm::GlobalData::Instance().objectFactory().GetSizeUnsafe();
        auto extraObjectsCountAfter = mm::GlobalData::Instance().extraObjectDataFactory().GetSizeUnsafe();

        gSafepointFlag = SafepointFlag::kNone;
        mm::ResumeThreads();
        auto timeResumeUs = konan::getTimeMicros();

        RuntimeLogDebug({kTagGC}, "Resumed threads in %" PRIu64 " microseconds.", timeResumeUs - timeSweepUs);

        auto finalizersCount = finalizerQueue.size();
        auto collectedCount = objectsCountBefore - objectsCountAfter - finalizersCount;

        RuntimeLogInfo(
                {kTagGC},
                "Finished GC epoch %zu. Collected %zu objects, to be finalized %zu objects, %zu objects and %zd extra data objects remain. Total pause time %" PRIu64
                " microseconds",
                epoch_, collectedCount, finalizersCount, objectsCountAfter, extraObjectsCountAfter, timeResumeUs - timeStartUs);
        ++epoch_;
        lastGCTimestampUs_ = timeResumeUs;
    }

    // Finalizers are run after threads are resumed, because finalizers may request GC themselves, which would
    // try to suspend threads again. Also, we run finalizers in the runnable state, because they may be executing
    // kotlin code.

    // TODO: These will actually need to be run on a separate thread.
    AssertThreadState(ThreadState::kRunnable);
    RuntimeLogDebug({kTagGC}, "Starting to run finalizers");
    auto timeBeforeUs = konan::getTimeMicros();
    finalizerQueue.Finalize();
    auto timeAfterUs = konan::getTimeMicros();
    RuntimeLogInfo({kTagGC}, "Finished running finalizers in %" PRIu64 " microseconds", timeAfterUs - timeBeforeUs);

    return true;
}
