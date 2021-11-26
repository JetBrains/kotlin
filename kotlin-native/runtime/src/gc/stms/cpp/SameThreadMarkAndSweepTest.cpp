/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SameThreadMarkAndSweep.hpp"

#include <condition_variable>
#include <future>
#include <mutex>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ExtraObjectData.hpp"
#include "FinalizerHooksTestSupport.hpp"
#include "GlobalData.hpp"
#include "ObjectOps.hpp"
#include "ObjectTestSupport.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

// These tests can only work if `GC` is `SameThreadMarkAndSweep`.
// TODO: Extracting GC into a separate module will help with this.

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

// TODO: This should go into test support for weak references.
struct WeakCounterPayload {
    void* referred;
    KInt lock;
    KInt cookie;

    static constexpr std::array<ObjHeader * WeakCounterPayload::*, 0> kFields{};
};

using WeakCounter = test_support::Object<WeakCounterPayload>;

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
test_support::TypeInfoHolder typeHolderWithFinalizer{test_support::TypeInfoHolder::ObjectBuilder<Payload>().addFlag(TF_HAS_FINALIZER)};
test_support::TypeInfoHolder typeHolderWeakCounter{test_support::TypeInfoHolder::ObjectBuilder<WeakCounterPayload>()};

// TODO: Clean GlobalObjectHolder after it's gone.
class GlobalObjectHolder : private Pinned {
public:
    explicit GlobalObjectHolder(mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &location_);
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), &location_);
    }

    GlobalObjectHolder(mm::ThreadData& threadData, ObjHeader* object) : location_(object) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &location_);
    }

    ObjHeader* header() { return location_; }

    test_support::Object<Payload>& operator*() { return test_support::Object<Payload>::FromObjHeader(location_); }
    test_support::Object<Payload>& operator->() { return test_support::Object<Payload>::FromObjHeader(location_); }

private:
    ObjHeader* location_ = nullptr;
};

// TODO: Clean GlobalPermanentObjectHolder after it's gone.
class GlobalPermanentObjectHolder : private Pinned {
public:
    explicit GlobalPermanentObjectHolder(mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &global_);
        global_->typeInfoOrMeta_ = setPointerBits(global_->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        RuntimeAssert(global_->permanent(), "Must be permanent");
    }

    ObjHeader* header() { return global_; }

    test_support::Object<Payload>& operator*() { return object_; }
    test_support::Object<Payload>& operator->() { return object_; }

private:
    test_support::Object<Payload> object_{typeHolder.typeInfo()};
    ObjHeader* global_{object_.header()};
};

// TODO: Clean GlobalObjectArrayHolder after it's gone.
class GlobalObjectArrayHolder : private Pinned {
public:
    explicit GlobalObjectArrayHolder(mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &location_);
        mm::AllocateArray(&threadData, theArrayTypeInfo, 3, &location_);
    }

    ObjHeader* header() { return location_; }

    test_support::ObjectArray<3>& operator*() { return test_support::ObjectArray<3>::FromArrayHeader(location_->array()); }
    test_support::ObjectArray<3>& operator->() { return test_support::ObjectArray<3>::FromArrayHeader(location_->array()); }

    ObjHeader*& operator[](size_t index) noexcept { return (**this).elements()[index]; }

private:
    ObjHeader* location_ = nullptr;
};

// TODO: Clean GlobalCharArrayHolder after it's gone.
class GlobalCharArrayHolder : private Pinned {
public:
    explicit GlobalCharArrayHolder(mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &location_);
        mm::AllocateArray(&threadData, theCharArrayTypeInfo, 3, &location_);
    }

    ObjHeader* header() { return location_; }

    test_support::CharArray<3>& operator*() { return test_support::CharArray<3>::FromArrayHeader(location_->array()); }
    test_support::CharArray<3>& operator->() { return test_support::CharArray<3>::FromArrayHeader(location_->array()); }

private:
    ObjHeader* location_ = nullptr;
};

class StackObjectHolder : private Pinned {
public:
    explicit StackObjectHolder(mm::ThreadData& threadData) { mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder_.slot()); }
    explicit StackObjectHolder(test_support::Object<Payload>& object) : holder_(object.header()) {}
    explicit StackObjectHolder(ObjHeader* object) : holder_(object) {}

    ObjHeader* header() { return holder_.obj(); }

    test_support::Object<Payload>& operator*() { return test_support::Object<Payload>::FromObjHeader(holder_.obj()); }
    test_support::Object<Payload>& operator->() { return test_support::Object<Payload>::FromObjHeader(holder_.obj()); }

