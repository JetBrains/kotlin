/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"

#include <functional>

#include "GCSchedulerConfig.hpp"
#include "HeapGrowthController.hpp"
#include "Logging.hpp"
#include "MutatorAssists.hpp"
#include "SafePoint.hpp"
#include "SafePointTracker.hpp"
#include "EpochScheduler.hpp"
#include "ThreadData.hpp"

namespace kotlin::gcScheduler {

namespace internal {
class GCSchedulerDataAggressive;
}

class GCScheduler::ThreadData::Impl : private Pinned {
public:
    Impl(GCScheduler& scheduler, mm::ThreadData& thread) noexcept;

    internal::GCSchedulerDataAggressive& scheduler() noexcept { return scheduler_; }

    internal::MutatorAssists::ThreadData& mutatorAssists() noexcept { return mutatorAssists_; }

private:
    internal::GCSchedulerDataAggressive& scheduler_;
    internal::MutatorAssists::ThreadData mutatorAssists_;
};

namespace internal {

// The slowpath will trigger GC if this thread didn't meet this safepoint/allocation site before.
class GCSchedulerDataAggressive {
public:
    GCSchedulerDataAggressive(GCSchedulerConfig& config, std::function<int64_t()> scheduleGC) noexcept :
        scheduleGC_(std::move(scheduleGC)), heapGrowthController_(config) {
        RuntimeLogInfo({kTagGC}, "Aggressive GC scheduler initialized");
    }

    void setAllocatedBytes(size_t bytes) noexcept {
        // Still checking allocations: with a long running loop all safepoints
        // might be "met", so that's the only trigger to not run out of memory.
        auto boundary = heapGrowthController_.boundaryForHeapSize(bytes);
        switch (boundary) {
            case HeapGrowthController::MemoryBoundary::kNone:
                safePoint();
                return;
            case HeapGrowthController::MemoryBoundary::kTrigger:
                RuntimeLogDebug({kTagGC}, "Scheduling GC by allocation");
                scheduleGC_.scheduleNextEpochIfNotInProgress();
                return;
            case HeapGrowthController::MemoryBoundary::kTarget:
                RuntimeLogDebug({kTagGC}, "Scheduling GC by allocation");
                auto epoch = scheduleGC_.scheduleNextEpochIfNotInProgress();
                RuntimeLogWarning({kTagGC}, "Pausing the mutators");
                mutatorAssists_.requestAssists(epoch);
                return;
        }
    }

    void safePoint() noexcept {
        if (safePointTracker_.registerCurrentSafePoint(1)) {
            RuntimeLogDebug({kTagGC}, "Scheduling GC by safepoint");
            schedule();
        }
    }

    void onGCFinish(int64_t epoch, size_t aliveBytes) noexcept {
        scheduleGC_.onGCFinish(epoch);
        heapGrowthController_.updateBoundaries(aliveBytes);
        // Must wait for all mutators to be released. GC thread cannot continue.
        // This is the contract between GC and mutators. With regular native state
        // each mutator must check that GC is not doing something. Here GC must check
        // that each mutator has done all it needs.
        mutatorAssists_.completeEpoch(epoch, [](mm::ThreadData& threadData) noexcept -> MutatorAssists::ThreadData& {
            return threadData.gcScheduler().impl().mutatorAssists();
        });
    }

    int64_t schedule() noexcept { return scheduleGC_.scheduleNextEpoch(); }

    MutatorAssists& mutatorAssists() noexcept { return mutatorAssists_; }

private:
    EpochScheduler scheduleGC_;
    HeapGrowthController heapGrowthController_;
    SafePointTracker<> safePointTracker_;
    mm::SafePointActivator safePointActivator_;
    MutatorAssists mutatorAssists_;
};

} // namespace internal

class GCScheduler::Impl : private Pinned {
public:
    explicit Impl(GCSchedulerConfig& config) noexcept;

    internal::GCSchedulerDataAggressive& impl() noexcept { return impl_; }

private:
    internal::GCSchedulerDataAggressive impl_;
};

} // namespace kotlin::gcScheduler
