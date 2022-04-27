/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>

namespace kotlin::std_support {

void* malloc(std::size_t size) noexcept;
void* aligned_alloc(std::size_t alignment, std::size_t size) noexcept;

void* calloc(std::size_t num, std::size_t size) noexcept;
void* aligned_calloc(std::size_t alignment, std::size_t num, std::size_t size) noexcept;

void* realloc(void* ptr, std::size_t size) noexcept;

void free(void* ptr) noexcept;

} // namespace kotlin::std_support
