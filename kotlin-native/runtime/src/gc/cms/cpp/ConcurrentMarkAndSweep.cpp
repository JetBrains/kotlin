/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMarkAndSweep.hpp"

#include <cinttypes>
#include <optional>

#include "CompilerConstants.hpp"
#include "GlobalData.hpp"
#include "GCImpl.hpp"
#include "Logging.hpp"
#include "MarkAndSweepUtils.hpp"
#include "Memory.h"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadSuspension.hpp"
#include "FinalizerProcessor.hpp"
#include "GCStatistics.hpp"

#ifdef CUSTOM_ALLOCATOR
#include "CustomFinalizerProcessor.hpp"
#include "Heap.hpp"
#endif

using namespace kotlin;

namespace {

[[clang::no_destroy]] std::mutex gcMutex;

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

template<typename Body>
ScopedThread createGCThread(const char* name, Body&& body) {
    return ScopedThread(ScopedThread::attributes().name(name), [name, body] {
        RuntimeLogDebug({kTagGC}, "%s %d starts execution", name, konan::currentThreadId());
        body();
        RuntimeLogDebug({kTagGC}, "%s %d finishes execution", name, konan::currentThreadId());
    });
}

// TODO move to common
[[maybe_unused]] inline void checkMarkCorrectness(mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::Iterable& heap) {
    if (compiler::runtimeAssertsMode() == compiler::RuntimeAssertsMode::kIgnore) return;
    for (auto objRef: heap) {
        auto obj = objRef.GetObjHeader();
        auto& objData = objRef.ObjectData();
        if (objData.marked()) {
            traverseReferredObjects(obj, [obj](ObjHeader* field) {
                if (field->heap()) {
                    auto& fieldObjData =
                            mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(field).ObjectData();
                    RuntimeAssert(fieldObjData.marked(), "Field %p of an alive obj %p must be alive", field, obj);
                }
            });
        }
    }
}

} // namespace

void gc::ConcurrentMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    gcScheduler_.OnSafePointAllocation(size);
    mm::SuspendIfRequested();
}

void gc::ConcurrentMarkAndSweep::ThreadData::Schedule() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    ThreadStateGuard guard(ThreadState::kNative);
    gc_.state_.schedule();
}

void gc::ConcurrentMarkAndSweep::ThreadData::ScheduleAndWaitFullGC() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    ThreadStateGuard guard(ThreadState::kNative);
    auto scheduled_epoch = gc_.state_.schedule();
    gc_.state_.waitEpochFinished(scheduled_epoch);
}

void gc::ConcurrentMarkAndSweep::ThreadData::ScheduleAndWaitFullGCWithFinalizers() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    ThreadStateGuard guard(ThreadState::kNative);
    auto scheduled_epoch = gc_.state_.schedule();
    gc_.state_.waitEpochFinalized(scheduled_epoch);
}

void gc::ConcurrentMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
    ScheduleAndWaitFullGC();
}

void gc::ConcurrentMarkAndSweep::ThreadData::OnSuspendForGC() noexcept {
    mark::MarkDispatcher::MarkJob markJob(gc_.markDispatcher_);
    markJob.runOnMutator(commonThreadData());
}

bool gc::ConcurrentMarkAndSweep::ThreadData::tryLockRootSet() {
    bool expected = false;
    bool locked = rootSetLocked_.compare_exchange_strong(expected, true, std::memory_order_acq_rel);
    if (locked) {
        RuntimeLogDebug({kTagGC}, "Thread %d have exclusively acquired thread %d's root set", konan::currentThreadId(), threadData_.threadId());
    }
    return locked;
}

bool gc::ConcurrentMarkAndSweep::ThreadData::rootSetLocked() const {
    return rootSetLocked_.load(std::memory_order_acquire);
}

void gc::ConcurrentMarkAndSweep::ThreadData::beginCooperation() {
    cooperative_.store(true, std::memory_order_release);
}

bool gc::ConcurrentMarkAndSweep::ThreadData::cooperative() const {
    return cooperative_.load(std::memory_order_relaxed);
}

void gc::ConcurrentMarkAndSweep::ThreadData::publish() {
    threadData_.Publish();
    published_.store(true, std::memory_order_release);
}

bool gc::ConcurrentMarkAndSweep::ThreadData::published() const {
    return published_.load(std::memory_order_acquire);
}

void gc::ConcurrentMarkAndSweep::ThreadData::clearMarkFlags() {
    published_.store(false, std::memory_order_relaxed);
    cooperative_.store(false, std::memory_order_relaxed);
    rootSetLocked_.store(false, std::memory_order_release);
}

mm::ThreadData& gc::ConcurrentMarkAndSweep::ThreadData::commonThreadData() const {
    return threadData_;
}

