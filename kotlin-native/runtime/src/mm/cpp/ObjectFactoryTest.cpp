/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectFactory.hpp"

#include <atomic>
#include <thread>
#include <type_traits>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "GC.hpp"
#include "TestSupport.hpp"
#include "Types.h"

using namespace kotlin;

using testing::_;

namespace {

using SimpleAllocator = mm::internal::SimpleAllocator;

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
    for (auto& node : storage.Iter()) {
        result.push_back(node.Data());
    }
    return result;
}

template <typename T, size_t DataAlignment>
KStdVector<T> Collect(ObjectFactoryStorage<DataAlignment>& storage) {
    KStdVector<T> result;
    for (auto& node : storage.Iter()) {
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
}

TEST(ObjectFactoryStorageTest, DoNotPublish) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);

    auto actual = Collect(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
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

    auto actual = storage.Iter();
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
        auto iter = storage.Iter();
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
}

TEST(ObjectFactoryStorageTest, EraseMiddle) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.Iter();
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
}

TEST(ObjectFactoryStorageTest, EraseLast) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.Iter();
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
}

TEST(ObjectFactoryStorageTest, EraseAll) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);
    producer.Insert<int>(2);
    producer.Insert<int>(3);

    producer.Publish();

    {
        auto iter = storage.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            iter.EraseAndAdvance(it);
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ObjectFactoryStorageTest, EraseTheOnlyElement) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());

    producer.Insert<int>(1);

    producer.Publish();

    {
        auto iter = storage.Iter();
        auto it = iter.begin();
        iter.EraseAndAdvance(it);
        EXPECT_THAT(it, iter.end());
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
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
        auto iter = storage.Iter();
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
        auto iter = storage.Iter();
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
        auto iter = storage.Iter();
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
        auto iter = storage.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            iter.MoveAndAdvance(consumer, it);
        }
    }

    auto actual = Collect<int>(storage);
    auto actualConsumer = Collect<int, alignof(void*)>(consumer);

    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(actualConsumer, testing::ElementsAre(1, 2, 3));
}

TEST(ObjectFactoryStorageTest, MoveTheOnlyElement) {
    ObjectFactoryStorageRegular storage;
    Producer<ObjectFactoryStorageRegular> producer(storage, SimpleAllocator());
    Consumer<ObjectFactoryStorageRegular> consumer;

    producer.Insert<int>(1);

    producer.Publish();

    {
        auto iter = storage.Iter();
        auto it = iter.begin();
        iter.MoveAndAdvance(consumer, it);
        EXPECT_THAT(it, iter.end());
    }

    auto actual = Collect<int>(storage);
    auto actualConsumer = Collect<int, alignof(void*)>(consumer);

    EXPECT_THAT(actual, testing::IsEmpty());
    EXPECT_THAT(actualConsumer, testing::ElementsAre(1));
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
        auto iter = storage.Iter();
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
}

TEST(ObjectFactoryStorageTest, ConcurrentPublish) {
    ObjectFactoryStorageRegular storage;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    KStdVector<std::thread> threads;
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
    for (auto& t : threads) {
        t.join();
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
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
    KStdVector<std::thread> threads;
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
        auto iter = storage.Iter();
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

    for (auto& t : threads) {
        t.join();
    }

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    auto actualAfter = Collect<int>(storage);

    EXPECT_THAT(actualAfter, testing::UnorderedElementsAreArray(expectedAfter));
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
    KStdVector<std::thread> threads;
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
        auto iter = storage.Iter();
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

    for (auto& t : threads) {
        t.join();
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expectedAfter));
}

using mm::internal::AllocatorWithGC;

namespace {

class MockAllocator {
public:
    MOCK_METHOD(void*, Alloc, (size_t, size_t));
};

class MockAllocatorWrapper {
public:
    MockAllocator& operator*() { return *mock_; }

    void* Alloc(size_t size, size_t alignment) { return mock_->Alloc(size, alignment); }

private:
    KStdUniquePtr<testing::StrictMock<MockAllocator>> mock_ = make_unique<testing::StrictMock<MockAllocator>>();
};

class MockGC {
public:
    MOCK_METHOD(void, SafePointAllocation, (size_t));
    MOCK_METHOD(void, OnOOM, (size_t));
};

} // namespace

