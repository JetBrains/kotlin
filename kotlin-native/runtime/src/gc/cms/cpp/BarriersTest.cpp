/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Barriers.hpp"
#include "ConcurrentMark.hpp"
#include "GCImpl.hpp"
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

    ~BarriersTest() override {
        mm::SpecialRefRegistry::instance().clearForTests();
        mm::GlobalData::Instance().allocator().clearForTests();
    }

    void initMutatorMarkQueue(mm::ThreadData& thread) {
        auto& markData = thread.gc().impl().gc().mark();
        markData.markQueue().construct(parProc_);
    }

private:
    gc::mark::ConcurrentMark::ParallelProcessor parProc_;
};

} // namespace

TEST_F(BarriersTest, Deletion) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        initMutatorMarkQueue(threadData);
        auto& prevObj = AllocateObject(threadData);
        auto& newObj = AllocateObject(threadData);

        ObjHeader* ref = prevObj.header();

        EXPECT_THAT(gc::isMarked(prevObj.header()), false);
        EXPECT_THAT(gc::isMarked(newObj.header()), false);

        {
            ThreadStateGuard guard(ThreadState::kNative); // pretend to be the GC thread
            gc::barriers::enableMarkBarriers(gcHandle.getEpoch());
        }

        UpdateHeapRef(&ref, newObj.header());

        EXPECT_THAT(gc::isMarked(prevObj.header()), true);
        EXPECT_THAT(gc::isMarked(newObj.header()), false);

        {
            ThreadStateGuard guard(ThreadState::kNative); // pretend to be the GC thread
            gc::barriers::disableMarkBarriers();
        }

    });
}

TEST_F(BarriersTest, AllocationDuringMarkBarreirs) {
    gc::barriers::enableMarkBarriers(gcHandle.getEpoch());

    RunInNewThread([this](mm::ThreadData& threadData) {
        initMutatorMarkQueue(threadData);
        auto& obj = AllocateObject(threadData);
        EXPECT_THAT(gc::isMarked(obj.header()), true);
    });

    gc::barriers::disableMarkBarriers();
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

    gc::barriers::enableMarkBarriers(gcHandle.getEpoch());

    std::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&]() noexcept {
            ScopedMemoryInit memory;
            mm::ThreadData& threadData = *memory.memoryState()->GetThreadData();
            initMutatorMarkQueue(threadData);

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

    EXPECT_THAT(gc::isMarked(ref), true);
}
