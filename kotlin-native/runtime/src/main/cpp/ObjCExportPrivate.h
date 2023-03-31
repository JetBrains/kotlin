/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJCEXPORTPRIVATE_H
#define RUNTIME_OBJCEXPORTPRIVATE_H

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>

#import "Types.h"
#import "Memory.h"
#import "ObjCExport.h"

@interface KotlinBase : NSObject <NSCopying>
+(instancetype)createRetainedWrapper:(ObjHeader*)obj;
@end

extern "C" void Kotlin_ObjCExport_initializeClass(Class clazz);
extern "C" const TypeInfo* Kotlin_ObjCExport_getAssociatedTypeInfo(Class clazz);
extern "C" OBJ_GETTER(Kotlin_ObjCExport_convertUnmappedObjCObject, id obj);
extern "C" SEL Kotlin_ObjCExport_toKotlinSelector;
extern "C" SEL Kotlin_ObjCExport_releaseAsAssociatedObjectSelector;

const TypeInfo* Kotlin_ObjCExport_createTypeInfoWithKotlinFieldsFrom(Class clazz, const TypeInfo* fieldsInfo);

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCEXPORTPRIVATE_H
