/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_OBJC_INTEROP

#include <objc/objc.h>

#include "ExternalRCRef.hpp"

extern "C" {

struct Block_descriptor_1;

// Based on https://clang.llvm.org/docs/Block-ABI-Apple.html and libclosure source.
struct Block_literal_1 {
    void *isa; // initialized to &_NSConcreteStackBlock or &_NSConcreteGlobalBlock
    int flags;
    int reserved;
    void (*invoke)(void *, ...);
    struct Block_descriptor_1  *descriptor; // IFF (1<<25)
    // Or:
    // struct Block_descriptor_1_without_helpers* descriptor // if hasn't (1<<25).

    // imported variables
};

struct Block_descriptor_1 {
    unsigned long int reserved;         // NULL
    unsigned long int size;             // sizeof(struct Block_literal_1)

    // optional helper functions
    void (*copy_helper)(void *dst, void *src);
    void (*dispose_helper)(void *src);
    // required ABI.2010.3.16
    const char *signature;                         // IFF (1<<30)
    const void* layout;                            // IFF (1<<31)
};

struct Block_descriptor_1_without_helpers {
    unsigned long int reserved;         // NULL
    unsigned long int size;             // sizeof(struct Block_literal_1)

    // required ABI.2010.3.16
    const char *signature;                         // IFF (1<<30)
    const void* layout;                            // IFF (1<<31)
};

id objc_retainBlock(id self);

struct Kotlin_ObjCBlock {
    Block_literal_1 literal;
    KRef kotlinFunction;
    kotlin::mm::RawExternalRCRef* ref;
};

static_assert(std::is_trivially_destructible_v<Kotlin_ObjCBlock>, "Kotlin_ObjCBlock destructor is not guaranteed to be called.");

RUNTIME_NOTHROW id Kotlin_ObjCBlock_new(KRef kotlinFunction, void (*invoke)(void*, ...), Block_descriptor_1* descriptor);
RUNTIME_NOTHROW void Kotlin_ObjCBlock_dispose(Kotlin_ObjCBlock* block);
RUNTIME_NOTHROW void Kotlin_ObjCBlock_copy(Kotlin_ObjCBlock* dst, Kotlin_ObjCBlock* src);
RUNTIME_NOTHROW KRef Kotlin_ObjCBlock_getKotlinFunction(Kotlin_ObjCBlock* block);

} // extern "C"

#endif
