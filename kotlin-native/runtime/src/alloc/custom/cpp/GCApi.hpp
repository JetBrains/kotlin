/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_GCAPI_HPP_
#define CUSTOM_ALLOC_CPP_GCAPI_HPP_

#include <cinttypes>
#include <cstdint>
#include <cstdlib>
#include <limits>

#include "Alignment.hpp"
#include "AtomicStack.hpp"
#include "ExtraObjectData.hpp"
#include "ExtraObjectPage.hpp"
#include "GC.hpp"
#include "GCStatistics.hpp"
#include "Memory.h"
#include "TypeLayout.hpp"

namespace kotlin::alloc {

struct HeapObjHeader {
    using descriptor = type_layout::Composite<HeapObjHeader, gc::GC::ObjectData, ObjHeader>;

    static HeapObjHeader& from(gc::GC::ObjectData& objectData) noexcept { return *descriptor().fromField<0>(&objectData); }

    static HeapObjHeader& from(ObjHeader* object) noexcept { return *descriptor().fromField<1>(object); }

    gc::GC::ObjectData& objectData() noexcept { return *descriptor().field<0>(this).second; }

    ObjHeader* object() noexcept { return descriptor().field<1>(this).second; }

private:
    HeapObjHeader() = delete;
    ~HeapObjHeader() = delete;
};

struct HeapObject {
    using descriptor = type_layout::Composite<HeapObject, HeapObjHeader, ObjectBody>;

    static descriptor make_descriptor(const TypeInfo* typeInfo) noexcept {
        return descriptor{{}, type_layout::descriptor_t<ObjectBody>{typeInfo}};
    }

    HeapObjHeader& header(descriptor descriptor) noexcept { return *descriptor.field<0>(this).second; }

private:
    HeapObject() = delete;
    ~HeapObject() = delete;
};

// Needs to be kept compatible with `HeapObjHeader` just like `ArrayHeader` is compatible
// with `ObjHeader`: the former can always be casted to the other.
struct HeapArrayHeader {
    using descriptor = type_layout::Composite<HeapArrayHeader, gc::GC::ObjectData, ArrayHeader>;

    static HeapArrayHeader& from(gc::GC::ObjectData& objectData) noexcept { return *descriptor().fromField<0>(&objectData); }

    static HeapArrayHeader& from(ArrayHeader* array) noexcept { return *descriptor().fromField<1>(array); }

    gc::GC::ObjectData& objectData() noexcept { return *descriptor().field<0>(this).second; }

    ArrayHeader* array() noexcept { return descriptor().field<1>(this).second; }

private:
    HeapArrayHeader() = delete;
    ~HeapArrayHeader() = delete;
};

struct HeapArray {
    using descriptor = type_layout::Composite<HeapArray, HeapArrayHeader, ArrayBody>;

    static descriptor make_descriptor(const TypeInfo* typeInfo, uint32_t size) noexcept {
        return descriptor{{}, type_layout::descriptor_t<ArrayBody>{typeInfo, size}};
    }

    HeapArrayHeader& header(descriptor descriptor) noexcept { return *descriptor.field<0>(this).second; }

private:
    HeapArray() = delete;
    ~HeapArray() = delete;
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