TEST(AllocatorWithGCTest, AllocateWithoutOOM) {
    constexpr size_t size = 256;
    constexpr size_t alignment = 8;
    void* nonNull = reinterpret_cast<void*>(1);
    MockAllocatorWrapper baseAllocator;
    testing::StrictMock<MockGC> gc;
    {
        testing::InSequence seq;
        EXPECT_CALL(gc, SafePointAllocation(size));
        EXPECT_CALL(*baseAllocator, Alloc(size, alignment)).WillOnce(testing::Return(nonNull));
        EXPECT_CALL(gc, OnOOM(_)).Times(0);
    }
    AllocatorWithGC<MockAllocatorWrapper, MockGC> allocator(std::move(baseAllocator), gc);
    void* ptr = allocator.Alloc(size, alignment);
    EXPECT_THAT(ptr, nonNull);
}

TEST(AllocatorWithGCTest, AllocateWithFixableOOM) {
    constexpr size_t size = 256;
    constexpr size_t alignment = 8;
    void* nonNull = reinterpret_cast<void*>(1);
    MockAllocatorWrapper baseAllocator;
    testing::StrictMock<MockGC> gc;
    {
        testing::InSequence seq;
        EXPECT_CALL(gc, SafePointAllocation(size));
        EXPECT_CALL(*baseAllocator, Alloc(size, alignment)).WillOnce(testing::Return(nullptr));
        EXPECT_CALL(gc, OnOOM(size));
        EXPECT_CALL(*baseAllocator, Alloc(size, alignment)).WillOnce(testing::Return(nonNull));
    }
    AllocatorWithGC<MockAllocatorWrapper, MockGC> allocator(std::move(baseAllocator), gc);
    void* ptr = allocator.Alloc(size, alignment);
    EXPECT_THAT(ptr, nonNull);
}

TEST(AllocatorWithGCTest, AllocateWithUnfixableOOM) {
    constexpr size_t size = 256;
    constexpr size_t alignment = 8;
    MockAllocatorWrapper baseAllocator;
    testing::StrictMock<MockGC> gc;
    {
        testing::InSequence seq;
        EXPECT_CALL(gc, SafePointAllocation(size));
        EXPECT_CALL(*baseAllocator, Alloc(size, alignment)).WillOnce(testing::Return(nullptr));
        EXPECT_CALL(gc, OnOOM(size));
        EXPECT_CALL(*baseAllocator, Alloc(size, alignment)).WillOnce(testing::Return(nullptr));
    }
    AllocatorWithGC<MockAllocatorWrapper, MockGC> allocator(std::move(baseAllocator), gc);
    void* ptr = allocator.Alloc(size, alignment);
    EXPECT_THAT(ptr, nullptr);
}

namespace {

class GC {
public:
    struct ObjectData {
        uint32_t flags = 42;
    };

    class ThreadData {
    public:
        void SafePointAllocation(size_t size) noexcept {}

        void OnOOM(size_t size) noexcept {}
    };
};

using ObjectFactory = mm::ObjectFactory<GC>;

KStdUniquePtr<TypeInfo> MakeObjectTypeInfo(int32_t size) {
    auto typeInfo = make_unique<TypeInfo>();
    typeInfo->typeInfo_ = typeInfo.get();
    typeInfo->instanceSize_ = size;
    return typeInfo;
}

KStdUniquePtr<TypeInfo> MakeArrayTypeInfo(int32_t elementSize) {
    auto typeInfo = make_unique<TypeInfo>();
    typeInfo->typeInfo_ = typeInfo.get();
    typeInfo->instanceSize_ = -elementSize;
    return typeInfo;
}

} // namespace

