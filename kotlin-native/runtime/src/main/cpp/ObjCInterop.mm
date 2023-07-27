/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_OBJC_INTEROP

#import <Foundation/NSException.h>
#import <objc/objc-exception.h>

#include <objc/objc.h>
#include <objc/runtime.h>
#include <objc/message.h>
#include <cstdio>
#include <cstdint>
#include <mutex>

#include "Memory.h"
#include "MemorySharedRefs.hpp"

#include "Natives.h"
#include "ObjCInterop.h"
#include "ObjCExportPrivate.h"
#include "ObjCMMAPI.h"
#include "StackTrace.hpp"
#include "Types.h"
#include "Mutex.hpp"
#include "std_support/String.hpp"

using namespace kotlin;

// Replaced in ObjCExportCodeGenerator.
__attribute__((weak)) const char* Kotlin_ObjCInterop_uniquePrefix = nullptr;

const char* Kotlin_ObjCInterop_getUniquePrefix() {
  auto result = Kotlin_ObjCInterop_uniquePrefix;
  RuntimeCheck(result != nullptr, "unique prefix is not initialized");
  return result;
}

extern "C" id objc_msgSendSuper2(struct objc_super *super, SEL op, ...);

// Acts only as container for the method, not actually applied to any class.
@protocol HasKotlinObjCClassData
@required
-(void*)_kotlinObjCClassData;
@end

static inline struct KotlinObjCClassData* GetKotlinClassData(id objOrClass) {
  void* ptr = [(id<HasKotlinObjCClassData>)objOrClass _kotlinObjCClassData];
  return static_cast<struct KotlinObjCClassData*>(ptr);
}

namespace {

BackRefFromAssociatedObject* getBackRef(id obj, KotlinObjCClassData* classData) {
  void* body = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(obj) + classData->bodyOffset);
  return reinterpret_cast<BackRefFromAssociatedObject*>(body);
}

BackRefFromAssociatedObject* getBackRef(id obj) {
  // TODO: suboptimal; consider specializing methods for each class.
  auto* classData = GetKotlinClassData(obj);
  return getBackRef(obj, classData);
}

OBJ_GETTER(toKotlinImp, id self, SEL _cmd) {
  RETURN_OBJ(getBackRef(self)->ref<ErrorPolicy::kTerminate>());
}

id allocWithZoneImp(Class self, SEL _cmd, void* zone) {
  // [super allocWithZone:zone]
  auto* classData = GetKotlinClassData(self); // TODO: suboptimal; consider specializing.
  struct objc_super s = {(id)self, object_getClass(classData->objcClass)};
  auto messenger = reinterpret_cast<id (*) (struct objc_super*, SEL _cmd, void* zone)>(objc_msgSendSuper2);
  id result = messenger(&s, _cmd, zone);

  auto* typeInfo = classData->typeInfo;

  kotlin::CalledFromNativeGuard guard;
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
    kotlin::DisallowSourceInfo();
    std::terminate();
  }
}

void releaseImp(id self, SEL _cmd) {
  getBackRef(self)->releaseRef();
}

