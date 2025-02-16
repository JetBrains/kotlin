/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCImpl.hpp"

namespace kotlin::gc::test_support {

void reconfigureGCParallelism(gc::GC::Impl& gc, size_t maxParallelism, bool mutatorsCooperate, size_t auxGCThreads) noexcept;

}
