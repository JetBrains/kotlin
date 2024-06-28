/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>

#include "GCSchedulerConfig.hpp"

namespace kotlin::gcScheduler::internal {

template <typename Clock>
class RegularIntervalPacer {
public:
    using TimePoint = std::chrono::time_point<Clock>;

    explicit RegularIntervalPacer(GCSchedulerConfig& config) noexcept : config_(config), lastGC_(Clock::now()) {}

    // Called by the mutators or the timer thread.
    bool NeedsGC() const noexcept {
        auto currentTime = Clock::now();
        return currentTime >= lastGC_.load() + config_.regularGcInterval();
    }

    // Called by the GC thread.
    void OnPerformFullGC() noexcept { lastGC_ = Clock::now(); }

private:
    GCSchedulerConfig& config_;
    // Updated by the GC thread, read by the mutators or the timer thread.
    std::atomic<TimePoint> lastGC_;
};

} // namespace kotlin::gcScheduler::internal
