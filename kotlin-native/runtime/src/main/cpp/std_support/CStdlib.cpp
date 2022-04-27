/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/CStdlib.hpp"

#include <cstdint>
#include <unistd.h>

using namespace kotlin;

#if KONAN_INTERNAL_DLMALLOC
extern "C" void* dlmalloc(size_t);
extern "C" void* dlcalloc(size_t, size_t);
extern "C" void* dlrealloc(void*, size_t);
extern "C" void dlfree(void*);
#define malloc_impl dlmalloc
#define aligned_alloc_impl(alignment, size) dlmalloc(size)
#define calloc_impl dlcalloc
#define aligned_calloc_impl(alignment, num, size) dlcalloc(num, size)
#define realloc_impl dlrealloc
#define free_impl dlfree
#else
extern "C" void* konan_malloc_impl(size_t);
extern "C" void* konan_aligned_alloc_impl(size_t, size_t);
extern "C" void* konan_calloc_impl(size_t, size_t);
extern "C" void* konan_aligned_calloc_impl(size_t, size_t, size_t);
extern "C" void* konan_realloc_impl(void*, size_t);
extern "C" void konan_free_impl(void*);
#define malloc_impl konan_malloc_impl
#define aligned_alloc_impl konan_aligned_alloc_impl
#define calloc_impl konan_calloc_impl
#define aligned_calloc_impl konan_aligned_calloc_impl
#define realloc_impl konan_realloc_impl
#define free_impl konan_free_impl
#endif

void* std_support::malloc(std::size_t size) noexcept {
    return malloc_impl(size);
}

void* std_support::aligned_alloc(std::size_t alignment, std::size_t size) noexcept {
    return aligned_alloc_impl(alignment, size);
}

void* std_support::calloc(std::size_t num, std::size_t size) noexcept {
    return calloc_impl(num, size);
}

void* std_support::aligned_calloc(std::size_t alignment, std::size_t num, std::size_t size) noexcept {
    return aligned_calloc_impl(alignment, num, size);
}

void* std_support::realloc(void* ptr, std::size_t size) noexcept {
    return realloc_impl(ptr, size);
}

void std_support::free(void* ptr) noexcept {
    return free_impl(ptr);
}

namespace konan {

#if KONAN_INTERNAL_DLMALLOC
// This function is being called when memory allocator needs more RAM.

#if KONAN_WASM

namespace {

constexpr uint32_t MFAIL = ~(uint32_t)0;
constexpr uint32_t WASM_PAGESIZE_EXPONENT = 16;
constexpr uint32_t WASM_PAGESIZE = 1u << WASM_PAGESIZE_EXPONENT;
constexpr uint32_t WASM_PAGEMASK = WASM_PAGESIZE - 1;

uint32_t pageAlign(int32_t value) {
    return (value + WASM_PAGEMASK) & ~(WASM_PAGEMASK);
}

uint32_t inBytes(uint32_t pageCount) {
    return pageCount << WASM_PAGESIZE_EXPONENT;
}

uint32_t inPages(uint32_t value) {
    return value >> WASM_PAGESIZE_EXPONENT;
}

extern "C" void Konan_notify_memory_grow();

uint32_t memorySize() {
    return __builtin_wasm_memory_size(0);
}

int32_t growMemory(uint32_t delta) {
    int32_t oldLength = __builtin_wasm_memory_grow(0, delta);
    Konan_notify_memory_grow();
    return oldLength;
}

} // namespace

void* moreCore(int32_t delta) {
    uint32_t top = inBytes(memorySize());
    if (delta > 0) {
        if (growMemory(inPages(pageAlign(delta))) == 0) {
            return (void*)MFAIL;
        }
    } else if (delta < 0) {
        return (void*)MFAIL;
    }
    return (void*)top;
}

// dlmalloc() wants to know the page size.
long getpagesize() {
    return WASM_PAGESIZE;
}

#else
void* moreCore(int size) {
    return sbrk(size);
}

long getpagesize() {
    return sysconf(_SC_PAGESIZE);
}
#endif
#endif

} // namespace konan
