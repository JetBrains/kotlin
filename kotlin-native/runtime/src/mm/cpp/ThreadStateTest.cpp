/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <thread>

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "Memory.h"
#include "MemoryPrivate.hpp"
#include "ScopedThread.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

namespace {

class ThreadStateTest : public testing::Test {
public:
    ThreadStateTest() {
        globalSomeFunctionMock = &someFunctionMock();
    }

    ~ThreadStateTest() {
        globalSomeFunctionMock = nullptr;
    }

    testing::MockFunction<int32_t(int32_t)>& someFunctionMock() { return someFunctionMock_; }

    static int32_t someFunction(int32_t arg) {
        return globalSomeFunctionMock->Call(arg);
    }

private:
    testing::MockFunction<int32_t(int32_t)> someFunctionMock_;
    static testing::MockFunction<int32_t(int32_t)>* globalSomeFunctionMock;
};

//static
testing::MockFunction<int32_t(int32_t)>* ThreadStateTest::globalSomeFunctionMock = nullptr;

#define EXPECT_NO_DEATH(statement) \
    do { EXPECT_EXIT({statement; exit(0);}, testing::ExitedWithCode(0), testing::_); } while(false)

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

TEST_F(ThreadStateTest, CalledFromNativeGuard_DetachedThread) {
    ScopedThread([] {
        ASSERT_FALSE(mm::IsCurrentThreadRegistered());
        {
            CalledFromNativeGuard guard;
            EXPECT_TRUE(mm::IsCurrentThreadRegistered());
            EXPECT_EQ(mm::GetMemoryState()->GetThreadData()->state(), ThreadState::kRunnable);
        }
        EXPECT_TRUE(mm::IsCurrentThreadRegistered());
        EXPECT_EQ(mm::GetMemoryState()->GetThreadData()->state(), ThreadState::kNative);
    });
}

TEST_F(ThreadStateTest, CalledFromNativeGuard_AttachedThread) {
    ScopedThread([] {
        // CalledFromNativeGuard checks that runtime is fully initialized under the hood.
        Kotlin_initRuntimeIfNeeded();

        auto threadData = mm::GetMemoryState()->GetThreadData();
        ASSERT_EQ(threadData->state(), ThreadState::kNative);
        {
            CalledFromNativeGuard guard;
            EXPECT_EQ(threadData->state(), ThreadState::kRunnable);
        }
        EXPECT_EQ(threadData->state(), ThreadState::kNative);
    });
}

TEST_F(ThreadStateTest, NativeOrUnregisteredThreadGuard_DetachedThread) {
    ScopedThread([] {
        {
            ASSERT_FALSE(mm::IsCurrentThreadRegistered());
            NativeOrUnregisteredThreadGuard guard;
            EXPECT_FALSE(mm::IsCurrentThreadRegistered());
        }
        EXPECT_FALSE(mm::IsCurrentThreadRegistered());
    });
}

TEST_F(ThreadStateTest, NativeOrUnregisteredThreadGuard_AttachedThread) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
        {
            NativeOrUnregisteredThreadGuard guard;
            EXPECT_EQ(threadData.state(), ThreadState::kNative);
        }
        EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
    });
}

TEST_F(ThreadStateTest, CallWithNativeState) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        ASSERT_THAT(threadData.state(), ThreadState::kRunnable);

        EXPECT_CALL(someFunctionMock(), Call(42))
            .WillOnce([&threadData](int32_t arg) {
                EXPECT_THAT(threadData.state(), ThreadState::kNative);
                return 24;
            });
        int32_t result = CallWithThreadState<ThreadState::kNative>(someFunction, 42);
        EXPECT_THAT(threadData.state(), ThreadState::kRunnable);
        EXPECT_THAT(result, 24);
    });
}

TEST_F(ThreadStateTest, CallWithRunnableState) {
    RunInNewThread([this](mm::ThreadData& threadData) {
       SwitchThreadState(&threadData, ThreadState::kNative);
       ASSERT_THAT(threadData.state(), ThreadState::kNative);

       EXPECT_CALL(someFunctionMock(), Call(42))
            .WillOnce([&threadData](int32_t arg) {
                EXPECT_THAT(threadData.state(), ThreadState::kRunnable);
                return 24;
            });
       int32_t result = CallWithThreadState<ThreadState::kRunnable>(someFunction, 42);
       EXPECT_THAT(threadData.state(), ThreadState::kNative);
       EXPECT_THAT(result, 24);
    });
}

