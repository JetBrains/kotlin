/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"

#include "Clock.hpp"

#ifndef KONAN_NO_THREADS
#include "RepeatedTimer.hpp"
#endif

namespace kotlin::gc::internal {

class HeapGrowthController {
public:
    explicit HeapGrowthController(gc::GCSchedulerConfig& config) noexcept : config_(config) {}

    // Called by the mutators.
    void OnAllocated(size_t allocatedBytes) noexcept { allocatedBytes_ += allocatedBytes; }

    // Called by the GC thread.
    void OnPerformFullGC() noexcept { allocatedBytes_ = 0; }

    // Called by the GC thread.
    void UpdateAliveSetBytes(size_t bytes) noexcept {
        lastAliveSetBytes_ = bytes;

        if (config_.autoTune.load()) {
            double targetHeapBytes = static_cast<double>(bytes) / config_.targetHeapUtilization;
            if (!std::isfinite(targetHeapBytes)) {
                // This shouldn't happen in practice: targetHeapUtilization is in (0, 1]. But in case it does, don't touch anything.
                return;
            }
            double minHeapBytes = static_cast<double>(config_.minHeapBytes.load());
            double maxHeapBytes = static_cast<double>(config_.maxHeapBytes.load());
            targetHeapBytes = std::min(std::max(targetHeapBytes, minHeapBytes), maxHeapBytes);
            config_.targetHeapBytes = static_cast<int64_t>(targetHeapBytes);
        }
    }

    // Called by the mutators.
    bool NeedsGC() const noexcept {
        uint64_t currentHeapBytes = allocatedBytes_.load() + lastAliveSetBytes_.load();
        uint64_t targetHeapBytes = config_.targetHeapBytes;
        return currentHeapBytes >= targetHeapBytes;
    }

private:
    gc::GCSchedulerConfig& config_;
    // Updated by both the mutators and the GC thread.
    std::atomic<size_t> allocatedBytes_ = 0;
    // Updated by the GC thread, read by the mutators.
    std::atomic<size_t> lastAliveSetBytes_ = 0;
};

template <typename Clock>
class RegularIntervalPacer {
public:
    using TimePoint = std::chrono::time_point<Clock>;

    explicit RegularIntervalPacer(gc::GCSchedulerConfig& config) noexcept : config_(config), lastGC_(Clock::now()) {}

    // Called by the mutators or the timer thread.
    bool NeedsGC() const noexcept {
        auto currentTime = Clock::now();
        return currentTime >= lastGC_.load() + config_.regularGcInterval();
    }

    // Called by the GC thread.
    void OnPerformFullGC() noexcept { lastGC_ = Clock::now(); }

private:
    gc::GCSchedulerConfig& config_;
    // Updated by the GC thread, read by the mutators or the timer thread.
    std::atomic<TimePoint> lastGC_;
};

class GCEmptySchedulerData : public gc::GCSchedulerData {
    void UpdateFromThreadData(gc::GCSchedulerThreadData& threadData) noexcept override {}
    void OnPerformFullGC() noexcept override {}
    void UpdateAliveSetBytes(size_t bytes) noexcept override {}
};

#ifndef KONAN_NO_THREADS

template <typename Clock>
class GCSchedulerDataWithTimer : public gc::GCSchedulerData {
public:
    GCSchedulerDataWithTimer(gc::GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept :
        config_(config),
        heapGrowthController_(config),
        regularIntervalPacer_(config),
        scheduleGC_(std::move(scheduleGC)),
        timer_("GC Timer thread", config_.regularGcInterval(), [this] {
            if (regularIntervalPacer_.NeedsGC()) {
                scheduleGC_();
            }
        }) {}

    void UpdateFromThreadData(gc::GCSchedulerThreadData& threadData) noexcept override {
        heapGrowthController_.OnAllocated(threadData.allocatedBytes());
        if (heapGrowthController_.NeedsGC()) {
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
    gc::GCSchedulerConfig& config_;
    HeapGrowthController heapGrowthController_;
    RegularIntervalPacer<Clock> regularIntervalPacer_;
    std::function<void()> scheduleGC_;
    RepeatedTimer<Clock> timer_;
};

#endif // !KONAN_NO_THREADS

template <typename Clock>
class GCSchedulerDataOnSafepoints : public gc::GCSchedulerData {
public:
    GCSchedulerDataOnSafepoints(gc::GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept :
        heapGrowthController_(config), regularIntervalPacer_(config), scheduleGC_(std::move(scheduleGC)) {}

    void UpdateFromThreadData(gc::GCSchedulerThreadData& threadData) noexcept override {
        heapGrowthController_.OnAllocated(threadData.allocatedBytes());
        if (heapGrowthController_.NeedsGC()) {
            scheduleGC_();
        } else if (regularIntervalPacer_.NeedsGC()) {
            scheduleGC_();
        }
    }

    void OnPerformFullGC() noexcept override {
        heapGrowthController_.OnPerformFullGC();
        regularIntervalPacer_.OnPerformFullGC();
    }

    void UpdateAliveSetBytes(size_t bytes) noexcept override { heapGrowthController_.UpdateAliveSetBytes(bytes); }

private:
    HeapGrowthController heapGrowthController_;
    RegularIntervalPacer<Clock> regularIntervalPacer_;
    std::function<void()> scheduleGC_;
};

class GCSchedulerDataAggressive : public gc::GCSchedulerData {
public:
    GCSchedulerDataAggressive(gc::GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept :
        scheduleGC_(std::move(scheduleGC)) {
        // TODO: Make it even more aggressive and run on a subset of backend.native tests.
        config.threshold = 1000;
        config.allocationThresholdBytes = 10000;
    }

    void UpdateFromThreadData(gc::GCSchedulerThreadData& threadData) noexcept override { scheduleGC_(); }

    void OnPerformFullGC() noexcept override {}
    void UpdateAliveSetBytes(size_t bytes) noexcept override {}

private:
    std::function<void()> scheduleGC_;
};

} // namespace kotlin::gc::internal
