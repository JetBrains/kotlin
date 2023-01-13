/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CustomLogging.hpp"

// These functions are just stubs to make the existing object creation
// infrastructure link correctly, but if they are ever called, something went
// wrong.

namespace kotlin {

void* allocateInObjectPool(size_t size) noexcept {
    CustomAllocWarning("static allocateInObjectPool(%zu) not supported", size);
    return nullptr;
}

void freeInObjectPool(void* ptr) noexcept {
    CustomAllocWarning("static freeInObjectPool(%p) not supported", ptr);
}

void initObjectPool() noexcept {}
void compactObjectPoolInMainThread() noexcept {}
void compactObjectPoolInCurrentThread() noexcept {}

} // namespace kotlin
