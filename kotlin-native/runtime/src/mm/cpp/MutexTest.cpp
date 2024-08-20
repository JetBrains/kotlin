/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "concurrent/Mutex.hpp"

#include "gtest/gtest.h"

#include "concurrent/ScopedThread.hpp"
#include "TestSupport.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

namespace {

template <bool kThreadStateAware_, ThreadState kInitialState_>
struct Mode {
};

template <ThreadState kInitialState_>
struct Mode<false, kInitialState_> {
    using Lock = SpinLock;
    static constexpr bool kThreadStateAware = false;
    static constexpr ThreadState initialState = kInitialState_;
};

template <ThreadState kInitialState_>
struct Mode<true, kInitialState_> {
    using Lock = ThreadStateAware<SpinLock>;
    static constexpr bool kThreadStateAware = true;
    static constexpr ThreadState initialState = kInitialState_;
};

} // namespace

template <typename T>
class MutexTestNewMM : public testing::Test {};

using TestModes = testing::Types<
        Mode<false, ThreadState::kRunnable>,
        Mode<false, ThreadState::kNative>,
        Mode<true, ThreadState::kRunnable>,
        Mode<true, ThreadState::kNative>>;

TYPED_TEST_SUITE(MutexTestNewMM, TestModes);

TYPED_TEST(MutexTestNewMM, SmokeAttachedThread) {
    RunInNewThread([](){
        using LockUnderTest = typename TypeParam::Lock;
        ThreadState initialThreadState = TypeParam::initialState;

        LockUnderTest lock;
        ScopedThread secondThread;
        std::atomic<bool> started = false;
        std::atomic<int32_t> protectedCounter = 0;
        mm::ThreadData* secondThreadData = nullptr;

        {
            std::unique_lock guard1(lock);
            secondThread = ScopedThread([&lock, &started, &protectedCounter, &secondThreadData, &initialThreadState]() {
                ScopedMemoryInit init;
                SwitchThreadState(init.memoryState(), initialThreadState, /* reentrant = */ true);

                secondThreadData = init.memoryState()->GetThreadData();
                ASSERT_EQ(secondThreadData->state(), initialThreadState);

                started = true;
                std::unique_lock guard2(lock);
                EXPECT_EQ(secondThreadData->state(), initialThreadState);
                protectedCounter++;
            });

            protectedCounter++;
            while (!started) {
                std::this_thread::yield();
            }

            // Wait to give the second thread a chance to try to acquire the lock.
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            EXPECT_EQ(protectedCounter, 1);

            auto expectedSecondThreadState = (TypeParam::kThreadStateAware) ? ThreadState::kNative : initialThreadState;
            EXPECT_EQ(secondThreadData->state(), expectedSecondThreadState);
        }
        secondThread.join();
        EXPECT_EQ(protectedCounter, 2);
    });
}
