/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Types.h"

extern "C" OBJ_GETTER(Kotlin_ObjCExport_createContinuationArgument, id completion, const TypeInfo** exceptionTypes);
extern "C" OBJ_GETTER(Kotlin_ObjCExport_createUnitContinuationArgument, id completion, const TypeInfo** exceptionTypes);
extern "C" void Kotlin_ObjCExport_resumeContinuation(KRef continuation, KRef result, id error);