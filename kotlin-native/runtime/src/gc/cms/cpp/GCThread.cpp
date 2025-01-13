/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCThread.hpp"

#include "Allocator.hpp"
#include "AllocatorImpl.hpp"
#include "ConcurrentMark.hpp"
#include "GCScheduler.hpp"
#include "GCStatistics.hpp"
#include "Logging.hpp"
#include "MarkAndSweepUtils.hpp"
#include "RootSet.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadSuspension.hpp"

using namespace kotlin;

namespace {

#ifndef CUSTOM_ALLOCATOR
// TODO move to common
[[maybe_unused]] inline void checkMarkCorrectness(alloc::ObjectFactoryImpl::Iterable& heap) {
    if (compiler::runtimeAssertsMode() == compiler::RuntimeAssertsMode::kIgnore) return;
    for (auto objRef : heap) {
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

gc::internal::GCThread::GCThread(
        GCStateHolder& state,
        SegregatedGCFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits>& finalizerProcessor,
        mark::ConcurrentMark& markDispatcher,
        alloc::Allocator& allocator,
        gcScheduler::GCScheduler& gcScheduler) noexcept :
    state_(state),
    finalizerProcessor_(finalizerProcessor),
    markDispatcher_(markDispatcher),
    allocator_(allocator),
    gcScheduler_(gcScheduler),
    thread_(std::string_view("Main GC thread"), [this] { body(); }) {}

void gc::internal::GCThread::body() noexcept {
    RuntimeLogWarning({kTagGC}, "Initializing Concurrent Mark and Sweep GC.");
    while (true) {
        if (auto epoch = state_.waitScheduled()) {
            PerformFullGC(*epoch);
        } else {
            break;
        }
    }
}

void gc::internal::GCThread::PerformFullGC(int64_t epoch) noexcept {
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();

    auto gcHandle = GCHandle::create(epoch);

    markDispatcher_.beginMarkingEpoch(gcHandle);
    GCLogDebug(epoch, "Main GC requested marking in mutators");

    stopTheWorld(gcHandle, "GC stop the world #1: collect root set");

    auto& scheduler = gcScheduler_;
    scheduler.onGCStart();

    state_.start(epoch);

    markDispatcher_.runMainInSTW();

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
        finalizerQueue.mergeFrom(thread.allocator().impl().alloc().ExtractFinalizerQueue());
    }
    finalizerQueue.mergeFrom(allocator_.impl().heap().ExtractFinalizerQueue());
#endif
    scheduler.onGCFinish(epoch, gcHandle.getKeptSizeBytes());
    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();

    // This may start a new thread. On some pthreads implementations, this may block waiting for concurrent thread
    // destructors running. So, it must ensured that no locks are held by this point.
    // TODO: Consider having an always on sleeping finalizer thread.
    finalizerProcessor_.schedule(std::move(finalizerQueue), epoch);
}
