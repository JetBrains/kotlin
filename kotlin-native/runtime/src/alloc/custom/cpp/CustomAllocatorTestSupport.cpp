/*
 * Copyright 2022-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CustomAllocatorTestSupport.hpp"

namespace {

testing::StrictMock<testing::MockFunction<void(std::size_t)>>* mock = nullptr;

void hookImpl(std::size_t allocatedBytes) {
    mock->Call(allocatedBytes);
}

}

kotlin::alloc::test_support::WithSchedulerNotificationHook::WithSchedulerNotificationHook() {
    mock = &schedulerNotificationHook_;
    setSchedulerNotificationHook(hookImpl);
}

kotlin::alloc::test_support::WithSchedulerNotificationHook::~WithSchedulerNotificationHook() {
    setSchedulerNotificationHook(nullptr);
    mock = nullptr;
}
