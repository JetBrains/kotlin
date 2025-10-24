/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MutatorAssists.hpp"

#include <atomic>
#include <map>
#include <shared_mutex>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "SafePoint.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

using gcScheduler::internal::MutatorAssists;
using Epoch = MutatorAssists::Epoch;

class MutatorAssistsTest : public ::testing::Test {
public:
    class Mutator {
    public:
        template <typename F>
        Mutator(MutatorAssistsTest& owner, F&& f) noexcept :
            owner_(owner), thread_([f = std::forward<F>(f), this]() noexcept {
                ScopedMemoryInit memory;
                {
                    std::unique_lock guard(initializedMutex_);
                    threadData_ = &memory.threadData();
                    assists_.emplace(owner_.assists_, *threadData_);
                }
                owner_.registerMutator(*this);
                initialized_.notify_one();
                f(*this);
            }) {
            std::unique_lock guard(initializedMutex_);
            initialized_.wait(guard, [this] { return threadData_ && assists_.has_value(); });
        }

        ~Mutator() {
            thread_.join();
            owner_.unregisterMutator(*this);
        }

        mm::ThreadData& threadData() noexcept { return *threadData_; }
        MutatorAssists::ThreadData& assists() noexcept { return *assists_; }

    private:
        friend MutatorAssistsTest;

        MutatorAssistsTest& owner_;
        std::condition_variable initialized_;
        std::mutex initializedMutex_;
        mm::ThreadData* threadData_;
        std::optional<MutatorAssists::ThreadData> assists_;
        ScopedThread thread_;
    };

    void requestAssists(Epoch epoch) noexcept { assists_.requestAssists(epoch); }

    void completeEpoch(Epoch epoch) noexcept {
        assists_.completeEpoch(epoch, [this](mm::ThreadData& threadData) noexcept -> MutatorAssists::ThreadData& {
            return getMutator(threadData).assists();
        });
    }

    Epoch completedEpoch(std::memory_order order) noexcept { return assists_.completedEpoch(order); }

    void safePoint() noexcept {
        if (!mm::test_support::safePointsAreActive()) return;
        auto& threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
        getMutator(threadData).assists().safePoint();
    }

private:
    void registerMutator(Mutator& mutator) noexcept {
        std::unique_lock guard(mutatorMapMutex_);
        auto [_, inserted] = mutatorMap_.insert(std::make_pair(&mutator.threadData(), &mutator));
        RuntimeAssert(inserted, "Mutator was already inserted");
    }

    void unregisterMutator(Mutator& mutator) noexcept {
        std::unique_lock guard(mutatorMapMutex_);
        auto count = mutatorMap_.erase(&mutator.threadData());
        RuntimeAssert(count == 1, "Mutator must be in the map");
    }

    Mutator& getMutator(mm::ThreadData& threadData) noexcept {
        std::shared_lock guard(mutatorMapMutex_);
        auto it = mutatorMap_.find(&threadData);
        RuntimeAssert(it != mutatorMap_.end(), "Mutator must be in the map");
        return *it->second;
    }

    MutatorAssists assists_;
    RWSpinLock mutatorMapMutex_;
    std::map<mm::ThreadData*, Mutator*> mutatorMap_;
};

TEST_F(MutatorAssistsTest, EnableSafePointsWhenRequestingAssists) {
    ASSERT_FALSE(mm::test_support::safePointsAreActive());
    requestAssists(1);
    EXPECT_TRUE(mm::test_support::safePointsAreActive());
    completeEpoch(1);
    EXPECT_FALSE(mm::test_support::safePointsAreActive());
}

TEST_F(MutatorAssistsTest, EnableSafePointsWithNestedRequest) {
    ASSERT_FALSE(mm::test_support::safePointsAreActive());
    requestAssists(1);
    ASSERT_TRUE(mm::test_support::safePointsAreActive());
    requestAssists(2);
    EXPECT_TRUE(mm::test_support::safePointsAreActive());
    completeEpoch(1);
    EXPECT_TRUE(mm::test_support::safePointsAreActive());
    completeEpoch(2);
    EXPECT_FALSE(mm::test_support::safePointsAreActive());
}

