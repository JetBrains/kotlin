/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCScheduler.hpp"

#include "CompilerConstants.hpp"
#include "GlobalData.hpp"
#include "KAssert.h"
#include "Porting.h"
#include "RepeatedTimer.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

namespace {

class GCEmptySchedulerData : public gc::GCSchedulerData {
    void OnSafePoint(gc::GCSchedulerThreadData& threadData) noexcept override {}
    void OnPerformFullGC() noexcept override {}
    void UpdateAliveSetBytes(size_t bytes) noexcept override {}
};

class GCSchedulerDataWithTimer : public gc::GCSchedulerData {
public:
    explicit GCSchedulerDataWithTimer(gc::GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept :
        config_(config), scheduleGC_(std::move(scheduleGC)), timer_(std::chrono::microseconds(config_.regularGcIntervalUs), [this]() {
            OnTimer();
            return std::chrono::microseconds(config_.regularGcIntervalUs);
        }) {}

    void OnSafePoint(gc::GCSchedulerThreadData& threadData) noexcept override {
        size_t allocatedBytes = threadData.allocatedBytes();
        if (allocatedBytes > config_.allocationThresholdBytes) {
            RuntimeAssert(static_cast<bool>(scheduleGC_), "scheduleGC_ cannot be empty");
            scheduleGC_();
        }
    }

    void OnPerformFullGC() noexcept override {}

    void UpdateAliveSetBytes(size_t bytes) noexcept override {}

private:
    void OnTimer() noexcept {
        auto allThreadsAreNative = []() {
            auto threadRegistryIter = mm::GlobalData::Instance().threadRegistry().LockForIter();
            return std::all_of(threadRegistryIter.begin(), threadRegistryIter.end(), [](mm::ThreadData& thread) {
                return thread.state() == ThreadState::kNative;
            });
        }();
        // Don't run, if kotlin code is not being executed.
        if (allThreadsAreNative) return;

        // TODO: Probably makes sense to check memory usage of the process.
        scheduleGC_();
    }

    gc::GCSchedulerConfig& config_;

    std::function<void()> scheduleGC_;
    RepeatedTimer timer_;
};

class GCSchedulerDataWithoutTimer : public gc::GCSchedulerData {
public:
    using CurrentTimeCallback = std::function<uint64_t()>;

    GCSchedulerDataWithoutTimer(
            gc::GCSchedulerConfig& config, std::function<void()> scheduleGC, CurrentTimeCallback currentTimeCallbackNs) noexcept :
        config_(config),
        currentTimeCallbackNs_(std::move(currentTimeCallbackNs)),
        timeOfLastGcNs_(currentTimeCallbackNs_()),
        scheduleGC_(std::move(scheduleGC)) {}

    void OnSafePoint(gc::GCSchedulerThreadData& threadData) noexcept override {
        size_t allocatedBytes = threadData.allocatedBytes();
        if (allocatedBytes > config_.allocationThresholdBytes ||
            currentTimeCallbackNs_() - timeOfLastGcNs_ >= config_.cooldownThresholdNs) {
            RuntimeAssert(static_cast<bool>(scheduleGC_), "scheduleGC_ cannot be empty");
            scheduleGC_();
        }
    }

    void OnPerformFullGC() noexcept override { timeOfLastGcNs_ = currentTimeCallbackNs_(); }

    void UpdateAliveSetBytes(size_t bytes) noexcept override {}

private:
    gc::GCSchedulerConfig& config_;
    CurrentTimeCallback currentTimeCallbackNs_;

    std::atomic<uint64_t> timeOfLastGcNs_;
    std::function<void()> scheduleGC_;
};


KStdUniquePtr<gc::GCSchedulerData> MakeEmptyGCSchedulerData() noexcept {
    return ::make_unique<GCEmptySchedulerData>();
}

KStdUniquePtr<gc::GCSchedulerData> MakeGCSchedulerDataWithTimer(
        gc::GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept {
    return ::make_unique<GCSchedulerDataWithTimer>(config, std::move(scheduleGC));
}

KStdUniquePtr<gc::GCSchedulerData> MakeGCSchedulerDataWithoutTimer(
        gc::GCSchedulerConfig& config, std::function<void()> scheduleGC, std::function<uint64_t()> currentTimeCallbackNs) noexcept {
    return ::make_unique<GCSchedulerDataWithoutTimer>(config, std::move(scheduleGC), std::move(currentTimeCallbackNs));
}

} // namespace

KStdUniquePtr<gc::GCSchedulerData> kotlin::gc::MakeGCSchedulerData(SchedulerType type, gc::GCSchedulerConfig& config, std::function<void()> scheduleGC) noexcept {
    switch (type) {
        case SchedulerType::kDisabled:
            return MakeEmptyGCSchedulerData();
        case SchedulerType::kWithTimer:
            return MakeGCSchedulerDataWithTimer(config, std::move(scheduleGC));
        case SchedulerType::kOnSafepoints:
            return MakeGCSchedulerDataWithoutTimer(config, std::move(scheduleGC), []() { return konan::getTimeNanos(); });
    }
}


void gc::GCScheduler::SetScheduleGC(std::function<void()> scheduleGC) noexcept {
    RuntimeAssert(static_cast<bool>(scheduleGC), "scheduleGC cannot be empty");
    RuntimeAssert(!static_cast<bool>(scheduleGC_), "scheduleGC must not have been set");
    scheduleGC_ = std::move(scheduleGC);
    RuntimeAssert(gcData_ == nullptr, "gcData_ must not be set prior to scheduleGC call");
    gcData_ = MakeGCSchedulerData(compiler::getGCSchedulerType(), config_, scheduleGC_);
}
