/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#include "Memory.h"
#include "MemoryPrivate.hpp"

// Note that only C++ part of the runtime goes via those functions, Kotlin uses specialized versions.

extern "C" {

const bool IsStrictMemoryModel = true;

OBJ_GETTER(AllocInstance, const TypeInfo* typeInfo) {
  RETURN_RESULT_OF(AllocInstanceStrict, typeInfo);
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* typeInfo, int32_t elements) {
  RETURN_RESULT_OF(AllocArrayInstanceStrict, typeInfo, elements);
}

OBJ_GETTER(InitInstance,
    ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
  RETURN_RESULT_OF(InitInstanceStrict, location, typeInfo, ctor);
}

OBJ_GETTER(InitSharedInstance,
    ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
  RETURN_RESULT_OF(InitSharedInstanceStrict, location, typeInfo, ctor);
}

void ReleaseHeapRef(const ObjHeader* object) {
  ReleaseHeapRefStrict(object);
}

void ReleaseHeapRefNoCollect(const ObjHeader* object) {
  ReleaseHeapRefNoCollectStrict(object);
}

void SetStackRef(ObjHeader** location, const ObjHeader* object) {
  SetStackRefStrict(location, object);
}

void SetHeapRef(ObjHeader** location, const ObjHeader* object) {
  SetHeapRefStrict(location, object);
}

void ZeroStackRef(ObjHeader** location) {
  ZeroStackRefStrict(location);
}

void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) {
  UpdateHeapRefStrict(location, object);
}

void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
  UpdateReturnRefStrict(returnSlot, object);
}

void EnterFrame(ObjHeader** start, int parameters, int count) {
  EnterFrameStrict(start, parameters, count);
}

void LeaveFrame(ObjHeader** start, int parameters, int count) {
  LeaveFrameStrict(start, parameters, count);
}

}  // extern "C"