private:
    ObjHolder holder_;
};

class StackObjectArrayHolder : private Pinned {
public:
    explicit StackObjectArrayHolder(mm::ThreadData& threadData) { mm::AllocateArray(&threadData, theArrayTypeInfo, 3, holder_.slot()); }

    ObjHeader* header() { return holder_.obj(); }

    test_support::ObjectArray<3>& operator*() { return test_support::ObjectArray<3>::FromArrayHeader(holder_.obj()->array()); }
    test_support::ObjectArray<3>& operator->() { return test_support::ObjectArray<3>::FromArrayHeader(holder_.obj()->array()); }

    ObjHeader*& operator[](size_t index) noexcept { return (**this).elements()[index]; }

private:
    ObjHolder holder_;
};

class StackCharArrayHolder : private Pinned {
public:
    explicit StackCharArrayHolder(mm::ThreadData& threadData) { mm::AllocateArray(&threadData, theCharArrayTypeInfo, 3, holder_.slot()); }

    ObjHeader* header() { return holder_.obj(); }

    test_support::CharArray<3>& operator*() { return test_support::CharArray<3>::FromArrayHeader(holder_.obj()->array()); }
    test_support::CharArray<3>& operator->() { return test_support::CharArray<3>::FromArrayHeader(holder_.obj()->array()); }

private:
    ObjHolder holder_;
};

test_support::Object<Payload>& AllocateObject(mm::ThreadData& threadData) {
    ObjHolder holder;
    mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder.slot());
    return test_support::Object<Payload>::FromObjHeader(holder.obj());
}

test_support::Object<Payload>& AllocateObjectWithFinalizer(mm::ThreadData& threadData) {
    ObjHolder holder;
    mm::AllocateObject(&threadData, typeHolderWithFinalizer.typeInfo(), holder.slot());
    return test_support::Object<Payload>::FromObjHeader(holder.obj());
}

KStdVector<ObjHeader*> Alive(mm::ThreadData& threadData) {
    KStdVector<ObjHeader*> objects;
    for (auto node : threadData.objectFactoryThreadQueue()) {
        objects.push_back(node.IsArray() ? node.GetArrayHeader()->obj() : node.GetObjHeader());
    }
    for (auto node : mm::GlobalData::Instance().objectFactory().LockForIter()) {
        objects.push_back(node.IsArray() ? node.GetArrayHeader()->obj() : node.GetObjHeader());
    }
    return objects;
}

using Color = gc::SameThreadMarkAndSweep::ObjectData::Color;

Color GetColor(ObjHeader* objHeader) {
    auto nodeRef = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(objHeader);
    return nodeRef.GCObjectData().color();
}

WeakCounter& InstallWeakCounter(mm::ThreadData& threadData, ObjHeader* objHeader, ObjHeader** location) {
    mm::AllocateObject(&threadData, typeHolderWeakCounter.typeInfo(), location);
    auto& weakCounter = WeakCounter::FromObjHeader(*location);
    auto& extraObjectData = mm::ExtraObjectData::GetOrInstall(objHeader);
    auto *setCounter = extraObjectData.GetOrSetWeakReferenceCounter(objHeader, weakCounter.header());
    EXPECT_EQ(setCounter, weakCounter.header());
    weakCounter->referred = objHeader;
    return weakCounter;
}

class SameThreadMarkAndSweepTest : public testing::Test {
public:
    SameThreadMarkAndSweepTest() {
        mm::GlobalData::Instance().gcScheduler().ReplaceGCSchedulerDataForTests(
                [](auto& config, auto scheduleGC) { return gc::internal::MakeEmptyGCSchedulerData(); });
    }

    ~SameThreadMarkAndSweepTest() {
        mm::GlobalsRegistry::Instance().ClearForTests();
        mm::GlobalData::Instance().extraObjectDataFactory().ClearForTests();
        mm::GlobalData::Instance().objectFactory().ClearForTests();
        mm::GlobalData::Instance().gcScheduler().ReplaceGCSchedulerDataForTests(
                [](auto& config, auto scheduleGC) { return gc::internal::MakeGCSchedulerData(config, std::move(scheduleGC)); });
    }

    testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHooks_.finalizerHook(); }

private:
    FinalizerHooksTestSupport finalizerHooks_;
};

} // namespace

TEST_F(SameThreadMarkAndSweepTest, RootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global1{threadData};
        GlobalObjectArrayHolder global2{threadData};
        GlobalCharArrayHolder global3{threadData};
        StackObjectHolder stack1{threadData};
        StackObjectArrayHolder stack2{threadData};
        StackCharArrayHolder stack3{threadData};

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        ASSERT_THAT(GetColor(global1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(global2.header()), Color::kWhite);
        ASSERT_THAT(GetColor(global3.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack2.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack3.header()), Color::kWhite);

        threadData.gc().PerformFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        EXPECT_THAT(GetColor(global1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(global2.header()), Color::kWhite);
        EXPECT_THAT(GetColor(global3.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack2.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack3.header()), Color::kWhite);
    });
}

TEST_F(SameThreadMarkAndSweepTest, InterconnectedRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global1{threadData};
        GlobalObjectArrayHolder global2{threadData};
        GlobalCharArrayHolder global3{threadData};
        StackObjectHolder stack1{threadData};
        StackObjectArrayHolder stack2{threadData};
        StackCharArrayHolder stack3{threadData};

        global1->field1 = stack1.header();
        global1->field2 = global1.header();
        global1->field3 = global2.header();
        global2[0] = global1.header();
        global2[1] = global3.header();
        stack1->field1 = global1.header();
        stack1->field2 = stack1.header();
        stack1->field3 = stack2.header();
        stack2[0] = stack1.header();
        stack2[1] = stack3.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        ASSERT_THAT(GetColor(global1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(global2.header()), Color::kWhite);
        ASSERT_THAT(GetColor(global3.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack2.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack3.header()), Color::kWhite);

        threadData.gc().PerformFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        EXPECT_THAT(GetColor(global1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(global2.header()), Color::kWhite);
        EXPECT_THAT(GetColor(global3.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack2.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack3.header()), Color::kWhite);
    });
}

TEST_F(SameThreadMarkAndSweepTest, FreeObjects) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), object2.header()));
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object2.header()), Color::kWhite);

        threadData.gc().PerformFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TEST_F(SameThreadMarkAndSweepTest, FreeObjectsWithFinalizers) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        auto& object1 = AllocateObjectWithFinalizer(threadData);
        auto& object2 = AllocateObjectWithFinalizer(threadData);

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), object2.header()));
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object2.header()), Color::kWhite);

        EXPECT_CALL(finalizerHook(), Call(object1.header()));
        EXPECT_CALL(finalizerHook(), Call(object2.header()));
        threadData.gc().PerformFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TEST_F(SameThreadMarkAndSweepTest, FreeObjectWithFreeWeak) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        auto& weak1 = ([&threadData, &object1]() -> WeakCounter& {
            ObjHolder holder;
            return InstallWeakCounter(threadData, object1.header(), holder.slot());
        })();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), weak1.header()));
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(weak1.header()), Color::kWhite);
        ASSERT_THAT(weak1->referred, object1.header());

        threadData.gc().PerformFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TEST_F(SameThreadMarkAndSweepTest, FreeObjectWithHoldedWeak) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        StackObjectHolder stack{threadData};
        auto& weak1 = InstallWeakCounter(threadData, object1.header(), &stack->field1);

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), weak1.header(), stack.header()));
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(weak1.header()), Color::kWhite);
        ASSERT_THAT(weak1->referred, object1.header());

        threadData.gc().PerformFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(weak1.header(), stack.header()));
        EXPECT_THAT(GetColor(weak1.header()), Color::kWhite);
        EXPECT_THAT(weak1->referred, nullptr);
    });
}

TEST_F(SameThreadMarkAndSweepTest, ObjectReferencedFromRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);
        auto& object3 = AllocateObject(threadData);
        auto& object4 = AllocateObject(threadData);

        global->field1 = object1.header();
        object1->field1 = object2.header();
        stack->field1 = object3.header();
        object3->field1 = object4.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        ASSERT_THAT(GetColor(global.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object2.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object3.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object4.header()), Color::kWhite);

        threadData.gc().PerformFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(GetColor(global.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object2.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object3.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object4.header()), Color::kWhite);
    });
}

