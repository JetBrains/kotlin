/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <thread>

#include "gtest/gtest.h"

#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

TEST(ThreadStateTest, StateSwitch) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto initialState = threadData.state();
        EXPECT_EQ(mm::ThreadState::kRunnable, initialState);

        mm::ThreadState oldState = mm::SwitchThreadState(&threadData, mm::ThreadState::kNative);
        EXPECT_EQ(initialState, oldState);
        EXPECT_EQ(mm::ThreadState::kNative, threadData.state());

        // Check functions exported for the compiler too.
        Kotlin_mm_switchThreadStateRunnable();
        EXPECT_EQ(mm::ThreadState::kRunnable, threadData.state());

        Kotlin_mm_switchThreadStateNative();
        EXPECT_EQ(mm::ThreadState::kNative, threadData.state());
    });
}

TEST(ThreadStateTest, StateGuard) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto initialState = threadData.state();
        EXPECT_EQ(mm::ThreadState::kRunnable, initialState);
        {
            mm::ThreadStateGuard guard(&threadData, mm::ThreadState::kNative);
            EXPECT_EQ(mm::ThreadState::kNative, threadData.state());
        }
        EXPECT_EQ(initialState, threadData.state());
    });
}

TEST(ThreadStateDeathTest, StateAsserts) {
    RunInNewThread([](mm::ThreadData& threadData) {
        EXPECT_DEATH(mm::AssertThreadState(&threadData, mm::ThreadState::kNative),
                     "runtime assert: Unexpected thread state. Expected: NATIVE. Actual: RUNNABLE");
    });
}

TEST(ThreadStateDeathTest, IncorrectStateSwitch) {
    RunInNewThread([](mm::ThreadData& threadData) {
        EXPECT_DEATH(mm::SwitchThreadState(&threadData, kotlin::mm::ThreadState::kRunnable),
                     "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");
        EXPECT_DEATH(Kotlin_mm_switchThreadStateRunnable(),
                     "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");

        mm::SwitchThreadState(&threadData, kotlin::mm::ThreadState::kNative);
        EXPECT_DEATH(Kotlin_mm_switchThreadStateNative(),
                     "runtime assert: Illegal thread state switch. Old state: NATIVE. New state: NATIVE");
    });
}
