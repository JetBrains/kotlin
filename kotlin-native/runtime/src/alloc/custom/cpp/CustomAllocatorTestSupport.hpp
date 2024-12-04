/*
 * Copyright 2022-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "gmock/gmock.h"

#include "AllocatedSizeTracker.hpp"

namespace kotlin::alloc::test_support {

class WithSchedulerNotificationHook {
public:
    WithSchedulerNotificationHook();
    ~WithSchedulerNotificationHook();

    auto& hook() { return schedulerNotificationHook_; }
private:
    testing::StrictMock<testing::MockFunction<void(std::size_t)>> schedulerNotificationHook_;
};

}