/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJCINTEROPUTILS_H
#define RUNTIME_OBJCINTEROPUTILS_H

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>
#import "Types.h"

extern "C" {
id MissingInitImp(id self, SEL _cmd);
KBoolean Kotlin_Interop_DoesObjectConformToProtocol(id obj, void* prot, KBoolean isMeta);
KBoolean Kotlin_Interop_IsObjectKindOfClass(id obj, void* cls);
}

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCINTEROPUTILS_H