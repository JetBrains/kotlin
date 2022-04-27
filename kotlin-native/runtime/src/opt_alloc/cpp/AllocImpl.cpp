/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstddef>

extern "C" {
void* mi_malloc(size_t size);
void* mi_malloc_aligned(size_t size, size_t alignment);
void* mi_calloc(size_t count, size_t size);
void* mi_calloc_aligned(size_t count, size_t size, size_t alignment);
void* mi_realloc(void* ptr, size_t size);
void mi_free(void* ptr);
}

extern "C" void* konan_malloc_impl(size_t size) {
    return mi_malloc(size);
}

extern "C" void* konan_aligned_alloc_impl(size_t alignment, size_t size) {
    return mi_malloc_aligned(size, alignment);
}

extern "C" void* konan_calloc_impl(size_t num, size_t size) {
    return mi_calloc(num, size);
}

extern "C" void* konan_aligned_calloc_impl(size_t alignment, size_t num, size_t size) {
    return mi_calloc_aligned(num, size, alignment);
}

extern "C" void* konan_realloc_impl(void* ptr, size_t size) {
    return mi_realloc(ptr, size);
}

extern "C" void konan_free_impl(void* ptr) {
    return mi_free(ptr);
}