TEST_F(MutatorAssistsTest, StressEnableSafePointsByMutators) {
    constexpr Epoch epochsCount = 4;
    std::array<std::atomic<bool>, epochsCount> enabled = {false};
    std::atomic<bool> canStart = false;
    std::atomic<bool> canStop = false;
    std::vector<std::unique_ptr<Mutator>> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back(std::make_unique<Mutator>(*this, [&, i](Mutator&) noexcept {
            while (!canStart.load(std::memory_order_relaxed)) {
                std::this_thread::yield();
            }
            requestAssists((i % epochsCount) + 1);
            enabled[i % epochsCount].store(true, std::memory_order_relaxed);
            while (!canStop.load(std::memory_order_relaxed)) {
                safePoint();
            }
        }));
    }

    ASSERT_FALSE(mm::test_support::safePointsAreActive());
    canStart.store(true, std::memory_order_relaxed);
    for (Epoch i = 0; i < epochsCount; ++i) {
        while (!enabled[i].load(std::memory_order_relaxed)) {
            std::this_thread::yield();
        }
        EXPECT_TRUE(mm::test_support::safePointsAreActive());
        completeEpoch(i + 1);
    }
    EXPECT_FALSE(mm::test_support::safePointsAreActive());
    canStop.store(true, std::memory_order_relaxed);
}

TEST_F(MutatorAssistsTest, Assist) {
    constexpr Epoch epochsCount = 4;
    struct EpochData {
        std::atomic<bool> canStart = false;
        std::atomic<size_t> started = 0;
        std::atomic<size_t> finished = 0;

        void waitToStart() noexcept {
            while (!canStart.load(std::memory_order_acquire)) {
                std::this_thread::yield();
            }
            started.fetch_add(1, std::memory_order_release);
        }

        void start(size_t expectedCount) noexcept {
            canStart.store(std::memory_order_release);
            while (started.load(std::memory_order_acquire) < expectedCount) {
                std::this_thread::yield();
            }
        }

        void finish() noexcept {
            finished.fetch_add(1, std::memory_order_release);
        }

        void waitFinished(size_t expectedCount) noexcept {
            while (finished.load(std::memory_order_acquire) < expectedCount) {
                std::this_thread::yield();
            }
        }
    };
    std::vector<EpochData> epochs(epochsCount);
    std::vector<std::unique_ptr<Mutator>> mutators;
    std::atomic<bool> canStop = false;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back(std::make_unique<Mutator>(*this, [&](Mutator&) noexcept {
            for (Epoch epoch = 0; epoch < epochsCount; ++epoch) {
                epochs[epoch].waitToStart();
                safePoint();
                EXPECT_THAT(completedEpoch(std::memory_order_relaxed), epoch + 1);
                epochs[epoch].finish();
            }
            while (!canStop.load(std::memory_order_acquire)) {
                std::this_thread::yield();
            }
        }));
    }
    for (auto& m : mutators) {
        EXPECT_THAT(m->threadData().state(), ThreadState::kRunnable);
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, 0);
        EXPECT_FALSE(waiting);
    }
    for (Epoch epoch = 0; epoch < epochsCount; ++epoch) {
        requestAssists(epoch + 1);
        epochs[epoch].start(mutators.size());
        for (auto& m : mutators) {
            Epoch waitingEpoch = 0;
            bool waiting = false;
            while (true) {
                std::tie(waitingEpoch, waiting) = m->assists().startedWaiting(std::memory_order_acquire);
                if (waiting) break;
                EXPECT_THAT(waitingEpoch, epoch); // If not waiting, should be exactly the previous epoch.
                std::this_thread::yield();
            }
            EXPECT_THAT(waitingEpoch, epoch + 1);
            EXPECT_THAT(m->threadData().state(), ThreadState::kNative);
        }
        completeEpoch(epoch + 1);
        epochs[epoch].waitFinished(mutators.size());
        for (auto& m : mutators) {
            EXPECT_THAT(m->threadData().state(), ThreadState::kRunnable);
            auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
            EXPECT_THAT(waitingEpoch, epoch + 1);
            EXPECT_FALSE(waiting);
        }
    }
    canStop.store(true, std::memory_order_release);
    mutators.clear();
}

