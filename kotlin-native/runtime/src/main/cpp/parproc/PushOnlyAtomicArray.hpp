/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include "../CompilerConstants.hpp"
#include "../KAssert.h"
#include "../Utils.hpp"

namespace kotlin {

template <typename T, std::size_t Capacity, T None>
class PushOnlyAtomicArray : private MoveOnly {
public:
    PushOnlyAtomicArray() noexcept {
        cleanElements(0, Capacity);
    }

    PushOnlyAtomicArray(PushOnlyAtomicArray&& that) noexcept {
        copyElements(that);
        cleanElements(that.size_, Capacity);
        size_.store(that.size_.load(std::memory_order_relaxed), std::memory_order_release);
    }

    // Not atomic
    PushOnlyAtomicArray& operator=(PushOnlyAtomicArray&& that) noexcept {
        copyElements(that);
        cleanElements(that.size_, size_);
        size_.store(that.size_.load(std::memory_order_relaxed), std::memory_order_release);
        return *this;
    }

    // Not atomic
    void clean() noexcept {
        cleanElements(0, size_);
        size_.store(0, std::memory_order_release);
    }

    std::size_t size(std::memory_order memoryOrder = std::memory_order_relaxed) const noexcept {
        return size_.load(memoryOrder);
    }

    T operator[](std::size_t idx) const noexcept {
        if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
            auto size = size_.load();
            RuntimeAssert(idx < size, "Index %zu out of bounds [0; %zu)", idx, size);
        }
        T value = elements_[idx].load(std::memory_order_relaxed);
        RuntimeAssert(value != None, "Invalid value");
        return value;
    }

    void push(T value) noexcept {
        RuntimeAssert(value != None, "Invalid value");
        std::size_t newIdx;
        while (true) {
            newIdx = size_.load(std::memory_order_acquire);
            RuntimeAssert(newIdx < Capacity, "Stack overflow");
            T expected = None;
            if (elements_[newIdx].compare_exchange_weak(expected, value, std::memory_order_relaxed)) {
                break;
            }
        }
        auto newSize = newIdx + 1;
        size_.store(newSize, std::memory_order_release);
    }

    using iterator = std::atomic<T>*;
    using const_iterator = const std::atomic<T>*;

    iterator begin() noexcept { return elements_.data(); }
    iterator end() noexcept { return elements_.data() + size(std::memory_order_acquire); }
    const_iterator begin() const noexcept { return elements_.data(); }
    const_iterator end() const noexcept { return elements_.data() + size(std::memory_order_acquire); }

private:
    void copyElements(PushOnlyAtomicArray& from) noexcept {
        auto size = from.size_.load(std::memory_order_acquire);
        for (std::size_t i = 0; i < size; ++i) {
            elements_[i].store(from.elements_[i].load(std::memory_order_relaxed), std::memory_order_relaxed);
        }
    }

    void cleanElements(std::size_t from, std::size_t until) noexcept {
        for (std::size_t i = from; i < until; ++i) {
            elements_[i].store(None, std::memory_order_relaxed);
        }
    }

    std::array<std::atomic<T>, Capacity> elements_;
    std::atomic<std::size_t> size_ = 0;
};

}