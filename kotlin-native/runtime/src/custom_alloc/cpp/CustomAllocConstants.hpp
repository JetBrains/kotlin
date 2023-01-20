/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_CUSTOMALLOCCONSTANTS_HPP_
#define CUSTOM_ALLOC_CPP_CUSTOMALLOCCONSTANTS_HPP_

#include <cstddef>
#include <cstdint>

#include "SmallPage.hpp"
#include "MediumPage.hpp"
#include "ExtraObjectPage.hpp"

inline constexpr const size_t KiB = 1024;

inline constexpr const size_t SMALL_PAGE_SIZE = (256 * KiB);
inline constexpr const int SMALL_PAGE_MAX_BLOCK_SIZE = 128;
inline constexpr const size_t SMALL_PAGE_CELL_COUNT =
        ((SMALL_PAGE_SIZE - sizeof(kotlin::alloc::SmallPage)) / sizeof(kotlin::alloc::SmallCell));

inline constexpr const size_t MEDIUM_PAGE_SIZE = (256 * KiB);
inline constexpr const size_t MEDIUM_PAGE_CELL_COUNT =
        ((MEDIUM_PAGE_SIZE - sizeof(kotlin::alloc::MediumPage)) / sizeof(kotlin::alloc::Cell));

inline constexpr const size_t LARGE_PAGE_SIZE_THRESHOLD = (MEDIUM_PAGE_CELL_COUNT - 1);

inline constexpr const size_t EXTRA_OBJECT_PAGE_SIZE = 64 * KiB;
inline constexpr const int EXTRA_OBJECT_COUNT =
        (EXTRA_OBJECT_PAGE_SIZE - sizeof(kotlin::alloc::ExtraObjectPage)) / sizeof(kotlin::alloc::ExtraObjectCell);

#endif
