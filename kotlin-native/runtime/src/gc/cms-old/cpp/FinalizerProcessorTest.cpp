/*
* Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#include "ConcurrentMarkAndSweep.hpp"

#include <condition_variable>
#include <future>
#include <mutex>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ExtraObjectData.hpp"
#include "FinalizerHooksTestSupport.hpp"
#include "GCImpl.hpp"
#include "GlobalData.hpp"
#include "ObjectOps.hpp"
#include "ObjectTestSupport.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "FinalizerProcessor.hpp"

using namespace kotlin;

// These tests can only work if `GC` is `ConcurrentMarkAndSweep`.

namespace {

struct Payload {
   ObjHeader* field1;
   ObjHeader* field2;
   ObjHeader* field3;

   static constexpr std::array kFields = {
           &Payload::field1,
           &Payload::field2,
           &Payload::field3,
   };
};

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
test_support::TypeInfoHolder typeHolderWithFinalizer{test_support::TypeInfoHolder::ObjectBuilder<Payload>().addFlag(TF_HAS_FINALIZER)};

test_support::Object<Payload>& AllocateObjectWithFinalizer(mm::ThreadData& threadData) {
   ObjHolder holder;
   mm::AllocateObject(&threadData, typeHolderWithFinalizer.typeInfo(), holder.slot());
   return test_support::Object<Payload>::FromObjHeader(holder.obj());
}

class FinalizerProcessorTest : public testing::Test {
public:

   ~FinalizerProcessorTest() {
       mm::GlobalsRegistry::Instance().ClearForTests();
       mm::GlobalData::Instance().extraObjectDataFactory().ClearForTests();
       mm::GlobalData::Instance().gc().ClearForTests();
   }

   testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHooks_.finalizerHook(); }

private:
   FinalizerHooksTestSupport finalizerHooks_;
};

int threadsCount() {
    int result = 0;
    for (auto &thread: mm::ThreadRegistry::Instance().LockForIter()) {
        static_cast<void>(thread); // to avoid unused warning
        result++;
    }
    return result;
};

} // namespace

TEST_F(FinalizerProcessorTest, NotRunningThreadWhenUnused) {
    GCStateHolder state;
    gc::FinalizerProcessor processor([](int64_t) {});
    ASSERT_EQ(threadsCount(), 0);
    ASSERT_FALSE(processor.IsRunning());
    mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::FinalizerQueue queue;
    processor.ScheduleTasks(std::move(queue), 1);
    ASSERT_EQ(threadsCount(), 0);
    ASSERT_FALSE(processor.IsRunning());
}

TEST_F(FinalizerProcessorTest, RemoveObject) {
    RunInNewThread([this] {
        ASSERT_EQ(threadsCount(), 1);
        std::atomic<int64_t> done = 0;
        gc::FinalizerProcessor processor([&](int64_t epoch) { done = epoch; });
        mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::FinalizerQueue queue;
        auto &object = AllocateObjectWithFinalizer(*mm::ThreadRegistry::Instance().CurrentThreadData());
        mm::ThreadRegistry::Instance().CurrentThreadData()->Publish();
        auto& factory = mm::GlobalData::Instance().gc().impl().objectFactory();
        auto iter = factory.LockForIter();
        auto iterator = iter.begin();
        iter.MoveAndAdvance(queue, iterator);
        ASSERT_EQ(queue.size(), 1u);
        EXPECT_CALL(finalizerHook(), Call(object.header()));
        processor.ScheduleTasks(std::move(queue), 1);
        while (done != 1) {}
        ASSERT_EQ(threadsCount(), 2);
        ASSERT_TRUE(processor.IsRunning());
        processor.StopFinalizerThread();
        ASSERT_EQ(threadsCount(), 1);
    });
}

TEST_F(FinalizerProcessorTest, ScheduleTasksWhileFinalizing) {
    RunInNewThread([this] {
        std::atomic<int64_t> done = 0;
        gc::FinalizerProcessor processor([&done](int64_t epoch) { done = epoch; });
        std::vector<mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::FinalizerQueue> queues;
        int epochs = 100;
        std::vector<ObjHeader*> headers;
        for (int epoch = 0; epoch < epochs; epoch++) {
            for (int i = 0; i < 10; i++) {
                auto& object = AllocateObjectWithFinalizer(*mm::ThreadRegistry::Instance().CurrentThreadData());
                headers.push_back(object.header());
            }
            auto& factory = mm::GlobalData::Instance().gc().impl().objectFactory();
            mm::ThreadRegistry::Instance().CurrentThreadData()->Publish();
            auto iter = factory.LockForIter();
            mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::FinalizerQueue queue;
            for (auto iterator = iter.begin(); iterator != iter.end();) {
                iter.MoveAndAdvance(queue, iterator);
            }
            queues.push_back(std::move(queue));
        }
        for (auto header: headers) {
            EXPECT_CALL(finalizerHook(), Call(header));
        }
        for (int epoch = 0; epoch < epochs; epoch++) {
            processor.ScheduleTasks(std::move(queues[epoch]), epoch + 1);
        }
        while (done != epochs) {}
        ASSERT_EQ(threadsCount(), 2);
        ASSERT_TRUE(processor.IsRunning());
        processor.StopFinalizerThread();
        ASSERT_EQ(threadsCount(), 1);
    });
}

