/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_NOOP_GC_H
#define RUNTIME_GC_NOOP_GC_H

#include "NoOpGC.hpp"

namespace kotlin {
namespace gc {

using GC = kotlin::gc::NoOpGC;

inline constexpr bool kSupportsMultipleMutators = true;

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_NOOP_GC_H
