/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMarkAndSweep.hpp"

#include <cinttypes>
#include <optional>

#include "AllocatorImpl.hpp"
#include "CallsChecker.hpp"
#include "CompilerConstants.hpp"
#include "GlobalData.hpp"
#include "GCImpl.hpp"
#include "Logging.hpp"
#include "MarkAndSweepUtils.hpp"
#include "Memory.h"
#include "ThreadData.hpp"
#include "ThreadSuspension.hpp"
#include "GCState.hpp"
#include "GCStatistics.hpp"

using namespace kotlin;

namespace {

[[clang::no_destroy]] std::mutex gcMutex;

template<typename Body>
ScopedThread createGCThread(const char* name, Body&& body) {
    return ScopedThread(ScopedThread::attributes().name(name), [name, body] {
        RuntimeLogDebug({kTagGC}, "%s %d starts execution", name, konan::currentThreadId());
        body();
        RuntimeLogDebug({kTagGC}, "%s %d finishes execution", name, konan::currentThreadId());
    });
}

#ifndef CUSTOM_ALLOCATOR
// TODO move to common
[[maybe_unused]] inline void checkMarkCorrectness(alloc::ObjectFactoryImpl::Iterable& heap) {
    if (compiler::runtimeAssertsMode() == compiler::RuntimeAssertsMode::kIgnore) return;
    for (auto objRef: heap) {
        auto obj = objRef.GetObjHeader();
        auto& objData = objRef.ObjectData();
        if (objData.marked()) {
            traverseReferredObjects(obj, [obj](ObjHeader* field) {
                if (field->heap()) {
                    auto& fieldObjData = alloc::ObjectFactoryImpl::NodeRef::From(field).ObjectData();
                    RuntimeAssert(fieldObjData.marked(), "Field %p of an alive obj %p must be alive", field, obj);
                }
            });
        }
    }
}
#endif

} // namespace

void gc::ConcurrentMarkAndSweep::ThreadData::OnSuspendForGC() noexcept {
    CallsCheckerIgnoreGuard guard;

    gc_.markDispatcher_.runOnMutator(commonThreadData());
}

bool gc::ConcurrentMarkAndSweep::ThreadData::tryLockRootSet() {
    bool expected = false;
    bool locked = rootSetLocked_.compare_exchange_strong(expected, true, std::memory_order_acq_rel);
    if (locked) {
        RuntimeLogDebug({kTagGC}, "Thread %d have exclusively acquired thread %d's root set", konan::currentThreadId(), threadData_.threadId());
    }
    return locked;
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
    rootSetLocked_.store(false, std::memory_order_release);
}

mm::ThreadData& gc::ConcurrentMarkAndSweep::ThreadData::commonThreadData() const {
    return threadData_;
}

