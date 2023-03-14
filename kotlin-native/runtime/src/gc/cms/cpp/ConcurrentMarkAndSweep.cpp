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

template<typename Body>
ScopedThread createGCThread(const char* name, Body&& body) {
    return ScopedThread(ScopedThread::attributes().name(name), [name, body] {
        RuntimeLogDebug({kTagGC}, "%s %d starts execution", name, konan::currentThreadId());
        body();
        RuntimeLogDebug({kTagGC}, "%s %d finishes execution", name, konan::currentThreadId());
    });
}

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

void gc::ConcurrentMarkAndSweep::ThreadData::OnSuspendForGC() noexcept {
    mark::MarkDispatcher::MarkJob markJob(gc_.markDispatcher_);
    markJob.runOnMutator(commonThreadData());
}

bool gc::ConcurrentMarkAndSweep::ThreadData::tryLockRootSet(bool own) {
    MarkedBy expected = MarkedBy::kNone;
    MarkedBy desired = own ? MarkedBy::kItself : MarkedBy::kOther;
    bool locked = markedBy_.compare_exchange_strong(expected, desired, std::memory_order_release);
    if (locked) {
        RuntimeLogDebug({kTagGC}, "Thread %d have exclusively acquired thread %d's root set", konan::currentThreadId(), threadData_.threadId());
    }
    return locked;
}

void gc::ConcurrentMarkAndSweep::ThreadData::clearMarkedBy() {
    markedBy_.store(MarkedBy::kNone, std::memory_order_relaxed);
}

bool gc::ConcurrentMarkAndSweep::ThreadData::rootSetLocked() const {
    return markedBy_.load(std::memory_order_relaxed) != MarkedBy::kNone;
}

bool gc::ConcurrentMarkAndSweep::ThreadData::cooperate() const {
    return markedBy_.load(std::memory_order_relaxed) == MarkedBy::kItself;
}

bool gc::ConcurrentMarkAndSweep::ThreadData::published() const {
    return published_.load(std::memory_order_acquire);
}

void gc::ConcurrentMarkAndSweep::ThreadData::markPublished() {
    published_.store(true, std::memory_order_release);
}

mm::ThreadData& gc::ConcurrentMarkAndSweep::ThreadData::commonThreadData() const {
    return threadData_;
}

gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep(mm::ObjectFactory<ConcurrentMarkAndSweep>& objectFactory,
                                                   GCScheduler& gcScheduler,
                                                   bool mutatorsCooperate, std::size_t auxGCThreads) noexcept :
#ifndef CUSTOM_ALLOCATOR
    objectFactory_(objectFactory),
    gcScheduler_(gcScheduler),
    finalizerProcessor_(std_support::make_unique<FinalizerProcessor>([this](int64_t epoch) {
#else
    gcScheduler_(gcScheduler), finalizerProcessor_(std_support::make_unique<alloc::CustomFinalizerProcessor>([this](int64_t epoch) {
#endif
        GCHandle::getByEpoch(epoch).finalizersDone();
        state_.finalized(epoch);
    })),
    markDispatcher_(1 + auxGCThreads, mutatorsCooperate),
    mainThread_(createGCThread("Main GC thread", [this] { mainGCThreadBody(); }))
{
    gcScheduler_.SetScheduleGC([this]() NO_INLINE {
        RuntimeLogDebug({kTagGC}, "Scheduling GC by thread %d", konan::currentThreadId());
        // This call acquires a lock, so we need to ensure that we're in the safe state.
        NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
        state_.schedule();
    });
    for (std::size_t i = 0; i < auxGCThreads; ++i) {
        auxThreads_.emplace_back(createGCThread("Auxiliary GC thread", [this] { auxiliaryGCThreadBody(); }));
    }
    RuntimeLogDebug({kTagGC}, "Stoop The World Mark & Sweep GC initialized");
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
    // STW begins

#ifdef CUSTOM_ALLOCATOR
    heap_.PrepareForGC();
#endif

    markContext.runMainInSTW();

    auto markStats = gcHandle.getMarked();
    scheduler.gcData().UpdateAliveSetBytes(markStats.totalObjectsSize);

#ifndef CUSTOM_ALLOCATOR
    mm::ExtraObjectDataFactory& extraObjectDataFactory = mm::GlobalData::Instance().extraObjectDataFactory();
    gc::SweepExtraObjects<SweepTraits>(gcHandle, extraObjectDataFactory);

    auto objectFactoryIterable = objectFactory_.LockForIter();
    // TODO outline
//    for (auto objRef: objectFactoryIterable) {
//        auto obj = objRef.GetObjHeader();
//        auto& objData = objRef.ObjectData();
//        auto* typeInfo = obj->type_info();
//        if (objData.marked()) {
//            if (typeInfo != theArrayTypeInfo) {
//                for (int i = 0; i < typeInfo->objOffsetsCount_; ++i) {
//                    auto offs = typeInfo->objOffsets_[i];
//                    auto* field = *reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(obj) + offs);
//                    if (!field) continue;
//                    auto& fieldObjData = mm::ObjectFactory<ConcurrentMarkAndSweep>::NodeRef::From(obj).ObjectData();
//                    RuntimeAssert(fieldObjData.marked(), "Field %p of an alive obj %p must be alive", field, obj);
//                }
//            }
//        }
//    }

    mm::ResumeThreads();
    gcHandle.threadsAreResumed();
    // STW ends

    auto finalizerQueue = gc::Sweep<SweepTraits>(gcHandle, objectFactoryIterable);
    kotlin::compactObjectPoolInMainThread();

#else
    auto finalizerQueue = heap_.SweepExtraObjects(gcHandle);

    mm::ResumeThreads();
    gcHandle.threadsAreResumed();
    // STW ends

    heap_.Sweep();
#endif
    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();
    finalizerProcessor_->ScheduleTasks(std::move(finalizerQueue), epoch);
    return true;
}

void gc::ConcurrentMarkAndSweep::reconfigure(bool mutatorsCooperate, std::size_t auxGCThreads) {
    std::unique_lock mainGCLock(gcMutex);
    markDispatcher_.reset(mutatorsCooperate, 1 + auxGCThreads, [this] { auxThreads_.clear(); });
    for (std::size_t i = 0; i < auxGCThreads; ++i) {
        auxThreads_.emplace_back(createGCThread("Auxiliary GC thread", [this] { auxiliaryGCThreadBody(); }));
    }
}