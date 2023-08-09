/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "AppStateTrackingTestSupport.hpp"
#include "ClockTestSupport.hpp"
#include "SingleThreadExecutor.hpp"
#include "TestSupport.hpp"
#include "std_support/Vector.hpp"

using namespace kotlin;

namespace {

class MutatorThread : private Pinned {
public:
    explicit MutatorThread(gcScheduler::internal::GCSchedulerDataAdaptive<test_support::manual_clock>& scheduler) :
        executor_([&scheduler] { return Context{scheduler}; }) {}

    std::future<void> SetAllocatedBytes(size_t bytes) {
        return executor_.execute([&, bytes] {
            auto& context = executor_.context();
            context.scheduler.setAllocatedBytes(bytes);
        });
    }

private:
    struct Context {
        gcScheduler::internal::GCSchedulerDataAdaptive<test_support::manual_clock>& scheduler;
    };

    SingleThreadExecutor<Context> executor_;
};

template <int MutatorCount>
class GCSchedulerDataTestApi {
public:
    explicit GCSchedulerDataTestApi(gcScheduler::GCSchedulerConfig& config) : scheduler_(config, scheduleGC_.AsStdFunction()) {
        mutators_.reserve(MutatorCount);
        for (int i = 0; i < MutatorCount; ++i) {
            mutators_.emplace_back(std_support::make_unique<MutatorThread>(scheduler_));
        }
    }

    std::future<void> Allocate(int mutator, size_t bytes) {
        size_t allocatedBytes = allocatedBytes_.fetch_add(bytes);
        allocatedBytes += bytes;
        return mutators_[mutator]->SetAllocatedBytes(allocatedBytes);
    }

    void OnPerformFullGC() { scheduler_.onGCStart(); }

    void onGCFinish(int64_t epoch, size_t bytes) {
        allocatedBytes_.store(bytes);
        scheduler_.onGCFinish(epoch, bytes);
    }

    testing::MockFunction<int64_t()>& scheduleGC() { return scheduleGC_; }

    template <typename Duration>
    void advance_time(Duration duration) {
        test_support::manual_clock::sleep_for(duration);
    }

    int64_t assistsRequested() noexcept { return scheduler_.mutatorAssists().assistsRequested(std::memory_order_relaxed); }

private:
    std::atomic<size_t> allocatedBytes_ = 0;
    std_support::vector<std_support::unique_ptr<MutatorThread>> mutators_;
    testing::MockFunction<int64_t()> scheduleGC_;
    gcScheduler::internal::GCSchedulerDataAdaptive<test_support::manual_clock> scheduler_;
};

} // namespace

class AdaptiveSchedulerTest : public ::testing::Test {
public:
    AdaptiveSchedulerTest() { test_support::manual_clock::reset(); }
};

TEST_F(AdaptiveSchedulerTest, CollectOnTargetHeapReached) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    gcScheduler::GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = 10;
    config.autoTune = false;
    config.targetHeapBytes = (mutatorsCount + 1) * 10;
    config.heapTriggerCoefficient = 0.9;
    config.setMutatorAssists(true);
    GCSchedulerDataTestApi<mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    std_support::vector<std::future<void>> futures;
    for (int i = 0; i < mutatorsCount; ++i) {
        futures.push_back(schedulerTestApi.Allocate(i, 9));
    }
    for (auto& future : futures) {
        future.get();
    }
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(1));
    schedulerTestApi.Allocate(0, 9).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    EXPECT_THAT(schedulerTestApi.assistsRequested(), 0);
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(1, 0);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(2));
    schedulerTestApi.Allocate(0, mutatorsCount * 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    EXPECT_THAT(schedulerTestApi.assistsRequested(), 2);
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(2, 0);
}

TEST_F(AdaptiveSchedulerTest, CollectOnTargetHeapReachedWithoutAssists) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    gcScheduler::GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = 10;
    config.autoTune = false;
    config.targetHeapBytes = (mutatorsCount + 1) * 10;
    config.heapTriggerCoefficient = 0.9;
    config.setMutatorAssists(false);
    GCSchedulerDataTestApi<mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    std_support::vector<std::future<void>> futures;
    for (int i = 0; i < mutatorsCount; ++i) {
        futures.push_back(schedulerTestApi.Allocate(i, 9));
    }
    for (auto& future : futures) {
        future.get();
    }
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(1));
    schedulerTestApi.Allocate(0, 9).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    EXPECT_THAT(schedulerTestApi.assistsRequested(), 0);
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(1, 0);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(2));
    schedulerTestApi.Allocate(0, mutatorsCount * 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    EXPECT_THAT(schedulerTestApi.assistsRequested(), 0);
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(2, 0);
}

