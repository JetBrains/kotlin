/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJCINTEROPUTILSPRIVATE_H
#define RUNTIME_OBJCINTEROPUTILSPRIVATE_H

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>

#import "Types.h"
#import "Memory.h"

extern "C" id (*Kotlin_Interop_createKotlinObjectHolder_ptr)(KRef any);
extern "C" KRef (*Kotlin_Interop_unwrapKotlinObjectHolder_ptr)(id holder);
extern "C" OBJ_GETTER((*Konan_ObjCInterop_getWeakReference_ptr), KRef ref);
extern "C" void (*Konan_ObjCInterop_initWeakReference_ptr)(KRef ref, id objcPtr);

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCINTEROPUTILSPRIVATE_H