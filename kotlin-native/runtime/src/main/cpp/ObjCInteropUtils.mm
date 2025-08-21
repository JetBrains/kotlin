/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Natives.h"

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>
#import <CoreFoundation/CoreFoundation.h>
#import <Foundation/Foundation.h>
#import "CompilerConstants.hpp"
#import "Memory.h"
#import "KString.h"
#import "ObjCInteropUtils.h"
#import "ObjCInteropUtilsPrivate.h"

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

id Kotlin_ObjCExport_CreateRetainedNSStringFromKString(ObjHeader* str);

extern "C" id objc_autorelease(id self);

id Kotlin_Interop_CreateNSStringFromKString(ObjHeader* str) {
  // Note: this function is just a bit specialized [Kotlin_Interop_refToObjC].
  if (str == nullptr) {
    return nullptr;
  }

  if (void* associatedObject = str->GetAssociatedObject()) {
    return (id)associatedObject;
  }

  return objc_autorelease(Kotlin_ObjCExport_CreateRetainedNSStringFromKString(str));
}

OBJ_GETTER(Kotlin_Interop_CreateKStringFromNSString, NSString* str) {
  if (str == nullptr) {
    RETURN_OBJ(nullptr);
  }

  KRef result;
  auto immutableCopyOrSameStr = CFStringCreateCopy(nullptr, (CFStringRef)str);
  auto length = CFStringGetLength(immutableCopyOrSameStr);
  if (length == 0) RETURN_RESULT_OF0(TheEmptyString);

  auto encoding = CFStringGetFastestEncoding(immutableCopyOrSameStr);
  switch (encoding) {
    case kCFStringEncodingASCII:
    case kCFStringEncodingNonLossyASCII:
    case kCFStringEncodingISOLatin1:
      if (kotlin::compiler::latin1Strings()) {
        result = CreateUninitializedString(StringEncoding::kLatin1, length, OBJ_RESULT);
        CFStringGetBytes(immutableCopyOrSameStr, {0, length}, encoding, '?', false,
          reinterpret_cast<UInt8*>(StringHeader::of(result)->data()), length, nullptr);
        break;
      }
    default:
      result = CreateUninitializedString(StringEncoding::kUTF16, length, OBJ_RESULT);
      CFStringGetCharacters(immutableCopyOrSameStr, {0, length},
        reinterpret_cast<UniChar*>(StringHeader::of(result)->data()));
      break;
  }
  result->SetAssociatedObject((void*)immutableCopyOrSameStr);
  RETURN_OBJ(result);
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

// Initialized in [ObjCInteropUtilsClasses.mm].
id (*Kotlin_Interop_createKotlinObjectHolder_ptr)(KRef any) = nullptr;
KRef (*Kotlin_Interop_unwrapKotlinObjectHolder_ptr)(id holder) = nullptr;

id Kotlin_Interop_createKotlinObjectHolder(KRef any) {
  return Kotlin_Interop_createKotlinObjectHolder_ptr(any);
}

KRef Kotlin_Interop_unwrapKotlinObjectHolder(id holder) {
  return Kotlin_Interop_unwrapKotlinObjectHolder_ptr(holder);
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

OBJ_GETTER((*Konan_ObjCInterop_getWeakReference_ptr), KRef ref) = nullptr;
void (*Konan_ObjCInterop_initWeakReference_ptr)(KRef ref, id objcPtr) = nullptr;

OBJ_GETTER(Konan_ObjCInterop_getWeakReference, KRef ref) {
  RETURN_RESULT_OF(Konan_ObjCInterop_getWeakReference_ptr, ref);
}

void Konan_ObjCInterop_initWeakReference(KRef ref, id objcPtr) {
  Konan_ObjCInterop_initWeakReference_ptr(ref, objcPtr);
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
