/*
 * Copyright 2022-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_GCAPI_HPP_
#define CUSTOM_ALLOC_CPP_GCAPI_HPP_

#include <cstdint>
#include <cstdlib>

#include "Alignment.hpp"
#include "AllocationSize.hpp"
#include "AtomicStack.hpp"
#include "CustomFinalizerProcessor.hpp"
#include "ExtraObjectData.hpp"
#include "GC.hpp"
#include "GCStatistics.hpp"
#include "HeapObject.hpp"
#include "Memory.h"
#include "TypeLayout.hpp"

namespace kotlin::alloc {

using CustomHeapObject = HeapObject<gc::GC::ObjectData>;
using CustomHeapArray = HeapArray<gc::GC::ObjectData>;

struct ObjectSweepTraits {
    using GCSweepScope = gc::GCHandle::GCSweepScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweep(); }

    static bool trySweepElement(uint8_t* data, FinalizerQueue& finalizerQueue, GCSweepScope& sweepScope) noexcept;

    static AllocationSize elementSize(uint8_t* data);
};

struct ExtraDataSweepTraits {
    using GCSweepScope = gc::GCHandle::GCSweepExtraObjectsScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweepExtraObjects(); }

    static bool trySweepElement(uint8_t* data, FinalizerQueue& finalizerQueue, GCSweepScope& sweepScope) noexcept;

    static AllocationSize elementSize(uint8_t*);
};

void* SafeAlloc(uint64_t size) noexcept;

void Free(void* ptr, size_t size) noexcept;

size_t GetAllocatedBytes() noexcept;

} // namespace kotlin::alloc

#endif
