/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <type_traits>

namespace kotlin {

void initObjectPool() noexcept;
void* allocateInObjectPool(size_t size) noexcept;
void freeInObjectPool(void* ptr, size_t size) noexcept;
// Instruct the allocator to free unused resources.
void compactObjectPoolInCurrentThread() noexcept;
// Platform dependent. Schedule `compactObjectPoolInCurrentThread` on the main thread.
// May do nothing if the main thread is not an event loop.
void compactObjectPoolInMainThread() noexcept;

size_t allocatedBytes() noexcept;

template <typename T>
struct ObjectPoolAllocator {
    using value_type = T;
    using size_type = std::size_t;
    using difference_type = std::ptrdiff_t;
    using propagate_on_container_move_assignment = std::true_type;
    using is_always_equal = std::true_type;

    constexpr ObjectPoolAllocator() noexcept = default;

    constexpr ObjectPoolAllocator(const ObjectPoolAllocator&) noexcept = default;

    template <typename U>
    constexpr ObjectPoolAllocator(const ObjectPoolAllocator<U>&) noexcept {}

    T* allocate(std::size_t n) noexcept { return static_cast<T*>(allocateInObjectPool(n * sizeof(T))); }

    void deallocate(T* p, std::size_t n) noexcept { freeInObjectPool(p, n * sizeof(T)); }
};

template <typename T, typename U>
constexpr bool operator==(const ObjectPoolAllocator<T>&, const ObjectPoolAllocator<U>&) noexcept {
    return true;
}

template <typename T, typename U>
constexpr bool operator!=(const ObjectPoolAllocator<T>&, const ObjectPoolAllocator<U>&) noexcept {
    return false;
}

}
