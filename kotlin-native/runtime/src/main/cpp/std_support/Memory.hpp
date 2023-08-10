/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <memory>
#include <type_traits>

#include "std_support/CStdlib.hpp"

namespace kotlin::std_support {

// Default allocator for Kotlin.
// TODO: Consider overriding global operator new and operator delete instead. However, make sure this does
//       not extend over to interop.
template <typename T>
struct allocator {
    using value_type = T;
    using size_type = std::size_t;
    using difference_type = std::ptrdiff_t;
    using propagate_on_container_move_assignment = std::true_type;
    using is_always_equal = std::true_type;

    allocator() noexcept = default;

    allocator(const allocator&) noexcept = default;

    template <typename U>
    allocator(const allocator<U>&) noexcept {}

    // TODO: maybe malloc, actually?
    T* allocate(std::size_t n) noexcept { return static_cast<T*>(std_support::calloc(n, sizeof(T))); }

    void deallocate(T* p, std::size_t n) noexcept { std_support::free(p); }
};

template <typename T, typename U>
bool operator==(const allocator<T>&, const allocator<U>&) noexcept {
    return true;
}

template <typename T, typename U>
bool operator!=(const allocator<T>&, const allocator<U>&) noexcept {
    return false;
}

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

template <typename T>
using default_delete = allocator_deleter<T, allocator<T>>;

template <typename T, typename Deleter = default_delete<T>>
using unique_ptr = std::unique_ptr<T, Deleter>;

template <typename T, typename... Args>
auto make_unique(Args&&... args) {
    static_assert(!std::is_array_v<T>, "T cannot be an array");

    return allocate_unique<T>(allocator<T>(), std::forward<Args>(args)...);
}

template <typename T, typename... Args>
auto make_shared(Args&&... args) {
    static_assert(!std::is_array_v<T>, "T cannot be an array");

    return std::allocate_shared<T>(allocator<T>(), std::forward<Args>(args)...);
}

template <typename T, typename Allocator>
auto nullptr_unique(const Allocator& allocator = Allocator()) noexcept {
    static_assert(!std::is_array_v<T>, "T cannot be an array");

    using TAllocator = typename std::allocator_traits<Allocator>::template rebind_alloc<T>;
    using TDeleter = allocator_deleter<T, TAllocator>;
    return std::unique_ptr<T, TDeleter>(nullptr, TDeleter(allocator));
}

} // namespace kotlin::std_support
