/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_GCAPI_HPP_
#define CUSTOM_ALLOC_CPP_GCAPI_HPP_

#include <cstdint>
#include <inttypes.h>
#include <limits>
#include <stdlib.h>

#include "AtomicStack.hpp"
#include "ExtraObjectPage.hpp"

namespace kotlin::alloc {

bool TryResetMark(void* ptr) noexcept;

enum class ExtraObjectStatus {
    TO_BE_FINALIZED,
    KEPT,
    SWEPT,
};

ExtraObjectStatus SweepExtraObject(ExtraObjectCell* extraObjectCell, AtomicStack<ExtraObjectCell>& finalizerQueue) noexcept;

void* SafeAlloc(uint64_t size) noexcept;
void Free(void* ptr, size_t size) noexcept;

size_t GetAllocatedBytes() noexcept;

} // namespace kotlin::alloc

#endif
