/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <utility>

#include "Utils.hpp"

namespace kotlin::mm {

class ThreadData;

class SafePointActivator : private MoveOnly {
public:
    SafePointActivator() noexcept;
    ~SafePointActivator();

    SafePointActivator(SafePointActivator&& rhs) noexcept : active_(rhs.active_) { rhs.active_ = false; }

    SafePointActivator& operator=(SafePointActivator&& rhs) noexcept {
        SafePointActivator other(std::move(rhs));
        swap(other);
        return *this;
    }

    void swap(SafePointActivator& rhs) noexcept { std::swap(active_, rhs.active_); }

private:
    bool active_;
};

void safePoint(std::memory_order fastPathOrder = std::memory_order_relaxed) noexcept;
void safePoint(ThreadData& threadData, std::memory_order fastPathOrder = std::memory_order_relaxed) noexcept;

namespace test_support {

bool safePointsAreActive() noexcept;
void setSafePointAction(void (*action)(mm::ThreadData&)) noexcept;

} // namespace test_support

} // namespace kotlin::mm
