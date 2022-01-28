/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCScheduler.hpp"

#include <future>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "SingleThreadExecutor.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

using testing::_;

namespace kotlin {
namespace gc {

class GCSchedulerThreadDataTestApi : private Pinned {
public:
    explicit GCSchedulerThreadDataTestApi(GCSchedulerThreadData& scheduler) : scheduler_(scheduler) {}

    void OnSafePointRegularImpl(size_t weight) { scheduler_.OnSafePointRegularImpl(weight); }

    void SetAllocatedBytes(size_t bytes) { scheduler_.allocatedBytes_ = bytes; }

private:
    GCSchedulerThreadData& scheduler_;
};

TEST(GCSchedulerThreadDataTest, RegularSafePoint) {
    constexpr size_t kWeight = 2;
    constexpr size_t kCount = 10;
    constexpr size_t kThreshold = kCount * kWeight;
    testing::MockFunction<void(GCSchedulerThreadData&)> slowPath;
    GCSchedulerConfig config;
    config.allocationThresholdBytes = 1;
    config.threshold = kThreshold;
    GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
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
    testing::MockFunction<void(GCSchedulerThreadData&)> slowPath;
    GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = 1;
    GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
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
    testing::MockFunction<void(GCSchedulerThreadData&)> slowPath;
    GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = kThreshold;
    GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

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
    testing::MockFunction<void(GCSchedulerThreadData&)> slowPath;
    GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = kThreshold;
    GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

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

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
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
    testing::MockFunction<void(GCSchedulerThreadData&)> slowPath;
    GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = kThreshold;
    GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    config.allocationThresholdBytes = kAllocationThreshold - kSize;
    config.threshold = kThreshold - kWeight;

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold);
    });
    schedulerTestApi.OnSafePointRegularImpl(kWeight);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
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
    testing::MockFunction<void(GCSchedulerThreadData&)> slowPath;
    GCSchedulerConfig config;
    config.allocationThresholdBytes = kAllocationThreshold;
    config.threshold = kThreshold;
    GCSchedulerThreadData scheduler(config, slowPath.AsStdFunction());
    GCSchedulerThreadDataTestApi schedulerTestApi(scheduler);

    config.allocationThresholdBytes = kAllocationThreshold - kSize;
    config.threshold = kThreshold - kWeight;

    EXPECT_CALL(slowPath, Call(_)).Times(0);
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold);
    });
    scheduler.OnSafePointAllocation(kSize);
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.safePointsCounter(), kThreshold - kWeight);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        schedulerTestApi.OnSafePointRegularImpl(kWeight);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);

    EXPECT_CALL(slowPath, Call(testing::Ref(scheduler))).WillOnce([&](GCSchedulerThreadData& scheduler) {
        EXPECT_THAT(scheduler.allocatedBytes(), kAllocationThreshold - kSize);
    });
    for (size_t i = 0; i < kCount - 1; ++i) {
        scheduler.OnSafePointAllocation(kSize);
    }
    testing::Mock::VerifyAndClearExpectations(&slowPath);
    EXPECT_THAT(scheduler.allocatedBytes(), 0);
    EXPECT_THAT(scheduler.safePointsCounter(), 0);
}

using TimePoint = std::chrono::time_point<std::chrono::steady_clock>;

class MutatorThread : private Pinned {
public:
    MutatorThread(GCSchedulerConfig& config, std::function<void(GCSchedulerThreadData&)> slowPath) :
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
        GCSchedulerThreadData threadData;
        GCSchedulerThreadDataTestApi threadDataTestApi;
        std::function<void(GCSchedulerThreadData&)> slowPath;

        Context(GCSchedulerConfig& config, std::function<void(GCSchedulerThreadData&)> slowPath) :
            threadData(config, [](GCSchedulerThreadData&) {}), threadDataTestApi(threadData), slowPath(slowPath) {}
    };

    SingleThreadExecutor<Context> executor_;
};

template <compiler::GCSchedulerType schedulerType, int MutatorCount>
class GCSchedulerDataTestApi {
public:
    static constexpr TimePoint initialTime = TimePoint();

    explicit GCSchedulerDataTestApi(GCSchedulerConfig& config) {
        ON_CALL(currentTime_, Call()).WillByDefault([&]() { return time_.load(); });

        scheduler_ = internal::MakeGCSchedulerData(schedulerType, config, scheduleGC_.AsStdFunction(), currentTime_.AsStdFunction());

        mutators_.reserve(MutatorCount);
        for (int i = 0; i < MutatorCount; ++i) {
            mutators_.emplace_back(make_unique<MutatorThread>(
                    config, [this](GCSchedulerThreadData& threadData) { scheduler_->UpdateFromThreadData(threadData); }));
        }
    }

    std::future<void> Allocate(int mutator, size_t bytes) { return mutators_[mutator]->Allocate(bytes); }

    void OnPerformFullGC() { scheduler_->OnPerformFullGC(); }

    void UpdateAliveSetBytes(size_t bytes) { scheduler_->UpdateAliveSetBytes(bytes); }

    testing::MockFunction<void()>& scheduleGC() { return scheduleGC_; }

    template <typename Duration>
    void advance_time(Duration duration) {
        auto time = time_.load();
        while (true) {
            auto newTime = time + std::chrono::duration_cast<TimePoint::duration>(duration);
            if (time_.compare_exchange_weak(time, newTime)) {
                // TODO: Figure out mocking out RepeatedTimer (or clock underneath it) to avoid sleeping.
                std::this_thread::sleep_for(duration);
                return;
            }
        }
    }

private:
    std::atomic<TimePoint> time_ = initialTime;
    KStdVector<KStdUniquePtr<MutatorThread>> mutators_;
    testing::MockFunction<void()> scheduleGC_;
    testing::NiceMock<testing::MockFunction<TimePoint()>> currentTime_;
    KStdUniquePtr<GCSchedulerData> scheduler_;
};

