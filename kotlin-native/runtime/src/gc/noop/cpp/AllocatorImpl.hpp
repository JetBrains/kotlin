/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#ifdef CUSTOM_ALLOCATOR

// TODO: Move into alloc/custom/AllocatorImpl.hpp

#include "CustomAllocator.hpp"
#include "GCApi.hpp"
#include "Heap.hpp"

#else

// TODO: Move into alloc/legacy/AllocatorImpl.hpp

#include "ExtraObjectDataFactory.hpp"
#include "GC.hpp"
#include "ObjectFactory.hpp"
#include "ObjectFactoryAllocator.hpp"

namespace kotlin::gc {

struct ObjectFactoryTraits {
    using Allocator = alloc::AllocatorBasic;
    using ObjectData = gc::GC::ObjectData;

    Allocator CreateAllocator() noexcept { return Allocator(); }
};

using ObjectFactory = alloc::ObjectFactory<ObjectFactoryTraits>;

} // namespace kotlin::gc

#endif
