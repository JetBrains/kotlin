/*
 * Copyright 2022-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstdint>
#include <vector>

#include "AllocatedSizeTracker.hpp"
#include "AtomicStack.hpp"
#include "GlobalData.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

constexpr auto kPageAlignment = 8;

template<typename Page>
class alignas(kPageAlignment) AnyPage : Pinned {
private:
    friend class AtomicStack<Page>;
    // Used for linking pages together in `pages` queue or in `unswept` queue.
    std::atomic<Page*> next_ = nullptr;

protected:
    // Intentionally non-virtual. `AnyPage` should not be used in any context other than base class clause.
    // Please use concrete implementations instead.
    ~AnyPage() = default;
};

template<typename Page>
class alignas(kPageAlignment) MultiObjectPage : public AnyPage<Page> {
protected:
    // Intentionally non-virtual. `MultiObjectPage` should not be used in any context other than base class clause.
    // Please use concrete implementations instead.
    ~MultiObjectPage() = default;

    AllocatedSizeTracker::Page allocatedSizeTracker_{};
};

} // namespace kotlin::alloc