TEST_F(ThreadStateTest, MovingGuard) {
    RunInNewThread([](MemoryState* memoryState) {
        auto& threadData = *memoryState->GetThreadData();
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
        {
            ThreadStateGuard outerGuard;
            EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
            {
                ThreadStateGuard innerGuard(memoryState, ThreadState::kNative);
                EXPECT_EQ(threadData.state(), ThreadState::kNative);
                outerGuard = std::move(innerGuard);
            }
            EXPECT_EQ(threadData.state(), ThreadState::kNative);
        }
        EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
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

TEST(ThreadStateDeathTest, StateAssertsForDetachedThread) {
    EXPECT_DEATH(AssertThreadState(static_cast<MemoryState*>(nullptr), ThreadState::kNative),
                 "runtime assert: thread must not be nullptr");
    EXPECT_DEATH(AssertThreadState(static_cast<mm::ThreadData*>(nullptr), ThreadState::kNative),
                 "runtime assert: threadData must not be nullptr");
    EXPECT_DEATH(AssertThreadState(ThreadState::kNative),
                 "runtime assert: Thread is not attached to the runtime");

    EXPECT_DEATH(AssertThreadState(static_cast<MemoryState*>(nullptr), {ThreadState::kNative}),
                 "runtime assert: thread must not be nullptr");
    EXPECT_DEATH(AssertThreadState(static_cast<mm::ThreadData*>(nullptr), {ThreadState::kNative}),
                 "runtime assert: threadData must not be nullptr");
    EXPECT_DEATH(AssertThreadState({ThreadState::kNative}),
                 "runtime assert: Thread is not attached to the runtime");

}

TEST(ThreadStateDeathTest, IncorrectStateSwitchWithDifferentFunctions) {
    RunInNewThread([](MemoryState* memoryState) {
        auto* threadData = memoryState->GetThreadData();
        EXPECT_DEATH(SwitchThreadState(memoryState, ThreadState::kRunnable),
                     "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");

        EXPECT_DEATH(Kotlin_mm_switchThreadStateRunnable(),
                     "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");

        SwitchThreadState(threadData, kotlin::ThreadState::kNative);
        EXPECT_DEATH(Kotlin_mm_switchThreadStateNative(),
                     "runtime assert: Illegal thread state switch. Old state: NATIVE. New state: NATIVE");
    });
}

TEST(ThreadStateDeathTest, StateSwitchCorrectness) {
    mm::ThreadData threadData(0);

    // Allowed state switches: runnable <-> native
    threadData.setState(ThreadState::kRunnable);
    ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
    EXPECT_DEATH(SwitchThreadState(&threadData, ThreadState::kRunnable),
                 "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");
    // Each EXPECT_NO_DEATH is executed in a fork process, so the global state of the test is not affected.
    EXPECT_NO_DEATH(SwitchThreadState(&threadData, ThreadState::kNative));

    threadData.setState(ThreadState::kNative);
    ASSERT_EQ(threadData.state(), ThreadState::kNative);
    EXPECT_NO_DEATH(SwitchThreadState(&threadData, ThreadState::kRunnable));
    EXPECT_DEATH(SwitchThreadState(&threadData, ThreadState::kNative),
                 "runtime assert: Illegal thread state switch. Old state: NATIVE. New state: NATIVE");
}

TEST(ThreadStateDeathTest, StateSwitchForDetachedThread) {
    EXPECT_DEATH(SwitchThreadState(static_cast<MemoryState*>(nullptr), ThreadState::kNative), "thread must not be nullptr");
    EXPECT_DEATH(SwitchThreadState(static_cast<mm::ThreadData*>(nullptr), ThreadState::kNative), "threadData must not be nullptr");

    EXPECT_DEATH(Kotlin_mm_switchThreadStateNative(), "Thread is not attached to the runtime");
    EXPECT_DEATH(Kotlin_mm_switchThreadStateRunnable(), "Thread is not attached to the runtime" );
}

TEST(ThreadStateDeathTest, ReentrantStateSwitch_Function) {
    RunInNewThread([](MemoryState* memoryState) {
        auto* threadData = memoryState->GetThreadData();
        ASSERT_EQ(threadData->state(), ThreadState::kRunnable);
        EXPECT_EXIT({ SwitchThreadState(memoryState, ThreadState::kRunnable, true); exit(0); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));
    });
}

TEST(ThreadStateDeathTest, ReentrantStateSwitch_Guard) {
    RunInNewThread([](MemoryState* memoryState) {
        auto* threadData = memoryState->GetThreadData();
        ASSERT_EQ(threadData->state(), ThreadState::kRunnable);
        ASSERT_EXIT({ ThreadStateGuard guard(ThreadState::kRunnable, true); exit(0); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));

        // Check that guards can be nested.
        ASSERT_EQ(threadData->state(), ThreadState::kRunnable);
        {
            ThreadStateGuard guard(ThreadState::kNative, true);
            EXPECT_EQ(threadData->state(), ThreadState::kNative);
            {
                ThreadStateGuard nestedGuard(ThreadState::kNative, true);
                EXPECT_EQ(threadData->state(), ThreadState::kNative);
            }
            EXPECT_EQ(threadData->state(), ThreadState::kNative);
        }
        EXPECT_EQ(threadData->state(), ThreadState::kRunnable);
    });
}

TEST(ThreadStateDeathTest, ReentrantStateSwitch_NativeOrUnregisteredThreadGuard) {
    RunInNewThread([](MemoryState* memoryState) {
        auto* threadData = memoryState->GetThreadData();
        ASSERT_EQ(threadData->state(), ThreadState::kRunnable);
        SwitchThreadState(threadData, ThreadState::kNative);
        ASSERT_EQ(threadData->state(), ThreadState::kNative);
        ASSERT_EXIT({ NativeOrUnregisteredThreadGuard guard(true); exit(0); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));

        // Check that guards can be nested.
        SwitchThreadState(threadData, ThreadState::kRunnable);
        ASSERT_EQ(threadData->state(), ThreadState::kRunnable);
        {
            NativeOrUnregisteredThreadGuard guard(true);
            EXPECT_EQ(threadData->state(), ThreadState::kNative);
            {
                NativeOrUnregisteredThreadGuard nestedGuard(true);
                EXPECT_EQ(threadData->state(), ThreadState::kNative);
            }
            EXPECT_EQ(threadData->state(), ThreadState::kNative);
        }
        EXPECT_EQ(threadData->state(), ThreadState::kRunnable);
    });
}

TEST(ThreadStateDeathTest, ReentrantStateSwitch_CalledFromNativeGuard) {
    ScopedThread([]() {
        // CalledFromNativeGuard checks that runtime is fully initialized under the hood.
        Kotlin_initRuntimeIfNeeded();
        auto* threadData = mm::GetMemoryState()->GetThreadData();
        ASSERT_EQ(threadData->state(), ThreadState::kNative);
        SwitchThreadState(threadData, ThreadState::kRunnable);
        ASSERT_EQ(threadData->state(), ThreadState::kRunnable);

        ASSERT_EXIT({ CalledFromNativeGuard guard(true); exit(0); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));

        // Check that guards can be nested.
        SwitchThreadState(threadData, ThreadState::kNative);
        ASSERT_EQ(threadData->state(), ThreadState::kNative);
        {
            CalledFromNativeGuard guard(true);
            EXPECT_EQ(threadData->state(), ThreadState::kRunnable);
            {
                CalledFromNativeGuard nestedGuard(true);
                EXPECT_EQ(threadData->state(), ThreadState::kRunnable);
            }
            EXPECT_EQ(threadData->state(), ThreadState::kRunnable);
        }
        EXPECT_EQ(threadData->state(), ThreadState::kNative);
    });
}


TEST(ThreadStateDeathTest, MovingReentrantGuard) {
    RunInNewThread([](MemoryState* memoryState) {

        auto blockUnderTest = [&memoryState]() {
            auto& threadData = *memoryState->GetThreadData();
            ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
            {
                ThreadStateGuard outerGuard;
                {
                    ThreadStateGuard innerGuard(memoryState, ThreadState::kRunnable, /* reentrant = */ true);
                    outerGuard = std::move(innerGuard);
                }
            }
            EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
            exit(0);
        };

        EXPECT_EXIT({ blockUnderTest(); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));
    });
}

TEST(ThreadStateDeathTest, GuardForDetachedThread) {
    auto expectedError = "thread must not be nullptr";
    EXPECT_DEATH({ ThreadStateGuard guard(nullptr, ThreadState::kRunnable, false); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(nullptr, ThreadState::kNative, false); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(nullptr, ThreadState::kRunnable, true); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(nullptr, ThreadState::kNative, true); }, expectedError);

    expectedError = "Thread is not attached to the runtime";
    EXPECT_DEATH({ ThreadStateGuard guard(ThreadState::kRunnable, false); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(ThreadState::kNative, false); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(ThreadState::kRunnable, true); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(ThreadState::kNative, true); }, expectedError);
}
