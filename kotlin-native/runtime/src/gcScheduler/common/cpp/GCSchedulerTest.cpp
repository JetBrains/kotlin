/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCScheduler.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "GCSchedulerTestSupport.hpp"

using namespace kotlin;

using testing::_;

TEST(GCSchedulerThreadDataTest, AllocationSafePoint) {
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold);
    });
    scheduler.OnSafePointAllocation(kSize);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    scheduler.OnSafePointAllocation(kSize);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), kSize);
}

TEST(GCSchedulerThreadDataTest, ResetByGC) {
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    scheduler.OnStoppedForGC();
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
}

TEST(GCSchedulerThreadDataTest, UpdateThresholdsAfterResetByGC) {
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    config.allocationThresholdBytes = kAllocationThreshold - kSize;

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    scheduler.OnStoppedForGC();
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
}

TEST(GCSchedulerThreadDataTest, UpdateThresholdsAfterAllocationSafePoint) {
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    config.allocationThresholdBytes = kAllocationThreshold - kSize;

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold);
    });
    scheduler.OnSafePointAllocation(kSize);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
}