TEST_F(SameThreadMarkAndSweepTest, ObjectsWithCycles) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);
        auto& object3 = AllocateObject(threadData);
        auto& object4 = AllocateObject(threadData);
        auto& object5 = AllocateObject(threadData);
        auto& object6 = AllocateObject(threadData);

        global->field1 = object1.header();
        object1->field1 = object2.header();
        object2->field1 = object1.header();
        stack->field1 = object3.header();
        object3->field1 = object4.header();
        object4->field1 = object3.header();
        object5->field1 = object6.header();
        object6->field1 = object5.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header(),
                        object5.header(), object6.header()));
        ASSERT_THAT(GetColor(global.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object2.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object3.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object4.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object5.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object6.header()), Color::kWhite);

        threadData.gc().PerformFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(GetColor(global.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object2.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object3.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object4.header()), Color::kWhite);
    });
}

TEST_F(SameThreadMarkAndSweepTest, ObjectsWithCyclesAndFinalizers) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObjectWithFinalizer(threadData);
        auto& object2 = AllocateObjectWithFinalizer(threadData);
        auto& object3 = AllocateObjectWithFinalizer(threadData);
        auto& object4 = AllocateObjectWithFinalizer(threadData);
        auto& object5 = AllocateObjectWithFinalizer(threadData);
        auto& object6 = AllocateObjectWithFinalizer(threadData);

        global->field1 = object1.header();
        object1->field1 = object2.header();
        object2->field1 = object1.header();
        stack->field1 = object3.header();
        object3->field1 = object4.header();
        object4->field1 = object3.header();
        object5->field1 = object6.header();
        object6->field1 = object5.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header(),
                        object5.header(), object6.header()));
        ASSERT_THAT(GetColor(global.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object2.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object3.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object4.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object5.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object6.header()), Color::kWhite);

        EXPECT_CALL(finalizerHook(), Call(object5.header()));
        EXPECT_CALL(finalizerHook(), Call(object6.header()));
        threadData.gc().PerformFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(GetColor(global.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object2.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object3.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object4.header()), Color::kWhite);
    });
}

TEST_F(SameThreadMarkAndSweepTest, ObjectsWithCyclesIntoRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);

        global->field1 = object1.header();
        object1->field1 = global.header();
        stack->field1 = object2.header();
        object2->field1 = stack.header();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), stack.header(), object1.header(), object2.header()));
        ASSERT_THAT(GetColor(global.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object2.header()), Color::kWhite);

        threadData.gc().PerformFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), stack.header(), object1.header(), object2.header()));
        EXPECT_THAT(GetColor(global.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object2.header()), Color::kWhite);
    });
}

TEST_F(SameThreadMarkAndSweepTest, RunGCTwice) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);
        auto& object3 = AllocateObject(threadData);
        auto& object4 = AllocateObject(threadData);
        auto& object5 = AllocateObject(threadData);
        auto& object6 = AllocateObject(threadData);

        global->field1 = object1.header();
        object1->field1 = object2.header();
        object2->field1 = object1.header();
        stack->field1 = object3.header();
        object3->field1 = object4.header();
        object4->field1 = object3.header();
        object5->field1 = object6.header();
        object6->field1 = object5.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header(),
                        object5.header(), object6.header()));
        ASSERT_THAT(GetColor(global.header()), Color::kWhite);
        ASSERT_THAT(GetColor(stack.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object2.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object3.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object4.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object5.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object6.header()), Color::kWhite);

        threadData.gc().PerformFullGC();
        threadData.gc().PerformFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(GetColor(global.header()), Color::kWhite);
        EXPECT_THAT(GetColor(stack.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object1.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object2.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object3.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object4.header()), Color::kWhite);
    });
}

TEST_F(SameThreadMarkAndSweepTest, PermanentObjects) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalPermanentObjectHolder global1{threadData};
        GlobalObjectHolder global2{threadData};
        test_support::Object<Payload> permanentObject{typeHolder.typeInfo()};
        permanentObject.header()->typeInfoOrMeta_ =
                setPointerBits(permanentObject.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        RuntimeAssert(permanentObject.header()->permanent(), "Must be permanent");

        global1->field1 = permanentObject.header();
        global2->field1 = global1.header();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(global2.header()));
        EXPECT_THAT(GetColor(global2.header()), Color::kWhite);

        threadData.gc().PerformFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global2.header()));
        EXPECT_THAT(GetColor(global2.header()), Color::kWhite);
    });
}

