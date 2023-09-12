/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SafePoint.hpp"

#include <atomic>
#include <optional>
#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ScopedThread.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

TEST(SafePointTest, SafePointActivator) {
    ASSERT_FALSE(mm::test_support::safePointsAreActive());
    {
        mm::SafePointActivator activator;
        ASSERT_TRUE(mm::test_support::safePointsAreActive());
    }
    ASSERT_FALSE(mm::test_support::safePointsAreActive());
}

TEST(SafePointTest, SafePointActivatorNested) {
    ASSERT_FALSE(mm::test_support::safePointsAreActive());
    {
        mm::SafePointActivator activator;
        ASSERT_TRUE(mm::test_support::safePointsAreActive());
        {
            mm::SafePointActivator activator;
            ASSERT_TRUE(mm::test_support::safePointsAreActive());
        }
        ASSERT_TRUE(mm::test_support::safePointsAreActive());
    }
    ASSERT_FALSE(mm::test_support::safePointsAreActive());
}

TEST(SafePointTest, SafePointActivatorMove) {
    ASSERT_FALSE(mm::test_support::safePointsAreActive());
    {
        std::optional<mm::SafePointActivator> activator;
        ASSERT_FALSE(mm::test_support::safePointsAreActive());
        {
            mm::SafePointActivator innerActivator;
            ASSERT_TRUE(mm::test_support::safePointsAreActive());
            activator = std::move(innerActivator);
        }
        ASSERT_TRUE(mm::test_support::safePointsAreActive());
    }
    ASSERT_FALSE(mm::test_support::safePointsAreActive());
}

TEST(SafePointTest, StressSafePointActivator) {
    std::atomic<size_t> initialized = 0;
    std::atomic<bool> canStart = false;
    std::atomic<size_t> started = 0;
    std::atomic<bool> canStop = false;
    std::atomic<size_t> stopped = 0;
    std::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&]() noexcept {
            initialized.fetch_add(1, std::memory_order_relaxed);
            while (!canStart.load(std::memory_order_relaxed)) {
                std::this_thread::yield();
            }
            {
                mm::SafePointActivator activator;
                started.fetch_add(1, std::memory_order_relaxed);
                while (!canStop.load(std::memory_order_relaxed)) {
                    std::this_thread::yield();
                }
            }
            stopped.fetch_add(1, std::memory_order_relaxed);
        });
    }
    while (initialized.load(std::memory_order_relaxed) < threads.size()) {
        std::this_thread::yield();
    }
    EXPECT_FALSE(mm::test_support::safePointsAreActive());
    canStart.store(true, std::memory_order_relaxed);
    while (true) {
        auto count = started.load(std::memory_order_relaxed);
        if (count > 0) {
            EXPECT_TRUE(mm::test_support::safePointsAreActive());
        }
        if (count == threads.size()) break;
        std::this_thread::yield();
    }
    canStop.store(true, std::memory_order_relaxed);
    while (stopped.load(std::memory_order_relaxed) < threads.size()) {
        std::this_thread::yield();
    }
    EXPECT_FALSE(mm::test_support::safePointsAreActive());
}

class SafePointActionTest : public ::testing::Test {
public:
    SafePointActionTest() noexcept {
        EXPECT_THAT(instance_, nullptr);
        instance_ = this;
    }

    ~SafePointActionTest() {
        EXPECT_THAT(instance_, this);
        instance_ = nullptr;
    }

    testing::MockFunction<void(mm::ThreadData&)>& mockSafePoint() noexcept { return mockSafePoint_; }

    static void action(mm::ThreadData& thread) noexcept { instance_->mockSafePoint_.Call(thread); }

private:
    static SafePointActionTest* instance_;

    testing::StrictMock<testing::MockFunction<void(mm::ThreadData&)>> mockSafePoint_;
};

// static
SafePointActionTest* SafePointActionTest::instance_ = nullptr;

TEST_F(SafePointActionTest, SafePoint) {
    RunInNewThread([this](mm::ThreadData& thread) noexcept {
        mm::test_support::setSafePointAction(&action);

        EXPECT_CALL(mockSafePoint(), Call(testing::Ref(thread)));
        mm::safePoint();
        testing::Mock::VerifyAndClearExpectations(&mockSafePoint());

        mm::test_support::setSafePointAction(nullptr);
    });
}

