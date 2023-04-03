/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

// These tests require a stack trace to contain call site addresses but
// on Windows a trace contains function addresses instead.
// So skip these tests on Windows.
#if (__MINGW32__ || __MINGW64__)
#define SKIP_ON_WINDOWS() \
    do { \
        GTEST_SKIP() << "Skip on Windows"; \
    } while (false)
#else
#define SKIP_ON_WINDOWS() \
    do { \
    } while (false)
#endif

TEST(AggressiveSchedulerTest, TriggerGCOnUniqueSafePoint) {
    SKIP_ON_WINDOWS();
    []() OPTNONE {
        testing::MockFunction<void()> scheduleGC;

        gcScheduler::GCSchedulerConfig config;
        gcScheduler::internal::GCSchedulerDataAggressive scheduler(config, scheduleGC.AsStdFunction());

        EXPECT_CALL(scheduleGC, Call()).Times(1);
        for (int i = 0; i < 10; i++) {
            scheduler.safePoint();
        }
        testing::Mock::VerifyAndClearExpectations(&scheduleGC);

        EXPECT_CALL(scheduleGC, Call()).Times(1);
        scheduler.safePoint();
        testing::Mock::VerifyAndClearExpectations(&scheduleGC);
    }();
}

TEST(AggressiveSchedulerTest, TriggerGCOnAllocationThreshold) {
    SKIP_ON_WINDOWS();
    []() OPTNONE {
        testing::MockFunction<void()> scheduleGC;

        gcScheduler::GCSchedulerConfig config;
        gcScheduler::internal::GCSchedulerDataAggressive scheduler(config, scheduleGC.AsStdFunction());
        gcScheduler::GCSchedulerThreadData threadSchedulerData(
                config, [&scheduler](gcScheduler::GCSchedulerThreadData& data) { scheduler.UpdateFromThreadData(data); });

        ASSERT_EQ(config.allocationThresholdBytes, 1);

        config.autoTune = false;
        config.targetHeapBytes = 10;

        int i = 0;
        // We trigger GC on the first iteration, when the unique allocation point is faced,
        // and on the last iteration when target heap size is reached.
        EXPECT_CALL(scheduleGC, Call()).WillOnce([&i]() { EXPECT_THAT(i, 0); }).WillOnce([&i]() { EXPECT_THAT(i, 9); });

        for (; i < 10; i++) {
            threadSchedulerData.OnSafePointAllocation(1);
        }
        testing::Mock::VerifyAndClearExpectations(&scheduleGC);
    }();
}
