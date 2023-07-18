/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConditionVariable.hpp"

#include <condition_variable>
#include <mutex>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ScopedThread.hpp"
#include "TestSupport.hpp"
#include "std_support/Vector.hpp"

using namespace kotlin;

template <typename T>
class ConditionVariableTest : public testing::Test {};

using CVTypes = testing::Types<
#ifndef KONAN_WINDOWS // winpthreads are acting strange in our mingw toolchain.
        std::condition_variable,
        std::condition_variable_any,
#endif
        ConditionVariableSpin>;
class CVNames {
public:
    template <typename T>
    static std::string GetName(int) {
        if constexpr (std::is_same_v<T, std::condition_variable>) {
            return "condition_variable";
        } else if constexpr (std::is_same_v<T, std::condition_variable_any>) {
            return "condition_variable_any";
        } else if constexpr (std::is_same_v<T, ConditionVariableSpin>) {
            return "ConditionVariableSpin";
        }
    }
};
TYPED_TEST_SUITE(ConditionVariableTest, CVTypes, CVNames);

TYPED_TEST(ConditionVariableTest, NotifyNobody) {
    using CVUnderTest = TypeParam;

    CVUnderTest cv;
    cv.notify_one();
    cv.notify_all();
}

TYPED_TEST(ConditionVariableTest, WaitOne) {
    using CVUnderTest = TypeParam;

    bool flag = false;
    std::mutex m;
    CVUnderTest cv;

    std::atomic<bool> waiting = false;
    ScopedThread thread([&] {
        std::unique_lock guard(m);
        EXPECT_FALSE(flag);
        waiting.store(true, std::memory_order_relaxed);
        while (!flag) {
            cv.wait(guard);
        }
    });

    while (!waiting.load(std::memory_order_relaxed)) {
        std::this_thread::yield();
    }
    {
        std::unique_lock guard(m);
        flag = true;
    }
    cv.notify_one();

    thread.join();
}

TYPED_TEST(ConditionVariableTest, WaitOneNotifyUnderLock) {
    using CVUnderTest = TypeParam;

    bool flag = false;
    std::mutex m;
    CVUnderTest cv;

    std::atomic<bool> waiting = false;
    ScopedThread thread([&] {
        std::unique_lock guard(m);
        EXPECT_FALSE(flag);
        waiting.store(true, std::memory_order_relaxed);
        while (!flag) {
            cv.wait(guard);
        }
    });

    while (!waiting.load(std::memory_order_relaxed)) {
        std::this_thread::yield();
    }
    {
        std::unique_lock guard(m);
        flag = true;
        cv.notify_one();
    }

    thread.join();
}

TYPED_TEST(ConditionVariableTest, WaitAll) {
    using CVUnderTest = TypeParam;

    bool flag = false;
    std::mutex m;
    CVUnderTest cv;

    std::atomic<size_t> waiting = 0;
    std_support::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&] {
            std::unique_lock guard(m);
            EXPECT_FALSE(flag);
            waiting.fetch_add(1, std::memory_order_relaxed);
            while (!flag) {
                cv.wait(guard);
            }
        });
    }

    while (waiting.load(std::memory_order_relaxed) != threads.size()) {
        std::this_thread::yield();
    }
    {
        std::unique_lock guard(m);
        flag = true;
    }
    cv.notify_all();

    threads.clear();
}

TYPED_TEST(ConditionVariableTest, WaitAllNotifyUnderLock) {
    using CVUnderTest = TypeParam;

    bool flag = false;
    std::mutex m;
    CVUnderTest cv;

    std::atomic<size_t> waiting = 0;
    std_support::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&] {
            std::unique_lock guard(m);
            EXPECT_FALSE(flag);
            waiting.fetch_add(1, std::memory_order_relaxed);
            while (!flag) {
                cv.wait(guard);
            }
        });
    }

    while (waiting.load(std::memory_order_relaxed) != threads.size()) {
        std::this_thread::yield();
    }
    {
        std::unique_lock guard(m);
        flag = true;
        cv.notify_all();
    }

    threads.clear();
}

TYPED_TEST(ConditionVariableTest, WaitPredicateOne) {
    using CVUnderTest = TypeParam;

    bool flag = false;
    std::mutex m;
    CVUnderTest cv;

    std::atomic<bool> waiting = false;
    ScopedThread thread([&] {
        std::unique_lock guard(m);
        EXPECT_FALSE(flag);
        waiting.store(true, std::memory_order_relaxed);
        cv.wait(guard, [&] { return flag; });
        EXPECT_TRUE(flag);
    });

    while (!waiting.load(std::memory_order_relaxed)) {
        std::this_thread::yield();
    }
    {
        std::unique_lock guard(m);
        flag = true;
    }
    cv.notify_all();

    thread.join();
}

