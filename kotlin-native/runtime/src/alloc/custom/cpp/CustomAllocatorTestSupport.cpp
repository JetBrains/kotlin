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

kotlin::alloc::ExtraObjectCell* kotlin::alloc::test_support::initExtraObjectCell(uint8_t* ptr) {
    EXPECT_TRUE(ptr[0] == 0 && memcmp(ptr, ptr + 1, kExtraObjCellSize.inBytes() - 1) == 0);
    auto* extraObjCell = new(ptr) ExtraObjectCell();
    new(extraObjCell->data_) kotlin::mm::ExtraObjectData(nullptr, nullptr);
    return extraObjCell;
}

kotlin::alloc::ExtraObjectCell* kotlin::alloc::test_support::allocExtraObjectCell(kotlin::alloc::FixedBlockPage* page) {
    uint8_t* ptr = page->TryAllocate();
    if (ptr) {
        return initExtraObjectCell(ptr);
    }
    return nullptr;
}
