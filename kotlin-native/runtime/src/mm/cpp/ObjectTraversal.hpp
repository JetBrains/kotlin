/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJECT_TRAVERSAL_H
#define RUNTIME_OBJECT_TRAVERSAL_H

#include <type_traits>

#include "Memory.h"
#include "Natives.h"
#include "ReferenceOps.hpp"
#include "Types.h"
#include "ObjectOps.hpp"

namespace kotlin {

// TODO: Consider an iterator/ranges based approaches for traversals.

template <typename F>
ALWAYS_INLINE void traverseClassObjectFields(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<mm::RefFieldAccessor>()))) {
    const TypeInfo* typeInfo = object->type_info();
    RuntimeAssert(typeInfo != theArrayTypeInfo, "Must not be an array of objects");
    for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
        auto fieldPtr = reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(object) + typeInfo->objOffsets_[index]);
        process(mm::RefFieldAccessor(fieldPtr));
    }
}

template <typename F>
ALWAYS_INLINE void traverseArrayOfObjectsElements(ArrayHeader* array, F process) noexcept(noexcept(process(std::declval<mm::RefFieldAccessor>()))) {
    RuntimeAssert(array->type_info() == theArrayTypeInfo, "Must be an array of objects");
    for (uint32_t index = 0; index < array->count_; index++) {
        process(mm::RefFieldAccessor(ArrayAddressOfElementAt(array, index)));
    }
}

template <typename F>
void traverseObjectFields(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<mm::RefFieldAccessor>()))) {
    const TypeInfo* typeInfo = object->type_info();
    // Only consider arrays of objects, not arrays of primitives.
    if (typeInfo != theArrayTypeInfo) {
        for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
            auto fieldPtr = reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(object) + typeInfo->objOffsets_[index]);
            process(mm::RefFieldAccessor(fieldPtr));
        }
    } else {
        ArrayHeader* array = object->array();
        for (uint32_t index = 0; index < array->count_; index++) {
            process(mm::RefFieldAccessor(ArrayAddressOfElementAt(array, index)));
        }
    }
}

// FIXME explicitly mention no barriers
template <typename F>
void traverseReferredObjects(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<ObjHeader*>()))) {
    traverseObjectFields(object, [&process](auto accessor) noexcept(noexcept(process(std::declval<ObjHeader*>()))) {
        if (ObjHeader* ref = accessor.direct()) {
            process(ref);
        }
    });
}

} // namespace kotlin

#endif // RUNTIME_OBJECT_TRAVERSAL_H
