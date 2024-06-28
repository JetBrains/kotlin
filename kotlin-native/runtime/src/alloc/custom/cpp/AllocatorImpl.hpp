/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Allocator.hpp"

#include "CustomAllocator.hpp"
#include "CustomFinalizerProcessor.hpp"
#include "GCApi.hpp"
#include "Heap.hpp"

namespace kotlin::alloc {

class Allocator::Impl : private Pinned {
public:
    Impl() noexcept = default;

    Heap& heap() noexcept { return heap_; }

private:
    Heap heap_;
};

class Allocator::ThreadData::Impl : private Pinned {
public:
    explicit Impl(Allocator::Impl& allocator) noexcept : alloc_(allocator.heap()) {}

    alloc::CustomAllocator& alloc() noexcept { return alloc_; }

private:
    CustomAllocator alloc_;
};

} // namespace kotlin::alloc