void releaseAsAssociatedObjectImp(id self, SEL _cmd) {
  auto* classData = GetKotlinClassData(self);
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    // No need for any special handling. Weak reference handling machinery
    // has already cleaned up the reference to Kotlin object.
    // [super release]
    Class clazz = classData->objcClass;
    struct objc_super s = {self, clazz};
    auto messenger = reinterpret_cast<void (*) (struct objc_super*, SEL _cmd)>(objc_msgSendSuper2);
    messenger(&s, @selector(release));
    return;
  }

  // This function is called by the GC. It made a decision to reclaim Kotlin object, and runs
  // deallocation hooks at the moment, including deallocation of the "associated object" ([self])
  // using the [super release] call below.

  auto* backRef = getBackRef(self, classData);

  // The deallocation involves running [self dealloc] which can contain arbitrary code.
  // In particular, this code can retain and release [self]. Obj-C and Swift runtimes handle this
  // gracefully (unless the object gets accessed after the deallocation of course), but Kotlin doesn't.
  // For example, this happens in https://youtrack.jetbrains.com/issue/KT-41811, provoked by
  // UIViewController.dealloc (which retains-releases self._view._viewDelegate == self) and UIView.dealloc.
  // Generally retaining and releasing Kotlin object that is being deallocated would lead to
  // use-after-dispose and double-dispose problems (with unpredictable consequences) or to an assertion failure.
  // To workaround this, detach the back ref from the Kotlin object:
  backRef->detach();

  // So retain/release/etc. on [self] won't affect the Kotlin object, and an attempt to get
  // the reference to it (e.g. when calling Kotlin method on [self]) would crash.
  // The latter is generally ok, because by the time superclass dealloc gets launched, subclass state
  // should already be deinitialized, and Kotlin methods operate on the subclass.
  // [super release]
  Class clazz = classData->objcClass;
  struct objc_super s = {self, clazz};
  auto messenger = reinterpret_cast<void (*) (struct objc_super*, SEL _cmd)>(objc_msgSendSuper2);
  messenger(&s, @selector(release));
}

void deallocImp(id self, SEL _cmd) {
  getBackRef(self)->dealloc();

  // [super dealloc]
  auto* classData = GetKotlinClassData(self);
  Class clazz = classData->objcClass;
  struct objc_super s = {self, clazz};
  auto messenger = reinterpret_cast<void (*) (struct objc_super*, SEL _cmd)>(objc_msgSendSuper2);
  messenger(&s, @selector(dealloc));
}

}

