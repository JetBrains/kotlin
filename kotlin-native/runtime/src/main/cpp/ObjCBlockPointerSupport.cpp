/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <Block.h>

#include "ExternalRCRef.hpp"
#include "ObjCBlockPointerSupport.hpp"

using namespace kotlin;

extern "C" RUNTIME_NOTHROW id Kotlin_ObjCBlock_new(KRef kotlinFunction, void (*invoke)(void*, ...), Block_descriptor_1* descriptor) {
    if (!kotlinFunction)
        return nullptr;

    Kotlin_ObjCBlock blockOnStack;
    auto& blockOnStackBase = blockOnStack.literal;
    blockOnStackBase.isa = &_NSConcreteStackBlock;
    blockOnStackBase.flags = (1 << 25) | (1 << 30) | (1 << 31);
    blockOnStackBase.reserved = 0;
    blockOnStackBase.invoke = invoke;
    blockOnStackBase.descriptor = descriptor;
    blockOnStack.kotlinFunction = kotlinFunction;
    blockOnStack.ref = nullptr;
    return objc_retainBlock(reinterpret_cast<id>(&blockOnStack));
}

extern "C" RUNTIME_NOTHROW void Kotlin_ObjCBlock_dispose(Kotlin_ObjCBlock* block) {
    mm::releaseAndDisposeExternalRCRef(block->ref);
}

extern "C" RUNTIME_NOTHROW void Kotlin_ObjCBlock_copy(Kotlin_ObjCBlock* dst, Kotlin_ObjCBlock* src) {
    AssertThreadState(ThreadState::kRunnable);
    auto kotlinFunction = src->kotlinFunction;
    dst->kotlinFunction = kotlinFunction;
    dst->ref = mm::createRetainedExternalRCRef(kotlinFunction);
}

extern "C" RUNTIME_NOTHROW KRef Kotlin_ObjCBlock_getKotlinFunction(Kotlin_ObjCBlock* block) {
    AssertThreadState(ThreadState::kRunnable);
    return block->kotlinFunction;
}
