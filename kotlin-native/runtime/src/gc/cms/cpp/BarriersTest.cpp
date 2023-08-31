/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Barriers.hpp"
#include "ObjectTestSupport.hpp"
#include "ObjectOps.hpp"
#include "ReferenceOps.hpp"
#include "TestSupport.hpp"
#include "ObjectData.hpp"


using namespace kotlin;

namespace {

auto gcHandle = gc::GCHandle::createFakeForTests();

struct Payload {
    mm::RefField field1;
    mm::RefField field2;
    mm::RefField field3;

    static constexpr std::array kFields = {
            &Payload::field1,
            &Payload::field2,
            &Payload::field3,
    };
};

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};

test_support::Object<Payload>& AllocateObject(mm::ThreadData& threadData) {
    ObjHolder holder;
    mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder.slot());
    return test_support::Object<Payload>::FromObjHeader(holder.obj());
}

class BarriersTest : public testing::Test {
public:

    ~BarriersTest() {
        mm::SpecialRefRegistry::instance().clearForTests();
        mm::GlobalData::Instance().allocator().clearForTests();
    }

};

} // namespace

TEST_F(BarriersTest, Deletion) {
    gc::barriers::beginMarkingEpoch(gcHandle);

    RunInNewThread([](mm::ThreadData& threadData) {
        auto& prevObj = AllocateObject(threadData);
        auto& newObj = AllocateObject(threadData);

        ObjHeader* ref = prevObj.header();

        EXPECT_THAT(gc::isMarked(prevObj.header()), false);
        EXPECT_THAT(gc::isMarked(newObj.header()), false);

        {
            ThreadStateGuard guard(ThreadState::kNative); // pretend to be the GC thread
            gc::barriers::enableMarkBarriers();
        }

        UpdateHeapRef(&ref, newObj.header());

        EXPECT_THAT(gc::isMarked(prevObj.header()), true);
        EXPECT_THAT(gc::isMarked(newObj.header()), false);

        {
            ThreadStateGuard guard(ThreadState::kNative); // pretend to be the GC thread
            gc::barriers::disableMarkBarriers();
        }

    });

    gc::barriers::endMarkingEpoch();
}

TEST_F(BarriersTest, WeakRefRead) {
    gc::barriers::beginMarkingEpoch(gcHandle);

    RunInNewThread([](mm::ThreadData& threadData) {
        auto& markedObj = AllocateObject(threadData);
        alloc::objectDataForObject(markedObj.header()).tryMark();
        auto& unmarkedObj = AllocateObject(threadData);

        auto& aliveWeakRef = ([&]() -> test_support::RegularWeakReferenceImpl& {
            ObjHolder holder;
            return test_support::InstallWeakReference(threadData, markedObj.header(), holder.slot());
        })();
        auto& deadWeakRef = ([&]() -> test_support::RegularWeakReferenceImpl& {
            ObjHolder holder;
            return test_support::InstallWeakReference(threadData, unmarkedObj.header(), holder.slot());
        })();

        EXPECT_THAT(gc::isMarked(markedObj.header()), true);
        EXPECT_THAT(gc::isMarked(unmarkedObj.header()), false);

        {
            ThreadStateGuard guard(ThreadState::kNative); // pretend to be the GC thread
            gc::barriers::enableWeakRefBarriers();
        }

        EXPECT_NE(aliveWeakRef.get(), nullptr);
        EXPECT_EQ(deadWeakRef.get(), nullptr);

        {
            ThreadStateGuard guard(ThreadState::kNative); // pretend to be the GC thread
            gc::barriers::disableWeakRefBarriers();
        }

        auto& markedObjsExtra = *mm::ExtraObjectData::Get(markedObj.header());
        auto& unmarkedObjsExtra = *mm::ExtraObjectData::Get(unmarkedObj.header());
        markedObjsExtra.ClearRegularWeakReferenceImpl();
        unmarkedObjsExtra.ClearRegularWeakReferenceImpl();
        markedObjsExtra.Uninstall();
        unmarkedObjsExtra.Uninstall();
        alloc::destroyExtraObjectData(markedObjsExtra);
        alloc::destroyExtraObjectData(unmarkedObjsExtra);
    });

    gc::barriers::endMarkingEpoch();
}

TEST_F(BarriersTest, AllocationDuringMarkBarreirs) {
    gc::barriers::beginMarkingEpoch(gcHandle);
    gc::barriers::enableMarkBarriers();

    RunInNewThread([](mm::ThreadData& threadData) {
        auto& obj = AllocateObject(threadData);
        EXPECT_THAT(gc::isMarked(obj.header()), true);
    });

    gc::barriers::disableMarkBarriers();
    gc::barriers::endMarkingEpoch();
}

TEST_F(BarriersTest, AllocationDuringWeakBarreirs) {
    gc::barriers::beginMarkingEpoch(gcHandle);
    gc::barriers::enableWeakRefBarriers();

    RunInNewThread([](mm::ThreadData& threadData) {
        auto& obj = AllocateObject(threadData);
        EXPECT_THAT(gc::isMarked(obj.header()), true);
    });

    gc::barriers::disableWeakRefBarriers();
    gc::barriers::endMarkingEpoch();
}

TEST_F(BarriersTest, ConcurrentDeletion) {
    constexpr auto kObjsPerThread = 100;

    ObjHeader* ref = nullptr;

    RunInNewThread([&](mm::ThreadData& threadData) {
        auto& obj = AllocateObject(threadData);
        UpdateHeapRef(&ref, obj.header());
        threadData.allocator().prepareForGC();
    });

    EXPECT_THAT(gc::isMarked(ref), false);

    std::atomic<bool> canStart = false;
    std::atomic<std::size_t> finished = 0;

    gc::barriers::beginMarkingEpoch(gcHandle);
    gc::barriers::enableMarkBarriers();

    std::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&]() noexcept {
            ScopedMemoryInit memory;
            mm::ThreadData& threadData = *memory.memoryState()->GetThreadData();

            while (!canStart.load()) std::this_thread::yield();

            for (int j = 0; j < kObjsPerThread; ++j) {
                auto& obj = AllocateObject(threadData);
                // auto&& accessor = mm::RefFieldAccessor(&ref);
                // accessor.storeAtomic(obj.header(), std::memory_order_release);
                UpdateHeapRef(&ref, obj.header());
            }

            finished += 1;

            threadData.allocator().prepareForGC();
        });
    }

    canStart = true;

    while (finished.load() < threads.size()) {
        std::this_thread::yield();
    }

    gc::barriers::disableMarkBarriers();
    gc::barriers::endMarkingEpoch();

    EXPECT_THAT(gc::isMarked(ref), true);
}
