/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#import "Types.h"
#import "Memory.h"
#import "MemorySharedRefs.hpp"

#if KONAN_OBJC_INTEROP

#import <Foundation/Foundation.h>
#import <objc/runtime.h>
#import <objc/objc-exception.h>
#import <dispatch/dispatch.h>

#import "CallsChecker.hpp"
#import "ObjCExport.h"
#import "ObjCExportInit.h"
#import "ObjCExportPrivate.h"
#import "Runtime.h"
#import "concurrent/Mutex.hpp"
#import "Exceptions.h"
#include "ExternalRCRef.hpp"
#include "swiftExportRuntime/SwiftExport.hpp"
#include "StackTrace.hpp"

@interface NSObject (NSObjectPrivateMethods)
// Implemented for NSObject in libobjc/NSObject.mm
-(BOOL)_tryRetain;
@end

static void injectToRuntime();

extern "C" KInt Kotlin_hashCode(KRef str);
extern "C" KBoolean Kotlin_equals(KRef lhs, KRef rhs);
extern "C" OBJ_GETTER(Kotlin_toString, KRef obj);

// Note: `KotlinBase`'s `toKotlin` and `_tryRetain` methods will terminate if
// called with non-frozen object on a wrong worker. `retain` will also terminate
// in these conditions if backref's refCount is zero.

@implementation KotlinBase {
  BackRefFromAssociatedObject refHolder;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(refHolder.ref());
}

+(void)load {
  injectToRuntime();
}

+(void)initialize {
  if (self == [KotlinBase class]) {
    injectToRuntime(); // In case `initialize` is called before `load` (see e.g. https://youtrack.jetbrains.com/issue/KT-50982).
    Kotlin_ObjCExport_initialize();
  }
  if (kotlin::compiler::swiftExport()) {
      // Swift Export generates types that don't need to be additionally initialized.
      return;
  }
  Kotlin_ObjCExport_initializeClass(self);
}

+(instancetype)allocWithZone:(NSZone*)zone {
  if (kotlin::compiler::swiftExport()) {
      // Swift Export types create Kotlin object themselves and do not dynamically associate
      // Swift type info with Kotlin type info.
      return [super allocWithZone:zone];
  }

  Kotlin_initRuntimeIfNeeded();
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable);

  KotlinBase* result = [super allocWithZone:zone];

  const TypeInfo* typeInfo = Kotlin_ObjCExport_getAssociatedTypeInfo(self);
  if (typeInfo == nullptr) {
    [NSException raise:NSGenericException
          format:@"%s is not allocatable or +[KotlinBase initialize] method wasn't called on it",
          class_getName(object_getClass(self))];
  }

  if (typeInfo->instanceSize_ < 0) {
    [NSException raise:NSGenericException
          format:@"%s must be allocated and initialized with a factory method",
          class_getName(object_getClass(self))];
  }
  ObjHolder holder;
  AllocInstanceWithAssociatedObject(typeInfo, result, holder.slot());
  result->refHolder.initAndAddRef(holder.obj());
  return result;
}

+(instancetype)createRetainedWrapper:(ObjHeader*)obj {
  RuntimeAssert(!kotlin::compiler::swiftExport(), "Must not be used in Swift Export");

  kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);

  KotlinBase* candidate = [super allocWithZone:nil];
  // TODO: should we call NSObject.init ?
  bool permanent = obj->permanent();

  if (!permanent) { // TODO: permanent objects should probably be supported as custom types.
    candidate->refHolder.initAndAddRef(obj);
    if (id old = AtomicCompareAndSwapAssociatedObject(obj, nullptr, candidate)) {
      {
        kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
        candidate->refHolder.releaseRef();
        [candidate releaseAsAssociatedObject];
      }
      return objc_retain(old);
    }
  } else {
    candidate->refHolder.initForPermanentObject(obj);
  }

  return candidate;
}

-(instancetype)retain {
  if (refHolder.isPermanent()) {
    [super retain];
  } else {
    refHolder.addRef();
  }
  return self;
}

-(BOOL)_tryRetain {
  if (refHolder.isPermanent()) {
    return [super _tryRetain];
  } else {
    return refHolder.tryAddRef();
  }
}

-(oneway void)release {
  if (refHolder.isPermanent()) {
    [super release];
  } else {
    refHolder.releaseRef();
  }
}

