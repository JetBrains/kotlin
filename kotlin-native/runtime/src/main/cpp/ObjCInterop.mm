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

#import <Foundation/NSException.h>
#import <objc/objc-exception.h>

#include <objc/objc.h>
#include <objc/runtime.h>
#include <objc/message.h>
#include <cstdio>
#include <cstdint>

#include "Memory.h"
#include "MemorySharedRefs.hpp"

#include "Natives.h"
#include "ObjCInterop.h"
#include "ObjCExportPrivate.h"
#include "ObjCMMAPI.h"
#include "Types.h"
#include "Mutex.hpp"

// Replaced in ObjCExportCodeGenerator.
__attribute__((weak)) const char* Kotlin_ObjCInterop_uniquePrefix = nullptr;

const char* Kotlin_ObjCInterop_getUniquePrefix() {
  auto result = Kotlin_ObjCInterop_uniquePrefix;
  RuntimeCheck(result != nullptr, "unique prefix is not initialized");
  return result;
}

extern "C" id objc_msgSendSuper2(struct objc_super *super, SEL op, ...);

struct KotlinClassData {
  const TypeInfo* typeInfo;
  int32_t bodyOffset;
};

static inline struct KotlinClassData* GetKotlinClassData(Class clazz) {
  void* ivars = object_getIndexedIvars(reinterpret_cast<id>(clazz));
  return static_cast<struct KotlinClassData*>(ivars);
}

namespace {

BackRefFromAssociatedObject* getBackRef(id obj, KotlinClassData* classData) {
  void* body = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(obj) + classData->bodyOffset);
  return reinterpret_cast<BackRefFromAssociatedObject*>(body);
}

BackRefFromAssociatedObject* getBackRef(id obj) {
  // TODO: suboptimal; consider specializing methods for each class.
  auto* classData = GetKotlinClassData(object_getClass(obj));
  return getBackRef(obj, classData);
}

OBJ_GETTER(toKotlinImp, id self, SEL _cmd) {
  RETURN_OBJ(getBackRef(self)->ref<ErrorPolicy::kTerminate>());
}

id allocWithZoneImp(Class self, SEL _cmd, void* zone) {
  // [super allocWithZone:zone]
  struct objc_super s = {(id)self, object_getClass((id)self)};
  auto messenger = reinterpret_cast<id (*) (struct objc_super*, SEL _cmd, void* zone)>(objc_msgSendSuper2);
  id result = messenger(&s, _cmd, zone);

  auto* classData = GetKotlinClassData(self); // TODO: suboptimal; consider specializing.
  auto* typeInfo = classData->typeInfo;
  ObjHolder holder;
  auto kotlinObj = AllocInstanceWithAssociatedObject(typeInfo, result, holder.slot());

  getBackRef(result, classData)->initAndAddRef(kotlinObj);

  return result;
}

id retainImp(id self, SEL _cmd) {
  getBackRef(self)->addRef<ErrorPolicy::kTerminate>();
  return self;
}

BOOL _tryRetainImp(id self, SEL _cmd) {
  // TODO: [tryAddRef] currently works only on the owner thread for non-shared objects;
  // this is a regression for instances of Kotlin subclasses of Obj-C classes:
  // loading a reference to such an object from Obj-C weak reference now fails on "wrong" thread
  // unless the object is frozen.
  try {
    return getBackRef(self)->tryAddRef<ErrorPolicy::kThrow>();
  } catch (ExceptionObjHolder& e) {
    // TODO: check for IncorrectDereferenceException and possible weak property access
    // Cannot use SourceInfo here, because CoreSymbolication framework (CSSymbolOwnerGetSymbolWithAddress)
    // fails at recursive retain lock. Similarly, cannot use objc exception here, because its unhandled
    // exception handler might fail at recursive retain lock too.
    // TODO: Refactor to be more explicit. Instead of relying on an unhandled exception termination
    // (and effectively setting a global to alter its behavior), just call an appropriate termination
    // function by hand.
    DisallowSourceInfo();
    std::terminate();
  }
}

void releaseImp(id self, SEL _cmd) {
  getBackRef(self)->releaseRef();
}

void releaseAsAssociatedObjectImp(id self, SEL _cmd) {
  // This function is called by the GC. It made a decision to reclaim Kotlin object, and runs
  // deallocation hooks at the moment, including deallocation of the "associated object" ([self])
  // using the [super release] call below.

  // The deallocation involves running [self dealloc] which can contain arbitrary code.
  // In particular, this code can retain and release [self]. Obj-C and Swift runtimes handle this
  // gracefully (unless the object gets accessed after the deallocation of course), but Kotlin doesn't.
  // For example, this happens in https://youtrack.jetbrains.com/issue/KT-41811, provoked by
  // UIViewController.dealloc (which retains-releases self._view._viewDelegate == self) and UIView.dealloc.
  // Generally retaining and releasing Kotlin object that is being deallocated would lead to
  // use-after-dispose and double-dispose problems (with unpredictable consequences) or to an assertion failure.
  // To workaround this, detach the back ref from the Kotlin object:
  getBackRef(self)->detach();
  // So retain/release/etc. on [self] won't affect the Kotlin object, and an attempt to get
  // the reference to it (e.g. when calling Kotlin method on [self]) would crash.
  // The latter is generally ok, because by the time superclass dealloc gets launched, subclass state
  // should already be deinitialized, and Kotlin methods operate on the subclass.

  // [super release]
  Class clazz = object_getClass(self);
  struct objc_super s = {self, clazz};
  auto messenger = reinterpret_cast<void (*) (struct objc_super*, SEL _cmd)>(objc_msgSendSuper2);
  messenger(&s, @selector(release));
}

}

