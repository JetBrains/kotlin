/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMarkAndSweep.hpp"

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
#include "GCState.hpp"
#include "FinalizerProcessor.hpp"

using namespace kotlin;

namespace {

struct MarkTraits {
    static bool IsMarked(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(object).GCObjectData();
        return objectData.color() == gc::ConcurrentMarkAndSweep::ObjectData::Color::kBlack;
    }

    static bool TryMark(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(object).GCObjectData();
        if (objectData.color() == gc::ConcurrentMarkAndSweep::ObjectData::Color::kBlack) return false;
        objectData.setColor(gc::ConcurrentMarkAndSweep::ObjectData::Color::kBlack);
        return true;
    };
};

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>;
    using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

    static bool IsMarkedByExtraObject(mm::ExtraObjectData &object) noexcept {
        auto *baseObject = object.GetBaseObject();
        if (!baseObject->heap()) return true;
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(baseObject).GCObjectData();
        return objectData.color() == gc::ConcurrentMarkAndSweep::ObjectData::Color::kBlack;
    }

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.GCObjectData();
        if (objectData.color() == gc::ConcurrentMarkAndSweep::ObjectData::Color::kWhite) return false;
        objectData.setColor(gc::ConcurrentMarkAndSweep::ObjectData::Color::kWhite);
        return true;
    }
};

} // namespace

ALWAYS_INLINE void gc::ConcurrentMarkAndSweep::ThreadData::SafePointFunctionPrologue() noexcept {
    SafePointRegular(GCSchedulerThreadData::kFunctionPrologueWeight);
}

ALWAYS_INLINE void gc::ConcurrentMarkAndSweep::ThreadData::SafePointLoopBody() noexcept {
    SafePointRegular(GCSchedulerThreadData::kLoopBodyWeight);
}

void gc::ConcurrentMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    threadData_.gcScheduler().OnSafePointAllocation(size);
    mm::SuspendIfRequested();
}
void gc::ConcurrentMarkAndSweep::ThreadData::ScheduleAndWaitFullGC() noexcept {
    ThreadStateGuard guard(ThreadState::kNative);
    auto scheduled_epoch = gc_.state_.schedule();
    gc_.state_.waitEpochFinished(scheduled_epoch);
}

void gc::ConcurrentMarkAndSweep::ThreadData::ScheduleAndWaitFullGCWithFinalizers() noexcept {
    ThreadStateGuard guard(ThreadState::kNative);
    auto scheduled_epoch = gc_.state_.schedule();
    gc_.state_.waitEpochFinalized(scheduled_epoch);
}

void gc::ConcurrentMarkAndSweep::ThreadData::StopFinalizerThreadForTests() noexcept {
    gc_.finalizerProcessor_->StopFinalizerThread();
}

void gc::ConcurrentMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
    ScheduleAndWaitFullGC();
}

ALWAYS_INLINE void gc::ConcurrentMarkAndSweep::ThreadData::SafePointRegular(size_t weight) noexcept {
    threadData_.gcScheduler().OnSafePointRegular(weight);
    mm::SuspendIfRequested();
}

gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep() noexcept :
    finalizerProcessor_(make_unique<FinalizerProcessor>([this](int64_t epoch) { state_.finalized(epoch);})) {
    mm::GlobalData::Instance().gcScheduler().SetScheduleGC([this]() NO_EXTERNAL_CALLS_CHECK NO_INLINE {
        RuntimeLogDebug({kTagGC}, "Scheduling GC by thread %d", konan::currentThreadId());
        state_.schedule();
    });
    gcThread_ = std::thread([this] {
        while (true) {
            auto epoch = state_.waitScheduled();
            if (epoch.has_value()) {
                PerformFullGC(*epoch);
            } else {
                break;
            }
        }
    });
}


gc::ConcurrentMarkAndSweep::~ConcurrentMarkAndSweep() {
    state_.shutdown();
    gcThread_.join();
}

