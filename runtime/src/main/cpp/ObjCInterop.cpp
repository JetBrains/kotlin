/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#if KONAN_OBJC_INTEROP

#include <objc/objc.h>
#include <objc/runtime.h>
#include <objc/message.h>
#include <cstdio>
#include <cstdint>

#include "Memory.h"
#include "MemoryPrivate.hpp"

#include "Natives.h"
#include "Utils.h"

extern "C" {

Class Kotlin_Interop_getObjCClass(const char* name);

struct KotlinClassData {
  const TypeInfo* typeInfo;
  int32_t bodyOffset;
};

static inline struct KotlinClassData* GetKotlinClassData(Class clazz) {
  void* ivars = object_getIndexedIvars(reinterpret_cast<id>(clazz));
  return static_cast<struct KotlinClassData*>(ivars);
}

static inline void SetKotlinTypeInfo(Class clazz, const TypeInfo* typeInfo) {
  GetKotlinClassData(clazz)->typeInfo = typeInfo;
}

const TypeInfo* GetObjCKotlinTypeInfo(ObjHeader* obj) RUNTIME_NOTHROW;

RUNTIME_NOTHROW const TypeInfo* GetObjCKotlinTypeInfo(ObjHeader* obj) {
  RuntimeAssert(obj->has_meta_object(), "");
  void* objcPtr = obj->meta_object()->associatedObject_;
  RuntimeAssert(objcPtr != nullptr, "");
  Class clazz = object_getClass(reinterpret_cast<id>(objcPtr));
  return GetKotlinClassData(clazz)->typeInfo;
}

id objc_msgSendSuper2(struct objc_super *super, SEL op, ...);

static void DeallocImp(id self, SEL _cmd) {
  // TODO: doesn't support overriding Kotlin classes.
  Class clazz = object_getClass(self);
  RuntimeAssert(clazz != nullptr, "Must not be null");

  struct KotlinClassData* classData = GetKotlinClassData(clazz);
  void* body = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(self) + classData->bodyOffset);

  const TypeInfo* typeInfo = classData->typeInfo;
  DeinitInstanceBody(typeInfo, body);

  // Call super.dealloc:
  struct objc_super s = {self, clazz};
  auto messenger = reinterpret_cast<void (*) (struct objc_super*, SEL _cmd)>(objc_msgSendSuper2);
  messenger(&s, _cmd);
}

static void AddDeallocMethod(Class clazz) {
  Class nsObjectClass = Kotlin_Interop_getObjCClass("NSObject");

  SEL deallocSelector = sel_registerName("dealloc");
  Method nsObjectDeallocMethod = class_getInstanceMethod(nsObjectClass, deallocSelector);
  RuntimeAssert(nsObjectDeallocMethod != nullptr, "[NSObject dealloc] method not found");

  const char* nsObjectDeallocMethodTypeEncoding = method_getTypeEncoding(nsObjectDeallocMethod);
  RuntimeAssert(nsObjectDeallocMethodTypeEncoding != nullptr, "[NSObject dealloc] method has no encoding provided");

  // TODO: something of the above can be cached.

  BOOL added = class_addMethod(clazz, deallocSelector, (IMP)DeallocImp, nsObjectDeallocMethodTypeEncoding);
  RuntimeAssert(added, "Unable to add dealloc method to Objective-C class");
}

struct ObjCMethodDescription {
  void* (*imp)(void*, void*, ...);
  const char* selector;
  const char* encoding;
};

struct KotlinObjCClassInfo {
  const char* name;

  const char* superclassName;
  const char** protocolNames;

  const struct ObjCMethodDescription* instanceMethods;
  int32_t instanceMethodsNum;

  const struct ObjCMethodDescription* classMethods;
  int32_t classMethodsNum;

  int32_t bodySize;
  int32_t* bodyOffset;

  const TypeInfo* typeInfo;
  const TypeInfo* metaTypeInfo;

  void** createdClass;
};

static void AddMethods(Class clazz, const struct ObjCMethodDescription* methods, int32_t methodsNum) {
  for (int32_t i = 0; i < methodsNum; ++i) {
    const struct ObjCMethodDescription* method = &methods[i];
    BOOL added = class_addMethod(clazz, sel_registerName(method->selector), (IMP)method->imp, method->encoding);
    RuntimeAssert(added == YES, "Unable to add method to Objective-C class");
  }
}