extern "C" {

Class Kotlin_Interop_getObjCClass(const char* name) {
    Class result = objc_lookUpClass(name);
    RuntimeCheck(result != nil, "Objective-C class '%s' not found. Ensure that the containing framework or library was linked.", name);
    return result;
}

RUNTIME_NOTHROW const TypeInfo* GetObjCKotlinTypeInfo(ObjHeader* obj) {
    void* objcPtr = obj->GetAssociatedObject();
    RuntimeAssert(objcPtr != nullptr, "");
    return GetKotlinClassData(reinterpret_cast<id>(objcPtr))->typeInfo;
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

static void AddKotlinClassData(bool isClassMethod, Class clazz, void* imp) {
  SEL selector = @selector(_kotlinObjCClassData);

  auto methodDescription = protocol_getMethodDescription(
      @protocol(HasKotlinObjCClassData),
      selector,
      YES, YES
  );

  const char* typeEncoding = methodDescription.types;

  RuntimeCheck(typeEncoding != nullptr, "unable to find method in Objective-C protocol");

  BOOL added = class_addMethod(
      isClassMethod ? object_getClass((id)clazz) : clazz, selector, (IMP)imp, typeEncoding);
  RuntimeCheck(added, "Unable to add method to Objective-C class");
}

static void AddMethods(Class clazz, const struct ObjCMethodDescription* methods, int32_t methodsNum) {
  for (int32_t i = 0; i < methodsNum; ++i) {
    const struct ObjCMethodDescription* method = &methods[i];
    BOOL added = class_addMethod(clazz, sel_registerName(method->selector), (IMP)method->imp, method->encoding);
    RuntimeAssert(added == YES, "Unable to add method to Objective-C class");
  }
}

static kotlin::SpinLock<kotlin::MutexThreadStateHandling::kSwitchIfRegistered> classCreationMutex;
static int anonymousClassNextId = 0;

NO_EXTERNAL_CALLS_CHECK static Class allocateClass(const KotlinObjCClassInfo* info) {
  Class superclass = Kotlin_Interop_getObjCClass(info->superclassName);

  if (info->exported) {
    RuntimeCheck(info->name != nullptr, "exported Objective-C class must have a name");
    Class result = objc_allocateClassPair(superclass, info->name, 0);
    if (result != nullptr) return result;
    // Similar to how Objective-C runtime handles this:
    fprintf(stderr, "Class %s has multiple implementations. Which one will be used is undefined.\n", info->name);
  }

  std_support::string className = Kotlin_ObjCInterop_getUniquePrefix();

  if (info->name != nullptr) {
    className += info->name;
  } else {
    className += "_kobjc";
  }

  int classId = anonymousClassNextId++;
  className += std::to_string(classId);

  Class result = objc_allocateClassPair(superclass, className.c_str(), 0);
  RuntimeCheck(result != nullptr, "Failed to allocate Objective-C class");
  return result;
}

void* CreateKotlinObjCClass(const KotlinObjCClassInfo* info) {
  std::lock_guard lockGuard(classCreationMutex);

  void* createdClass = *info->createdClass;
  if (createdClass != nullptr) {
    return createdClass;
  }

  kotlin::NativeOrUnregisteredThreadGuard threadStateGuard(/* reentrant = */ true);

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
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    AddNSObjectOverride(false, newClass, @selector(dealloc), (void*)&deallocImp);
  }

  AddMethods(newClass, info->instanceMethods, info->instanceMethodsNum);
  AddMethods(newMetaclass, info->classMethods, info->classMethodsNum);

  // Adding both instance and class methods to make [GetKotlinClassData] work
  // for instances as well as the class itself.
  AddKotlinClassData(false, newClass, (void*)info->classDataImp);
  AddKotlinClassData(true, newClass, (void*)info->classDataImp);

  const TypeInfo* actualTypeInfo = Kotlin_ObjCExport_createTypeInfoWithKotlinFieldsFrom(newClass, info->typeInfo);

  int bodySize = sizeof(BackRefFromAssociatedObject);
  char bodyTypeEncoding[16];
  snprintf(bodyTypeEncoding, sizeof(bodyTypeEncoding), "[%dc]", bodySize);
  BOOL added = class_addIvar(newClass, "kotlinBody", bodySize, /* log2(align) = */ 3, bodyTypeEncoding);
  RuntimeAssert(added == YES, "Unable to add ivar to Objective-C class");

  objc_registerClassPair(newClass);

  Ivar body = class_getInstanceVariable(newClass, "kotlinBody");
  RuntimeAssert(body != nullptr, "Unable to get ivar added to Objective-C class");
  int32_t offset = (int32_t)ivar_getOffset(body);
  *info->bodyOffset = offset;

  // Doing this after objc_registerClassPair because it is not clear whether calling class methods
  // is safe before that.
  auto* classData = GetKotlinClassData(newClass);
  classData->typeInfo = actualTypeInfo;
  classData->objcClass = newClass;
  classData->bodyOffset = offset;

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
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative, true);
  objc_autoreleasePoolPop(handle);
}

void* Kotlin_objc_autoreleasePoolPush() {
  return objc_autoreleasePoolPush();
}

void Kotlin_objc_autoreleasePoolPop(void* ptr) {
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
  objc_autoreleasePoolPop(ptr);
}

id Kotlin_objc_allocWithZone(Class clazz) {
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
  return objc_allocWithZone(clazz);
}

id Kotlin_objc_retain(id ptr) {
  return objc_retain(ptr);
}

void Kotlin_objc_release(id ptr) {
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
  objc_release(ptr);
}

void Kotlin_objc_detachObjCObject(KRef ref) {
  id associatedObject = GetAssociatedObject(ref);
  while (true) {
    if (associatedObject == nullptr) break;
    id actualAssociatedObject = AtomicCompareAndSwapAssociatedObject(ref, associatedObject, nullptr);
    if (actualAssociatedObject == associatedObject) {
      Kotlin_ObjCExport_releaseAssociatedObject(associatedObject);
      break;
    }
    associatedObject = actualAssociatedObject;
  }
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

void Kotlin_objc_detachObjCObject(void* ref) {
  RuntimeAssert(false, "Objective-C interop is disabled");
}

} // extern "C"

#endif // KONAN_OBJC_INTEROP