TEST_F(SameThreadMarkAndSweepTest, SameObjectInRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack(*global);
        auto& object = AllocateObject(threadData);

        global->field1 = object.header();

        ASSERT_THAT(global.header(), stack.header());
        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), object.header()));
        EXPECT_THAT(GetColor(global.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object.header()), Color::kWhite);

        threadData.gc().PerformFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), object.header()));
        EXPECT_THAT(GetColor(global.header()), Color::kWhite);
        EXPECT_THAT(GetColor(object.header()), Color::kWhite);
    });
}

namespace {

class Mutator : private Pinned {
public:
    Mutator() : thread_(&Mutator::RunLoop, this) {}

    ~Mutator() {
        {
            std::unique_lock guard(queueMutex_);
            shutdownRequested_ = true;
        }
        queueCV_.notify_one();
        thread_.join();
        RuntimeAssert(queue_.empty(), "The queue must be empty, has size=%zu", queue_.size());
        RuntimeAssert(memory_ == nullptr, "Memory must have been deinitialized");
        RuntimeAssert(stackRoots_.empty(), "Stack roots must be empty, has size=%zu", stackRoots_.size());
        RuntimeAssert(globalRoots_.empty(), "Global roots must be empty, has size=%zu", globalRoots_.size());
    }

    template <typename F>
    [[nodiscard]] std::future<void> Execute(F&& f) {
        std::packaged_task<void()> task([this, f = std::forward<F>(f)]() { f(*memory_->memoryState()->GetThreadData(), *this); });
        auto future = task.get_future();
        {
            std::unique_lock guard(queueMutex_);
            queue_.push_back(std::move(task));
        }
        queueCV_.notify_one();
        return future;
    }

    StackObjectHolder& AddStackRoot() {
        RuntimeAssert(std::this_thread::get_id() == thread_.get_id(), "AddStackRoot can only be called in the mutator thread");
        auto holder = make_unique<StackObjectHolder>(*memory_->memoryState()->GetThreadData());
        auto& holderRef = *holder;
        stackRoots_.push_back(std::move(holder));
        return holderRef;
    }

    StackObjectHolder& AddStackRoot(ObjHeader* object) {
        RuntimeAssert(std::this_thread::get_id() == thread_.get_id(), "AddStackRoot can only be called in the mutator thread");
        auto holder = make_unique<StackObjectHolder>(object);
        auto& holderRef = *holder;
        stackRoots_.push_back(std::move(holder));
        return holderRef;
    }

    GlobalObjectHolder& AddGlobalRoot() {
        RuntimeAssert(std::this_thread::get_id() == thread_.get_id(), "AddGlobalRoot can only be called in the mutator thread");
        auto holder = make_unique<GlobalObjectHolder>(*memory_->memoryState()->GetThreadData());
        auto& holderRef = *holder;
        globalRoots_.push_back(std::move(holder));
        return holderRef;
    }

    GlobalObjectHolder& AddGlobalRoot(ObjHeader* object) {
        RuntimeAssert(std::this_thread::get_id() == thread_.get_id(), "AddGlobalRoot can only be called in the mutator thread");
        auto holder = make_unique<GlobalObjectHolder>(*memory_->memoryState()->GetThreadData(), object);
        auto& holderRef = *holder;
        globalRoots_.push_back(std::move(holder));
        return holderRef;
    }

    KStdVector<ObjHeader*> Alive() { return ::Alive(*memory_->memoryState()->GetThreadData()); }

private:
    void RunLoop() {
        memory_ = make_unique<ScopedMemoryInit>();
        AssertThreadState(memory_->memoryState(), ThreadState::kRunnable);

        while (true) {
            std::packaged_task<void()> task;
            {
                std::unique_lock guard(queueMutex_);
                queueCV_.wait(guard, [this]() { return !queue_.empty() || shutdownRequested_; });
                if (shutdownRequested_) {
                    globalRoots_.clear();
                    stackRoots_.clear();
                    memory_.reset();
                    return;
                }
                task = std::move(queue_.front());
                queue_.pop_front();
            }
            task();
        }
    }

