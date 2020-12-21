/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_ALIGNMENT_H
#define RUNTIME_ALIGNMENT_H

#include <cstddef>
#include <cstdint>

namespace kotlin {

constexpr size_t kObjectAlignment = 8;

constexpr inline size_t AlignUp(size_t size, size_t alignment) {
    return (size + alignment - 1) & ~(alignment - 1);
}

inline void* AlignUp(void* ptr, size_t alignment) {
    static_assert(sizeof(void*) == sizeof(size_t), "size_t size must be equal to pointer size for this to work");
    return reinterpret_cast<void*>(AlignUp(reinterpret_cast<size_t>(ptr), alignment));
}

constexpr inline bool IsValidAlignment(size_t alignment) {
    return alignment != 0 && (alignment & (alignment - 1)) == 0;
}

constexpr inline bool IsAligned(size_t size, size_t alignment) {
    return size % alignment == 0;
}

inline bool IsAligned(void* ptr, size_t alignment) {
    return reinterpret_cast<uintptr_t>(ptr) % alignment == 0;
}

} // namespace kotlin

#endif // RUNTIME_ALIGNMENT_H
