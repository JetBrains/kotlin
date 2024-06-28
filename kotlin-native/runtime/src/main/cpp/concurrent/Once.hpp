/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#pragma once

#include <atomic>
#include <mutex>

#include <ManuallyScoped.hpp>
#include <Utils.hpp>

namespace kotlin {

/** A value that can be computed only once. even with concurrent attempts from multiple threads. */
template <typename T>
class OnceComputable : private Pinned {
public:
    ~OnceComputable() noexcept { result_.destroy(); }

    const T* tryGetValue() const noexcept {
        if (computed_.load(std::memory_order_acquire)) return &*result_;
        return nullptr;
    }

    template <typename Computer>
    const T& ensureComputed(Computer&& computer) noexcept(noexcept(computer())) {
        if (auto res = tryGetValue()) {
            return *res;
        }

        std::lock_guard lock(mutex_);
        if (!computed_.load(std::memory_order_relaxed)) {
            result_.construct(computer());
            computed_.store(true, std::memory_order_release);
        }
        return *result_;
    }

    template <typename Computer>
    const T& operator=(Computer&& computer) noexcept(noexcept(computer())) {
        return ensureComputed(std::forward<Computer>(computer));
    }

private:
    ManuallyScoped<T> result_;

    std::atomic<bool> computed_ = false;
    std::mutex mutex_;
};

template <>
class OnceComputable<void> : private Pinned {
public:
    bool computed() const noexcept { return computed_.load(std::memory_order_acquire); }

    template <typename Computer>
    void ensureComputed(Computer&& computer) noexcept(noexcept(computer())) {
        if (computed()) {
            return;
        }

        std::lock_guard lock(mutex_);
        if (!computed_.load(std::memory_order_relaxed)) {
            computer();
            computed_.store(true, std::memory_order_release);
        }
    }

    template <typename Computer>
    void operator=(Computer&& computer) noexcept(noexcept(computer())) {
        return ensureComputed(std::forward<Computer>(computer));
    }

private:
    std::atomic<bool> computed_ = false;
    std::mutex mutex_;
};

/** A guard helping to ensure a certain action is executed only once. Even with concurrent attempts from multiple threads. */
class OnceExecutable : private OnceComputable<void> {
public:
    bool executed() const noexcept { return computed(); }

    template <typename Action>
    void ensureExecuted(Action&& action) noexcept(noexcept(action())) {
        ensureComputed(std::forward<Action>(action));
    }

    template <typename Action>
    void operator=(Action&& action) noexcept(noexcept(action())) {
        ensureExecuted(std::forward<Action>(action));
    }
};

} // namespace kotlin