    KStdUniquePtr<ScopedMemoryInit> memory_;

    // TODO: Consider full runtime init instead, and interact with initialized worker
    std::condition_variable queueCV_;
    std::mutex queueMutex_;
    KStdDeque<std::packaged_task<void()>> queue_;
    bool shutdownRequested_ = false;
    std::thread thread_;

    KStdVector<KStdUniquePtr<GlobalObjectHolder>> globalRoots_;
    KStdVector<KStdUniquePtr<StackObjectHolder>> stackRoots_;
};

} // namespace

TEST_F(SameThreadMarkAndSweepTest, MultipleMutatorsCollect) {
    KStdVector<Mutator> mutators(kDefaultThreadCount);
    KStdVector<ObjHeader*> globals(kDefaultThreadCount);
    KStdVector<ObjHeader*> locals(kDefaultThreadCount);
    KStdVector<ObjHeader*> reachables(kDefaultThreadCount);

    auto expandRootSet = [&globals, &locals, &reachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = mutator.AddGlobalRoot();
        auto& local = mutator.AddStackRoot();
        auto& reachable = AllocateObject(threadData);
        AllocateObject(threadData);
        local->field1 = reachable.header();
        globals[i] = global.header();
        locals[i] = local.header();
        reachables[i] = reachable.header();
    };

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i); })
                .wait();
    }

    KStdVector<std::future<void>> gcFutures(kDefaultThreadCount);

    gcFutures[0] = mutators[0].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().PerformFullGC(); });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] =
                mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().SafePointFunctionPrologue(); });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    KStdVector<ObjHeader*> expectedAlive;
    for (auto& global : globals) {
        expectedAlive.push_back(global);
    }
    for (auto& local : locals) {
        expectedAlive.push_back(local);
    }
    for (auto& reachable : reachables) {
        expectedAlive.push_back(reachable);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }
}

TEST_F(SameThreadMarkAndSweepTest, MultipleMutatorsAllCollect) {
    KStdVector<Mutator> mutators(kDefaultThreadCount);
    KStdVector<ObjHeader*> globals(kDefaultThreadCount);
    KStdVector<ObjHeader*> locals(kDefaultThreadCount);
    KStdVector<ObjHeader*> reachables(kDefaultThreadCount);

    auto expandRootSet = [&globals, &locals, &reachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = mutator.AddGlobalRoot();
        auto& local = mutator.AddStackRoot();
        auto& reachable = AllocateObject(threadData);
        AllocateObject(threadData);
        local->field1 = reachable.header();
        globals[i] = global.header();
        locals[i] = local.header();
        reachables[i] = reachable.header();
    };

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i); })
                .wait();
    }

    KStdVector<std::future<void>> gcFutures(kDefaultThreadCount);

    // TODO: Maybe check that only one GC is performed.
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        gcFutures[i] = mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().PerformFullGC(); });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    KStdVector<ObjHeader*> expectedAlive;
    for (auto& global : globals) {
        expectedAlive.push_back(global);
    }
    for (auto& local : locals) {
        expectedAlive.push_back(local);
    }
    for (auto& reachable : reachables) {
        expectedAlive.push_back(reachable);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }
}

TEST_F(SameThreadMarkAndSweepTest, MultipleMutatorsAddToRootSetAfterCollectionRequested) {
    KStdVector<Mutator> mutators(kDefaultThreadCount);
    KStdVector<ObjHeader*> globals(kDefaultThreadCount);
    KStdVector<ObjHeader*> locals(kDefaultThreadCount);
    KStdVector<ObjHeader*> reachables(kDefaultThreadCount);

    auto allocateInHeap = [&globals, &locals, &reachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = AllocateObject(threadData);
        auto& local = AllocateObject(threadData);
        auto& reachable = AllocateObject(threadData);
        AllocateObject(threadData);

        local->field1 = reachable.header();

        globals[i] = global.header();
        locals[i] = local.header();
        reachables[i] = reachable.header();
    };

    auto expandRootSet = [&globals, &locals](mm::ThreadData& threadData, Mutator& mutator, int i) {
        mutator.AddGlobalRoot(globals[i]);
        mutator.AddStackRoot(locals[i]);
    };

    mutators[0]
            .Execute([expandRootSet, allocateInHeap](mm::ThreadData& threadData, Mutator& mutator) {
                allocateInHeap(threadData, mutator, 0);
                expandRootSet(threadData, mutator, 0);
            })
            .wait();

    // Allocate everything in heap before scheduling the GC.
    for (int i = 1; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([allocateInHeap, i](mm::ThreadData& threadData, Mutator& mutator) { allocateInHeap(threadData, mutator, i); })
                .wait();
    }

    KStdVector<std::future<void>> gcFutures(kDefaultThreadCount);
    gcFutures[0] = mutators[0].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().PerformFullGC(); });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] = mutators[i].Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) {
            expandRootSet(threadData, mutator, i);
            threadData.gc().SafePointFunctionPrologue();
        });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    KStdVector<ObjHeader*> expectedAlive;
    for (auto& global : globals) {
        expectedAlive.push_back(global);
    }
    for (auto& local : locals) {
        expectedAlive.push_back(local);
    }
    for (auto& reachable : reachables) {
        expectedAlive.push_back(reachable);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }
}

