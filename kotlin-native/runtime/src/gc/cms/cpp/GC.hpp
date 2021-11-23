/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "ConcurrentMarkAndSweep.hpp"

namespace kotlin {
namespace gc {

using GC = kotlin::gc::ConcurrentMarkAndSweep;

inline constexpr bool kSupportsMultipleMutators = true;

} // namespace gc
} // namespace kotlin

