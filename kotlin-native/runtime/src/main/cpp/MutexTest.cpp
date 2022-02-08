/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Mutex.hpp"

#include <mutex>

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


