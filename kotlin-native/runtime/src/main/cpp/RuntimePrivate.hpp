/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

namespace kotlin {
namespace internal {

inline constexpr int FILE_NOT_INITIALIZED = 0;
inline constexpr int FILE_BEING_INITIALIZED = 1;
inline constexpr int FILE_INITIALIZED = 2;
inline constexpr int FILE_FAILED_TO_INITIALIZE = 3;

} // namespace internal
} // namespace kotlin
