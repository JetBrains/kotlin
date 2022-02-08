/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemoryPrivate.hpp"
#include "Runtime.h"
#include "RuntimePrivate.hpp"
#include "ScopedThread.hpp"
#include "ThreadSuspension.hpp"
#include "ThreadState.hpp"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <future>
#include <TestSupport.hpp>
#include <TestSupportCompilerGenerated.hpp>

#include <iostream>

using namespace kotlin;

namespace {

#ifdef KONAN_WINDOWS
constexpr size_t kDefaultIterations = 1000;
constexpr size_t kDefaultReportingStep = 100;
#else
constexpr size_t kDefaultIterations = 10000;
constexpr size_t kDefaultReportingStep = 1000;
#endif // #ifdef KONAN_WINDOWS

KStdVector<mm::ThreadData*> collectThreadData() {
    KStdVector<mm::ThreadData*> result;
    auto iter = mm::ThreadRegistry::Instance().LockForIter();
    for (auto& thread : iter) {
        result.push_back(&thread);
    }
    return result;
}

template<typename T, typename F>
KStdVector<T> collectFromThreadData(F extractFunction) {
    KStdVector<T> result;
    auto threadData = collectThreadData();
    std::transform(threadData.begin(), threadData.end(), std::back_inserter(result), extractFunction);
    return result;
}

KStdVector<bool> collectSuspended() {
    return collectFromThreadData<bool>(
            [](mm::ThreadData* threadData) { return threadData->suspensionData().suspended(); });
}

void reportProgress(size_t currentIteration, size_t totalIterations) {
    if (currentIteration % kDefaultReportingStep == 0) {
       std::cout << "Iteration: " << currentIteration << " of " << totalIterations << std::endl;
    }
}

testing::MockFunction<void()>* initializationMock = nullptr;

void initializationFunction() {
    ASSERT_NE(initializationMock, nullptr);
    initializationMock->Call();
}

test_support::ScopedMockFunction<void(), /* Strict = */ true> ScopedInitializationMock() {
    return test_support::ScopedMockFunction(&initializationMock);
}

} // namespace

class ThreadSuspensionTest : public ::testing::Test {
public:
    ~ThreadSuspensionTest() {
        canStart = true;
        shouldStop = true;
    }

    static constexpr size_t kThreadCount = kDefaultThreadCount;
    static constexpr size_t kIterations = kDefaultIterations;

    KStdVector<ScopedThread> threads;
    std::array<std::atomic<bool>, kThreadCount> ready{false};
    std::atomic<bool> canStart{false};
    std::atomic<bool> shouldStop{false};

    void waitUntilCanStart(size_t threadNumber) {
        ready[threadNumber] = true;
        while(!canStart) {
            std::this_thread::yield();
        }
        ready[threadNumber] = false;
    }

    void waitUntilThreadsAreReady() {
        canStart = false;
        while (!std::all_of(ready.begin(), ready.end(), [](bool it) { return it; })) {
            std::this_thread::yield();
        }
    }
};

TEST_F(ThreadSuspensionTest, SimpleStartStop) {
    ASSERT_THAT(collectThreadData(), testing::IsEmpty());
    for (size_t i = 0; i < kThreadCount; i++) {
        threads.emplace_back([this, i]() {
            ScopedMemoryInit init;
            auto& suspensionData = init.memoryState()->GetThreadData()->suspensionData();
            EXPECT_EQ(mm::IsThreadSuspensionRequested(), false);

            while(!shouldStop) {
                waitUntilCanStart(i);

                EXPECT_FALSE(suspensionData.suspended());
                suspensionData.suspendIfRequested();
                EXPECT_FALSE(suspensionData.suspended());
           }
        });
    }
    waitUntilThreadsAreReady();

    for (size_t i = 0; i < kIterations; i++) {
        reportProgress(i, kIterations);
        canStart = true;

        mm::SuspendThreads();
        auto suspended = collectSuspended();
        EXPECT_THAT(suspended, testing::Each(true));
        EXPECT_EQ(mm::IsThreadSuspensionRequested(), true);

        mm::ResumeThreads();

        // Wait for threads to run and sync for the next iteration
        waitUntilThreadsAreReady();

        suspended = collectSuspended();
        EXPECT_THAT(suspended, testing::Each(false));
        EXPECT_EQ(mm::IsThreadSuspensionRequested(), false);
    }
}


