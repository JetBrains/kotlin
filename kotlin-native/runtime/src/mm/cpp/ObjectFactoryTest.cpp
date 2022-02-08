/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectFactory.hpp"

#include <atomic>
#include <type_traits>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Allocator.hpp"
#include "FinalizerHooksTestSupport.hpp"
#include "ObjectOps.hpp"
#include "ObjectTestSupport.hpp"
#include "ScopedThread.hpp"
#include "TestSupport.hpp"
#include "Types.h"

using namespace kotlin;

using testing::_;

namespace {

using SimpleAllocator = gc::AlignedAllocator;

template <size_t DataAlignment>
using ObjectFactoryStorage = mm::internal::ObjectFactoryStorage<DataAlignment, SimpleAllocator>;

using ObjectFactoryStorageRegular = ObjectFactoryStorage<alignof(void*)>;

template <typename Storage>
using Producer = typename Storage::Producer;

template <typename Storage>
using Consumer = typename Storage::Consumer;

template <size_t DataAlignment>
KStdVector<void*> Collect(ObjectFactoryStorage<DataAlignment>& storage) {
    KStdVector<void*> result;
    for (auto& node : storage.LockForIter()) {
        result.push_back(node.Data());
    }
    return result;
}

template <typename T, size_t DataAlignment>
KStdVector<T> Collect(ObjectFactoryStorage<DataAlignment>& storage) {
    KStdVector<T> result;
    for (auto& node : storage.LockForIter()) {
        result.push_back(*static_cast<T*>(node.Data()));
    }
    return result;
}

template <typename T, size_t DataAlignment>
KStdVector<T> Collect(Consumer<ObjectFactoryStorage<DataAlignment>>& consumer) {
    KStdVector<T> result;
    for (auto& node : consumer) {
        result.push_back(*static_cast<T*>(node.Data()));
    }
    return result;
}

struct MoveOnlyImpl : private MoveOnly {
    MoveOnlyImpl(int value1, int value2) : value1(value1), value2(value2) {}

    int value1;
    int value2;
};

struct PinnedImpl : private Pinned {
    PinnedImpl(int value1, int value2, int value3) : value1(value1), value2(value2), value3(value3) {}

    int value1;
    int value2;
    int value3;
};

struct MaxAlignedData {
    explicit MaxAlignedData(int value) : value(value) {}

    std::max_align_t padding;
    int value;
};

} // namespace

TEST(ObjectFactoryStorageTest, Empty) {
    ObjectFactoryStorageRegular storage;

    auto actual = Collect(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(storage.GetSizeUnsafe(), 0);
}

TEST(ObjectFactoryStorageTest, DoNotPublish) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);

    auto actual = Collect(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(storage.GetSizeUnsafe(), 0);
    EXPECT_THAT(producer.size(), 2);
}

TEST(ObjectFactoryStorageTest, Publish) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer1(storage, SimpleAllocator());
    Producer<ObjectFactoryStorageRegular> producer2(storage, SimpleAllocator());

    producer1.Insert<int>(1);
    producer1.Insert<int>(2);
    producer2.Insert<int>(10);
    producer2.Insert<int>(20);

    producer1.Publish();
    producer2.Publish();

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2, 10, 20));
    EXPECT_THAT(storage.GetSizeUnsafe(), 4);
    EXPECT_THAT(producer1.size(), 0);
    EXPECT_THAT(producer2.size(), 0);
}

TEST(ObjectFactoryStorageTest, PublishDifferentTypes) {
    ObjectFactoryStorage<alignof(MaxAlignedData)> storage;
    Producer<ObjectFactoryStorage<alignof(MaxAlignedData)>> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<size_t>(2);
    producer.Insert<MoveOnlyImpl>(3, 4);
    producer.Insert<PinnedImpl>(5, 6, 7);
    producer.Insert<MaxAlignedData>(8);

    producer.Publish();

    auto actual = storage.LockForIter();
    auto it = actual.begin();
    EXPECT_THAT(it->Data<int>(), 1);
    ++it;
    EXPECT_THAT(it->Data<size_t>(), 2);
    ++it;
    auto& moveOnly = it->Data<MoveOnlyImpl>();
    EXPECT_THAT(moveOnly.value1, 3);
    EXPECT_THAT(moveOnly.value2, 4);
    ++it;
    auto& pinned = it->Data<PinnedImpl>();
    EXPECT_THAT(pinned.value1, 5);
    EXPECT_THAT(pinned.value2, 6);
    EXPECT_THAT(pinned.value3, 7);
    ++it;
    auto& maxAlign = it->Data<MaxAlignedData>();
    EXPECT_THAT(maxAlign.value, 8);
    ++it;
    EXPECT_THAT(it, actual.end());
    EXPECT_THAT(storage.GetSizeUnsafe(), 5);
    EXPECT_THAT(producer.size(), 0);
}

