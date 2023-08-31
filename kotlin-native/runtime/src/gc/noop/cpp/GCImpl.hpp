/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "AllocatorImpl.hpp"
#include "Logging.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace gc {

class GC::Impl : private Pinned {
public:
    Impl() noexcept { RuntimeLogInfo({kTagGC}, "No-op GC initialized"); }

    alloc::Allocator::Impl& allocator() noexcept { return allocator_; }

private:
    alloc::Allocator::Impl allocator_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept : allocator_(gc.impl_->allocator()) {}

    alloc::Allocator::ThreadData::Impl& allocator() noexcept { return allocator_; }

private:
    alloc::Allocator::ThreadData::Impl allocator_;
};

} // namespace gc
} // namespace kotlin
