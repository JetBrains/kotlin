/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Allocator.hpp"

#include "CustomAllocator.hpp"
#include "CustomFinalizerProcessor.hpp"
#include "GCApi.hpp"
#include "GlobalData.hpp"
#include "Heap.hpp"
#include "SegregatedFinalizerProcessor.hpp"

namespace kotlin::alloc {

struct SweepState {};

class Allocator::Impl : private Pinned {
public:
    Impl() noexcept : finalizerProcessor_([](int64_t epoch) noexcept { mm::GlobalData::Instance().gc().onEpochFinalized(epoch); }) {}

    Heap& heap() noexcept { return heap_; }
    SegregatedFinalizerProcessor<FinalizerQueueSingle, FinalizerQueueTraits>& finalizerProcessor() noexcept { return finalizerProcessor_; }

    SweepState prepareForSweep() noexcept;
    FinalizerQueue sweep(gc::GCHandle gcHandle, SweepState state) noexcept;
    void scheduleFinalization(FinalizerQueue queue, int64_t epoch) noexcept;

private:
    Heap heap_;
    SegregatedFinalizerProcessor<FinalizerQueueSingle, FinalizerQueueTraits> finalizerProcessor_;
};

class Allocator::ThreadData::Impl : private Pinned {
public:
    explicit Impl(Allocator::Impl& allocator) noexcept : alloc_(allocator.heap()) {}

    alloc::CustomAllocator& alloc() noexcept { return alloc_; }

private:
    CustomAllocator alloc_;
};

} // namespace kotlin::alloc
