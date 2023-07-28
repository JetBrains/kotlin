/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_NOOP_NOOP_GC_H
#define RUNTIME_GC_NOOP_NOOP_GC_H

#include <cstddef>

#include "AllocatorImpl.hpp"
#include "GC.hpp"
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
    class ThreadData : private Pinned {
    public:
        ThreadData() noexcept {}
        ~ThreadData() = default;

    private:
    };

    NoOpGC() noexcept { RuntimeLogInfo({kTagGC}, "No-op GC initialized"); }
    ~NoOpGC() = default;

#ifdef CUSTOM_ALLOCATOR
    alloc::Heap& heap() noexcept { return heap_; }
#endif

private:
#ifdef CUSTOM_ALLOCATOR
    alloc::Heap heap_;
#endif
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_NOOP_NOOP_GC_H