TYPED_TEST(ConditionVariableTest, WaitPredicateOneNotifyUnderLock) {
    using CVUnderTest = TypeParam;

    bool flag = false;
    std::mutex m;
    CVUnderTest cv;

    std::atomic<bool> waiting = 0;
    ScopedThread thread([&] {
        std::unique_lock guard(m);
        EXPECT_FALSE(flag);
        waiting.store(true, std::memory_order_relaxed);
        cv.wait(guard, [&] { return flag; });
        EXPECT_TRUE(flag);
    });

    while (!waiting.load(std::memory_order_relaxed)) {
        std::this_thread::yield();
    }
    {
        std::unique_lock guard(m);
        flag = true;
        cv.notify_all();
    }

    thread.join();
}

TYPED_TEST(ConditionVariableTest, WaitPredicateAll) {
    using CVUnderTest = TypeParam;

    bool flag = false;
    std::mutex m;
    CVUnderTest cv;

    std::atomic<size_t> waiting = 0;
    std_support::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&] {
            std::unique_lock guard(m);
            EXPECT_FALSE(flag);
            waiting.fetch_add(1, std::memory_order_relaxed);
            cv.wait(guard, [&] { return flag; });
            EXPECT_TRUE(flag);
        });
    }

    while (waiting.load(std::memory_order_relaxed) != threads.size()) {
        std::this_thread::yield();
    }
    {
        std::unique_lock guard(m);
        flag = true;
    }
    cv.notify_all();

    threads.clear();
}

TYPED_TEST(ConditionVariableTest, WaitPredicateAllNotifyUnderLock) {
    using CVUnderTest = TypeParam;

    bool flag = false;
    std::mutex m;
    CVUnderTest cv;

    std::atomic<size_t> waiting = 0;
    std_support::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&] {
            std::unique_lock guard(m);
            EXPECT_FALSE(flag);
            waiting.fetch_add(1, std::memory_order_relaxed);
            cv.wait(guard, [&] { return flag; });
            EXPECT_TRUE(flag);
        });
    }

    while (waiting.load(std::memory_order_relaxed) != threads.size()) {
        std::this_thread::yield();
    }
    {
        std::unique_lock guard(m);
        flag = true;
        cv.notify_all();
    }

    threads.clear();
}

TYPED_TEST(ConditionVariableTest, Checkpoint) {
    constexpr uint64_t epochsCount = 1000;
    using CVUnderTest = TypeParam;

    uint64_t epochScheduled = 0;
    uint64_t epochStarted = 0;
    uint64_t epochFinished = 0;
    std::mutex m;
    CVUnderTest cv;

    auto schedule = [&] {
        std::unique_lock guard(m);
        if (epochScheduled > epochStarted) {
            return epochScheduled;
        }
        epochScheduled = epochStarted + 1;
        return epochScheduled;
    };

    std_support::vector<ScopedThread> threads;
    std::array<std::atomic<uint64_t>, kDefaultThreadCount> checkpoints = {0};
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&, i] {
            while (true) {
                uint64_t epoch = schedule();
                if (epoch >= epochsCount) return;
                {
                    std::unique_lock guard(m);
                    if (epochFinished < epoch) {
                        checkpoints[i].store(2 * epoch, std::memory_order_relaxed);
                        cv.wait(guard, [&] { return epochFinished >= epoch; });
                        checkpoints[i].store(2 * epoch + 1, std::memory_order_relaxed);
                    }
                }
            }
        });
    }

    while (epochStarted <= epochsCount) {
        {
            std::unique_lock guard(m);
            ++epochStarted;
        }
        std::this_thread::yield();
        {
            std::unique_lock guard(m);
            epochFinished = epochStarted;
        }
        cv.notify_all();
        for (auto& checkpoint : checkpoints) {
            while (true) {
                auto value = checkpoint.load(std::memory_order_relaxed);
                auto epoch = value / 2;
                bool isWaiting = value % 2 == 0;
                if (epoch > epochFinished) break;
                if (!isWaiting) break;
                std::this_thread::yield();
            }
        }
    }
    threads.clear();
}
