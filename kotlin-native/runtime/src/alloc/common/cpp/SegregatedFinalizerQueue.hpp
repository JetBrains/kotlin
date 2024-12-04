/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <utility>

namespace kotlin::alloc {

// Finalizer queue split between threads.
template <typename FinalizerQueue>
struct SegregatedFinalizerQueue {
    FinalizerQueue regular;
    FinalizerQueue mainThread;

    size_t size() noexcept { return regular.size() + mainThread.size(); }

    void mergeIntoRegular() noexcept {
        regular.TransferAllFrom(std::move(mainThread));
        mainThread = FinalizerQueue{};
    }

    void mergeFrom(SegregatedFinalizerQueue rhs) noexcept {
        regular.TransferAllFrom(std::move(rhs.regular));
        mainThread.TransferAllFrom(std::move(rhs.mainThread));
    }
};

} // namespace kotlin::alloc