/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJCINTEROP_H
#define RUNTIME_OBJCINTEROP_H

#if KONAN_OBJC_INTEROP

#include <Foundation/NSString.h>
#include <Foundation/NSException.h>
#include <objc/objc-exception.h>

#include <objc/objc.h>
#include <objc/runtime.h>
#include <objc/message.h>

#include "TypeInfo.h"

extern "C" {

struct KotlinObjCClassData {
  const TypeInfo* typeInfo;
  Class objcClass;
  int32_t bodyOffset;
};

struct ObjCMethodDescription {
  void* (*imp)(void*, void*, ...);
  const char* selector;
  const char* encoding;
};

struct KotlinObjCClassInfo {
  const char* name;
  int exported;

  const char* superclassName;
  const char** protocolNames;

  const struct ObjCMethodDescription* instanceMethods;
  int32_t instanceMethodsNum;

  const struct ObjCMethodDescription* classMethods;
  int32_t classMethodsNum;

  int32_t* bodyOffset;

  const TypeInfo* typeInfo;
  const TypeInfo* metaTypeInfo;

  void** createdClass;

  KotlinObjCClassData* (*classDataImp)(void*, void*);
};

void* CreateKotlinObjCClass(const KotlinObjCClassInfo* info);
RUNTIME_NOTHROW const TypeInfo* GetObjCKotlinTypeInfo(ObjHeader* obj);

} // extern "C"

const char* Kotlin_ObjCInterop_getUniquePrefix();

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCINTEROP_H
