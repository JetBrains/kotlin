/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <type_traits>

#include "Common.h"
#include "ManuallyScoped.hpp"

namespace kotlin::std_support {

namespace internal {

// TODO current implementation supports only pointer or integral T
template <typename T>
inline constexpr bool supports_atomic_ref_v =
#ifdef KONAN_NO_64BIT_ATOMIC
    sizeof(T) <= 4 &&
#endif
    (std::is_pointer_v<T> || std::is_integral_v<T>);

}

#pragma clang diagnostic push
// On 32-bit android arm clang warns of significant performance penalty because of large atomic operations.
// TODO: Consider using alternative ways of ordering memory operations
//       if they turn out to be more efficient on these platforms.
#pragma clang diagnostic ignored "-Watomic-alignment"

template<typename T>
class atomic_ref {
    static_assert(internal::supports_atomic_ref_v<T>, "T does not support atomic_ref");
public:
    explicit atomic_ref(T& ref) : ref_(ref) {}
    atomic_ref(const atomic_ref& other) noexcept : ref_(other.ref_) {}

    ALWAYS_INLINE T operator=(T desired) const noexcept {
        store(desired);
        return desired;
    }
    atomic_ref& operator=(const atomic_ref&) = delete;

    ALWAYS_INLINE bool is_lock_free() const noexcept {
        static_assert(sizeof(T) <= 16); // TODO support larger types if needed
        constexpr auto requiredAlignment = sizeof(T) > alignof(T) ? sizeof(T) : alignof(T);
        return __atomic_is_lock_free(sizeof(T), reinterpret_cast<void*>(-requiredAlignment));
    }

    static constexpr bool is_always_lock_free = __atomic_always_lock_free(sizeof(T), nullptr);

    ALWAYS_INLINE void store(T desired, std::memory_order order = std::memory_order_seq_cst) const noexcept {
        __atomic_store(&ref_, &desired, builtinOrder(order));
    }

    ALWAYS_INLINE T load(std::memory_order order = std::memory_order_seq_cst) const noexcept {
        kotlin::ManuallyScoped<std::remove_const_t<T>> ret; // the intrinsic below strips const.
        __atomic_load(&ref_, &*ret, builtinOrder(order));
        return *ret;
    }

    ALWAYS_INLINE operator T() const noexcept {
        return load();
    }

    ALWAYS_INLINE T exchange(T desired, std::memory_order order = std::memory_order_seq_cst) const noexcept {
        kotlin::ManuallyScoped<std::remove_const_t<T>> ret; // the intrinsic below strips const.
        __atomic_exchange(&ref_, &desired, &*ret, builtinOrder(order));
        return *ret;
    }

    ALWAYS_INLINE bool compare_exchange_weak(T& expected, T desired,
                               std::memory_order success,
                               std::memory_order failure) const noexcept {
        return __atomic_compare_exchange(&ref_,
                                           &expected,
                                           &desired,
                                           true,
                                           builtinOrder(success),
                                           builtinOrder(failure));
    }

    ALWAYS_INLINE bool compare_exchange_weak(T& expected, T desired,
                               std::memory_order order =
                               std::memory_order_seq_cst) const noexcept {
        return compare_exchange_weak(expected, desired, order, order);
    }

    ALWAYS_INLINE bool compare_exchange_strong(T& expected, T desired,
                                 std::memory_order success,
                                 std::memory_order failure) const noexcept {
        return __atomic_compare_exchange(&ref_,
                                           &expected,
                                           &desired,
                                           false,
                                           builtinOrder(success),
                                           builtinOrder(failure));
    }

    ALWAYS_INLINE bool compare_exchange_strong(T& expected, T desired,
                                 std::memory_order order =
                                 std::memory_order_seq_cst) const noexcept {
        return compare_exchange_strong(expected, desired, order, order);
    }

    // TODO implement fetch_*** functions for appropriate types

    ALWAYS_INLINE T fetch_or(T value, std::memory_order order = std::memory_order_seq_cst) noexcept {
        return __atomic_fetch_or(&ref_, value, builtinOrder(order));
    }

private:
    ALWAYS_INLINE static constexpr auto builtinOrder(std::memory_order stdOrder) {
        switch(stdOrder) {
            case (std::memory_order_relaxed): return __ATOMIC_RELAXED;
            case (std::memory_order_consume): return __ATOMIC_CONSUME;
            case (std::memory_order_acquire): return __ATOMIC_ACQUIRE;
            case (std::memory_order_release): return __ATOMIC_RELEASE;
            case (std::memory_order_acq_rel): return __ATOMIC_ACQ_REL;
            case (std::memory_order_seq_cst): return __ATOMIC_SEQ_CST;
        }
    }

    T& ref_;
};

#pragma clang diagnostic pop

template<typename T, typename Atomic>
ALWAYS_INLINE T atomic_compare_swap_strong(Atomic&& atomic, T expectedValue, T newValue) {
    atomic.compare_exchange_strong(expectedValue, newValue);
    return expectedValue;
}

template<typename T, typename Atomic>
ALWAYS_INLINE bool atomic_compare_exchange_strong(Atomic&& atomic, T expectedValue, T newValue) {
    return atomic.compare_exchange_strong(expectedValue, newValue);
}

}
