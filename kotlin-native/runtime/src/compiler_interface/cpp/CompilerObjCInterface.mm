/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_OBJC_INTEROP

#include "ObjCInterop.h"
#include "ObjCInteropUtils.h"
#include "ObjCExport.h"
#include "ObjCExportErrors.h"
#include "ObjCExportCoroutines.h"

#define touchType(type) type touch##type;
#define touchFunction(function) void* touch##function() { return reinterpret_cast<void*>(&::function); }

// Types and functions used by the compiler (at Runtime.kt and ContextUtils.kt)
extern "C" {

touchType(WritableTypeInfo)
touchType(KotlinObjCClassData)
touchType(KotlinObjCClassInfo)
touchType(ObjCMethodDescription)
touchType(ObjCTypeAdapter)
touchType(ObjCToKotlinMethodAdapter)
touchType(KotlinToObjCMethodAdapter)
touchType(TypeInfoObjCExportAddition)

touchType(Block_literal_1)
touchType(Block_descriptor_1)

NSString* touchNSString() { return [[NSString alloc] init]; }

touchFunction(CreateKotlinObjCClass)
touchFunction(GetObjCKotlinTypeInfo)
touchFunction(MissingInitImp)

touchFunction(Kotlin_Interop_DoesObjectConformToProtocol)
touchFunction(Kotlin_Interop_IsObjectKindOfClass)

touchFunction(Kotlin_ObjCExport_refToLocalObjC)
touchFunction(Kotlin_ObjCExport_refToRetainedObjC)
touchFunction(Kotlin_ObjCExport_refFromObjC)
touchFunction(Kotlin_ObjCExport_CreateRetainedNSStringFromKString)
touchFunction(Kotlin_ObjCExport_convertUnitToRetained)
touchFunction(Kotlin_ObjCExport_GetAssociatedObject)
touchFunction(Kotlin_ObjCExport_AbstractMethodCalled)
touchFunction(Kotlin_ObjCExport_AbstractClassConstructorCalled)
touchFunction(Kotlin_ObjCExport_RethrowExceptionAsNSError)
touchFunction(Kotlin_ObjCExport_WrapExceptionToNSError)
touchFunction(Kotlin_ObjCExport_NSErrorAsException)
touchFunction(Kotlin_ObjCExport_AllocInstanceWithAssociatedObject)
touchFunction(Kotlin_ObjCExport_createContinuationArgument)
touchFunction(Kotlin_ObjCExport_createUnitContinuationArgument)
touchFunction(Kotlin_ObjCExport_resumeContinuation)
touchFunction(Kotlin_ObjCExport_NSIntegerTypeProvider)
touchFunction(Kotlin_longTypeProvider)

}

#endif // KONAN_OBJC_INTEROP
