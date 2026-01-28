/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_OBJCEXPORTINIT_H
#define RUNTIME_OBJCEXPORTINIT_H

#if KONAN_OBJC_INTEROP

extern "C" void Kotlin_ObjCExport_initialize(void);

// Initialize type adapters for ObjC export.
// This can be called after JIT loading to set up type adapters that are
// defined in user code (which may be loaded after the initial runtime init).
// This is specifically needed for hot reload scenarios.
extern "C" void Kotlin_ObjCExport_initializeTypeAdapters(void);

// Forward declaration of ObjCTypeAdapter (defined in compiler-generated code)
struct ObjCTypeAdapter;

// Initialize type adapters with explicitly provided adapter arrays.
// This is used by hot reload to pass adapters looked up from JIT'd code.
// Parameters:
//   classAdapters - pointer to array of class adapters
//   classAdaptersNum - number of class adapters
//   protocolAdapters - pointer to array of protocol adapters
//   protocolAdaptersNum - number of protocol adapters
extern "C" void Kotlin_ObjCExport_initializeTypeAdaptersWithPointers(
    const ObjCTypeAdapter** classAdapters, int classAdaptersNum,
    const ObjCTypeAdapter** protocolAdapters, int protocolAdaptersNum);

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCEXPORTINIT_H
