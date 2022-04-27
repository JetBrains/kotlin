/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdlib>

extern "C" void* konan_malloc_impl(size_t size) {
    return ::malloc(size);
}

extern "C" void* konan_aligned_alloc_impl(size_t alignment, size_t size) {
    return ::malloc(size);
}

extern "C" void* konan_calloc_impl(size_t num, size_t size) {
    return ::calloc(num, size);
}

extern "C" void* konan_aligned_calloc_impl(size_t alignment, size_t num, size_t size) {
    return ::calloc(num, size);
}

extern "C" void* konan_realloc_impl(void* ptr, size_t size) {
    return ::realloc(ptr, size);
}

extern "C" void konan_free_impl(void* ptr) {
    return ::free(ptr);
}
