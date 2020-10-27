/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>
#import <Foundation/NSObject.h>
#import "Memory.h"
#import "MemorySharedRefs.hpp"
#import "ObjCInteropUtilsPrivate.h"

// TODO: rework the interface to reduce the number of virtual calls
// in Kotlin_Interop_createKotlinObjectHolder and Kotlin_Interop_unwrapKotlinObjectHolder
@interface KotlinObjectHolder : NSObject
-(id)initWithRef:(KRef)ref;
-(KRef)ref;
@end;

@implementation KotlinObjectHolder {
  KRefSharedHolder refHolder;
};

-(id)initWithRef:(KRef)ref {
  if (self = [super init]) {
    refHolder.init(ref);
  }
  return self;
}

-(KRef)ref {
  return refHolder.ref<ErrorPolicy::kTerminate>();
}

-(void)dealloc {
  refHolder.dispose();
  [super dealloc];
}

@end;

static id Kotlin_Interop_createKotlinObjectHolder(KRef any) {
  if (any == nullptr) {
    return nullptr;
  }

  return [[[KotlinObjectHolder alloc] initWithRef:any] autorelease];
}

static KRef Kotlin_Interop_unwrapKotlinObjectHolder(id holder) {
  if (holder == nullptr) {
    return nullptr;
  }

  return [((KotlinObjectHolder*)holder) ref];
}

// Used as an associated object for ObjCWeakReferenceImpl.
@interface KotlinObjCWeakReference : NSObject
@end;

// libobjc:
extern "C" {
id objc_loadWeakRetained(id *location);
id objc_storeWeak(id *location, id newObj);
void objc_destroyWeak(id *location);
void objc_release(id obj);
}

@implementation KotlinObjCWeakReference {
  @public id referred;
}

// Called when removing Kotlin object.
-(void)releaseAsAssociatedObject {
  objc_destroyWeak(&referred);
  objc_release(self);
}

@end;

extern "C" OBJ_GETTER(Kotlin_Interop_refFromObjC, id obj);

static OBJ_GETTER(Konan_ObjCInterop_getWeakReference, KRef ref) {
  KotlinObjCWeakReference* objcRef = (KotlinObjCWeakReference*)ref->GetAssociatedObject();

  id objcReferred = objc_loadWeakRetained(&objcRef->referred);
  KRef result = Kotlin_Interop_refFromObjC(objcReferred, OBJ_RESULT);
  objc_release(objcReferred);

  return result;
}

static void Konan_ObjCInterop_initWeakReference(KRef ref, id objcPtr) {
  KotlinObjCWeakReference* objcRef = [KotlinObjCWeakReference new];
  objc_storeWeak(&objcRef->referred, objcPtr);
  ref->SetAssociatedObject(objcRef);
}

__attribute__((constructor))
static void injectToRuntime() {
  RuntimeCheck(Kotlin_Interop_createKotlinObjectHolder_ptr == nullptr, "runtime injected twice");
  Kotlin_Interop_createKotlinObjectHolder_ptr = &Kotlin_Interop_createKotlinObjectHolder;

  RuntimeCheck(Kotlin_Interop_unwrapKotlinObjectHolder_ptr == nullptr, "runtime injected twice");
  Kotlin_Interop_unwrapKotlinObjectHolder_ptr = &Kotlin_Interop_unwrapKotlinObjectHolder;

  RuntimeCheck(Konan_ObjCInterop_getWeakReference_ptr == nullptr, "runtime injected twice");
  Konan_ObjCInterop_getWeakReference_ptr = &Konan_ObjCInterop_getWeakReference;

  RuntimeCheck(Konan_ObjCInterop_initWeakReference_ptr == nullptr, "runtime injected twice");
  Konan_ObjCInterop_initWeakReference_ptr = &Konan_ObjCInterop_initWeakReference;
}

#endif // KONAN_OBJC_INTEROP
