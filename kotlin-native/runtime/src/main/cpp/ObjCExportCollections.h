/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

#ifndef RUNTIME_OBJCEXPORTCOLLECTIONS_H
#define RUNTIME_OBJCEXPORTCOLLECTIONS_H

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>
#import <Foundation/NSNull.h>

#import "Memory.h"
#import "ObjCExport.h"
#import "Runtime.h"

// Objective-C collections can't store `nil`, and the common convention is to use `NSNull.null` instead.
// Follow the convention when converting Kotlin `null`:

static inline id refToObjCOrNSNull(KRef obj) {
  if (obj == nullptr) {
    return NSNull.null;
  } else {
    return Kotlin_ObjCExport_refToObjC(obj);
  }
}

static inline OBJ_GETTER(refFromObjCOrNSNull, id obj) {
  if (obj == NSNull.null) {
    RETURN_OBJ(nullptr);
  } else {
    RETURN_RESULT_OF(Kotlin_ObjCExport_refFromObjC, obj);
  }
}

static inline OBJ_GETTER(invokeAndAssociate, KRef (*func)(KRef* result), id obj) {
  Kotlin_initRuntimeIfNeeded();

  KRef kotlinObj = func(OBJ_RESULT);

  SetAssociatedObject(kotlinObj, obj);

  return kotlinObj;
}

#endif // KONAN_OBJC_INTEROP
#endif // RUNTIME_OBJCEXPORTCOLLECTIONS_H