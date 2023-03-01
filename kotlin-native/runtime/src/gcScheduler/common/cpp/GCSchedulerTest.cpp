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

TEST(GCSchedulerThreadDataTest, RegularSafePoint) {
    constexpr size_t kWeight = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kThreshold = kCount * kWeight;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = 1;
    config.threshold = kThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), 0);
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold);
    });
    schedulerTestApi.OnSafePointRegularImpl(kWeight);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    schedulerTestApi.OnSafePointRegularImpl(kWeight);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), kWeight);
}

TEST(GCSchedulerThreadDataTest, AllocationSafePoint) {
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = 1;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold);
        EXPECT_THAT(scheduler.safePointsCounter(), 0);
    });
    scheduler.OnSafePointAllocation(kSize);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    scheduler.OnSafePointAllocation(kSize);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), kSize);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);
}

TEST(GCSchedulerThreadDataTest, ResetByGC) {
    constexpr size_t kWeight = 2;
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kThreshold = kCount * kWeight;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = kThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    scheduler.OnStoppedForGC();
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);
}

TEST(GCSchedulerThreadDataTest, UpdateThresholdsAfterResetByGC) {
    constexpr size_t kWeight = 2;
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kThreshold = kCount * kWeight;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = kThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    config.allocationThresholdBytes = kAllocationThreshold - kSize;
    config.threshold = kThreshold - kWeight;

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
        scheduler.OnSafePointAllocation(kSize);
    }
    scheduler.OnStoppedForGC();
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);
}

TEST(GCSchedulerThreadDataTest, UpdateThresholdsAfterRegularSafePoint) {
    constexpr size_t kWeight = 2;
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kThreshold = kCount * kWeight;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = kThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    config.allocationThresholdBytes = kAllocationThreshold - kSize;
    config.threshold = kThreshold - kWeight;

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold);
    });
    schedulerTestApi.OnSafePointRegularImpl(kWeight);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);
}

TEST(GCSchedulerThreadDataTest, UpdateThresholdsAfterAllocationSafePoint) {
    constexpr size_t kWeight = 2;
    constexpr size_t kSize = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kThreshold = kCount * kWeight;
    constexpr size_t kAllocationThreshold = kCount * kSize;
    testing::MockFunction<void(gcScheduler::GCSchedulerThreadData&)> slowPath;
    gcScheduler::GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = kThreshold;
    gcScheduler::GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    gcScheduler::test_support::GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    config.allocationThresholdBytes = kAllocationThreshold - kSize;
    config.threshold = kThreshold - kWeight;

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold);
    });
    scheduler.OnSafePointAllocation(kSize);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](gcScheduler::GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);
}
