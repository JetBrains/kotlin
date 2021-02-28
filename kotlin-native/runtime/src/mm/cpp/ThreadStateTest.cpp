/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <thread>

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "MemoryPrivate.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

namespace {

class ThreadStateTest : public testing::Test {
public:
    ThreadStateTest() {
        globalKotlinFunctionMock = &kotlinFunctionMock_;
    }

    ~ThreadStateTest() {
        globalKotlinFunctionMock = nullptr;
    }

    testing::MockFunction<int32_t(int32_t)>& kotlinFunctionMock() { return kotlinFunctionMock_; }

    static int32_t kotlinFunction(int32_t arg) {
        return globalKotlinFunctionMock->Call(arg);
    }

    static RUNTIME_NORETURN void noReturnKotlinFunciton(int32_t arg) {
        globalKotlinFunctionMock->Call(arg);
        throw std::exception();
    }
private:
    testing::MockFunction<int32_t(int32_t)> kotlinFunctionMock_;
    static testing::MockFunction<int32_t(int32_t)>* globalKotlinFunctionMock;
};

//static
testing::MockFunction<int32_t(int32_t)>* ThreadStateTest::globalKotlinFunctionMock = nullptr;

} // namespace

TEST_F(ThreadStateTest, StateSwitchWithThreadData) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto initialState = threadData.state();
        EXPECT_EQ(ThreadState::kRunnable, initialState);

        ThreadState oldState = SwitchThreadState(&threadData, ThreadState::kNative);
        EXPECT_EQ(initialState, oldState);
        EXPECT_EQ(ThreadState::kNative, threadData.state());
    });
}

TEST_F(ThreadStateTest, StateSwitchWithMemoryState) {
    RunInNewThread([](MemoryState* memoryState) {
        auto threadData = memoryState->GetThreadData();
        auto initialState = threadData->state();
        EXPECT_EQ(ThreadState::kRunnable, initialState);

        ThreadState oldState = SwitchThreadState(memoryState, ThreadState::kNative);
        EXPECT_EQ(initialState, oldState);
        EXPECT_EQ(ThreadState::kNative, threadData->state());
    });
}

TEST_F(ThreadStateTest, StateSwitchExported) {
    RunInNewThread([](mm::ThreadData& threadData) {
        // Check functions exported for the compiler.
        EXPECT_EQ(ThreadState::kRunnable, threadData.state());

        Kotlin_mm_switchThreadStateNative();
        EXPECT_EQ(ThreadState::kNative, threadData.state());

        Kotlin_mm_switchThreadStateRunnable();
        EXPECT_EQ(ThreadState::kRunnable, threadData.state());
    });
}

TEST_F(ThreadStateTest, StateGuard) {
    RunInNewThread([](MemoryState* memoryState) {
        mm::ThreadData& threadData = *memoryState->GetThreadData();
        auto initialState = threadData.state();
        EXPECT_EQ(ThreadState::kRunnable, initialState);
        {
            ThreadStateGuard guard(memoryState, ThreadState::kNative);
            EXPECT_EQ(ThreadState::kNative, threadData.state());
        }
        EXPECT_EQ(initialState, threadData.state());
    });
}

TEST_F(ThreadStateTest, StateGuardForCurrentThread) {
    RunInNewThread([]() {
        auto* memoryState = mm::GetMemoryState();
        auto initialState = memoryState->GetThreadData()->state();
        EXPECT_EQ(ThreadState::kRunnable, initialState);
        {
            ThreadStateGuard guard(memoryState, ThreadState::kNative);
            EXPECT_EQ(ThreadState::kNative, memoryState->GetThreadData()->state());
        }
        EXPECT_EQ(initialState, memoryState->GetThreadData()->state());
    });
}

TEST_F(ThreadStateTest, CallKotlin) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        SwitchThreadState(&threadData, ThreadState::kNative);
        ASSERT_THAT(threadData.state(), ThreadState::kNative);

        EXPECT_CALL(kotlinFunctionMock(), Call(42))
            .WillOnce([&threadData](int32_t arg) {
                EXPECT_THAT(threadData.state(), ThreadState::kRunnable);
                return 24;
            });
        int32_t result = CallKotlin(kotlinFunction, 42);
        EXPECT_THAT(threadData.state(), ThreadState::kNative);
        EXPECT_THAT(result, 24);
    });
}

TEST_F(ThreadStateTest, CallKotlinNoReturn) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        SwitchThreadState(&threadData, ThreadState::kNative);
        ASSERT_THAT(threadData.state(), ThreadState::kNative);

        EXPECT_CALL(kotlinFunctionMock(), Call(42))
            .WillOnce([&threadData](int32_t arg){
                EXPECT_THAT(threadData.state(), ThreadState::kRunnable);
                return 24;
            });

        EXPECT_THROW(CallKotlinNoReturn(noReturnKotlinFunciton, 42), std::exception);
        EXPECT_THAT(threadData.state(), ThreadState::kNative);
    });
}

TEST(ThreadStateDeathTest, StateAsserts) {
    RunInNewThread([](MemoryState* memoryState) {
        mm::ThreadData* threadData = memoryState->GetThreadData();
        EXPECT_DEATH(AssertThreadState(memoryState, ThreadState::kNative),
                     "runtime assert: Unexpected thread state. Expected: NATIVE. Actual: RUNNABLE");
        EXPECT_DEATH(AssertThreadState(threadData, ThreadState::kNative),
                     "runtime assert: Unexpected thread state. Expected: NATIVE. Actual: RUNNABLE");
        EXPECT_DEATH(AssertThreadState(ThreadState::kNative),
                     "runtime assert: Unexpected thread state. Expected: NATIVE. Actual: RUNNABLE");
    });
}

TEST(ThreadStateDeathTest, IncorrectStateSwitch) {
    RunInNewThread([](MemoryState* memoryState) {
        auto* threadData = memoryState->GetThreadData();
        EXPECT_DEATH(SwitchThreadState(memoryState, ThreadState::kRunnable),
                     "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");
        EXPECT_DEATH(SwitchThreadState(threadData, ThreadState::kRunnable),
                     "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");

        EXPECT_DEATH(Kotlin_mm_switchThreadStateRunnable(),
                     "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");

        SwitchThreadState(threadData, kotlin::ThreadState::kNative);
        EXPECT_DEATH(Kotlin_mm_switchThreadStateNative(),
                     "runtime assert: Illegal thread state switch. Old state: NATIVE. New state: NATIVE");
    });
}
