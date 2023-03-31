/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Mutex.hpp"

#include <mutex>
#include <shared_mutex>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ScopedThread.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

template <typename T>
class MutexTest : public testing::Test {};

using LockTypes = testing::Types<SpinLock<MutexThreadStateHandling::kIgnore>, SpinLock<MutexThreadStateHandling::kSwitchIfRegistered>>;
TYPED_TEST_SUITE(MutexTest, LockTypes);

TYPED_TEST(MutexTest, SmokeDetachedThread) {
    using LockUnderTest = TypeParam;

    LockUnderTest lock;
    ScopedThread secondThread;
    std::atomic<bool> started = false;
    std::atomic<int32_t> protectedCounter = 0;

    {
        std::unique_lock guard1(lock);
        secondThread = ScopedThread([&lock, &started, &protectedCounter]() {
            started = true;
            std::unique_lock guard2(lock);
            protectedCounter++;
        });

        protectedCounter++;
        while (!started) { std::this_thread::yield(); }

        // Wait to give the second thread a chance to try to acquire the lock.
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        EXPECT_EQ(protectedCounter, 1);
    }
    secondThread.join();
    EXPECT_EQ(protectedCounter, 2);
}

TEST(RWSpinLockTest, Lock) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    mutex.lock();
    mutex.unlock();
    // Check that unlock unlocked.
    mutex.lock();
    mutex.unlock();
}

TEST(RWSpinLockTest, TryLock) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    EXPECT_TRUE(mutex.try_lock());
    EXPECT_FALSE(mutex.try_lock());
    mutex.unlock();
    mutex.lock();
    EXPECT_FALSE(mutex.try_lock());
    mutex.unlock();
}

TEST(RWSpinLockTest, LockShared) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    mutex.lock_shared();
    mutex.lock_shared();
    mutex.unlock_shared();
    mutex.lock_shared();
    mutex.unlock_shared();
    mutex.unlock_shared();
}

TEST(RWSpinLockTest, TryLockShared) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    EXPECT_TRUE(mutex.try_lock_shared());
    EXPECT_TRUE(mutex.try_lock_shared());
    mutex.unlock_shared();
    EXPECT_TRUE(mutex.try_lock_shared());
    mutex.unlock_shared();
    mutex.unlock_shared();
}

TEST(RWSpinLockTest, LockSharedWhileLocked) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    bool done = false;
    mutex.lock();
    ScopedThread thread([&] {
        mutex.lock_shared();
        EXPECT_TRUE(done);
        mutex.unlock_shared();
    });
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    done = true;
    mutex.unlock();
}

TEST(RWSpinLockTest, TryLockSharedWhileLocked) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    mutex.lock();
    EXPECT_FALSE(mutex.try_lock_shared());
    mutex.unlock();
}

TEST(RWSpinLockTest, LockWhileLockShared) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    bool done = false;
    mutex.lock_shared();
    ScopedThread thread([&] {
        mutex.lock();
        EXPECT_TRUE(done);
        mutex.unlock();
    });
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    // Shared lock is not where one does mutability. But it's okay here.
    done = true;
    mutex.unlock_shared();
}

TEST(RWSpinLockTest, TryLockWhileLockShared) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    mutex.lock_shared();
    EXPECT_FALSE(mutex.try_lock());
    mutex.unlock_shared();
}

TEST(RWSpinLockTest, LockSharedWhileLockSharedWithPendingLock) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    int state = 0;
    mutex.lock_shared();
    ScopedThread thread_lock([&] {
        mutex.lock();
        EXPECT_THAT(state, 1);
        state = 2;
        mutex.unlock();
    });
    // Wait for thread_lock to say that it wants to lock the mutex.
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    ScopedThread thread_lock_shared([&] {
        mutex.lock_shared();
        EXPECT_THAT(state, 2);
        mutex.unlock_shared();
    });
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    // Shared lock is not where one does mutability. But it's okay here.
    state = 1;
    mutex.unlock_shared();
}

TEST(RWSpinLockTest, TryLockSharedWhileLockSharedWithPendingLock) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    bool done = false;
    mutex.lock_shared();
    ScopedThread thread_lock([&] {
        mutex.lock();
        EXPECT_TRUE(done);
        mutex.unlock();
    });
    // Wait for thread_lock to say that it wants to lock the mutex.
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    EXPECT_FALSE(mutex.try_lock_shared());
    // Shared lock is not where one does mutability. But it's okay here.
    done = true;
    mutex.unlock_shared();
}

TEST(RWSpinLockTest, LockSharedWhileLockSharedWithFailedPendingLock) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    mutex.lock_shared();
    EXPECT_FALSE(mutex.try_lock());
    mutex.lock_shared();
    mutex.unlock_shared();
    mutex.unlock_shared();
}

TEST(RWSpinLockTest, TryLockSharedWhileLockSharedWithFailedPendingLock) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    mutex.lock_shared();
    EXPECT_FALSE(mutex.try_lock());
    EXPECT_TRUE(mutex.try_lock_shared());
    mutex.unlock_shared();
    mutex.unlock_shared();
}

TEST(RWSpinLockTest, Counter) {
    RWSpinLock<MutexThreadStateHandling::kIgnore> mutex;
    // tsan will help catch if the lock breaks memory ordering.
    uint64_t counter = 0;
    constexpr uint64_t boundary = 10000;
    std::atomic<bool> canStart = false;
    std::vector<ScopedThread> writers;
    for (int i = 0; i < 2; ++i) {
        writers.emplace_back([&] {
            while (!canStart.load(std::memory_order_acquire)) {
            }
            while (true) {
                std::unique_lock guard(mutex);
                if (counter == boundary) return;
                counter += 1;
            }
        });
    }
    std::vector<ScopedThread> readers;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        readers.emplace_back([&] {
            while (!canStart.load(std::memory_order_acquire)) {
            }
            while (true) {
                std::shared_lock guard(mutex);
                EXPECT_THAT(counter, testing::Ge(uint64_t(0)));
                EXPECT_THAT(counter, testing::Le(boundary));
                if (counter == boundary) return;
            }
        });
    }
    canStart.store(true, std::memory_order_release);
    readers.clear();
    writers.clear();
    EXPECT_THAT(counter, boundary);
}
