/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SameThreadMarkAndSweep.hpp"

#include <cinttypes>

#include "CompilerConstants.hpp"
#include "GlobalData.hpp"
#include "GCStatistics.hpp"
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

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SameThreadMarkAndSweep>;
    using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

    static bool IsMarkedByExtraObject(mm::ExtraObjectData &object) noexcept {
        auto *baseObject = object.GetBaseObject();
        if (!baseObject->heap()) return true;
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(baseObject).ObjectData();
        return objectData.marked();
    }

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.ObjectData();
        return objectData.tryResetMark();
    }
};

struct FinalizeTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SameThreadMarkAndSweep>;
};

struct ProcessWeaksTraits {
    static bool IsMarked(ObjHeader* obj) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(obj).ObjectData();
        return objectData.marked();
    }
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
    bool didSuspend = mm::RequestThreadsSuspension();
    if (!didSuspend) {
        RuntimeLogDebug({kTagGC}, "Failed to suspend threads by thread %d", konan::currentThreadId());
        // Somebody else suspended the threads, and so ran a GC.
        // TODO: This breaks if suspension is used by something apart from GC.
        return false;
    }
    gSafepointFlag = SafepointFlag::kNeedsSuspend;
    auto gcHandle = GCHandle::create(epoch_++);
    gcHandle.suspensionRequested();

    mm::ObjectFactory<gc::SameThreadMarkAndSweep>::FinalizerQueue finalizerQueue;
    {
        // Switch state to native to simulate this thread being a GC thread.
        ThreadStateGuard guard(ThreadState::kNative);

        mm::WaitForThreadsSuspension();
        gcHandle.threadsAreSuspended();

        auto& scheduler = gcScheduler_;
        scheduler.gcData().OnPerformFullGC();

        gc::collectRootSet<internal::MarkTraits>(gcHandle, markQueue_, [] (mm::ThreadData&) { return true; });
        auto& extraObjectsDataFactory = mm::GlobalData::Instance().extraObjectDataFactory();

        gc::Mark<internal::MarkTraits>(gcHandle, markQueue_);
        auto markStats = gcHandle.getMarked();
        scheduler.gcData().UpdateAliveSetBytes(markStats.totalObjectsSize);

        gc::processWeaks<ProcessWeaksTraits>(gcHandle, mm::SpecialRefRegistry::instance());

        gc::SweepExtraObjects<SweepTraits>(gcHandle, extraObjectsDataFactory);
        finalizerQueue = gc::Sweep<SweepTraits>(gcHandle, objectFactory_);

        kotlin::compactObjectPoolInMainThread();

        gSafepointFlag = SafepointFlag::kNone;
        mm::ResumeThreads();
        gcHandle.threadsAreResumed();
        gcHandle.finalizersScheduled(finalizerQueue.size());
        gcHandle.finished();
    }

    // Finalizers are run after threads are resumed, because finalizers may request GC themselves, which would
    // try to suspend threads again. Also, we run finalizers in the runnable state, because they may be executing
    // kotlin code.

    // TODO: These will actually need to be run on a separate thread.
    AssertThreadState(ThreadState::kRunnable);
    finalizerQueue.Finalize();
    gcHandle.finalizersDone();

    return true;
}

gc::SameThreadMarkAndSweep::SafepointFlag gc::internal::loadSafepointFlag() noexcept {
    return gSafepointFlag.load();
}
