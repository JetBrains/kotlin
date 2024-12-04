/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <type_traits>
#include <utility>

#include "Utils.hpp"

namespace kotlin {

// Like T but must be manually constructed and destroyed.
template <typename T, bool kChecked = false>
class ManuallyScoped : private Pinned {
public:
    // Construct T
    template <typename... Args>
    void construct(Args&&... args) noexcept(std::is_nothrow_constructible_v<T, Args...>) {
        new (impl()) T(std::forward<Args>(args)...);
    }

    // Destroy T
    void destroy() noexcept { impl()->~T(); }

    T& operator*() noexcept { return *impl(); }
    T* operator->() noexcept { return impl(); }
    const T& operator*() const noexcept { return *impl(); }
    const T* operator->() const noexcept { return impl(); }

private:
    __attribute__((used)) T* impl() noexcept { return reinterpret_cast<T*>(implStorage_); }
    const T* impl() const noexcept { return reinterpret_cast<const T*>(implStorage_); }

    alignas(T) char implStorage_[sizeof(T)];
};

template <typename T>
class ManuallyScoped<T, true> : private Pinned {
public:
    ManuallyScoped() : constructed_(false) {}
    ~ManuallyScoped() { assertDestroyed(); }

    // Construct T
    template <typename... Args>
    void construct(Args&&... args) noexcept(noexcept(T(std::forward<Args>(args)...))) {
        assertDestroyed();
        impl_.construct(std::forward<Args>(args)...);
        constructed_ = true;
    }

    // Destroy T
    void destroy() noexcept {
        assertConstructed();
        impl_.destroy();
        constructed_ = false;
    }

    T& operator*() noexcept { return *impl(); }
    T* operator->() noexcept { return impl(); }
    const T& operator*() const noexcept { return *impl(); }
    const T* operator->() const noexcept { return impl(); }

private:
    T* impl() noexcept {
        assertConstructed();
        return &*impl_;
    }
    const T* impl() const noexcept {
        assertConstructed();
        return &*impl_;
    }

    ALWAYS_INLINE void assertConstructed() const noexcept {
        RuntimeAssert(constructed_, "ManuallyScoped value must have been constructed by this point");
    }
    ALWAYS_INLINE void assertDestroyed() const noexcept {
        RuntimeAssert(!constructed_, "ManuallyScoped value must have been destroyed by this point");
    }

    bool constructed_;
    ManuallyScoped<T, false> impl_;
};

} // namespace kotlin
