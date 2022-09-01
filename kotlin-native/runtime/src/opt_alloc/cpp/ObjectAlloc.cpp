/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectAlloc.hpp"

#include <mutex>

#include "../../mimalloc/c/include/mimalloc.h"
#include "Alignment.hpp"
#include "CompilerConstants.hpp"

using namespace kotlin;

namespace {

std::once_flag initOptions;

}

void kotlin::initObjectPool() noexcept {
    if (!compiler::mimallocUseDefaultOptions()) {
        std::call_once(initOptions, [] { mi_option_enable(mi_option_reset_decommits); });
    }
    mi_thread_init();
}

void* kotlin::allocateInObjectPool(size_t size) noexcept {
    return mi_calloc_aligned(1, size, kObjectAlignment);
}

void kotlin::freeInObjectPool(void* ptr) noexcept {
    mi_free(ptr);
}