TEST_F(SafePointActionTest, SafePointWithExplicitThread) {
    RunInNewThread([this](mm::ThreadData& thread) noexcept {
        mm::test_support::setSafePointAction(&action);

        EXPECT_CALL(mockSafePoint(), Call(testing::Ref(thread)));
        mm::safePoint(thread);
        testing::Mock::VerifyAndClearExpectations(&mockSafePoint());

        mm::test_support::setSafePointAction(nullptr);
    });
}

class OncePerThreadActionTest : public ::testing::Test {
public:
    struct FakeSafePointActivator : private MoveOnly {
        FakeSafePointActivator() noexcept {
            EXPECT_FALSE(mm::test_support::safePointsAreActive());
            mm::test_support::setSafePointAction([](mm::ThreadData& thread) {
                if (instance_->actionReady_) {
                    TestAction::getUtilityData(thread).onSafePoint();
                }
            });
        }
        ~FakeSafePointActivator() {
            EXPECT_TRUE(mm::test_support::safePointsAreActive());
            mm::test_support::setSafePointAction(nullptr);
        }
    };

    struct TestAction : public mm::OncePerThreadAction<TestAction, FakeSafePointActivator> {
        static OncePerThreadAction::ThreadData& getUtilityData(mm::ThreadData& threadData) {
            return instance_->actionUtilData_.find(&threadData)->second;
        }
        static void action(mm::ThreadData& threadData) noexcept {
            instance_->mockAction_.Call(threadData);
        }
    };

    OncePerThreadActionTest() {
        EXPECT_THAT(instance_, nullptr);
        instance_ = this;
    }

    ~OncePerThreadActionTest() {
        EXPECT_THAT(instance_, this);
        instance_ = nullptr;
    }

    void registerThread(mm::ThreadData& thread) {
        auto [_, inserted] = actionUtilData_.try_emplace(&thread, testAction_, thread);
        EXPECT_TRUE(inserted);
    }

    void setActionReady() {
        actionReady_ = true;
    }

    auto& testAction() { return testAction_; }
    auto& mockAction() { return mockAction_; }

private:
    static OncePerThreadActionTest* instance_;
    TestAction testAction_;
    std::unordered_map<mm::ThreadData*, TestAction::ThreadData> actionUtilData_;
    testing::StrictMock<testing::MockFunction<void(mm::ThreadData&)>> mockAction_;
    std::atomic<bool> actionReady_ = false;
};

// static
OncePerThreadActionTest* OncePerThreadActionTest::instance_ = nullptr;

TEST_F(OncePerThreadActionTest, StressOncePerThreadAction) {
    constexpr auto kIterations = 10;

    std::atomic<size_t> initialized = 0;
    std::mutex initializationMutex_;

    std::atomic<bool> canStop = false;

    std::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&]() noexcept {
            ScopedMemoryInit memory;
            mm::ThreadData& threadData = *memory.memoryState()->GetThreadData();
            {
                std::unique_lock initLock(initializationMutex_);
                registerThread(threadData);
            }
            initialized += 1;

            while (!canStop.load(std::memory_order_relaxed)) {
                std::this_thread::yield();

                {
                    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
                    std::this_thread::yield();
                }

                mm::safePoint();
            }
        });
    }

    while (initialized.load(std::memory_order_relaxed) < threads.size()) {
        std::this_thread::yield();
    }
    std::unique_lock initLock(initializationMutex_);
    setActionReady();

    for (int i = 0; i < kIterations; ++i) {
        auto threadRegistry = mm::ThreadRegistry::Instance().LockForIter();
        for (auto& thread: threadRegistry) {
            EXPECT_CALL(mockAction(), Call(testing::Ref(thread))).Times(1);
        }
        testAction().ensurePerformed(threadRegistry);
        testing::Mock::VerifyAndClearExpectations(&mockAction());

        std::this_thread::yield();
    }

    canStop = true;
}
