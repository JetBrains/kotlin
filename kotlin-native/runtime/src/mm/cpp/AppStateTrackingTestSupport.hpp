/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "AppStateTracking.hpp"

#include "Utils.hpp"

namespace kotlin::mm {

class AppStateTrackingTestSupport : private Pinned {
public:
    AppStateTrackingTestSupport(AppStateTracking& appStateTracking) noexcept : appStateTracking_(appStateTracking) {}

    void setState(AppStateTracking::State state) noexcept { appStateTracking_.setState(state); }

private:
    AppStateTracking& appStateTracking_;
};

} // namespace kotlin::mm