TEST_F(ThreadSuspensionTest, SwitchStateToNative) {
    ASSERT_THAT(collectThreadData(), testing::IsEmpty());

    for (size_t i = 0; i < kThreadCount; i++) {
        threads.emplace_back([this, i]() {
            ScopedMemoryInit init;
            auto* threadData = init.memoryState()->GetThreadData();
            EXPECT_EQ(mm::IsThreadSuspensionRequested(), false);

            while(!shouldStop) {
                waitUntilCanStart(i);

                EXPECT_EQ(threadData->state(), ThreadState::kRunnable);
                SwitchThreadState(threadData, ThreadState::kNative);
                EXPECT_EQ(threadData->state(), ThreadState::kNative);
                SwitchThreadState(threadData, ThreadState::kRunnable);
                EXPECT_EQ(threadData->state(), ThreadState::kRunnable);
            }
        });
    }
    waitUntilThreadsAreReady();

    for (size_t i = 0; i < kIterations; i++) {
        reportProgress(i, kIterations);
        canStart = true;

        mm::SuspendThreads();
        EXPECT_EQ(mm::IsThreadSuspensionRequested(), true);

        mm::ResumeThreads();
        EXPECT_EQ(mm::IsThreadSuspensionRequested(), false);

        // Sync for the next iteration.
        waitUntilThreadsAreReady();
    }
}

TEST_F(ThreadSuspensionTest, ConcurrentSuspend) {
    ASSERT_THAT(collectThreadData(), testing::IsEmpty());
    std::atomic<size_t> successCount = 0;

    for (size_t i = 0; i < kThreadCount; i++) {
        threads.emplace_back([this, i, &successCount]() {
            ScopedMemoryInit init;
            auto* currentThreadData = init.memoryState()->GetThreadData();
            EXPECT_EQ(mm::IsThreadSuspensionRequested(), false);

            // Sync with other threads.
            ready[i] = true;
            waitUntilThreadsAreReady();

            bool success = mm::SuspendThreads();
            if (success) {
                successCount++;
                auto allThreadData = collectThreadData();
                auto isCurrentOrSuspended = [currentThreadData](mm::ThreadData* data) {
                    return data == currentThreadData || data->suspensionData().suspended();
                };
                EXPECT_THAT(allThreadData, testing::Each(testing::Truly(isCurrentOrSuspended)));
                EXPECT_FALSE(currentThreadData->suspensionData().suspended());
                mm::ResumeThreads();
            } else {
                EXPECT_TRUE(mm::IsThreadSuspensionRequested());
                currentThreadData->suspensionData().suspendIfRequested();
            }
        });
    }
    for (auto& thread : threads) {
        thread.join();
    }
    EXPECT_EQ(successCount, 1u);
}

TEST_F(ThreadSuspensionTest, FileInitializationWithSuspend) {
    ASSERT_THAT(collectThreadData(), testing::IsEmpty());
    ASSERT_FALSE(mm::IsThreadSuspensionRequested());

    volatile int lock = internal::FILE_NOT_INITIALIZED;

    auto scopedInitializationMock = ScopedInitializationMock();
    EXPECT_CALL(*scopedInitializationMock, Call()).WillOnce([] {
        EXPECT_EQ(GetThreadState(), ThreadState::kRunnable);
        // Give other threads a chance to call CallInitGlobalPossiblyLock.
        std::this_thread::yield();
        mm::SuspendIfRequested();
    });

    for (size_t i = 0; i < kThreadCount; i++) {
        threads.emplace_back([this, i, &lock] {
            ScopedMemoryInit init;
            auto* threadData = init.memoryState()->GetThreadData();
            ASSERT_EQ(threadData->state(), ThreadState::kRunnable);

            waitUntilCanStart(i);

            CallInitGlobalPossiblyLock(&lock, initializationFunction);
            // Try to suspend to handle a case when this thread doesn't call the initialization function.
            mm::SuspendIfRequested();
        });
    }
    waitUntilThreadsAreReady();

    auto gcThread = std::async(std::launch::async, [] {
        mm::RequestThreadsSuspension();
        mm::WaitForThreadsSuspension();
        mm::ResumeThreads();
    });
    while(!mm::IsThreadSuspensionRequested()) {
    }
    canStart = true;

    auto futureStatus = gcThread.wait_for(std::chrono::seconds(10));
    EXPECT_NE(futureStatus, std::future_status::timeout);
    if (futureStatus == std::future_status::timeout) {
        // Possibly CallInitGlobalPossiblyLock is hanging in a dead-lock.
        // Set the lock variable to FILE_INITIALIZED to interrupt the loop inside CallInitGlobalPossiblyLock.
        // And wait for the gc thread to stop.
        lock = internal::FILE_INITIALIZED;
        gcThread.wait();
    }

    for (auto& t : threads) {
        t.join();
    }
}