TEST(ObjectFactoryStorageTest, PublishSeveralTimes) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    // Add 2 elements and publish.
    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Publish();

    // Add another element and publish.
    producer.Insert<int>(3);
    producer.Publish();

    // Publish without adding elements.
    producer.Publish();

    // Add yet another two elements and publish.
    producer.Insert<int>(4);
    producer.Insert<int>(5);
    producer.Publish();

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2, 3, 4, 5));
    EXPECT_THAT(storage.GetSizeUnsafe(), 5);
    EXPECT_THAT(producer.size(), 0);
}

TEST(ObjectFactoryStorageTest, PublishInDestructor) {
    ObjectFactoryStorageRegular storage;

    {
        Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
        producer.Insert<int>(1);
        producer.Insert<int>(2);
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2));
    EXPECT_THAT(storage.GetSizeUnsafe(), 2);
}

TEST(ObjectFactoryStorageTest, FindNode) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    auto& node1 = producer.Insert<int>(1);
    auto& node2 = producer.Insert<int>(2);

    producer.Publish();

    EXPECT_THAT(&ObjectFactoryStorageRegular::Node::FromData(node1.Data()), &node1);
    EXPECT_THAT(&ObjectFactoryStorageRegular::Node::FromData(node2.Data()), &node2);
}

TEST(ObjectFactoryStorageTest, EraseFirst) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->Data<int>() == 1) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(2, 3));
    EXPECT_THAT(storage.GetSizeUnsafe(), 2);
    EXPECT_THAT(producer.size(), 0);
}

TEST(ObjectFactoryStorageTest, EraseMiddle) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->Data<int>() == 2) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 3));
    EXPECT_THAT(storage.GetSizeUnsafe(), 2);
    EXPECT_THAT(producer.size(), 0);
}

TEST(ObjectFactoryStorageTest, EraseLast) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->Data<int>() == 3) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2));
    EXPECT_THAT(storage.GetSizeUnsafe(), 2);
    EXPECT_THAT(producer.size(), 0);
}

TEST(ObjectFactoryStorageTest, EraseAll) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            iter.EraseAndAdvance(it);
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(storage.GetSizeUnsafe(), 0);
    EXPECT_THAT(producer.size(), 0);
}

TEST(ObjectFactoryStorageTest, EraseTheOnlyElement) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        auto it = iter.begin();
        iter.EraseAndAdvance(it);
        EXPECT_THAT(it, iter.end());
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(storage.GetSizeUnsafe(), 0);
    EXPECT_THAT(producer.size(), 0);
}

TEST(ObjectFactoryStorageTest, MoveFirst) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    Consumer<ObjectFactoryStorageRegular> consumer;

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->Data<int>() == 1) {
                iter.MoveAndAdvance(consumer, it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);
    auto actualConsumer = Collect<int, alignof(void*)>(consumer);

    EXPECT_THAT(actual, testing::ElementsAre(2, 3));
    EXPECT_THAT(actualConsumer, testing::ElementsAre(1));
    EXPECT_THAT(storage.GetSizeUnsafe(), 2);
    EXPECT_THAT(producer.size(), 0);
    EXPECT_THAT(consumer.size(), 1);
}

TEST(ObjectFactoryStorageTest, MoveMiddle) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    Consumer<ObjectFactoryStorageRegular> consumer;

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->Data<int>() == 2) {
                iter.MoveAndAdvance(consumer, it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);
    auto actualConsumer = Collect<int, alignof(void*)>(consumer);

    EXPECT_THAT(actual, testing::ElementsAre(1, 3));
    EXPECT_THAT(actualConsumer, testing::ElementsAre(2));
    EXPECT_THAT(storage.GetSizeUnsafe(), 2);
    EXPECT_THAT(producer.size(), 0);
    EXPECT_THAT(consumer.size(), 1);
}