TEST_F(SameThreadMarkAndSweepTest, CrossThreadReference) {
    KStdVector<Mutator> mutators(kDefaultThreadCount);
    KStdVector<ObjHeader*> globals(kDefaultThreadCount);
    KStdVector<ObjHeader*> locals(kDefaultThreadCount);
    KStdVector<ObjHeader*> reachables(2 * kDefaultThreadCount);

    auto expandRootSet = [&globals, &locals, &reachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = mutator.AddGlobalRoot();
        auto& local = mutator.AddStackRoot();
        auto& reachable1 = AllocateObject(threadData);
        auto& reachable2 = AllocateObject(threadData);
        globals[i] = global.header();
        locals[i] = local.header();
        reachables[2 * i] = reachable1.header();
        reachables[2 * i + 1] = reachable2.header();

        // Expected to be run consequtively, so `reachables` for `j < i` are set.
        if (i != 0) {
            global->field1 = reachables[2 * (i - 1)];
            local->field1 = reachables[2 * (i - 1) + 1];
        }
    };

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        // `expandRootSet` is expected to be run consequtively for each thread, so `.wait()` is required below.
        mutators[i]
                .Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i); })
                .wait();
    }

    KStdVector<std::future<void>> gcFutures(kDefaultThreadCount);

    gcFutures[0] = mutators[0].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().PerformFullGC(); });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] =
                mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().SafePointFunctionPrologue(); });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    KStdVector<ObjHeader*> expectedAlive;
    for (auto& global : globals) {
        expectedAlive.push_back(global);
    }
    for (auto& local : locals) {
        expectedAlive.push_back(local);
    }
    // The last two are in fact unreachable. Their absence allows us to check that GC was in fact performed.
    reachables.pop_back();
    reachables.pop_back();
    for (auto& reachable : reachables) {
        expectedAlive.push_back(reachable);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }
}

TEST_F(SameThreadMarkAndSweepTest, MultipleMutatorsWeaks) {
    KStdVector<Mutator> mutators(kDefaultThreadCount);
    ObjHeader* globalRoot = nullptr;
    WeakCounter* weak = nullptr;

    mutators[0]
            .Execute([&weak, &globalRoot](mm::ThreadData& threadData, Mutator& mutator) {
                auto& global = mutator.AddGlobalRoot();

                auto& object = AllocateObject(threadData);
                auto& objectWeak = ([&threadData, &object]() -> WeakCounter& {
                    ObjHolder holder;
                    return InstallWeakCounter(threadData, object.header(), holder.slot());
                })();
                global->field1 = objectWeak.header();
                weak = &objectWeak;
                globalRoot = global.header();
            })
            .wait();

    // Make sure all mutators are initialized.
    for (int i = 1; i < kDefaultThreadCount; ++i) {
        mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) {}).wait();
    }

    KStdVector<std::future<void>> gcFutures(kDefaultThreadCount);

    gcFutures[0] = mutators[0].Execute([weak](mm::ThreadData& threadData, Mutator& mutator) {
        threadData.gc().PerformFullGC();
        EXPECT_THAT((*weak)->referred, nullptr);
    });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] = mutators[i].Execute([weak](mm::ThreadData& threadData, Mutator& mutator) {
            threadData.gc().SafePointFunctionPrologue();
            EXPECT_THAT((*weak)->referred, nullptr);
        });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAre(globalRoot, weak->header()));
    }
}

