/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>

#include "GCThread.hpp"

#include "Allocator.hpp"
#include "Barriers.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ParallelMark.hpp"

using namespace kotlin;

namespace {

template <typename Body>
UtilityThread createGCThread(const char* name, Body&& body) {
    return UtilityThread(std::string_view(name), [name, body] {
        RuntimeLogDebug({kTagGC}, "%s %" PRIuPTR " starts execution", name, konan::currentThreadId());
        body();
        RuntimeLogDebug({kTagGC}, "%s %" PRIuPTR " finishes execution", name, konan::currentThreadId());
    });
}

} // namespace

gc::internal::MainGCThread::MainGCThread(
        GCStateHolder& state,
        mark::ParallelMark& markDispatcher,
        alloc::Allocator& allocator,
        gcScheduler::GCScheduler& gcScheduler) noexcept :
    state_(state),
    markDispatcher_(markDispatcher),
    allocator_(allocator),
    gcScheduler_(gcScheduler),
    thread_(createGCThread("Main GC Thread", [this] { body(); })) {}

void gc::internal::MainGCThread::body() noexcept {
    while (true) {
        if (auto epoch = state_.waitScheduled()) {
            PerformFullGC(*epoch);
        } else {
            break;
        }
    }
    markDispatcher_.requestShutdown();
}

void gc::internal::MainGCThread::PerformFullGC(int64_t epoch) noexcept {
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();

    auto gcHandle = GCHandle::create(epoch);

    markDispatcher_.beginMarkingEpoch(gcHandle);
    GCLogDebug(epoch, "Main GC requested marking in mutators");

    stopTheWorld(gcHandle, "GC stop the world #1: mark");

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

    gc::processWeaks<DefaultProcessWeaksTraits>(gcHandle, mm::ExternalRCRefRegistry::instance());

    if (compiler::concurrentWeakSweep()) {
        stopTheWorld(gcHandle, "GC stop the world #2: prepare heap for sweep");
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

    resumeTheWorld(gcHandle);

    allocator_.sweep(gcHandle);
    scheduler.onGCFinish(epoch, gcHandle.getKeptSizeBytes());
    state_.finish(epoch);
    gcHandle.finished();

    // This may start a new thread. On some pthreads implementations, this may block waiting for concurrent thread
    // destructors running. So, it must ensured that no locks are held by this point.
    // TODO: Consider having an always on sleeping finalizer thread.
    allocator_.scheduleFinalization(gcHandle);
}

gc::internal::AuxiliaryGCThreads::AuxiliaryGCThreads(mark::ParallelMark& markDispatcher, size_t count) noexcept :
    markDispatcher_(markDispatcher) {
    startThreads(count);
}

void gc::internal::AuxiliaryGCThreads::stopThreads() noexcept {
    threads_.clear();
}

void gc::internal::AuxiliaryGCThreads::startThreads(size_t count) noexcept {
    RuntimeAssert(threads_.empty(), "Auxiliary threads must have been cleared");
    for (size_t i = 0; i < count; ++i) {
        threads_.emplace_back(createGCThread("Auxiliary GC thread", [this] { body(); }));
    }
}

void gc::internal::AuxiliaryGCThreads::body() noexcept {
    RuntimeAssert(!compiler::gcMarkSingleThreaded(), "Should not reach here during single threaded mark");
    while (!markDispatcher_.shutdownRequested()) {
        markDispatcher_.runAuxiliary();
    }
}
