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

id Kotlin_Interop_CreateNSStringFromKString(const ObjHeader* str) {
  if (str == nullptr) {
    return nullptr;
  }

  const KChar* utf16Chars = CharArrayAddressOfElementAt(str->array(), 0);

  NSString* result = [[[getNSStringClass() alloc] initWithBytes:utf16Chars
    length:str->array()->count_*sizeof(KChar)
    encoding:NSUTF16LittleEndianStringEncoding] autorelease];

  return result;
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

OBJ_GETTER(Kotlin_Interop_ObjCToString, id <NSObject> ptr) {
  RETURN_RESULT_OF(Kotlin_Interop_CreateKStringFromNSString, ptr.description);
}

KInt Kotlin_Interop_ObjCHashCode(id <NSObject> ptr) {
  return (KInt) ptr.hash;
}

KBoolean Kotlin_Interop_ObjCEquals(id <NSObject> ptr, id otherPtr) {
  return [ptr isEqual:otherPtr];
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
  KRef ref_;
};

-(id)initWithRef:(KRef)ref {
  if (self = [super init]) {
    UpdateRef(&ref_, ref);
  }
  return self;
}

-(KRef)ref {
  return ref_;
}

-(void)dealloc {
  UpdateRef(&ref_, nullptr);
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

OBJ_GETTER(Kotlin_Interop_ObjCToString, KNativePtr ptr) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

KInt Kotlin_Interop_ObjCHashCode(KNativePtr ptr) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return 0;
}

KBoolean Kotlin_Interop_ObjCEquals(KNativePtr ptr, KNativePtr otherPtr) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return 0;
}

void* Kotlin_Interop_createKotlinObjectHolder(KRef any) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

KRef Kotlin_Interop_unwrapKotlinObjectHolder(void* holder) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

} // extern "C"

#endif // KONAN_OBJC_INTEROP