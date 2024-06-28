/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "HeapGrowthController.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

using gcScheduler::internal::HeapGrowthController;
using MemoryBoundary = HeapGrowthController::MemoryBoundary;

TEST(HeapGrowthControllerTest, BoundariesWithAssists) {
    gcScheduler::GCSchedulerConfig config;
    config.targetHeapBytes = 10;
    config.heapTriggerCoefficient = 0.7;
    ASSERT_TRUE(config.mutatorAssists());

    HeapGrowthController controller(config);
    EXPECT_THAT(controller.targetHeapBytes(), 10);
    EXPECT_THAT(controller.triggerHeapBytes(), 7);

    for (size_t i = 0; i < 12; ++i) {
        auto expected = i < 7 ? MemoryBoundary::kNone : i < 10 ? MemoryBoundary::kTrigger : MemoryBoundary::kTarget;
        EXPECT_THAT(controller.boundaryForHeapSize(i), expected);
    }
}

TEST(HeapGrowthControllerTest, BoundariesWithoutAssists) {
    gcScheduler::GCSchedulerConfig config;
    config.targetHeapBytes = 10;
    config.heapTriggerCoefficient = 0.7;
    config.setMutatorAssists(false);

    HeapGrowthController controller(config);
    EXPECT_THAT(controller.targetHeapBytes(), 10);
    EXPECT_THAT(controller.triggerHeapBytes(), 7);

    for (size_t i = 0; i < 12; ++i) {
        auto expected = i < 7 ? MemoryBoundary::kNone : MemoryBoundary::kTrigger;
        EXPECT_THAT(controller.boundaryForHeapSize(i), expected);
    }
}

TEST(HeapGrowthControllerTest, NoTune) {
    gcScheduler::GCSchedulerConfig config;
    config.autoTune = false;
    config.targetHeapBytes = 10;
    config.heapTriggerCoefficient = 0.7;

    HeapGrowthController controller(config);
    EXPECT_THAT(controller.targetHeapBytes(), 10);
    EXPECT_THAT(controller.triggerHeapBytes(), 7);

    controller.updateBoundaries(0);
    EXPECT_THAT(controller.targetHeapBytes(), 10);
    EXPECT_THAT(controller.triggerHeapBytes(), 7);

    controller.updateBoundaries(10);
    EXPECT_THAT(controller.targetHeapBytes(), 10);
    EXPECT_THAT(controller.triggerHeapBytes(), 7);

    controller.updateBoundaries(10000);
    EXPECT_THAT(controller.targetHeapBytes(), 10);
    EXPECT_THAT(controller.triggerHeapBytes(), 7);

    controller.updateBoundaries(std::numeric_limits<uint32_t>::max());
    EXPECT_THAT(controller.targetHeapBytes(), 10);
    EXPECT_THAT(controller.triggerHeapBytes(), 7);
}

TEST(HeapGrowthControllerTest, Tune) {
    gcScheduler::GCSchedulerConfig config;
    config.autoTune = true;
    config.minHeapBytes = 10;
    config.maxHeapBytes = 1000;
    config.targetHeapBytes = 100;
    config.heapTriggerCoefficient = 0.7;
    config.targetHeapUtilization = 0.5;

    HeapGrowthController controller(config);
    EXPECT_THAT(controller.targetHeapBytes(), 100);
    EXPECT_THAT(controller.triggerHeapBytes(), 70);

    controller.updateBoundaries(0);
    EXPECT_THAT(controller.targetHeapBytes(), 10);
    EXPECT_THAT(controller.triggerHeapBytes(), 7);

    controller.updateBoundaries(10);
    EXPECT_THAT(controller.targetHeapBytes(), 20);
    EXPECT_THAT(controller.triggerHeapBytes(), 14);

    controller.updateBoundaries(100);
    EXPECT_THAT(controller.targetHeapBytes(), 200);
    EXPECT_THAT(controller.triggerHeapBytes(), 140);

    controller.updateBoundaries(10000);
    EXPECT_THAT(controller.targetHeapBytes(), 1000);
    EXPECT_THAT(controller.triggerHeapBytes(), 700);

    controller.updateBoundaries(std::numeric_limits<uint32_t>::max());
    EXPECT_THAT(controller.targetHeapBytes(), 1000);
    EXPECT_THAT(controller.triggerHeapBytes(), 700);
}
