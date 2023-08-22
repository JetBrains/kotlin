/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#import "Types.h"
#import "Memory.h"
#include "ObjCInterop.h"
#include "KString.h"

#if KONAN_OBJC_INTEROP

#import <mutex>

#import <Foundation/Foundation.h>

#import "SwiftExport.h"
#import "ObjCExport.h"
#import "Memory.h"

using namespace kotlin;

extern "C" id Kotlin_SwiftExport_refToSwiftObject(ObjHeader *obj) {
    return Kotlin_ObjCExport_refToObjC(obj); // FIXME: For now, we just return objc counterparts
}

extern "C" OBJ_GETTER(Kotlin_SwiftExport_swiftObjectToRef, id obj) {
    RETURN_RESULT_OF(Kotlin_ObjCExport_refFromObjC, obj); // FIXME: For now, we just unwrap objc counterparts
}

#endif // KONAN_OBJC_INTEROP
