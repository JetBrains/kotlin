/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>

namespace kotlin::std_support {

// TODO: Replace with `aligned_alloc` that's compatible with normal `free`.
// Allocate aligned memory. Must be freed with `aligned_free`.
void* aligned_malloc(std::size_t alignment, std::size_t size) noexcept;

// Free memory allocated with `aligned_malloc`.
void aligned_free(void* ptr) noexcept;

} // namespace kotlin::std_support
