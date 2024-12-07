/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "ParallelMarkConcurrentSweep.hpp"

namespace kotlin {
namespace gc {

class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler) noexcept :
        gc_(allocator, gcScheduler, compiler::gcMutatorsCooperate(), compiler::auxGCThreads()) {}

    ParallelMarkConcurrentSweep& gc() noexcept { return gc_; }

private:
    ParallelMarkConcurrentSweep gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept : gc_(gc.impl_->gc(), threadData) {}

    ParallelMarkConcurrentSweep::ThreadData& gc() noexcept { return gc_; }

private:
    ParallelMarkConcurrentSweep::ThreadData gc_;
};

namespace barriers {
class SpecialRefReleaseGuard::Impl {};
}

} // namespace gc
} // namespace kotlin