-(void)releaseAsAssociatedObject {
  RuntimeAssert(!refHolder.isPermanent(), "Cannot be called on permanent objects");
  // No need for any special handling. Weak reference handling machinery
  // has already cleaned up the reference to Kotlin object.
  [super release];
}

- (instancetype)copyWithZone:(NSZone *)zone {
  // TODO: write documentation.
  return [self retain];
}

- (instancetype)initWithExternalRCRef:(uintptr_t)ref {
    RuntimeAssert(kotlin::compiler::swiftExport(), "Must be used in Swift Export only");
    kotlin::AssertThreadState(kotlin::ThreadState::kNative);

    auto externalRCRef = reinterpret_cast<kotlin::mm::RawExternalRCRef*>(ref);
    Class bestFittingClass =
            kotlin::swiftExportRuntime::bestFittingObjCClassFor(kotlin::mm::typeOfExternalRCRef(externalRCRef));
    if ([self class] != bestFittingClass) {
        if ([[self class] isSubclassOfClass:bestFittingClass]) {
            konan::consoleErrorf(
                    "Inheritance from Kotlin exported classes is not supported: %s inherits from %s\n", class_getName([self class]),
                    class_getName(bestFittingClass));
            kotlin::PrintStackTraceStderr();
            std::abort();
        }
        RuntimeAssert(
                [bestFittingClass isSubclassOfClass:[self class]], "Best-fitting class is %s which is not a subclass of self (%s)",
                class_getName(bestFittingClass), class_getName([self class]));

        KotlinBase* retiredSelf = self; // old `self`

        // Rerun the entire initializer, but with the best-fitting class now.
        self = [[bestFittingClass alloc] initWithExternalRCRef:ref]; // new `self`, retained.

        // Fully release old `self` by just decrementing NSObject refcount.
        [retiredSelf releaseAsAssociatedObject];

        // Return new `self`.
        return self;
    }

    refHolder.initWithExternalRCRef(externalRCRef);
    if (refHolder.isPermanent()) {
        // Cannot attach associated objects to permanent objects.
        return self;
    }

    id newSelf = nil;
    {
        // TODO: Make it okay to get/replace associated objects w/o runnable state.
        kotlin::CalledFromNativeGuard guard;
        // `ref` holds a strong reference to obj, no need to place obj onto a stack.
        KRef obj = refHolder.ref();
        newSelf = AtomicCompareAndSwapAssociatedObject(obj, nullptr, self);
    }

    if (newSelf == nil) {
        // No previous associated object was set, `self` is the associated object.
        return self;
    }

    RuntimeAssert(
            [[newSelf class] isSubclassOfClass:[self class]],
            "During initialization of %p (%s) for Kotlin object %p trying to replace self with %p (%s) that is not a subclass", self,
            class_getName([self class]), refHolder.ref(), newSelf, class_getName([newSelf class]));

    KotlinBase* retiredSelf = self; // old `self`
    self = [newSelf retain]; // new `self`, retained.

    // Fully release old `self`:
    [retiredSelf release]; // decrement ExternalRCRef refcount,
    [retiredSelf releaseAsAssociatedObject]; // and decrement NSObject refcount.

    // Return new `self`.
    return self;
}

- (uintptr_t)externalRCRef {
    return reinterpret_cast<uintptr_t>(refHolder.externalRCRef());
}

- (NSString *)description {
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable);
    ObjHolder h1;
    ObjHolder h2;
    return Kotlin_Interop_CreateNSStringFromKString(Kotlin_toString([self toKotlin:h1.slot()], h2.slot()));
}

- (NSUInteger)hash {
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable);
    ObjHolder holder;
    return (NSUInteger)Kotlin_hashCode([self toKotlin:holder.slot()]);
}

- (BOOL)isEqual:(id)other {
    if (self == other) {
        return YES;
    }

    if (other == nil) {
        return NO;
    }

    // All `NSObject`'s, `__SwiftObject`'s and `NSProxy`-ies wrapping them should respond well to `toKotlin:`.
    // However, other system- or user- defined root classes may not.
    // But, at the very least, we expect them to conform to NSObject protocol. There's no test for that.
    if (![other respondsToSelector:Kotlin_ObjCExport_toKotlinSelector]) {
        return NO;
    }

    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable);
    ObjHolder lhsHolder;
    ObjHolder rhsHolder;
    KRef lhs = [self toKotlin:lhsHolder.slot()];
    KRef rhs = [other toKotlin:rhsHolder.slot()];
    return Kotlin_equals(lhs, rhs);
}