gc::ConcurrentMarkAndSweep::ConcurrentMarkAndSweep(
        alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler, bool mutatorsCooperate, std::size_t auxGCThreads) noexcept :
    allocator_(allocator),
    gcScheduler_(gcScheduler),
    finalizerProcessor_([this](int64_t epoch) {
        GCHandle::getByEpoch(epoch).finalizersDone();
        state_.finalized(epoch);
    }),
    markDispatcher_(mutatorsCooperate),
    mainThread_(createGCThread("Main GC thread", [this] { mainGCThreadBody(); })) {
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

void gc::ConcurrentMarkAndSweep::mainGCThreadBody() {
    while (true) {
        auto epoch = state_.waitScheduled();
        if (epoch.has_value()) {
            PerformFullGC(*epoch);
        } else {
            break;
        }
    }
    markDispatcher_.requestShutdown();
}

void gc::ConcurrentMarkAndSweep::auxiliaryGCThreadBody() {
    RuntimeAssert(!compiler::gcMarkSingleThreaded(), "Should not reach here during single threaded mark");
    while (!markDispatcher_.shutdownRequested()) {
        markDispatcher_.runAuxiliary();
    }
}

void gc::ConcurrentMarkAndSweep::PerformFullGC(int64_t epoch) noexcept {
    std::unique_lock mainGCLock(gcMutex);
    auto gcHandle = GCHandle::create(epoch);

    markDispatcher_.beginMarkingEpoch(gcHandle);
    GCLogDebug(epoch, "Main GC requested marking in mutators");

    stopTheWorld(gcHandle);

    auto& scheduler = gcScheduler_;
    scheduler.onGCStart();

    state_.start(epoch);

    markDispatcher_.runMainInSTW();

    markDispatcher_.endMarkingEpoch();

    if (compiler::concurrentWeakSweep()) {
        // Expected to happen inside STW.
        gc::EnableWeakRefBarriers(epoch);
        resumeTheWorld(gcHandle);
    }

    gc::processWeaks<DefaultProcessWeaksTraits>(gcHandle, mm::SpecialRefRegistry::instance());

    if (compiler::concurrentWeakSweep()) {
        stopTheWorld(gcHandle);
        gc::DisableWeakRefBarriers();
    }

    // TODO outline as mark_.isolateMarkedHeapAndFinishMark()
    // By this point all the alive heap must be marked.
    // All the mutations (incl. allocations) after this method will be subject for the next GC.
    // This should really be done by each individual thread while waiting
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        thread.allocator().prepareForGC();
    }
    allocator_.prepareForGC();

#ifndef CUSTOM_ALLOCATOR
    // Taking the locks before the pause is completed. So that any destroying thread
    // would not publish into the global state at an unexpected time.
    std::optional objectFactoryIterable = allocator_.impl().objectFactory().LockForIter();
    std::optional extraObjectFactoryIterable = allocator_.impl().extraObjectDataFactory().LockForIter();

    checkMarkCorrectness(*objectFactoryIterable);
#endif

    resumeTheWorld(gcHandle);

#ifndef CUSTOM_ALLOCATOR
    alloc::SweepExtraObjects<alloc::DefaultSweepTraits<alloc::ObjectFactoryImpl>>(gcHandle, *extraObjectFactoryIterable);
    extraObjectFactoryIterable = std::nullopt;
    auto finalizerQueue = alloc::Sweep<alloc::DefaultSweepTraits<alloc::ObjectFactoryImpl>>(gcHandle, *objectFactoryIterable);
    objectFactoryIterable = std::nullopt;
    alloc::compactObjectPoolInMainThread();
#else
    // also sweeps extraObjects
    auto finalizerQueue = allocator_.impl().heap().Sweep(gcHandle);
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        finalizerQueue.TransferAllFrom(thread.allocator().impl().alloc().ExtractFinalizerQueue());
    }
    finalizerQueue.TransferAllFrom(allocator_.impl().heap().ExtractFinalizerQueue());
#endif
    scheduler.onGCFinish(epoch, alloc::allocatedBytes());
    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();

    // This may start a new thread. On some pthreads implementations, this may block waiting for concurrent thread
    // destructors running. So, it must ensured that no locks are held by this point.
    // TODO: Consider having an always on sleeping finalizer thread.
    finalizerProcessor_.ScheduleTasks(std::move(finalizerQueue), epoch);
}

void gc::ConcurrentMarkAndSweep::reconfigure(std::size_t maxParallelism, bool mutatorsCooperate, std::size_t auxGCThreads) noexcept {
    if (compiler::gcMarkSingleThreaded()) {
        RuntimeCheck(auxGCThreads == 0, "Auxiliary GC threads must not be created with gcMarkSingleThread");
        return;
    }
    std::unique_lock mainGCLock(gcMutex);
    markDispatcher_.reset(maxParallelism, mutatorsCooperate, [this] { auxThreads_.clear(); });
    for (std::size_t i = 0; i < auxGCThreads; ++i) {
        auxThreads_.emplace_back(createGCThread("Auxiliary GC thread", [this] { auxiliaryGCThreadBody(); }));
    }
}
