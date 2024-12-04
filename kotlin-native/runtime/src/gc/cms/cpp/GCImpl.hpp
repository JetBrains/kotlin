/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "ConcurrentMarkAndSweep.hpp"

namespace kotlin {
namespace gc {

class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler) noexcept :
        gc_(allocator, gcScheduler, compiler::gcMutatorsCooperate(), compiler::auxGCThreads()) {}

    ConcurrentMarkAndSweep& gc() noexcept { return gc_; }

private:
    ConcurrentMarkAndSweep gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept : gc_(gc.impl_->gc(), threadData) {}

    ConcurrentMarkAndSweep::ThreadData& gc() noexcept { return gc_; }

private:
    ConcurrentMarkAndSweep::ThreadData gc_;
};

} // namespace gc
} // namespace kotlin
