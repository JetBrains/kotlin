/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "Memory.h"
#include "Runtime.h"
#include "concurrent/ScopedThread.hpp"
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

        ThreadState oldState = SwitchThreadState(threadData, ThreadState::kNative);
        EXPECT_EQ(initialState, oldState);
        EXPECT_EQ(ThreadState::kNative, threadData.state());
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
    RunInNewThread([](mm::ThreadData& threadData) {
        auto initialState = threadData.state();
        EXPECT_EQ(ThreadState::kRunnable, initialState);
        {
            ThreadStateGuard guard(threadData, ThreadState::kNative);
            EXPECT_EQ(ThreadState::kNative, threadData.state());
        }
        EXPECT_EQ(initialState, threadData.state());
    });
}

TEST_F(ThreadStateTest, StateGuardForCurrentThread) {
    RunInNewThread([]() {
        auto& threadData = mm::currentThreadData();
        auto initialState = threadData.state();
        EXPECT_EQ(ThreadState::kRunnable, initialState);
        {
            ThreadStateGuard guard(threadData, ThreadState::kNative);
            EXPECT_EQ(ThreadState::kNative, threadData.state());
        }
        EXPECT_EQ(initialState, threadData.state());
    });
}

TEST_F(ThreadStateTest, CalledFromNativeGuard_DetachedThread) {
    ScopedThread([] {
        ASSERT_FALSE(mm::IsCurrentThreadRegistered());
        {
            CalledFromNativeGuard guard;
            EXPECT_TRUE(mm::IsCurrentThreadRegistered());
            EXPECT_EQ(mm::currentThreadData().state(), ThreadState::kRunnable);
        }
        EXPECT_TRUE(mm::IsCurrentThreadRegistered());
        EXPECT_EQ(mm::currentThreadData().state(), ThreadState::kNative);
    });
}

TEST_F(ThreadStateTest, CalledFromNativeGuard_AttachedThread) {
    ScopedThread([] {
        // CalledFromNativeGuard checks that runtime is fully initialized under the hood.
        Kotlin_initRuntimeIfNeeded();

        auto& threadData = mm::currentThreadData();
        ASSERT_EQ(threadData.state(), ThreadState::kNative);
        {
            CalledFromNativeGuard guard;
            EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
        }
        EXPECT_EQ(threadData.state(), ThreadState::kNative);
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
       SwitchThreadState(threadData, ThreadState::kNative);
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
    RunInNewThread([](mm::ThreadData& threadData) {
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
        {
            ThreadStateGuard outerGuard;
            EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
            {
                ThreadStateGuard innerGuard(threadData, ThreadState::kNative);
                EXPECT_EQ(threadData.state(), ThreadState::kNative);
                outerGuard = std::move(innerGuard);
            }
            EXPECT_EQ(threadData.state(), ThreadState::kNative);
        }
        EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
    });
}

TEST(ThreadStateDeathTest, StateAsserts) {
    RunInNewThread([](mm::ThreadData& threadData) {
        EXPECT_DEATH(AssertThreadState(threadData, ThreadState::kNative),
                     "runtime assert: Unexpected thread state. Expected: NATIVE. Actual: RUNNABLE");
        EXPECT_DEATH(AssertThreadState(ThreadState::kNative),
                     "runtime assert: Unexpected thread state. Expected: NATIVE. Actual: RUNNABLE");
    });
}

TEST(ThreadStateDeathTest, StateAssertsForDetachedThread) {
    EXPECT_DEATH(AssertThreadState(ThreadState::kNative),
                 "runtime assert: Thread is not attached to the runtime");
    EXPECT_DEATH(AssertThreadState({ThreadState::kNative}),
                 "runtime assert: Thread is not attached to the runtime");

}

TEST(ThreadStateDeathTest, IncorrectStateSwitchWithDifferentFunctions) {
    RunInNewThread([](mm::ThreadData& threadData) {
        EXPECT_DEATH(SwitchThreadState(threadData, ThreadState::kRunnable),
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
    EXPECT_DEATH(SwitchThreadState(threadData, ThreadState::kRunnable),
                 "runtime assert: Illegal thread state switch. Old state: RUNNABLE. New state: RUNNABLE");
    // Each EXPECT_NO_DEATH is executed in a fork process, so the global state of the test is not affected.
    EXPECT_NO_DEATH(SwitchThreadState(threadData, ThreadState::kNative));

    threadData.setState(ThreadState::kNative);
    ASSERT_EQ(threadData.state(), ThreadState::kNative);
    EXPECT_NO_DEATH(SwitchThreadState(threadData, ThreadState::kRunnable));
    EXPECT_DEATH(SwitchThreadState(threadData, ThreadState::kNative),
                 "runtime assert: Illegal thread state switch. Old state: NATIVE. New state: NATIVE");
}

TEST(ThreadStateDeathTest, StateSwitchForDetachedThread) {
    EXPECT_DEATH(Kotlin_mm_switchThreadStateNative(), "Thread is not attached to the runtime");
    EXPECT_DEATH(Kotlin_mm_switchThreadStateRunnable(), "Thread is not attached to the runtime" );
}

TEST(ThreadStateDeathTest, ReentrantStateSwitch_Function) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
        EXPECT_EXIT({ SwitchThreadState(threadData, ThreadState::kRunnable, true); exit(0); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));
    });
}

TEST(ThreadStateDeathTest, ReentrantStateSwitch_Guard) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
        ASSERT_EXIT({ ThreadStateGuard guard(ThreadState::kRunnable, true); exit(0); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));

        // Check that guards can be nested.
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
        {
            ThreadStateGuard guard(ThreadState::kNative, true);
            EXPECT_EQ(threadData.state(), ThreadState::kNative);
            {
                ThreadStateGuard nestedGuard(ThreadState::kNative, true);
                EXPECT_EQ(threadData.state(), ThreadState::kNative);
            }
            EXPECT_EQ(threadData.state(), ThreadState::kNative);
        }
        EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
    });
}