TEST(ObjectFactoryStorageTest, MoveLast) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    Consumer<ObjectFactoryStorageRegular> consumer;

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->Data<int>() == 3) {
                iter.MoveAndAdvance(consumer, it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);
    auto actualConsumer = Collect<int, alignof(void*)>(consumer);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2));
    EXPECT_THAT(actualConsumer, testing::ElementsAre(3));
    EXPECT_THAT(storage.GetSizeUnsafe(), 2);
    EXPECT_THAT(producer.size(), 0);
    EXPECT_THAT(consumer.size(), 1);
}

TEST(ObjectFactoryStorageTest, MoveAll) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    Consumer<ObjectFactoryStorageRegular> consumer;

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            iter.MoveAndAdvance(consumer, it);
        }
    }

    auto actual = Collect<int>(storage);
    auto actualConsumer = Collect<int, alignof(void*)>(consumer);

    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(actualConsumer, testing::ElementsAre(1, 2, 3));
    EXPECT_THAT(storage.GetSizeUnsafe(), 0);
    EXPECT_THAT(producer.size(), 0);
    EXPECT_THAT(consumer.size(), 3);
}

TEST(ObjectFactoryStorageTest, MergeWith) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    Consumer<ObjectFactoryStorageRegular> consumer1;
    Consumer<ObjectFactoryStorageRegular> consumer2;


    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);
    producer.Insert<int>(4);
    producer.Insert<int>(5);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->Data<int>() % 2 == 0) {
                iter.MoveAndAdvance(consumer1, it);
            } else {
                ++it;
            }
        }
    }
    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            iter.MoveAndAdvance(consumer2, it);
        }
    }

    auto actual = Collect<int>(storage);
    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(storage.GetSizeUnsafe(), 0);
    EXPECT_THAT(producer.size(), 0);

    {
        auto actualConsumer1 = Collect<int, alignof(void*)>(consumer1);
        auto actualConsumer2 = Collect<int, alignof(void*)>(consumer2);

        EXPECT_THAT(actualConsumer1, testing::ElementsAre(2, 4));
        EXPECT_THAT(consumer1.size(), 2);
        EXPECT_THAT(actualConsumer2, testing::ElementsAre(1, 3, 5));
        EXPECT_THAT(consumer2.size(), 3);
    }

    consumer1.MergeWith(std::move(consumer2));
    {
        auto actualConsumer1 = Collect<int, alignof(void*)>(consumer1);
        auto actualConsumer2 = Collect<int, alignof(void*)>(consumer2);
        EXPECT_THAT(actualConsumer1, testing::ElementsAre(2, 4, 1, 3, 5));
        EXPECT_THAT(consumer1.size(), 5);
        EXPECT_THAT(actualConsumer2, testing::ElementsAre());
        EXPECT_THAT(consumer2.size(), 0);
    }

    Consumer<ObjectFactoryStorageRegular> consumer3;
    consumer1.MergeWith(std::move(consumer3));
    {
        auto actualConsumer1 = Collect<int, alignof(void*)>(consumer1);
        auto actualConsumer2 = Collect<int, alignof(void*)>(consumer2);
        auto actualConsumer3 = Collect<int, alignof(void*)>(consumer3);
        EXPECT_THAT(actualConsumer1, testing::ElementsAre(2, 4, 1, 3, 5));
        EXPECT_THAT(consumer1.size(), 5);
        EXPECT_THAT(actualConsumer2, testing::ElementsAre());
        EXPECT_THAT(consumer2.size(), 0);
        EXPECT_THAT(actualConsumer3, testing::ElementsAre());
        EXPECT_THAT(consumer3.size(), 0);
    }

    consumer3.MergeWith(std::move(consumer1));
    {
        auto actualConsumer1 = Collect<int, alignof(void*)>(consumer1);
        auto actualConsumer2 = Collect<int, alignof(void*)>(consumer2);
        auto actualConsumer3 = Collect<int, alignof(void*)>(consumer3);
        EXPECT_THAT(actualConsumer1, testing::ElementsAre());
        EXPECT_THAT(consumer1.size(), 0);
        EXPECT_THAT(actualConsumer2, testing::ElementsAre());
        EXPECT_THAT(consumer2.size(), 0);
        EXPECT_THAT(actualConsumer3, testing::ElementsAre(2, 4, 1, 3, 5));
        EXPECT_THAT(consumer3.size(), 5);
    }
}

