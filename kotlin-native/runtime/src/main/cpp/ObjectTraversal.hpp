/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJECT_TRAVERSAL_H
#define RUNTIME_OBJECT_TRAVERSAL_H

#include <type_traits>

#include "Memory.h"
#include "Natives.h"
#include "Types.h"

namespace kotlin {

// TODO: Consider an iterator/ranges based approaches for traversals.

template <typename F>
void traverseObjectFields(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<ObjHeader**>()))) {
    const TypeInfo* typeInfo = object->type_info();
    // Only consider arrays of objects, not arrays of primitives.
    if (typeInfo != theArrayTypeInfo) {
        for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
            process(reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(object) + typeInfo->objOffsets_[index]));
        }
    } else {
        ArrayHeader* array = object->array();
        for (uint32_t index = 0; index < array->count_; index++) {
            process(ArrayAddressOfElementAt(array, index));
        }
    }
}

template <typename F>
void traverseReferredObjects(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<ObjHeader*>()))) {
    traverseObjectFields(object, [&process](ObjHeader** location) noexcept(noexcept(process(std::declval<ObjHeader*>()))) {
        if (ObjHeader* ref = *location) {
            process(ref);
        }
    });
}

} // namespace kotlin

#endif // RUNTIME_OBJECT_TRAVERSAL_H
