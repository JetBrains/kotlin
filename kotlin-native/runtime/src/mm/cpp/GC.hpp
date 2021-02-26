/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_GC_H
#define RUNTIME_MM_GC_H

#include "gc/NoOpGC.hpp"

namespace kotlin {
namespace mm {

// TODO: GC should be extracted into a separate module, so that we can do different GCs without
//       the need to redo the entire MM. For now changing GCs can be done by modifying `using` below.

using GC = NoOpGC;

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GC_H
