/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJCEXPORT_H
#define RUNTIME_OBJCEXPORT_H

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>
#import <Foundation/Foundation.h>

#import "Types.h"
#import "Memory.h"

extern "C" {

id objc_retain(id self);
void objc_release(id self);

id Kotlin_ObjCExport_refToObjC(ObjHeader* obj);
id Kotlin_ObjCExport_refToLocalObjC(ObjHeader* obj);
id Kotlin_ObjCExport_refToRetainedObjC(ObjHeader* obj);
id Kotlin_ObjCExport_CreateRetainedNSStringFromKString(ObjHeader* str);
id Kotlin_ObjCExport_convertUnitToRetained(ObjHeader* unitInstance);
id Kotlin_ObjCExport_GetAssociatedObject(ObjHeader* obj);
void Kotlin_ObjCExport_AbstractMethodCalled(id self, SEL selector);
void Kotlin_ObjCExport_AbstractClassConstructorCalled(id self, const TypeInfo *clazz);
OBJ_GETTER(Kotlin_ObjCExport_refFromObjC, id obj);
RUNTIME_NOTHROW OBJ_GETTER(Kotlin_ObjCExport_AllocInstanceWithAssociatedObject,
                           const TypeInfo* typeInfo, id associatedObject);

id Kotlin_Interop_CreateNSStringFromKString(KRef str);
OBJ_GETTER(Kotlin_Interop_CreateKStringFromNSString, NSString* str);

/// Utility function that is used to determine NSInteger size in compile time.
NSInteger Kotlin_ObjCExport_NSIntegerTypeProvider();

} // extern "C"

inline static id GetAssociatedObject(ObjHeader* obj) {
    return (id)obj->GetAssociatedObject();
}

// Note: this function shall not be used on shared objects.
inline static void SetAssociatedObject(ObjHeader* obj, id value) {
    obj->SetAssociatedObject((void*)value);
}

inline static id AtomicCompareAndSwapAssociatedObject(ObjHeader* obj, id expectedValue, id newValue) {
    return static_cast<id>(obj->CasAssociatedObject(expectedValue, newValue));
}

inline static OBJ_GETTER(AllocInstanceWithAssociatedObject, const TypeInfo* typeInfo, id associatedObject) {
  ObjHeader* result = AllocInstance(typeInfo, OBJ_RESULT);
  SetAssociatedObject(result, associatedObject);
  return result;
}

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCEXPORT_H
