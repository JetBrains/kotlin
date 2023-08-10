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
