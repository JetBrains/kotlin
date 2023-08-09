/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "EpochScheduler.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "TestSupport.hpp"

using namespace kotlin;

using gcScheduler::internal::EpochScheduler;
using Epoch = EpochScheduler::Epoch;

TEST(EpochSchedulerTest, ScheduleNext) {
    testing::StrictMock<testing::MockFunction<Epoch()>> scheduleGC;
    EpochScheduler adapter(scheduleGC.AsStdFunction());

    // Schedule new epoch.
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(1));
    EXPECT_THAT(adapter.scheduleNextEpoch(), 1);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Schedule already scheduled epoch.
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(1));
    EXPECT_THAT(adapter.scheduleNextEpoch(), 1);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Schedule new epoch, while the other is still in progress
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(2));
    EXPECT_THAT(adapter.scheduleNextEpoch(), 2);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Finish the first epoch.
    adapter.onGCFinish(1);

    // Schedule yet another epoch, while the other is still in progress
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(3));
    EXPECT_THAT(adapter.scheduleNextEpoch(), 3);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Finish all epochs
    adapter.onGCFinish(2);
    adapter.onGCFinish(3);

    // Schedule and finish the final epoch
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(4));
    EXPECT_THAT(adapter.scheduleNextEpoch(), 4);
    testing::Mock::VerifyAndClear(&scheduleGC);
    adapter.onGCFinish(4);
}

TEST(EpochSchedulerTest, ScheduleNextIfNotInProgress) {
    testing::StrictMock<testing::MockFunction<Epoch()>> scheduleGC;
    EpochScheduler adapter(scheduleGC.AsStdFunction());

    // Schedule new epoch.
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(1));
    EXPECT_THAT(adapter.scheduleNextEpochIfNotInProgress(), 1);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Schedule already scheduled epoch.
    EXPECT_CALL(scheduleGC, Call()).Times(0);
    EXPECT_THAT(adapter.scheduleNextEpochIfNotInProgress(), 1);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Finish the first epoch.
    adapter.onGCFinish(1);

    // Schedule and finish the final epoch
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(2));
    EXPECT_THAT(adapter.scheduleNextEpochIfNotInProgress(), 2);
    testing::Mock::VerifyAndClear(&scheduleGC);
    adapter.onGCFinish(2);
}

TEST(EpochSchedulerTest, ScheduleNextMix) {
    testing::StrictMock<testing::MockFunction<Epoch()>> scheduleGC;
    EpochScheduler adapter(scheduleGC.AsStdFunction());

    // Schedule new epoch.
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(1));
    EXPECT_THAT(adapter.scheduleNextEpochIfNotInProgress(), 1);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Schedule new epoch, while the other is still in progress
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(2));
    EXPECT_THAT(adapter.scheduleNextEpoch(), 2);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Schedule already scheduled epoch.
    EXPECT_CALL(scheduleGC, Call()).Times(0);
    EXPECT_THAT(adapter.scheduleNextEpochIfNotInProgress(), 2);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Finish the first epoch.
    adapter.onGCFinish(1);

    // Schedule already scheduled epoch again.
    EXPECT_CALL(scheduleGC, Call()).Times(0);
    EXPECT_THAT(adapter.scheduleNextEpochIfNotInProgress(), 2);
    testing::Mock::VerifyAndClear(&scheduleGC);

    // Finish the second epoch.
    adapter.onGCFinish(2);

    // Schedule and finish the final epoch
    EXPECT_CALL(scheduleGC, Call()).WillOnce(testing::Return(3));
    EXPECT_THAT(adapter.scheduleNextEpochIfNotInProgress(), 3);
    testing::Mock::VerifyAndClear(&scheduleGC);
    adapter.onGCFinish(3);
}

TEST(EpochSchedulerTest, StressScheduleNext) {
    constexpr Epoch epochsCount = 1000;

    std::mutex epochsMutex; // Protects scheduled and started relationship.
    int64_t scheduledEpoch = 0;
    int64_t startedEpoch = 0;
    int64_t completedEpoch = 0;
    auto scheduleGC = [&]() -> Epoch {
        std::unique_lock guard(epochsMutex);
        EXPECT_THAT(scheduledEpoch, testing::Ge(startedEpoch));
        if (scheduledEpoch == startedEpoch) {
            scheduledEpoch = startedEpoch + 1;
            return scheduledEpoch;
        }
        EXPECT_THAT(scheduledEpoch, startedEpoch + 1);
        return scheduledEpoch;
    };
    EpochScheduler adapter(std::move(scheduleGC));
    auto startGC = [&]() -> bool {
        std::unique_lock guard(epochsMutex);
        if (startedEpoch == scheduledEpoch) return false;
        EXPECT_THAT(startedEpoch, scheduledEpoch - 1);
        startedEpoch = scheduledEpoch;
        return true;
    };
    auto completeGC = [&] {
        adapter.onGCFinish(startedEpoch);
        EXPECT_THAT(completedEpoch, startedEpoch - 1);
        completedEpoch = startedEpoch;
    };

    std::atomic<bool> canStop = false;
    std_support::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&, i] {
            Epoch pastEpoch = 0;
            while (!canStop.load(std::memory_order_relaxed)) {
                auto epoch = (i % 2 == 0) ? adapter.scheduleNextEpoch() : adapter.scheduleNextEpochIfNotInProgress();
                EXPECT_THAT(epoch, testing::Ge(pastEpoch));
                pastEpoch = epoch;
                std::this_thread::yield();
            }
        });
    }

    for (Epoch epoch = 0; epoch < epochsCount; ++epoch) {
        while (!startGC()) {
            std::this_thread::yield();
        }
        std::this_thread::yield();
        completeGC();
    }
    canStop.store(true, std::memory_order_relaxed);
}