TEST_F(AdaptiveSchedulerTest, CollectOnTimeoutReached) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    gcScheduler::GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = 10;
    config.autoTune = false;
    config.targetHeapBytes = std::numeric_limits<size_t>::max();
    GCSchedulerDataTestApi<mutatorsCount> schedulerTestApi(config);

    // Wait until the timer is initialized.
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(1));
    schedulerTestApi.advance_time(microseconds(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(1, 0);
}

TEST_F(AdaptiveSchedulerTest, FullTimeoutAfterLastGC) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    gcScheduler::GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = 10;
    config.autoTune = false;
    config.targetHeapBytes = 10;
    GCSchedulerDataTestApi<mutatorsCount> schedulerTestApi(config);

    // Wait until the timer is initialized.
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));

    schedulerTestApi.advance_time(microseconds(5));
    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(1));
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(1, 0);

    // pending should restart to be 10us since the previous collection without scheduling another GC.
    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
}

TEST_F(AdaptiveSchedulerTest, DoNotTuneTargetHeap) {
    constexpr int mutatorsCount = 1;

    gcScheduler::GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = 10;
    config.autoTune = false;
    config.targetHeapBytes = 10;
    GCSchedulerDataTestApi<mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(1));
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(1, 10);

    EXPECT_THAT(config.targetHeapBytes.load(), 10);
}

TEST_F(AdaptiveSchedulerTest, TuneTargetHeap) {
    constexpr int mutatorsCount = 1;

    gcScheduler::GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = 10;
    config.autoTune = true;
    config.targetHeapBytes = 10;
    config.targetHeapUtilization = 0.5;
    config.minHeapBytes = 5;
    config.maxHeapBytes = 50;
    GCSchedulerDataTestApi<mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(1));
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(1, 10);

    EXPECT_THAT(config.targetHeapBytes.load(), 20);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(2));
    // For a total heap of 20.
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(2, 20);

    EXPECT_THAT(config.targetHeapBytes.load(), 40);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(3));
    // For a total heap of 60.
    schedulerTestApi.Allocate(0, 40).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(3, 60);

    // But we will keep the 50, which means we will trigger GC every allocation, until alive set falls down
    EXPECT_THAT(config.targetHeapBytes.load(), 50);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(4));
    // Keeping total heap of 60.
    schedulerTestApi.Allocate(0, 0).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(4, 60);

    EXPECT_THAT(config.targetHeapBytes.load(), 50);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(5));
    schedulerTestApi.Allocate(0, 0).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    // Dropping to 40
    schedulerTestApi.onGCFinish(5, 40);

    EXPECT_THAT(config.targetHeapBytes.load(), 50);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(6));
    // For a total heap of 50
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    // Dropping to 1
    schedulerTestApi.onGCFinish(6, 1);

    // But the minimum is set to 5.
    EXPECT_THAT(config.targetHeapBytes.load(), 5);
}

TEST_F(AdaptiveSchedulerTest, DoNotCollectOnTimerInBackground) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    gcScheduler::GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = 10;
    config.autoTune = false;
    config.targetHeapBytes = std::numeric_limits<size_t>::max();
    GCSchedulerDataTestApi<mutatorsCount> schedulerTestApi(config);

    // TODO: Not a global, please.
    mm::AppStateTrackingTestSupport appStateTracking(mm::GlobalData::Instance().appStateTracking());

    // Wait until the timer is initialized.
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));

    // Now go into the background.
    ASSERT_THAT(mm::GlobalData::Instance().appStateTracking().state(), mm::AppStateTracking::State::kForeground);
    appStateTracking.setState(mm::AppStateTracking::State::kBackground);

    // Timer works in the background, but does nothing.
    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.advance_time(microseconds(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    // Now go back into the foreground.
    appStateTracking.setState(mm::AppStateTracking::State::kForeground);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).WillOnce(testing::Return(1));
    schedulerTestApi.advance_time(microseconds(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.onGCFinish(1, 0);
}
