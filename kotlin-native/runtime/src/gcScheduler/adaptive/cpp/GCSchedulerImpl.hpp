/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"

#include "AppStateTracking.hpp"
#include "GCSchedulerConfig.hpp"
#include "GlobalData.hpp"
#include "HeapGrowthController.hpp"
#include "Logging.hpp"
#include "RegularIntervalPacer.hpp"
#include "RepeatedTimer.hpp"

namespace kotlin::gcScheduler::internal {

template <typename Clock>
class GCSchedulerDataAdaptive : public GCSchedulerData {
public:
    GCSchedulerDataAdaptive(GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept :
        config_(config),
        scheduleGC_(std::move(scheduleGC)),
        appStateTracking_(mm::GlobalData::Instance().appStateTracking()),
        heapGrowthController_(config),
        regularIntervalPacer_(config),
        timer_("GC Timer thread", config_.regularGcInterval(), [this] {
            if (appStateTracking_.state() == mm::AppStateTracking::State::kBackground) {
                return;
            }
            if (regularIntervalPacer_.NeedsGC()) {
                RuntimeLogDebug({kTagGC}, "Scheduling GC by timer");
                scheduleGC_();
            }
        }) {
        RuntimeLogInfo({kTagGC}, "Adaptive GC scheduler initialized");
    }

    void UpdateFromThreadData(GCSchedulerThreadData& threadData) noexcept override {
        heapGrowthController_.OnAllocated(threadData.allocatedBytes());
        if (heapGrowthController_.NeedsGC()) {
            RuntimeLogDebug({kTagGC}, "Scheduling GC by allocation");
            scheduleGC_();
        }
    }

    void OnPerformFullGC() noexcept override {
        heapGrowthController_.OnPerformFullGC();
        regularIntervalPacer_.OnPerformFullGC();
        timer_.restart(config_.regularGcInterval());
    }

    void UpdateAliveSetBytes(size_t bytes) noexcept override { heapGrowthController_.UpdateAliveSetBytes(bytes); }

private:
    GCSchedulerConfig& config_;
    std::function<void()> scheduleGC_;
    mm::AppStateTracking& appStateTracking_;
    HeapGrowthController heapGrowthController_;
    RegularIntervalPacer<Clock> regularIntervalPacer_;
    RepeatedTimer<Clock> timer_;
};

} // namespace kotlin::gcScheduler::internal
