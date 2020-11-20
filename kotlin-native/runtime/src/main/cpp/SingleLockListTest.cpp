/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SingleLockList.hpp"

#include <atomic>
#include <deque>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

namespace {

using IntList = SingleLockList<int>;

} // namespace

TEST(SingleLockListTest, Emplace) {
    IntList list;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    auto* firstNode = list.Emplace(kFirst);
    auto* secondNode = list.Emplace(kSecond);
    auto* thirdNode = list.Emplace(kThird);
    int* first = firstNode->Get();
    int* second = secondNode->Get();
    int* third = thirdNode->Get();
    EXPECT_THAT(*first, kFirst);
    EXPECT_THAT(*second, kSecond);
    EXPECT_THAT(*third, kThird);
}

TEST(SingleLockListTest, EmplaceAndIter) {
    IntList list;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    list.Emplace(kFirst);
    list.Emplace(kSecond);
    list.Emplace(kThird);

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(kThird, kSecond, kFirst));
}

TEST(SingleLockListTest, EmplaceEraseAndIter) {
    IntList list;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    list.Emplace(kFirst);
    auto* secondNode = list.Emplace(kSecond);
    list.Emplace(kThird);
    list.Erase(secondNode);

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(kThird, kFirst));
}

TEST(SingleLockListTest, IterEmpty) {
    IntList list;

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(SingleLockListTest, EraseToEmptyEmplaceAndIter) {
    IntList list;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    constexpr int kFourth = 4;
    auto* firstNode = list.Emplace(kFirst);
    auto* secondNode = list.Emplace(kSecond);
    list.Erase(firstNode);
    list.Erase(secondNode);
    list.Emplace(kThird);
    list.Emplace(kFourth);

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(kFourth, kThird));
}

TEST(SingleLockListTest, ConcurrentEmplace) {
    IntList list;
    constexpr int kThreadCount = 100;
    std::atomic<bool> canStart(false);
    std::vector<std::thread> threads;
    std::vector<int> expected;
    for (int i = 0; i < kThreadCount; ++i) {
        expected.push_back(i);
        threads.emplace_back([i, &list, &canStart]() {
            while (!canStart) {
            }
            list.Emplace(i);
        });
    }

    canStart = true;
    for (auto& t : threads) {
        t.join();
    }

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}

TEST(SingleLockListTest, ConcurrentErase) {
    IntList list;
    constexpr int kThreadCount = 100;
    std::vector<IntList::Node*> items;
    for (int i = 0; i < kThreadCount; ++i) {
        items.push_back(list.Emplace(i));
    }

    std::atomic<bool> canStart(false);
    std::vector<std::thread> threads;
    for (auto* item : items) {
        threads.emplace_back([item, &list, &canStart]() {
            while (!canStart) {
            }
            list.Erase(item);
        });
    }

    canStart = true;
    for (auto& t : threads) {
        t.join();
    }

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(SingleLockListTest, IterWhileConcurrentEmplace) {
    IntList list;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = 100;

    std::deque<int> expectedBefore;
    std::vector<int> expectedAfter;
    for (int i = 0; i < kStartCount; ++i) {
        expectedBefore.push_front(i);
        expectedAfter.push_back(i);
        list.Emplace(i);
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> startedCount(0);
    std::vector<std::thread> threads;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = i + kStartCount;
        expectedAfter.push_back(j);
        threads.emplace_back([j, &list, &canStart, &startedCount]() {
            while (!canStart) {
            }
            ++startedCount;
            list.Emplace(j);
        });
    }

    std::vector<int> actualBefore;
    {
        auto iter = list.Iter();
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
    for (int element : list.Iter()) {
        actualAfter.push_back(element);
    }

    EXPECT_THAT(actualAfter, testing::UnorderedElementsAreArray(expectedAfter));
}

TEST(SingleLockListTest, IterWhileConcurrentErase) {
    IntList list;
    constexpr int kThreadCount = 100;

    std::deque<int> expectedBefore;
    std::vector<IntList::Node*> items;
    for (int i = 0; i < kThreadCount; ++i) {
        expectedBefore.push_front(i);
        items.push_back(list.Emplace(i));
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> startedCount(0);
    std::vector<std::thread> threads;
    for (auto* item : items) {
        threads.emplace_back([item, &list, &canStart, &startedCount]() {
            while (!canStart) {
            }
            ++startedCount;
            list.Erase(item);
        });
    }

    std::vector<int> actualBefore;
    {
        auto iter = list.Iter();
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
    for (int element : list.Iter()) {
        actualAfter.push_back(element);
    }

    EXPECT_THAT(actualAfter, testing::IsEmpty());
}

namespace {

class PinnedType : private Pinned {
public:
    PinnedType(int value) : value_(value) {}

    int value() const { return value_; }

private:
    int value_;
};

} // namespace

TEST(SingleLockListTest, PinnedType) {
    SingleLockList<PinnedType> list;
    constexpr int kFirst = 1;

    auto* itemNode = list.Emplace(kFirst);
    PinnedType* item = itemNode->Get();
    EXPECT_THAT(item->value(), kFirst);

    list.Erase(itemNode);

    std::vector<PinnedType*> actualAfter;
    for (auto& element : list.Iter()) {
        actualAfter.push_back(&element);
    }

    EXPECT_THAT(actualAfter, testing::IsEmpty());
}
