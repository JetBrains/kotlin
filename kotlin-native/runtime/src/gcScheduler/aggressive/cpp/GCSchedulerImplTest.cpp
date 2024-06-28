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
        testing::MockFunction<int64_t()> scheduleGC;

        gcScheduler::GCSchedulerConfig config;
        gcScheduler::internal::GCSchedulerDataAggressive scheduler(config, scheduleGC.AsStdFunction());

        EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(0));
        for (int i = 0; i < 10; i++) {
            scheduler.safePoint();
        }
        testing::Mock::VerifyAndClearExpectations(&scheduleGC);

        EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(1));
        scheduler.safePoint();
        testing::Mock::VerifyAndClearExpectations(&scheduleGC);
    }();
}

TEST(AggressiveSchedulerTest, TriggerGCOnAllocationThreshold) {
    SKIP_ON_WINDOWS();
    []() OPTNONE {
        testing::MockFunction<int64_t()> scheduleGC;

        gcScheduler::GCSchedulerConfig config;
        config.autoTune = false;
        config.targetHeapBytes = 10;
        config.heapTriggerCoefficient = 0.9;
        gcScheduler::internal::GCSchedulerDataAggressive scheduler(config, scheduleGC.AsStdFunction());

        int i = 0;
        std::optional<int64_t> scheduled;
        // We trigger GC on the first iteration, when the unique allocation point is faced,
        // on the second to last iteration when weak target heap size is reached,
        // and on the last iteration when target heap size is reached.
        EXPECT_CALL(scheduleGC, Call())
                .WillOnce([&]() {
                    EXPECT_THAT(i, 0);
                    EXPECT_THAT(scheduled, std::nullopt);
                    scheduled = 1;
                    return 1;
                })
                .WillOnce([&]() {
                    EXPECT_THAT(i, 8);
                    EXPECT_THAT(scheduled, std::nullopt);
                    scheduled = 2;
                    return 2;
                })
                .WillOnce([&]() {
                    EXPECT_THAT(i, 9);
                    EXPECT_THAT(scheduled, std::nullopt);
                    scheduled = 3;
                    return 3;
                });

        for (; i < 10; i++) {
            scheduler.setAllocatedBytes(i + 1);
            if (scheduled) {
                scheduler.onGCFinish(*scheduled, i + 1);
                scheduled = std::nullopt;
            }
        }
        testing::Mock::VerifyAndClearExpectations(&scheduleGC);
    }();
}
