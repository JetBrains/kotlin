/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SameThreadMarkAndSweep.hpp"

#include <cinttypes>

#include "CompilerConstants.hpp"
#include "GlobalData.hpp"
#include "GCImpl.hpp"
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

#ifdef CUSTOM_ALLOCATOR
gc::SameThreadMarkAndSweep::SameThreadMarkAndSweep(
        gcScheduler::GCScheduler& gcScheduler) noexcept :
#else
gc::SameThreadMarkAndSweep::SameThreadMarkAndSweep(
        ObjectFactory& objectFactory, alloc::ExtraObjectDataFactory& extraObjectDataFactory, gcScheduler::GCScheduler& gcScheduler) noexcept :

    objectFactory_(objectFactory),
    extraObjectDataFactory_(extraObjectDataFactory),
#endif
    gcScheduler_(gcScheduler), finalizerProcessor_([this](int64_t epoch) noexcept {
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
    RuntimeLogDebug({kTagGC}, "Same thread Mark & Sweep GC initialized");
}

gc::SameThreadMarkAndSweep::~SameThreadMarkAndSweep() {
    state_.shutdown();
}

void gc::SameThreadMarkAndSweep::StartFinalizerThreadIfNeeded() noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
    finalizerProcessor_.StartFinalizerThreadIfNone();
    finalizerProcessor_.WaitFinalizerThreadInitialized();
}

void gc::SameThreadMarkAndSweep::StopFinalizerThreadIfRunning() noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
    finalizerProcessor_.StopFinalizerThread();
}

bool gc::SameThreadMarkAndSweep::FinalizersThreadIsRunning() noexcept {
    return finalizerProcessor_.IsRunning();
}

void gc::SameThreadMarkAndSweep::PerformFullGC(int64_t epoch) noexcept {
    auto gcHandle = GCHandle::create(epoch);

    stopTheWorld(gcHandle);

    auto& scheduler = gcScheduler_;
    scheduler.onGCStart();

    state_.start(epoch);

#ifdef CUSTOM_ALLOCATOR
    // This should really be done by each individual thread while waiting
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        thread.gc().impl().alloc().PrepareForGC();
    }
    heap_.PrepareForGC();
#endif

    gc::collectRootSet<internal::MarkTraits>(gcHandle, markQueue_, [](mm::ThreadData&) { return true; });

    gc::Mark<internal::MarkTraits>(gcHandle, markQueue_);

    gc::processWeaks<DefaultProcessWeaksTraits>(gcHandle, mm::SpecialRefRegistry::instance());

#ifndef CUSTOM_ALLOCATOR
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        thread.gc().PublishObjectFactory();
    }

    // Taking the locks before the pause is completed. So that any destroying thread
    // would not publish into the global state at an unexpected time.
    std::optional extraObjectFactoryIterable = extraObjectDataFactory_.LockForIter();
    std::optional objectFactoryIterable = objectFactory_.LockForIter();

    alloc::SweepExtraObjects<alloc::DefaultSweepTraits<ObjectFactory>>(gcHandle, *extraObjectFactoryIterable);
    extraObjectFactoryIterable = std::nullopt;
    auto finalizerQueue = alloc::Sweep<alloc::DefaultSweepTraits<ObjectFactory>>(gcHandle, *objectFactoryIterable);
    objectFactoryIterable = std::nullopt;
    alloc::compactObjectPoolInMainThread();
#else
    // also sweeps extraObjects
    auto finalizerQueue = heap_.Sweep(gcHandle);
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        finalizerQueue.TransferAllFrom(thread.gc().impl().alloc().ExtractFinalizerQueue());
    }
    finalizerQueue.TransferAllFrom(heap_.ExtractFinalizerQueue());
#endif

    scheduler.onGCFinish(epoch, mm::GlobalData::Instance().gc().GetTotalHeapObjectsSizeBytes());

    resumeTheWorld(gcHandle);

    state_.finish(epoch);
    gcHandle.finalizersScheduled(finalizerQueue.size());
    gcHandle.finished();
    finalizerProcessor_.ScheduleTasks(std::move(finalizerQueue), epoch);
}
