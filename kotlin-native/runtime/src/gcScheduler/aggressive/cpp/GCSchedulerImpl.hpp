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
#include "SafePoint.hpp"
#include "SafePointTracker.hpp"

namespace kotlin::gcScheduler::internal {

// The slowpath will trigger GC if this thread didn't meet this safepoint/allocation site before.
class GCSchedulerDataAggressive : public GCSchedulerData {
public:
    GCSchedulerDataAggressive(GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept :
        scheduleGC_(std::move(scheduleGC)), heapGrowthController_(config) {
        RuntimeLogInfo({kTagGC}, "Aggressive GC scheduler initialized");
    }

    void OnPerformFullGC() noexcept override { heapGrowthController_.OnPerformFullGC(); }
    void UpdateAliveSetBytes(size_t bytes) noexcept override { heapGrowthController_.UpdateAliveSetBytes(bytes); }
    void SetAllocatedBytes(size_t bytes) noexcept override {
        // Still checking allocations: with a long running loop all safepoints
        // might be "met", so that's the only trigger to not run out of memory.
        if (heapGrowthController_.SetAllocatedBytes(bytes)) {
            RuntimeLogDebug({kTagGC}, "Scheduling GC by allocation");
            scheduleGC_();
        } else {
            safePoint();
        }
    }

    void safePoint() noexcept {
        if (safePointTracker_.registerCurrentSafePoint(1)) {
            RuntimeLogDebug({kTagGC}, "Scheduling GC by safepoint");
            scheduleGC_();
        }
    }

private:
    std::function<void()> scheduleGC_;
    HeapGrowthController heapGrowthController_;
    SafePointTracker<> safePointTracker_;
    mm::SafePointActivator safePointActivator_;
};

} // namespace kotlin::gcScheduler::internal
