/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMarkAndSweep.hpp"

#include <cinttypes>
#include <optional>

#include "CallsChecker.hpp"
#include "CompilerConstants.hpp"
#include "GlobalData.hpp"
#include "GCImpl.hpp"
#include "Logging.hpp"
#include "MarkAndSweepUtils.hpp"
#include "Memory.h"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadSuspension.hpp"
#include "GCState.hpp"
#include "GCStatistics.hpp"

#ifdef CUSTOM_ALLOCATOR
#include "Heap.hpp"
#endif

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

struct ProcessWeaksTraits {
    static bool IsMarked(ObjHeader* obj) noexcept {
        auto& objectData = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(obj).ObjectData();
        return objectData.marked();
    }
};

} // namespace

void gc::ConcurrentMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
    // TODO: This will print the log for "manual" scheduling. Fix this.
    mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinished();
}

void gc::ConcurrentMarkAndSweep::ThreadData::OnSuspendForGC() noexcept {
    CallsCheckerIgnoreGuard guard;

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

#ifndef CUSTOM_ALLOCATOR
gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep(
        mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory,
        mm::ExtraObjectDataFactory& extraObjectDataFactory,
        gcScheduler::GCScheduler& gcScheduler) noexcept :
    objectFactory_(objectFactory),
    extraObjectDataFactory_(extraObjectDataFactory),
#else
gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep(gcScheduler::GCScheduler& gcScheduler) noexcept :
#endif
    gcScheduler_(gcScheduler),
    finalizerProcessor_([this](int64_t epoch) {
        GCHandle::getByEpoch(epoch).finalizersDone();
        state_.finalized(epoch);
    }) {
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
    finalizerProcessor_.StartFinalizerThreadIfNone();
    finalizerProcessor_.WaitFinalizerThreadInitialized();
}

void gc::ConcurrentMarkAndSweep::StopFinalizerThreadIfRunning() noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
    finalizerProcessor_.StopFinalizerThread();
}

bool gc::ConcurrentMarkAndSweep::FinalizersThreadIsRunning() noexcept {
    return finalizerProcessor_.IsRunning();
}

void gc::ConcurrentMarkAndSweep::SetMarkingBehaviorForTests(MarkingBehavior markingBehavior) noexcept {
    markingBehavior_ = markingBehavior;
}

void gc::ConcurrentMarkAndSweep::PerformFullGC(int64_t epoch) noexcept {
    auto gcHandle = GCHandle::create(epoch);
    SetMarkingRequested(epoch);
    bool didSuspend = mm::RequestThreadsSuspension();
    RuntimeAssert(didSuspend, "Only GC thread can request suspension");
    gcHandle.suspensionRequested();

    WaitForThreadsReadyToMark();
    gcHandle.threadsAreSuspended();

#ifdef CUSTOM_ALLOCATOR
    // This should really be done by each individual thread while waiting
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        thread.gc().impl().alloc().PrepareForGC();
    }
    heap_.PrepareForGC();
#endif

    auto& scheduler = gcScheduler_;
    scheduler.onGCStart();

    state_.start(epoch);

    CollectRootSetAndStartMarking(gcHandle);

    // Can be unsafe, because we've stopped the world.
    gc::Mark<internal::MarkTraits>(gcHandle, markQueue_);

    mm::WaitForThreadsSuspension();

#ifndef CUSTOM_ALLOCATOR
    // Taking the locks before the pause is completed. So that any destroying thread
    // would not publish into the global state at an unexpected time.
    std::optional extraObjectFactoryIterable = extraObjectDataFactory_.LockForIter();
    std::optional objectFactoryIterable = objectFactory_.LockForIter();
#endif

    if (compiler::concurrentWeakSweep()) {
        // Expected to happen inside STW.
        gc::EnableWeakRefBarriers();

        mm::ResumeThreads();
        gcHandle.threadsAreResumed();
    }

    gc::processWeaks<ProcessWeaksTraits>(gcHandle, mm::SpecialRefRegistry::instance());

    if (compiler::concurrentWeakSweep()) {
        // Expected to happen outside STW.
        gc::DisableWeakRefBarriers();
    } else {
        mm::ResumeThreads();
        gcHandle.threadsAreResumed();
    }

#ifndef CUSTOM_ALLOCATOR
    gc::SweepExtraObjects<SweepTraits>(gcHandle, *extraObjectFactoryIterable);
    extraObjectFactoryIterable = std::nullopt;
    auto finalizerQueue = gc::Sweep<SweepTraits>(gcHandle, *objectFactoryIterable);
    objectFactoryIterable = std::nullopt;
    kotlin::compactObjectPoolInMainThread();
#else
    // also sweeps extraObjects
    auto finalizerQueue = heap_.Sweep(gcHandle);
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        finalizerQueue.TransferAllFrom(thread.gc().impl().alloc().ExtractFinalizerQueue());
    }
#endif
    scheduler.onGCFinish(epoch, allocatedBytes());
    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();

    // This may start a new thread. On some pthreads implementations, this may block waiting for concurrent thread
    // destructors running. So, it must ensured that no locks are held by this point.
    // TODO: Consider having an always on sleeping finalizer thread.
    finalizerProcessor_.ScheduleTasks(std::move(finalizerQueue), epoch);
}

void gc::ConcurrentMarkAndSweep::SetMarkingRequested(uint64_t epoch) noexcept {
    markingRequested = markingBehavior_ == MarkingBehavior::kMarkOwnStack;
    markingEpoch = epoch;
}

void gc::ConcurrentMarkAndSweep::WaitForThreadsReadyToMark() noexcept {
    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "GC must run on unregistered thread");
    mm::ThreadRegistry::Instance().waitAllThreads([](mm::ThreadData& thread) noexcept {
        return thread.suspensionData().suspendedOrNative() || thread.gc().impl().gc().marking_.load();
    });
}

void gc::ConcurrentMarkAndSweep::CollectRootSetAndStartMarking(GCHandle gcHandle) noexcept {
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
