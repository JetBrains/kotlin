/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_NOOP_NOOP_GC_H
#define RUNTIME_GC_NOOP_NOOP_GC_H

#include <cstddef>

#include "GCScheduler.hpp"
#include "Utils.hpp"
#include "Types.h"

namespace kotlin {

namespace mm {
class ThreadData;
}

namespace gc {

// No-op GC is a GC that does not free memory.
// TODO: It can be made more efficient.
class NoOpGC : private Pinned {
public:
    class ObjectData {};

    class ThreadData : private Pinned {
    public:
        using ObjectData = NoOpGC::ObjectData;

        explicit ThreadData(NoOpGC& gc, mm::ThreadData& threadData) noexcept {}
        ~ThreadData() = default;

        void SafePointFunctionPrologue() noexcept {}
        void SafePointLoopBody() noexcept {}
        void SafePointAllocation(size_t size) noexcept {}

        void PerformFullGC() noexcept {}

        void OnOOM(size_t size) noexcept {}

    private:
    };

    NoOpGC() noexcept {}
    ~NoOpGC() = default;

    GCScheduler& scheduler() noexcept { return scheduler_; }

private:
    GCScheduler scheduler_;
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_NOOP_NOOP_GC_H