TEST_F(MutatorAssistsTest, AssistNoSync) {
    constexpr Epoch epochsCount = 10000;
    std::atomic<bool> canStop = false;
    std::atomic<size_t> finished = 0;
    std::vector<std::unique_ptr<Mutator>> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back(std::make_unique<Mutator>(*this, [&](Mutator&) noexcept {
            while (!canStop.load(std::memory_order_relaxed)) {
                safePoint();
                std::this_thread::yield();
            }
            finished.fetch_add(1, std::memory_order_relaxed);
        }));
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, 0);
        EXPECT_FALSE(waiting);
    }
    for (Epoch epoch = 0; epoch < epochsCount; ++epoch) {
        requestAssists(epoch + 1);
        completeEpoch(epoch + 1);
    }
    canStop.store(true, std::memory_order_relaxed);
    while (finished.load(std::memory_order_relaxed) < mutators.size()) {
        std::this_thread::yield();
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, testing::Le(epochsCount));
        EXPECT_FALSE(waiting);
    }
}

TEST_F(MutatorAssistsTest, AssistWithNativeMutators) {
    constexpr Epoch epochsCount = 10000;
    std::atomic<bool> canStop = false;
    std::atomic<size_t> finished = 0;
    std::vector<std::unique_ptr<Mutator>> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back(std::make_unique<Mutator>(*this, [&, i](Mutator&) noexcept {
            if (i % 2 == 0) {
                ThreadStateGuard guard(ThreadState::kNative);
                while (!canStop.load(std::memory_order_relaxed)) {
                    std::this_thread::yield();
                }
            } else {
                while (!canStop.load(std::memory_order_relaxed)) {
                    safePoint();
                    std::this_thread::yield();
                }
            }
            finished.fetch_add(1, std::memory_order_relaxed);
        }));
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, 0);
        EXPECT_FALSE(waiting);
    }
    for (Epoch epoch = 0; epoch < epochsCount; ++epoch) {
        requestAssists(epoch + 1);
        completeEpoch(epoch + 1);
    }
    canStop.store(true, std::memory_order_relaxed);
    while (finished.load(std::memory_order_relaxed) < mutators.size()) {
        std::this_thread::yield();
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, testing::Le(epochsCount));
        EXPECT_FALSE(waiting);
    }
}

TEST_F(MutatorAssistsTest, AssistNoRequests) {
    constexpr Epoch epochsCount = 10000;
    std::atomic<bool> canStart = false;
    std::atomic<bool> canStop = false;
    std::atomic<size_t> started = 0;
    std::atomic<size_t> finished = 0;
    std::vector<std::unique_ptr<Mutator>> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back(std::make_unique<Mutator>(*this, [&](Mutator&) noexcept {
            while (!canStart.load(std::memory_order_relaxed)) {
                std::this_thread::yield();
            }
            started.fetch_add(1, std::memory_order_relaxed);
            while (!canStop.load(std::memory_order_relaxed)) {
                safePoint();
                std::this_thread::yield();
            }
            finished.fetch_add(1, std::memory_order_relaxed);
        }));
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, 0);
        EXPECT_FALSE(waiting);
    }
    canStart.store(true, std::memory_order_relaxed);
    for (Epoch epoch = 0; epoch < epochsCount; ++epoch) {
        completeEpoch(epoch + 1);
    }
    canStop.store(true, std::memory_order_relaxed);
    while (finished.load(std::memory_order_relaxed) < mutators.size()) {
        std::this_thread::yield();
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, 0);
        EXPECT_FALSE(waiting);
    }
}