TEST(ObjectFactoryTest, CreateObject) {
    auto typeInfo = MakeObjectTypeInfo(24);
    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);

    auto* object = threadQueue.CreateObject(typeInfo.get());
    threadQueue.Publish();

    auto node = ObjectFactory::NodeRef::From(object);
    EXPECT_FALSE(node.IsArray());
    EXPECT_THAT(node.GetObjHeader(), object);
    EXPECT_THAT(node.GCObjectData().flags, 42);

    auto iter = objectFactory.Iter();
    auto it = iter.begin();
    EXPECT_THAT(*it, node);
    ++it;
    EXPECT_THAT(it, iter.end());
}

TEST(ObjectFactoryTest, CreateArray) {
    auto typeInfo = MakeArrayTypeInfo(24);
    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);

    auto* array = threadQueue.CreateArray(typeInfo.get(), 3);
    threadQueue.Publish();

    auto node = ObjectFactory::NodeRef::From(array);
    EXPECT_TRUE(node.IsArray());
    EXPECT_THAT(node.GetArrayHeader(), array);
    EXPECT_THAT(node.GCObjectData().flags, 42);

    auto iter = objectFactory.Iter();
    auto it = iter.begin();
    EXPECT_THAT(*it, node);
    ++it;
    EXPECT_THAT(it, iter.end());
}

TEST(ObjectFactoryTest, Erase) {
    auto objectTypeInfo = MakeObjectTypeInfo(24);
    auto arrayTypeInfo = MakeArrayTypeInfo(24);
    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);

    for (int i = 0; i < 10; ++i) {
        threadQueue.CreateObject(objectTypeInfo.get());
        threadQueue.CreateArray(arrayTypeInfo.get(), 3);
    }

    threadQueue.Publish();

    {
        auto iter = objectFactory.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->IsArray()) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    {
        auto iter = objectFactory.Iter();
        int count = 0;
        for (auto it = iter.begin(); it != iter.end(); ++it, ++count) {
            EXPECT_FALSE(it->IsArray());
        }
        EXPECT_THAT(count, 10);
    }
}

TEST(ObjectFactoryTest, Move) {
    auto objectTypeInfo = MakeObjectTypeInfo(24);
    auto arrayTypeInfo = MakeArrayTypeInfo(24);
    GC::ThreadData gc;
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);
    ObjectFactory::FinalizerQueue finalizerQueue;

    for (int i = 0; i < 10; ++i) {
        threadQueue.CreateObject(objectTypeInfo.get());
        threadQueue.CreateArray(arrayTypeInfo.get(), 3);
    }

    threadQueue.Publish();

    {
        auto iter = objectFactory.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it->IsArray()) {
                iter.MoveAndAdvance(finalizerQueue, it);
            } else {
                ++it;
            }
        }
    }

    {
        auto iter = objectFactory.Iter();
        int count = 0;
        for (auto it = iter.begin(); it != iter.end(); ++it, ++count) {
            EXPECT_FALSE(it->IsArray());
        }
        EXPECT_THAT(count, 10);
    }

    {
        int count = 0;
        for (auto it = finalizerQueue.begin(); it != finalizerQueue.end(); ++it, ++count) {
            EXPECT_TRUE(it->IsArray());
        }
        EXPECT_THAT(count, 10);
    }
}

TEST(ObjectFactoryTest, ConcurrentPublish) {
    auto typeInfo = MakeObjectTypeInfo(24);
    ObjectFactory objectFactory;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    KStdVector<std::thread> threads;
    std::mutex expectedMutex;
    KStdVector<ObjHeader*> expected;

    for (int i = 0; i < kThreadCount; ++i) {
        threads.emplace_back([&typeInfo, &objectFactory, &canStart, &readyCount, &expected, &expectedMutex]() {
            GC::ThreadData gc;
            ObjectFactory::ThreadQueue threadQueue(objectFactory, gc);
            auto* object = threadQueue.CreateObject(typeInfo.get());
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
    canStart = true;
    for (auto& t : threads) {
        t.join();
    }

    auto iter = objectFactory.Iter();
    KStdVector<ObjHeader*> actual;
    for (auto it = iter.begin(); it != iter.end(); ++it) {
        actual.push_back(it->GetObjHeader());
    }

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}
