/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SingleLockList.hpp"

#include <atomic>
#include <deque>
#include <functional>
#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "StdAllocatorTestSupport.hpp"
#include "ScopedThread.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

using IntList = SingleLockList<int, SpinLock<MutexThreadStateHandling::kIgnore>>;

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
    for (int element : list.LockForIter()) {
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
    for (int element : list.LockForIter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(kThird, kFirst));
}

TEST(SingleLockListTest, IterEmpty) {
    IntList list;

    std::vector<int> actual;
    for (int element : list.LockForIter()) {
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
    for (int element : list.LockForIter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(kFourth, kThird));
}

TEST(SingleLockListTest, ConcurrentEmplace) {
    IntList list;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<ScopedThread> threads;
    std::vector<int> expected;
    for (int i = 0; i < kThreadCount; ++i) {
        expected.push_back(i);
        threads.emplace_back([i, &list, &canStart, &readyCount]() {
            ++readyCount;
            while (!canStart) {
            }
            list.Emplace(i);
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    threads.clear();

    std::vector<int> actual;
    for (int element : list.LockForIter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}

TEST(SingleLockListTest, ConcurrentErase) {
    IntList list;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::vector<IntList::Node*> items;
    for (int i = 0; i < kThreadCount; ++i) {
        items.push_back(list.Emplace(i));
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<ScopedThread> threads;
    for (auto* item : items) {
        threads.emplace_back([item, &list, &canStart, &readyCount]() {
            ++readyCount;
            while (!canStart) {
            }
            list.Erase(item);
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    threads.clear();

    std::vector<int> actual;
    for (int element : list.LockForIter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(SingleLockListTest, IterWhileConcurrentEmplace) {
    IntList list;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::deque<int> expectedBefore;
    std::vector<int> expectedAfter;
    for (int i = 0; i < kStartCount; ++i) {
        expectedBefore.push_front(i);
        expectedAfter.push_back(i);
        list.Emplace(i);
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> startedCount(0);
    std::vector<ScopedThread> threads;
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
        auto iter = list.LockForIter();
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (int element : iter) {
            actualBefore.push_back(element);
        }
    }

    threads.clear();

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    std::vector<int> actualAfter;
    for (int element : list.LockForIter()) {
        actualAfter.push_back(element);
    }

    EXPECT_THAT(actualAfter, testing::UnorderedElementsAreArray(expectedAfter));
}

TEST(SingleLockListTest, IterWhileConcurrentErase) {
    IntList list;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::deque<int> expectedBefore;
    std::vector<IntList::Node*> items;
    for (int i = 0; i < kThreadCount; ++i) {
        expectedBefore.push_front(i);
        items.push_back(list.Emplace(i));
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> startedCount(0);
    std::vector<ScopedThread> threads;
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
        auto iter = list.LockForIter();
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (int element : iter) {
            actualBefore.push_back(element);
        }
    }

    threads.clear();

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    std::vector<int> actualAfter;
    for (int element : list.LockForIter()) {
        actualAfter.push_back(element);
    }

    EXPECT_THAT(actualAfter, testing::IsEmpty());
}

TEST(SingleLockListTest, LockAndEmplace) {
    SingleLockList<int, std::recursive_mutex> list;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::vector<ScopedThread> threads;
    std::vector<int> actualLocked;
    std::vector<int> actualUnlocked;
    std::vector<int> expectedUnlocked;
    for (int i = 0; i < kThreadCount; i++) {
        expectedUnlocked.push_back(i);
    }
    std::atomic<int> startedCount(0);
    {
        std::unique_lock lock = list.Lock();
        for (int i = 0; i < kThreadCount; i++) {
            threads.emplace_back([&startedCount, &list, i]() {
                startedCount++;
                list.Emplace(i);
            });
        }
        while (startedCount != kThreadCount) {
            std::this_thread::yield();
        }
        // Here still may be a race leading to false-successful EXPECT
        // if the scheduler suspend all threads right before list.Emplace.
        // But this situation looks unlikely
        for (int element : list.LockForIter()) {
            actualLocked.push_back(element);
        }
    }

    threads.clear();
    for (int element : list.LockForIter()) {
        actualUnlocked.push_back(element);
    }
    EXPECT_THAT(actualLocked, testing::IsEmpty());
    EXPECT_THAT(actualUnlocked, testing::UnorderedElementsAreArray(expectedUnlocked));
}

TEST(SingleLockListTest, LockAndErase) {
    SingleLockList<int, std::recursive_mutex> list;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::vector<SingleLockList<int, std::recursive_mutex>::Node*> items;
    std::vector<int> expectedLocked;
    std::vector<ScopedThread> threads;
    std::vector<int> actualLocked;
    std::vector<int> actualUnlocked;
    std::atomic<int> startedCount(0);

    for (int i = 0; i < kThreadCount; i++) {
        expectedLocked.push_back(i);
        items.push_back(list.Emplace(i));
    }
    {
        std::unique_lock lock = list.Lock();
        for (int i = 0; i < kThreadCount; i++) {
            threads.emplace_back([&startedCount, &list, &items, i]() {
                startedCount++;
                list.Erase(items[i]);
            });
        }
        while (startedCount != kThreadCount) {
            std::this_thread::yield();
        }
        // Here still may be a race leading to false-successful EXPECT
        // if the scheduler suspend all threads right before list.Erase.
        // But this situation looks unlikely
        for (int element : list.LockForIter()) {
            actualLocked.push_back(element);
        }
    }

    threads.clear();
    for (int element : list.LockForIter()) {
        actualUnlocked.push_back(element);
    }

    EXPECT_THAT(actualLocked, testing::UnorderedElementsAreArray(expectedLocked));
    EXPECT_THAT(actualUnlocked, testing::IsEmpty());
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
    SingleLockList<PinnedType, SpinLock<MutexThreadStateHandling::kIgnore>> list;
    constexpr int kFirst = 1;

    auto* itemNode = list.Emplace(kFirst);
    PinnedType* item = itemNode->Get();
    EXPECT_THAT(item->value(), kFirst);

    list.Erase(itemNode);

    std::vector<PinnedType*> actualAfter;
    for (auto& element : list.LockForIter()) {
        actualAfter.push_back(&element);
    }

    EXPECT_THAT(actualAfter, testing::IsEmpty());
}

namespace {

class WithDestructorHook;

using DestructorHook = void(WithDestructorHook*);

class WithDestructorHook : private Pinned {
public:
    explicit WithDestructorHook(std::function<DestructorHook> hook) : hook_(std::move(hook)) {}

    ~WithDestructorHook() { hook_(this); }

private:
    std::function<DestructorHook> hook_;
};

} // namespace

TEST(SingleLockListTest, Destructor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> hook;
    {
        SingleLockList<WithDestructorHook, SpinLock<MutexThreadStateHandling::kIgnore>> list;
        auto* first = list.Emplace(hook.AsStdFunction())->Get();
        auto* second = list.Emplace(hook.AsStdFunction())->Get();
        auto* third = list.Emplace(hook.AsStdFunction())->Get();
        {
            testing::InSequence seq;
            // `list` is `third`->`second`->`first`. If destruction
            // were to cause recursion, the order of destructors
            // would've been backwards.
            EXPECT_CALL(hook, Call(third));
            EXPECT_CALL(hook, Call(second));
            EXPECT_CALL(hook, Call(first));
        }
    }
}

TEST(SingleLockListTest, CustomAllocator) {
    testing::StrictMock<test_support::SpyAllocatorCore> allocatorCore;
    auto allocator = test_support::MakeAllocator<int>(allocatorCore);
    SingleLockList<int, SpinLock<MutexThreadStateHandling::kIgnore>, decltype(allocator)> list(allocator);

    EXPECT_CALL(allocatorCore, allocate(_)).Times(3);
    auto* node1 = list.Emplace(1);
    auto* node2 = list.Emplace(2);
    auto* node3 = list.Emplace(3);
    testing::Mock::VerifyAndClearExpectations(&allocatorCore);

    {
        testing::InSequence seq;
        EXPECT_CALL(allocatorCore, deallocate(node1, _));
        EXPECT_CALL(allocatorCore, deallocate(node2, _));
        EXPECT_CALL(allocatorCore, deallocate(node3, _));
    }
    list.Erase(node1);
    list.Erase(node2);
    list.Erase(node3);
    testing::Mock::VerifyAndClearExpectations(&allocatorCore);
}
