/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MultiSourceQueue.hpp"

#include <atomic>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "TestSupport.hpp"

using namespace kotlin;

using IntQueue = MultiSourceQueue<int>;

TEST(MultiSourceQueueTest, Empty) {
    IntQueue queue;

    std::vector<int> actual;
    for (int element : queue.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(MultiSourceQueueTest, DoNotPublish) {
    IntQueue queue;
    IntQueue::Producer producer(queue);

    producer.Insert(1);
    producer.Insert(2);

    std::vector<int> actual;
    for (int element : queue.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(MultiSourceQueueTest, Publish) {
    IntQueue queue;
    IntQueue::Producer producer1(queue);
    IntQueue::Producer producer2(queue);

    producer1.Insert(1);
    producer1.Insert(2);
    producer2.Insert(10);
    producer2.Insert(20);

    producer1.Publish();
    producer2.Publish();

    std::vector<int> actual;
    for (int element : queue.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(1, 2, 10, 20));
}

TEST(MultiSourceQueueTest, PublishSeveralTimes) {
    IntQueue queue;
    IntQueue::Producer producer(queue);

    // Add 2 elements and publish.
    producer.Insert(1);
    producer.Insert(2);
    producer.Publish();

    // Add another element and publish.
    producer.Insert(3);
    producer.Publish();

    // Publish without adding elements.
    producer.Publish();

    // Add yet another two elements and publish.
    producer.Insert(4);
    producer.Insert(5);
    producer.Publish();

    std::vector<int> actual;
    for (int element : queue.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(1, 2, 3, 4, 5));
}

TEST(MultiSourceQueueTest, PublishInDestructor) {
    IntQueue queue;

    {
        IntQueue::Producer producer(queue);
        producer.Insert(1);
        producer.Insert(2);
    }

    std::vector<int> actual;
    for (int element : queue.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(1, 2));
}

TEST(MultiSourceQueueTest, ConcurrentPublish) {
    IntQueue queue;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    std::vector<int> expected;

    for (int i = 0; i < kThreadCount; ++i) {
        expected.push_back(i);
        threads.emplace_back([i, &queue, &canStart, &readyCount]() {
            IntQueue::Producer producer(queue);
            producer.Insert(i);
            ++readyCount;
            while (!canStart) {
            }
            producer.Publish();
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    for (auto& t : threads) {
        t.join();
    }

    std::vector<int> actual;
    for (int element : queue.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}

TEST(MultiSourceQueueTest, IterWhileConcurrentPublish) {
    IntQueue queue;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::vector<int> expectedBefore;
    std::vector<int> expectedAfter;
    IntQueue::Producer producer(queue);
    for (int i = 0; i < kStartCount; ++i) {
        expectedBefore.push_back(i);
        expectedAfter.push_back(i);
        producer.Insert(i);
    }
    producer.Publish();

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::atomic<int> startedCount(0);
    std::vector<std::thread> threads;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = i + kStartCount;
        expectedAfter.push_back(j);
        threads.emplace_back([j, &queue, &canStart, &startedCount, &readyCount]() {
            IntQueue::Producer producer(queue);
            producer.Insert(j);
            ++readyCount;
            while (!canStart) {
            }
            ++startedCount;
            producer.Publish();
        });
    }

    std::vector<int> actualBefore;
    {
        auto iter = queue.Iter();
        while (readyCount < kThreadCount) {
        }
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (int element : iter) {
            actualBefore.push_back(element);
        }
    }

    for (auto& t : threads) {
        t.join();
    }

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    std::vector<int> actualAfter;
    for (int element : queue.Iter()) {
        actualAfter.push_back(element);
    }

    EXPECT_THAT(actualAfter, testing::UnorderedElementsAreArray(expectedAfter));
}
