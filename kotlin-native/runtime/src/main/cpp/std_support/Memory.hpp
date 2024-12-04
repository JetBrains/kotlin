/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <memory>
#include <type_traits>

namespace kotlin::std_support {

template <typename T, typename Allocator, typename... Args>
T* allocator_new(const Allocator& allocator, Args&&... args) {
    static_assert(!std::is_array_v<T>, "T cannot be an array");

    using TAllocatorTraits = typename std::allocator_traits<Allocator>::template rebind_traits<T>;
    using TAllocator = typename std::allocator_traits<Allocator>::template rebind_alloc<T>;

    auto a = TAllocator(allocator);
    T* ptr = TAllocatorTraits::allocate(a, 1);

    try {
        TAllocatorTraits::construct(a, ptr, std::forward<Args>(args)...);
        return ptr;
    } catch (...) {
        TAllocatorTraits::deallocate(a, ptr, 1);
        throw;
    }
}

template <typename T, typename Allocator>
void allocator_delete(const Allocator& allocator, T* ptr) noexcept {
    static_assert(!std::is_array_v<T>, "T cannot be an array");

    using TAllocatorTraits = typename std::allocator_traits<Allocator>::template rebind_traits<T>;
    using TAllocator = typename std::allocator_traits<Allocator>::template rebind_alloc<T>;

    auto a = TAllocator(allocator);
    TAllocatorTraits::destroy(a, ptr);
    TAllocatorTraits::deallocate(a, ptr, 1);
}

template <typename T, typename Allocator>
class allocator_deleter {
    static_assert(!std::is_array_v<T>, "T cannot be an array");

    template <typename A, typename V>
    using Rebind = typename std::allocator_traits<A>::template rebind_alloc<V>;

    template <typename A1, typename A2>
    static inline constexpr bool allocatorsCompatible = std::is_same_v<Rebind<A1, T>, Rebind<A2, T>>;

public:
    allocator_deleter() noexcept = default;

    explicit allocator_deleter(const Allocator& allocator) noexcept : allocator(allocator) {}

    template <
            typename U,
            typename Other,
            typename = std::enable_if_t<std::is_convertible_v<U*, T*> && allocatorsCompatible<Allocator, Other>>>
    allocator_deleter(const allocator_deleter<U, Other>& rhs) noexcept : allocator(rhs.allocator) {}

    void operator()(T* ptr) noexcept { allocator_delete(allocator, ptr); }

    [[no_unique_address]] Allocator allocator;
};

template <typename T, typename Allocator, typename... Args>
auto allocate_unique(const Allocator& allocator, Args&&... args) {
    static_assert(!std::is_array_v<T>, "T cannot be an array");

    using TAllocator = typename std::allocator_traits<Allocator>::template rebind_alloc<T>;
    using TDeleter = allocator_deleter<T, TAllocator>;
    return std::unique_ptr<T, TDeleter>(allocator_new<T>(allocator, std::forward<Args>(args)...), TDeleter(allocator));
}

template <typename T, typename Allocator>
auto nullptr_unique(const Allocator& allocator = Allocator()) noexcept {
    static_assert(!std::is_array_v<T>, "T cannot be an array");

    using TAllocator = typename std::allocator_traits<Allocator>::template rebind_alloc<T>;
    using TDeleter = allocator_deleter<T, TAllocator>;
    return std::unique_ptr<T, TDeleter>(nullptr, TDeleter(allocator));
}

} // namespace kotlin::std_support
