/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMarkAndSweep.hpp"

#include <cinttypes>

#include "CompilerConstants.hpp"
#include "GlobalData.hpp"
#include "GCImpl.hpp"
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
    [[clang::no_destroy]] std::mutex markingMutex;
    [[clang::no_destroy]] std::condition_variable markingCondVar;
    [[clang::no_destroy]] std::atomic<bool> markingRequested = false;

struct MarkTraits {
    using MarkQueue = gc::ConcurrentMarkAndSweep::MarkQueue;

    static bool isEmpty(const MarkQueue& queue) noexcept {
        return queue.empty();
    }

    static void clear(MarkQueue& queue) noexcept {
        queue.clear();
    }

    static ObjHeader* dequeue(MarkQueue& queue) noexcept {
        auto& top = queue.front();
        queue.pop_front();
        auto node = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(top);
        return node->GetObjHeader();
    }

    static void enqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(object).ObjectData();
        if (!objectData.atomicSetToBlack()) return;
        queue.push_front(objectData);
    }
};

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>;
    using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

    static bool IsMarkedByExtraObject(mm::ExtraObjectData &object) noexcept {
        auto *baseObject = object.GetBaseObject();
        if (!baseObject->heap()) return true;
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(baseObject).ObjectData();
        return objectData.color() == gc::ConcurrentMarkAndSweep::ObjectData::Color::kBlack;
    }

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.ObjectData();
        if (objectData.color() == gc::ConcurrentMarkAndSweep::ObjectData::Color::kWhite) return false;
        objectData.setColor(gc::ConcurrentMarkAndSweep::ObjectData::Color::kWhite);
        return true;
    }
};

} // namespace

void gc::ConcurrentMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    gcScheduler_.OnSafePointAllocation(size);
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

void gc::ConcurrentMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
    ScheduleAndWaitFullGC();
}

NO_EXTERNAL_CALLS_CHECK void gc::ConcurrentMarkAndSweep::ThreadData::OnSuspendForGC() noexcept {
    std::unique_lock lock(markingMutex);
    if (!markingRequested.load())
        return;
    AutoReset scopedAssignMarking(&marking_, true);
    threadData_.Publish();
    markingCondVar.wait(lock, []() { return !markingRequested.load(); });
    // // Unlock while marking to allow mutliple threads to mark in parallel.
    lock.unlock();
    RuntimeLogDebug({kTagGC}, "Parallel marking in thread %d", konan::currentThreadId());
    MarkQueue markQueue;
    gc::collectRootSetForThread<MarkTraits>(markQueue, threadData_);
    MarkStats stats = gc::Mark<MarkTraits>(markQueue);
    gc_.MergeMarkStats(stats);
}

gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep(
        mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory, GCScheduler& gcScheduler) noexcept :
    objectFactory_(objectFactory),
    gcScheduler_(gcScheduler),
    finalizerProcessor_(std_support::make_unique<FinalizerProcessor>([this](int64_t epoch) { state_.finalized(epoch); })) {
    gcScheduler_.SetScheduleGC([this]() NO_INLINE {
        RuntimeLogDebug({kTagGC}, "Scheduling GC by thread %d", konan::currentThreadId());
        // This call acquires a lock, so we need to ensure that we're in the safe state.
        NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
        state_.schedule();
    });
    gcThread_ = ScopedThread(ScopedThread::attributes().name("GC thread"), [this] {
        while (true) {
            auto epoch = state_.waitScheduled();
            if (epoch.has_value()) {
                PerformFullGC(*epoch);
            } else {
                break;
            }
        }
    });
    markingBehavior_ = kotlin::compiler::gcMarkSingleThreaded() ? MarkingBehavior::kDoNotMark : MarkingBehavior::kMarkOwnStack;
    RuntimeLogDebug({kTagGC}, "Concurrent Mark & Sweep GC initialized");
}

gc::ConcurrentMarkAndSweep::~ConcurrentMarkAndSweep() {
    state_.shutdown();
}

void gc::ConcurrentMarkAndSweep::StartFinalizerThreadIfNeeded() noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
    finalizerProcessor_->StartFinalizerThreadIfNone();
    finalizerProcessor_->WaitFinalizerThreadInitialized();
}

void gc::ConcurrentMarkAndSweep::StopFinalizerThreadIfRunning() noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
    finalizerProcessor_->StopFinalizerThread();
}

bool gc::ConcurrentMarkAndSweep::FinalizersThreadIsRunning() noexcept {
    return finalizerProcessor_->IsRunning();
}

void gc::ConcurrentMarkAndSweep::SetMarkingBehaviorForTests(MarkingBehavior markingBehavior) noexcept {
    markingBehavior_ = markingBehavior;
}

