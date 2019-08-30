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

#include "Natives.h"

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>
#import <Foundation/NSException.h>
#import <Foundation/NSString.h>
#import "Memory.h"
#import "MemorySharedRefs.hpp"

namespace {
  Class nsStringClass = nullptr;

  Class getNSStringClass() {
    Class result = nsStringClass;
    if (result == nullptr) {
      // Lookup dynamically to avoid direct reference to Foundation:
      result = objc_getClass("NSString");
      RuntimeAssert(result != nullptr, "NSString class not found");
      nsStringClass = result;
    }
    return result;
  }

  // Note: using @"foo" string literals leads to linkage dependency on frameworks.
  NSString* cStringToNS(const char* str) {
    return [getNSStringClass() stringWithCString:str encoding:NSUTF8StringEncoding];
  }
}

extern "C" {

id Kotlin_ObjCExport_CreateNSStringFromKString(ObjHeader* str);

id Kotlin_Interop_CreateNSStringFromKString(ObjHeader* str) {
  // Note: this function is just a bit specialized [Kotlin_Interop_refToObjC].
  if (str == nullptr) {
    return nullptr;
  }

  if (str->has_meta_object()) {
    void* associatedObject = str->meta_object()->associatedObject_;
    if (associatedObject != nullptr) {
      return (id)associatedObject;
    }
  }

  return Kotlin_ObjCExport_CreateNSStringFromKString(str);
}

OBJ_GETTER(Kotlin_Interop_CreateKStringFromNSString, NSString* str) {
  if (str == nullptr) {
    RETURN_OBJ(nullptr);
  }

  size_t length = [str length];
  NSRange range = {0, length};
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, length, OBJ_RESULT)->array();
  KChar* rawResult = CharArrayAddressOfElementAt(result, 0);

  [str getCharacters:rawResult range:range];

  RETURN_OBJ(result->obj());
}

// Note: this body is used for init methods with signatures differing from this;
// it is correct on arm64 and x86_64, because the body uses only the first two arguments which are fixed,
// and returns pointers.
id MissingInitImp(id self, SEL _cmd) {
  const char* className = object_getClassName(self);
  [self release]; // Since init methods receive ownership on the receiver.

  // Lookup dynamically to avoid direct reference to Foundation:
  Class nsExceptionClass = objc_getClass("NSException");
  RuntimeAssert(nsExceptionClass != nullptr, "NSException class not found");

  [nsExceptionClass raise:cStringToNS("Initializer is not implemented")
    format:cStringToNS("%s is not implemented in %s"),
    sel_getName(_cmd), className];

  return nullptr;
}

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
  return refHolder.ref();
}

-(void)dealloc {
  refHolder.dispose();
  [super dealloc];
}

@end;

id Kotlin_Interop_createKotlinObjectHolder(KRef any) {
  if (any == nullptr) {
    return nullptr;
  }

  return [[[KotlinObjectHolder alloc] initWithRef:any] autorelease];
}

KRef Kotlin_Interop_unwrapKotlinObjectHolder(id holder) {
  if (holder == nullptr) {
    return nullptr;
  }

  return [((KotlinObjectHolder*)holder) ref];
}

KBoolean Kotlin_Interop_DoesObjectConformToProtocol(id obj, void* prot, KBoolean isMeta) {
  BOOL objectIsClass = class_isMetaClass(object_getClass(obj));
  if ((isMeta && !objectIsClass) || (!isMeta && objectIsClass)) return false;
  // TODO: handle root classes properly.

  return [((id<NSObject>)obj) conformsToProtocol:(Protocol*)prot];
}

KBoolean Kotlin_Interop_IsObjectKindOfClass(id obj, void* cls) {
  return [((id<NSObject>)obj) isKindOfClass:(Class)cls];
}

// Used as an associated object for ObjCWeakReferenceImpl.
@interface KotlinObjCWeakReference : NSObject
@end;

// libobjc:
id objc_loadWeakRetained(id *location);
id objc_storeWeak(id *location, id newObj);
void objc_destroyWeak(id *location);
void objc_release(id obj);

@implementation KotlinObjCWeakReference {
  @public id referred;
}

// Called when removing Kotlin object.
-(void)releaseAsAssociatedObject {
  objc_destroyWeak(&referred);
  objc_release(self);
}

@end;

OBJ_GETTER(Kotlin_Interop_refFromObjC, id obj);

OBJ_GETTER(Konan_ObjCInterop_getWeakReference, KRef ref) {
  MetaObjHeader* meta = ref->meta_object();
  KotlinObjCWeakReference* objcRef = (KotlinObjCWeakReference*)meta->associatedObject_;

  id objcReferred = objc_loadWeakRetained(&objcRef->referred);
  KRef result = Kotlin_Interop_refFromObjC(objcReferred, OBJ_RESULT);
  objc_release(objcReferred);

  return result;
}

void Konan_ObjCInterop_initWeakReference(KRef ref, id objcPtr) {
  MetaObjHeader* meta = ref->meta_object();
  KotlinObjCWeakReference* objcRef = [KotlinObjCWeakReference new];
  objc_storeWeak(&objcRef->referred, objcPtr);
  meta->associatedObject_ = objcRef;
}

} // extern "C"

#else // KONAN_OBJC_INTEROP

extern "C" {

void* Kotlin_Interop_CreateNSStringFromKString(const ArrayHeader* str) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

OBJ_GETTER(Kotlin_Interop_CreateKStringFromNSString, void* str) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

void* Kotlin_Interop_createKotlinObjectHolder(KRef any) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

KRef Kotlin_Interop_unwrapKotlinObjectHolder(void* holder) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}
  
OBJ_GETTER(Konan_ObjCInterop_getWeakReference, KRef ref) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

void Konan_ObjCInterop_initWeakReference(KRef ref, void* objcPtr) {
  RuntimeAssert(false, "Objective-C interop is disabled");
}

} // extern "C"

#endif // KONAN_OBJC_INTEROP
