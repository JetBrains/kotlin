/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <functional>
#include <optional>
#include <string>
#include <string_view>
#include <thread>

#include "ScopedThread.hpp"

#include "Utils.hpp"

namespace kotlin {

namespace mm {
    void waitGlobalDataInitialized() noexcept;
}

class UtilityThread : public ScopedThread {
public:
    UtilityThread() noexcept = default;

    template <typename F, typename... Args>
    explicit UtilityThread(std::string_view name, F&& f, Args&&... args) :
        ScopedThread(ScopedThread::attributes().name(name), &UtilityThread::Run<F, Args...>, std::forward<F>(f), std::forward<Args>(args)...) {}

    UtilityThread(UtilityThread&& rhs) noexcept = default;
    UtilityThread& operator=(UtilityThread&& rhs) noexcept = default;

private:
    template <typename F, typename... Args>
    static std::invoke_result_t<F, Args...> Run(F&& f, Args&&... args) {
        mm::waitGlobalDataInitialized();
        return std::invoke(std::forward<F>(f), std::forward<Args>(args)...);
    }
};

} // namespace kotlin
