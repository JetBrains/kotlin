/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJCEXPORTINIT_H
#define RUNTIME_OBJCEXPORTINIT_H

#if KONAN_OBJC_INTEROP

extern "C" void Kotlin_ObjCExport_initialize(void);

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCEXPORTINIT_H
