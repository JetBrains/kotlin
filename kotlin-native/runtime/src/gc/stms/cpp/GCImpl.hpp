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
#ifdef CUSTOM_ALLOCATOR
    explicit Impl(gcScheduler::GCScheduler& gcScheduler) noexcept : gc_(gcScheduler) {}
#else
    explicit Impl(gcScheduler::GCScheduler& gcScheduler) noexcept : gc_(objectFactory_, extraObjectDataFactory_, gcScheduler) {}

    ObjectFactory& objectFactory() noexcept { return objectFactory_; }
    mm::ExtraObjectDataFactory& extraObjectDataFactory() noexcept { return extraObjectDataFactory_; }
#endif
    SameThreadMarkAndSweep& gc() noexcept { return gc_; }

private:
#ifndef CUSTOM_ALLOCATOR
    ObjectFactory objectFactory_;
    mm::ExtraObjectDataFactory extraObjectDataFactory_;
#endif
    SameThreadMarkAndSweep gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept :
        gc_(gc.impl_->gc(), threadData),
#ifdef CUSTOM_ALLOCATOR
        alloc_(gc.impl_->gc().heap()) {
    }
#else
        objectFactoryThreadQueue_(gc.impl_->objectFactory(), objectFactoryTraits_.CreateAllocator()),
        extraObjectDataFactoryThreadQueue_(gc.impl_->extraObjectDataFactory()) {
    }
#endif

    SameThreadMarkAndSweep::ThreadData& gc() noexcept { return gc_; }
#ifndef CUSTOM_ALLOCATOR
    ObjectFactory::ThreadQueue& objectFactoryThreadQueue() noexcept { return objectFactoryThreadQueue_; }
    mm::ExtraObjectDataFactory::ThreadQueue& extraObjectDataFactoryThreadQueue() noexcept { return extraObjectDataFactoryThreadQueue_; }
#else
    alloc::CustomAllocator& alloc() noexcept { return alloc_; }
#endif

private:
    SameThreadMarkAndSweep::ThreadData gc_;
#ifdef CUSTOM_ALLOCATOR
    alloc::CustomAllocator alloc_;
#else
    [[no_unique_address]] ObjectFactoryTraits objectFactoryTraits_;
    ObjectFactory::ThreadQueue objectFactoryThreadQueue_;
    mm::ExtraObjectDataFactory::ThreadQueue extraObjectDataFactoryThreadQueue_;
#endif
};

} // namespace gc
} // namespace kotlin