extern "C" {

Class Kotlin_Interop_getObjCClass(const char* name);

static inline void SetKotlinTypeInfo(Class clazz, const TypeInfo* typeInfo) {
  GetKotlinClassData(clazz)->typeInfo = typeInfo;
}

const TypeInfo* GetObjCKotlinTypeInfo(ObjHeader* obj) RUNTIME_NOTHROW;

RUNTIME_NOTHROW const TypeInfo* GetObjCKotlinTypeInfo(ObjHeader* obj) {
    void* objcPtr = obj->GetAssociatedObject();
    RuntimeAssert(objcPtr != nullptr, "");
    Class clazz = object_getClass(reinterpret_cast<id>(objcPtr));
    return GetKotlinClassData(clazz)->typeInfo;
}


static void AddNSObjectOverride(bool isClassMethod, Class clazz, SEL selector, void* imp) {
  Class nsObjectClass = Kotlin_Interop_getObjCClass("NSObject");

  Method nsObjectMethod = class_getInstanceMethod(
      isClassMethod ? object_getClass((id)nsObjectClass) : nsObjectClass, selector);
  RuntimeCheck(nsObjectMethod != nullptr, "NSObject method not found");

  const char* nsObjectMethodTypeEncoding = method_getTypeEncoding(nsObjectMethod);
  RuntimeCheck(nsObjectMethodTypeEncoding != nullptr, "NSObject method has no encoding provided");

  // TODO: something of the above can be cached.

  BOOL added = class_addMethod(
      isClassMethod ? object_getClass((id)clazz) : clazz, selector, (IMP)imp, nsObjectMethodTypeEncoding);
  RuntimeCheck(added, "Unable to add method to Objective-C class");
}

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

static Class allocateClass(const KotlinObjCClassInfo* info) {
  Class superclass = Kotlin_Interop_getObjCClass(info->superclassName);
  size_t extraBytes = sizeof(struct KotlinClassData);

  if (info->exported) {
    RuntimeCheck(info->name != nullptr, "exported Objective-C class must have a name");
    Class result = objc_allocateClassPair(superclass, info->name, extraBytes);
    if (result != nullptr) return result;
    // Similar to how Objective-C runtime handles this:
    fprintf(stderr, "Class %s has multiple implementations. Which one will be used is undefined.\n", info->name);
  }

  KStdString className = Kotlin_ObjCInterop_getUniquePrefix();

  if (info->name != nullptr) {
    className += info->name;
  } else {
    className += "_kobjc";
  }

  int classId = anonymousClassNextId++;
  className += std::to_string(classId);

  Class result = objc_allocateClassPair(superclass, className.c_str(), extraBytes);
  RuntimeCheck(result != nullptr, "Failed to allocate Objective-C class");
  return result;
}

void* CreateKotlinObjCClass(const KotlinObjCClassInfo* info) {
  LockGuard<SimpleMutex> lockGuard(classCreationMutex);

  void* createdClass = *info->createdClass;
  if (createdClass != nullptr) {
    return createdClass;
  }

  Class newClass = allocateClass(info);

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

  AddNSObjectOverride(false, newClass, Kotlin_ObjCExport_toKotlinSelector, (void*)&toKotlinImp);
  AddNSObjectOverride(true, newClass, @selector(allocWithZone:), (void*)&allocWithZoneImp);
  AddNSObjectOverride(false, newClass, @selector(retain), (void*)&retainImp);
  AddNSObjectOverride(false, newClass, @selector(_tryRetain), (void*)&_tryRetainImp);
  AddNSObjectOverride(false, newClass, @selector(release), (void*)&releaseImp);
  AddNSObjectOverride(false, newClass, Kotlin_ObjCExport_releaseAsAssociatedObjectSelector,
      (void*)&releaseAsAssociatedObjectImp);

  AddMethods(newClass, info->instanceMethods, info->instanceMethodsNum);
  AddMethods(newMetaclass, info->classMethods, info->classMethodsNum);

  SetKotlinTypeInfo(newClass, Kotlin_ObjCExport_createTypeInfoWithKotlinFieldsFrom(newClass, info->typeInfo));

  int bodySize = sizeof(BackRefFromAssociatedObject);
  char bodyTypeEncoding[16];
  snprintf(bodyTypeEncoding, sizeof(bodyTypeEncoding), "[%dc]", bodySize);
  BOOL added = class_addIvar(newClass, "kotlinBody", bodySize, /* log2(align) = */ 3, bodyTypeEncoding);
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

konan::AutoreleasePool::AutoreleasePool()
  : handle(objc_autoreleasePoolPush()) {}

konan::AutoreleasePool::~AutoreleasePool() {
  objc_autoreleasePoolPop(handle);
}

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
