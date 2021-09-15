/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include <gtest/gtest.h>

#include "TestSupport.hpp"

using namespace kotlin;

TEST(MemoryStateTestDeathTest, GetMemoryStateForUnregisteredThread) {
    if (CurrentMemoryModel == MemoryModel::kExperimental) {
        EXPECT_DEATH(mm::GetMemoryState(), "Thread is not attached to the runtime");
    } else {
        EXPECT_EQ(mm::GetMemoryState(), nullptr);
    }
}

TEST(MemoryStateTest, GetMemoryStateForRegisteredThread) {
    RunInNewThread([](MemoryState* expectedState) {
        MemoryState* actualState = mm::GetMemoryState();
        EXPECT_NE(actualState, nullptr);
        EXPECT_EQ(actualState, expectedState);
    });
}

TEST(MemoryStateTest, IsCurrentThreadRegistered) {
    EXPECT_FALSE(mm::IsCurrentThreadRegistered());
    RunInNewThread([]() {
        EXPECT_TRUE(mm::IsCurrentThreadRegistered());
    });
}