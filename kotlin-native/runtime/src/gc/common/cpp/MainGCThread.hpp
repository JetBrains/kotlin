/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Allocator.hpp"
#include "CollectionScope.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "Utils.hpp"
#include "concurrent/UtilityThread.hpp"
#include "GlobalData.hpp"
#include "GCStatistics.hpp"
#include "MarkAndSweepUtils.hpp"

namespace kotlin::gc::internal {

template <typename GCTraits>
class MainGCThread : private MoveOnly {
public:
    MainGCThread(
            GCStateHolder& state,
            alloc::Allocator& allocator,
            gcScheduler::GCScheduler& gcScheduler,
            typename GCTraits::Mark& mark) noexcept :
        state_(state), allocator_(allocator), gcScheduler_(gcScheduler), mark_(mark), thread_(std::string_view("Main GC thread"), [this] {
            body();
        }) {}

private:
    void body() noexcept {
        RuntimeLogInfo({kTagGC}, "Initializing %s GC.", GCTraits::kName);
        while (true) {
            if (auto collection = state_.waitScheduled()) {
                PerformCollection(collection->epoch, collection->scope);
            } else {
                break;
            }
        }
        mark_.requestShutdown();
    }

    // Runs a single collection cycle. For generational collectors `scope` selects an Eden (minor)
    // or Full (major) collection; for non-generational collectors it is always Full. The generational
    // behavior (sticky "old" marks, remembered-set roots, eden-scoped sweep and promotion) is applied
    // through the GCTraits::onCollectionStart/onCollectionFinish hooks below and the scope-aware
    // gc::tryResetMark, so an Eden collection genuinely skips the old generation -- it does not run a
    // full-heap collection like CMS.
    void PerformCollection(int64_t epoch, gc::CollectionScope scope) noexcept {
        if constexpr (!GCTraits::kGenerational) {
            RuntimeAssert(scope == gc::CollectionScope::Full, "Non-generational GC only performs Full collections");
        }
        auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();

        auto gcHandle = GCHandle::create(epoch);

        mark_.setupBeforeSTW(gcHandle);

        stopTheWorld(gcHandle, "GC stop the world: mark");

        gcScheduler_.onGCStart();

        // Select Eden/Full behavior for the mark & sweep phases (no-op for non-generational GCs).
        auto actualScope = GCTraits::onCollectionStart(scope);
        RuntimeLogInfo({kTagGC}, "Performing %s collection", gc::toString(actualScope));

        mark_.markInSTW();

        // TODO outline as mark_.isolateMarkedHeapAndFinishMark()
        // By this point all the alive heap must be marked.
        // All the mutations (incl. allocations) after this method will be subject for the next GC.
        // This should really be done by each individual thread while waiting
        for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
            thread.allocator().prepareForGC();
        }
        allocator_.prepareForGC();

        if (GCTraits::kConcurrentSweep) {
            resumeTheWorld(gcHandle);
        }

        allocator_.sweep(gcHandle);
        auto keptBytes = gcHandle.getKeptSizeBytes();
        gcScheduler_.onGCFinish(epoch, keptBytes);
        // Feed the live-byte result into the generational policy/statistics (no-op for others).
        GCTraits::onCollectionFinish(actualScope, keptBytes);

        if (!GCTraits::kConcurrentSweep) {
            resumeTheWorld(gcHandle);
        }

        state_.finish(epoch);
        gcHandle.finished(actualScope);

        // This may start a new thread. On some pthreads implementations, this may block waiting for concurrent thread
        // destructors running. So, it must ensured that no locks are held by this point.
        // TODO: Consider having an always on sleeping finalizer thread.
        allocator_.scheduleFinalization(gcHandle);
    }

    GCStateHolder& state_;
    alloc::Allocator& allocator_;
    gcScheduler::GCScheduler& gcScheduler_;
    typename GCTraits::Mark& mark_;
    UtilityThread thread_;
};

} // namespace kotlin::gc::internal
