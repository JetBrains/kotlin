/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include <gtest/gtest.h>

#include "TestSupport.hpp"

using namespace kotlin;

TEST(CurrentThreadDataDeathTest, GetThreadDataForUnregisteredThread) {
    EXPECT_DEATH(mm::currentThreadData(), "Thread is not attached to the runtime");
}

TEST(CurrentThreadDataTest, GetThreadDataForRegisteredThread) {
    RunInNewThread([](mm::ThreadData& expectedThreadData) {
        auto& actualThreadData = mm::currentThreadData();
        EXPECT_NE(&actualThreadData, nullptr);
        EXPECT_EQ(&actualThreadData, &expectedThreadData);
    });
}

TEST(CurrentThreadDataTest, IsCurrentThreadRegistered) {
    EXPECT_FALSE(mm::IsCurrentThreadRegistered());
    RunInNewThread([]() {
        EXPECT_TRUE(mm::IsCurrentThreadRegistered());
    });
}