TEST_F(SameThreadMarkAndSweepTest, NewThreadsWhileRequestingCollection) {
    KStdVector<Mutator> mutators(kDefaultThreadCount);
    KStdVector<ObjHeader*> globals(2 * kDefaultThreadCount);
    KStdVector<ObjHeader*> locals(2 * kDefaultThreadCount);
    KStdVector<ObjHeader*> reachables(2 * kDefaultThreadCount);
    KStdVector<ObjHeader*> unreachables(2 * kDefaultThreadCount);

    auto expandRootSet = [&globals, &locals, &reachables, &unreachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = mutator.AddGlobalRoot();
        auto& local = mutator.AddStackRoot();
        auto& reachable = AllocateObject(threadData);
        auto& unreachable = AllocateObject(threadData);
        local->field1 = reachable.header();
        globals[i] = global.header();
        locals[i] = local.header();
        reachables[i] = reachable.header();
        unreachables[i] = unreachable.header();
    };

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i); })
                .wait();
    }

    KStdVector<std::future<void>> gcFutures(kDefaultThreadCount);

    gcFutures[0] = mutators[0].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().PerformFullGC(); });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    // Now start attaching new threads.
    KStdVector<Mutator> newMutators(kDefaultThreadCount);
    KStdVector<std::future<void>> attachFutures(kDefaultThreadCount);

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        attachFutures[i] = newMutators[i].Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i + kDefaultThreadCount); });
    }

    // All the other threads are stopping at safe points.
    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] =
                mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().SafePointFunctionPrologue(); });
    }

    // GC will be completed first
    for (auto& future : gcFutures) {
        future.wait();
    }

    // Only then will the new threads be allowed to attach.
    for (auto& future : attachFutures) {
        future.wait();
    }

    // Old mutators don't even see alive objects from the new threads yet (as the latter ones have not published anything).

    KStdVector<ObjHeader*> expectedAlive;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        expectedAlive.push_back(globals[i]);
        expectedAlive.push_back(locals[i]);
        expectedAlive.push_back(reachables[i]);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        KStdVector<ObjHeader*> aliveForThisThread(expectedAlive.begin(), expectedAlive.end());
        aliveForThisThread.push_back(globals[kDefaultThreadCount + i]);
        aliveForThisThread.push_back(locals[kDefaultThreadCount + i]);
        aliveForThisThread.push_back(reachables[kDefaultThreadCount + i]);
        // Unreachables for new threads were not collected.
        aliveForThisThread.push_back(unreachables[kDefaultThreadCount + i]);
        EXPECT_THAT(newMutators[i].Alive(), testing::UnorderedElementsAreArray(aliveForThisThread));
    }
}


TEST_F(SameThreadMarkAndSweepTest, FreeObjectWithFreeWeakReversedOrder) {
    KStdVector<Mutator> mutators(2);
    std::atomic<test_support::Object<Payload>*> object1 = nullptr;
    std::atomic<WeakCounter*> weak = nullptr;
    std::atomic<bool> done = false;
    auto f0 = mutators[0].Execute([&](mm::ThreadData& threadData, Mutator &) {
        GlobalObjectHolder global1{threadData};
        auto& object1_local = AllocateObject(threadData);
        object1 = &object1_local;
        global1->field1 = object1_local.header();
        while (weak.load() == nullptr);
        threadData.gc().PerformFullGC();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1_local.header(), weak.load()->header(), global1.header()));
        ASSERT_THAT(GetColor(global1.header()), Color::kWhite);
        ASSERT_THAT(GetColor(object1_local.header()), Color::kWhite);
        ASSERT_THAT(GetColor(weak.load()->header()), Color::kWhite);
        ASSERT_THAT((*weak.load())->referred, object1_local.header());

        global1->field1 = nullptr;

        threadData.gc().PerformFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global1.header()));
        done = true;
    });
    
    auto f1 = mutators[1].Execute([&](mm::ThreadData& threadData, Mutator &) {
        while (object1.load() == nullptr) {}
        ObjHolder holder;
        auto &weak_local = InstallWeakCounter(threadData, object1.load()->header(), holder.slot());
        weak = &weak_local;
        *holder.slot() = nullptr;
        while (!done) threadData.gc().SafePointLoopBody();
    });

    f0.wait();
    f1.wait();
}
