/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SingleThreadExecutor.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "KAssert.h"
#include "TestSupport.hpp"

using namespace kotlin;

using testing::_;

namespace {

class PinnedContext : private Pinned {
public:
    struct ScopedMocks : private Pinned {
        testing::StrictMock<testing::MockFunction<void(PinnedContext&)>> ctorMock;
        testing::StrictMock<testing::MockFunction<void(PinnedContext&)>> dtorMock;

        ScopedMocks() {
            RuntimeAssert(PinnedContext::ctorMock == nullptr, "ctor mock must be null was %p", PinnedContext::ctorMock);
            PinnedContext::ctorMock = &ctorMock;
            RuntimeAssert(PinnedContext::dtorMock == nullptr, "dtor mock must be null was %p", PinnedContext::dtorMock);
            PinnedContext::dtorMock = &dtorMock;
        }

        ~ScopedMocks() {
            RuntimeAssert(PinnedContext::ctorMock == &ctorMock, "ctor mock must be %p was %p", &ctorMock, PinnedContext::ctorMock);
            PinnedContext::ctorMock = nullptr;
            RuntimeAssert(PinnedContext::dtorMock == &dtorMock, "dtor mock must be %p was %p", &dtorMock, PinnedContext::dtorMock);
            PinnedContext::dtorMock = nullptr;
        }
    };

    PinnedContext() { ctorMock->Call(*this); }

    ~PinnedContext() { dtorMock->Call(*this); }

private:
    static testing::MockFunction<void(PinnedContext&)>* ctorMock;
    static testing::MockFunction<void(PinnedContext&)>* dtorMock;
};

testing::MockFunction<void(PinnedContext&)>* PinnedContext::ctorMock = nullptr;
testing::MockFunction<void(PinnedContext&)>* PinnedContext::dtorMock = nullptr;

} // namespace

TEST(SingleThreadExecutorTest, ContextThreadBound) {
    PinnedContext::ScopedMocks mocks;
    PinnedContext* createdContext = nullptr;
    ScopedThread::id createdThread;
    EXPECT_CALL(mocks.ctorMock, Call(_)).WillOnce([&](PinnedContext& context) {
        createdContext = &context;
        createdThread = std::this_thread::get_id();
    });
    auto executor = ::make_unique<SingleThreadExecutor<PinnedContext>>();
    // Make sure context is created.
    executor->context();
    testing::Mock::VerifyAndClearExpectations(&mocks.ctorMock);
    EXPECT_THAT(createdThread, executor->threadId());
    EXPECT_THAT(executor->context(), testing::Ref(*createdContext));

    testing::StrictMock<testing::MockFunction<void()>> task;
    EXPECT_CALL(task, Call()).WillOnce([&] { EXPECT_THAT(std::this_thread::get_id(), createdThread); });
    executor->execute(task.AsStdFunction()).get();
    testing::Mock::VerifyAndClearExpectations(&task);

    EXPECT_CALL(mocks.dtorMock, Call(testing::Ref(*createdContext))).WillOnce([&] {
        EXPECT_THAT(std::this_thread::get_id(), createdThread);
    });
    executor.reset();
    testing::Mock::VerifyAndClearExpectations(&mocks.dtorMock);
}

TEST(SingleThreadExecutorTest, WaitContext) {
    PinnedContext::ScopedMocks mocks;
    PinnedContext* createdContext = nullptr;
    std::mutex ctorMutex;
    EXPECT_CALL(mocks.ctorMock, Call(_)).WillOnce([&](PinnedContext& context) {
        std::unique_lock guard(ctorMutex);
        createdContext = &context;
    });

    ctorMutex.lock();
    SingleThreadExecutor<PinnedContext> executor;

    auto future = executor.execute([] {});

    auto futureStatus = future.wait_for(std::chrono::milliseconds(10));
    EXPECT_THAT(futureStatus, std::future_status::timeout);

    ctorMutex.unlock();
    // Wait for `thread` to initialize.
    executor.context();
    future.get();
    testing::Mock::VerifyAndClearExpectations(&mocks.ctorMock);

    EXPECT_THAT(executor.context(), testing::Ref(*createdContext));
    EXPECT_CALL(mocks.dtorMock, Call(testing::Ref(*createdContext)));
}

TEST(SingleThreadExecutorTest, execute) {
    struct Context {};
    SingleThreadExecutor<Context> executor;

    std::mutex taskMutex;
    testing::StrictMock<testing::MockFunction<void()>> task;

    EXPECT_CALL(task, Call()).WillOnce([&] { std::unique_lock guard(taskMutex); });
    taskMutex.lock();
    auto future = executor.execute(task.AsStdFunction());

    auto futureStatus = future.wait_for(std::chrono::milliseconds(10));
    EXPECT_THAT(futureStatus, std::future_status::timeout);

    taskMutex.unlock();
    future.get();
    testing::Mock::VerifyAndClearExpectations(&task);
}

TEST(SingleThreadExecutorTest, DropExecutorWithTasks) {
    struct Context {};
    auto executor = make_unique<SingleThreadExecutor<Context>>();

    std::mutex taskMutex;
    testing::StrictMock<testing::MockFunction<void()>> task;

    std::atomic_bool taskStarted = false;
    EXPECT_CALL(task, Call()).WillOnce([&] {
        taskStarted = true;
        std::unique_lock guard(taskMutex);
    });
    taskMutex.lock();
    auto future = executor->execute(task.AsStdFunction());
    while (!taskStarted) {}

    KStdVector<std::pair<std::future<void>, bool>> newTasks;
    constexpr size_t tasksCount = 100;
    for (size_t i = 0; i < tasksCount; ++i) {
        newTasks.push_back(std::make_pair(executor->execute([&newTasks, i] { newTasks[i].second = true; }), false));
    }

    taskMutex.unlock();
    executor.reset();

    testing::Mock::VerifyAndClearExpectations(&task);
    future.get();

    // There's no guarantee whether any of those succeed, or any fail.
    for (auto& [future, success] : newTasks) {
        if (success) {
            future.get();
        } else {
            EXPECT_THROW(future.get(), std::future_error);
        }
    }
}

TEST(SingleThreadExecutorTest, ExecuteFromManyThreads) {
    struct Context {
        KStdVector<int> result;
    };
    SingleThreadExecutor<Context> executor;

    std::atomic_bool canStart = false;

    KStdVector<int> expected;
    KStdVector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        expected.push_back(i);
        threads.emplace_back([&, i] {
            while (!canStart) {
            }
            executor.execute([&] { executor.context().result.push_back(i); }).get();
        });
    }

    canStart = true;

    threads.clear();

    EXPECT_THAT(executor.context().result, testing::UnorderedElementsAreArray(expected));
}
