/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "ConcurrentMarkAndSweep.hpp"

namespace kotlin {
namespace gc {

using GCImpl = ConcurrentMarkAndSweep;

class GC::Impl : private Pinned {
public:
    Impl() noexcept : gc_(objectFactory_, gcScheduler_) {}

    mm::ObjectFactory<gc::GCImpl>& objectFactory() noexcept { return objectFactory_; }
    GCScheduler& gcScheduler() noexcept { return gcScheduler_; }
    GCImpl& gc() noexcept { return gc_; }

private:
    mm::ObjectFactory<gc::GCImpl> objectFactory_;
    GCScheduler gcScheduler_;
    GCImpl gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept :
        gcScheduler_(gc.impl_->gcScheduler().NewThreadData()),
        gc_(gc.impl_->gc(), threadData, gcScheduler_),
        objectFactoryThreadQueue_(gc.impl_->objectFactory(), gc_.CreateAllocator()) {}

    GCSchedulerThreadData& gcScheduler() noexcept { return gcScheduler_; }
    GCImpl::ThreadData& gc() noexcept { return gc_; }
    mm::ObjectFactory<GCImpl>::ThreadQueue& objectFactoryThreadQueue() noexcept { return objectFactoryThreadQueue_; }

private:
    GCSchedulerThreadData gcScheduler_;
    GCImpl::ThreadData gc_;
    mm::ObjectFactory<GCImpl>::ThreadQueue objectFactoryThreadQueue_;
};

} // namespace gc
} // namespace kotlin
