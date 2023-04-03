/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_NOOP_NOOP_GC_H
#define RUNTIME_GC_NOOP_NOOP_GC_H

#include <cstddef>

#include "Allocator.hpp"
#include "Logging.hpp"
#include "Utils.hpp"

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

    using Allocator = gc::Allocator;

    class ThreadData : private Pinned {
    public:
        using ObjectData = NoOpGC::ObjectData;

        ThreadData() noexcept {}
        ~ThreadData() = default;

        void SafePointAllocation(size_t size) noexcept {}

        void Schedule() noexcept {}
        void ScheduleAndWaitFullGC() noexcept {}
        void ScheduleAndWaitFullGCWithFinalizers() noexcept {}

        void OnOOM(size_t size) noexcept {}

        Allocator CreateAllocator() noexcept { return Allocator(); }

    private:
    };

    NoOpGC() noexcept { RuntimeLogInfo({kTagGC}, "No-op GC initialized"); }
    ~NoOpGC() = default;

private:
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_NOOP_NOOP_GC_H
