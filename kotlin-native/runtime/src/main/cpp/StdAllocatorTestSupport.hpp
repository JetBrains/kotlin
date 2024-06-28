/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstdlib>
#include <map>
#include <mutex>
#include <optional>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Utils.hpp"

namespace kotlin::test_support {

class MockAllocatorCore : private Pinned {
public:
    MOCK_METHOD(void*, allocate, (std::size_t), (noexcept));
    MOCK_METHOD(void, deallocate, (void*, std::size_t), (noexcept));
};

class SpyAllocatorCore : private Pinned {
public:
    SpyAllocatorCore() noexcept {
        ON_CALL(*this, allocate(testing::_)).WillByDefault([](std::size_t size) { return std::malloc(size); });
        ON_CALL(*this, deallocate(testing::_, testing::_)).WillByDefault([](void* ptr, std::size_t size) { std::free(ptr); });
    }

    MOCK_METHOD(void*, allocate, (std::size_t), (noexcept));
    MOCK_METHOD(void, deallocate, (void*, std::size_t), (noexcept));
};

template <typename T, typename Core>
struct Allocator {
    using value_type = T;
    using size_type = std::size_t;
    using difference_type = std::ptrdiff_t;
    using propagate_on_container_move_assignment = std::true_type;
    using is_always_equal = std::false_type;

    explicit Allocator(Core& core) : core_(&core) {}

    Allocator(const Allocator&) noexcept = default;

    template <typename U>
    Allocator(const Allocator<U, Core>& other) noexcept : core_(other.core_) {}

    template <typename U>
    Allocator& operator=(const Allocator<U, Core>& other) noexcept {
        core_ = other.core_;
    }

    T* allocate(std::size_t n) noexcept { return static_cast<T*>(core_->allocate(sizeof(T) * n)); }

    void deallocate(T* p, std::size_t n) noexcept { core_->deallocate(p, sizeof(T) * n); }

    Core* core_;
};

template <typename T, typename Core>
auto MakeAllocator(Core& core) {
    return Allocator<T, Core>(core);
}

template <typename T, typename U, typename Core>
bool operator==(const Allocator<T, Core>& lhs, const Allocator<U, Core>& rhs) noexcept {
    return lhs.core_ == rhs.core_;
}

template <typename T, typename U, typename Core>
bool operator!=(const Allocator<T, Core>& lhs, const Allocator<U, Core>& rhs) noexcept {
    return !(lhs == rhs);
}

} // namespace kotlin::test_support
