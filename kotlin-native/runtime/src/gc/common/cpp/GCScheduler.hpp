/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_COMMON_GC_SCHEDULER_H
#define RUNTIME_GC_COMMON_GC_SCHEDULER_H

#include <atomic>
#include <cinttypes>
#include <cstddef>
#include <functional>
#include <utility>

#include "CompilerConstants.hpp"
#include "Logging.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace gc {

namespace internal {

inline bool useGCTimer() noexcept {
#if KONAN_NO_THREADS
    return false;
#else
    // With aggressive mode we use safepoint counting to drive GC.
    return !compiler::gcAggressive();
#endif
}

} // namespace internal

struct GCSchedulerConfig {
    std::atomic<size_t> threshold = 100000; // Roughly 1 safepoint per 10ms (on a subset of examples on one particular machine).
    std::atomic<size_t> allocationThresholdBytes = 10 * 1024 * 1024; // 10MiB by default.
    std::atomic<uint64_t> cooldownThresholdNs = 200 * 1000 * 1000; // 200 milliseconds by default.
    std::atomic<bool> autoTune = false;
    std::atomic<uint64_t> regularGcIntervalUs = 200 * 1000; // 200 milliseconds by default.

    GCSchedulerConfig() noexcept {
        if (compiler::gcAggressive()) {
            // TODO: Make a separate GCSchedulerData for the aggressive mode and move this log there.
            RuntimeLogInfo({kTagGC}, "Initialize GC scheduler config in the aggressive mode");
            // TODO: Make it even more aggressive and run on a subset of backend.native tests.
            threshold = 1000;
            allocationThresholdBytes = 10000;
            cooldownThresholdNs = 0;
        }
    }
};

class GCSchedulerThreadData;

class GCSchedulerData {
public:
    virtual ~GCSchedulerData() = default;

    // Called by different mutator threads.
    virtual void OnSafePoint(GCSchedulerThreadData& threadData) noexcept = 0;

    // Always called by the GC thread.
    virtual void OnPerformFullGC() noexcept = 0;
};

class GCSchedulerThreadData {
public:
    static constexpr size_t kFunctionPrologueWeight = 1;
    static constexpr size_t kLoopBodyWeight = 1;

    explicit GCSchedulerThreadData(GCSchedulerConfig& config, std::function<void(GCSchedulerThreadData&)> onSafePoint) noexcept :
        config_(config), onSafePoint_(std::move(onSafePoint)) {
        ClearCountersAndUpdateThresholds();
    }

    // Should be called on encountering a safepoint.
    void OnSafePointRegular(size_t weight) noexcept {
        if (!internal::useGCTimer()) {
            safePointsCounter_ += weight;
            if (safePointsCounter_ < safePointsCounterThreshold_) {
                return;
            }
            OnSafePointSlowPath();
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
    void OnSafePointSlowPath() noexcept {
        onSafePoint_(*this);
        ClearCountersAndUpdateThresholds();
    }

    void ClearCountersAndUpdateThresholds() noexcept {
        allocatedBytes_ = 0;
        safePointsCounter_ = 0;

        allocatedBytesThreshold_ = config_.allocationThresholdBytes;
        safePointsCounterThreshold_ = config_.threshold;
    }

    GCSchedulerConfig& config_;
    std::function<void(GCSchedulerThreadData&)> onSafePoint_;

    size_t allocatedBytes_ = 0;
    size_t allocatedBytesThreshold_ = 0;
    size_t safePointsCounter_ = 0;
    size_t safePointsCounterThreshold_ = 0;
};

namespace internal {

KStdUniquePtr<GCSchedulerData> MakeEmptyGCSchedulerData() noexcept;
KStdUniquePtr<GCSchedulerData> MakeGCSchedulerDataWithTimer(GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept;
KStdUniquePtr<GCSchedulerData> MakeGCSchedulerDataWithoutTimer(
        GCSchedulerConfig& config, std::function<void()> scheduleGC, std::function<uint64_t()> currentTimeCallbackNs) noexcept;
KStdUniquePtr<GCSchedulerData> MakeGCSchedulerData(GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept;

} // namespace internal

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
        return GCSchedulerThreadData(config_, [this](auto& threadData) { gcData_->OnSafePoint(threadData); });
    }

    template <typename F>
    KStdUniquePtr<GCSchedulerData> ReplaceGCSchedulerDataForTests(F&& factory) noexcept {
        RuntimeAssert(static_cast<bool>(scheduleGC_), "Can only be called after SetScheduleGC");

        auto other = std::forward<F>(factory)(config_, scheduleGC_);
        RuntimeAssert(other != nullptr, "factory cannot return a null");
        using std::swap;
        swap(gcData_, other);
        return other;
    }

private:
    GCSchedulerConfig config_;
    KStdUniquePtr<GCSchedulerData> gcData_;
    std::function<void()> scheduleGC_;
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_COMMON_GC_SCHEDULER_H