#ifndef CUSTOM_ALLOCATOR
gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep(
        mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory, GCScheduler& gcScheduler,
        bool mutatorsCooperate, std::size_t auxGCThreads) noexcept :
    objectFactory_(objectFactory),
    gcScheduler_(gcScheduler),
    finalizerProcessor_(std_support::make_unique<FinalizerProcessor>([this](int64_t epoch) {
#else
gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep(GCScheduler& gcScheduler,
                                                   bool mutatorsCooperate, std::size_t auxGCThreads) noexcept :
    gcScheduler_(gcScheduler),
    finalizerProcessor_(std_support::make_unique<alloc::CustomFinalizerProcessor>([this](int64_t epoch) {
#endif
        GCHandle::getByEpoch(epoch).finalizersDone();
        state_.finalized(epoch);
    })),
    markDispatcher_(1 + auxGCThreads, mutatorsCooperate),
    mainThread_(createGCThread("Main GC thread", [this] { mainGCThreadBody(); }))
{
    gcScheduler_.SetScheduleGC([this]() NO_INLINE {
        // This call acquires a lock, so we need to ensure that we're in the safe state.
        NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
        state_.schedule();
    });
    for (std::size_t i = 0; i < auxGCThreads; ++i) {
        auxThreads_.emplace_back(createGCThread("Auxiliary GC thread", [this] { auxiliaryGCThreadBody(); }));
    }
    RuntimeLogInfo({kTagGC}, "Parallel Mark & Concurrent Sweep GC initialized");
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

void gc::ConcurrentMarkAndSweep::mainGCThreadBody() {
    gc::mark::MarkDispatcher::MarkJob markJob(markDispatcher_);
    while (true) {
        auto epoch = state_.waitScheduled();
        if (epoch.has_value()) {
            PerformFullGC(*epoch, markJob);
        } else {
            break;
        }
    }
    markDispatcher_.requestShutdown();
}

void gc::ConcurrentMarkAndSweep::auxiliaryGCThreadBody() {
    mark::MarkDispatcher::MarkJob markJob(markDispatcher_);
    while (!markDispatcher_.shutdownRequested()) {
        markJob.runAuxiliary();
    }
}

bool gc::ConcurrentMarkAndSweep::PerformFullGC(int64_t epoch, mark::MarkDispatcher::MarkJob& markContext) noexcept {
    std::unique_lock mainGCLock(gcMutex);
    auto gcHandle = GCHandle::create(epoch);

    auto& scheduler = gcScheduler_;
    scheduler.gcData().OnPerformFullGC();

    state_.start(epoch);
    markDispatcher_.beginMarkingEpoch(gcHandle);
    GCLogDebug(epoch, "Main GC requested marking in mutators");

    // Request STW
    bool didSuspend = mm::RequestThreadsSuspension();
    RuntimeAssert(didSuspend, "Only GC thread can request suspension");
    gcHandle.suspensionRequested();

    RuntimeAssert(!kotlin::mm::IsCurrentThreadRegistered(), "Concurrent GC must run on unregistered thread");

    markDispatcher_.waitForThreadsPauseMutation();
    GCLogDebug(epoch, "All threads have paused mutation");
    gcHandle.threadsAreSuspended();

#ifdef CUSTOM_ALLOCATOR
    heap_.PrepareForGC();
#endif

    markContext.runMainInSTW();

    auto markStats = gcHandle.getMarked();
    scheduler.gcData().UpdateAliveSetBytes(markStats.markedSizeBytes);

    gc::processWeaks<ProcessWeaksTraits>(gcHandle, mm::SpecialRefRegistry::instance());

#ifndef CUSTOM_ALLOCATOR
    // Taking the locks before the pause is completed. So that any destroying thread
    // would not publish into the global state at an unexpected time.
    std::optional extraObjectFactoryIterable = mm::GlobalData::Instance().extraObjectDataFactory().LockForIter();
    std::optional objectFactoryIterable = objectFactory_.LockForIter();
    checkMarkCorrectness(*objectFactoryIterable);
    mm::ResumeThreads();
    gcHandle.threadsAreResumed();

    gc::SweepExtraObjects<SweepTraits>(gcHandle, *extraObjectFactoryIterable);
    extraObjectFactoryIterable = std::nullopt;
    auto finalizerQueue = gc::Sweep<SweepTraits>(gcHandle, *objectFactoryIterable);
    objectFactoryIterable = std::nullopt;
    kotlin::compactObjectPoolInMainThread();

#else
    auto finalizerQueue = heap_.SweepExtraObjects(gcHandle);

    mm::ResumeThreads();
    gcHandle.threadsAreResumed();
    heap_.Sweep(gcHandle);
#endif
    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();
    // This may start a new thread. On some pthreads implementations, this may block waiting for concurrent thread
    // destructors running. So, it must ensured that no locks are held by this point.
    // TODO: Consider having an always on sleeping finalizer thread.
    finalizerProcessor_->ScheduleTasks(std::move(finalizerQueue), epoch);
    return true;
}

void gc::ConcurrentMarkAndSweep::reconfigure(bool mutatorsCooperate, std::size_t auxGCThreads) noexcept {
    std::unique_lock mainGCLock(gcMutex);
    markDispatcher_.reset(mutatorsCooperate, 1 + auxGCThreads, [this] { auxThreads_.clear(); });
    for (std::size_t i = 0; i < auxGCThreads; ++i) {
        auxThreads_.emplace_back(createGCThread("Auxiliary GC thread", [this] { auxiliaryGCThreadBody(); }));
    }
}