bool gc::ConcurrentMarkAndSweep::PerformFullGC(int64_t epoch) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to suspend threads by thread %d", konan::currentThreadId());
    SetMarkingRequested();
    auto timeStartUs = konan::getTimeMicros();
    bool didSuspend = mm::RequestThreadsSuspension();
    RuntimeAssert(didSuspend, "Only GC thread can request suspension");
    RuntimeLogDebug({kTagGC}, "Requested thread suspension by thread %d", konan::currentThreadId());

    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "Concurrent GC must run on unregistered thread");
    WaitForThreadsReadyToMark();
    auto timeSuspendUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Suspended all threads in %" PRIu64 " microseconds", timeSuspendUs - timeStartUs);
    lastGCMarkStats_ = MarkStats();

    auto& scheduler = gcScheduler_;
    scheduler.gcData().OnPerformFullGC();

    state_.start(epoch);
    RuntimeLogInfo(
            {kTagGC}, "Started GC epoch %" PRId64 ". Time since last GC %" PRIu64 " microseconds", epoch, timeStartUs - lastGCTimestampUs_);

    CollectRootSetAndStartMarking();

    // Can be unsafe, because we've stopped the world.
    auto objectsCountBefore = objectFactory_.GetSizeUnsafe();

    auto markStats = gc::Mark<MarkTraits>(markQueue_);
    MergeMarkStats(markStats);

    RuntimeLogDebug({kTagGC}, "Waiting for marking in threads");
    mm::WaitForThreadsSuspension();
    auto timeMarkingUs = konan::getTimeMicros();
    RuntimeLogInfo({kTagGC}, "Collected root set of size %zu and marked %zu objects in all threads in %" PRIu64 " microseconds", lastGCMarkStats_.rootSetSize, lastGCMarkStats_.aliveHeapSet, timeMarkingUs - timeSuspendUs);

    scheduler.gcData().UpdateAliveSetBytes(lastGCMarkStats_.aliveHeapSetBytes);

    gc::SweepExtraObjects<SweepTraits>(mm::GlobalData::Instance().extraObjectDataFactory());
    auto timeSweepExtraObjectsUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Sweeped extra objects in %" PRIu64 " microseconds", timeSweepExtraObjectsUs - timeMarkingUs);

    auto objectFactoryIterable = objectFactory_.LockForIter();

    mm::ResumeThreads();
    auto timeResumeUs = konan::getTimeMicros();

    RuntimeLogInfo({kTagGC},
                    "Resumed threads in %" PRIu64 " microseconds. Total pause for most threads is %"  PRIu64" microseconds",
                    timeResumeUs - timeSweepExtraObjectsUs, timeResumeUs - timeStartUs);

    auto finalizerQueue = gc::Sweep<SweepTraits>(objectFactoryIterable);
    auto timeSweepUs = konan::getTimeMicros();
    RuntimeLogDebug({kTagGC}, "Swept in %" PRIu64 " microseconds", timeSweepUs - timeResumeUs);

    // Can be unsafe, because we have a lock in objectFactoryIterable
    auto objectsCountAfter = objectFactory_.GetSizeUnsafe();
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

namespace {
    bool isSuspendedOrNative(kotlin::mm::ThreadData& thread) noexcept {
        auto& suspensionData = thread.suspensionData();
        return suspensionData.suspended() || suspensionData.state() == kotlin::ThreadState::kNative;
    }

    template <typename F>
    bool allThreads(F predicate) noexcept {
        auto& threadRegistry = kotlin::mm::ThreadRegistry::Instance();
        auto* currentThread = (threadRegistry.IsCurrentThreadRegistered()) ? threadRegistry.CurrentThreadData() : nullptr;
        kotlin::mm::ThreadRegistry::Iterable threads = kotlin::mm::ThreadRegistry::Instance().LockForIter();
        for (auto& thread : threads) {
            // Handle if suspension was initiated by the mutator thread.
            if (&thread == currentThread) continue;
            if (!predicate(thread)) {
                return false;
            }
        }
        return true;
    }

    void yield() noexcept {
        std::this_thread::yield();
    }
} // namespace

void gc::ConcurrentMarkAndSweep::SetMarkingRequested() noexcept {
    markingRequested = markingBehavior_ == MarkingBehavior::kMarkOwnStack;
}

void gc::ConcurrentMarkAndSweep::WaitForThreadsReadyToMark() noexcept {
    while(!allThreads([](kotlin::mm::ThreadData& thread) { return isSuspendedOrNative(thread) || thread.gc().impl().gc().marking_.load(); })) {
        yield();
    }
}

NO_EXTERNAL_CALLS_CHECK void gc::ConcurrentMarkAndSweep::CollectRootSetAndStartMarking() noexcept {
        std::unique_lock lock(markingMutex);
        markingRequested = false;
        gc::collectRootSet<MarkTraits>(markQueue_, [](mm::ThreadData& thread) { return !thread.gc().impl().gc().marking_.load(); });
        RuntimeLogDebug({kTagGC}, "Requesting marking in threads");
        markingCondVar.notify_all();
}

void gc::ConcurrentMarkAndSweep::MergeMarkStats(gc::MarkStats stats) noexcept {
    std::unique_lock lock(markingMutex);
    lastGCMarkStats_.merge(stats);
}
