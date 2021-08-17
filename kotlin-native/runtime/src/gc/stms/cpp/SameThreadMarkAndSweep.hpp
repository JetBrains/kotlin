/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H
#define RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H

#include <cstddef>

#include "ObjectFactory.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {

namespace mm {
class ThreadData;
}

namespace gc {

// Stop-the-world Mark-and-Sweep that runs on mutator threads. Can support targets that do not have threads.
class SameThreadMarkAndSweep : private Pinned {
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
        using ObjectData = SameThreadMarkAndSweep::ObjectData;

        explicit ThreadData(SameThreadMarkAndSweep& gc, mm::ThreadData& threadData) noexcept : gc_(gc), threadData_(threadData) {}
        ~ThreadData() = default;

        void SafePointFunctionEpilogue() noexcept;
        void SafePointLoopBody() noexcept;
        void SafePointExceptionUnwind() noexcept;
        void SafePointAllocation(size_t size) noexcept;

        void PerformFullGC() noexcept;

        void OnOOM(size_t size) noexcept;

    private:
        void SafePointRegular(size_t weight) noexcept;
        void SafePointRegularSlowPath() noexcept;

        SameThreadMarkAndSweep& gc_;
        mm::ThreadData& threadData_;
        size_t allocatedBytes_ = 0;
        size_t safePointsCounter_ = 0;
        uint64_t timeOfLastGcUs_ = konan::getTimeMicros();
    };

    SameThreadMarkAndSweep() noexcept;
    ~SameThreadMarkAndSweep() = default;

    void SetThreshold(size_t value) noexcept { threshold_ = value; }
    size_t GetThreshold() noexcept { return threshold_; }

    void SetAllocationThresholdBytes(size_t value) noexcept { allocationThresholdBytes_ = value; }
    size_t GetAllocationThresholdBytes() noexcept { return allocationThresholdBytes_; }

    void SetCooldownThresholdUs(uint64_t value) noexcept { cooldownThresholdUs_ = value; }
    uint64_t GetCooldownThresholdUs() noexcept { return cooldownThresholdUs_; }

    void SetAutoTune(bool value) noexcept { autoTune_ = value; }
    bool GetAutoTune() noexcept { return autoTune_; }

private:
    mm::ObjectFactory<SameThreadMarkAndSweep>::FinalizerQueue PerformFullGC() noexcept;

    size_t threshold_ = 100000;  // Roughly 1 safepoint per 10ms (on a subset of examples on one particular machine).
    size_t allocationThresholdBytes_ = 10 * 1024 * 1024;  // 10MiB by default.
    uint64_t cooldownThresholdUs_ = 200 * 1000; // 200 milliseconds by default.
    bool autoTune_ = false;
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H
