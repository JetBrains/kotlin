/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#ifndef KONAN_NO_THREADS

#include <functional>
#include <optional>
#include <string_view>
#include <thread>

#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace internal {

void setCurrentThreadName(std::string_view name) noexcept;

} // namespace internal

// This is a combination of `std::jthread` and http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2020/p2019r0.pdf
// Missing stop token handling from `std::jthread`, and only a name attribute is supported from the paper.
class ScopedThread : private MoveOnly {
public:
    using id = std::thread::id;
    using native_handle_type = std::thread::native_handle_type;

    // There's no guarantee when and if these attributes will be applied. Use them as hints.
    class attributes {
    public:
        attributes() noexcept = default;
        attributes(const attributes&) noexcept = default;
        attributes(attributes&&) noexcept = default;
        attributes& operator=(const attributes&) noexcept = default;
        attributes& operator=(attributes&&) noexcept = default;
        ~attributes() = default;

        attributes& name(std::string_view name) noexcept {
            name_ = name;
            return *this;
        }

    private:
        friend class ScopedThread;
        std::optional<KStdString> name_;
    };

    ScopedThread() noexcept = default;

    template <
            typename F,
            typename... Args,
            typename = std::enable_if_t<!std::is_same_v<std::remove_cv_t<std::remove_reference_t<F>>, attributes>>>
    explicit ScopedThread(F&& f, Args&&... args) : ScopedThread(attributes(), std::forward<F>(f), std::forward<Args>(args)...) {}

    template <typename F, typename... Args>
    explicit ScopedThread(const attributes& attr, F&& f, Args&&... args) :
        thread_(&ScopedThread::Run<F, Args...>, attr, std::forward<F>(f), std::forward<Args>(args)...) {}

    ScopedThread(ScopedThread&& rhs) noexcept = default;
    ScopedThread& operator=(ScopedThread&& rhs) noexcept = default;

    ~ScopedThread() {
        if (thread_.joinable()) {
            thread_.join();
        }
    }

    void swap(ScopedThread& rhs) noexcept { thread_.swap(rhs.thread_); }

    [[nodiscard]] static unsigned int hardware_concurrency() noexcept { return std::thread::hardware_concurrency(); }

    [[nodiscard]] bool joinable() const noexcept { return thread_.joinable(); }

    [[nodiscard]] id get_id() const noexcept { return thread_.get_id(); }

    [[nodiscard]] native_handle_type native_handle() { return thread_.native_handle(); }

    void join() { thread_.join(); }

    void detach() { thread_.detach(); }

private:
    template <typename F, typename... Args>
    static std::invoke_result_t<F, Args...> Run(attributes attr, F&& f, Args&&... args) {
        if (attr.name_) {
            internal::setCurrentThreadName(*attr.name_);
        }
        return std::invoke(std::forward<F>(f), std::forward<Args>(args)...);
    }

    std::thread thread_;
};

} // namespace kotlin

#endif // !KONAN_NO_THREADS
