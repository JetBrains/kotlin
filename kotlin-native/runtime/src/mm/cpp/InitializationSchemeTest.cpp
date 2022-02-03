/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "InitializationScheme.hpp"

#include <atomic>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ObjectTestSupport.hpp"
#include "ScopedThread.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "Types.h"

using namespace kotlin;

using testing::_;

namespace {

struct EmptyPayload {
    using Field = ObjHeader* EmptyPayload::*;
    static constexpr std::array<Field, 0> kFields{};
};

class InitSingletonTest : public testing::Test {
public:
    InitSingletonTest() {
        globalConstructor_ = &constructor_;
    }

    ~InitSingletonTest() {
        globalConstructor_ = nullptr;
        mm::GlobalData::Instance().gc().ClearForTests();
        mm::GlobalData::Instance().globalsRegistry().ClearForTests();
    }

    testing::MockFunction<void(ObjHeader*)>& constructor() { return constructor_; }

    OBJ_GETTER(InitThreadLocalSingleton, ObjHeader** location, mm::ThreadData& threadData) {
        RETURN_RESULT_OF(mm::InitThreadLocalSingleton, &threadData, location, type_.typeInfo(), constructorImpl);
    }

    OBJ_GETTER(InitSingleton, ObjHeader** location, mm::ThreadData& threadData) {
        RETURN_RESULT_OF(mm::InitSingleton, &threadData, location, type_.typeInfo(), constructorImpl);
    }

private:
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> constructor_;
    test_support::TypeInfoHolder type_{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};

    static testing::MockFunction<void(ObjHeader*)>* globalConstructor_;

    static void constructorImpl(ObjHeader* object) { globalConstructor_->Call(object); }
};

// static
testing::MockFunction<void(ObjHeader*)>* InitSingletonTest::globalConstructor_ = nullptr;

} // namespace

TEST_F(InitSingletonTest, InitThreadLocalSingleton) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        ObjHeader* location = nullptr;
        ObjHeader* stackLocation = nullptr;

        ObjHeader* valueAtConstructor = nullptr;
        EXPECT_CALL(constructor(), Call(_)).WillOnce(
                [&location, &stackLocation, &valueAtConstructor](ObjHeader* value) {
                    EXPECT_THAT(value, stackLocation);
                    EXPECT_THAT(value, location);
                    valueAtConstructor = value;
                });
        ObjHeader* value = InitThreadLocalSingleton(&location, threadData, &stackLocation);
        EXPECT_THAT(value, stackLocation);
        EXPECT_THAT(value, location);
        EXPECT_THAT(valueAtConstructor, location);
    });
}

TEST_F(InitSingletonTest, InitThreadLocalSingletonTwice) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        ObjHeader previousValue;
        ObjHeader* location = &previousValue;
        ObjHeader* stackLocation = nullptr;

        EXPECT_CALL(constructor(), Call(_)).Times(0);
        ObjHeader* value = InitThreadLocalSingleton(&location, threadData, &stackLocation);
        EXPECT_THAT(value, stackLocation);
        EXPECT_THAT(value, location);
        EXPECT_THAT(value, &previousValue);
    });
}

TEST_F(InitSingletonTest, InitThreadLocalSingletonFail) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        ObjHeader* location = nullptr;
        ObjHeader* stackLocation = nullptr;
        constexpr int kException = 42;

        EXPECT_CALL(constructor(), Call(_)).WillOnce([]() { throw kException; });
        try {
            InitThreadLocalSingleton(&location, threadData, &stackLocation);
            ASSERT_TRUE(false); // Cannot be reached.
        } catch (int exception) {
            EXPECT_THAT(exception, kException);
        }
        EXPECT_THAT(stackLocation, nullptr);
        EXPECT_THAT(location, nullptr);
    });
}

TEST_F(InitSingletonTest, InitSingleton) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        ObjHeader* location = nullptr;
        ObjHeader* stackLocation = nullptr;

        ObjHeader* valueAtConstructor = nullptr;
        EXPECT_CALL(constructor(), Call(_)).WillOnce(
                [&location, &stackLocation, &valueAtConstructor](ObjHeader* value) {
                    EXPECT_THAT(value, stackLocation);
                    EXPECT_THAT(location, kInitializingSingleton);
                    valueAtConstructor = value;
                });
        ObjHeader* value = InitSingleton(&location, threadData, &stackLocation);
        EXPECT_THAT(value, stackLocation);
        EXPECT_THAT(value, location);
        EXPECT_THAT(valueAtConstructor, location);
    });
}


TEST_F(InitSingletonTest, InitSingletonTwice) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        ObjHeader previousValue;
        ObjHeader* location = &previousValue;
        ObjHeader* stackLocation = nullptr;

        EXPECT_CALL(constructor(), Call(_)).Times(0);
        ObjHeader* value = InitSingleton(&location, threadData, &stackLocation);
        EXPECT_THAT(value, stackLocation);
        EXPECT_THAT(value, location);
        EXPECT_THAT(value, &previousValue);
    });
}