TEST(ThreadStateDeathTest, ReentrantStateSwitch_NativeOrUnregisteredThreadGuard) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
        SwitchThreadState(threadData, ThreadState::kNative);
        ASSERT_EQ(threadData.state(), ThreadState::kNative);
        ASSERT_EXIT({ NativeOrUnregisteredThreadGuard guard(true); exit(0); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));

        // Check that guards can be nested.
        SwitchThreadState(threadData, ThreadState::kRunnable);
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
        {
            NativeOrUnregisteredThreadGuard guard(true);
            EXPECT_EQ(threadData.state(), ThreadState::kNative);
            {
                NativeOrUnregisteredThreadGuard nestedGuard(true);
                EXPECT_EQ(threadData.state(), ThreadState::kNative);
            }
            EXPECT_EQ(threadData.state(), ThreadState::kNative);
        }
        EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
    });
}

TEST(ThreadStateDeathTest, ReentrantStateSwitch_CalledFromNativeGuard) {
    ScopedThread([]() {
        // CalledFromNativeGuard checks that runtime is fully initialized under the hood.
        Kotlin_initRuntimeIfNeeded();
        auto& threadData = mm::currentThreadData();
        ASSERT_EQ(threadData.state(), ThreadState::kNative);
        SwitchThreadState(threadData, ThreadState::kRunnable);
        ASSERT_EQ(threadData.state(), ThreadState::kRunnable);

        ASSERT_EXIT({ CalledFromNativeGuard guard(true); exit(0); },
                    testing::ExitedWithCode(0),
                    testing::Not(testing::ContainsRegex("runtime assert: Illegal thread state switch.")));

        // Check that guards can be nested.
        SwitchThreadState(threadData, ThreadState::kNative);
        ASSERT_EQ(threadData.state(), ThreadState::kNative);
        {
            CalledFromNativeGuard guard(true);
            EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
            {
                CalledFromNativeGuard nestedGuard(true);
                EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
            }
            EXPECT_EQ(threadData.state(), ThreadState::kRunnable);
        }
        EXPECT_EQ(threadData.state(), ThreadState::kNative);
    });
}


TEST(ThreadStateDeathTest, MovingReentrantGuard) {
    RunInNewThread([](mm::ThreadData& threadData) {

        auto blockUnderTest = [&threadData]() {
            ASSERT_EQ(threadData.state(), ThreadState::kRunnable);
            {
                ThreadStateGuard outerGuard;
                {
                    ThreadStateGuard innerGuard(threadData, ThreadState::kRunnable, /* reentrant = */ true);
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
    auto expectedError = "Thread is not attached to the runtime";
    EXPECT_DEATH({ ThreadStateGuard guard(ThreadState::kRunnable, false); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(ThreadState::kNative, false); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(ThreadState::kRunnable, true); }, expectedError);
    EXPECT_DEATH({ ThreadStateGuard guard(ThreadState::kNative, true); }, expectedError);
}
