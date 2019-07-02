/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#include "Memory.h"

// Note that only C++ part of the runtime goes via those functions, Kotlin uses specialized versions.

extern "C" {

OBJ_GETTER(AllocInstance, const TypeInfo* typeInfo) {
  RETURN_RESULT_OF(AllocInstanceRelaxed, typeInfo);
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* typeInfo, int32_t elements) {
  RETURN_RESULT_OF(AllocArrayInstanceRelaxed, typeInfo, elements);
}

OBJ_GETTER(InitInstance,
    ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
  RETURN_RESULT_OF(InitInstanceRelaxed, location, typeInfo, ctor);
}

OBJ_GETTER(InitSharedInstance,
    ObjHeader** location, ObjHeader** localLocation, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
  RETURN_RESULT_OF(InitSharedInstanceRelaxed, location, localLocation, typeInfo, ctor);
}

}  // extern "C"
