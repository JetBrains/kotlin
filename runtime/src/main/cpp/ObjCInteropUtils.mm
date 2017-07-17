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
}

extern "C" {

NSString* CreateNSStringFromKString(const ArrayHeader* str) {
  if (str == nullptr) {
    return nullptr;
  }

  const KChar* utf16Chars = CharArrayAddressOfElementAt(str, 0);

  NSString* result = [[[getNSStringClass() alloc] initWithBytes:utf16Chars
    length:str->count_*sizeof(KChar)
    encoding:NSUTF16LittleEndianStringEncoding] autorelease];

  return result;
}

OBJ_GETTER(CreateKStringFromNSString, NSString* str) {
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

} // extern "C"

#else // KONAN_OBJC_INTEROP

extern "C" {

void* CreateNSStringFromKString(const ArrayHeader* str) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return nullptr;
}

OBJ_GETTER(CreateKStringFromNSString, void* str) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

} // extern "C"

#endif // KONAN_OBJC_INTEROP