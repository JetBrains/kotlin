/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"

#include "Logging.hpp"

namespace kotlin::gcScheduler {

class GCScheduler::Impl : private Pinned {
public:
    Impl() noexcept { RuntimeLogInfo({kTagGC}, "Manual GC scheduler initialized"); }
};

class GCScheduler::ThreadData::Impl : private Pinned {};

} // namespace kotlin::gcScheduler
