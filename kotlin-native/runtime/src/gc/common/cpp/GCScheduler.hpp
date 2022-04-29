/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_COMMON_GC_SCHEDULER_H
#define RUNTIME_GC_COMMON_GC_SCHEDULER_H

#include <atomic>
#include <chrono>
#include <cinttypes>
#include <cstddef>
#include <functional>
#include <utility>

#include "CompilerConstants.hpp"
#include "Logging.hpp"
#include "Types.h"
#include "Utils.hpp"
#include "std_support/Memory.hpp"

namespace kotlin {
namespace gc {

using SchedulerType = compiler::GCSchedulerType;

// NOTE: When changing default values, reflect them in GC.kt as well.
struct GCSchedulerConfig {
    // Only used when useGCTimer() is false. How many regular safepoints will trigger slow path.
    std::atomic<int32_t> threshold = 100000;
    // How many object bytes a thread must allocate to trigger slow path.
    std::atomic<int64_t> allocationThresholdBytes = 10 * 1024;
    std::atomic<bool> autoTune = true;
    // The target interval between collections when Kotlin code is idle. GC will be triggered
    // by timer no sooner than this value and no later than twice this value since the previous collection.
    std::atomic<int64_t> regularGcIntervalMicroseconds = 10 * 1000 * 1000;
    // How many object bytes must be in the heap to trigger collection. Autotunes when autoTune is true.
    std::atomic<int64_t> targetHeapBytes = 1024 * 1024;
    // The rate at which targetHeapBytes changes when autoTune = true. Concretely: if after the collection
    // N object bytes remain in the heap, the next targetHeapBytes will be N / targetHeapUtilization capped
    // between minHeapBytes and maxHeapBytes.
    std::atomic<double> targetHeapUtilization = 0.5;
    // The minimum value of targetHeapBytes for autoTune = true
    std::atomic<int64_t> minHeapBytes = 1024 * 1024;
    // The maximum value of targetHeapBytes for autoTune = true
    std::atomic<int64_t> maxHeapBytes = std::numeric_limits<int64_t>::max();

    std::chrono::microseconds regularGcInterval() const { return std::chrono::microseconds(regularGcIntervalMicroseconds.load()); }
};

class GCSchedulerThreadData;

class GCSchedulerData {
public:
    virtual ~GCSchedulerData() = default;

    // Called by different mutator threads.
    virtual void UpdateFromThreadData(GCSchedulerThreadData& threadData) noexcept = 0;

    // The protocol is: after the scheduler schedules the GC, the GC eventually calls `OnPerformFullGC`
    // when the collection has started, followed by `UpdateAliveSetBytes` when the marking has finished.
    // TODO: Consider returning a sort of future from the scheduleGC, and listen to it instead.

    // Always called by the GC thread.
    virtual void OnPerformFullGC() noexcept = 0;

    // Always called by the GC thread.
    virtual void UpdateAliveSetBytes(size_t bytes) noexcept = 0;
};

class GCSchedulerThreadData {
public:
    static constexpr size_t kFunctionPrologueWeight = 1;
    static constexpr size_t kLoopBodyWeight = 1;

    explicit GCSchedulerThreadData(GCSchedulerConfig& config, std::function<void(GCSchedulerThreadData&)> slowPath) noexcept :
        config_(config), slowPath_(std::move(slowPath)) {
        ClearCountersAndUpdateThresholds();
    }

    // Should be called on encountering a safepoint.
    void OnSafePointRegular(size_t weight) noexcept {
        // TODO: This is a weird design. Consider replacing switch+virtual functions with pimpl+separate compilation.
        switch (compiler::getGCSchedulerType()) {
            case compiler::GCSchedulerType::kOnSafepoints:
            case compiler::GCSchedulerType::kAggressive:
                OnSafePointRegularImpl(weight);
                return;
            default:
                return;
        }
    }

    // Should be called on encountering a safepoint placed by the allocator.
    // TODO: Should this even be a safepoint (i.e. a place, where we suspend)?
    void OnSafePointAllocation(size_t size) noexcept {
        allocatedBytes_ += size;
        if (allocatedBytes_ < allocatedBytesThreshold_) {
            return;
        }
        OnSafePointSlowPath();
    }

    void OnStoppedForGC() noexcept { ClearCountersAndUpdateThresholds(); }

    size_t allocatedBytes() const noexcept { return allocatedBytes_; }

    size_t safePointsCounter() const noexcept { return safePointsCounter_; }

private:
    friend class GCSchedulerThreadDataTestApi;

    void OnSafePointRegularImpl(size_t weight) noexcept {
        safePointsCounter_ += weight;
        if (safePointsCounter_ < safePointsCounterThreshold_) {
            return;
        }
        OnSafePointSlowPath();
    }

    void OnSafePointSlowPath() noexcept {
        slowPath_(*this);
        ClearCountersAndUpdateThresholds();
    }

    void ClearCountersAndUpdateThresholds() noexcept {
        allocatedBytes_ = 0;
        safePointsCounter_ = 0;

        allocatedBytesThreshold_ = config_.allocationThresholdBytes;
        safePointsCounterThreshold_ = config_.threshold;
    }

    GCSchedulerConfig& config_;
    std::function<void(GCSchedulerThreadData&)> slowPath_;

    size_t allocatedBytes_ = 0;
    size_t allocatedBytesThreshold_ = 0;
    size_t safePointsCounter_ = 0;
    size_t safePointsCounterThreshold_ = 0;
};

class GCScheduler : private Pinned {
public:
    GCScheduler() noexcept = default;

    GCSchedulerConfig& config() noexcept { return config_; }
    // Only valid after `SetScheduleGC` is called.
    GCSchedulerData& gcData() noexcept {
        RuntimeAssert(gcData_ != nullptr, "Cannot be called before SetScheduleGC");
        return *gcData_;
    }

    // Can only be called once.
    void SetScheduleGC(std::function<void()> scheduleGC) noexcept;

    GCSchedulerThreadData NewThreadData() noexcept {
        return GCSchedulerThreadData(config_, [this](auto& threadData) { gcData_->UpdateFromThreadData(threadData); });
    }

private:
    GCSchedulerConfig config_;
    std_support::unique_ptr<GCSchedulerData> gcData_;
    std::function<void()> scheduleGC_;
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_COMMON_GC_SCHEDULER_H
