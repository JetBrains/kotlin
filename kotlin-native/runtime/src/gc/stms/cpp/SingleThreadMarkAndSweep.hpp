/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_STMS_SINGLE_THREAD_MARK_AND_SWEEP_H
#define RUNTIME_GC_STMS_SINGLE_THREAD_MARK_AND_SWEEP_H

#include <cstddef>

#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace gc {

// Stop-the-world Mark-and-Sweep for a single mutator
class SingleThreadMarkAndSweep : private Pinned {
public:
    class ObjectData {
    public:
        enum class Color {
            kWhite = 0, // Initial color at the start of collection cycles. Objects with this color at the end of GC cycle are collected.
                        // All new objects are allocated with this color.
            kBlack, // Objects encountered during mark phase.
        };

        Color color() const noexcept { return color_; }
        void setColor(Color color) noexcept { color_ = color; }

    private:
        Color color_ = Color::kWhite;
    };

    class ThreadData : private Pinned {
    public:
        using ObjectData = SingleThreadMarkAndSweep::ObjectData;

        explicit ThreadData(SingleThreadMarkAndSweep& gc) noexcept : gc_(gc) {}
        ~ThreadData() = default;

        void SafePointFunctionEpilogue() noexcept;
        void SafePointLoopBody() noexcept;
        void SafePointExceptionUnwind() noexcept;
        void SafePointAllocation(size_t size) noexcept;

        void PerformFullGC() noexcept;

        void OnOOM(size_t size) noexcept;

    private:
        SingleThreadMarkAndSweep& gc_;
        size_t allocatedBytes_ = 0;
        size_t safePointsCounter_ = 0;
    };

    SingleThreadMarkAndSweep() noexcept {}
    ~SingleThreadMarkAndSweep() = default;

    void SetThreshold(size_t value) noexcept { threshold_ = value; }
    size_t GetThreshold() noexcept { return threshold_; }

    void SetAllocationThresholdBytes(size_t value) noexcept { allocationThresholdBytes_ = value; }
    size_t GetAllocationThresholdBytes() noexcept { return allocationThresholdBytes_; }

    void SetAutoTune(bool value) noexcept { autoTune_ = value; }
    bool GetAutoTune() noexcept { return autoTune_; }

private:
    void PerformFullGC() noexcept;

    bool running_ = false;

    size_t threshold_ = 1000;
    size_t allocationThresholdBytes_ = 10000;
    bool autoTune_ = false;
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_STMS_SINGLE_THREAD_MARK_AND_SWEEP_H