TEST(ObjectFactoryStorageTest, MoveTheOnlyElement) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    Consumer<ObjectFactoryStorageRegular> consumer;

    producer.Insert<int>(1);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        auto it = iter.begin();
        iter.MoveAndAdvance(consumer, it);
        EXPECT_THAT(it, iter.end());
    }

    auto actual = Collect<int>(storage);
    auto actualConsumer = Collect<int, alignof(void*)>(consumer);

    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(actualConsumer, testing::ElementsAre(1));
    EXPECT_THAT(storage.GetSizeUnsafe(), 0);
    EXPECT_THAT(producer.size(), 0);
    EXPECT_THAT(consumer.size(), 1);
}

TEST(ObjectFactoryStorageTest, MoveAndErase) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    Consumer<ObjectFactoryStorageRegular> consumer;

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);
    producer.Insert<int>(4);
    producer.Insert<int>(5);
    producer.Insert<int>(6);
    producer.Insert<int>(7);
    producer.Insert<int>(8);
    producer.Insert<int>(9);

    producer.Publish();

    {
        auto iter = storage.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            ++it;
            iter.EraseAndAdvance(it);
            iter.MoveAndAdvance(consumer, it);
        }
    }

    auto actual = Collect<int>(storage);
    auto actualConsumer = Collect<int, alignof(void*)>(consumer);

    EXPECT_THAT(actual, testing::ElementsAre(1, 4, 7));
    EXPECT_THAT(actualConsumer, testing::ElementsAre(3, 6, 9));
    EXPECT_THAT(storage.GetSizeUnsafe(), 3);
    EXPECT_THAT(producer.size(), 0);
    EXPECT_THAT(consumer.size(), 3);
}

