/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"

namespace kotlin::gcScheduler::test_support {

class GCSchedulerThreadDataTestApi : private Pinned {
public:
    explicit GCSchedulerThreadDataTestApi(GCSchedulerThreadData& scheduler) : scheduler_(scheduler) {}

    void SetAllocatedBytes(size_t bytes) { scheduler_.allocatedBytes_ = bytes; }

private:
    GCSchedulerThreadData& scheduler_;
};

} // namespace kotlin::gcScheduler::test_support