TEST_F(MutatorAssistsTest, AssistRequestsByMutators) {
    constexpr Epoch epochsCount = 100;
    std::atomic<bool> canStart = false;
    std::atomic<bool> canStop = false;
    std::atomic<size_t> started = 0;
    std::atomic<size_t> finished = 0;
    std::atomic<Epoch> currentEpoch = 0;
    std::vector<std::unique_ptr<Mutator>> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back(std::make_unique<Mutator>(*this, [&, i](Mutator&) noexcept {
            while (!canStart.load(std::memory_order_relaxed)) {
                std::this_thread::yield();
            }
            started.fetch_add(1, std::memory_order_relaxed);
            while (!canStop.load(std::memory_order_relaxed)) {
                if (i % 2 != 0) {
                    auto epoch = currentEpoch.load(std::memory_order_relaxed);
                    requestAssists(epoch + 1);
                }
                safePoint();
                std::this_thread::yield();
            }
            finished.fetch_add(1, std::memory_order_relaxed);
        }));
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, 0);
        EXPECT_FALSE(waiting);
    }
    canStart.store(true, std::memory_order_relaxed);
    for (Epoch epoch = 0; epoch < epochsCount; ++epoch) {
        currentEpoch.store(epoch, std::memory_order_relaxed);
        completeEpoch(epoch + 1);
    }
    canStop.store(true, std::memory_order_relaxed);
    while (finished.load(std::memory_order_relaxed) < mutators.size()) {
        std::this_thread::yield();
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, testing::Le(epochsCount));
        EXPECT_FALSE(waiting);
    }
}

TEST_F(MutatorAssistsTest, AssistRequestsByMutatorsIntoTheFuture) {
    constexpr Epoch epochsCount = 100;
    std::atomic<bool> canStart = false;
    std::atomic<bool> canStop = false;
    std::atomic<size_t> started = 0;
    std::atomic<size_t> finished = 0;
    std::mutex mutexEpoch;
    Epoch scheduledEpoch = 0;
    Epoch currentEpoch = 0;
    auto scheduleGC = [&]() noexcept -> Epoch {
        std::unique_lock guard(mutexEpoch);
        if (scheduledEpoch > currentEpoch) return scheduledEpoch;
        scheduledEpoch = currentEpoch + 1;
        return scheduledEpoch;
    };
    std::vector<std::unique_ptr<Mutator>> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back(std::make_unique<Mutator>(*this, [&, i](Mutator&) noexcept {
            while (!canStart.load(std::memory_order_relaxed)) {
                std::this_thread::yield();
            }
            started.fetch_add(1, std::memory_order_relaxed);
            while (!canStop.load(std::memory_order_relaxed)) {
                if (i % 2 != 0) {
                    auto epoch = scheduleGC();
                    requestAssists(epoch);
                }
                safePoint();
                std::this_thread::yield();
            }
            finished.fetch_add(1, std::memory_order_relaxed);
        }));
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, 0);
        EXPECT_FALSE(waiting);
    }
    canStart.store(true, std::memory_order_relaxed);
    for (Epoch epoch = 1; epoch <= epochsCount; ++epoch) {
        {
            std::unique_lock guard(mutexEpoch);
            currentEpoch = epoch;
            EXPECT_THAT(currentEpoch, testing::Ge(scheduledEpoch));
        }
        completeEpoch(epoch);
    }
    canStop.store(true, std::memory_order_relaxed);
    completeEpoch(epochsCount + 1); // The last GC.
    while (finished.load(std::memory_order_relaxed) < mutators.size()) {
        std::this_thread::yield();
    }
    for (auto& m : mutators) {
        auto [waitingEpoch, waiting] = m->assists().startedWaiting(std::memory_order_relaxed);
        EXPECT_THAT(waitingEpoch, testing::Le(epochsCount + 1));
        EXPECT_FALSE(waiting);
    }
}