TEST(ObjectFactoryStorageTest, ConcurrentPublish) {
    ObjectFactoryStorageRegular storage;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    KStdVector<ScopedThread> threads;
    KStdVector<int> expected;

    for (int i = 0; i < kThreadCount; ++i) {
        expected.push_back(i);
        threads.emplace_back([i, &storage, &canStart, &readyCount]() {
            Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
            producer.Insert<int>(i);
            ++readyCount;
            while (!canStart) {
            }
            producer.Publish();
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    threads.clear();

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
    EXPECT_THAT(storage.GetSizeUnsafe(), expected.size());
}

TEST(ObjectFactoryStorageTest, IterWhileConcurrentPublish) {
    ObjectFactoryStorageRegular storage;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = kDefaultThreadCount;

    KStdVector<int> expectedBefore;
    KStdVector<int> expectedAfter;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    for (int i = 0; i < kStartCount; ++i) {
        expectedBefore.push_back(i);
        expectedAfter.push_back(i);
        producer.Insert<int>(i);
    }
    producer.Publish();

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::atomic<int> startedCount(0);
    KStdVector<ScopedThread> threads;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = i + kStartCount;
        expectedAfter.push_back(j);
        threads.emplace_back([j, &storage, &canStart, &startedCount, &readyCount]() {
            Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
            producer.Insert<int>(j);
            ++readyCount;
            while (!canStart) {
            }
            ++startedCount;
            producer.Publish();
        });
    }

    KStdVector<int> actualBefore;
    {
        auto iter = storage.LockForIter();
        while (readyCount < kThreadCount) {
        }
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (auto& node : iter) {
            int element = *static_cast<int*>(node.Data());
            actualBefore.push_back(element);
        }
    }

    threads.clear();

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    auto actualAfter = Collect<int>(storage);

    EXPECT_THAT(actualAfter, testing::UnorderedElementsAreArray(expectedAfter));
    EXPECT_THAT(storage.GetSizeUnsafe(), expectedAfter.size());
}

TEST(ObjectFactoryStorageTest, EraseWhileConcurrentPublish) {
    ObjectFactoryStorageRegular storage;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = kDefaultThreadCount;

    KStdVector<int> expectedAfter;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    for (int i = 0; i < kStartCount; ++i) {
        if (i % 2 == 0) {
            expectedAfter.push_back(i);
        }
        producer.Insert<int>(i);
    }
    producer.Publish();

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::atomic<int> startedCount(0);
    KStdVector<ScopedThread> threads;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = i + kStartCount;
        expectedAfter.push_back(j);
        threads.emplace_back([j, &storage, &canStart, &startedCount, &readyCount]() {
            Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
            producer.Insert<int>(j);
            ++readyCount;
            while (!canStart) {
            }
            ++startedCount;
            producer.Publish();
        });
    }

    {
        auto iter = storage.LockForIter();
        while (readyCount < kThreadCount) {
        }
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (auto it = iter.begin(); it != iter.end();) {
            if (it->Data<int>() % 2 != 0) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    threads.clear();

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expectedAfter));
    EXPECT_THAT(storage.GetSizeUnsafe(), expectedAfter.size());
}

namespace {

class MockAllocator : private Pinned {
public:
    MockAllocator();
    ~MockAllocator();

    MOCK_METHOD(void*, Alloc, (size_t, size_t));
    MOCK_METHOD(void, Free, (void*));

    void* DefaultAlloc(size_t size, size_t alignment) { return konanAllocAlignedMemory(size, alignment); }

    void DefaultFree(void* instance) { konanFreeMemory(instance); }
};

class GlobalMockAllocator {
public:
    void* Alloc(size_t size, size_t alignment) {
        RuntimeAssert(instance_ != nullptr, "Global allocator must be set");
        return instance_->Alloc(size, alignment);
    }

    static void Free(void* instance) {
        RuntimeAssert(instance_ != nullptr, "Global allocator must be set");
        instance_->Free(instance);
    }

    static void SetMockAllocator(MockAllocator* instance) {
        RuntimeAssert(instance != nullptr, "Cannot be null");
        RuntimeAssert(instance_ == nullptr, "No global allocator can be set");
        instance_ = instance;
    }

    static void ClearMockAllocator(MockAllocator* instance) {
        RuntimeAssert(instance != nullptr, "Cannot be null");
        RuntimeAssert(instance_ == instance, "Allocators must match");
        instance_ = nullptr;
    }

private:
    static MockAllocator* instance_;
};

MockAllocator::MockAllocator() {
    GlobalMockAllocator::SetMockAllocator(this);
    ON_CALL(*this, Alloc(_, _)).WillByDefault([this](size_t size, size_t alignment) { return DefaultAlloc(size, alignment); });
    ON_CALL(*this, Free(_)).WillByDefault([this](void* instance) { DefaultFree(instance); });
}

MockAllocator::~MockAllocator() {
    GlobalMockAllocator::ClearMockAllocator(this);
}

// static
MockAllocator* GlobalMockAllocator::instance_ = nullptr;

class GC {
public:
    struct ObjectData {
        uint32_t flags = 42;
    };

    using Allocator = GlobalMockAllocator;

    class ThreadData {
    public:
        void SafePointAllocation(size_t size) noexcept {}

        void OnOOM(size_t size) noexcept {}

        Allocator CreateAllocator() noexcept { return Allocator(); }
    };
};

using ObjectFactory = mm::ObjectFactory<GC>;

struct Payload {
    ObjHeader* field1;
    ObjHeader* field2;

    static constexpr std::array kFields{
            &Payload::field1,
            &Payload::field2,
    };
};

} // namespace

TEST(ObjectFactoryTest, CreateObject) {
    testing::StrictMock<MockAllocator> allocator;

    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);

    size_t allocSize = 0;
    void* allocAddress = nullptr;
    EXPECT_CALL(allocator, Alloc(_, _)).WillOnce([&](size_t size, size_t alignment) {
        allocSize = size;
        allocAddress = allocator.DefaultAlloc(size, alignment);
        return allocAddress;
    });
    auto* object = threadQueue.CreateObject(type.typeInfo());
    testing::Mock::VerifyAndClearExpectations(&allocator);
    EXPECT_THAT(allocSize, testing::Gt<size_t>(type.typeInfo()->instanceSize_));
    EXPECT_THAT(allocAddress, testing::Ne(nullptr));
    EXPECT_THAT(mm::GetAllocatedHeapSize(object), allocSize);

    threadQueue.Publish();

    auto node = ObjectFactory::NodeRef::From(object);
    EXPECT_FALSE(node.IsArray());
    EXPECT_THAT(node.GetObjHeader(), object);
    EXPECT_THAT(node.GCObjectData().flags, 42);

    auto iter = objectFactory.LockForIter();
    auto it = iter.begin();
    EXPECT_THAT(*it, node);
    ++it;
    EXPECT_THAT(it, iter.end());

    EXPECT_CALL(allocator, Free(allocAddress));
}