TEST(GCSchedulerDataOnSafePoints, CollectOnTargetHeapReached) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::minutes(10)).count();
    config.autoTune = false;
    config.targetHeapBytes = (mutatorsCount + 1) * 10;
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kOnSafepoints, mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    KStdVector<std::future<void>> futures;
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

TEST(GCSchedulerDataOnSafePoints, CollectOnTimeoutReached) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::milliseconds(20)).count();
    config.autoTune = false;
    config.targetHeapBytes = std::numeric_limits<size_t>::max();
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kOnSafepoints, mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.advance_time(std::chrono::milliseconds(10));
    KStdVector<std::future<void>> futures;
    for (int i = 0; i < mutatorsCount; ++i) {
        futures.push_back(schedulerTestApi.Allocate(i, 0));
    }
    for (auto& future : futures) {
        future.get();
    }
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.advance_time(std::chrono::milliseconds(15));
    schedulerTestApi.Allocate(0, 0).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);
}

TEST(GCSchedulerDataOnSafePoints, FullTimeoutAfterLastGC) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::milliseconds(20)).count();
    config.autoTune = false;
    config.targetHeapBytes = 10;
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kOnSafepoints, mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.advance_time(std::chrono::milliseconds(10));
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.advance_time(std::chrono::milliseconds(15));
    schedulerTestApi.Allocate(0, 0).get();
    // It's now 25 ms since the start, but only 15ms since previous collection.
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.advance_time(std::chrono::milliseconds(10));
    schedulerTestApi.Allocate(0, 0).get();
    // It's now 25 ms since the previous collection.
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);
}

TEST(GCSchedulerDataOnSafePoints, DoNotTuneTargetHeap) {
    constexpr int mutatorsCount = 1;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::minutes(10)).count();
    config.autoTune = false;
    config.targetHeapBytes = 10;
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kOnSafepoints, mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(10);

    EXPECT_THAT(config.targetHeapBytes.load(), 10);
}

TEST(GCSchedulerDataOnSafePoints, TuneTargetHeap) {
    constexpr int mutatorsCount = 1;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::minutes(10)).count();
    config.autoTune = true;
    config.targetHeapBytes = 10;
    config.targetHeapUtilization = 0.5;
    config.minHeapBytes = 5;
    config.maxHeapBytes = 50;
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kOnSafepoints, mutatorsCount> schedulerTestApi(config);

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

TEST(GCSchedulerDataWithTimer, CollectOnTargetHeapReached) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::minutes(10)).count();
    config.autoTune = false;
    config.targetHeapBytes = (mutatorsCount + 1) * 10;
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kWithTimer, mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    KStdVector<std::future<void>> futures;
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

TEST(GCSchedulerDataWithTimer, CollectOnTimeoutReached) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::milliseconds(2000)).count();
    config.autoTune = false;
    config.targetHeapBytes = std::numeric_limits<size_t>::max();
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kWithTimer, mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.advance_time(std::chrono::milliseconds(1000));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.advance_time(std::chrono::milliseconds(1500));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);
}

TEST(GCSchedulerDataWithTimer, FullTimeoutAfterLastGC) {
    constexpr int mutatorsCount = kDefaultThreadCount;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::milliseconds(2000)).count();
    config.autoTune = false;
    config.targetHeapBytes = 10;
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kWithTimer, mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.advance_time(std::chrono::milliseconds(1000));
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.advance_time(std::chrono::milliseconds(1500));
    // It's now 250 ms since the start, but only 150ms since previous collection.
    // However, the timer has fired once ~50ms ago.
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call()).Times(0);
    schedulerTestApi.advance_time(std::chrono::milliseconds(1000));
    // It's now 250 ms since the previous collection, but the timer will fire in ~50ms.
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.advance_time(std::chrono::milliseconds(1000));
    // 350ms since previous collection, and the timer has fired ~50ms ago.
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(0);
}

TEST(GCSchedulerDataWithTimer, DoNotTuneTargetHeap) {
    constexpr int mutatorsCount = 1;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::minutes(10)).count();
    config.autoTune = false;
    config.targetHeapBytes = 10;
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kWithTimer, mutatorsCount> schedulerTestApi(config);

    EXPECT_CALL(schedulerTestApi.scheduleGC(), Call());
    schedulerTestApi.Allocate(0, 10).get();
    testing::Mock::VerifyAndClearExpectations(&schedulerTestApi.scheduleGC());
    schedulerTestApi.OnPerformFullGC();
    schedulerTestApi.UpdateAliveSetBytes(10);

    EXPECT_THAT(config.targetHeapBytes.load(), 10);
}

TEST(GCSchedulerDataWithTimer, TuneTargetHeap) {
    constexpr int mutatorsCount = 1;

    GCSchedulerConfig config;
    config.regularGcIntervalMicroseconds = std::chrono::microseconds(std::chrono::minutes(10)).count();
    config.autoTune = true;
    config.targetHeapBytes = 10;
    config.targetHeapUtilization = 0.5;
    config.minHeapBytes = 5;
    config.maxHeapBytes = 50;
    GCSchedulerDataTestApi<compiler::GCSchedulerType::kWithTimer, mutatorsCount> schedulerTestApi(config);

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

} // namespace gc
} // namespace kotlin
