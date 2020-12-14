/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#include "Memory.h"
#include "../../legacymm/cpp/MemoryPrivate.hpp" // Fine, because this module is a part of legacy MM.

// Note that only C++ part of the runtime goes via those functions, Kotlin uses specialized versions.

extern "C" {

const bool IsStrictMemoryModel = false;

OBJ_GETTER(AllocInstance, const TypeInfo* typeInfo) {
  RETURN_RESULT_OF(AllocInstanceRelaxed, typeInfo);
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* typeInfo, int32_t elements) {
  RETURN_RESULT_OF(AllocArrayInstanceRelaxed, typeInfo, elements);
}

OBJ_GETTER(InitThreadLocalSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    RETURN_RESULT_OF(InitThreadLocalSingletonRelaxed, location, typeInfo, ctor);
}

OBJ_GETTER(InitSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    RETURN_RESULT_OF(InitSingletonRelaxed, location, typeInfo, ctor);
}

RUNTIME_NOTHROW void ReleaseHeapRef(const ObjHeader* object) {
  ReleaseHeapRefRelaxed(object);
}

RUNTIME_NOTHROW void ReleaseHeapRefNoCollect(const ObjHeader* object) {
  ReleaseHeapRefNoCollectRelaxed(object);
}

RUNTIME_NOTHROW void ZeroStackRef(ObjHeader** location) {
  ZeroStackRefRelaxed(location);
}

RUNTIME_NOTHROW void SetStackRef(ObjHeader** location, const ObjHeader* object) {
  SetStackRefRelaxed(location, object);
}

RUNTIME_NOTHROW void SetHeapRef(ObjHeader** location, const ObjHeader* object) {
  SetHeapRefRelaxed(location, object);
}

RUNTIME_NOTHROW void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) {
  UpdateHeapRefRelaxed(location, object);
}

RUNTIME_NOTHROW void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
  UpdateReturnRefRelaxed(returnSlot, object);
}

RUNTIME_NOTHROW void EnterFrame(ObjHeader** start, int parameters, int count) {
  EnterFrameRelaxed(start, parameters, count);
}

RUNTIME_NOTHROW void LeaveFrame(ObjHeader** start, int parameters, int count) {
  LeaveFrameRelaxed(start, parameters, count);
}

RUNTIME_NOTHROW void UpdateStackRef(ObjHeader** location, const ObjHeader* object) {
    UpdateStackRefRelaxed(location, object);
}

}  // extern "C"
