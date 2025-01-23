/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCThread.hpp"

#include "Allocator.hpp"
#include "AllocatorImpl.hpp"
#include "GCScheduler.hpp"
#include "GCStatistics.hpp"
#include "Logging.hpp"
#include "MarkAndSweepUtils.hpp"
#include "RootSet.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadSuspension.hpp"

using namespace kotlin;

gc::internal::GCThread::GCThread(
        GCStateHolder& state,
        SegregatedGCFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits>& finalizerProcessor,
        alloc::Allocator& allocator,
        gcScheduler::GCScheduler& gcScheduler) noexcept :
    state_(state),
    finalizerProcessor_(finalizerProcessor),
    allocator_(allocator),
    gcScheduler_(gcScheduler),
    thread_(std::string_view("GC thread"), [this] { body(); }) {}

void gc::internal::GCThread::body() noexcept {
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

    stopTheWorld(gcHandle, "GC stop the world");

    gcScheduler_.onGCStart();

    state_.start(epoch);

    gc::collectRootSet<internal::MarkTraits>(gcHandle, markQueue_, [](mm::ThreadData&) { return true; });

    gc::Mark<internal::MarkTraits>(gcHandle, markQueue_);

    gc::processWeaks<DefaultProcessWeaksTraits>(gcHandle, mm::ExternalRCRefRegistry::instance());

    // This should really be done by each individual thread while waiting
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        thread.allocator().prepareForGC();
    }
    allocator_.prepareForGC();

#ifndef CUSTOM_ALLOCATOR
    // Taking the locks before the pause is completed. So that any destroying thread
    // would not publish into the global state at an unexpected time.
    std::optional extraObjectFactoryIterable = allocator_.impl().extraObjectDataFactory().LockForIter();
    std::optional objectFactoryIterable = allocator_.impl().objectFactory().LockForIter();

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

    gcScheduler_.onGCFinish(epoch, gcHandle.getKeptSizeBytes());

    resumeTheWorld(gcHandle);

    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();
    finalizerProcessor_.schedule(std::move(finalizerQueue), epoch);
}