TEST(ObjectFactoryTest, CreateObjectArray) {
    testing::StrictMock<MockAllocator> allocator;

    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);

    size_t allocSize = 0;
    void* allocAddress = nullptr;
    EXPECT_CALL(allocator, Alloc(_, _)).WillOnce([&](size_t size, size_t alignment) {
        allocSize = size;
        allocAddress = allocator.DefaultAlloc(size, alignment);
        return allocAddress;
    });
    auto* array = threadQueue.CreateArray(theArrayTypeInfo, 3);
    testing::Mock::VerifyAndClearExpectations(&allocator);
    EXPECT_THAT(allocSize, testing::Gt<size_t>(-theArrayTypeInfo->instanceSize_ * 3));
    EXPECT_THAT(allocAddress, testing::Ne(nullptr));
    EXPECT_THAT(mm::GetAllocatedHeapSize(array->obj()), allocSize);

    threadQueue.Publish();

    auto node = ObjectFactory::NodeRef::From(array);
    EXPECT_TRUE(node.IsArray());
    EXPECT_THAT(node.GetArrayHeader(), array);
    EXPECT_THAT(node.GCObjectData().flags, 42);

    auto iter = objectFactory.LockForIter();
    auto it = iter.begin();
    EXPECT_THAT(*it, node);
    ++it;
    EXPECT_THAT(it, iter.end());

    EXPECT_CALL(allocator, Free(allocAddress));
}

TEST(ObjectFactoryTest, CreateCharArray) {
    testing::StrictMock<MockAllocator> allocator;

    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);

    size_t allocSize = 0;
    void* allocAddress = nullptr;
    EXPECT_CALL(allocator, Alloc(_, _)).WillOnce([&](size_t size, size_t alignment) {
        allocSize = size;
        allocAddress = allocator.DefaultAlloc(size, alignment);
        return allocAddress;
    });
    auto* array = threadQueue.CreateArray(theCharArrayTypeInfo, 3);
    testing::Mock::VerifyAndClearExpectations(&allocator);
    EXPECT_THAT(allocSize, testing::Gt<size_t>(-theCharArrayTypeInfo->instanceSize_ * 3));
    EXPECT_THAT(allocAddress, testing::Ne(nullptr));
    EXPECT_THAT(mm::GetAllocatedHeapSize(array->obj()), allocSize);

    threadQueue.Publish();

    auto node = ObjectFactory::NodeRef::From(array);
    EXPECT_TRUE(node.IsArray());
    EXPECT_THAT(node.GetArrayHeader(), array);
    EXPECT_THAT(node.GCObjectData().flags, 42);

    auto iter = objectFactory.LockForIter();
    auto it = iter.begin();
    EXPECT_THAT(*it, node);
    ++it;
    EXPECT_THAT(it, iter.end());

    EXPECT_CALL(allocator, Free(allocAddress));
}

TEST(ObjectFactoryTest, Erase) {
    testing::StrictMock<MockAllocator> allocator;

    test_support::TypeInfoHolder objectType{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);

    EXPECT_CALL(allocator, Alloc(_, _)).Times(20);
    for (int i = 0; i < 10; ++i) {
        threadQueue.CreateObject(objectType.typeInfo());
        threadQueue.CreateArray(theArrayTypeInfo, 3);
    }
    testing::Mock::VerifyAndClearExpectations(&allocator);

    threadQueue.Publish();

    {
        auto iter = objectFactory.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->IsArray()) {
                EXPECT_CALL(allocator, Free(_));
                iter.EraseAndAdvance(it);
                testing::Mock::VerifyAndClearExpectations(&allocator);
            } else {
                ++it;
            }
        }
    }

    {
        auto iter = objectFactory.LockForIter();
        int count = 0;
        for (auto it = iter.begin(); it != iter.end(); ++it, ++count) {
            EXPECT_FALSE(it->IsArray());
        }
        EXPECT_THAT(count, 10);
    }
    EXPECT_CALL(allocator, Free(_)).Times(10);
}