@end

@interface NSObject (NSObjectToKotlin)
@end

@implementation NSObject (NSObjectToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}
@end

@interface NSString (NSStringToKotlin)
@end

@implementation NSString (NSStringToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_Interop_CreateKStringFromNSString, self);
}
@end

extern "C" {

OBJ_GETTER(Kotlin_boxByte, KByte value);
OBJ_GETTER(Kotlin_boxShort, KShort value);
OBJ_GETTER(Kotlin_boxInt, KInt value);
OBJ_GETTER(Kotlin_boxLong, KLong value);
OBJ_GETTER(Kotlin_boxUByte, KUByte value);
OBJ_GETTER(Kotlin_boxUShort, KUShort value);
OBJ_GETTER(Kotlin_boxUInt, KUInt value);
OBJ_GETTER(Kotlin_boxULong, KULong value);
OBJ_GETTER(Kotlin_boxFloat, KFloat value);
OBJ_GETTER(Kotlin_boxDouble, KDouble value);

}

@interface NSNumber (NSNumberToKotlin)
@end

@implementation NSNumber (NSNumberToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  const char* type = self.objCType;

  {
    // Extracting known primitive numbers and boxing them should be fast enough.
    kotlin::CallsCheckerIgnoreGuard guard;

    // TODO: the code below makes some assumption on char, short, int and long sizes.
    switch (type[0]) {
      case 'c': RETURN_RESULT_OF(Kotlin_boxByte, self.charValue);
      case 's': RETURN_RESULT_OF(Kotlin_boxShort, self.shortValue);
      case 'i': RETURN_RESULT_OF(Kotlin_boxInt, self.intValue);
      case 'l': if constexpr (sizeof(long) == 8) {
                  RETURN_RESULT_OF(Kotlin_boxLong, self.longLongValue);
                } else {
                  RETURN_RESULT_OF(Kotlin_boxInt, self.intValue);
                }
      case 'q': RETURN_RESULT_OF(Kotlin_boxLong, self.longLongValue);
      case 'C': RETURN_RESULT_OF(Kotlin_boxUByte, self.unsignedCharValue);
      case 'S': RETURN_RESULT_OF(Kotlin_boxUShort, self.unsignedShortValue);
      case 'I': RETURN_RESULT_OF(Kotlin_boxUInt, self.unsignedIntValue);
      case 'L': if constexpr (sizeof(long) == 8) {
                  RETURN_RESULT_OF(Kotlin_boxULong, self.unsignedLongLongValue);
                } else {
                  RETURN_RESULT_OF(Kotlin_boxUInt, self.unsignedIntValue);
                }
      case 'Q': RETURN_RESULT_OF(Kotlin_boxULong, self.unsignedLongLongValue);
      case 'f': RETURN_RESULT_OF(Kotlin_boxFloat, self.floatValue);
      case 'd': RETURN_RESULT_OF(Kotlin_boxDouble, self.doubleValue);
    }
  }

  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}
@end

@interface NSDecimalNumber (NSDecimalNumberToKotlin)
@end

@implementation NSDecimalNumber (NSDecimalNumberToKotlin)
// Overrides [NSNumber toKotlin:] implementation.
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}
@end

static void injectToRuntimeImpl() {
  // If the code below fails, then it is most likely caused by KT-42254.
  constexpr const char* errorMessage = "runtime injected twice; https://youtrack.jetbrains.com/issue/KT-42254 might be related";

  RuntimeCheck(Kotlin_ObjCExport_toKotlinSelector == nullptr, errorMessage);
  Kotlin_ObjCExport_toKotlinSelector = @selector(toKotlin:);

  RuntimeCheck(Kotlin_ObjCExport_releaseAsAssociatedObjectSelector == nullptr, errorMessage);
  Kotlin_ObjCExport_releaseAsAssociatedObjectSelector = @selector(releaseAsAssociatedObject);
}

static void injectToRuntime() {
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    injectToRuntimeImpl();
  });
}

#endif // KONAN_OBJC_INTEROP
