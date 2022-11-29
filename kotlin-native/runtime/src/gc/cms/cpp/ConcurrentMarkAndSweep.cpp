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
#include "GCStatistics.hpp"

using namespace kotlin;

namespace {
    [[clang::no_destroy]] std::mutex markingMutex;
    [[clang::no_destroy]] std::condition_variable markingCondVar;
    [[clang::no_destroy]] std::atomic<bool> markingRequested = false;
    [[clang::no_destroy]] std::atomic<uint64_t> markingEpoch = 0;

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>;
    using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

    static bool IsMarkedByExtraObject(mm::ExtraObjectData &object) noexcept {
        auto *baseObject = object.GetBaseObject();
        if (!baseObject->heap()) return true;
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(baseObject).ObjectData();
        return objectData.marked();
    }

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.ObjectData();
        return objectData.tryResetMark();
    }
};

} // namespace

void gc::ConcurrentMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    gcScheduler_.OnSafePointAllocation(size);
    mm::SuspendIfRequested();
}

void gc::ConcurrentMarkAndSweep::ThreadData::Schedule() noexcept {
    ThreadStateGuard guard(ThreadState::kNative);
    gc_.state_.schedule();
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
    if (!markingRequested.load()) return;
    AutoReset scopedAssignMarking(&marking_, true);
    threadData_.Publish();
    markingCondVar.wait(lock, []() { return !markingRequested.load(); });
    // // Unlock while marking to allow mutliple threads to mark in parallel.
    lock.unlock();
    uint64_t epoch = markingEpoch.load();
    GCLogDebug(epoch, "Parallel marking in thread %d", konan::currentThreadId());
    MarkQueue markQueue;
    auto handle = GCHandle::getByEpoch(epoch);
    gc::collectRootSetForThread<internal::MarkTraits>(handle, markQueue, threadData_);
    gc::Mark<internal::MarkTraits>(handle, markQueue);
}

gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep(
        mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory, GCScheduler& gcScheduler) noexcept :
    objectFactory_(objectFactory),
    gcScheduler_(gcScheduler),
    finalizerProcessor_(std_support::make_unique<FinalizerProcessor>([this](int64_t epoch) {
        state_.finalized(epoch);
        GCHandle::getByEpoch(epoch).finalizersDone();
    })) {
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
    auto gcHandle = GCHandle::create(epoch);
    SetMarkingRequested(epoch);
    bool didSuspend = mm::RequestThreadsSuspension();
    RuntimeAssert(didSuspend, "Only GC thread can request suspension");
    gcHandle.suspensionRequested();

    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "Concurrent GC must run on unregistered thread");
    WaitForThreadsReadyToMark();
    gcHandle.threadsAreSuspended();

    auto& scheduler = gcScheduler_;
    scheduler.gcData().OnPerformFullGC();

    state_.start(epoch);

    CollectRootSetAndStartMarking(gcHandle);

    // Can be unsafe, because we've stopped the world.
    gc::Mark<internal::MarkTraits>(gcHandle, markQueue_);

    mm::WaitForThreadsSuspension();
    mm::ExtraObjectDataFactory& extraObjectDataFactory = mm::GlobalData::Instance().extraObjectDataFactory();
    auto markStats = gcHandle.getMarked();
    scheduler.gcData().UpdateAliveSetBytes(markStats.totalObjectsSize);

    gc::SweepExtraObjects<SweepTraits>(gcHandle, extraObjectDataFactory);

    auto objectFactoryIterable = objectFactory_.LockForIter();

    mm::ResumeThreads();
    gcHandle.threadsAreResumed();

    auto finalizerQueue = gc::Sweep<SweepTraits>(gcHandle, objectFactoryIterable);

    kotlin::compactObjectPoolInMainThread();

    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();
    finalizerProcessor_->ScheduleTasks(std::move(finalizerQueue), epoch);
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

void gc::ConcurrentMarkAndSweep::SetMarkingRequested(uint64_t epoch) noexcept {
    markingRequested = markingBehavior_ == MarkingBehavior::kMarkOwnStack;
    markingEpoch = epoch;
}

void gc::ConcurrentMarkAndSweep::WaitForThreadsReadyToMark() noexcept {
    while(!allThreads([](kotlin::mm::ThreadData& thread) { return isSuspendedOrNative(thread) || thread.gc().impl().gc().marking_.load(); })) {
        yield();
    }
}

NO_EXTERNAL_CALLS_CHECK void gc::ConcurrentMarkAndSweep::CollectRootSetAndStartMarking(GCHandle gcHandle) noexcept {
        std::unique_lock lock(markingMutex);
        markingRequested = false;
        gc::collectRootSet<internal::MarkTraits>(
                gcHandle,
                markQueue_,
                [](mm::ThreadData& thread) {
                    return !thread.gc().impl().gc().marking_.load();
                }
            );
        RuntimeLogDebug({kTagGC}, "Requesting marking in threads");
        markingCondVar.notify_all();
}
