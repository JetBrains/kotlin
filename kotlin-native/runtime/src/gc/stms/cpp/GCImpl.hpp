/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "AllocatorImpl.hpp"
#include "SameThreadMarkAndSweep.hpp"

namespace kotlin {
namespace gc {

class GC::Impl : private Pinned {
public:
    explicit Impl(gcScheduler::GCScheduler& gcScheduler) noexcept : gc_(allocator_, gcScheduler) {}

    alloc::Allocator::Impl& allocator() noexcept { return allocator_; }
    SameThreadMarkAndSweep& gc() noexcept { return gc_; }

private:
    alloc::Allocator::Impl allocator_;
    SameThreadMarkAndSweep gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept : gc_(gc.impl_->gc(), threadData), allocator_(gc.impl_->allocator()) {}

    SameThreadMarkAndSweep::ThreadData& gc() noexcept { return gc_; }
    alloc::Allocator::ThreadData::Impl& allocator() noexcept { return allocator_; }

private:
    SameThreadMarkAndSweep::ThreadData gc_;
    alloc::Allocator::ThreadData::Impl allocator_;
};

} // namespace gc
} // namespace kotlin
