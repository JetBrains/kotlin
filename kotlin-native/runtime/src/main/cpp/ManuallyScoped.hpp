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
template <typename T>
class ManuallyScoped : private Pinned {
public:
    // Construct T
    template <typename... Args>
    void construct(Args&&... args) noexcept(noexcept(T(std::forward<Args>(args)...))) {
        new (impl()) T(std::forward<Args>(args)...);
    }

    // Destroy T
    void destroy() noexcept { impl()->~T(); }

    T& operator*() noexcept { return *impl(); }
    T* operator->() noexcept { return impl(); }
    const T& operator*() const noexcept { return *impl(); }
    const T* operator->() const noexcept { return impl(); }

private:
    T* impl() noexcept { return reinterpret_cast<T*>(implStorage_); }
    const T* impl() const noexcept { return reinterpret_cast<const T*>(implStorage_); }

    alignas(T) char implStorage_[sizeof(T)];
};

} // namespace kotlin
