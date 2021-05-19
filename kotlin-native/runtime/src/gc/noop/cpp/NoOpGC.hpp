/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_NOOP_NOOP_GC_H
#define RUNTIME_GC_NOOP_NOOP_GC_H

#include <cstddef>

#include "Utils.hpp"

namespace kotlin {
namespace gc {

// No-op GC is a GC that does not free memory.
// TODO: It can be made more efficient.
class NoOpGC : private Pinned {
public:
    class ObjectData {};

    class ThreadData : private Pinned {
    public:
        using ObjectData = NoOpGC::ObjectData;

        explicit ThreadData(NoOpGC& gc) noexcept {}
        ~ThreadData() = default;

        void SafePointFunctionEpilogue() noexcept {}
        void SafePointLoopBody() noexcept {}
        void SafePointExceptionUnwind() noexcept {}
        void SafePointAllocation(size_t size) noexcept {}

        void PerformFullGC() noexcept {}

        void OnOOM(size_t size) noexcept {}

    private:
    };

    NoOpGC() noexcept {}
    ~NoOpGC() = default;

    void SetThreshold(size_t value) noexcept { threshold_ = value; }
    size_t GetThreshold() noexcept { return threshold_; }

    void SetAllocationThresholdBytes(size_t value) noexcept { allocationThresholdBytes_ = value; }
    size_t GetAllocationThresholdBytes() noexcept { return allocationThresholdBytes_; }

    void SetAutoTune(bool value) noexcept { autoTune_ = value; }
    bool GetAutoTune() noexcept { return autoTune_; }

private:
    size_t threshold_ = 0;
    size_t allocationThresholdBytes_ = 0;
    bool autoTune_ = false;
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_NOOP_NOOP_GC_H
