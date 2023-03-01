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
    explicit HeapGrowthController(GCSchedulerConfig& config) noexcept : config_(config) {}

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
    GCSchedulerConfig& config_;
    // Updated by both the mutators and the GC thread.
    std::atomic<size_t> allocatedBytes_ = 0;
    // Updated by the GC thread, read by the mutators.
    std::atomic<size_t> lastAliveSetBytes_ = 0;
};

} // namespace kotlin::gcScheduler::internal
