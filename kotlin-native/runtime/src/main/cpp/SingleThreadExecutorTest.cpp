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

TEST(ThreadWithContextTest, ContextThreadBound) {
    PinnedContext::ScopedMocks mocks;
    PinnedContext* createdContext = nullptr;
    std::thread::id createdThread;
    testing::StrictMock<testing::MockFunction<void()>> function;
    EXPECT_CALL(mocks.ctorMock, Call(_)).WillOnce([&](PinnedContext& context) {
        createdContext = &context;
        createdThread = std::this_thread::get_id();
    });
    EXPECT_CALL(function, Call()).WillOnce([&] { EXPECT_THAT(std::this_thread::get_id(), createdThread); });
    auto thread = ::make_unique<ThreadWithContext<PinnedContext>>([] { return PinnedContext(); }, function.AsStdFunction());
    thread->waitInitialized();
    testing::Mock::VerifyAndClearExpectations(&function);
    testing::Mock::VerifyAndClearExpectations(&mocks.ctorMock);
    EXPECT_THAT(createdThread, thread->get_id());
    EXPECT_THAT(thread->context(), testing::Ref(*createdContext));

    EXPECT_CALL(mocks.dtorMock, Call(testing::Ref(*createdContext))).WillOnce([&] {
        EXPECT_THAT(std::this_thread::get_id(), createdThread);
    });
    thread.reset();
    testing::Mock::VerifyAndClearExpectations(&mocks.dtorMock);
}

TEST(ThreadWithContextTest, WaitInitialized) {
    PinnedContext::ScopedMocks mocks;
    PinnedContext* createdContext = nullptr;
    std::mutex ctorMutex;
    EXPECT_CALL(mocks.ctorMock, Call(_)).WillOnce([&](PinnedContext& context) {
        std::unique_lock guard(ctorMutex);
        createdContext = &context;
    });

    testing::StrictMock<testing::MockFunction<void()>> function;
    EXPECT_CALL(function, Call()).Times(0);
    ctorMutex.lock();
    auto thread = ::make_unique<ThreadWithContext<PinnedContext>>([] { return PinnedContext(); }, function.AsStdFunction());

    std::atomic_bool initialized = false;
    std::thread initializedWaiter([&] {
        thread->waitInitialized();
        initialized = true;
    });

    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    EXPECT_THAT(initialized.load(), false);
    testing::Mock::VerifyAndClearExpectations(&function);

    EXPECT_CALL(function, Call());
    ctorMutex.unlock();
    initializedWaiter.join();
    testing::Mock::VerifyAndClearExpectations(&mocks.ctorMock);
    testing::Mock::VerifyAndClearExpectations(&function);
    EXPECT_THAT(initialized.load(), true);

    EXPECT_THAT(thread->context(), testing::Ref(*createdContext));
    EXPECT_CALL(mocks.dtorMock, Call(testing::Ref(*createdContext)));
}

TEST(SingleThreadExecutorTest, Execute) {
    SingleThreadExecutor<JoiningThread> executor;

    std::mutex taskMutex;
    testing::StrictMock<testing::MockFunction<void()>> task;

    EXPECT_CALL(task, Call()).WillOnce([&] { std::unique_lock guard(taskMutex); });
    taskMutex.lock();
    auto future = executor.Execute(task.AsStdFunction());

    auto futureStatus = future.wait_for(std::chrono::milliseconds(10));
    EXPECT_THAT(futureStatus, std::future_status::timeout);

    taskMutex.unlock();
    future.get();
    testing::Mock::VerifyAndClearExpectations(&task);
}

TEST(SingleThreadExecutorTest, DropExecutorWithTasks) {
    auto executor = make_unique<SingleThreadExecutor<JoiningThread>>();

    std::mutex taskMutex;
    testing::StrictMock<testing::MockFunction<void()>> task;

    std::atomic_bool taskStarted = false;
    EXPECT_CALL(task, Call()).WillOnce([&] {
        taskStarted = true;
        std::unique_lock guard(taskMutex);
    });
    taskMutex.lock();
    auto future = executor->Execute(task.AsStdFunction());
    while (!taskStarted) {}

    KStdVector<std::pair<std::future<void>, bool>> newTasks;
    constexpr size_t tasksCount = 100;
    for (size_t i = 0; i < tasksCount; ++i) {
        newTasks.push_back(std::make_pair(executor->Execute([&newTasks, i] { newTasks[i].second = true; }), false));
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
    auto executor = MakeSingleThreadExecutorWithContext<Context>();

    std::atomic_bool canStart = false;

    KStdVector<int> expected;
    KStdVector<std::thread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        expected.push_back(i);
        threads.emplace_back([&, i] {
            while (!canStart) {
            }
            executor.Execute([&] { executor.thread().context().result.push_back(i); }).get();
        });
    }

    canStart = true;

    for (auto& thread : threads) {
        thread.join();
    }

    EXPECT_THAT(executor.thread().context().result, testing::UnorderedElementsAreArray(expected));
}
