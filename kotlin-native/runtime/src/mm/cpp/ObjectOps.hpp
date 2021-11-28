/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_OBJECT_OPS_H
#define RUNTIME_MM_OBJECT_OPS_H

#include "Memory.h"

namespace kotlin {
namespace mm {

class ThreadData;

// TODO: Make sure these operations work with any kind of thread stopping: safepoints and signals.

// TODO: Consider adding some kind of an `Object` type (that wraps `ObjHeader*`) which
//       will have these operations for a friendlier API.

// TODO: `OBJ_GETTER` is used because the returned objects needs to be accessible via the rootset before the function
//       returns. If we had a different way to efficiently keep the object in the roots, `OBJ_GETTER` can be removed.

void SetStackRef(ObjHeader** location, ObjHeader* value) noexcept;
void SetHeapRef(ObjHeader** location, ObjHeader* value) noexcept;
void SetHeapRefAtomic(ObjHeader** location, ObjHeader* value) noexcept;
OBJ_GETTER(ReadHeapRefAtomic, ObjHeader** location) noexcept;
OBJ_GETTER(CompareAndSwapHeapRef, ObjHeader** location, ObjHeader* expected, ObjHeader* value) noexcept;
OBJ_GETTER(AllocateObject, ThreadData* threadData, const TypeInfo* typeInfo) noexcept;
OBJ_GETTER(AllocateArray, ThreadData* threadData, const TypeInfo* typeInfo, uint32_t elements) noexcept;

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_OBJECT_OPS_H
