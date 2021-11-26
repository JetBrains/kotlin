/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#include "Memory.h"
#include "../../legacymm/cpp/MemoryPrivate.hpp" // Fine, because this module is a part of legacy MM.

// Note that only C++ part of the runtime goes via those functions, Kotlin uses specialized versions.

extern "C" {

const MemoryModel CurrentMemoryModel = MemoryModel::kStrict;

RUNTIME_NOTHROW OBJ_GETTER(AllocInstance, const TypeInfo* typeInfo) {
  RETURN_RESULT_OF(AllocInstanceStrict, typeInfo);
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* typeInfo, int32_t elements) {
  RETURN_RESULT_OF(AllocArrayInstanceStrict, typeInfo, elements);
}

OBJ_GETTER(InitThreadLocalSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    RETURN_RESULT_OF(InitThreadLocalSingletonStrict, location, typeInfo, ctor);
}

OBJ_GETTER(InitSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    RETURN_RESULT_OF(InitSingletonStrict, location, typeInfo, ctor);
}

RUNTIME_NOTHROW void ReleaseHeapRef(const ObjHeader* object) {
  ReleaseHeapRefStrict(object);
}

RUNTIME_NOTHROW void ReleaseHeapRefNoCollect(const ObjHeader* object) {
  ReleaseHeapRefNoCollectStrict(object);
}

RUNTIME_NOTHROW void SetStackRef(ObjHeader** location, const ObjHeader* object) {
  SetStackRefStrict(location, object);
}

RUNTIME_NOTHROW void SetHeapRef(ObjHeader** location, const ObjHeader* object) {
  SetHeapRefStrict(location, object);
}

RUNTIME_NOTHROW void ZeroStackRef(ObjHeader** location) {
  ZeroStackRefStrict(location);
}

RUNTIME_NOTHROW void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) {
  UpdateHeapRefStrict(location, object);
}

RUNTIME_NOTHROW void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
  UpdateReturnRefStrict(returnSlot, object);
}

RUNTIME_NOTHROW void EnterFrame(ObjHeader** start, int parameters, int count) {
  EnterFrameStrict(start, parameters, count);
}

RUNTIME_NOTHROW void LeaveFrame(ObjHeader** start, int parameters, int count) {
  LeaveFrameStrict(start, parameters, count);
}

RUNTIME_NOTHROW void SetCurrentFrame(ObjHeader** start) {
    SetCurrentFrameStrict(start);
}

RUNTIME_NOTHROW void UpdateStackRef(ObjHeader** location, const ObjHeader* object) {
    UpdateStackRefStrict(location, object);
}

RUNTIME_NOTHROW void UpdateHeapRefsInsideOneArray(const ArrayHeader* array, int fromIndex, int toIndex, int count) {
  UpdateHeapRefsInsideOneArrayStrict(array, fromIndex, toIndex, count);
}

}  // extern "C"
