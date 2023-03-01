/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "AppStateTrackingTestSupport.hpp"
#include "ClockTestSupport.hpp"
#include "GCSchedulerTestSupport.hpp"
#include "SingleThreadExecutor.hpp"
#include "TestSupport.hpp"
#include "std_support/Vector.hpp"

using namespace kotlin;

namespace {

class MutatorThread : private Pinned {
public:
    MutatorThread(gcScheduler::GCSchedulerConfig& config, std::function<void(gcScheduler::GCSchedulerThreadData&)> slowPath) :
        executor_([&config, slowPath = std::move(slowPath)] { return Context(config, std::move(slowPath)); }) {}

    std::future<void> Allocate(size_t bytes) {
        return executor_.execute([&, bytes] {
            auto& context = executor_.context();
            context.threadDataTestApi.SetAllocatedBytes(bytes);
            context.slowPath(context.threadData);
        });
    }

private:
    struct Context {
        gcScheduler::GCSchedulerThreadData threadData;
        gcScheduler::test_support::GCSchedulerThreadDataTestApi threadDataTestApi;
        std::function<void(gcScheduler::GCSchedulerThreadData&)> slowPath;

        Context(gcScheduler::GCSchedulerConfig& config, std::function<void(gcScheduler::GCSchedulerThreadData&)> slowPath) :
            threadData(config, [](gcScheduler::GCSchedulerThreadData&) {}), threadDataTestApi(threadData), slowPath(slowPath) {}
    };

    SingleThreadExecutor<Context> executor_;
};

template <int MutatorCount>
class GCSchedulerDataTestApi {
public:
    explicit GCSchedulerDataTestApi(gcScheduler::GCSchedulerConfig& config) : scheduler_(config, scheduleGC_.AsStdFunction()) {
        mutators_.reserve(MutatorCount);
        for (int i = 0; i < MutatorCount; ++i) {
            mutators_.emplace_back(std_support::make_unique<MutatorThread>(
                    config, [this](gcScheduler::GCSchedulerThreadData& threadData) { scheduler_.UpdateFromThreadData(threadData); }));
        }
    }

    std::future<void> Allocate(int mutator, size_t bytes) { return mutators_[mutator]->Allocate(bytes); }

    void OnPerformFullGC() { scheduler_.OnPerformFullGC(); }

    void UpdateAliveSetBytes(size_t bytes) { scheduler_.UpdateAliveSetBytes(bytes); }

    testing::MockFunction<void()>& scheduleGC() { return scheduleGC_; }

    template <typename Duration>
    void advance_time(Duration duration) {
        test_support::manual_clock::sleep_for(duration);
    }

private:
    std_support::vector<std_support::unique_ptr<MutatorThread>> mutators_;
    testing::MockFunction<void()> scheduleGC_;
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
    GCSchedulerDataTestApi<mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    std_support::vector<std::future<void>> futures;
    for (int i = 0; i < mutatorsCount; ++i) {
        futures.push_back(schedulerTestApi.Allocate(i, 10));
    }
    for (auto& future : futures) {
        future.get();
    }
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, mutatorsCount * 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);
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

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.advance_time(microseconds(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);
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
    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);

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

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(10);

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

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(10);

    EXPECT_THAT(config.targetHeapBytes.load(), 20);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    // For a total heap of 20.
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(20);

    EXPECT_THAT(config.targetHeapBytes.load(), 40);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    // For a total heap of 60.
    schedulerTestApi.Allocate(0, 40).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(60);

    // But we will keep the 50, which means we will trigger GC every allocation, until alive set falls down
    EXPECT_THAT(config.targetHeapBytes.load(), 50);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    // Keeping total heap of 60.
    schedulerTestApi.Allocate(0, 0).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(60);

    EXPECT_THAT(config.targetHeapBytes.load(), 50);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, 0).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    // Dropping to 40
    schedulerTestApi.UpdateAliveSetBytes(40);

    EXPECT_THAT(config.targetHeapBytes.load(), 50);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    // For a total heap of 50
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    // Dropping to 1
    schedulerTestApi.UpdateAliveSetBytes(1);

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

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.advance_time(microseconds(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + microseconds(10));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);
}