static SimpleMutex classCreationMutex;
static int anonymousClassNextId = 0;

void* CreateKotlinObjCClass(const KotlinObjCClassInfo* info) {
  LockGuard<SimpleMutex> lockGuard(classCreationMutex);

  void* createdClass = *info->createdClass;
  if (createdClass != nullptr) {
    return createdClass;
  }

  char classNameBuffer[64];
  const char* className = info->name;
  if (className == nullptr) {
    snprintf(classNameBuffer, sizeof(classNameBuffer), "kobjc%d", anonymousClassNextId++);
    className = classNameBuffer;
  }

  Class superclass = Kotlin_Interop_getObjCClass(info->superclassName);
  Class newClass = objc_allocateClassPair(superclass, className, sizeof(struct KotlinClassData));
  RuntimeAssert(newClass != nullptr, "Failed to allocate Objective-C class");

  Class newMetaclass = object_getClass(reinterpret_cast<id>(newClass));

  for (size_t i = 0;; ++i) {
    const char* protocolName = info->protocolNames[i];
    if (protocolName == nullptr) break;
    Protocol* proto = objc_getProtocol(protocolName);
    if (proto != nullptr) {
      BOOL added = class_addProtocol(newClass, proto);
      RuntimeAssert(added == YES, "Unable to add protocol to Objective-C class");
      added = class_addProtocol(newMetaclass, proto);
      RuntimeAssert(added == YES, "Unable to add protocol to Objective-C metaclass");
    }
  }

  AddDeallocMethod(newClass);

  AddMethods(newClass, info->instanceMethods, info->instanceMethodsNum);
  AddMethods(newMetaclass, info->classMethods, info->classMethodsNum);

  SetKotlinTypeInfo(newClass, info->typeInfo);
  SetKotlinTypeInfo(newMetaclass, info->metaTypeInfo);

  char bodyTypeEncoding[16];
  snprintf(bodyTypeEncoding, sizeof(bodyTypeEncoding), "[%dc]", info->bodySize);
  BOOL added = class_addIvar(newClass, "kotlinBody", info->bodySize, /* log2(align) = */ 3, bodyTypeEncoding);
  RuntimeAssert(added == YES, "Unable to add ivar to Objective-C class");

  objc_registerClassPair(newClass);

  Ivar body = class_getInstanceVariable(newClass, "kotlinBody");
  RuntimeAssert(body != nullptr, "Unable to get ivar added to Objective-C class");
  int32_t offset = (int32_t)ivar_getOffset(body);
  GetKotlinClassData(newClass)->bodyOffset = offset;
  *info->bodyOffset = offset;

  *info->createdClass = newClass;
  return newClass;
}

void* objc_autoreleasePoolPush();
void objc_autoreleasePoolPop(void* ptr);
id objc_allocWithZone(Class clazz);
id objc_retain(id ptr);
void objc_release(id ptr);

void* Kotlin_objc_autoreleasePoolPush() {
  return objc_autoreleasePoolPush();
}

void Kotlin_objc_autoreleasePoolPop(void* ptr) {
  objc_autoreleasePoolPop(ptr);
}

id Kotlin_objc_allocWithZone(Class clazz) {
  return objc_allocWithZone(clazz);
}

id Kotlin_objc_retain(id ptr) {
  return objc_retain(ptr);
}

void Kotlin_objc_release(id ptr) {
  objc_release(ptr);
}

Class Kotlin_objc_lookUpClass(const char* name) {
  return objc_lookUpClass(name);
}

} // extern "C"

#else  // KONAN_OBJC_INTEROP

#include "KAssert.h"

extern "C" {

void* Kotlin_objc_autoreleasePoolPush() {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

void Kotlin_objc_autoreleasePoolPop(void* ptr) {
  RuntimeAssert(false, "Objective-C interop is disabled");
}

void* Kotlin_objc_allocWithZone(void* clazz) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

void* Kotlin_objc_retain(void* ptr) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

void Kotlin_objc_release(void* ptr) {
  RuntimeAssert(false, "Objective-C interop is disabled");
}

void* Kotlin_objc_lookUpClass(const char* name) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

} // extern "C"

#endif // KONAN_OBJC_INTEROP