TEST(ObjectFactoryTest, Move) {
    testing::StrictMock<MockAllocator> allocator;

    test_support::TypeInfoHolder objectType{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);
    ObjectFactory::FinalizerQueue finalizerQueue;

    EXPECT_CALL(allocator, Alloc(_, _)).Times(20);
    for (int i = 0; i < 10; ++i) {
        threadQueue.CreateObject(objectType.typeInfo());
        threadQueue.CreateArray(theArrayTypeInfo, 3);
    }
    testing::Mock::VerifyAndClearExpectations(&allocator);

    threadQueue.Publish();

    {
        auto iter = objectFactory.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->IsArray()) {
                iter.MoveAndAdvance(finalizerQueue, it);
            } else {
                ++it;
            }
        }
    }

    {
        auto iter = objectFactory.LockForIter();
        int count = 0;
        for (auto it = iter.begin(); it != iter.end(); ++it, ++count) {
            EXPECT_FALSE(it->IsArray());
        }
        EXPECT_THAT(count, 10);
    }

    {
        int count = 0;
        auto iter = finalizerQueue.IterForTests();
        for (auto it = iter.begin(); it != iter.end(); ++it, ++count) {
            EXPECT_TRUE(it->IsArray());
        }
        EXPECT_THAT(count, 10);
    }

    EXPECT_CALL(allocator, Free(_)).Times(20);
}

TEST(ObjectFactoryTest, RunFinalizers) {
    testing::StrictMock<MockAllocator> allocator;

    FinalizerHooksTestSupport finalizerHooks;

    test_support::TypeInfoHolder objectType{test_support::TypeInfoHolder::ObjectBuilder<Payload>().addFlag(TF_HAS_FINALIZER)};
    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);
    ObjectFactory::FinalizerQueue finalizerQueue;

    KStdVector<ObjHeader*> objects;
    EXPECT_CALL(allocator, Alloc(_, _)).Times(10);
    for (int i = 0; i < 10; ++i) {
        objects.push_back(threadQueue.CreateObject(objectType.typeInfo()));
    }
    testing::Mock::VerifyAndClearExpectations(&allocator);

    threadQueue.Publish();

    {
        auto iter = objectFactory.LockForIter();
        for (auto it = iter.begin(); it != iter.end();) {
            iter.MoveAndAdvance(finalizerQueue, it);
        }
    }

    for (auto& object : objects) {
        EXPECT_CALL(finalizerHooks.finalizerHook(), Call(object));
    }
    finalizerQueue.Finalize();
    // Hooks called before `FinalizerQueue` destructor.
    testing::Mock::VerifyAndClearExpectations(&finalizerHooks.finalizerHook());
    EXPECT_CALL(allocator, Free(_)).Times(10);
}

TEST(ObjectFactoryTest, ConcurrentPublish) {
    testing::StrictMock<MockAllocator> allocator;

    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjectFactory objectFactory;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    KStdVector<ScopedThread> threads;
    std::mutex expectedMutex;
    KStdVector<ObjHeader*> expected;

    EXPECT_CALL(allocator, Alloc(_, _)).Times(kThreadCount);
    for (int i = 0; i < kThreadCount; ++i) {
        threads.emplace_back([&type, &objectFactory, &canStart, &readyCount, &expected, &expectedMutex]() {
            GC::ThreadData gc;
            ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);
            auto* object = threadQueue.CreateObject(type.typeInfo());
            {
                std::lock_guard<std::mutex> guard(expectedMutex);
                expected.push_back(object);
            }
            ++readyCount;
            while (!canStart) {
            }
            threadQueue.Publish();
        });
    }

    while (readyCount < kThreadCount) {
    }
    testing::Mock::VerifyAndClearExpectations(&allocator);
    canStart = true;
    threads.clear();

    auto iter = objectFactory.LockForIter();
    KStdVector<ObjHeader*> actual;
    for (auto it = iter.begin(); it != iter.end(); ++it) {
        actual.push_back(it->GetObjHeader());
    }

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
    EXPECT_CALL(allocator, Free(_)).Times(kThreadCount);
}
