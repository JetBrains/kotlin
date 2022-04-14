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
    using MarkQueue = gc::SameThreadMarkAndSweep::MarkQueue;

    static bool isEmpty(const MarkQueue& queue) noexcept {
        return queue.empty();
    }

    static void clear(MarkQueue& queue) noexcept {
        queue.clear();
    }

    static ObjHeader* dequeue(MarkQueue& queue) noexcept {
        auto& top = queue.front();
        queue.pop_front();
        auto node = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(top);
        return node->GetObjHeader();
    }

    static void enqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(object).ObjectData();
        if (objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack) return;
        objectData.setColor(gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack);
        queue.push_front(objectData);
    }
};

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SameThreadMarkAndSweep>;
    using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

    static bool IsMarkedByExtraObject(mm::ExtraObjectData &object) noexcept {
        auto *baseObject = object.GetBaseObject();
        if (!baseObject->heap()) return true;
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(baseObject).ObjectData();
        return objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack;
    }

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.ObjectData();
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

void gc::SameThreadMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    gcScheduler_.OnSafePointAllocation(size);
    SafepointFlag flag = gSafepointFlag.load();
    if (flag != SafepointFlag::kNone) {
        SafePointSlowPath(flag);
    }
}

void gc::SameThreadMarkAndSweep::ThreadData::ScheduleAndWaitFullGC() noexcept {
    auto didGC = gc_.PerformFullGC();

    if (!didGC) {
        // If we failed to suspend threads, someone else might be asking to suspend them.
        mm::SuspendIfRequested();
    }
}

void gc::SameThreadMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
    ScheduleAndWaitFullGC();
}

NO_INLINE void gc::SameThreadMarkAndSweep::ThreadData::SafePointSlowPath(SafepointFlag flag) noexcept {
    switch (flag) {
        case SafepointFlag::kNone:
            RuntimeAssert(false, "Must've been handled by the caller");
            return;
        case SafepointFlag::kNeedsSuspend:
            mm::SuspendIfRequested();
            return;
        case SafepointFlag::kNeedsGC:
            RuntimeLogDebug({kTagGC}, "Attempt to GC at SafePoint");
            ScheduleAndWaitFullGC();
            return;
    }
}

gc::SameThreadMarkAndSweep::SameThreadMarkAndSweep(
        mm::ObjectFactory<SameThreadMarkAndSweep>& objectFactory, GCScheduler& gcScheduler) noexcept :
    objectFactory_(objectFactory), gcScheduler_(gcScheduler) {
    gcScheduler_.SetScheduleGC([]() {
        // TODO: CMS is also responsible for avoiding scheduling while GC hasn't started running.
        //       Investigate, if it's possible to move this logic into the scheduler.
        SafepointFlag expectedFlag = SafepointFlag::kNone;
        if (gSafepointFlag.compare_exchange_strong(expectedFlag, SafepointFlag::kNeedsGC)) {
            RuntimeLogDebug({kTagGC}, "Scheduling GC by thread %d", konan::currentThreadId());
        }
    });
    RuntimeLogDebug({kTagGC}, "Same thread Mark & Sweep GC initialized");
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

        auto& scheduler = gcScheduler_;
        scheduler.gcData().OnPerformFullGC();

        RuntimeLogInfo(
                {kTagGC}, "Started GC epoch %zu. Time since last GC %" PRIu64 " microseconds", epoch_, timeStartUs - lastGCTimestampUs_);
        gc::collectRootSet<MarkTraits>(markQueue_);
        auto timeRootSetUs = konan::getTimeMicros();
        // Can be unsafe, because we've stopped the world.
        auto objectsCountBefore = objectFactory_.GetSizeUnsafe();

        RuntimeLogInfo(
                {kTagGC}, "Collected root set of size %zu in %" PRIu64 " microseconds", markQueue_.size(),
                timeRootSetUs - timeSuspendUs);
        auto markStats = gc::Mark<MarkTraits>(markQueue_);
        auto timeMarkUs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC}, "Marked %zu objects in %" PRIu64 " microseconds", markStats.aliveHeapSet, timeMarkUs - timeRootSetUs);
        scheduler.gcData().UpdateAliveSetBytes(markStats.aliveHeapSetBytes);
        gc::SweepExtraObjects<SweepTraits>(mm::GlobalData::Instance().extraObjectDataFactory());
        auto timeSweepExtraObjectsUs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC}, "Sweeped extra objects in %" PRIu64 " microseconds", timeSweepExtraObjectsUs - timeMarkUs);
        finalizerQueue = gc::Sweep<SweepTraits>(objectFactory_);
        auto timeSweepUs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC}, "Sweeped in %" PRIu64 " microseconds", timeSweepUs - timeSweepExtraObjectsUs);

        // Can be unsafe, because we've stopped the world.
        auto objectsCountAfter = objectFactory_.GetSizeUnsafe();
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

gc::SameThreadMarkAndSweep::SafepointFlag gc::internal::loadSafepointFlag() noexcept {
    return gSafepointFlag.load();
}
