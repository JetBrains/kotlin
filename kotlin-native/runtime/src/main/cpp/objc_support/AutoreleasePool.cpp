/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AutoreleasePool.hpp"
#include "Memory.h"

using namespace kotlin;

#if KONAN_OBJC_INTEROP

// Obj-C runtime provided by clang.
extern "C" void* objc_autoreleasePoolPush(void);
extern "C" void objc_autoreleasePoolPop(void*);

objc_support::AutoreleasePool::AutoreleasePool() noexcept : handle_(objc_autoreleasePoolPush()) {}

objc_support::AutoreleasePool::~AutoreleasePool() {
    NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
    objc_autoreleasePoolPop(handle_);
}

#else

objc_support::AutoreleasePool::AutoreleasePool() noexcept = default;
objc_support::AutoreleasePool::~AutoreleasePool() = default;

#endif