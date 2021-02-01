/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_INITIALIZATION_SCHEME_H
#define RUNTIME_MM_INITIALIZATION_SCHEME_H

#include "Memory.h"

namespace kotlin {
namespace mm {

class ThreadData;

OBJ_GETTER(InitThreadLocalSingleton, ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));
OBJ_GETTER(InitSingleton, ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_INITIALIZATION_SCHEME_H