bool gc::ConcurrentMarkAndSweep::PerformFullGC(int64_t epoch) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to suspend threads by thread %d", konan::currentThreadId());
    auto timeStartUs = konan::getTimeMicros();
    bool didSuspend = mm::RequestThreadsSuspension();
    RuntimeAssert(didSuspend, "Only GC thread can request suspension");
    RuntimeLogDebug({kTagGC}, "Requested thread suspension by thread %d", konan::currentThreadId());

    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "Concurrent GC must run on unregistered thread");

    mm::WaitForThreadsSuspension();
    auto timeSuspendUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Suspended all threads in %" PRIu64 " microseconds", timeSuspendUs - timeStartUs);

    auto& scheduler = mm::GlobalData::Instance().gcScheduler();
    scheduler.gcData().OnPerformFullGC();

    state_.start(epoch);
    RuntimeLogInfo(
            {kTagGC}, "Started GC epoch %" PRId64 ". Time since last GC %" PRIu64 " microseconds", epoch, timeStartUs - lastGCTimestampUs_);
    auto graySet = collectRootSet();
    auto timeRootSetUs = konan::getTimeMicros();
    // Can be unsafe, because we've stopped the world.

    auto objectsCountBefore = mm::GlobalData::Instance().objectFactory().GetSizeUnsafe();
    RuntimeLogInfo(
            {kTagGC}, "Collected root set of size %zu in %" PRIu64 " microseconds", graySet.size(),
            timeRootSetUs - timeSuspendUs);
    auto markStats = gc::Mark<MarkTraits>(std::move(graySet));
    auto timeMarkUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Marked %zu objects in %" PRIu64 " microseconds. Processed %zu duplicate entries in the gray set", markStats.aliveHeapSet, timeMarkUs - timeRootSetUs, markStats.duplicateEntries);
    scheduler.gcData().UpdateAliveSetBytes(markStats.aliveHeapSetBytes);
    gc::SweepExtraObjects<SweepTraits>(mm::GlobalData::Instance().extraObjectDataFactory());
    auto timeSweepExtraObjectsUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Sweeped extra objects in %" PRIu64 " microseconds", timeSweepExtraObjectsUs - timeMarkUs);

    auto objectFactoryIterable = mm::GlobalData::Instance().objectFactory().LockForIter();

    mm::ResumeThreads();
    auto timeResumeUs = konan::getTimeMicros();

    RuntimeLogDebug({kTagGC},
                    "Resumed threads in %" PRIu64 " microseconds. Total pause for most threads is %"  PRIu64" microseconds",
                    timeResumeUs - timeSweepExtraObjectsUs, timeResumeUs - timeStartUs);

    auto finalizerQueue = gc::Sweep<SweepTraits>(objectFactoryIterable);
    auto timeSweepUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Swept in %" PRIu64 " microseconds", timeSweepUs - timeResumeUs);

    // Can be unsafe, because we have a lock in objectFactoryIterable
    auto objectsCountAfter = mm::GlobalData::Instance().objectFactory().GetSizeUnsafe();
    auto extraObjectsCountAfter = mm::GlobalData::Instance().extraObjectDataFactory().GetSizeUnsafe();

    auto finalizersCount = finalizerQueue.size();
    auto collectedCount = objectsCountBefore - objectsCountAfter - finalizersCount;

    state_.finish(epoch);
    finalizerProcessor_->ScheduleTasks(std::move(finalizerQueue), epoch);

    RuntimeLogInfo(
            {kTagGC},
            "Finished GC epoch %" PRId64 ". Collected %zu objects, to be finalized %zu objects, %zu objects and %zd extra data objects remain. Total pause time %" PRIu64
            " microseconds",
            epoch, collectedCount, finalizersCount, objectsCountAfter, extraObjectsCountAfter, timeSweepUs - timeStartUs);
    lastGCTimestampUs_ = timeResumeUs;
    return true;
}

