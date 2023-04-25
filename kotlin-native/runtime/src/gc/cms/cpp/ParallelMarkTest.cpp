/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ParallelMark.hpp"
#include "ParallelMarkTestSupport.hpp"
#include "GCImpl.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ScopedThread.hpp"
#include "TestSupport.hpp"
#include "ObjectTestSupport.hpp"

#include "std_support/Deque.hpp"
#include "std_support/Vector.hpp"
#include "SingleThreadExecutor.hpp"

using ::testing::_;
using namespace kotlin;
using namespace kotlin::gc::mark::test;

namespace {

struct Payload {
    using Field = ObjHeader* Payload::*;
    static constexpr std::array<Field, 0> kFields{};
};
test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};

using Object = test_support::Object<Payload>;

test_support::Object<Payload>& AllocateObject(mm::ThreadData& threadData) {
    ObjHolder holder;
    mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder.slot());
    return test_support::Object<Payload>::FromObjHeader(holder.obj());
}

std::list<ObjHeader*> workBatch(std::size_t size) {
    std::list<ObjHeader*> batch;
    RunInNewThread([&](mm::ThreadData& threadData) {
        for (size_t i = 0; i < size; ++i) {
            auto obj = AllocateObject(threadData).header();
            batch.push_back(obj);
            auto nodeRef = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(obj);
            auto& objData = nodeRef.ObjectData();
            objData.tryResetMark();
            RuntimeAssert(!objData.marked(), "Must not be marked");
        }
        threadData.Publish();
    });
    return batch;
}

template <typename Iterable>
void offerWork(gc::mark::MarkDispatcher::MarkJob& job, Iterable& workBatch) {
    auto& workList = ParallelMarkTestSupport::workList(job);
    for (auto& obj: workBatch) {
        auto nodeRef = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>::NodeRef::From(obj);
        auto& objData = nodeRef.ObjectData();
        RuntimeAssert(!objData.marked(), "Must not be marked");
        bool accepted = workList.tryPushLocal(objData);
        RuntimeAssert(accepted, "Must be accepted");
    }
}

const auto kEnoughToShare = gc::mark::MarkDispatcher::kMinWorkSizeToShare * 2;

class ParallelMarkTest : public testing::Test {
public:
    ~ParallelMarkTest() override {
        mm::GlobalsRegistry::Instance().ClearForTests();
        mm::GlobalData::Instance().extraObjectDataFactory().ClearForTests();
        mm::GlobalData::Instance().gc().impl().objectFactory().ClearForTests();
    }
};

}

TEST_F(ParallelMarkTest, UncontendedStealing) {
    auto dispatcher = ParallelMarkTestSupport::fakeDispatcher(2);
    gc::mark::MarkDispatcher::MarkJob worker(*dispatcher);
    ParallelMarkTestSupport::registerTask(*dispatcher, worker);
    gc::mark::MarkDispatcher::MarkJob thief(*dispatcher);
    ParallelMarkTestSupport::registerTask(*dispatcher, thief);

    auto work = workBatch(kEnoughToShare);
    offerWork(worker, work);
    
    EXPECT_TRUE(ParallelMarkTestSupport::shareWork(worker));
    EXPECT_TRUE(ParallelMarkTestSupport::tryAcquireWork(thief));
}

TEST_F(ParallelMarkTest, ShareEventually) {
    auto dispatcher = ParallelMarkTestSupport::fakeDispatcher(1);
    gc::mark::MarkDispatcher::MarkJob worker(*dispatcher);
    ParallelMarkTestSupport::registerTask(*dispatcher, worker);

    auto work = workBatch(kEnoughToShare);
    offerWork(worker, work);
    
    EXPECT_TRUE(ParallelMarkTestSupport::workList(worker).sharedEmpty());
    ParallelMarkTestSupport::performWork(worker);
    EXPECT_TRUE(!ParallelMarkTestSupport::workList(worker).sharedEmpty());
}

TEST_F(ParallelMarkTest, ContendedRegistration) {
    auto dispatcher = ParallelMarkTestSupport::fakeDispatcher(kDefaultThreadCount);

    std::list<SingleThreadExecutor<gc::mark::MarkDispatcher::MarkJob>> executors;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        executors.emplace_back([&dispatcher] { return gc::mark::MarkDispatcher::MarkJob(*dispatcher); });
    }

    std::vector<std::future<void>> regFutures;
    for (auto& exec: executors) {
        auto future = exec.execute([&exec, &dispatcher] {
            ParallelMarkTestSupport::registerTask(*dispatcher, exec.context());
        });
        regFutures.push_back(std::move(future));
    }

    for (auto& fut: regFutures) {
        fut.wait();
    }

    EXPECT_THAT(ParallelMarkTestSupport::registeredTasks(*dispatcher), kDefaultThreadCount);
}
