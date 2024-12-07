/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "KObject.hpp"

namespace kotlin::alloc {

template <typename HeapHeader>
struct HeapObject {
    using descriptor = type_layout::Composite<HeapObject, HeapHeader, KObject>;

    static descriptor descriptorFrom(const TypeInfo* typeInfo) noexcept {
        return descriptor{{}, type_layout::descriptor_t<KObject>{typeInfo}};
    }

    static HeapObject& from(HeapHeader& heapHeader) noexcept { return *descriptorFrom(nullptr).template fromField<0>(&heapHeader); }

    static HeapObject& from(ObjHeader* object) noexcept {
        RuntimeAssert(object->heap(), "Object %p does not reside in the heap", object);
        return *descriptorFrom(nullptr).template fromField<1>(KObject::from(object));
    }

    HeapHeader& heapHeader() noexcept { return *descriptorFrom(nullptr).template field<0>(this).second; }

    ObjHeader* object() noexcept { return descriptorFrom(nullptr).template field<1>(this).second->header(); }

    size_t size() noexcept;

private:
    HeapObject() = delete;
    ~HeapObject() = delete;
};

// Every `HeapArray<Header>` is also a `HeapObject<Header>`.
template <typename HeapHeader>
struct HeapArray {
    using descriptor = type_layout::Composite<HeapArray, HeapHeader, KArray>;

    static descriptor descriptorFrom(const TypeInfo* typeInfo, uint32_t size) noexcept {
        return descriptor{{}, type_layout::descriptor_t<KArray>{typeInfo, size}};
    }

    static HeapArray& from(HeapHeader& heapHeader) noexcept { return *descriptorFrom(nullptr, 0).template fromField<0>(&heapHeader); }

    static HeapArray& from(ArrayHeader* array) noexcept {
        RuntimeAssert(array->obj()->heap(), "Array %p does not reside in the heap", array);
        return *descriptorFrom(nullptr, 0).template fromField<1>(KArray::from(array));
    }

    HeapHeader& heapHeader() noexcept { return *descriptorFrom(nullptr).template field<0>(this).second; }

    ArrayHeader* array() noexcept { return descriptorFrom(nullptr, 0).template field<1>(this).second->header(); }

    operator HeapObject<HeapHeader>&() noexcept { return reinterpret_cast<HeapObject<HeapHeader>&>(*this); }

private:
    HeapArray() = delete;
    ~HeapArray() = delete;
};

template <typename HeapHeader>
size_t HeapObject<HeapHeader>::size() noexcept {
    const auto* obj = object();
    const auto* typeInfo = obj->type_info();
    if (!typeInfo->IsArray())
        return descriptorFrom(typeInfo).size();
    return HeapArray<HeapHeader>::descriptorFrom(typeInfo, obj->array()->count_).size();
}

} // namespace kotlin::alloc
