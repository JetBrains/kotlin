/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerConfig.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

TEST(GCSchedulerConfigTest, DefaultMutatorAssists) {
    gcScheduler::GCSchedulerConfig config;
    EXPECT_TRUE(config.mutatorAssists());
    config.autoTune = false;
    EXPECT_FALSE(config.mutatorAssists());
    config.autoTune = true;
    ASSERT_TRUE(config.mutatorAssists());
    config.maxHeapBytes = 1024 * 1024 * 1024;
    EXPECT_FALSE(config.mutatorAssists());
    config.maxHeapBytes = std::numeric_limits<int64_t>::max();
    EXPECT_TRUE(config.mutatorAssists());
}

TEST(GCSchedulerConfigTest, DisabledMutatorAssists) {
    gcScheduler::GCSchedulerConfig config;
    config.setMutatorAssists(false);
    EXPECT_FALSE(config.mutatorAssists());
    config.autoTune = false;
    EXPECT_FALSE(config.mutatorAssists());
    config.autoTune = true;
    ASSERT_FALSE(config.mutatorAssists());
    config.maxHeapBytes = 1024 * 1024 * 1024;
    EXPECT_FALSE(config.mutatorAssists());
    config.maxHeapBytes = std::numeric_limits<int64_t>::max();
    EXPECT_FALSE(config.mutatorAssists());
}

TEST(GCSchedulerConfigTest, EnabledMutatorAssists) {
    gcScheduler::GCSchedulerConfig config;
    config.setMutatorAssists(true);
    EXPECT_TRUE(config.mutatorAssists());
    config.autoTune = false;
    EXPECT_TRUE(config.mutatorAssists());
    config.autoTune = true;
    ASSERT_TRUE(config.mutatorAssists());
    config.maxHeapBytes = 1024 * 1024 * 1024;
    EXPECT_TRUE(config.mutatorAssists());
    config.maxHeapBytes = std::numeric_limits<int64_t>::max();
    EXPECT_TRUE(config.mutatorAssists());
}
