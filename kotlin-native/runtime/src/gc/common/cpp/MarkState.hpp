/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#pragma once

#include <utility>

namespace kotlin {
namespace gc {

template <typename Traits>
struct MarkState {
    /**
     * This is the mark queue for proper heap objects (ObjHeader::heapNotLocal == true).
     */
    typename Traits::MarkQueue globalQueue;

    template <typename... Args>
    explicit MarkState(Args&&... args) : globalQueue(std::forward<Args>(args)...) {}
};

} // namespace gc
} // namespace kotlin