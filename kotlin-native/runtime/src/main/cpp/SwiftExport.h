/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_SWIFTEXPORT_H
#define RUNTIME_SWIFTEXPORT_H

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>
#import <Foundation/Foundation.h>

#import "Types.h"
#import "Memory.h"

extern "C" {

id Kotlin_SwiftExport_refToSwiftObject(ObjHeader *obj);

OBJ_GETTER(Kotlin_SwiftExport_swiftObjectToRef, id obj);

} // extern "C"

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCEXPORT_H
