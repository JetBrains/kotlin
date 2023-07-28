/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "AllocatorImpl.hpp"
#include "NoOpGC.hpp"

namespace kotlin {
namespace gc {

class GC::Impl : private Pinned {
public:
    Impl() noexcept = default;

#ifndef CUSTOM_ALLOCATOR
    ObjectFactory& objectFactory() noexcept { return objectFactory_; }
    mm::ExtraObjectDataFactory& extraObjectDataFactory() noexcept { return extraObjectDataFactory_; }
#endif
    NoOpGC& gc() noexcept { return gc_; }

private:
#ifndef CUSTOM_ALLOCATOR
    ObjectFactory objectFactory_;
    mm::ExtraObjectDataFactory extraObjectDataFactory_;
#endif
    NoOpGC gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
#ifdef CUSTOM_ALLOCATOR
    Impl(GC& gc, mm::ThreadData& threadData) noexcept : alloc_(gc.impl_->gc().heap()) {}
#else
    Impl(GC& gc, mm::ThreadData& threadData) noexcept :
        objectFactoryThreadQueue_(gc.impl_->objectFactory(), objectFactoryTraits_.CreateAllocator()),
        extraObjectDataFactoryThreadQueue_(gc.impl_->extraObjectDataFactory()) {}
#endif

    NoOpGC::ThreadData& gc() noexcept { return gc_; }
#ifdef CUSTOM_ALLOCATOR
    alloc::CustomAllocator& alloc() noexcept { return alloc_; }
#else
    ObjectFactory::ThreadQueue& objectFactoryThreadQueue() noexcept { return objectFactoryThreadQueue_; }
    mm::ExtraObjectDataFactory::ThreadQueue& extraObjectDataFactoryThreadQueue() noexcept { return extraObjectDataFactoryThreadQueue_; }
#endif

private:
    NoOpGC::ThreadData gc_;
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
