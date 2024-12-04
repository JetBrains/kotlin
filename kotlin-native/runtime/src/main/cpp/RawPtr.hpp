/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <functional>
#include <utility>

#include "Utils.hpp"

namespace kotlin {

// raw_ptr<T> is T* but
// * with default constructor setting value to null
// * with destructive move
// * with <, <=, >=, > guaranteeing total order.
template <typename T>
class [[clang::trivial_abi]] raw_ptr final {
    // TODO: Look up if there're active C++ proposals for that.
    // TODO: Support type casts.
public:
    raw_ptr() noexcept = default;
    raw_ptr(std::nullptr_t) noexcept {}
    raw_ptr(T* impl) noexcept : impl_(impl) {} // implicit by design

    raw_ptr(const raw_ptr&) noexcept = default;
    raw_ptr& operator=(const raw_ptr&) noexcept = default;

    raw_ptr(raw_ptr&& rhs) noexcept : impl_(rhs.impl_) { rhs.impl_ = nullptr; }

    raw_ptr& operator=(raw_ptr&& rhs) noexcept {
        raw_ptr tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    ~raw_ptr() = default;

    void swap(raw_ptr& rhs) noexcept { std::swap(impl_, rhs.impl_); }

    T& operator*() const noexcept { return *impl_; }

    T* operator->() const noexcept { return impl_; }

    explicit operator bool() const noexcept { return impl_; }

    bool operator==(raw_ptr<T> rhs) const noexcept { return impl_ == rhs.impl_; }

    bool operator!=(raw_ptr<T> rhs) const noexcept { return impl_ != rhs.impl_; }

    bool operator<(raw_ptr<T> rhs) const noexcept { return std::less<>()(impl_, rhs.impl_); }

    bool operator<=(raw_ptr<T> rhs) const noexcept { return std::less_equal<>()(impl_, rhs.impl_); }

    bool operator>(raw_ptr<T> rhs) const noexcept { return std::greater<>()(impl_, rhs.impl_); }

    bool operator>=(raw_ptr<T> rhs) const noexcept { return std::greater_equal<>()(impl_, rhs.impl_); }

    raw_ptr<T>& operator++() noexcept {
        ++impl_;
        return *this;
    }

    raw_ptr<T> operator++(int) noexcept { return impl_++; }

    raw_ptr<T>& operator--() noexcept {
        --impl_;
        return *this;
    }

    raw_ptr<T> operator--(int) noexcept { return impl_--; }

    raw_ptr<T>& operator+=(std::ptrdiff_t arg) noexcept {
        impl_ += arg;
        return *this;
    }

    raw_ptr<T>& operator-=(std::ptrdiff_t arg) noexcept {
        impl_ -= arg;
        return *this;
    }

    raw_ptr<T> operator+(std::ptrdiff_t arg) const noexcept { return impl_ + arg; }

    raw_ptr<T> operator-(std::ptrdiff_t arg) const noexcept { return impl_ - arg; }

    std::ptrdiff_t operator-(raw_ptr<T> rhs) const noexcept { return impl_ - rhs.impl_; }

    explicit operator T*() const noexcept { return impl_; }

private:
    friend struct std::atomic<raw_ptr>;

    T* impl_ = nullptr;
};

} // namespace kotlin

namespace std {

template <typename T>
struct atomic<kotlin::raw_ptr<T>> : kotlin::Pinned {
public:
    static constexpr bool is_always_lock_free = atomic<T*>::is_always_lock_free;

    atomic() noexcept = default;

    atomic(kotlin::raw_ptr<T> ptr) noexcept : impl_(static_cast<T*>(ptr)) {}

    kotlin::raw_ptr<T> operator=(kotlin::raw_ptr<T> ptr) noexcept { return impl_ = static_cast<T*>(ptr); }

    bool is_lock_free() const noexcept { return impl_.is_lock_free(); }

    void store(kotlin::raw_ptr<T> ptr, memory_order order = memory_order_seq_cst) noexcept { impl_.store(static_cast<T*>(ptr), order); }

    // This loads a copy of a pointer. To perform a destructive load (i.e. null out `this`, use `exchange(nullptr)`).
    [[nodiscard("expensive pure function")]] kotlin::raw_ptr<T> load(memory_order order = memory_order_seq_cst) const noexcept {
        return impl_.load(order);
    }

    // This loads a copy of a pointer. To perform a destructive load (i.e. null out `this`, use `exchange(nullptr)`).
    [[nodiscard("expensive pure function")]] operator kotlin::raw_ptr<T>() const noexcept { return static_cast<T*>(impl_); }

    [[nodiscard("use store if result is not needed")]] kotlin::raw_ptr<T> exchange(
            kotlin::raw_ptr<T> desired, memory_order order = memory_order_seq_cst) noexcept {
        return impl_.exchange(static_cast<T*>(desired), order);
    }

    bool compare_exchange_weak(
            kotlin::raw_ptr<T>& expected, kotlin::raw_ptr<T> desired, memory_order success, memory_order failure) noexcept {
        return impl_.compare_exchange_weak(expected.impl_, static_cast<T*>(desired), success, failure);
    }

    bool compare_exchange_weak(
            kotlin::raw_ptr<T>& expected, kotlin::raw_ptr<T> desired, memory_order order = memory_order_seq_cst) noexcept {
        return impl_.compare_exchange_weak(expected.impl_, static_cast<T*>(desired), order);
    }

    bool compare_exchange_strong(
            kotlin::raw_ptr<T>& expected, kotlin::raw_ptr<T> desired, memory_order success, memory_order failure) noexcept {
        return impl_.compare_exchange_strong(expected.impl_, static_cast<T*>(desired), success, failure);
    }

    bool compare_exchange_strong(
            kotlin::raw_ptr<T>& expected, kotlin::raw_ptr<T> desired, memory_order order = memory_order_seq_cst) noexcept {
        return impl_.compare_exchange_strong(expected.impl_, static_cast<T*>(desired), order);
    }

    kotlin::raw_ptr<T> fetch_add(ptrdiff_t arg, memory_order order = memory_order_seq_cst) noexcept { return impl_.fetch_add(arg, order); }

    kotlin::raw_ptr<T> fetch_sub(ptrdiff_t arg, memory_order order = memory_order_seq_cst) noexcept { return impl_.fetch_sub(arg, order); }

    kotlin::raw_ptr<T> operator++() noexcept { return impl_++; }

    kotlin::raw_ptr<T> operator++(int) noexcept { return ++impl_; }

    kotlin::raw_ptr<T> operator--() noexcept { return impl_--; }

    kotlin::raw_ptr<T> operator--(int) noexcept { return --impl_; }

    kotlin::raw_ptr<T> operator+=(ptrdiff_t arg) noexcept { return impl_ += arg; }

    kotlin::raw_ptr<T> operator-=(ptrdiff_t arg) noexcept { return impl_ -= arg; }

private:
    atomic<T*> impl_;
};

template <typename T>
struct hash<kotlin::raw_ptr<T>> {
public:
    size_t operator()(kotlin::raw_ptr<T> ptr) const { return impl_(static_cast<T*>(ptr)); }

private:
    hash<T*> impl_;
};

} // namespace std
