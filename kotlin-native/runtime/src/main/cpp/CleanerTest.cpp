/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Cleaner.h"

#include <future>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Atomic.h"
#include "TestSupport.hpp"
#include "TestSupportCompilerGenerated.hpp"
#include "Types.h"

using testing::_;

// TODO: Also test disposal. (This requires extracting Worker interface)

TEST(CleanerTest, ConcurrentCreation) {
    ResetCleanerWorkerForTests();

    constexpr int threadCount = kotlin::kDefaultThreadCount;
    constexpr KInt workerId = 42;

    auto createCleanerWorkerMock = ScopedCreateCleanerWorkerMock();

    EXPECT_CALL(*createCleanerWorkerMock, Call()).Times(1).WillOnce(testing::Return(workerId));

    int startedThreads = 0;
    bool allowRunning = false;
    KStdVector<std::future<KInt>> futures;
    for (int i = 0; i < threadCount; ++i) {
        auto future = std::async(std::launch::async, [&startedThreads, &allowRunning]() {
            atomicAdd(&startedThreads, 1);
            while (!atomicGet(&allowRunning)) {
            }
            return Kotlin_CleanerImpl_getCleanerWorker();
        });
        futures.push_back(std::move(future));
    }
    while (atomicGet(&startedThreads) != threadCount) {
    }
    atomicSet(&allowRunning, true);
    KStdVector<KInt> values;
    for (auto& future : futures) {
        values.push_back(future.get());
    }

    ASSERT_THAT(values.size(), threadCount);
    EXPECT_THAT(values, testing::Each(workerId));
}

TEST(CleanerTest, ShutdownWithoutCreation) {
    ResetCleanerWorkerForTests();

    auto createCleanerWorkerMock = ScopedCreateCleanerWorkerMock();
    auto shutdownCleanerWorkerMock = ScopedShutdownCleanerWorkerMock();

    EXPECT_CALL(*createCleanerWorkerMock, Call()).Times(0);
    EXPECT_CALL(*shutdownCleanerWorkerMock, Call(_, _)).Times(0);
    ShutdownCleaners(true);
}

TEST(CleanerTest, ShutdownWithCreation) {
    ResetCleanerWorkerForTests();

    constexpr KInt workerId = 42;
    constexpr bool executeScheduledCleaners = true;

    auto createCleanerWorkerMock = ScopedCreateCleanerWorkerMock();
    auto shutdownCleanerWorkerMock = ScopedShutdownCleanerWorkerMock();

    EXPECT_CALL(*createCleanerWorkerMock, Call()).WillOnce(testing::Return(workerId));
    Kotlin_CleanerImpl_getCleanerWorker();

    EXPECT_CALL(*shutdownCleanerWorkerMock, Call(workerId, executeScheduledCleaners));
    ShutdownCleaners(executeScheduledCleaners);
}
