/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_GCAPI_HPP_
#define CUSTOM_ALLOC_CPP_GCAPI_HPP_

#include <cstdint>
#include <inttypes.h>
#include <limits>
#include <stdlib.h>

#include "AtomicStack.hpp"
#include "ConcurrentMarkAndSweep.hpp"
#include "ExtraObjectData.hpp"
#include "ExtraObjectPage.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

// copied over from ObjectFactory

using ObjectData = gc::ConcurrentMarkAndSweep::ObjectData;

struct HeapObjHeader {
    ObjectData gcData;
    alignas(kObjectAlignment) ObjHeader object;
};

struct HeapArrayHeader {
    ObjectData gcData;
    alignas(kObjectAlignment) ArrayHeader array;
};

// Returns `true` if the `object` must be kept alive still.
bool SweepObject(uint8_t* object, FinalizerQueue& finalizerQueue, gc::GCHandle::GCSweepScope& sweepScope) noexcept;

// Returns `true` if the `extraObject` must be kept alive still
bool SweepExtraObject(mm::ExtraObjectData* extraObject, gc::GCHandle::GCSweepExtraObjectsScope& sweepScope) noexcept;

void* SafeAlloc(uint64_t size) noexcept;

void Free(void* ptr, size_t size) noexcept;

size_t GetAllocatedBytes() noexcept;

} // namespace kotlin::alloc

#endif