TEST_F(InitSingletonTest, InitSingletonFail) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        ObjHeader* location = nullptr;
        ObjHeader* stackLocation = nullptr;
        constexpr int kException = 42;

        EXPECT_CALL(constructor(), Call(_)).WillOnce([]() { throw kException; });
        try {
            InitSingleton(&location, threadData, &stackLocation);
            ASSERT_TRUE(false); // Cannot be reached.
        } catch (int exception) {
            EXPECT_THAT(exception, kException);
        }
        EXPECT_THAT(stackLocation, nullptr);
        EXPECT_THAT(location, nullptr);
    });
}

TEST_F(InitSingletonTest, InitSingletonRecursive) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        // The first singleton. Its constructor depends on the second singleton.
        ObjHeader* location1 = nullptr;
        ObjHeader* stackLocation1 = nullptr;
        // The second singleton. Its constructor depends on the first singleton.
        ObjHeader* location2 = nullptr;
        ObjHeader* stackLocation2 = nullptr;

        EXPECT_CALL(constructor(), Call(_))
                .Times(2) // called only once for each singleton.
                .WillRepeatedly([this, &location1, &stackLocation1, &location2, &stackLocation2, &threadData](ObjHeader* value) {
                    if (value == stackLocation1) {
                        ObjHeader* result = InitSingleton(&location2, threadData, &stackLocation2);
                        EXPECT_THAT(result, stackLocation2);
                        EXPECT_THAT(result, location2);
                        EXPECT_THAT(result, testing::Not(testing::Truly(isNullOrMarker)));
                    } else {
                        ObjHeader* result = InitSingleton(&location1, threadData, &stackLocation1);
                        EXPECT_THAT(result, stackLocation1);
                        EXPECT_THAT(result, testing::Ne(location1));
                        EXPECT_THAT(location1, kInitializingSingleton);
                    }
                });
        ObjHeader* value = InitSingleton(&location1, threadData, &stackLocation1);
        EXPECT_THAT(value, stackLocation1);
        EXPECT_THAT(value, location1);
    });
}

TEST_F(InitSingletonTest, InitSingletonConcurrent) {
    constexpr size_t kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<size_t> readyCount(0);
    KStdVector<ScopedThread> threads;
    ObjHeader* location = nullptr;
    KStdVector<ObjHeader*> stackLocations(kThreadCount, nullptr);
    KStdVector<ObjHeader*> actual(kThreadCount, nullptr);

    for (size_t i = 0; i < kThreadCount; ++i) {
        threads.emplace_back([this, i, &location, &stackLocations, &actual, &readyCount, &canStart]() {
            ScopedMemoryInit init;
            auto* threadData = init.memoryState()->GetThreadData();
            ++readyCount;
            while (!canStart) {
            }
            actual[i] = InitSingleton(&location, *threadData, &stackLocations[i]);
            threadData->Publish();
        });
    }

    while (readyCount < kThreadCount) {
    }
    // Constructor is called exactly once.
    EXPECT_CALL(constructor(), Call(_));
    canStart = true;
    threads.clear();
    testing::Mock::VerifyAndClearExpectations(&constructor());

    EXPECT_THAT(location, testing::Not(testing::Truly(isNullOrMarker)));
    EXPECT_THAT(stackLocations, testing::Each(location));
    EXPECT_THAT(actual, testing::Each(location));
}

TEST_F(InitSingletonTest, InitSingletonConcurrentFailing) {
    constexpr size_t kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<size_t> readyCount(0);
    KStdVector<ScopedThread> threads;
    constexpr int kException = 42;
    ObjHeader* location = nullptr;
    KStdVector<ObjHeader*> stackLocations(kThreadCount, nullptr);

    for (size_t i = 0; i < kThreadCount; ++i) {
        threads.emplace_back([this, i, &location, &stackLocations, &readyCount, &canStart]() {
            ScopedMemoryInit init;
            auto* threadData = init.memoryState()->GetThreadData();
            ++readyCount;
            while (!canStart) {
            }
            try {
                InitSingleton(&location, *threadData, &stackLocations[i]);
                ASSERT_TRUE(false); // Cannot be reached.
            } catch (int exception) {
                EXPECT_THAT(exception, kException);
            }
            threadData->Publish();
        });
    }

    while (readyCount < kThreadCount) {
    }
    // Constructor is called exactly `kThreadCount` times.
    EXPECT_CALL(constructor(), Call(_)).Times(kThreadCount).WillRepeatedly([]() { throw kException; });
    canStart = true;
    threads.clear();
    testing::Mock::VerifyAndClearExpectations(&constructor());

    EXPECT_THAT(location, nullptr);
    EXPECT_THAT(stackLocations, testing::Each(nullptr));
}
