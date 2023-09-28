/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstddef>
#include <cstdint>

#include "GCSchedulerConfig.hpp"

namespace kotlin::gcScheduler::internal {

class HeapGrowthController {
public:
    enum class MemoryBoundary {
        // Memory usage is low.
        kNone,
        // Memory usage is high, GC should be triggered.
        kTrigger,
        // Memory usage is critical, GC is running behind the mutators. Mutators should pause.
        kTarget,
    };

    explicit HeapGrowthController(GCSchedulerConfig& config) noexcept :
        config_(config), targetHeapBytes_(config.targetHeapBytes.load(std::memory_order_relaxed)) {
        triggerHeapBytes_ = targetHeapBytes_ * config_.heapTriggerCoefficient.load(std::memory_order_relaxed);
    }

    // Can be called by any thread.
    MemoryBoundary boundaryForHeapSize(size_t totalAllocatedBytes) noexcept {
        if (totalAllocatedBytes >= targetHeapBytes_) {
            return config_.mutatorAssists() ? MemoryBoundary::kTarget : MemoryBoundary::kTrigger;
        } else if (totalAllocatedBytes >= triggerHeapBytes_) {
            return MemoryBoundary::kTrigger;
        } else {
            return MemoryBoundary::kNone;
        }
    }

    // Called by the GC thread.
    void updateBoundaries(size_t aliveBytes) noexcept {
        if (config_.autoTune.load()) {
            double targetHeapBytes = static_cast<double>(aliveBytes) / config_.targetHeapUtilization;
            if (!std::isfinite(targetHeapBytes)) {
                // This shouldn't happen in practice: targetHeapUtilization is in (0, 1]. But in case it does, don't touch anything.
                return;
            }
            double minHeapBytes = static_cast<double>(config_.minHeapBytes.load(std::memory_order_relaxed));
            double maxHeapBytes = static_cast<double>(config_.maxHeapBytes.load(std::memory_order_relaxed));
            targetHeapBytes = std::min(std::max(targetHeapBytes, minHeapBytes), maxHeapBytes);
            triggerHeapBytes_ = static_cast<size_t>(targetHeapBytes * config_.heapTriggerCoefficient.load(std::memory_order_relaxed));
            config_.targetHeapBytes.store(static_cast<int64_t>(targetHeapBytes), std::memory_order_relaxed);
            targetHeapBytes_ = static_cast<size_t>(targetHeapBytes);
        } else {
            targetHeapBytes_ = config_.targetHeapBytes.load(std::memory_order_relaxed);
        }
    }

    size_t targetHeapBytes() const noexcept { return targetHeapBytes_; }
    size_t triggerHeapBytes() const noexcept { return triggerHeapBytes_; }

private:
    GCSchedulerConfig& config_;
    size_t targetHeapBytes_ = 0;
    size_t triggerHeapBytes_ = 0;
};

} // namespace kotlin::gcScheduler::internal
